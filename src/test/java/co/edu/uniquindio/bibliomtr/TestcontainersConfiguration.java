package co.edu.uniquindio.bibliomtr;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Postgres real con pgvector para los tests, misma imagen que {@code compose.dev.yaml}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/** Imagen fijada de Postgres con pgvector; la reutilizan los tests que gestionan su propio contenedor. */
	public static final DockerImageName PGVECTOR_IMAGE =
			DockerImageName.parse("pgvector/pgvector:0.8.7-pg18").asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(PGVECTOR_IMAGE);
	}

}
