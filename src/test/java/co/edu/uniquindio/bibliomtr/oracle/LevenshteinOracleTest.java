package co.edu.uniquindio.bibliomtr.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.Levenshtein;

/**
 * Oráculo de {@link Levenshtein} contra {@code levenshtein()} de la extensión {@code fuzzystrmatch} de
 * PostgreSQL (spec 001, REQ-6 y AC-6).
 *
 * <p><b>Cómo.</b> Un {@link PostgreSQLContainer} con la imagen fijada de pgvector (que trae las extensiones de
 * {@code contrib}) crea {@code fuzzystrmatch} solo en este contenedor. Con {@code java.util.Random(42)}
 * (constitución P12) se generan pares de cadenas de longitud 0 a 50 sobre el alfabeto a–e; cada cadena se
 * convierte en la secuencia de sus caracteres y la distancia de Java debe ser igual a la de Postgres.
 *
 * <p><b>Por qué aquí.</b> {@code levenshtein()} está prohibido en producción; este paquete es el único lugar
 * donde se permite, como referencia. La longitud se limita a 50 porque {@code levenshtein()} admite como máximo
 * 255 caracteres. Falla hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests LevenshteinOracleTest}.
 *
 * <p>Complejidad: O(P · L²) en tiempo y O(L²) en espacio, con P = número de pares y L = 50.
 */
@Tag("student")
class LevenshteinOracleTest {

	private static final long SEED = 42L;
	private static final int PAIRS = 200;
	private static final int MAX_LENGTH = 50;
	private static final String ALPHABET = "abcde";

	private static final PostgreSQLContainer POSTGRES =
			new PostgreSQLContainer(TestcontainersConfiguration.PGVECTOR_IMAGE);

	private final Levenshtein levenshtein = new Levenshtein();

	@BeforeAll
	static void startDatabase() throws SQLException {
		POSTGRES.start();
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch");
		}
	}

	@AfterAll
	static void stopDatabase() {
		POSTGRES.stop();
	}

	@Test
	@DisplayName("AC-6: la distancia de Java es igual a levenshtein() de Postgres en 200 pares aleatorios (semilla 42)")
	void matchesPostgresLevenshtein() throws SQLException {
		Random random = new Random(SEED);
		try (Connection connection = connect();
				PreparedStatement query = connection.prepareStatement("SELECT levenshtein(?, ?)")) {
			for (int pair = 0; pair < PAIRS; pair++) {
				String a = randomString(random);
				String b = randomString(random);
				query.setString(1, a);
				query.setString(2, b);
				int expected;
				try (ResultSet row = query.executeQuery()) {
					row.next();
					expected = row.getInt(1);
				}
				double actual = levenshtein.compare(characters(a), characters(b)).value();
				assertThat(actual).as("levenshtein('%s', '%s')", a, b).isEqualTo(expected);
			}
		}
	}

	private static Connection connect() throws SQLException {
		return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
	}

	/** Cadena de longitud uniforme en [0, 50] sobre a–e. */
	private static String randomString(Random random) {
		int length = random.nextInt(MAX_LENGTH + 1);
		StringBuilder text = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
		}
		return text.toString();
	}

	/** Secuencia de caracteres de la cadena, cada uno como símbolo. */
	private static List<String> characters(String text) {
		List<String> symbols = new ArrayList<>(text.length());
		for (int i = 0; i < text.length(); i++) {
			symbols.add(String.valueOf(text.charAt(i)));
		}
		return symbols;
	}

}
