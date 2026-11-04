# Despliegue en AWS (Requerimiento 5)

Runbook para publicar bibliomtr en una sola EC2 con la base de datos en RDS. Cubre la spec `docs/specs/005-despliegue-aws/`.

## 1. Uso de IA generativa

Los archivos de despliegue (`compose.prod.yaml`, `.env.prod.example`, la configuración de nginx en `web/`, `scripts/bootstrap-ec2.sh`, `scripts/certbot-loop.sh`, `scripts/check-nginx.sh`, los tests de invariantes y este runbook) se generaron con IA generativa como apoyo y fueron revisados por el equipo (constitución P4). El despliegue no implementa ningún algoritmo del proyecto: nginx, certbot, Docker y Ollama son infraestructura auxiliar (P2).

## 2. Arquitectura

```
Internet :80/:443 -> web (nginx) -- /api/ --> api:8080 -- JDBC con TLS --> RDS :5432 (fuera de Compose)
                        |                        \-- HTTP --> ollama:11434
                        |-- /.well-known/acme-challenge/ <- volumen certbot-webroot
                        \-- /etc/letsencrypt (solo lectura) <- volumen letsencrypt
```

- `compose.prod.yaml` define cuatro servicios: `ollama`, `api`, `web` y `certbot`. No define `db`: PostgreSQL vive en RDS.
- Solo `web` publica puertos (80 y 443). `api` y `ollama` se comunican por la red interna de Compose.
- Orden de arranque: `ollama` saludable, luego `api`, luego `web` y por último `certbot`.
- Memoria (EC2 de 4 GB): `api` 2048 MiB, `ollama` 1024 MiB, `web` 128 MiB y `certbot` 128 MiB (suma 3 328 MiB) más 4 GB de swap en `/swapfile`. El heap de la JVM de `api` se acota con `-Xmx768m` vía `JAVA_TOOL_OPTIONS`, y el proceso hijo de JMH hereda ese límite.
- Datos persistentes en volúmenes nombrados: `ollama-data` (modelos de Ollama), `letsencrypt` (certificados) y `certbot-webroot` (desafío ACME).
- HTTPS: con `DOMAIN` definido, certbot emite el certificado de Let's Encrypt por HTTP-01 y renueva dos veces al día; nginx lo recarga sin reiniciar el contenedor. Con `DOMAIN` vacío el stack sirve solo por HTTP.

## 3. Crear la infraestructura en la consola de AWS (región us-east-2)

Todos los pasos se hacen a mano en la consola web de AWS, en la región us-east-2 (Ohio). El repositorio no contiene ni ejecuta comandos `aws`, Terraform ni CloudFormation: ningún paso de esta sección se automatiza.

### 3.1 Grupo de seguridad de la EC2

Cree un grupo de seguridad (por ejemplo `bibliomtr-ec2`) en la VPC predeterminada con estas reglas de entrada:

| Tipo | Puerto | Origen |
|---|---|---|
| HTTP | 80 | `0.0.0.0/0` |
| HTTPS | 443 | `0.0.0.0/0` |
| SSH | 22 | solo la IP pública del operador (`/32`) |

El puerto 80 debe estar abierto aunque use HTTPS: Let's Encrypt valida el dominio por HTTP.

### 3.2 Grupo de seguridad de RDS

Cree un segundo grupo (por ejemplo `bibliomtr-rds`) con una sola regla de entrada: PostgreSQL, puerto 5432, origen el grupo de seguridad `bibliomtr-ec2` (no una IP ni `0.0.0.0/0`).

### 3.3 Base de datos en RDS

Cree una base de datos RDS con estos valores:

- Motor: PostgreSQL, versión que ofrezca la extensión `vector` (pgvector).
- Plantilla: desarrollo/pruebas, implementación Single-AZ.
- Clase de instancia: `db.t4g.micro`.
- Usuario maestro y contraseña: elija el usuario y una contraseña propios; son las únicas credenciales de la base (no se crea un usuario dedicado).
- Nombre inicial de la base de datos: `bibliomtr`.
- Conectividad: acceso público desactivado (privada), misma VPC que la EC2, grupo de seguridad `bibliomtr-rds`.

Anote el endpoint de RDS (lo verá en la pestaña de conectividad de la base cuando esté disponible). La extensión `vector` la crea la migración Flyway `V1__enable_pgvector.sql` al arrancar la API; el bootstrap no ejecuta `CREATE EXTENSION`.

### 3.4 Instancia EC2

Cree una instancia con estos valores:

