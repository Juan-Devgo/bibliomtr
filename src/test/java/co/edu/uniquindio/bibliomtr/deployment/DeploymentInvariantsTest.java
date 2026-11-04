package co.edu.uniquindio.bibliomtr.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Verifica los invariantes de los archivos de despliegue de la spec 005 (REQ-27; REQ-1, REQ-3, REQ-4, REQ-5,
 * REQ-7, REQ-21 y REQ-22) sin Docker y sin red hacia AWS.
 *
 * <p><b>Cómo.</b> Lee como texto {@code compose.prod.yaml}, {@code compose.dev.yaml}, {@code .env.prod.example},
 * {@code .gitignore}, {@code .dockerignore}, {@code web/Dockerfile}, {@code web/nginx*.conf} y los scripts, desde
 * la raíz del proyecto. Los dos archivos de Compose se interpretan con SnakeYAML (llega transitivamente por
 * Spring Boot); el resto se revisa con búsquedas de texto.
 *
 * <p><b>Criterio.</b> Un caso por invariante: sin {@code db}, {@code include} ni {@code extends}; imágenes con
 * versión fija y {@code ollama} igual a la de desarrollo; solo {@code web} con {@code ports}; {@code restart},
 * {@code healthcheck} y {@code mem_limit} en cada servicio con suma de memoria dentro del presupuesto;
 * {@code -Xmx} acotado; {@code depends_on} con {@code service_healthy}; volumen nombrado de {@code ollama};
 * nginx con ACME, redirección 301, TLS 1.2 o superior y sin {@code proxy_read_timeout}; vigilante con
 * {@code nginx -s reload}; estructura del bootstrap; las ocho claves de {@code .env.prod.example}; {@code .env}
 * ignorado; y ausencia de secretos.
 *
 * <p><b>Nota.</b> Gradle no registra estos archivos como entradas de {@code test}: tras editar solo ellos,
 * ejecutar {@code ./gradlew test --rerun}.
 *
 * <p>Complejidad: O(L) en tiempo y espacio por caso, con L = tamaño total de los archivos leídos.
 */
class DeploymentInvariantsTest {

	private static final Path COMPOSE_PROD = Path.of("compose.prod.yaml");
	private static final Path COMPOSE_DEV = Path.of("compose.dev.yaml");
	private static final Path ENV_EXAMPLE = Path.of(".env.prod.example");
	private static final Path GITIGNORE = Path.of(".gitignore");
	private static final Path DOCKERIGNORE = Path.of(".dockerignore");
	private static final Path WEB_DOCKERFILE = Path.of("web", "Dockerfile");
	private static final Path NGINX_HTTP = Path.of("web", "nginx.conf");
	private static final Path NGINX_HTTPS = Path.of("web", "nginx-https.conf");
	private static final Path NGINX_LOCATIONS = Path.of("web", "nginx-locations.conf");
	private static final Path SELECT_MODE = Path.of("web", "docker-entrypoint.d", "15-select-nginx-mode.sh");
	private static final Path WATCHER = Path.of("web", "docker-entrypoint.d", "40-watch-certificate.sh");
	private static final Path BOOTSTRAP = Path.of("scripts", "bootstrap-ec2.sh");
	private static final Path CERTBOT_LOOP = Path.of("scripts", "certbot-loop.sh");
	private static final Path CHECK_NGINX = Path.of("scripts", "check-nginx.sh");
	private static final Path RUNBOOK = Path.of("docs", "despliegue-aws.md");

