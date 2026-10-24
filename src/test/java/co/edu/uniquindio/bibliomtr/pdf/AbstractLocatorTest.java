package co.edu.uniquindio.bibliomtr.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests de las reglas de detección del abstract (spec 001, REQ-10 y AC-9).
 *
 * <p>Los textos imitan la salida de {@code PDFTextStripper} con {@code setParagraphEnd("\n¶\n")} para los formatos
 * del corpus: encabezado en su propia línea, "Abstract:" en línea (MDPI), encabezado espaciado a mitad de línea
 * con bloque "Article info" (Elsevier), terminadores espaciados (Wiley) y terminador pegado al final de una línea.
 */
class AbstractLocatorTest {

	private static final String SENTENCE_1 = "Artificial intelligence is changing how universities design assessment.";
	private static final String SENTENCE_2 = "This review maps the evidence published between 2019 and 2024.";
	private static final String EXPECTED = SENTENCE_1 + " " + SENTENCE_2;

	private final AbstractLocator locator = new AbstractLocator();

	@Test
	@DisplayName("Regla 1: el encabezado \"Abstract\" en su línea abre el abstract y \"Keywords\" lo cierra")
	void locatesPlainHeader() {
		String text = """
				A Scoping Review of Generative AI
				Ana Pérez, Luis Gómez
				¶
				Abstract
				%s
				%s
				¶
				Keywords: education, artificial intelligence
				Introduction
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 1: \"Abstract:\" en línea conserva el texto y quita el encabezado y los dos puntos")
	void locatesInlineHeader() {
		String text = """
				Abstract: %s
				%s
				Keywords: assessment; ChatGPT
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 1: \"A R T I C L E I N F O A B S T R A C T\" a mitad de línea abre el abstract")
	void locatesSpacedHeaderMidLine() {
		String text = """
				Received 18 July 2020; licence nc-nd/4.0/).A B S T R A C T %s
				%s
				1. Introduction
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 2: \"Keywords:\" y su lista corta tras el encabezado no entran en el abstract")
	void skipsKeywordListAfterHeader() {
		String longLine1 = "The rapid expansion of Learning Analytics and Artificial Intelligence in Education";
		String longLine2 = "offers new scalable, data-intensive systems but raises concerns about data privacy.";
		String text = """
				A R T I C L E I N F O A B S T R A C T
				¶
				Keywords:
				¶
				Human-centered AI
				AI in education
				Ethical considerations
				¶
				%s
				%s
				1. Introduction
				""".formatted(longLine1, longLine2);

		assertThat(locator.locate(text)).contains(longLine1 + " " + longLine2);
	}

	@Test
	@DisplayName("Regla 3: \"K E Y W O R D S\" es terminador")
	void stopsAtSpacedKeywords() {
		String text = """
				Abstract
				%s
				%s
				K E Y W O R D S
				AI competence frameworks for teachers
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 3: \"1 | INTRODUC TION\" es terminador")
	void stopsAtSpacedNumberedIntroduction() {
		String text = """
				Abstract
				%s
				%s
				1  | INTRODUC TION
				Artificial Intelligence has brought transformative changes in education.
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 4: \"… learning. 1. Introduction\" al final de una línea cierra el abstract en esa línea")
	void stopsAtTrailingIntroduction() {
		String text = """
				Abstract
				%s
				This review maps the evidence on learning.1. Introduction
				Artificial Intelligence in Education concerns mainly about computers.
				""".formatted(SENTENCE_1);

		assertThat(locator.locate(text))
				.contains(SENTENCE_1 + " This review maps the evidence on learning.");
	}

	@Test
	@DisplayName("Regla 5: sin terminador, el abstract termina en la primera marca ¶ después de su inicio")
	void stopsAtParagraphMarkWithoutTerminator() {
		String text = """
				A B S T R A C T
				¶
				%s
				%s
				¶
				Since Alan Turing first articulated the promising vision of thinking machines in 1950.
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).contains(EXPECTED);
	}

	@Test
	@DisplayName("Regla 6: el guion de fin de línea se quita y la palabra se une sin espacio")
	void joinsHyphenatedLineBreak() {
		String text = "ABSTRACT\r\n"
				+ "Generative artificial intelligence transforms the way students and teachers ap-\r\n"
				+ "proach assessment, feedback and the design of learning activities.\r\n"
				+ "Index Terms - education\r\n";

		assertThat(locator.locate(text)).contains("Generative artificial intelligence transforms the way "
				+ "students and teachers approach assessment, feedback and the design of learning activities.");
	}

	@Test
	@DisplayName("Regla 6: un texto sin encabezado Abstract no tiene abstract")
	void returnsEmptyWithoutHeader() {
		String text = """
				A Scoping Review of Generative AI
				%s
				%s
				1 Introduction
				""".formatted(SENTENCE_1, SENTENCE_2);

		assertThat(locator.locate(text)).isEmpty();
	}

	@Test
	@DisplayName("Regla 6: un resultado de menos de 100 caracteres no cuenta como abstract")
	void returnsEmptyWhenTooShort() {
		assertThat(locator.locate("Abstract\nToo short to be an abstract.\nKeywords: none\n")).isEmpty();
	}

	@Test
	@DisplayName("La palabra \"abstract\" en prosa, sin letras espaciadas ni al inicio de línea, no es encabezado")
	void ignoresAbstractWordInProse() {
		String text = "We study abstract reasoning in students. " + SENTENCE_1 + "\n" + SENTENCE_2 + "\n";

		assertThat(locator.locate(text)).isEmpty();
	}

}
