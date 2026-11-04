# Plan: Entorno para el Requerimiento 5: Despliegue en AWS (EC2 + RDS)

## 1. Metadata
- Spec: `docs/specs/005-despliegue-aws/spec.md`
- Updated: 2026-11-03
- Constitution version: 1.2.0

## 2. Constitution check
- P2: nginx, certbot, Docker y Ollama son infraestructura auxiliar; no se agrega ninguna librería de Java ni de web, y ningún servicio nuevo implementa un algoritmo.
- P3: `ollama` solo genera vectores (`nomic-embed-text:v1.5`) y SPECTER2 sigue en ONNX Runtime dentro de `api`; la similitud no cambia.
- P4: `docs/despliegue-aws.md` declara que los archivos de despliegue se generaron con IA generativa como apoyo.
- P5: el corpus desplegado son los 20 PDF del docente copiados a `./corpus`; el despliegue no agrega ni transforma artículos.
- P9: `docs/traceability.md` agrega las filas del Requerimiento 5 con `docs/documento-tecnico.md` § Requerimiento 5: Despliegue `(pendiente)`; el documento técnico y `docs/guia-implementacion.md` no se tocan (la guía describe qué implementa el estudiante en cada algoritmo; el despliegue no tiene algoritmos ni stubs).
- P11: el despliegue no cambia las mediciones; el runbook advierte que JMH en una vCPU compartida es ruidoso y que los números del documento técnico se miden en hardware controlado registrándolo.
- P12: ninguna imagen usa `latest` ni etiqueta omitida: `ollama/ollama:0.12.6` (la de `compose.dev.yaml`), `certbot/certbot:v5.1.0` y `nginx:1.29.3-alpine` (hoy `web/Dockerfile` usa `nginx:alpine`, sin versión); la tarea de cada fase verifica que la etiqueta existe en el registro antes de fijarla. El modelo de Ollama sigue fijado en `nomic-embed-text:v1.5`.
- P13: la infraestructura vive en `compose.prod.yaml`, `.env.prod.example`, `web/` (nginx), `scripts/` y `docs/`; no se edita `src/main/java`, ninguna migración ni `web/src` (REQ-30).

## 3. Architecture
```
Internet :80/:443 -> web (nginx) -- /api/ --> api:8080 -- JDBC TLS --> RDS :5432 (fuera de Compose)
                        |                        \-- HTTP --> ollama:11434
                        |-- /.well-known/acme-challenge/ <- volumen certbot-webroot (rw en certbot)
                        \-- /etc/letsencrypt (ro)        <- volumen letsencrypt (rw en certbot)
```
- `compose.prod.yaml` (nombre de proyecto fijo `bibliomtr`) define `ollama`, `api`, `web` y `certbot`; no incluye `compose.yaml` ni `compose.dev.yaml` y no define `db`. Solo `web` publica puertos (`80:80` y `443:443`). Volúmenes nombrados: `ollama-data` (`/root/.ollama`), `letsencrypt` y `certbot-webroot`.
- Orden de arranque: `ollama` saludable → `api` (`service_healthy`) → `web` (`service_healthy`) → `certbot` (`web` saludable). Con `ollama` no saludable, `api` y `web` no arrancan (AC-7).
- La configuración sale de `.env` (interpolación de Compose, sin `env_file` para no darle la contraseña a `web`). `api` recibe `SPRING_DATASOURCE_*`, `BIBLIOMTR_CORPUS_DIR`, `BIBLIOMTR_OLLAMA_URL` y `JAVA_TOOL_OPTIONS`; `web` y `certbot` reciben `DOMAIN`; `certbot` recibe además `CERTBOT_EMAIL` y `CERTBOT_STAGING`.
- Modos de nginx en `web` (imagen oficial, que aplica `envsubst` a `/etc/nginx/templates/*.template` hacia `/etc/nginx/conf.d/` en el arranque):
  - Modo HTTP: `DOMAIN` vacío, o con `DOMAIN` y sin certificado. Un `server` en el puerto 80 con `/healthz`, el desafío ACME, el proxy `/api/` y el fallback de SPA. No hay `server` en el 443, por lo que el 443 publicado no tiene quien escuche (AC-12).
  - Modo HTTPS: `DOMAIN` definido y existen `fullchain.pem` y `privkey.pem` en `/etc/letsencrypt/live/<DOMAIN>/`. El puerto 80 sirve `/healthz` y el desafío ACME y responde 301 hacia `https://<DOMAIN>` para lo demás; el 443 sirve la aplicación con TLS 1.2 o 1.3.
  - `15-select-nginx-mode.sh` (en `/docker-entrypoint.d/`, antes del `20-envsubst-on-templates.sh` oficial) copia la plantilla del modo elegido a `/etc/nginx/templates/default.conf.template`. El filtro `NGINX_ENVSUBST_FILTER` limita `envsubst` a `DOMAIN` para no tocar las variables de nginx (`$host`, `$request_uri`); la imagen fija `DOMAIN=""` para que la variable siempre exista.
  - `40-watch-certificate.sh` lanza en segundo plano un bucle (cada 60 s) que, si `DOMAIN` está definido, calcula el modo y el `cksum` del certificado; si cambian, repite selección y `envsubst`, valida con `nginx -t` (si falla, restaura el archivo anterior y registra el error) y ejecuta `nginx -s reload`. Así la emisión inicial (HTTP → HTTPS) y la renovación se aplican sin detener ni reiniciar `web` (AC-11).