- Tipo: `c7i-flex.large` (2 vCPU, 4 GB de RAM).
- Imagen: Ubuntu 26.04 LTS.
- Almacenamiento: volumen EBS gp3 de 30 GB (el modelo SPECTER2 y PyTorch ocupan unos 2 GB y las imágenes de Docker varios más).
- Red: misma VPC que RDS, grupo de seguridad `bibliomtr-ec2`.
- Par de claves: el suyo, para entrar por SSH.

### 3.5 Elastic IP y registro DNS

1. Asigne una Elastic IP y asóciela a la instancia, para que la dirección no cambie al reiniciar.
2. En el proveedor de su dominio cree un registro DNS tipo A: `tu-dominio.example` apunta a la Elastic IP. Sin el registro A propagado, Let's Encrypt no podrá emitir el certificado.

## 4. Orden de operación

Todo se hace en la EC2, como usuario normal con `sudo` (el script rechaza ejecutarse como root).

1. Entre por SSH y clone el repositorio:

   ```bash
   git clone <url-del-repositorio> bibliomtr
   cd bibliomtr
   ```

2. Cree y complete `.env`. La primera ejecución del bootstrap lo crea desde la plantilla con permisos 600 y termina con código 1 para que lo complete; también puede hacerlo antes:

   ```bash
   cp .env.prod.example .env
   chmod 600 .env
   nano .env
   ```

   Reemplace todo valor que empiece con `CAMBIAR_`. Las variables son:

   | Variable | Qué poner |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<endpoint de RDS>:5432/bibliomtr?sslmode=require` |
   | `SPRING_DATASOURCE_USERNAME` | usuario maestro de RDS |
   | `SPRING_DATASOURCE_PASSWORD` | contraseña del usuario maestro |
   | `BIBLIOMTR_CORPUS_DIR` | `/data/corpus` (ruta dentro del contenedor `api`) |
   | `BIBLIOMTR_OLLAMA_URL` | `http://ollama:11434` |
   | `DOMAIN` | su dominio (vacío = solo HTTP) |
   | `CERTBOT_EMAIL` | correo para Let's Encrypt (obligatorio si `DOMAIN` está definido) |
   | `CERTBOT_STAGING` | `0` en producción; `1` para probar con el entorno de pruebas de Let's Encrypt |

   `.env` queda fuera de git; nunca lo suba al repositorio.

3. Copie los 20 PDF del docente a `./corpus` del repositorio en la EC2 (por ejemplo con `scp` desde su equipo). El bootstrap exige exactamente 20 archivos `*.pdf`.

4. Ejecute el bootstrap:

   ```bash
   scripts/bootstrap-ec2.sh
   ```

   Valida `.env`, el corpus y RDS antes de instalar Docker; crea el swap de 4 GB; instala Docker; descarga el modelo ONNX si falta; ejecuta `docker compose -f compose.prod.yaml up -d --build` y espera hasta 20 minutos (`READY_TIMEOUT_SECONDS`, 1200 por defecto) a que se cumplan las tres condiciones de "listo": `/api/actuator/health` responde `UP`, `nomic-embed-text:v1.5` figura en `/api/tags` de Ollama y `GET /api/articles` devuelve los 20 artículos. Termina con código 0 y la URL al cumplirse.

   El script es idempotente: puede re-ejecutarlo sin duplicar el swap, reinstalar paquetes, reemplazar `.env`, descargar de nuevo el modelo ni borrar volúmenes. El grupo `docker` solo se aplica a las sesiones nuevas; en la primera ejecución el script usa `sudo docker` por esa razón.

5. Verifique el despliegue:

   ```bash
   curl -I http://tu-dominio.example/
   curl -I https://tu-dominio.example/
   curl -s https://tu-dominio.example/api/actuator/health
   docker compose -f compose.prod.yaml ps
   ```

   - Mientras no exista certificado, `http://` sirve la aplicación (certbot reintenta la emisión cada 30 minutos). Una vez emitido, `http://` responde 301 hacia `https://` y `https://` responde 200 con TLS 1.2 o superior; nginx recarga el certificado en menos de unos 90 segundos, sin reiniciar `web`.
   - `/api/actuator/health` devuelve `UP`.
   - Abra la aplicación en el navegador y recorra las pestañas: Similitud textual, Ranking de términos, Clustering jerárquico y Búsqueda Patrones.

6. Actualizar la aplicación con un nuevo despliegue:

   ```bash
   git pull
   docker compose -f compose.prod.yaml up -d --build
   ```

   O vuelva a ejecutar `scripts/bootstrap-ec2.sh`. Los volúmenes (`ollama-data`, `letsencrypt`, `certbot-webroot`) se conservan. No use `down -v`: borraría el modelo de Ollama y los certificados.