	private static final List<String> SERVICES = List.of("ollama", "api", "web", "certbot");
	private static final long MEMORY_BUDGET_MIB = 3584;
	private static final long MAX_HEAP_MIB = 1024;
	private static final List<String> ENV_KEYS = List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME",
			"SPRING_DATASOURCE_PASSWORD", "BIBLIOMTR_CORPUS_DIR", "BIBLIOMTR_OLLAMA_URL", "DOMAIN", "CERTBOT_EMAIL",
			"CERTBOT_STAGING");

	/** Archivos que la spec 005 agrega o modifica y que no pueden contener secretos (REQ-22). */
	private static List<Path> deploymentFiles() {
		return List.of(COMPOSE_PROD, ENV_EXAMPLE, WEB_DOCKERFILE, NGINX_HTTP, NGINX_HTTPS, NGINX_LOCATIONS,
				SELECT_MODE, WATCHER, BOOTSTRAP, CERTBOT_LOOP, CHECK_NGINX, RUNBOOK);
	}

	// ---------------------------------------------------------------------------------------------
	// Compose
	// ---------------------------------------------------------------------------------------------

	@Test
	@DisplayName("REQ-1, AC-1: compose.prod.yaml define los 4 servicios, sin db, include ni extends")
	void composeHasNoDbIncludeOrExtends() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);

		assertThat(services.keySet()).containsExactlyInAnyOrderElementsOf(SERVICES);
		assertThat(services).doesNotContainKey("db");
		assertThat(withoutComments(read(COMPOSE_PROD)))
				.doesNotContainPattern("(?m)^\\s*(include|extends)\\s*:")
				.doesNotContain("compose.dev.yaml")
				.doesNotContain("compose.yaml");
		assertThat(composeRoot(COMPOSE_PROD)).containsEntry("name", "bibliomtr");
	}

	@Test
	@DisplayName("REQ-2: api se construye desde la raíz y web desde web/")
	void apiAndWebAreBuiltFromRepository() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);

		assertThat(service(services, "api")).containsEntry("build", ".");
		assertThat(service(services, "web")).containsEntry("build", "./web");
	}

	@Test
	@DisplayName("REQ-3, AC-3: ninguna imagen usa latest, todas llevan versión y ollama es la de desarrollo")
	void imagesHaveFixedVersions() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);
		Pattern versioned = Pattern.compile("^[^:@\\s]+(/[^:@\\s]+)*:v?\\d+(\\.\\d+)*(-[\\w.]+)?$");

		for (String name : SERVICES) {
			Object image = service(services, name).get("image");
			if (image != null) {
				assertThat(image.toString()).as("imagen de %s", name).doesNotContain("latest").matches(versioned);
			}
		}
		assertThat(service(services, "ollama").get("image"))
				.isEqualTo(service(services(COMPOSE_DEV), "ollama").get("image"));
		assertThat(service(services, "certbot")).containsKey("image");

		String dockerfile = read(WEB_DOCKERFILE);
		assertThat(dockerfile).containsPattern("(?m)^FROM nginx:\\d+\\.\\d+\\.\\d+(-[\\w.]+)?\\s*$");
		assertThat(dockerfile).doesNotContain("latest").doesNotContainPattern("(?m)^FROM nginx:alpine");
	}

	@Test
	@DisplayName("REQ-4, AC-2: solo web publica puertos (80 y 443)")
	void onlyWebPublishesPorts() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);

		for (String name : SERVICES) {
			if (name.equals("web")) {
				continue;
			}
			assertThat(service(services, name)).as("servicio %s", name).doesNotContainKey("ports");
		}
		assertThat(strings(service(services, "web").get("ports"))).containsExactlyInAnyOrder("80:80", "443:443");
		assertThat(environment(service(services, "api")).get("BIBLIOMTR_OLLAMA_URL"))
				.contains("BIBLIOMTR_OLLAMA_URL");
	}

	@Test
	@DisplayName("REQ-5, REQ-7, AC-4: cada servicio tiene restart, healthcheck y mem_limit; la suma cabe en 3 584 MiB")
	void everyServiceHasRestartHealthcheckAndMemoryWithinBudget() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);
		long total = 0;

		for (String name : SERVICES) {
			Map<String, Object> service = service(services, name);
			assertThat(service).as("servicio %s", name).containsEntry("restart", "unless-stopped")
					.containsKey("healthcheck").containsKey("mem_limit");
			total += toMib(service.get("mem_limit").toString());
		}
		assertThat(total).as("suma de mem_limit (MiB)").isLessThanOrEqualTo(MEMORY_BUDGET_MIB);
	}

	@Test
	@DisplayName("REQ-5: las comprobaciones de salud usan las rutas que pide la spec")
	void healthchecksTargetExpectedEndpoints() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);

		assertThat(healthcheckTest(service(services, "api"))).contains("/api/actuator/health", "status", "UP", "/dev/tcp/127.0.0.1/8080");
		assertThat(healthcheckTest(service(services, "ollama"))).contains("ollama", "list");
		assertThat(healthcheckTest(service(services, "web"))).contains("/healthz");
		assertThat(healthcheckTest(service(services, "certbot"))).contains("/tmp/certbot-alive");
	}

	@Test
	@DisplayName("REQ-7, AC-5: api acota el heap con -Xmx (máximo 1 GiB) vía JAVA_TOOL_OPTIONS")
	void apiBoundsHeapWithXmx() throws IOException {
		String options = environment(service(services(COMPOSE_PROD), "api")).get("JAVA_TOOL_OPTIONS");

		assertThat(options).isNotNull();
		Matcher xmx = Pattern.compile("-Xmx(\\d+)([mMgG])").matcher(options);
		assertThat(xmx.find()).as("-Xmx en JAVA_TOOL_OPTIONS: %s", options).isTrue();
		long mib = Long.parseLong(xmx.group(1)) * (xmx.group(2).equalsIgnoreCase("g") ? 1024 : 1);
		assertThat(mib).isLessThanOrEqualTo(MAX_HEAP_MIB);
		assertThat(options).contains("-XX:+ExitOnOutOfMemoryError");
	}

	@Test
	@DisplayName("REQ-6, AC-7: api espera a ollama, web a api y certbot a web, todos con service_healthy")
	void dependenciesWaitForHealthyServices() throws IOException {
		Map<String, Object> services = services(COMPOSE_PROD);

		assertThat(condition(services, "api", "ollama")).isEqualTo("service_healthy");
		assertThat(condition(services, "web", "api")).isEqualTo("service_healthy");
		assertThat(condition(services, "certbot", "web")).isEqualTo("service_healthy");
		assertThat(service(services, "ollama")).doesNotContainKey("depends_on");
	}

	@Test
	@DisplayName("REQ-8, AC-6: ollama guarda sus modelos en un volumen nombrado en /root/.ollama")
	void ollamaUsesNamedVolume() throws IOException {
		Map<String, Object> root = composeRoot(COMPOSE_PROD);
		Map<String, Object> ollama = service(services(COMPOSE_PROD), "ollama");

		assertThat(strings(ollama.get("volumes"))).contains("ollama-data:/root/.ollama");
		assertThat(mapOf(root.get("volumes"))).containsKeys("ollama-data", "letsencrypt", "certbot-webroot");
		assertThat(environment(ollama)).containsEntry("OLLAMA_NUM_PARALLEL", "1")
				.containsEntry("OLLAMA_MAX_LOADED_MODELS", "1");
	}

	@Test
	@DisplayName("REQ-4, REQ-22: api monta el corpus en solo lectura y la configuración sale de .env sin valores fijos")
	void apiConfigurationComesFromEnvironmentFile() throws IOException {
		Map<String, Object> api = service(services(COMPOSE_PROD), "api");
		Map<String, String> environment = environment(api);

		for (String key : List.of("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
				"BIBLIOMTR_CORPUS_DIR", "BIBLIOMTR_OLLAMA_URL")) {
			assertThat(environment.get(key)).as(key).startsWith("${" + key + ":?");
		}
		assertThat(strings(api.get("volumes"))).anyMatch(volume -> volume.startsWith("./corpus:") && volume.endsWith(":ro"));
		assertThat(api).doesNotContainKey("env_file");
		assertThat(service(services(COMPOSE_PROD), "web")).doesNotContainKey("env_file");
	}

	// ---------------------------------------------------------------------------------------------
	// nginx y certificados
	// ---------------------------------------------------------------------------------------------

	@Test
	@DisplayName("REQ-9, AC-14: ningún web/nginx*.conf define proxy_read_timeout y el proxy lleva las cabeceras")
	void nginxHasNoProxyReadTimeout() throws IOException {
		for (Path path : List.of(NGINX_HTTP, NGINX_HTTPS, NGINX_LOCATIONS)) {
			assertThat(withoutComments(read(path))).as(path.toString()).doesNotContain("proxy_read_timeout");
		}
		assertThat(read(NGINX_LOCATIONS)).contains("proxy_pass http://api:8080", "proxy_set_header Host",
				"proxy_set_header X-Real-IP", "proxy_set_header X-Forwarded-For", "proxy_set_header X-Forwarded-Proto",
				"try_files $uri /index.html");
	}

	@Test
	@DisplayName("REQ-11, AC-9, AC-10: nginx sirve el desafío ACME, redirige con 301 y usa TLS 1.2 o superior")
	void nginxServesAcmeRedirectsAndUsesModernTls() throws IOException {
		String http = read(NGINX_HTTP);
		String https = read(NGINX_HTTPS);

		assertThat(http).contains("/.well-known/acme-challenge/", "/var/www/certbot", "/healthz");
		assertThat(withoutComments(http)).doesNotContain("443").doesNotContain("ssl_certificate");
		assertThat(https).contains("/.well-known/acme-challenge/", "return 301 https://${DOMAIN}$request_uri",
				"ssl_protocols TLSv1.2 TLSv1.3;", "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem",
				"/etc/letsencrypt/live/${DOMAIN}/privkey.pem");
		assertThat(withoutComments(https)).doesNotContain("TLSv1 ").doesNotContain("TLSv1.1");
	}

	@Test
	@DisplayName("REQ-12, AC-11: el vigilante recarga con nginx -s reload y ningún archivo reinicia web")
	void watcherReloadsWithoutRestartingWeb() throws IOException {
		String watcher = read(WATCHER);

		assertThat(watcher).contains("nginx -s reload", "nginx -t", "cksum");
		for (Path path : deploymentFiles()) {
			if (path.equals(RUNBOOK)) {
				continue;
			}
			assertThat(withoutComments(read(path))).as(path.toString())
					.doesNotContainPattern("restart\\s+web\\b")
					.doesNotContain("docker.sock")
					.doesNotContain("docker restart");
		}
		assertThat(read(COMPOSE_PROD)).doesNotContain("docker.sock");
	}

	@Test
	@DisplayName("REQ-13, AC-12: con DOMAIN vacío ni la selección ni el ciclo de certbot emiten un certificado")
	void emptyDomainNeverIssuesCertificates() throws IOException {
		String loop = read(CERTBOT_LOOP);
		String select = read(SELECT_MODE);

		int emptyCheck = loop.indexOf("if [ -z \"$DOMAIN\" ]");
		assertThat(emptyCheck).as("comprobación de DOMAIN vacío").isNotNegative();
		assertThat(loop.indexOf("certbot certonly")).isGreaterThan(emptyCheck);
		assertThat(loop.indexOf("certbot renew")).isGreaterThan(emptyCheck);
		assertThat(loop).contains("--webroot", "--keep-until-expiring", "CERTBOT_STAGING", "--staging",
				"/tmp/certbot-alive", "RETRY_SECONDS=1800", "RENEW_SECONDS=43200");
		assertThat(select).contains("fullchain.pem", "privkey.pem", "echo http");
		assertThat(read(WEB_DOCKERFILE)).contains("ENV DOMAIN=\"\"", "NGINX_ENVSUBST_FILTER", "/var/www/certbot");
	}

	// ---------------------------------------------------------------------------------------------
	// Bootstrap
	// ---------------------------------------------------------------------------------------------

	@Test
	@DisplayName("REQ-15, REQ-18, AC-17, AC-18: el bootstrap es idempotente y tiene el fallback de docker.io")
	void bootstrapIsIdempotentAndHasDockerFallback() throws IOException {
		String script = read(BOOTSTRAP);
		String code = withoutComments(script);

		assertThat(script).startsWith("#!/usr/bin/env bash");
		assertThat(code).contains("set -euo pipefail", "dpkg -s", "download.docker.com", "curl -fsI", "docker.io",
				"docker-compose-v2", "docker-compose-plugin", "apt-cache policy", "/etc/fstab", "usermod -aG docker",
				"fallocate -l", "mkswap", "swapon", "scripts/download-model.sh", "model.onnx", "tokenizer.json");
		assertThat(code).containsPattern("grep -Eq \"\\^\\$\\{SWAPFILE\\}\\[\\[:space:\\]\\]\" /etc/fstab");
		assertThat(code).doesNotContain("down -v").doesNotContain("--volumes").doesNotContain("docker push")
				.doesNotContain("CREATE EXTENSION").doesNotContainPattern("(?m)^\\s*(source|\\.)\\s+\\.env")
				.doesNotContainPattern("(?m)^\\s*aws\\s");
	}

	@Test
	@DisplayName("REQ-19, AC-19, AC-20: el bootstrap valida .env, corpus y RDS antes de instalar Docker")
	void bootstrapValidatesBeforeInstallingDocker() throws IOException {
		String code = withoutComments(read(BOOTSTRAP));
		String main = code.substring(code.indexOf("main() {"));

		assertThat(code).contains("pg_isready", "sslmode=require", "pg_extension", "pg_available_extensions",
				"CAMBIAR_", "chmod 600", "EUID");
		List<String> order = List.of("validate_env", "check_corpus", "ensure_packages curl", "check_rds",
				"ensure_swap", "install_docker", "ensure_docker_group", "ensure_model", "start_stack", "wait_ready");
		int previous = -1;
		for (String step : order) {
			int position = main.indexOf(step);
			assertThat(position).as("paso %s en main", step).isGreaterThan(previous);
			previous = position;
		}
	}

	@Test
	@DisplayName("REQ-17, REQ-20, AC-21: el bootstrap levanta con up -d --build y espera 1 200 s con diagnóstico")
	void bootstrapStartsStackAndWaitsForReadiness() throws IOException {
		String code = withoutComments(read(BOOTSTRAP));

		assertThat(code).contains("up -d --build", "COMPOSE_FILE=compose.prod.yaml",
				"READY_TIMEOUT_SECONDS=\"${READY_TIMEOUT_SECONDS:-1200}\"", "READY_INTERVAL_SECONDS=10",
				"nomic-embed-text:v1.5", "/api/tags", "/api/articles", "/api/actuator/health", "\"fileName\"",
				"--tail=50", "docker compose -f $COMPOSE_FILE logs", "EXPECTED_ARTICLES=20");
	}

	// ---------------------------------------------------------------------------------------------
	// Entorno y secretos
	// ---------------------------------------------------------------------------------------------

	@Test
	@DisplayName("REQ-21, AC-24: .env.prod.example tiene las ocho claves con valores de ejemplo")
	void envExampleHasEightKeysWithExampleValues() throws IOException {
		Map<String, String> values = envValues();

		assertThat(values.keySet()).containsExactlyElementsOf(ENV_KEYS);
		assertThat(values.get("SPRING_DATASOURCE_URL")).contains("CAMBIAR_").contains("sslmode=require");
		assertThat(values.get("SPRING_DATASOURCE_USERNAME")).startsWith("CAMBIAR_");
		assertThat(values.get("SPRING_DATASOURCE_PASSWORD")).startsWith("CAMBIAR_");
		assertThat(values.get("BIBLIOMTR_CORPUS_DIR")).isEqualTo("/data/corpus");
		assertThat(values.get("BIBLIOMTR_OLLAMA_URL")).isEqualTo("http://ollama:11434");
		assertThat(values.get("DOMAIN")).isEqualTo("tu-dominio.example");
		assertThat(values.get("CERTBOT_EMAIL")).startsWith("CAMBIAR_").endsWith("@tu-dominio.example");
		assertThat(values.get("CERTBOT_STAGING")).isIn("0", "1");
	}

	@Test
	@DisplayName("REQ-21, AC-24: .gitignore ignora .env y no ignora .env.prod.example; .dockerignore excluye .env")
	void envIsIgnoredAndTemplateIsNot() throws IOException {
		List<String> gitignore = Files.readAllLines(GITIGNORE).stream().map(String::strip).toList();

		assertThat(gitignore).contains(".env", "!.env.prod.example");
		assertThat(gitignore.indexOf("!.env.prod.example")).isGreaterThan(gitignore.indexOf(".env"));
		assertThat(Files.readAllLines(DOCKERIGNORE).stream().map(String::strip)).contains(".env");
		assertThat(Files.exists(Path.of(".env")) && isTracked()).as(".env no debe versionarse").isFalse();
	}

	@Test
	@DisplayName("REQ-22, AC-23: los archivos de despliegue no contienen secretos, endpoints de RDS ni datos reales")
	void deploymentFilesContainNoSecrets() throws IOException {
		Pattern accessKey = Pattern.compile("AKIA[0-9A-Z]{16}");
		Pattern email = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+");
		Pattern assignment = Pattern.compile("(?i)(password|passwd|secret|token)\\s*[=:]\\s*(\\S+)");
		Pattern url = Pattern.compile("https?://([A-Za-z0-9.-]+)");
		List<String> problems = new ArrayList<>();

		for (Path path : deploymentFiles()) {
			String text = read(path);
			if (accessKey.matcher(text).find()) {
				problems.add(path + ": clave de acceso de AWS");
			}
			if (text.contains(".rds.amazonaws.com")) {
				problems.add(path + ": endpoint real de RDS");
			}
			Matcher emails = email.matcher(text);
			while (emails.find()) {
				if (!emails.group().endsWith(".example")) {
					problems.add(path + ": correo real " + emails.group());
				}
			}
			Matcher assignments = assignment.matcher(text);
			while (assignments.find()) {
				String value = assignments.group(2).replaceAll("^['\"`]+", "");
				if (!value.isEmpty() && !value.startsWith("$") && !value.startsWith("CAMBIAR_")
						&& !value.startsWith("<") && !value.startsWith("*")) {
					problems.add(path + ": valor literal en " + assignments.group().strip());
				}
			}
			Matcher urls = url.matcher(text);
			while (urls.find()) {
				String host = urls.group(1);
				if (host.contains(".") && !host.endsWith(".example") && !host.equals("download.docker.com")
						&& !host.equals("127.0.0.1")) {
					problems.add(path + ": URL a un dominio que no es de ejemplo " + host);
				}
			}
		}
		assertThat(problems).isEmpty();
	}

	@Test
	@DisplayName("REQ-26: los scripts empiezan con shebang, usan set -eu y llevan comentario de cabecera")
	void scriptsHaveHeaderAndStrictMode() throws IOException {
		for (Path path : List.of(BOOTSTRAP, CHECK_NGINX, SELECT_MODE, WATCHER)) {
			List<String> lines = Files.readAllLines(path);
			assertThat(lines.get(0)).as(path.toString()).startsWith("#!/");
			assertThat(lines.get(1)).as(path.toString()).startsWith("#");
			assertThat(String.join("\n", lines)).as(path.toString()).containsPattern("(?m)^set -eu(o pipefail)?$");
		}
		assertThat(Files.isExecutable(BOOTSTRAP)).as("bootstrap ejecutable").isTrue();
		assertThat(read(CHECK_NGINX)).contains("--add-host api:127.0.0.1", "nginx -t");
	}

	// ---------------------------------------------------------------------------------------------
	// Utilidades
	// ---------------------------------------------------------------------------------------------

	private static String read(Path path) throws IOException {
		return Files.readString(path);
	}

	/** Quita las líneas completas que son comentario ({@code #}) para no confundir texto con configuración. */
	private static String withoutComments(String text) {
		StringBuilder code = new StringBuilder();
		for (String line : text.split("\n", -1)) {
			if (!line.stripLeading().startsWith("#") || line.startsWith("#!")) {
				code.append(line).append('\n');
			}
		}
		return code.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> composeRoot(Path path) throws IOException {
		Object parsed = new Yaml().load(read(path));
		assertThat(parsed).as("YAML de %s", path).isInstanceOf(Map.class);
		return (Map<String, Object>) parsed;
	}

	private static Map<String, Object> services(Path path) throws IOException {
		return mapOf(composeRoot(path).get("services"));
	}

	private static Map<String, Object> service(Map<String, Object> services, String name) {
		assertThat(services).as("servicios").containsKey(name);
		return mapOf(services.get(name));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapOf(Object value) {
		assertThat(value).isInstanceOf(Map.class);
		return (Map<String, Object>) value;
	}

	private static List<String> strings(Object value) {
		assertThat(value).isInstanceOf(List.class);
		return ((List<?>) value).stream().map(String::valueOf).toList();
	}

	/** Interpreta {@code environment} en forma de lista {@code CLAVE=valor} o de mapa. */
	private static Map<String, String> environment(Map<String, Object> service) {
		Map<String, String> result = new LinkedHashMap<>();
		Object environment = service.get("environment");
		if (environment instanceof List<?> entries) {
			for (Object entry : entries) {
				String text = entry.toString();
				int equals = text.indexOf('=');
				result.put(text.substring(0, equals), text.substring(equals + 1));
			}
		} else if (environment instanceof Map<?, ?> entries) {
			entries.forEach((key, value) -> result.put(key.toString(), String.valueOf(value)));
		}
		return result;
	}

	private static String healthcheckTest(Map<String, Object> service) {
		return String.join(" ", strings(mapOf(service.get("healthcheck")).get("test")));
	}

	private static Object condition(Map<String, Object> services, String service, String dependency) {
		Map<String, Object> dependsOn = mapOf(service(services, service).get("depends_on"));
		return mapOf(dependsOn.get(dependency)).get("condition");
	}

	/** Convierte {@code 2048m}, {@code 2g} o {@code 512k} a MiB. */
	private static long toMib(String limit) {
		Matcher matcher = Pattern.compile("(?i)^(\\d+)([kmg]?)b?$").matcher(limit.strip());
		assertThat(matcher.matches()).as("mem_limit '%s'", limit).isTrue();
		long amount = Long.parseLong(matcher.group(1));
		return switch (matcher.group(2).toLowerCase()) {
			case "g" -> amount * 1024;
			case "k" -> amount / 1024;
			case "m" -> amount;
			default -> amount / (1024 * 1024);
		};
	}

	/** Lee las claves de {@code .env.prod.example} en orden, ignorando comentarios y líneas vacías. */
	private static Map<String, String> envValues() throws IOException {
		Map<String, String> values = new LinkedHashMap<>();
		try (Stream<String> lines = Files.lines(ENV_EXAMPLE)) {
			lines.map(String::strip).filter(line -> !line.isEmpty() && !line.startsWith("#")).forEach(line -> {
				int equals = line.indexOf('=');
				values.put(line.substring(0, equals), line.substring(equals + 1));
			});
		}
		return values;
	}

	/** Indica si {@code .env} figura en el índice de git; sin git disponible se asume que no. */
	private static boolean isTracked() {
		try {
			Process process = new ProcessBuilder("git", "ls-files", "--error-unmatch", ".env")
					.redirectErrorStream(true).start();
			process.getInputStream().readAllBytes();
			return process.waitFor() == 0;
		} catch (IOException | InterruptedException e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			return false;
		}
	}
}
