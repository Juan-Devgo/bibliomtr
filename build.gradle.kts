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
}

jmh {
	jmhVersion = jmhLibVersion
	warmupIterations = 3
	iterations = 5
	fork = 1
	resultFormat = "JSON"
}
