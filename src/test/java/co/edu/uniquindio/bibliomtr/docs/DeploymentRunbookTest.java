package co.edu.uniquindio.bibliomtr.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Verifica el runbook de despliegue y la trazabilidad del Requerimiento 5 (spec 005, REQ-23, REQ-24, REQ-25,
 * REQ-28, REQ-29; AC-22, AC-25, AC-26, AC-28, AC-30; constitución P4, P9 y P11).
 *
 * <p><b>Cómo.</b> Lee {@code docs/despliegue-aws.md}, {@code docs/traceability.md} y {@code README.md} desde la
 * raíz del proyecto. Una sección del runbook es el texto desde un encabezado {@code ### } que contiene una
 * palabra clave hasta el siguiente encabezado {@code ##} o {@code ###}.
 *
 * <p><b>Criterio.</b>
 * <ul>
 *   <li>AC-25: el runbook cubre us-east-2, los dos grupos de seguridad, RDS {@code db.t4g.micro}, EC2
 *       {@code c7i-flex.large} con Ubuntu 26.04 LTS, EBS gp3 de 30 GB, Elastic IP y registro A; ningún bloque
 *       de código tiene una línea que empiece con {@code aws }.</li>
 *   <li>REQ-24: el orden de operación (.env, corpus, bootstrap, verificación, actualización, registros,
 *       reversión) está en ese orden.</li>
 *   <li>AC-26: cada fallo de REQ-25 tiene su sección con causa y corrección.</li>
 *   <li>AC-22, AC-30: el runbook documenta {@code OOMKilled}, {@code swapon} y la advertencia de JMH.</li>
 *   <li>AC-28: {@code docs/traceability.md} tiene filas del Requerimiento 5 con la spec 005, módulo
 *       {@code — (infraestructura:} y la sección del documento técnico {@code (pendiente)}.</li>
 * </ul>
 *
 * <p><b>Nota.</b> Gradle registra {@code docs/} como entrada de {@code test}, así que un cambio en el runbook o
 * en la trazabilidad vuelve a ejecutar este test; {@code README.md} no, y requiere {@code --rerun}.
 *
 * <p>Complejidad: O(L) en tiempo y espacio por caso, con L = longitud de los documentos.
 */
class DeploymentRunbookTest {

	private static final Path RUNBOOK = Path.of("docs", "despliegue-aws.md");
	private static final Path TRACEABILITY = Path.of("docs", "traceability.md");
	private static final Path README = Path.of("README.md");

	@Test
	@DisplayName("P4: el runbook declara el uso de IA generativa")
	void runbookDeclaresGenerativeAi() throws IOException {
		assertThat(Files.readString(RUNBOOK)).contains("IA generativa");
	}

	@Test
	@DisplayName("REQ-23, AC-25: el runbook trae los datos de AWS de us-east-2")
	void runbookCoversAwsConsoleData() throws IOException {
		String runbook = Files.readString(RUNBOOK);

		assertThat(runbook).contains("us-east-2", "grupo de seguridad", "0.0.0.0/0", "puerto 5432", "IP pública del operador",
				"Single-AZ", "db.t4g.micro", "bibliomtr", "usuario maestro", "c7i-flex.large", "Ubuntu 26.04 LTS", "gp3",
				"30 GB", "Elastic IP", "registro DNS tipo A");
		assertThat(runbook).containsPattern("SSH\\s*\\|\\s*22");
		assertThat(runbook).containsPattern("HTTP\\s*\\|\\s*80").containsPattern("HTTPS\\s*\\|\\s*443");
	}

	@Test
	@DisplayName("REQ-23, REQ-24, AC-25: ningún bloque de código del runbook empieza con un comando aws")
	void runbookHasNoAwsCommands() throws IOException {
		List<String> offending = new ArrayList<>();
		boolean inBlock = false;
		for (String line : Files.readAllLines(RUNBOOK)) {
			if (line.stripLeading().startsWith("```")) {
				inBlock = !inBlock;
			} else if (inBlock && line.stripLeading().matches("(?i)^(\\$\\s*)?aws(\\s.*)?$")) {
				offending.add(line);
			}
		}
		assertThat(offending).isEmpty();
		assertThat(Files.readString(RUNBOOK)).contains("Nada con `aws` se automatiza");
	}

	@Test
	@DisplayName("REQ-24: el orden de operación va de .env a corpus, bootstrap, verificación, actualización, logs y reversión")
	void runbookListsOperationOrder() throws IOException {
		String runbook = Files.readString(RUNBOOK);
		int start = runbook.indexOf("## 4. Orden de operación");
		assertThat(start).as("sección de orden de operación").isNotNegative();

		List<String> steps = List.of("cp .env.prod.example .env", "`DOMAIN`", "`CERTBOT_EMAIL`", "./corpus",
				"scripts/bootstrap-ec2.sh", "curl -I https://", "/api/actuator/health", "Búsqueda Patrones",
				"git pull", "logs", "Revertir a un commit anterior", "migraciones Flyway");
		int previous = start;
		for (String step : steps) {
			int position = runbook.indexOf(step, previous);
			assertThat(position).as("paso '%s' después de la posición %d", step, previous).isGreaterThanOrEqualTo(previous);
			previous = position;
		}
		assertThat(runbook).contains("no se revierten");
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			grupo de seguridad|puerto 5432|bibliomtr-ec2
			extensión `vector`|V1__enable_pgvector.sql|no ejecute `CREATE EXTENSION`
			DNS sin propagar|CERTBOT_STAGING=1|certbot delete --cert-name
			Memoria agotada|swapon --show|JMH
			Descarga lenta|ollama pull nomic-embed-text:v1.5|READY_TIMEOUT_SECONDS
			""")
	@DisplayName("REQ-25, AC-26: cada fallo previsible tiene una sección con causa y corrección")
	void everyFailureHasCauseAndCorrection(String keyword, String first, String second) throws IOException {
		String section = failureSection(keyword);

		assertThat(section).as("sección de '%s'", keyword).isNotEmpty();
		assertThat(section).contains("Causa:", "Corrección:", first);
		assertThat(section.toLowerCase()).contains(second.toLowerCase());
	}

	@Test
	@DisplayName("AC-22: el runbook documenta OOMKilled, swapon y la recreación de ollama")
	void runbookDocumentsMemoryVerification() throws IOException {
		String runbook = Files.readString(RUNBOOK);

		assertThat(runbook).contains("OOMKilled", "docker inspect", "swapon --show", "--force-recreate ollama",
				"JAVA_TOOL_OPTIONS", "-Xmx768m", "3 328 MiB");
	}

	@Test
	@DisplayName("REQ-26, AC-27: el runbook documenta las validaciones estáticas")
	void runbookDocumentsStaticChecks() throws IOException {
		assertThat(Files.readString(RUNBOOK)).contains(
				"docker compose --env-file .env.prod.example -f compose.prod.yaml config", "bash -n", "shellcheck",
				"scripts/check-nginx.sh",
				"./gradlew test --tests DeploymentInvariantsTest --tests DeploymentRunbookTest");
	}

	@Test
	@DisplayName("REQ-29, AC-30: el runbook advierte de JMH en una vCPU compartida, hardware controlado y registro")
	void runbookWarnsAboutJmhNoise() throws IOException {
		String runbook = Files.readString(RUNBOOK);
		int jmh = runbook.indexOf("## 8. Mediciones JMH en la EC2");
		assertThat(jmh).isNotNegative();
		String section = runbook.substring(jmh);

		assertThat(section).contains("JMH", "vCPU compartida", "hardware controlado", "se registra el hardware",
				"ingesta inicial");
	}

	@Test
	@DisplayName("REQ-28, AC-28: la trazabilidad tiene las filas del Requerimiento 5 con documento técnico pendiente")
	void traceabilityHasRequirementFiveRows() throws IOException {
		List<String> rows = Files.readAllLines(TRACEABILITY).stream()
				.filter(line -> line.startsWith("| Requerimiento 5")).toList();

		assertThat(rows).isNotEmpty();
		for (String row : rows) {
			assertThat(row).contains("| 005 |", "— (infraestructura:", "| — |",
					"`docs/documento-tecnico.md` § Requerimiento 5: Despliegue (pendiente)");
		}
		String traceability = String.join("\n", rows);
		assertThat(traceability).contains("compose.prod.yaml", "web/nginx*.conf", "scripts/bootstrap-ec2.sh",
				"docs/despliegue-aws.md", "DeploymentInvariantsTest", "DeploymentRunbookTest");
		assertThat(Files.readAllLines(TRACEABILITY).stream().filter(line -> line.startsWith("| Requerimiento 4")))
				.isNotEmpty();
	}

	@Test
	@DisplayName("REQ-23: el README enlaza el runbook y lista las variables de .env.prod.example")
	void readmeLinksRunbookAndVariables() throws IOException {
		String readme = Files.readString(README);
		int start = readme.indexOf("## Despliegue en AWS (Requerimiento 5)");
		assertThat(start).as("sección de despliegue del README").isNotNegative();
		int end = readme.indexOf("\n## ", start + 1);
		String section = readme.substring(start, end < 0 ? readme.length() : end);

		assertThat(section).contains("docs/despliegue-aws.md", "scripts/bootstrap-ec2.sh", "SPRING_DATASOURCE_URL",
				"SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD", "BIBLIOMTR_CORPUS_DIR",
				"BIBLIOMTR_OLLAMA_URL", "DOMAIN", "CERTBOT_EMAIL", "CERTBOT_STAGING");
	}

	/** Texto de la sección {@code ### 6.x} (fallos previsibles) cuyo encabezado contiene la palabra clave, hasta el siguiente encabezado. */
	private static String failureSection(String keyword) throws IOException {
		StringBuilder section = new StringBuilder();
		boolean inside = false;
		for (String line : Files.readAllLines(RUNBOOK)) {
			if (line.startsWith("## ") || line.startsWith("### ")) {
				inside = line.startsWith("### 6.") && line.toLowerCase().contains(keyword.toLowerCase());
			}
			if (inside) {
				section.append(line).append('\n');
			}
		}
		return section.toString();
	}
}
