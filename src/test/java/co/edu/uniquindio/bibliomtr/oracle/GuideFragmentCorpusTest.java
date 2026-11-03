package co.edu.uniquindio.bibliomtr.oracle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.GuideFragment;
import co.edu.uniquindio.bibliomtr.pdf.ExtractedArticle;
import co.edu.uniquindio.bibliomtr.pdf.PdfArticleExtractor;

/**
 * Comprueba que el ejemplo de la guía ({@link GuideFragment}) es reproducible (spec 004, REQ-12, REQ-32 y AC-11).
 *
 * <p><b>Qué verifica.</b> Sin el PDF: el fragmento tiene como mucho 300 {@code char}, el patrón de 1 a 12 y sus
 * posiciones por {@code indexOf} (incluidas las solapadas) son las de {@link GuideFragment#POSITIONS}, con al
 * menos 2. Con el PDF: el fragmento es parte literal del abstract que extrae {@link PdfArticleExtractor} de
 * {@code corpus/}{@value GuideFragment#PDF_FILE}.
 *
 * <p><b>Por qué en la suite estándar.</b> No llama a los algoritmos, así que pasa con los stubs sin implementar;
 * usa {@code indexOf}, permitido solo en este paquete. Los PDF del corpus no se versionan: sin el archivo, el test
 * del abstract se omite con {@link Assumptions} y el build no falla.
 *
 * <p>Complejidad: O(n · m) en tiempo y O(n) en espacio para el fragmento; la extracción del PDF es lineal en su
 * tamaño.
 */
class GuideFragmentCorpusTest {

	/** Directorio del corpus, relativo al directorio de trabajo de Gradle (la raíz del proyecto). */
	private static final Path CORPUS_DIR = Path.of("corpus");

	private static final int MAX_FRAGMENT_LENGTH = 300;
	private static final int MAX_PATTERN_LENGTH = 12;

	@Test
	@DisplayName("REQ-32: el fragmento tiene hasta 300 char, el patrón hasta 12 y aparece en las posiciones de la guía")
	void fragmentPositionsMatchIndexOf() {
		assertThat(GuideFragment.FRAGMENT).hasSize(GuideFragment.FRAGMENT_LENGTH);
		assertThat(GuideFragment.FRAGMENT.length()).isLessThanOrEqualTo(MAX_FRAGMENT_LENGTH);
		assertThat(GuideFragment.PATTERN.length()).isBetween(1, MAX_PATTERN_LENGTH);
		assertThat(GuideFragment.POSITIONS).hasSizeGreaterThanOrEqualTo(2);
		assertThat(GuideFragment.FRAGMENT.length() - GuideFragment.PATTERN.length() + 1)
				.isEqualTo(GuideFragment.WINDOW_COUNT);

		assertThat(indexOfPositions(GuideFragment.FRAGMENT, GuideFragment.PATTERN))
				.containsExactlyElementsOf(GuideFragment.POSITIONS);
	}

	@Test
	@DisplayName("AC-11, REQ-32: el fragmento es parte literal del abstract extraído del PDF (se omite sin el PDF)")
	void fragmentIsPartOfExtractedAbstract() throws IOException {
		Path pdf = CORPUS_DIR.resolve(GuideFragment.PDF_FILE);
		Assumptions.assumeTrue(Files.isRegularFile(pdf),
				"Sin " + pdf.toAbsolutePath() + ": se omite la comprobación del fragmento sobre el abstract real");

		ExtractedArticle article = new PdfArticleExtractor().extract(pdf);

		assertThat(article.hasAbstract()).as("el PDF tiene abstract").isTrue();
		assertThat(article.abstractText()).contains(GuideFragment.FRAGMENT);
	}

	/** Todas las posiciones de {@code pattern} en {@code text}, incluidas las solapadas, con {@code indexOf}. */
	private static List<Integer> indexOfPositions(String text, String pattern) {
		List<Integer> positions = new ArrayList<>();
		int position = text.indexOf(pattern);
		while (position >= 0) {
			positions.add(position);
			position = text.indexOf(pattern, position + 1);
		}
		return positions;
	}

}
