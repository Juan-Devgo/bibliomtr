# Entorno para el Requerimiento 5: Despliegue en AWS (EC2 + RDS)

## 1. Metadata
- Status: Approved
- Updated: 2026-11-03
- Constitution version: 1.2.0
- Required by: none

## 2. Constitution relationship
- Applies:
  - P2: nginx, certbot, Docker y Ollama son infraestructura auxiliar; ninguna librería ni servicio nuevo implementa un algoritmo del proyecto.
  - P3: Ollama solo genera vectores (`nomic-embed-text:v1.5`) y SPECTER2 sigue ejecutándose con ONNX Runtime dentro de `api`; la métrica de similitud no cambia.
  - P4: los archivos de despliegue se generan con IA generativa como apoyo y su uso se declara en el runbook.
  - P5: el corpus desplegado son únicamente los 20 PDF del docente; el despliegue no agrega ni transforma artículos.
  - P9: `docs/traceability.md` registra requerimiento → módulo → vista → sección del documento técnico `(pendiente)` → verificación; el documento técnico no se redacta en este flujo.
  - P11: el despliegue no cambia las mediciones de tiempo; el runbook advierte que los números para el documento técnico se miden en hardware controlado y se registra el hardware.
  - P12: imágenes de contenedor con versión fijada (sin `latest`), modelos con versión fijada y configuración sin aleatoriedad; la reproducibilidad del despliegue no depende de versiones flotantes.
  - P13: la infraestructura vive en archivos propios (`compose.prod.yaml`, `web/nginx.conf`, `scripts/`, `docs/`), separada de los módulos de PDF, preprocesamiento, algoritmos, evaluación y API.
- Conflicts: none

## 3. User stories
| ID | Priority | Actor | Story |
|---|---|---|---|
| US-1 | P1 | Operador | As a operador, I need un archivo de composición de producción independiente del de desarrollo, for levantar web, api y ollama en una sola EC2 apuntando a RDS |
| US-2 | P1 | Operador | As a operador, I need un proxy reverso nginx con HTTPS emitido por Let's Encrypt para mi dominio, for publicar la aplicación de forma segura |
| US-3 | P1 | Operador | As a operador, I need un script de bootstrap de la EC2 que se pueda re-ejecutar, for dejar la máquina lista sin repetir ni romper pasos |
| US-4 | P1 | Operador | As a operador, I need toda la configuración y los secretos en un `.env` fuera de git con una plantilla sin secretos, for no publicar credenciales |
| US-5 | P1 | Operador | As a operador, I need un runbook con los pasos manuales de AWS (RDS, grupos de seguridad, EC2, Elastic IP, DNS) con valores recomendados, for crear la infraestructura sin automatizar sobre la cuenta real |
| US-6 | P2 | Estudiante | As a estudiante, I need que los archivos de despliegue se validen sin tocar AWS y queden en la trazabilidad con la sección del documento técnico `(pendiente)`, for revisar el despliegue y escribir mi documento |
| US-7 | P2 | Estudiante | As a estudiante, I need saber que las mediciones JMH en la EC2 son ruidosas, for medir los números del documento técnico en hardware controlado |

