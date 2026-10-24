package co.edu.uniquindio.bibliomtr.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests de la extracción de título, autores y abstract con PDF sintéticos generados por {@link SyntheticPdf}
 * (spec 001, REQ-8, REQ-10, AC-8 y AC-9).
 */
class PdfArticleExtractorTest {

	private static final String LINE_1 = "Generative artificial intelligence is reshaping assessment in higher";
	private static final String LINE_2 = "education. This review maps the evidence published between 2019 and";
	private static final String LINE_3 = "2024 and proposes principles for the redesign of assessment tasks.";
	private static final String EXPECTED_ABSTRACT = LINE_1 + " " + LINE_2 + " " + LINE_3;

	private final PdfArticleExtractor extractor = new PdfArticleExtractor();

	@TempDir
	Path directory;

	@Test
	@DisplayName("AC-8: un título de metadatos válido se conserva, y el abstract va de Abstract a Keywords")
	void keepsValidMetadataTitle() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("  Generative AI in Higher Education  ")
				.metadataAuthor("Ana Pérez")
				.visibleTitle("Generative AI in Higher Education")
				.abstractSection(LINE_1, LINE_2, LINE_3)
				.lines("The rest of the introduction.")
				.writeTo(directory, "1. Generative AI.pdf");

		ExtractedArticle article = extractor.extract(pdf);