- Emisión y renovación en `certbot` (`scripts/certbot-loop.sh`, montado de solo lectura): con `DOMAIN` vacío registra el motivo y no ejecuta `certbot` (AC-12, REQ-13). Con `DOMAIN` definido, en un ciclo de 60 s: si no existe el linaje, `certbot certonly --webroot -w /var/www/certbot -d "$DOMAIN" --email "$CERTBOT_EMAIL" --agree-tos --no-eff-email --non-interactive --keep-until-expiring` y, ante fallo, reintenta a los 30 min (4 intentos por hora, bajo el límite de 5 validaciones fallidas por hora de Let's Encrypt); si ya existe, `certbot renew --webroot -w /var/www/certbot` cada 12 h (2 intentos por día). `CERTBOT_STAGING=1` agrega `--staging`. Cada ciclo toca `/tmp/certbot-alive` para el `healthcheck`.
- Bootstrap (`scripts/bootstrap-ec2.sh`), en este orden: validar `.env` → paquetes ligeros (`curl`, `ca-certificates`, `postgresql-client`) → verificar RDS → swap → Docker → grupo `docker` → `python3`, `python3-venv` → modelo ONNX → `./corpus` → `docker compose -f compose.prod.yaml up -d --build` → espera de "listo". Los fallos de `.env` y RDS ocurren antes de instalar Docker y antes de descargar el modelo (AC-19, AC-20).
- Verificación de "listo" (REQ-20): cada 10 s, hasta 1 200 s, ejecuta desde el contenedor `web` (misma red, sin depender del modo HTTP/HTTPS): `wget -qO- http://api:8080/api/actuator/health` contiene `"status":"UP"`; `wget -qO- http://ollama:11434/api/tags` contiene `nomic-embed-text:v1.5`; `wget -qO- http://api:8080/api/articles` tiene 20 apariciones de `"fileName"`. Si `up -d` falla porque una dependencia no queda saludable, o se agota el tiempo, el script muestra las últimas 50 líneas del primer servicio que no está saludable (`api` si todos lo están), el comando `docker compose -f compose.prod.yaml logs` y sale con 1.

## 4. Design patterns
- Plantillas de nginx con `envsubst` en `web/`: una misma imagen sirve el desarrollo (puerto 5173, sin dominio) y producción (HTTP o HTTPS según el dominio y el certificado), sin duplicar `nginx.conf`.
- Snippet compartido `nginx-locations.conf` incluido por ambos modos: el proxy de `/api/` y el fallback de SPA existen una sola vez.
- Bucle de reconciliación (comparar estado deseado con el real cada 60 s) en `40-watch-certificate.sh` y `certbot-loop.sh`: evita que `certbot` necesite el socket de Docker para recargar nginx.
- Script idempotente por verificación previa ("comprobar y luego actuar") en `bootstrap-ec2.sh`: cada paso detecta si ya está hecho.

## 5. Code structure
- create: `compose.prod.yaml` - stack de producción (`ollama`, `api`, `web`, `certbot`, volúmenes, límites, salud, dependencias).
- create: `.env.prod.example` - plantilla sin secretos con las siete variables mínimas más `CERTBOT_STAGING`.
- modify: `.gitignore` - `.env` ignorado y `!.env.prod.example` explícito.
- modify: `.dockerignore` - agrega `.env` para que el contexto de build de `api` no lo envíe al daemon.
- modify: `web/Dockerfile` - `FROM nginx:1.29.3-alpine`; copia plantillas, snippet y scripts de entrada con `--chmod=755`; `ENV DOMAIN=""` y `NGINX_ENVSUBST_FILTER`; crea `/var/www/certbot`.
- modify: `web/nginx.conf` - modo HTTP: `listen 80`, `/healthz`, desafío ACME, `include` del snippet.
- create: `web/nginx-https.conf` - modo HTTPS (plantilla con `${DOMAIN}`): redirección 301 en el 80 y `server` 443 con TLS.
- create: `web/nginx-locations.conf` - `/api/` hacia `api:8080` con `Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, y fallback de SPA; sin `proxy_read_timeout`.
- create: `web/docker-entrypoint.d/15-select-nginx-mode.sh`, `web/docker-entrypoint.d/40-watch-certificate.sh` - selección de modo y recarga.
- create: `scripts/certbot-loop.sh` - emisión inicial y renovación.
- create: `scripts/check-nginx.sh` - `nginx -t` en contenedor en modo HTTP y HTTPS (certificado autofirmado temporal de `openssl`, `--add-host api:127.0.0.1`).
- create: `scripts/bootstrap-ec2.sh` - bootstrap idempotente de Ubuntu 26.04.
- create: `docs/despliegue-aws.md` - runbook.
- modify: `README.md` - sección "Despliegue en AWS (Requerimiento 5)" con enlace al runbook.
- modify: `docs/traceability.md` - filas del Requerimiento 5.
- create: `src/test/java/co/edu/uniquindio/bibliomtr/deployment/DeploymentInvariantsTest.java` - invariantes de los archivos (REQ-27).
- create: `src/test/java/co/edu/uniquindio/bibliomtr/docs/DeploymentRunbookTest.java` - runbook y trazabilidad.
- modify (corrección posterior al despliegue): `Dockerfile` raíz - copia `models/specter2-base/` a `/app/models/` y fija `BIBLIOMTR_EMBEDDINGS_MODELPATH` y `BIBLIOMTR_EMBEDDINGS_TOKENIZERPATH` con `file:` hacia esa carpeta.
- no se tocan: `compose.yaml`, `compose.dev.yaml`, `src/main/java`, migraciones, `web/src`, `docs/guia-implementacion.md`, `docs/documento-tecnico.md`, `web/src/features/clustering/ClusteringSection.tsx`, `web/pnpm-lock.yaml`.

## 6. Interfaces and data
`.env.prod.example` (valores evidentes de ejemplo):
```
SPRING_DATASOURCE_URL=jdbc:postgresql://CAMBIAR_ENDPOINT_RDS:5432/bibliomtr?sslmode=require
SPRING_DATASOURCE_USERNAME=CAMBIAR_USUARIO_MAESTRO
SPRING_DATASOURCE_PASSWORD=CAMBIAR_CONTRASENA
BIBLIOMTR_CORPUS_DIR=/data/corpus
BIBLIOMTR_OLLAMA_URL=http://ollama:11434
DOMAIN=tu-dominio.example
CERTBOT_EMAIL=CAMBIAR_correo@tu-dominio.example
CERTBOT_STAGING=0
```
- `DOMAIN` vacío = solo HTTP. El bootstrap trata como pendiente todo valor que empiece con `CAMBIAR_` y el dominio `tu-dominio.example`; `CERTBOT_EMAIL` solo es obligatorio con `DOMAIN` definido; `CERTBOT_STAGING` es opcional.

Servicios de `compose.prod.yaml`:

| Servicio | Imagen / build | `mem_limit` | Salud | Depende de |
|---|---|---|---|---|
| `ollama` | `ollama/ollama:0.12.6`; volumen `ollama-data:/root/.ollama`; `OLLAMA_NUM_PARALLEL=1`, `OLLAMA_MAX_LOADED_MODELS=1` | 1024m | `ollama list` (intervalo 15 s, `start_period` 20 s, 10 reintentos) | — |
| `api` | build `.` (`Dockerfile` raíz); `./corpus:${BIBLIOMTR_CORPUS_DIR:-/data/corpus}:ro`; `JAVA_TOOL_OPTIONS=-Xmx768m -XX:+ExitOnOutOfMemoryError` | 2048m | `bash -c` con `/dev/tcp/127.0.0.1/8080` y `GET /api/actuator/health`, exige `"status":"UP"` (no depende de `curl`; `start_period` 120 s) | `ollama` `service_healthy` |
| `web` | build `./web`; `80:80`, `443:443`; `letsencrypt:/etc/letsencrypt:ro`, `certbot-webroot:/var/www/certbot:ro` | 128m | `wget -q --spider http://127.0.0.1/healthz` | `api` `service_healthy` |
| `certbot` | `certbot/certbot:v5.1.0`; entrypoint `/bin/sh /scripts/certbot-loop.sh`; `letsencrypt` y `certbot-webroot` en escritura | 128m | archivo `/tmp/certbot-alive` modificado hace menos de 3 minutos (`find -mmin -3`) | `web` `service_healthy` |

- Los cuatro tienen `restart: unless-stopped`. Suma de `mem_limit` = 2048 + 1024 + 128 + 128 = 3 328 MiB ≤ 3 584 MiB (REQ-7); quedan 768 MiB de los 4 096 para el sistema, el motor de contenedores y 4 GB de swap.
- `JAVA_TOOL_OPTIONS` lo heredan por entorno el proceso hijo `java -jar benchmarks.jar` que lanza `JmhProcessLauncher` (`ProcessBuilder` sin cambios de entorno) y el fork de JMH (`-f 1`): hasta tres JVM con heap ≤ 768 MiB durante una medición; por eso el runbook prohíbe medir durante la ingesta y concurrentemente.
- Las `healthcheck` de `ollama`, `api` y `web` no usan `$`, así que Compose no necesita `$$`.

`web/nginx.conf` (modo HTTP) y `web/nginx-https.conf` (plantilla):
```
# nginx.conf
server { listen 80; root /usr/share/nginx/html;
  location = /healthz { access_log off; return 200 'ok'; }
  location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
  include /etc/nginx/snippets/app-locations.conf; }
# nginx-https.conf
server { listen 80; (healthz, ACME) location / { return 301 https://${DOMAIN}$request_uri; } }
server { listen 443 ssl; http2 on; server_name ${DOMAIN}; root ...;
  ssl_certificate /etc/letsencrypt/live/${DOMAIN}/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/${DOMAIN}/privkey.pem;
  ssl_protocols TLSv1.2 TLSv1.3; include /etc/nginx/snippets/app-locations.conf; }
```
Imagen `web`: `/etc/nginx/templates-available/http.conf.template` (de `nginx.conf`), `.../https.conf.template` (de `nginx-https.conf`), `/etc/nginx/snippets/app-locations.conf`, `/var/www/certbot`.

`scripts/bootstrap-ec2.sh` (se ejecuta como usuario normal con `sudo`; con `EUID` 0 termina con 1 y un mensaje):
- Códigos de salida: 0 = listo; 1 = fallo con mensaje en español que dice qué corregir.
- `.env` (antes de cualquier instalación): si no existe, copia `.env.prod.example` a `.env` con permisos 600, informa y sale con 1. Si existe, lee los valores con una función `env_get KEY` (`grep` + `sed`, sin `source`, porque la contraseña puede tener caracteres especiales) y exige las cinco variables de `api` no vacías y sin `CAMBIAR_`; con `DOMAIN` definido exige `CERTBOT_EMAIL` válido. Lista todas las que faltan y sale con 1. Nunca imprime la contraseña ni reemplaza el `.env`.
- Corpus: `mkdir -p corpus`; si no hay 20 archivos `corpus/*.pdf`, sale con 1 pidiendo copiarlos (la espera de 20 minutos no puede cumplirse sin ellos).
- RDS: extrae servidor, puerto y base de `SPRING_DATASOURCE_URL` con `sed`; `pg_isready -h … -p … -t 10` (si falla: mensaje que apunta al grupo de seguridad de RDS —5432 desde el grupo de la EC2— y al endpoint, AC-20); luego `psql` con `PGPASSWORD` y `sslmode=require` consulta `pg_extension` y `pg_available_extensions` por `vector`; sin la extensión instalada ni disponible sale con 1 explicando que Flyway `V1__enable_pgvector.sql` la crea y que RDS debe ofrecerla. El script no ejecuta `CREATE EXTENSION`.
- Swap: si `/swapfile` ya está activo no hace nada; si no, `fallocate -l 4G /swapfile` (si no existe), `chmod 600`, `mkswap`, `swapon`, y agrega `/swapfile none swap sw 0 0` a `/etc/fstab` solo si `grep -Eq '^/swapfile[[:space:]]'` falla.
- Docker: si `docker compose version` funciona, no instala. Si no, lee `VERSION_CODENAME` de `/etc/os-release` y comprueba en ejecución con `curl -fsI` que existen `https://download.docker.com/linux/ubuntu/dists/<codename>/Release` y `…/stable/binary-<arch>/Packages`; si existen, escribe `/etc/apt/sources.list.d/docker.sources` con la clave en `/etc/apt/keyrings/docker.asc`, ejecuta `apt-get update` y exige que `apt-cache policy docker-ce` tenga candidato distinto de `(none)` antes de instalar `docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin`. Si cualquier comprobación falla, elimina esa fuente, informa "repositorio de Docker CE sin paquetes para <codename>; se usa docker.io de Ubuntu" e instala `docker.io docker-compose-v2` (exige candidato de `docker-compose-v2`; si no existe, sale con 1). No asume que el repositorio ni los paquetes existan para Ubuntu 26.04.
- Usuario: `usermod -aG docker "$USER"` solo si `id -nG` no lo incluye. Los comandos de Compose del propio script usan `sudo` cuando el grupo aún no se aplicó a la sesión.
- Modelo: ejecuta `scripts/download-model.sh` solo si faltan `src/main/resources/models/specter2-base/model.onnx` o `tokenizer.json`.
- Arranque: `docker compose -f compose.prod.yaml up -d --build`; no usa `down -v`, `docker push` ni registro.

Verificación estática (REQ-26), todas sin AWS:
- `docker compose --env-file .env.prod.example -f compose.prod.yaml config`
- `bash -n scripts/bootstrap-ec2.sh scripts/certbot-loop.sh scripts/check-nginx.sh web/docker-entrypoint.d/*.sh` y `shellcheck` sobre los mismos archivos si está instalado.
- `scripts/check-nginx.sh`: construye `web`, genera un certificado autofirmado temporal para un dominio de prueba (`prueba.example`), y para cada modo ejecuta `docker run --rm --add-host api:127.0.0.1 [-e DOMAIN=… -v <cert>:/etc/letsencrypt:ro] bibliomtr-web-check nginx -t` tras correr los dos scripts de entrada.
- `./gradlew test --tests DeploymentInvariantsTest --tests DeploymentRunbookTest`.

## 7. Test-first
- No aplica: `AGENTS.md` no define test-first. Los tests por AC están en la tabla de § 8.

## 8. Traceability
| Requirement | Component / file | Verified by (AC) |
|---|---|---|
| REQ-1 | `compose.prod.yaml` (sin `include`, `extends` ni `db`) | AC-1 |
| REQ-2 | `compose.prod.yaml` (`web`, `api`, `ollama`, `certbot`; `build`) | AC-1 |
| REQ-3 | `compose.prod.yaml`, `web/Dockerfile` (`FROM`) | AC-3 |
| REQ-4 | `compose.prod.yaml` (`ports` solo en `web`, red interna, `BIBLIOMTR_OLLAMA_URL`) | AC-2 |
| REQ-5 | `compose.prod.yaml` (`restart`, `healthcheck`) | AC-4 |
| REQ-6 | `compose.prod.yaml` (`depends_on`) | AC-7 |
| REQ-7 | `compose.prod.yaml` (`mem_limit`, `JAVA_TOOL_OPTIONS`); `docs/despliegue-aws.md` | AC-4, AC-5, AC-22 |
| REQ-8 | `compose.prod.yaml` (`ollama-data`) | AC-6 |
| REQ-9 | `web/nginx.conf`, `web/nginx-https.conf`, `web/nginx-locations.conf` | AC-13, AC-14 |
| REQ-10 | `scripts/certbot-loop.sh`, volúmenes `letsencrypt` y `certbot-webroot` | AC-9, AC-10 |
| REQ-11 | `web/nginx.conf`, `web/nginx-https.conf`, `web/docker-entrypoint.d/15-select-nginx-mode.sh` | AC-8, AC-9, AC-10 |
| REQ-12 | `scripts/certbot-loop.sh`, `web/docker-entrypoint.d/40-watch-certificate.sh` | AC-11 |
| REQ-13 | `scripts/certbot-loop.sh`, `web/docker-entrypoint.d/15-select-nginx-mode.sh` | AC-12 |
| REQ-14 | `web/Dockerfile`, `web/nginx.conf` | AC-15 |
| REQ-15 | `scripts/bootstrap-ec2.sh` (Docker) | AC-16, AC-18 |
| REQ-16 | `scripts/bootstrap-ec2.sh` (swap, grupo, paquetes, `./corpus`) | AC-16 |
| REQ-17 | `scripts/bootstrap-ec2.sh` (modelo, `up -d --build`) | AC-16, AC-17 |
| REQ-18 | `scripts/bootstrap-ec2.sh` (comprobar y luego actuar) | AC-17 |
| REQ-19 | `scripts/bootstrap-ec2.sh` (`.env`, RDS, `vector`) | AC-19, AC-20 |
| REQ-20 | `scripts/bootstrap-ec2.sh` (espera de listo) | AC-16, AC-21 |
| REQ-21 | `.env.prod.example`, `.gitignore` | AC-24 |
| REQ-22 | todos los archivos de despliegue; `DeploymentInvariantsTest` | AC-23 |
| REQ-23 | `docs/despliegue-aws.md`, `README.md` | AC-25 |
| REQ-24 | `docs/despliegue-aws.md` | AC-25 |
| REQ-25 | `docs/despliegue-aws.md` | AC-26 |
| REQ-26 | `docs/despliegue-aws.md`, `scripts/check-nginx.sh` | AC-8, AC-27 |
| REQ-27 | `DeploymentInvariantsTest` | AC-27 |
| REQ-28 | `docs/traceability.md`; `DeploymentRunbookTest` | AC-28 |
| REQ-29 | `docs/despliegue-aws.md` | AC-30 |
| REQ-30 | `git diff` de la rama | AC-29 |

Tests por AC (la columna "manual" se ejecuta en la EC2 siguiendo el runbook; el repositorio no ejecuta nada contra AWS):

| AC | Verificación | Comprobación |
|---|---|---|
| AC-1 | `docker compose --env-file .env.prod.example -f compose.prod.yaml config`; `DeploymentInvariantsTest` | sin errores; la salida no tiene servicio `db` ni `compose.dev.yaml`; el archivo no contiene `include:` ni `extends:` |
| AC-2 | `config`; `DeploymentInvariantsTest` | solo `web` tiene `ports`, con 80 y 443 |
| AC-3 | `DeploymentInvariantsTest` | cada `image:` tiene etiqueta distinta de `latest` y con versión; la de `ollama` es igual a la de `compose.dev.yaml`; el `FROM nginx` de `web/Dockerfile` tiene versión fija |
| AC-4 | `DeploymentInvariantsTest` | cada servicio tiene `restart: unless-stopped`, `healthcheck` y `mem_limit`; la suma de `mem_limit` ≤ 3 584 MiB |
| AC-5 | `DeploymentInvariantsTest`; manual `docker compose -f compose.prod.yaml exec api printenv JAVA_TOOL_OPTIONS` | `api` define `JAVA_TOOL_OPTIONS` con `-Xmx` ≤ 1 GiB; el hijo JMH hereda el entorno |
| AC-6 | `DeploymentInvariantsTest`; manual `up -d --force-recreate ollama` y `GET /api/tags` | `ollama` monta un volumen nombrado en `/root/.ollama` y el modelo sigue en `/api/tags` |
| AC-7 | `DeploymentInvariantsTest` | `api` depende de `ollama` y `web` de `api`, ambos con `service_healthy` |
| AC-8 | `scripts/check-nginx.sh` | `nginx -t` correcto en los dos modos con la versión fijada en `web/Dockerfile` |
| AC-9 | `DeploymentInvariantsTest`; contenedor `web` con `DOMAIN=prueba.example` sin certificado; manual | `nginx.conf` tiene la ubicación ACME; la imagen responde `/` y `/healthz` por HTTP |
| AC-10 | `DeploymentInvariantsTest`; manual `curl -I http://<dominio>/` y `curl -I https://<dominio>/` | la plantilla HTTPS tiene `return 301` y `ssl_protocols TLSv1.2 TLSv1.3` sin `TLSv1 ` ni `TLSv1.1`; en la EC2, 301 y 200 |
| AC-11 | `DeploymentInvariantsTest`; manual `certbot renew --force-renewal` | el vigilante usa `nginx -s reload` y ningún archivo reinicia `web`; en la EC2, la conexión activa sigue |
| AC-12 | contenedor `web` con `DOMAIN` vacío; `DeploymentInvariantsTest` | el 443 del contenedor no escucha (`wget` a `https://127.0.0.1` falla) y `/` responde por HTTP; `certbot-loop.sh` sale del flujo de emisión con `DOMAIN` vacío |
| AC-13 | contenedor `web` con la API simulada | `GET /pattern-search` devuelve 200 con `index.html`; `/api/…` llega al upstream |
| AC-14 | `DeploymentInvariantsTest`; manual | ningún archivo `web/nginx*.conf` contiene `proxy_read_timeout`; `POST /api/benchmarks` responde 202 |
| AC-15 | `docker compose up --build` de `compose.yaml` | `http://localhost:5173/` sirve la aplicación y `/api/` llega a `api` |
| AC-16 | manual en Ubuntu 26.04; `bash -n`, `shellcheck` | salida 0, `docker compose version`, `swapon --show` de 4 GB, `id -nG` con `docker`, tres condiciones de listo |
| AC-17 | manual (segunda ejecución); `DeploymentInvariantsTest` | salida 0; una sola línea `/swapfile` en `/etc/fstab`; `.env` y modelo intactos; el script no contiene `down -v` |
| AC-18 | manual; `DeploymentInvariantsTest` | el script contiene `download.docker.com`, la comprobación `curl -fsI`, `docker.io` y `docker-compose-v2` |
| AC-19 | ejecución local del script copiado a un directorio temporal sin `.env`, y con `.env` incompleto | salida distinta de 0 antes de `apt-get`, `.env` creado desde la plantilla y variables listadas |
| AC-20 | manual con el grupo de seguridad de RDS cerrado; `bash -n` | salida distinta de 0 y mensaje con grupo de seguridad y endpoint |
| AC-21 | manual (p. ej. `READY_TIMEOUT_SECONDS=30`); `DeploymentInvariantsTest` | salida distinta de 0, 50 líneas de registro y el comando `logs`; el valor por defecto es 1 200 |
| AC-22 | manual `docker inspect --format '{{.State.OOMKilled}}'`; `DeploymentRunbookTest` | el runbook documenta `OOMKilled`, `swapon --show` y que JMH en la EC2 es de demostración |
| AC-23 | `DeploymentInvariantsTest` | los archivos de despliegue no contienen `AKIA[0-9A-Z]{16}`, `.rds.amazonaws.com`, contraseñas distintas de `CAMBIAR_…` ni dominios o correos reales |
| AC-24 | `DeploymentInvariantsTest`; `git check-ignore -v .env .env.prod.example` | las ocho claves con valores de ejemplo; `.env` ignorado y `.env.prod.example` no |
| AC-25 | `DeploymentRunbookTest` | el runbook cubre región us-east-2, grupos de seguridad, RDS `db.t4g.micro`, EC2 `c7i-flex.large`, gp3 de 30 GB, Elastic IP y registro A; ningún bloque de código empieza con `aws ` |
| AC-26 | `DeploymentRunbookTest` | una sección con causa y corrección por cada fallo de REQ-25 (grupo de seguridad, `vector`, DNS, límite de Let's Encrypt y `--staging`, memoria, descarga lenta) |
| AC-27 | todas las validaciones estáticas de § 6 y `./gradlew test` | verdes sin red hacia AWS |
| AC-28 | `DeploymentRunbookTest` | filas con "Requerimiento 5", spec 005, `— (infraestructura:` y `(pendiente)` |
| AC-29 | `git diff --name-only main...HEAD` | sin `ClusteringSection.tsx`, `pnpm-lock.yaml`, `documento-tecnico.md`, `src/main/java` ni migraciones |
| AC-30 | `DeploymentRunbookTest` | el runbook contiene "JMH", "vCPU compartida", "hardware controlado" y el registro del hardware |

Comandos: `docker compose --env-file .env.prod.example -f compose.prod.yaml config`, `scripts/check-nginx.sh`, `bash -n`, `shellcheck`, `./gradlew test`.

## 9. Decisions and risks
- nginx en contenedor con la imagen oficial y `/etc/nginx/templates`: se reutiliza el `web/Dockerfile` y `web/nginx.conf` existentes (REQ-9). Se rechazó nginx en el host (no sería reproducible ni lo cubriría el `compose.yaml` de desarrollo) y un segundo contenedor proxy delante de `web` (otro servicio y otro límite de memoria sin necesidad).
- Etiqueta de nginx: `web/Dockerfile` usaba `nginx:alpine` (flotante). Se fija `nginx:1.29.3-alpine` (REQ-3, P12); el build de desarrollo no cambia porque usa el mismo Dockerfile.
- Modo HTTP/HTTPS por plantillas y no por un único archivo con condiciones: nginx no tiene condicionales de nivel `server`, y un `listen 443 ssl` sin certificado impide arrancar nginx. Se rechazó emitir el certificado con un nginx temporal (`certbot --standalone` con la 80 libre): duplica el arranque y rompe la regla de que el stack sirve HTTP desde el primer momento (REQ-11).
- certbot en contenedor con `certbot/certbot:v5.1.0`, HTTP-01 por webroot y volúmenes nombrados: el desafío lo sirve nginx sin detenerse. Se rechazó `--standalone` (necesita el puerto 80 libre) y certbot en el host (no queda en Compose ni en el bootstrap idempotente).
- Emisión inicial dentro del bucle de `certbot`, sin servicio de un solo disparo ni paso en el bootstrap: un único lugar que también funciona si el operador arranca con `docker compose … up -d` sin bootstrap, y el bucle reintenta tras corregir el DNS. Se rechazó un servicio `certbot-init` con `restart: "no"` porque obliga a ordenar dos servicios y a ejecutarlo a mano si falla.
- Recarga sin reinicio por vigilancia en `web` (comparar `cksum`) y no por el socket de Docker: montar `/var/run/docker.sock` en `certbot` da control de root sobre el host. La latencia máxima de recarga es de 60 s.
- Renovación cada 12 h: `certbot renew` solo renueva a menos de 30 días del vencimiento, así que 2 intentos diarios (recomendación de Let's Encrypt) no generan emisiones de más.
- Puerto 443: `web` lo publica siempre (`443:443`, REQ-4) y solo escucha en modo HTTPS, o sea con `DOMAIN` definido y certificado emitido (AC-12 admite "no escucha"). Se rechazó el perfil de Compose porque habría que repetir `web` con y sin 443, y porque `COMPOSE_PROFILES` obligaría a mantener `DOMAIN` y el perfil sincronizados; Compose tampoco permite quitar un `ports` por interpolación. Consecuencia: con `DOMAIN` vacío el motor de contenedores puede aceptar la conexión en el 443 y cerrarla; nginx no responde nada en ese puerto.
- Perfil `tls` descartado también para `certbot`: el servicio siempre arranca y, con `DOMAIN` vacío, no ejecuta `certbot` (queda inactivo con un límite de 128m). A cambio, `docker compose … up -d` es el mismo comando en ambos modos.
- Memoria: `api` 2048m en vez de los 1792m propuestos, porque en una medición coexisten el heap de la API, la memoria nativa de ONNX Runtime (SPECTER2 base, unos 440 MB de pesos), el metaspace y, durante JMH, el lanzador y el fork con `-Xmx768m` cada uno. `-Xmx768m` es el mínimo del rango 768m–1g: acota las tres JVM. `ollama` 1024m (modelo de 274 MB con un solo modelo y una sola solicitud en paralelo). `web` 128m. `certbot` 128m y no 64m, porque Python más el cliente ACME supera 64 MiB al emitir. Total 3 328 MiB ≤ 3 584 MiB, con 4 GB de swap. `-XX:+ExitOnOutOfMemoryError` hace que un OOM de la JVM reinicie `api` por `restart` en vez de dejarla degradada.
- Modelo fuera del jar (corrección del 2026-11-04, tras el primer despliegue): dentro de `app.jar` el `model.onnx` no es un archivo, `Specter2OnnxEmbedder` lo leía con `getContentAsByteArray()` y el pico en el heap (unos 2 × 437 MB) superaba `-Xmx768m`, así que `api` terminaba con `OutOfMemoryError: Java heap space` y no llegaba a saludable. El `Dockerfile` raíz copia el modelo a `/app/models/` y apunta las propiedades con `file:`; ONNX Runtime lo carga en memoria nativa, dentro de los 2048m de `api`. Se rechazó subir `-Xmx` (reduce el margen de ONNX Runtime y de las JVM de JMH, REQ-7) y cambiar `Specter2OnnxEmbedder` (código de la spec 001).
- Salud de `api` con `bash` y `/dev/tcp`: la imagen `eclipse-temurin:25-jre-noble` no garantiza `curl` ni `wget`, y agregar paquetes obligaría a editar el `Dockerfile` raíz. Una tarea verifica el comando sobre la imagen construida si Docker está disponible.
- Salud de `ollama` con `ollama list`: usa el binario incluido en la imagen y consulta el servidor local.
- Espera de "listo" desde `web` con `wget`: el chequeo no depende de que el 80 sirva la aplicación (en modo HTTPS responde 301) ni de publicar `api`.
- Ubuntu 26.04 LTS: el repositorio apt de Docker CE no se da por existente. El script lo comprueba al ejecutarse (codename de `/etc/os-release`, `HEAD` a `Release` y al índice `Packages`, y candidato de `docker-ce` tras `apt-get update`); si falla usa `docker.io` y `docker-compose-v2` de Ubuntu, exigiendo que ese paquete tenga candidato. Se rechazó fijar el codename `noble` como sustituto: sería instalar paquetes de otra versión.
- `.env` leído con `grep`/`sed` y no con `source`: una contraseña con `$`, comillas o espacios rompería o ejecutaría código del archivo.
- `.env` en `.dockerignore`: el contexto de build de `api` incluía todo el árbol; sin esto el archivo con la contraseña se enviaría al daemon.
- Validación con `--env-file .env.prod.example`: `config` funciona sin un `.env` real ni secretos (AC-1).
- Risk: las versiones `nginx:1.29.3-alpine` y `certbot/certbot:v5.1.0` pueden no existir en el registro - la tarea de cada fase ejecuta `docker manifest inspect` (o consulta Docker Hub) y ajusta la etiqueta en `web/Dockerfile` y `compose.prod.yaml` antes de commitear.
- Risk: no hay Docker en la máquina de desarrollo - las tareas indican qué validaciones se omiten (`config`, `check-nginx.sh`, `docker build`), las dejan registradas en el mensaje de cierre de la fase y las repite el operador en la EC2; `DeploymentInvariantsTest` y `bash -n` no necesitan Docker.
- Risk: el build de `api` y de `web` en una EC2 de 4 GB (Gradle y npm) agota la memoria - el swap de 4 GB se crea antes del build y el runbook lo indica; la construcción no corre dentro de un contenedor con `mem_limit`.
- Risk: la descarga de SPECTER2 (`download-model.sh`, unos 2 GB de disco y PyTorch) tarda y usa memoria - corre después del swap y solo si el modelo falta; EBS de 30 GB lo cubre.
- Risk: la emisión de Let's Encrypt falla con DNS sin propagar - el bucle reintenta a los 30 min y el runbook describe la espera, `CERTBOT_STAGING=1` y qué hacer ante el límite de emisión.
- Risk: de staging a producción el linaje de staging queda en el volumen - el runbook indica borrarlo con `docker compose -f compose.prod.yaml run --rm --entrypoint certbot certbot delete --cert-name <dominio>` antes de cambiar `CERTBOT_STAGING` a 0.
- Risk: `sslmode=require` cifra la conexión a RDS pero no verifica el certificado del servidor - se deja como lo pide REQ-21; el runbook menciona `verify-full` con el paquete de certificados de RDS como endurecimiento fuera del alcance.
- Risk: la SnakeYAML que usa `DeploymentInvariantsTest` llega transitivamente por `spring-boot-starter-webmvc` - si no está en el classpath de test, la tarea lo avisa y el test lee el YAML con un lector de líneas.
- Risk: Gradle no registra los archivos de despliegue como entradas de `test` - tras editar solo ellos se ejecuta `./gradlew test --rerun`.