## 4. Requirements
- REQ-1 (US-1): Existe `compose.prod.yaml` en la raíz, independiente de `compose.yaml` y `compose.dev.yaml`: no usa `include` ni `extends` hacia ellos y no define ningún servicio `db`.
- REQ-2 (US-1): `compose.prod.yaml` define los servicios `web` (nginx), `api` y `ollama`, más los servicios auxiliares de certbot que el plan decida (REQ-10 a REQ-12); `api` se construye desde el `Dockerfile` raíz y `web` desde `web/Dockerfile`.
- REQ-3 (US-1): Toda imagen que no se construye en el repositorio usa una etiqueta de versión fija (nunca `latest` ni etiqueta omitida); `ollama` usa la misma imagen que `compose.dev.yaml`.
- REQ-4 (US-1): Solo `web` publica puertos al host (80 y 443); `api` y `ollama` no tienen `ports` y se comunican por la red interna de Compose (`api` → `ollama` por `http://ollama:11434`, `web` → `api` por `api:8080`).
- REQ-5 (US-1): Todos los servicios permanentes tienen `restart: unless-stopped` y un `healthcheck`; `api` se comprueba con `/api/actuator/health`, `ollama` con una consulta a su API local y `web` con una petición HTTP local.
- REQ-6 (US-1): `depends_on` usa condiciones: `api` espera a `ollama` con `service_healthy` y `web` espera a `api` con `service_healthy`.
- REQ-7 (US-1): Cada servicio tiene `mem_limit`, la suma de los `mem_limit` no supera 3 584 MiB (deja el resto de los 4 GB para el sistema operativo y el motor de contenedores), y el heap de la JVM de `api` queda acotado por `-Xmx` vía `JAVA_TOOL_OPTIONS`, de modo que también acota el proceso hijo JMH que lanza `api` (heredan la variable). Una sola medición JMH a la vez (la API ya rechaza la concurrente); el runbook indica no lanzarla durante la ingesta inicial.
- REQ-8 (US-1): `ollama` guarda sus modelos en un volumen nombrado montado en `/root/.ollama`, y el modelo `nomic-embed-text:v1.5` se conserva entre reinicios y recreaciones del contenedor.
- REQ-9 (US-2): `web/nginx.conf` (el archivo que ya copia `web/Dockerfile`) se ajusta, no se duplica: proxy de `/api/` hacia `api:8080` con cabeceras `Host`, `X-Real-IP`, `X-Forwarded-For` y `X-Forwarded-Proto` y fallback de SPA a `index.html`; no define tiempos de lectura del proxy especiales (rige el valor predeterminado de nginx de 60 s), porque la descarga del modelo va directa de `api` a `ollama` y `POST /api/benchmarks` responde 202 de inmediato.
- REQ-10 (US-2): Con `DOMAIN` y `CERTBOT_EMAIL` definidos, el certificado de Let's Encrypt para `DOMAIN` se emite por el desafío HTTP-01 con certbot, y los certificados y el directorio webroot del desafío viven en volúmenes nombrados (`/etc/letsencrypt` y el webroot).
- REQ-11 (US-2): Mientras no exista certificado emitido (primer arranque), el stack arranca y sirve la aplicación por HTTP en el puerto 80 y responde el desafío ACME en `/.well-known/acme-challenge/`; una vez emitido, el puerto 80 redirige a HTTPS con 301 salvo el desafío ACME, y el puerto 443 sirve la aplicación con TLS 1.2 o superior.
- REQ-12 (US-2): La renovación del certificado es automática (intento al menos dos veces al día, como recomienda Let's Encrypt) y nginx recarga el certificado nuevo sin detener el servicio.
- REQ-13 (US-2): Con `DOMAIN` vacío o sin definir, el stack sigue funcionando solo por HTTP, sin intentar emitir certificado.
- REQ-14 (US-2): El stack de desarrollo (`compose.yaml` con `web` en el puerto 5173) sigue funcionando sin cambios de comportamiento tras ajustar `web/nginx.conf`.
- REQ-15 (US-3): Existe `scripts/bootstrap-ec2.sh`, escrito para Ubuntu 26.04 LTS, que instala Docker CE y el plugin de Compose desde el repositorio apt oficial de Docker; si ese repositorio no ofrece paquetes para la versión del sistema, usa como alternativa `docker.io` de Ubuntu más el plugin de Compose y lo informa.
- REQ-16 (US-3): El script crea un swap de 4 GB en `/swapfile` con entrada en `/etc/fstab`, agrega al usuario operador al grupo `docker`, instala `python3`, `python3-venv` (que necesita `scripts/download-model.sh`) y `postgresql-client` (para REQ-19), y crea únicamente `./corpus` bajo el repositorio (los datos de Postgres están en RDS y los modelos de Ollama en un volumen nombrado, por lo que no hay otros directorios de datos).
- REQ-17 (US-3): El script ejecuta `scripts/download-model.sh` antes del build solo si el modelo ONNX no está ya en `src/main/resources/models/specter2-base/`, construye y levanta el stack con `docker compose -f compose.prod.yaml up -d --build`, y no sube ninguna imagen a un registro.
- REQ-18 (US-3): El script es idempotente: re-ejecutarlo no duplica el swap ni la entrada de `/etc/fstab`, no reinstala paquetes ya instalados, no reemplaza un `.env` existente, no vuelve a descargar el modelo ni borra volúmenes, y termina con código 0 si el estado final es el esperado.
- REQ-19 (US-3): El script verifica que `.env` existe y contiene las variables obligatorias (REQ-21), que la base RDS responde en el puerto 5432 y que la extensión `vector` está disponible o instalada; ante un fallo termina con código distinto de 0 y un mensaje en español que dice qué corregir. No ejecuta `CREATE EXTENSION` por su cuenta: lo hace la migración Flyway `V1__enable_pgvector.sql` ya existente, que no se edita.
- REQ-20 (US-3): El script considera el despliegue listo cuando se cumplen las tres condiciones: `GET /api/actuator/health` responde `UP`, el modelo `nomic-embed-text:v1.5` figura en `/api/tags` de Ollama y `GET /api/articles` devuelve los 20 artículos del corpus (la descarga del modelo y la ingesta ocurren después de que el servidor arranca, el pull tiene un límite de 15 minutos); espera hasta 20 minutos y, si el tiempo se agota, termina con código distinto de 0, muestra los últimos registros del servicio que falla y el comando para ver el resto.
- REQ-21 (US-4): La configuración se lee de un `.env` fuera de git; el repositorio entrega `.env.prod.example` sin secretos con, como mínimo, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `BIBLIOMTR_CORPUS_DIR` (ruta dentro del contenedor, `/data/corpus`, montada en solo lectura desde `./corpus` del host), `BIBLIOMTR_OLLAMA_URL`, `DOMAIN` y `CERTBOT_EMAIL`; la URL de ejemplo de la base exige TLS hacia RDS (`sslmode=require`); y `.gitignore` ignora `.env` y no ignora `.env.prod.example`.
- REQ-22 (US-4): Ningún archivo del repositorio contiene contraseñas, claves de AWS, endpoints reales de RDS, ni valores reales de dominio o correo; `.env.prod.example` usa valores de ejemplo evidentes (`CAMBIAR_...`, `tu-dominio.example`).
- REQ-23 (US-5): Existe `docs/despliegue-aws.md` con el runbook en español y el `README.md` lo enlaza; el runbook cubre, para la región us-east-2: grupo de seguridad de la EC2 (80 y 443 abiertos, 22 solo desde la IP del operador), grupo de seguridad de RDS (5432 solo desde el grupo de la EC2), RDS PostgreSQL Single-AZ `db.t4g.micro` privado con el usuario maestro y la base `bibliomtr` (un solo usuario), EC2 `c7i-flex.large` con Ubuntu 26.04 LTS, EBS gp3 de 30 GB, Elastic IP y registro DNS tipo A hacia ella.
- REQ-24 (US-5): El runbook indica en qué orden rellenar `.env` (incluidos `DOMAIN` y `CERTBOT_EMAIL`, antes del bootstrap), copiar los 20 PDF a `./corpus` del repositorio en la EC2, ejecutar el bootstrap, verificar el despliegue (health, HTTPS, pestañas de la aplicación), actualizar la aplicación con un nuevo despliegue, ver registros, y revertir (volver a un commit anterior y reconstruir; las migraciones Flyway ya aplicadas no se revierten); indica también que ningún paso con `aws` ni de creación de recursos se automatiza en el repositorio.
- REQ-25 (US-5): El runbook documenta los fallos previsibles y su corrección: RDS inaccesible por grupo de seguridad, extensión `vector` no disponible, certificado no emitido por DNS sin propagar o límite de emisión de Let's Encrypt (con la opción de probar antes con el entorno staging), memoria agotada y descarga lenta del modelo de Ollama.
- REQ-26 (US-6): La validación estática no usa AWS: `docker compose -f compose.prod.yaml config` sin errores con un `.env` de ejemplo, `bash -n` y `shellcheck` (si está instalado) sin errores sobre el script, y `nginx -t` sobre la configuración, ejecutado en contenedor cuando Docker esté disponible; el runbook documenta cómo ejecutarlas.
- REQ-27 (US-6): Un test automatizado verifica, leyendo los archivos, los invariantes de REQ-1, REQ-3, REQ-4, REQ-5 y REQ-22 (sin `db` ni `include`, sin `latest`, solo `web` con `ports`, `restart` y `healthcheck` en cada servicio permanente, sin secretos).
- REQ-28 (US-6): `docs/traceability.md` agrega las filas del Requerimiento 5 (despliegue) con módulo `— (infraestructura: …)`, vista `—`, sección `docs/documento-tecnico.md` § Requerimiento 5: Despliegue (pendiente) y su verificación; `docs/guia-implementacion.md` no cambia salvo que el plan demuestre que el formato vigente lo exige.
- REQ-29 (US-7): El runbook advierte que las mediciones JMH en una vCPU compartida de la EC2 son ruidosas, que los números para el documento técnico se miden en hardware controlado y que se registra el hardware, y no cambia el diseño de JMH.
- REQ-30 (US-6): Los archivos de despliegue no modifican código de producción en `src/main/java`, migraciones Flyway ya aplicadas ni `web/src`.

## 5. Acceptance criteria
- AC-1 (US-1): Given el repositorio, when se ejecuta `docker compose -f compose.prod.yaml config` con un `.env` de ejemplo, then termina sin errores, la salida no contiene un servicio `db` y no aparece `compose.dev.yaml`.
- AC-2 (US-1): Given la salida de `config`, when se listan los `ports`, then solo `web` publica 80 y 443; `api` y `ollama` no publican ninguno.
- AC-3 (US-1): Given la salida de `config`, when se revisa cada imagen no construida, then todas tienen etiqueta de versión fija y `ollama` coincide con la de `compose.dev.yaml`.
- AC-4 (US-1): Given la salida de `config`, when se revisan los servicios permanentes, then cada uno tiene `restart: unless-stopped`, `healthcheck` y `mem_limit`, y la suma de `mem_limit` es menor o igual a 3 584 MiB.
- AC-5 (US-1): Given `api` en ejecución, when se inspeccionan sus variables, then `JAVA_TOOL_OPTIONS` contiene `-Xmx` y el proceso hijo JMH hereda ese límite.
- AC-6 (US-1): Given `ollama` ya con el modelo descargado, when se recrea el contenedor con `docker compose -f compose.prod.yaml up -d --force-recreate ollama`, then el modelo sigue disponible sin volver a descargarse.
- AC-7 (US-1): Given `ollama` no saludable, when arranca el stack, then `api` y `web` no arrancan hasta que `ollama` esté saludable.
- AC-8 (US-2): Given `web/nginx.conf` ajustado, when se ejecuta `nginx -t` en un contenedor `nginx` con la misma versión que usa `web`, then la configuración es válida tanto en modo HTTP como en modo HTTPS.
- AC-9 (US-2): Given un primer arranque con `DOMAIN` definido y sin certificado, when se pide `http://<host>/` y `http://<host>/api/actuator/health`, then responden la aplicación y `UP` por HTTP, y `/.well-known/acme-challenge/` se sirve desde el webroot.
- AC-10 (US-2): Given un certificado emitido, when se pide `http://<dominio>/`, then responde 301 hacia `https://<dominio>/`, y `https://<dominio>/` sirve la aplicación con TLS 1.2 o superior.
- AC-11 (US-2): Given un certificado renovado, when termina la renovación, then nginx lo recarga sin cortar las conexiones activas y sin reiniciar el contenedor `web`.
- AC-12 (US-2): Given `DOMAIN` vacío, when arranca el stack, then no se ejecuta certbot, el puerto 443 no se publica o no escucha, y la aplicación responde por HTTP.
- AC-13 (US-2): Given una ruta del cliente como `/pattern-search`, when se pide a `web`, then se devuelve `index.html` con 200, y `/api/...` se envía a `api`.
- AC-14 (US-2): Given `web/nginx.conf`, when se buscan directivas `proxy_read_timeout`, then no hay ninguna (rige el valor predeterminado) y `POST /api/benchmarks` a través de nginx responde 202 sin esperar la medición.
- AC-15 (US-2): Given `compose.yaml` de desarrollo, when se ejecuta `docker compose up --build`, then `web` sigue respondiendo en `http://localhost:5173` con la aplicación y el proxy `/api/`.
- AC-16 (US-3): Given un Ubuntu 26.04 limpio y un `.env` válido, when se ejecuta el script con un usuario con `sudo`, then termina con código 0, `docker compose version` funciona, el swap activo es de 4 GB, el usuario pertenece al grupo `docker` y se cumplen las tres condiciones de listo de REQ-20.
- AC-17 (US-3): Given que el script ya terminó una vez, when se vuelve a ejecutar, then termina con código 0, `/etc/fstab` conserva una sola entrada de swap, el `.env` no cambia, el modelo ONNX no se vuelve a descargar y los volúmenes se conservan.
- AC-18 (US-3): Given que el repositorio de Docker CE no soporta Ubuntu 26.04, when el script instala Docker, then usa `docker.io` y el plugin de Compose, informa del fallback y continúa.
- AC-19 (US-3): Given que no existe `.env` o falta una variable obligatoria, when se ejecuta el script, then termina con código distinto de 0 antes de instalar paquetes pesados, copia `.env.prod.example` a `.env` si no existe y dice qué variables completar.
- AC-20 (US-3): Given que RDS no es alcanzable desde la EC2 (grupo de seguridad cerrado o endpoint erróneo), when se ejecuta el script, then termina con código distinto de 0 y un mensaje en español que apunta al grupo de seguridad y al endpoint.
- AC-21 (US-3): Given que no se cumplen las tres condiciones de listo de REQ-20 en 20 minutos, when se agota la espera, then el script termina con código distinto de 0, muestra las últimas líneas del registro del servicio que falla y el comando `docker compose -f compose.prod.yaml logs`.
- AC-22 (US-3): Given el stack arrancado con los 20 PDF ingeridos, when se ejecuta `docker inspect` sobre cada contenedor, then ninguno tiene `OOMKilled=true` y el swap de 4 GB está activo; una medición JMH en la EC2 es solo de demostración y no es criterio de aceptación.
- AC-23 (US-4): Given el repositorio, when se busca texto con apariencia de secreto (contraseñas, `AKIA…`, endpoints `*.rds.amazonaws.com`, dominio o correo reales), then no aparece en ningún archivo versionado.
- AC-24 (US-4): Given `.env.prod.example`, when se revisan sus claves, then incluye las siete mínimas de REQ-21 con valores de ejemplo evidentes, y `git check-ignore .env` confirma que `.env` está ignorado mientras `.env.prod.example` no.
- AC-25 (US-5): Given el runbook, when un operador sigue sus pasos en la consola de AWS de us-east-2, then crea RDS, los dos grupos de seguridad, la EC2 y la Elastic IP sin ejecutar ningún comando `aws` ni scripts del repositorio contra la cuenta.
- AC-26 (US-5): Given un fallo de la lista de REQ-25, when se consulta el runbook, then hay una causa y una corrección para ese fallo.
- AC-27 (US-6): Given el repositorio con los archivos de despliegue, when se ejecutan las validaciones de REQ-26 y el test de REQ-27, then todas pasan sin red hacia AWS.
- AC-28 (US-6): Given `docs/traceability.md`, when se busca "Requerimiento 5", then hay filas con spec 005, módulo `— (infraestructura: …)` y la sección del documento técnico `(pendiente)`.
- AC-29 (US-6): Given el diff del flujo de despliegue, when se lista, then no incluye `web/src/features/clustering/ClusteringSection.tsx`, `web/pnpm-lock.yaml`, `docs/documento-tecnico.md`, ningún archivo de `src/main/java` ni migraciones.
- AC-30 (US-7): Given el runbook, when se busca "JMH", then hay una advertencia de vCPU compartida y la instrucción de medir en hardware controlado y registrar el hardware.

## 6. Out of scope
- Crear, modificar o destruir recursos de AWS (RDS, EC2, grupos de seguridad, Elastic IP, DNS) desde el repositorio: `aws`, Terraform, CloudFormation u otro IaC.
- Balanceador de carga (ALB), NAT, Multi-AZ, auto scaling y varias instancias.
- Registro de imágenes (ECR u otro): la imagen de `api` se construye en la propia EC2.
- Copias de seguridad y restauración de RDS, monitoreo, alarmas y agregación de registros.
- Autenticación, autorización o limitación de peticiones de la aplicación.
- Redacción de `docs/documento-tecnico.md`.
- Cambios en `src/main/java`, migraciones Flyway ya aplicadas, `web/src`, JMH o los algoritmos.
- Integración continua y despliegue automático.
- Registro del dominio y gestión de DNS fuera de apuntar el registro A a la Elastic IP.
- Un usuario de base de datos dedicado distinto del maestro de RDS.

## 7. Clarifications
- 2026-11-03. Q: ¿TLS y dominio? A: Dominio propio con nginx y certbot (HTTP-01); el usuario no dio el nombre del dominio ni el correo, así que `DOMAIN` y `CERTBOT_EMAIL` son variables de `.env` que se rellenan antes del bootstrap, sin valores reales en el repositorio.
- 2026-11-03. Q: ¿Sistema operativo de la EC2? A: Ubuntu 26.04 LTS; si el repositorio apt de Docker CE no la soporta, el script usa `docker.io` de Ubuntu más el plugin de Compose.
- 2026-11-03. Q: ¿Región de AWS? A: us-east-2.
- 2026-11-03. Q: ¿Usuario y base de RDS? A: Usuario maestro único, base `bibliomtr`, clase `db.t4g.micro`, sin usuario dedicado.
- 2026-11-03. Q: ¿Qué significa "despliegue listo" para el bootstrap? A (confirmada por el usuario): health `UP`, modelo en `/api/tags` de Ollama y 20 artículos en `GET /api/articles` (REQ-20).
- 2026-11-03. Q: ¿Tiempos de lectura largos en nginx? A (confirmada por el usuario): no hay; la descarga del modelo va directa api → ollama y `POST /api/benchmarks` responde 202 (REQ-9, AC-14).
- 2026-11-03. Q: ¿Cómo se verifica la memoria y se admite JMH en la EC2? A (confirmada por el usuario): sin `OOMKilled` y con swap activo; JMH en la EC2 solo de demostración, una a la vez y no durante la ingesta (REQ-7, AC-22).
- 2026-11-03. Q: ¿Qué es `BIBLIOMTR_CORPUS_DIR` y dónde está el corpus? A (confirmada por el usuario): ruta dentro del contenedor (`/data/corpus`), montada en solo lectura desde `./corpus` del repositorio en la EC2; el bootstrap solo crea `./corpus` (REQ-16, REQ-21).
- 2026-11-03. Menores incorporados sin consulta: `postgresql-client` en el bootstrap (REQ-16), `sslmode=require` hacia RDS (REQ-21), definición de "revertir" (REQ-24) y entorno staging de Let's Encrypt (REQ-25). El plan fija si 443 se publica siempre o solo con `DOMAIN` definido (REQ-4, AC-12).
- 2026-11-03. Q: ¿Swap y reparto de memoria? A: Swap de 4 GB y `mem_limit` por servicio dentro de 4 GB, con el reparto exacto definido en el plan.
