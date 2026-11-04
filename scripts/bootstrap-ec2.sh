#!/usr/bin/env bash
# Bootstrap idempotente de la EC2 (Ubuntu 26.04 LTS) para el despliegue de la spec 005.
# Se ejecuta como usuario normal con sudo (no como root), desde cualquier directorio:
#   scripts/bootstrap-ec2.sh
# Orden: validar .env -> comprobar ./corpus -> paquetes ligeros -> verificar RDS -> swap de 4 GB
#   -> Docker -> grupo docker, python3 y modelo ONNX -> `docker compose -f compose.prod.yaml up -d --build`
#   -> espera de "listo" (salud UP, modelo en Ollama y 20 artículos).
# Cada paso comprueba si ya está hecho antes de actuar, así que se puede re-ejecutar sin riesgo.
# No usa AWS, no sube imágenes a ningún registro y nunca borra volúmenes.
# Variable opcional: READY_TIMEOUT_SECONDS (espera máxima de "listo", 1200 por defecto).
# Códigos de salida: 0 = listo; 1 = fallo con un mensaje en español que dice qué corregir.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

COMPOSE_FILE=compose.prod.yaml
ENV_FILE=.env
ENV_EXAMPLE=.env.prod.example
CORPUS_DIR=corpus
EXPECTED_ARTICLES=20
MODEL_DIR=src/main/resources/models/specter2-base
SWAPFILE=/swapfile
SWAP_SIZE=4G
READY_TIMEOUT_SECONDS="${READY_TIMEOUT_SECONDS:-1200}"
READY_INTERVAL_SECONDS=10
OLLAMA_MODEL=nomic-embed-text:v1.5

info() {
  echo "==> $*"
}

warn() {
  echo "AVISO: $*" >&2
}

# Termina con código 1 y un mensaje que dice qué corregir.
fail() {
  echo "ERROR: $*" >&2
  exit 1
}

if [ "${EUID:-$(id -u)}" -eq 0 ]; then
  fail "no ejecute este script como root. Use un usuario normal con sudo: scripts/bootstrap-ec2.sh"
fi

# Lee el valor de una clave de .env sin ejecutar el archivo (la contraseña puede tener $, comillas
# o espacios). Toma la última aparición y quita CR final y comillas simples o dobles envolventes.
env_get() {
  local key="$1"
  { grep -E "^${key}=" "$ENV_FILE" || true; } | tail -n 1 | sed -E \
    -e "s/^${key}=//" -e 's/\r$//' \
    -e 's/^"(.*)"$/\1/' -e "s/^'(.*)'\$/\1/"
}

# ---------------------------------------------------------------------------------------------
# 1. Validación de .env (antes de instalar nada)
# ---------------------------------------------------------------------------------------------
validate_env() {
  if [ ! -f "$ENV_FILE" ]; then
    [ -f "$ENV_EXAMPLE" ] || fail "no existe $ENV_EXAMPLE en $(pwd); ejecute el script desde el repositorio completo."
    (umask 077 && cp "$ENV_EXAMPLE" "$ENV_FILE")
    chmod 600 "$ENV_FILE"
    fail "no existía $ENV_FILE; se creó desde $ENV_EXAMPLE con permisos 600. Complete los valores que empiezan con CAMBIAR_ (y DOMAIN y CERTBOT_EMAIL) y vuelva a ejecutar el script."
  fi

  local missing=() key value domain email
  for key in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD \
    BIBLIOMTR_CORPUS_DIR BIBLIOMTR_OLLAMA_URL; do
    value="$(env_get "$key")"
    if [ -z "$value" ] || [[ "$value" == *CAMBIAR_* ]]; then
      missing+=("$key")
    fi
  done

  domain="$(env_get DOMAIN)"
  if [ "$domain" = "tu-dominio.example" ] || [[ "$domain" == *CAMBIAR_* ]]; then
    missing+=("DOMAIN (escriba su dominio real, o déjelo vacío para servir solo por HTTP)")
  elif [ -n "$domain" ]; then
    email="$(env_get CERTBOT_EMAIL)"
    if [[ "$email" == *CAMBIAR_* ]] || ! [[ "$email" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]]; then
      missing+=("CERTBOT_EMAIL (correo válido, obligatorio cuando DOMAIN está definido)")
    fi
  fi

  if [ "${#missing[@]}" -gt 0 ]; then
    echo "ERROR: faltan valores en $ENV_FILE (no se muestran las contraseñas):" >&2
    printf '  - %s\n' "${missing[@]}" >&2
    fail "complete $ENV_FILE y vuelva a ejecutar el script."
  fi
  info "$ENV_FILE completo"
}