7. Ver registros:

   ```bash
   docker compose -f compose.prod.yaml logs
   docker compose -f compose.prod.yaml logs --tail=100 -f api
   ```

8. Revertir a un commit anterior: vuelva al commit y reconstruya.

   ```bash
   git checkout <commit-anterior>
   docker compose -f compose.prod.yaml up -d --build
   ```

   Las migraciones Flyway ya aplicadas en RDS no se revierten: si el commit anterior espera un esquema distinto, hay que resolverlo a mano en la base.

9. Nada con `aws` se automatiza: crear, cambiar o destruir los recursos de la sección 3 se hace siempre a mano en la consola. Ningún script del repositorio habla con AWS salvo la conexión de la API y del bootstrap a la base de datos RDS.

## 5. Verificación de memoria

La EC2 tiene 4 GB: la memoria se reparte con `mem_limit` por servicio y 4 GB de swap.

```bash
# Ningún contenedor debe haber sido terminado por falta de memoria (esperado: false).
for service in ollama api web certbot; do
  id="$(docker compose -f compose.prod.yaml ps -q "$service")"
  echo -n "$service OOMKilled="
  docker inspect --format '{{.State.OOMKilled}}' "$id"
done

# El swap de 4 GB debe estar activo.
swapon --show
free -h
```

- Con el stack arrancado y los 20 PDF ingeridos, ningún contenedor debe tener `OOMKilled=true` y `swapon --show` debe listar `/swapfile` de 4 GB (criterio de aceptación AC-22).
- Para comprobar que el modelo de Ollama sobrevive a la recreación del contenedor:

  ```bash
  docker compose -f compose.prod.yaml up -d --force-recreate ollama
  docker compose -f compose.prod.yaml exec web wget -qO- http://ollama:11434/api/tags
  ```

  `nomic-embed-text:v1.5` debe seguir en la lista sin descargarse otra vez, porque `/root/.ollama` es un volumen nombrado.
- Para ver el heap acotado de la API: `docker compose -f compose.prod.yaml exec api printenv JAVA_TOOL_OPTIONS`.

## 6. Fallos previsibles y su corrección

### 6.1 RDS inaccesible por el grupo de seguridad

- Síntoma: el bootstrap termina con código 1 y el mensaje "RDS no responde en <endpoint>:5432".
- Causa: el grupo de seguridad de RDS no permite el puerto 5432 desde el grupo de seguridad de la EC2, o el endpoint de `SPRING_DATASOURCE_URL` está mal escrito, o la EC2 y RDS están en VPC distintas.
- Corrección: en la consola, edite el grupo `bibliomtr-rds` y deje una sola regla de entrada PostgreSQL 5432 con origen el grupo `bibliomtr-ec2`; compare el endpoint de la consola con el de `.env`; confirme que ambos están en la misma VPC. Vuelva a ejecutar el bootstrap.

### 6.2 Extensión `vector` no disponible

- Síntoma: el bootstrap termina con código 1 y el mensaje "la extensión vector no está instalada ni disponible en RDS".
- Causa: la versión de PostgreSQL elegida en RDS no ofrece pgvector, o la base de `SPRING_DATASOURCE_URL` no es la creada.
- Corrección: cree la instancia con una versión de PostgreSQL que incluya pgvector (o actualice la versión del motor) y vuelva a ejecutar el bootstrap. No ejecute `CREATE EXTENSION` a mano: la migración Flyway `V1__enable_pgvector.sql` ya lo hace al arrancar la API.

### 6.3 Certificado no emitido: DNS sin propagar o límite de Let's Encrypt

- Síntoma: `https://` no responde, `http://` sigue sirviendo la aplicación y `docker compose -f compose.prod.yaml logs certbot` muestra "no se pudo emitir".
- Causa: el registro A todavía no apunta a la Elastic IP (DNS sin propagar), el puerto 80 no está abierto en el grupo de seguridad, o se alcanzó el límite de emisión o de validaciones fallidas de Let's Encrypt.
- Corrección:
  - Compruebe que `dig +short tu-dominio.example` devuelve la Elastic IP y que el puerto 80 está abierto. Certbot reintenta cada 30 minutos sin intervención (4 intentos por hora, por debajo del límite de validaciones fallidas).
  - Para probar sin gastar el límite de producción, ponga `CERTBOT_STAGING=1` en `.env` y reinicie `certbot` con `docker compose -f compose.prod.yaml up -d certbot`. Los certificados de staging no son confiables en el navegador.
  - Antes de volver a producción (`CERTBOT_STAGING=0`) borre el linaje de staging, o certbot lo seguirá renovando en lugar de emitir uno real: `docker compose -f compose.prod.yaml run --rm --entrypoint certbot certbot delete --cert-name tu-dominio.example`. Después reinicie `certbot`; `web` no necesita reinicio porque recarga el certificado nuevo por sí solo.
  - Si el límite de Let's Encrypt ya se alcanzó, espere a que expire (por hora o por semana, según el límite) y use `CERTBOT_STAGING=1` mientras tanto.

