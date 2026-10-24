package co.edu.uniquindio.bibliomtr.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Generador de PDF sintéticos de una página con PDFBox, para probar la extracción sin depender del corpus real
 * (spec 001, REQ-8).
 *
 * <p>El PDF tiene metadatos {@code Title} y {@code Author} opcionales, un título visible opcional en Helvetica de
 * {@value #TITLE_FONT_SIZE} pt y, en el orden en que se agregan, líneas en Helvetica de {@value #BODY_FONT_SIZE}
 * pt: la sección Abstract ({@code Abstract}, sus líneas, {@code Keywords: …} y {@code 1. Introduction}), líneas
 * libres y saltos de párrafo verticales (un espacio en blanco de {@value #PARAGRAPH_GAP_LINES} interlineados, que
 * {@code PDFTextStripper} interpreta como fin de párrafo).
 *
 * <p>Complejidad: O(c) en tiempo y espacio al escribir, con c = número total de caracteres.
 */
public final class SyntheticPdf {

	static final float TITLE_FONT_SIZE = 18;
	static final float BODY_FONT_SIZE = 10;
	static final int PARAGRAPH_GAP_LINES = 4;
	private static final float MARGIN = 50;
	private static final float LEADING = 14;

	/** Marca interna de un salto de párrafo vertical en la lista de líneas. */
	private static final String PARAGRAPH_GAP = null;

	private String metadataTitle;
	private String metadataAuthor;
	private String visibleTitle;
	private final List<String> lines = new ArrayList<>();

	/** Crea un PDF sin metadatos ni texto. */
	public static SyntheticPdf builder() {
		return new SyntheticPdf();
	}

	/** Fija el campo {@code Title} de los metadatos. */
	public SyntheticPdf metadataTitle(String title) {
		this.metadataTitle = title;
		return this;
	}

	/** Fija el campo {@code Author} de los metadatos. */
	public SyntheticPdf metadataAuthor(String author) {
		this.metadataAuthor = author;
		return this;
	}

	/** Agrega un título visible en una fuente mayor, antes de todas las líneas. */
	public SyntheticPdf visibleTitle(String title) {
		this.visibleTitle = title;
		return this;
	}

	/** Agrega la sección Abstract con estas líneas, cerrada por {@code Keywords: …} y {@code 1. Introduction}. */
	public SyntheticPdf abstractSection(String... abstractLines) {
		lines.add("Abstract");
		lines.addAll(List.of(abstractLines));
		lines.add("Keywords: artificial intelligence, education");
		lines.add("1. Introduction");
		return this;
	}

	/** Agrega líneas libres, sin encabezados. */
	public SyntheticPdf lines(String... freeLines) {
		lines.addAll(List.of(freeLines));
		return this;
	}

	/** Agrega un salto de párrafo vertical. */
	public SyntheticPdf paragraphBreak() {
		lines.add(PARAGRAPH_GAP);
		return this;
	}

	/**
	 * Escribe el PDF en {@code directory/fileName}.
	 *
	 * @return la ruta del PDF escrito
	 * @throws IOException si no se puede escribir
	 */
	public Path writeTo(Path directory, String fileName) throws IOException {
		Path file = directory.resolve(fileName);
		try (PDDocument document = new PDDocument()) {
			PDDocumentInformation info = document.getDocumentInformation();
			if (metadataTitle != null) {
				info.setTitle(metadataTitle);
			}
			if (metadataAuthor != null) {
				info.setAuthor(metadataAuthor);
			}

			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				float y = page.getMediaBox().getHeight() - MARGIN;
				if (visibleTitle != null) {
					writeLine(content, font, TITLE_FONT_SIZE, y, visibleTitle);
					y -= 2 * LEADING;
				}
				for (String line : lines) {
					if (line == PARAGRAPH_GAP) {
						y -= PARAGRAPH_GAP_LINES * LEADING;
						continue;
					}
					writeLine(content, font, BODY_FONT_SIZE, y, line);
					y -= LEADING;
				}
			}
			document.save(file.toFile());
		}
		return file;
	}

	private static void writeLine(PDPageContentStream content, PDType1Font font, float size, float y, String text)
			throws IOException {
		content.beginText();
		content.setFont(font, size);
		content.newLineAtOffset(MARGIN, y);
		content.showText(text);
		content.endText();
	}

}