# ---------------------------------------------------------------------------------------------
# 2. Corpus
# ---------------------------------------------------------------------------------------------
check_corpus() {
  mkdir -p "$CORPUS_DIR"
  local count
  count="$(find "$CORPUS_DIR" -maxdepth 1 -type f -name '*.pdf' | wc -l)"
  if [ "$count" -ne "$EXPECTED_ARTICLES" ]; then
    fail "./$CORPUS_DIR tiene $count archivos PDF y se esperan $EXPECTED_ARTICLES. Copie los $EXPECTED_ARTICLES PDF del docente a ./$CORPUS_DIR y vuelva a ejecutar el script."
  fi
  info "./$CORPUS_DIR con $count PDF"
}

# ---------------------------------------------------------------------------------------------
# 3. Paquetes (solo se instalan los que faltan)
# ---------------------------------------------------------------------------------------------
package_installed() {
  dpkg -s "$1" 2>/dev/null | grep -q '^Status: install ok installed'
}

ensure_packages() {
  local missing=() package
  for package in "$@"; do
    package_installed "$package" || missing+=("$package")
  done
  if [ "${#missing[@]}" -eq 0 ]; then
    info "paquetes ya instalados: $*"
    return
  fi
  info "instalando paquetes: ${missing[*]}"
  sudo apt-get update
  sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y "${missing[@]}"
}