### 6.4 Memoria agotada

- Síntoma: un contenedor se reinicia solo, `OOMKilled=true` en `docker inspect`, o el build de `api` o de `web` termina con "Killed".
- Causa: swap inactivo, una medición JMH lanzada durante la ingesta inicial, dos mediciones a la vez, o el build de Gradle o de npm en una máquina de 4 GB sin swap. Si `api` termina con `OutOfMemoryError: Java heap space` justo después de `Cargando SPECTER2 desde class path resource`, la imagen se construyó con un `Dockerfile` que no copia el modelo a `/app/models/`: actualice el repositorio y reconstruya con `docker compose -f compose.prod.yaml up -d --build`.
- Corrección: verifique `swapon --show` (debe aparecer `/swapfile`; si no, vuelva a ejecutar el bootstrap, que lo crea antes del build); no lance JMH durante la ingesta ni dos a la vez; reinicie el servicio caído con `docker compose -f compose.prod.yaml up -d`. La JVM de `api` usa `-XX:+ExitOnOutOfMemoryError`, así que un OOM de Java la reinicia por `restart: unless-stopped`.

### 6.5 Descarga lenta del modelo de Ollama

- Síntoma: el bootstrap agota los 20 minutos con el modelo `nomic-embed-text:v1.5` ausente en `/api/tags` y menos de 20 artículos.
- Causa: la API descarga el modelo con `POST /api/pull` la primera vez (unos 274 MB) y la descarga tiene un límite de 15 minutos; una red lenta puede excederlo.
- Corrección: revise `docker compose -f compose.prod.yaml logs ollama api`; reinicie la API con `docker compose -f compose.prod.yaml restart api` para repetir la descarga y la ingesta, o descargue el modelo a mano con `docker compose -f compose.prod.yaml exec ollama ollama pull nomic-embed-text:v1.5` (queda en el volumen `ollama-data`). Luego vuelva a ejecutar el bootstrap, o con más tiempo: `READY_TIMEOUT_SECONDS=2400 scripts/bootstrap-ec2.sh`.

### 6.6 Endurecimiento fuera del alcance

`sslmode=require` cifra la conexión a RDS pero no verifica el certificado del servidor. Pasar a `verify-full` con el paquete de certificados de RDS queda fuera de esta spec.

## 7. Validaciones estáticas

Ninguna toca AWS. Se ejecutan en el equipo de desarrollo o en la EC2:

```bash
# Compose sin errores, sin servicio db, con un .env de ejemplo
docker compose --env-file .env.prod.example -f compose.prod.yaml config

# Sintaxis y análisis de los scripts (shellcheck si está instalado)
bash -n scripts/bootstrap-ec2.sh scripts/certbot-loop.sh scripts/check-nginx.sh web/docker-entrypoint.d/*.sh
shellcheck scripts/bootstrap-ec2.sh scripts/certbot-loop.sh scripts/check-nginx.sh web/docker-entrypoint.d/*.sh

# nginx -t en contenedor, en modo HTTP y en modo HTTPS (requiere Docker y openssl)
scripts/check-nginx.sh

# Invariantes de los archivos y del runbook
./gradlew test --tests DeploymentInvariantsTest --tests DeploymentRunbookTest --rerun
```

Gradle no registra los archivos de despliegue como entradas de `test`: tras editar solo ellos, use `--rerun`.

## 8. Mediciones JMH en la EC2

Las mediciones JMH en la EC2 son solo de demostración: la instancia tiene una vCPU compartida y ruidosa (`c7i-flex`), y sus tiempos varían según la carga de otros inquilinos.

- Los números para el documento técnico se miden en hardware controlado, y se registra el hardware (modelo de CPU, núcleos, memoria, sistema operativo y versión de Java) junto a los resultados.
- No lance JMH durante la ingesta inicial ni dos mediciones a la vez (la API ya rechaza la concurrente). El proceso hijo de JMH hereda `JAVA_TOOL_OPTIONS`, con lo que su heap queda acotado por `-Xmx768m`.
- Este despliegue no cambia el diseño de JMH ni las mediciones (constitución P11).

## 9. Trazabilidad

El Requerimiento 5 está registrado en [`traceability.md`](traceability.md). La sección `docs/documento-tecnico.md` § Requerimiento 5: Despliegue (pendiente) la redactan los estudiantes.
