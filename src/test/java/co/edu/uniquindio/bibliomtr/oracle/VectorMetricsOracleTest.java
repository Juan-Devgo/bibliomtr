package co.edu.uniquindio.bibliomtr.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.CosineSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.EuclideanDistance;

/**
 * Oráculo de {@link CosineSimilarity} y {@link EuclideanDistance} contra los operadores de pgvector
 * (spec 001, REQ-6 y AC-6).
 *
 * <p><b>Cómo.</b> Un {@link PostgreSQLContainer} con la imagen fijada de pgvector crea la extensión
 * {@code vector}. Con {@code java.util.Random(42)} (constitución P12) se generan pares de vectores de 768
 * componentes en [−1, 1), la dimensión de SPECTER2 y nomic-embed-text. Para cada par, el coseno de Java debe ser
 * {@code 1 − (a <=> b)} (pgvector da la distancia coseno) y la distancia euclidiana de Java debe ser
 * {@code a <-> b}, con tolerancia 1e-4 porque pgvector calcula en {@code float}.
 *
 * <p><b>Por qué aquí.</b> Los operadores de pgvector están prohibidos en producción; este paquete es el único
 * lugar donde se permiten, como referencia. Falla hasta que se implementen las métricas:
 * {@code ./gradlew studentTest --tests VectorMetricsOracleTest}.
 *
 * <p>Complejidad: O(P · d) en tiempo y O(d) en espacio, con P = número de pares y d = 768.
 */
@Tag("student")
class VectorMetricsOracleTest {

	private static final long SEED = 42L;
	private static final int PAIRS = 50;
	private static final int DIMENSION = 768;
	private static final double TOLERANCE = 1e-4;

	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(TestcontainersConfiguration.PGVECTOR_IMAGE);

	private final CosineSimilarity cosine = new CosineSimilarity();
	private final EuclideanDistance euclidean = new EuclideanDistance();

	@BeforeAll
	static void startDatabase() throws SQLException {
		POSTGRES.start();
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
		}
	}

	@AfterAll
	static void stopDatabase() {
		POSTGRES.stop();
	}

	@Test
	@DisplayName("AC-6: coseno y euclidiana de Java iguales a 1 − (a <=> b) y a <-> b de pgvector (768, semilla 42)")
	void matchesPgvectorOperators() throws SQLException {
		Random random = new Random(SEED);
		try (Connection connection = connect();
				PreparedStatement query = connection.prepareStatement(
						"SELECT 1 - (CAST(? AS vector) <=> CAST(? AS vector)), CAST(? AS vector) <-> CAST(? AS vector)")) {
			for (int pair = 0; pair < PAIRS; pair++) {
				float[] a = randomVector(random);
				float[] b = randomVector(random);
				String literalA = literal(a);
				String literalB = literal(b);
				query.setString(1, literalA);
				query.setString(2, literalB);
				query.setString(3, literalA);
				query.setString(4, literalB);
				double expectedCosine;
				double expectedDistance;
				try (ResultSet row = query.executeQuery()) {
					row.next();
					expectedCosine = row.getDouble(1);
					expectedDistance = row.getDouble(2);
				}
				assertThat(cosine.compare(a, b).value()).as("coseno del par %d", pair)
						.isCloseTo(expectedCosine, within(TOLERANCE));
				assertThat(euclidean.compare(a, b).value()).as("distancia euclidiana del par %d", pair)
						.isCloseTo(expectedDistance, within(TOLERANCE));
			}
		}
	}

	private static Connection connect() throws SQLException {
		return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
	}

	/** Vector de 768 componentes uniformes en [−1, 1). */
	private static float[] randomVector(Random random) {
		float[] vector = new float[DIMENSION];
		for (int i = 0; i < DIMENSION; i++) {
			vector[i] = random.nextFloat() * 2 - 1;
		}
		return vector;
	}

	/** Literal de texto de pgvector: {@code [v0,v1,…]}. */
	private static String literal(float[] vector) {
		StringBuilder text = new StringBuilder("[");
		for (int i = 0; i < vector.length; i++) {
			if (i > 0) {
				text.append(',');
			}
			text.append(vector[i]);
		}
		return text.append(']').toString();
	}

}