# ---------------------------------------------------------------------------------------------
# 4. RDS: alcance en el puerto y extensión vector (no se ejecuta CREATE EXTENSION)
# ---------------------------------------------------------------------------------------------
check_rds() {
  local url host port database username password
  url="$(env_get SPRING_DATASOURCE_URL)"
  if ! [[ "$url" =~ ^jdbc:postgresql://([^:/?]+)(:([0-9]+))?/([^?]+) ]]; then
    fail "SPRING_DATASOURCE_URL no tiene el formato jdbc:postgresql://<endpoint>:5432/<base>?sslmode=require."
  fi
  host="${BASH_REMATCH[1]}"
  port="${BASH_REMATCH[3]:-5432}"
  database="${BASH_REMATCH[4]}"
  username="$(env_get SPRING_DATASOURCE_USERNAME)"
  password="$(env_get SPRING_DATASOURCE_PASSWORD)"

  info "verificando RDS en $host:$port"
  if ! pg_isready -h "$host" -p "$port" -t 10 >/dev/null 2>&1; then
    fail "RDS no responde en $host:$port. Revise (1) que el endpoint de SPRING_DATASOURCE_URL sea el correcto y (2) que el grupo de seguridad de RDS permita el puerto $port desde el grupo de seguridad de esta EC2."
  fi

  local result installed available
  if ! result="$(PGPASSWORD="$password" PGSSLMODE=require PGCONNECT_TIMEOUT=10 psql \
    -h "$host" -p "$port" -U "$username" -d "$database" -v ON_ERROR_STOP=1 -tA -F ' ' -c \
    "SELECT (SELECT count(*) FROM pg_extension WHERE extname = 'vector'), (SELECT count(*) FROM pg_available_extensions WHERE name = 'vector')" 2>&1)"; then
    fail "no se pudo consultar RDS con psql (sslmode=require). Revise usuario, contraseña y nombre de la base ($database) en $ENV_FILE. Detalle: ${result//"$password"/***}"
  fi
  # psql puede anteponer avisos en stderr: el resultado es la última línea ("instaladas disponibles").
  result="$(printf '%s\n' "$result" | tail -n 1)"
  installed="${result%% *}"
  available="${result##* }"
  if [ "${installed:-0}" -eq 0 ] && [ "${available:-0}" -eq 0 ]; then
    fail "la extensión vector no está instalada ni disponible en RDS. La migración Flyway V1__enable_pgvector.sql la crea al arrancar la API, pero RDS debe ofrecerla: use una versión de PostgreSQL en RDS con pgvector."
  fi
  info "RDS responde y la extensión vector está disponible"
}

# ---------------------------------------------------------------------------------------------
# 5. Swap de 4 GB en /swapfile
# ---------------------------------------------------------------------------------------------
ensure_swap() {
  if swapon --show=NAME --noheadings 2>/dev/null | grep -qx "$SWAPFILE"; then
    info "swap $SWAPFILE ya activo"
  else
    info "creando swap de $SWAP_SIZE en $SWAPFILE"
    if [ ! -e "$SWAPFILE" ]; then
      sudo fallocate -l "$SWAP_SIZE" "$SWAPFILE"
    fi
    sudo chmod 600 "$SWAPFILE"
    sudo mkswap "$SWAPFILE" >/dev/null
    sudo swapon "$SWAPFILE"
  fi
  if ! grep -Eq "^${SWAPFILE}[[:space:]]" /etc/fstab; then
    info "agregando $SWAPFILE a /etc/fstab"
    echo "$SWAPFILE none swap sw 0 0" | sudo tee -a /etc/fstab >/dev/null
  fi
}

# ---------------------------------------------------------------------------------------------
# 6. Docker: Docker CE si el repositorio oficial ofrece paquetes para esta versión; si no, docker.io
# ---------------------------------------------------------------------------------------------
apt_candidate() {
  apt-cache policy "$1" 2>/dev/null | awk '/Candidate:/ {print $2}'
}

install_docker_io() {
  sudo rm -f /etc/apt/sources.list.d/docker.sources
  sudo apt-get update
  local candidate
  candidate="$(apt_candidate docker-compose-v2)"
  if [ -z "$candidate" ] || [ "$candidate" = "(none)" ]; then
    fail "ni el repositorio de Docker CE ni Ubuntu ofrecen un paquete de Compose (docker-compose-v2 sin candidato). Instale el plugin de Docker Compose a mano y vuelva a ejecutar el script."
  fi
  sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-v2
}

install_docker() {
  if docker compose version >/dev/null 2>&1; then
    info "Docker Compose ya instalado: $(docker compose version --short 2>/dev/null || echo ok)"
    return
  fi

  local codename arch base candidate use_ce=0
  codename="$({ grep -E '^VERSION_CODENAME=' /etc/os-release || true; } | head -n 1 | cut -d= -f2 | tr -d '"')"
  arch="$(dpkg --print-architecture)"
  base=https://download.docker.com/linux/ubuntu

  if [ -n "$codename" ] \
    && curl -fsI "$base/dists/$codename/Release" >/dev/null 2>&1 \
    && curl -fsI "$base/dists/$codename/stable/binary-$arch/Packages" >/dev/null 2>&1; then
    info "el repositorio de Docker CE tiene paquetes para '$codename'; configurando la fuente apt"
    sudo install -m 0755 -d /etc/apt/keyrings
    if sudo curl -fsSL "$base/gpg" -o /etc/apt/keyrings/docker.asc; then
      sudo chmod a+r /etc/apt/keyrings/docker.asc
      sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: $base
Suites: $codename
Components: stable
Architectures: $arch
Signed-By: /etc/apt/keyrings/docker.asc
EOF
      if sudo apt-get update; then
        candidate="$(apt_candidate docker-ce)"
        if [ -n "$candidate" ] && [ "$candidate" != "(none)" ]; then
          use_ce=1
        fi
      fi
    fi
  fi

  if [ "$use_ce" -eq 1 ]; then
    info "instalando Docker CE y docker-compose-plugin"
    sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y \
      docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  else
    warn "repositorio de Docker CE sin paquetes para '${codename:-desconocido}'; se usa docker.io de Ubuntu"
    install_docker_io
  fi

  if command -v systemctl >/dev/null 2>&1; then
    sudo systemctl enable --now docker
  fi
  docker compose version >/dev/null 2>&1 || fail "Docker Compose no quedó disponible tras la instalación."
}

# ---------------------------------------------------------------------------------------------
# 7. Grupo docker, python3 y modelo ONNX
# ---------------------------------------------------------------------------------------------
DOCKER_SUDO=0

ensure_docker_group() {
  local user
  user="$(id -un)"
  if ! getent group docker | grep -Eq "(:|,)${user}(,|\$)"; then
    info "agregando a $user al grupo docker"
    sudo usermod -aG docker "$user"
  fi
  # El grupo solo aplica a sesiones nuevas: si esta no lo tiene, los comandos de Docker usan sudo.
  if ! id -nG | tr ' ' '\n' | grep -qx docker; then
    DOCKER_SUDO=1
    warn "el grupo docker se aplicará al abrir una sesión nueva; en esta ejecución se usa sudo"
  fi
}

docker_cli() {
  if [ "$DOCKER_SUDO" -eq 1 ]; then
    sudo docker "$@"
  else
    docker "$@"
  fi
}

compose() {
  docker_cli compose -f "$COMPOSE_FILE" "$@"
}

ensure_model() {
  ensure_packages python3 python3-venv
  if [ -s "$MODEL_DIR/model.onnx" ] && [ -s "$MODEL_DIR/tokenizer.json" ]; then
    info "modelo SPECTER2 ya presente en $MODEL_DIR"
  else
    info "descargando y exportando SPECTER2 a ONNX (tarda varios minutos)"
    scripts/download-model.sh
  fi
}

# ---------------------------------------------------------------------------------------------
# 8. Arranque y espera de "listo"
# ---------------------------------------------------------------------------------------------
# Estado de salud de un servicio: healthy, unhealthy, starting, running, exited o absent.
service_state() {
  local id
  id="$(compose ps -a -q "$1" 2>/dev/null | head -n 1 || true)"
  if [ -z "$id" ]; then
    echo absent
    return
  fi
  docker_cli inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id" 2>/dev/null || echo absent
}

# Muestra las últimas 50 líneas del primer servicio no saludable (api si todos lo están).
diagnose() {
  local service target=api
  for service in ollama api web certbot; do
    if [ "$(service_state "$service")" != "healthy" ]; then
      target="$service"
      break
    fi
  done
  echo "--- Últimas 50 líneas del registro de '$target' ---" >&2
  compose logs --tail=50 "$target" >&2 || true
  echo "Para ver el resto: docker compose -f $COMPOSE_FILE logs" >&2
}

start_stack() {
  info "ejecutando: docker compose -f $COMPOSE_FILE up -d --build"
  if ! compose up -d --build; then
    echo "ERROR: 'up' falló (p. ej. una dependencia no quedó saludable)." >&2
    diagnose
    exit 1
  fi
}

# Consulta HTTP desde el contenedor web (misma red de Compose, sin depender de HTTP/HTTPS).
web_get() {
  compose exec -T web wget -qO- "$1" 2>/dev/null || true
}

wait_ready() {
  if ! [[ "$READY_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]]; then
    fail "READY_TIMEOUT_SECONDS debe ser un número entero de segundos."
  fi
  info "esperando que el despliegue esté listo (máximo ${READY_TIMEOUT_SECONDS} s)"
  local start="$SECONDS" health tags articles up_ok model_ok count_ok count
  while true; do
    health="$(web_get http://api:8080/api/actuator/health)"
    tags="$(web_get http://ollama:11434/api/tags)"
    articles="$(web_get http://api:8080/api/articles)"
    count="$(printf '%s' "$articles" | grep -o '"fileName"' | wc -l)"
    up_ok=no; model_ok=no; count_ok=no
    [[ "$health" == *'"status":"UP"'* ]] && up_ok=si
    [[ "$tags" == *"$OLLAMA_MODEL"* ]] && model_ok=si
    [ "$count" -eq "$EXPECTED_ARTICLES" ] && count_ok=si
    info "salud UP: $up_ok | modelo $OLLAMA_MODEL: $model_ok | artículos: $count/$EXPECTED_ARTICLES"
    if [ "$up_ok" = si ] && [ "$model_ok" = si ] && [ "$count_ok" = si ]; then
      break
    fi
    if [ $((SECONDS - start)) -ge "$READY_TIMEOUT_SECONDS" ]; then
      echo "ERROR: el despliegue no estuvo listo en ${READY_TIMEOUT_SECONDS} s." >&2
      diagnose
      exit 1
    fi
    sleep "$READY_INTERVAL_SECONDS"
  done

  local domain
  domain="$(env_get DOMAIN)"
  if [ -n "$domain" ]; then
    info "despliegue listo: https://$domain (hasta que se emita el certificado, http://$domain)"
  else
    info "despliegue listo: http://<IP pública de la EC2>/"
  fi
}

main() {
  validate_env
  check_corpus
  ensure_packages curl ca-certificates postgresql-client
  check_rds
  ensure_swap
  install_docker
  ensure_docker_group
  ensure_model
  start_stack
  wait_ready
}

main "$@"
