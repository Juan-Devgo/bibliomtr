plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
	id("me.champeau.jmh") version "0.7.3"
}

group = "co.edu.uniquindio"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

// Versiones fijadas (constitución P12). Las no listadas las gestiona el BOM de Spring Boot.
val pdfboxVersion = "3.0.8"
val onnxruntimeVersion = "1.31.0"
val djlVersion = "0.38.0"
val springdocVersion = "3.1.1"
val jmhLibVersion = "1.37"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.flywaydb:flyway-database-postgresql")
	// Mapeo del tipo vector de pgvector (solo almacenamiento).
	implementation("org.hibernate.orm:hibernate-vector")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

	// PDF: solo extracción de texto, título y autores.
	implementation("org.apache.pdfbox:pdfbox:$pdfboxVersion")

	// Embeddings: ONNX Runtime ejecuta el modelo; el tokenizer de DJL solo tokeniza.
	// El pooling y la métrica de similitud se implementan a mano.
	implementation("com.microsoft.onnxruntime:onnxruntime:$onnxruntimeVersion")
	implementation("ai.djl.huggingface:tokenizers:$djlVersion")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	jmh("org.openjdk.jmh:jmh-core:$jmhLibVersion")
	jmh("org.openjdk.jmh:jmh-generator-annprocess:$jmhLibVersion")
	jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:$jmhLibVersion")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// Spec 001, REQ-7: StubDocumentationTest, StubScaffoldTest, ForbiddenSortUsageTest,
	// ForbiddenSimilarityUsageTest e ImplementationGuideTest leen estos archivos como texto. Sin
	// declararlos, un cambio que solo toca un Javadoc, `docs/` o `web/src` no cambia los `.class` y la
	// tarea queda UP-TO-DATE sin volver a verificar. Se excluye `models/` (el modelo ONNX pesa ~440 MB y
	// ningún test de análisis lo lee).
	inputs.files(
		fileTree("src/main/java"),
		fileTree("src/main/resources") { exclude("models/**") },
		fileTree("web/src"),
		fileTree("docs"),
		file("build.gradle.kts"),
	).withPropertyName("scannedSources").withPathSensitivity(PathSensitivity.RELATIVE)
}

// Spec 001, REQ-7: los tests de los stubs de similitud se separan por etiqueta para que
// `./gradlew test` (y por tanto `check` y `build`) quede en verde con los stubs sin implementar.
// - `student`: casos a mano, bordes y oráculos de los 6 algoritmos; fallan hasta implementarlos.
// - `scaffold`: comprueba que los stubs del andamiaje siguen sin implementar.
tasks.test {
	useJUnitPlatform {
		excludeTags("student", "scaffold")
	}
}

// Tests de los estudiantes: `./gradlew studentTest` (opcional `--tests <Clase>Test`).
// No depende de `check`, así que `build` no los ejecuta.
tasks.register<Test>("studentTest") {
	description = "Ejecuta los tests de estudiante (etiqueta student) de los algoritmos de similitud."
	group = "verification"
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	useJUnitPlatform {
		includeTags("student")
	}
}

// Tests del andamiaje: `./gradlew scaffoldTest`. Tampoco depende de `check`.
tasks.register<Test>("scaffoldTest") {
	description = "Ejecuta los tests del andamiaje (etiqueta scaffold) de los stubs de similitud."
	group = "verification"
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	useJUnitPlatform {
		includeTags("scaffold")
	}
}

// Parámetros fijos de JMH (spec 002, REQ-18; constitución P12): 1 fork, 5 iteraciones de
// calentamiento de 1 s, 10 iteraciones de medición de 1 s, tiempo promedio en milisegundos.
// Deben coincidir con las anotaciones de SortingBenchmark y con JmhParameters.FIXED.
jmh {
	jmhVersion = jmhLibVersion
	fork = 1
	warmupIterations = 5
	warmup = "1s"
	iterations = 10
	timeOnIteration = "1s"
	benchmarkMode = listOf("avgt")
	timeUnit = "ms"
	resultFormat = "JSON"
}

// Nombres fijos de los artefactos: la API corre como app.jar y lanza las mediciones
// como proceso hijo sobre benchmarks.jar (spec 002, REQ-17).
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName = "app.jar"
}

tasks.named<Jar>("jmhJar") {
	archiveFileName = "benchmarks.jar"
}
