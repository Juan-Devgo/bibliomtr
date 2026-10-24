package co.edu.uniquindio.bibliomtr.preprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests del preprocesamiento determinista (spec 001, REQ-12 y AC-12; constitución P12).
 *
 * <p>Los valores esperados se calculan a mano a partir de las reglas documentadas en {@link TextPreprocessor}.
 */
class TextPreprocessorTest {

	private static final String SAMPLE = "Machine-Learning in Educación, 2023: the AI models!";
	private static final List<String> SAMPLE_TOKENS = List.of("machine", "learning", "educacion", "ai", "models");

	private final TextPreprocessor preprocessor = new TextPreprocessor();

	@Test
	@DisplayName("AC-12: el ejemplo de la spec produce [machine, learning, educacion, ai, models]")
	void tokenizesSample() {
		assertThat(preprocessor.tokenize(SAMPLE)).containsExactlyElementsOf(SAMPLE_TOKENS);
	}

	@Test
	@DisplayName("AC-12: dos llamadas sobre el mismo texto producen exactamente los mismos tokens")
	void tokenizeIsDeterministicAcrossCalls() {
		List<String> first = preprocessor.tokenize(SAMPLE);
		List<String> second = preprocessor.tokenize(SAMPLE);
		assertThat(second).isEqualTo(first).containsExactlyElementsOf(SAMPLE_TOKENS);
	}

	@Test
	@DisplayName("AC-12: dos instancias distintas producen exactamente los mismos tokens")
	void tokenizeIsDeterministicAcrossInstances() {
		TextPreprocessor other = new TextPreprocessor();
		assertThat(other.tokenize(SAMPLE)).isEqualTo(preprocessor.tokenize(SAMPLE));
	}

	@Test
	@DisplayName("normalize: minúsculas, sin tildes, separadores a espacio, espacios colapsados y recortados")
	void normalizesSample() {
		assertThat(preprocessor.normalize(SAMPLE)).isEqualTo("machine learning in educacion 2023 the ai models");
		assertThat(preprocessor.normalize("  Ñandú   ÜBER\t--\nçà  ")).isEqualTo("nandu uber ca");
		assertThat(preprocessor.normalize("¡¿—…!?")).isEmpty();
		assertThat(preprocessor.normalize("")).isEmpty();
	}

	@Test
	@DisplayName("tokenize: descarta longitud < 2, solo dígitos y stopwords, y conserva repeticiones y alfanuméricos")
	void tokenizeFilters() {
		assertThat(preprocessor.tokenize("A b 42 x9 COVID-19 gpt4 data, data; and the DATA"))
				.containsExactly("x9", "covid", "gpt4", "data", "data", "data");
		assertThat(preprocessor.tokenize("Don't stop: it's ours")).containsExactly("stop");
		assertThat(preprocessor.tokenize("")).isEmpty();
		assertThat(preprocessor.tokenize("the of and 2023")).isEmpty();
	}

	@Test
	@DisplayName("characters: cada carácter del texto normalizado, espacios incluidos")
	void charactersOfNormalizedText() {
		assertThat(preprocessor.characters("Gato, Pato")).containsExactly("g", "a", "t", "o", " ", "p", "a", "t", "o");
		assertThat(preprocessor.characters("The É!")).containsExactly("t", "h", "e", " ", "e");
		assertThat(preprocessor.characters("")).isEmpty();
		assertThat(preprocessor.characters(SAMPLE))
				.isEqualTo(new TextPreprocessor().characters(SAMPLE))
				.hasSize("machine learning in educacion 2023 the ai models".length());
	}

	@Test
	@DisplayName("Las entradas nulas lanzan NullPointerException")
	void rejectsNull() {
		assertThatNullPointerException().isThrownBy(() -> preprocessor.normalize(null));
		assertThatNullPointerException().isThrownBy(() -> preprocessor.tokenize(null));
		assertThatNullPointerException().isThrownBy(() -> preprocessor.characters(null));
	}

	@Test
	@DisplayName("La lista de stopwords es la inglesa de NLTK: 179 palabras distintas, una por línea")
	void stopwordListHas179Words() throws IOException {
		try (InputStream stream = TextPreprocessor.class.getResourceAsStream(TextPreprocessor.STOPWORDS_RESOURCE)) {
			assertThat(stream).isNotNull();
			List<String> lines = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
			assertThat(lines).hasSize(179).doesNotHaveDuplicates().doesNotContain("")
					.startsWith("i", "me", "my", "myself", "we").endsWith("wouldn", "wouldn't");
		}
	}

}
