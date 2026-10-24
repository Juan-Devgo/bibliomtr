package co.edu.uniquindio.bibliomtr.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Extrae título, autores y abstract de un PDF del corpus con Apache PDFBox (spec 001, REQ-8, REQ-10 y AC-8).
 *
 * <p><b>Qué hace.</b>
 * <ul>
 *   <li><b>Abstract:</b> {@link PDFTextStripper} sobre las páginas 1 y 2 con
 *       {@code setParagraphEnd("\n" + PARAGRAPH_MARK + "\n")}, de modo que cada fin de párrafo queda como una línea
 *       que solo contiene {@link AbstractLocator#PARAGRAPH_MARK}, y {@link AbstractLocator} sobre ese texto. Es
 *       {@code null} si no se detecta.</li>
 *   <li><b>Título:</b> {@link PDDocumentInformation#getTitle()}, salvo que esté vacío, contenga {@code _} o tenga
 *       menos de {@value #MIN_TITLE_WORDS} palabras separadas por espacios (p. ej. el identificador de prueba de
 *       imprenta {@code "ET-04-2023-0117_proof 609..645"}). En esos casos, el nombre del archivo sin extensión y sin
 *       el prefijo {@code ^\d+\.\s*} del corpus ({@code "3. Myths….pdf"} → {@code "Myths…"}).</li>
 *   <li><b>Autores:</b> {@link PDDocumentInformation#getAuthor()} separado por {@code ;}, {@code ,} y
 *       {@code " and "}, sin espacios en los extremos y sin elementos vacíos. Lista vacía si no hay.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> PDFBox solo lee el PDF (constitución P2); las heurísticas de título y abstract viven aquí
 * y en {@link AbstractLocator} para que la ingesta no dependa del formato del PDF. Este módulo no depende de
 * {@code api} ni de {@code persistence}.
 *
 * <p>Las instancias no tienen estado mutable compartido y son seguras para uso concurrente.
 */
public final class PdfArticleExtractor {

	/** Última página leída para buscar el abstract. */
	static final int LAST_ABSTRACT_PAGE = 2;

	/** Número mínimo de palabras de un título de metadatos válido. */
	static final int MIN_TITLE_WORDS = 3;

	/** Separador de autores en forma de palabra, como en {@code "Ana Gómez and Luis Pérez"}. */
	private static final String AND_SEPARATOR = " and ";

	private final AbstractLocator abstractLocator;

	/**
	 * Crea el extractor con un {@link AbstractLocator} nuevo.
	 *
	 * <p>Complejidad: O(1).
	 */
	public PdfArticleExtractor() {
		this(new AbstractLocator());
	}

	/**
	 * Crea el extractor con el localizador de abstract indicado.
	 *
	 * <p>Complejidad: O(1).
	 *
	 * @param abstractLocator localizador del abstract; no nulo
	 */
	public PdfArticleExtractor(AbstractLocator abstractLocator) {
		this.abstractLocator = Objects.requireNonNull(abstractLocator, "abstractLocator");
	}

	/**
	 * Lee el PDF y devuelve su título, autores y abstract.
	 *
	 * <p>Complejidad: O(c) en tiempo y espacio, con c = número de caracteres de las páginas 1 y 2 (más el costo
	 * de PDFBox para cargar el documento, proporcional a su tamaño).
	 *
	 * @param pdf ruta del PDF; no nula
	 * @return el artículo extraído; {@code abstractText} es {@code null} si no se detecta
	 * @throws IOException si el archivo no existe o no es un PDF legible
	 */
	public ExtractedArticle extract(Path pdf) throws IOException {
		Objects.requireNonNull(pdf, "pdf");
		try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
			PDDocumentInformation info = document.getDocumentInformation();

			PDFTextStripper stripper = new PDFTextStripper();
			stripper.setStartPage(1);
			stripper.setEndPage(LAST_ABSTRACT_PAGE);
			// Cada fin de párrafo queda como una línea que solo contiene la marca (regla 5 de AbstractLocator).
			stripper.setParagraphEnd("\n" + AbstractLocator.PARAGRAPH_MARK + "\n");
			String firstPagesText = stripper.getText(document);
			String abstractText = abstractLocator.locate(firstPagesText).orElse(null);

			String title = title(info.getTitle(), pdf.getFileName().toString());
			List<String> authors = splitAuthors(info.getAuthor());
			return new ExtractedArticle(title, authors, abstractText);
		}
	}

	/**
	 * El título de los metadatos si es válido ({@link #isValidMetadataTitle}); si no, el título derivado del
	 * nombre del archivo.
	 *
	 * <p>Complejidad: O(t + f), con t = longitud del título de metadatos y f = longitud del nombre.
	 *
	 * @param metadataTitle campo {@code Title} de los metadatos; puede ser nulo
	 * @param fileName nombre del archivo, sin directorio
	 * @return el título, sin espacios en los extremos
	 */
	static String title(String metadataTitle, String fileName) {
		if (isValidMetadataTitle(metadataTitle)) {
			return metadataTitle.strip();
		}
		return titleFromFileName(fileName);
	}

	/**
	 * {@code true} si el título de metadatos no es nulo ni vacío, no contiene {@code _} y tiene al menos
	 * {@link #MIN_TITLE_WORDS} palabras separadas por espacios.
	 *
	 * <p>Complejidad: O(t), con t = longitud del título.
	 */
	static boolean isValidMetadataTitle(String metadataTitle) {
		if (metadataTitle == null || metadataTitle.isBlank() || metadataTitle.indexOf('_') >= 0) {
			return false;
		}
		// Cuenta las corridas de caracteres que no son espacio.
		int words = 0;
		boolean inWord = false;
		for (int i = 0; i < metadataTitle.length(); i++) {
			boolean space = Character.isWhitespace(metadataTitle.charAt(i));
			if (!space && !inWord) {
				words++;
			}
			inWord = !space;
		}
		return words >= MIN_TITLE_WORDS;
	}

	/**
	 * Nombre de archivo sin extensión {@code .pdf} y sin el prefijo {@code ^\d+\.\s*} ({@code "3. Myths.pdf"} →
	 * {@code "Myths"}). Si el resultado queda vacío, devuelve el nombre sin extensión.
	 *
	 * <p>Complejidad: O(f), con f = longitud del nombre.
	 *
	 * @param fileName nombre del archivo, sin directorio
	 * @return el título derivado del nombre
	 */
	static String titleFromFileName(String fileName) {
		String base = fileName;
		int dot = base.lastIndexOf('.');
		if (dot > 0 && base.substring(dot).equalsIgnoreCase(".pdf")) {
			base = base.substring(0, dot);
		}
		// Prefijo numérico del corpus: uno o más dígitos, un punto y espacios opcionales.
		int position = 0;
		while (position < base.length() && Character.isDigit(base.charAt(position))) {
			position++;
		}
		if (position > 0 && position < base.length() && base.charAt(position) == '.') {
			position++;
			while (position < base.length() && Character.isWhitespace(base.charAt(position))) {
				position++;
			}
			String withoutPrefix = base.substring(position).strip();
			if (!withoutPrefix.isEmpty()) {
				return withoutPrefix;
			}
		}
		return base.strip();
	}

	/**
	 * Separa el campo de autores por {@code ;}, {@code ,} y {@code " and "}, con un recorrido manual; quita espacios
	 * en los extremos y descarta los vacíos.
	 *
	 * <p>Ejemplo: {@code "Ana Gómez and Luis Pérez; Eva Ruiz, Juan Díaz"} →
	 * {@code [Ana Gómez, Luis Pérez, Eva Ruiz, Juan Díaz]}.
	 *
	 * <p>Complejidad: O(f) en tiempo y espacio, con f = longitud del campo (el separador {@code " and "} tiene
	 * longitud constante).
	 *
	 * @param authorField campo {@code Author} de los metadatos; puede ser nulo
	 * @return los autores en orden; vacía si el campo es nulo o vacío
	 */
	static List<String> splitAuthors(String authorField) {
		List<String> authors = new ArrayList<>();
		if (authorField == null) {
			return authors;
		}
		int start = 0;
		int i = 0;
		while (i <= authorField.length()) {
			int separatorLength = 0;
			if (i == authorField.length() || authorField.charAt(i) == ';' || authorField.charAt(i) == ',') {
				separatorLength = 1;
			} else if (authorField.startsWith(AND_SEPARATOR, i)) {
				separatorLength = AND_SEPARATOR.length();
			}
			if (separatorLength > 0) {
				addAuthor(authors, authorField.substring(start, i));
				i += separatorLength;
				start = i;
			} else {
				i++;
			}
		}
		return authors;
	}

	/** Agrega el autor sin espacios en los extremos, si no queda vacío. Complejidad: O(a). */
	private static void addAuthor(List<String> authors, String candidate) {
		String author = candidate.strip();
		if (!author.isEmpty()) {
			authors.add(author);
		}
	}

}