		assertThat(article.title()).isEqualTo("Generative AI in Higher Education");
		assertThat(article.authors()).containsExactly("Ana Pérez");
		assertThat(article.abstractText()).isEqualTo(EXPECTED_ABSTRACT);
		assertThat(article.hasAbstract()).isTrue();
	}

	@Test
	@DisplayName("AC-8: un título de metadatos con _ se reemplaza por el nombre de archivo sin prefijo")
	void replacesTitleWithUnderscore() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("ET-04-2023-0117_proof 609..645")
				.abstractSection(LINE_1, LINE_2, LINE_3)
				.writeTo(directory, "13. Artificial intelligence for education a review.pdf");

		assertThat(extractor.extract(pdf).title()).isEqualTo("Artificial intelligence for education a review");
	}

	@Test
	@DisplayName("AC-8: un título de metadatos de 2 palabras se reemplaza por el nombre de archivo sin prefijo")
	void replacesTwoWordTitle() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("Untitled document")
				.abstractSection(LINE_1, LINE_2, LINE_3)
				.writeTo(directory, "3. Myths mis and preconceptions.pdf");

		assertThat(extractor.extract(pdf).title()).isEqualTo("Myths mis and preconceptions");
	}

	@Test
	@DisplayName("AC-8: sin metadatos ni texto, el título es el nombre de archivo y no hay autores ni abstract")
	void usesFileNameWithoutMetadata() throws IOException {
		Path pdf = SyntheticPdf.builder().writeTo(directory, "12. A Meta Systematic Review.pdf");

		ExtractedArticle article = extractor.extract(pdf);

		assertThat(article.title()).isEqualTo("A Meta Systematic Review");
		assertThat(article.authors()).isEmpty();
		assertThat(article.abstractText()).isNull();
	}

	@Test
	@DisplayName("AC-8: \"Ana Gómez and Luis Pérez; Eva Ruiz, Juan Díaz\" da 4 autores")
	void splitsAuthorsByAndSemicolonAndComma() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("Generative AI in Higher Education")
				.metadataAuthor("Ana Gómez and Luis Pérez; Eva Ruiz, Juan Díaz")
				.abstractSection(LINE_1, LINE_2, LINE_3)
				.writeTo(directory, "2. Authors.pdf");

		assertThat(extractor.extract(pdf).authors())
				.containsExactly("Ana Gómez", "Luis Pérez", "Eva Ruiz", "Juan Díaz");
	}

	@Test
	@DisplayName("AC-9: sin terminador, el abstract se corta en el salto de párrafo vertical")
	void cutsAbstractAtParagraphBreakWithoutTerminator() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("AI technologies for education")
				.lines("A B S T R A C T")
				.lines(LINE_1, LINE_2, LINE_3)
				.paragraphBreak()
				.lines("Since Alan Turing first articulated the promising vision of thinking machines in 1950,",
						"artificial intelligence research has been advanced in many different fields.")
				.writeTo(directory, "9. AI technologies.pdf");

		assertThat(extractor.extract(pdf).abstractText()).isEqualTo(EXPECTED_ABSTRACT);
	}

	@Test
	@DisplayName("AC-9: un PDF sin encabezado Abstract queda con abstractText nulo")
	void returnsNullAbstractWithoutHeader() throws IOException {
		Path pdf = SyntheticPdf.builder()
				.metadataTitle("An Article Without Abstract")
				.metadataAuthor("Ana Pérez")
				.lines(LINE_1, LINE_2, LINE_3)
				.writeTo(directory, "5. Without Abstract.pdf");

		ExtractedArticle article = extractor.extract(pdf);

		assertThat(article.abstractText()).isNull();
		assertThat(article.hasAbstract()).isFalse();
		assertThat(article.title()).isEqualTo("An Article Without Abstract");
	}

	@Test
	@DisplayName("Un archivo que no es PDF lanza IOException")
	void rejectsNonPdf() throws IOException {
		Path notPdf = Files.writeString(directory.resolve("not-a-pdf.pdf"), "plain text");

		assertThatExceptionOfType(IOException.class).isThrownBy(() -> extractor.extract(notPdf));
	}

	@Test
	@DisplayName("Validez del título de metadatos: no vacío, sin _ y con al menos 3 palabras")
	void validatesMetadataTitle() {
		assertThat(PdfArticleExtractor.isValidMetadataTitle("AI technologies for education")).isTrue();
		assertThat(PdfArticleExtractor.isValidMetadataTitle("  Three   word  title ")).isTrue();
		assertThat(PdfArticleExtractor.isValidMetadataTitle("Two words")).isFalse();
		assertThat(PdfArticleExtractor.isValidMetadataTitle("an_identifier with words")).isFalse();
		assertThat(PdfArticleExtractor.isValidMetadataTitle("   ")).isFalse();
		assertThat(PdfArticleExtractor.isValidMetadataTitle(null)).isFalse();
	}

	@Test
	@DisplayName("El nombre de archivo pierde la extensión y el prefijo numérico ^\\d+\\.\\s*")
	void derivesTitleFromFileName() {
		assertThat(PdfArticleExtractor.titleFromFileName("3. Myths mis and preconceptions.pdf"))
				.isEqualTo("Myths mis and preconceptions");
		assertThat(PdfArticleExtractor.titleFromFileName("20.Systematic review.PDF")).isEqualTo("Systematic review");
		assertThat(PdfArticleExtractor.titleFromFileName("Article 2024.pdf")).isEqualTo("Article 2024");
		assertThat(PdfArticleExtractor.titleFromFileName("7. .pdf")).isEqualTo("7.");
	}

	@Test
	@DisplayName("Los autores se separan por ;, , y \" and \" sin espacios en los extremos ni vacíos")
	void splitsAuthors() {
		assertThat(PdfArticleExtractor.splitAuthors(" Qi Xia ")).containsExactly("Qi Xia");
		assertThat(PdfArticleExtractor.splitAuthors("A; B,, C ;")).containsExactly("A", "B", "C");
		assertThat(PdfArticleExtractor.splitAuthors("Olakunle Olayinka and Hemlata Sharma"))
				.containsExactly("Olakunle Olayinka", "Hemlata Sharma");
		assertThat(PdfArticleExtractor.splitAuthors("Alexander Brandon")).containsExactly("Alexander Brandon");
		assertThat(PdfArticleExtractor.splitAuthors("")).isEmpty();
		assertThat(PdfArticleExtractor.splitAuthors(null)).isEmpty();
	}

}
