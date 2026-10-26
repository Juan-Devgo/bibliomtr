package co.edu.uniquindio.bibliomtr.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Detecta el abstract en el texto de las primeras páginas de un artículo (spec 001, REQ-8, REQ-10 y AC-9).
 *
 * <p><b>Entrada.</b> El texto de {@code PDFTextStripper} con {@code setParagraphEnd("\n" + PARAGRAPH_MARK + "\n")}:
 * cada fin de párrafo queda como una línea que solo contiene {@link #PARAGRAPH_MARK}. La <i>forma compacta</i>
 * de una línea es la línea en minúsculas ({@link Locale#ROOT}) y sin espacios.
 *
 * <p><b>Reglas</b>, en este orden:
 * <ol>
 *   <li><b>Encabezado.</b> Vale el primero que aparezca de estos dos: una línea cuya forma compacta empieza por
 *       {@code abstract} ("Abstract", "ABSTRACT", "Abstract:"), con el abstract después de {@code abstract} y de
 *       un {@code :} opcional; o la forma espaciada {@code A B S T R A C T} (letras separadas por espacios, sin
 *       distinguir mayúsculas) en cualquier posición de una línea, como en
 *       "A R T I C L E I N F O A B S T R A C T", con el abstract después de la {@code T} final.</li>
 *   <li><b>Lista de keywords junto al encabezado.</b> Si la primera línea no vacía después del encabezado es un
 *       terminador {@code keywords}, se saltan esa línea y las siguientes de menos de
 *       {@value #KEYWORD_LINE_MAX_LENGTH} caracteres; el abstract empieza en la primera línea de
 *       {@value #KEYWORD_LINE_MAX_LENGTH} caracteres o más.</li>
 *   <li><b>Terminador.</b> El abstract termina antes de la primera línea cuya forma compacta empieza por
 *       {@code keywords}, {@code indexterms}, {@code introduction}, {@code 1.introduction},
 *       {@code 1introduction} o {@code 1|introduction} ("Key words", "K E Y W O R D S", "1 | INTRODUC TION").</li>
 *   <li><b>Terminador al final de una línea.</b> Si la forma compacta de una línea termina en
 *       {@code 1.introduction} ("…learning. 1. Introduction"), el abstract incluye esa línea hasta antes del
 *       "1." y termina ahí.</li>
 *   <li><b>Sin terminador.</b> Si no hay ninguno, el abstract termina en la primera línea {@link #PARAGRAPH_MARK}
 *       después de su inicio.</li>
 *   <li><b>Limpieza.</b> Se quitan las líneas {@link #PARAGRAPH_MARK} y el guion de fin de línea
 *       ({@code "learn-" + "ing"} → {@code "learning"}), y las líneas, sin espacios en los extremos, se unen con un
 *       espacio. Si no hay encabezado o el resultado tiene menos de {@value #MIN_LENGTH} caracteres, no se detecta
 *       abstract.</li>
 * </ol>
 *
 * <p><b>Por qué.</b> La constitución (P5) exige comparar solo el abstract. Es una heurística sobre el texto que
 * devuelve PDFBox, no un algoritmo del requerimiento; las reglas 1 a 5 cubren los formatos de los 20 PDF del
 * corpus (Elsevier con encabezado espaciado y bloque "Article info", Wiley con "K E Y W O R D S", MDPI con
 * "Abstract:" en línea). Si falla en un PDF, el artículo queda marcado como sin abstract y no es seleccionable
 * (REQ-10).
 *
 * <p>Las instancias no tienen estado y son seguras para uso concurrente.
 */
public final class AbstractLocator {

	/** Marca de fin de párrafo: una línea que solo contiene esta marca separa dos párrafos. */
	public static final String PARAGRAPH_MARK = "¶";

	/** Longitud mínima, en caracteres, de un abstract válido. */
	public static final int MIN_LENGTH = 100;

	/** Las líneas de la lista de keywords junto al encabezado tienen menos de estos caracteres. */
	public static final int KEYWORD_LINE_MAX_LENGTH = 60;

	/** Encabezado que abre el abstract, en forma compacta. */
	private static final String HEADER = "abstract";

	/** Terminador que abre la lista de keywords, en forma compacta. */
	private static final String KEYWORDS = "keywords";

	/** Prefijos, en forma compacta, de las líneas que cierran el abstract (regla 3). */
	private static final List<String> TERMINATORS = List.of(
			KEYWORDS, "indexterms", "introduction", "1.introduction", "1introduction", "1|introduction");

	/** Sufijo, en forma compacta, de una línea con el terminador pegado al final (regla 4). */
	private static final String TRAILING_TERMINATOR = "1.introduction";

	/**
	 * Busca el abstract en el texto de las primeras páginas.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio, con n = longitud del texto (cada línea se recorre un número
	 * constante de veces y los patrones comparados tienen longitud constante).
	 *
	 * @param firstPagesText texto extraído de las primeras páginas, con las marcas {@link #PARAGRAPH_MARK}; no
	 *        nulo
	 * @return el abstract, o vacío si no hay encabezado o tiene menos de {@link #MIN_LENGTH} caracteres
	 * @throws NullPointerException si {@code firstPagesText} es nulo
	 */
	public Optional<String> locate(String firstPagesText) {
		Objects.requireNonNull(firstPagesText, "firstPagesText");
		List<String> lines = splitLines(firstPagesText);

		// Regla 1: la primera línea con un encabezado; content[0] es lo que sigue al encabezado en esa línea.
		List<String> content = null;
		for (int i = 0; i < lines.size() && content == null; i++) {
			int start = headerEnd(lines.get(i));
			if (start >= 0) {
				content = new ArrayList<>();
				content.add(lines.get(i).substring(start));
				content.addAll(lines.subList(i + 1, lines.size()));
			}
		}
		if (content == null) {
			return Optional.empty();
		}

		int start = skipKeywordList(content);

		// Reglas 3 y 4: se busca el primer terminador desde el inicio del abstract.
		int end = content.size();
		String trailingPart = null;
		for (int i = start; i < content.size(); i++) {
			String line = content.get(i);
			if (isTerminator(line)) {
				end = i;
				break;
			}
			int cut = trailingTerminatorStart(line);
			if (cut >= 0) {
				end = i;
				trailingPart = line.substring(0, cut);
				break;
			}
		}
		boolean terminatorFound = end < content.size();

		StringBuilder abstractText = new StringBuilder();
		for (int i = start; i < end; i++) {
			String line = content.get(i);
			if (isParagraphMark(line)) {
				// Regla 5: sin terminador, el primer fin de párrafo después del inicio cierra el abstract.
				if (!terminatorFound && abstractText.length() > 0) {
					break;
				}
				continue;
			}
			append(abstractText, line);
		}
		if (trailingPart != null) {
			append(abstractText, trailingPart);
		}

		String result = abstractText.toString();
		return result.length() < MIN_LENGTH ? Optional.empty() : Optional.of(result);
	}

	/**
	 * Regla 2: si la primera línea no vacía de {@code content} es un terminador {@code keywords}, devuelve el índice
	 * de la primera línea posterior de {@value #KEYWORD_LINE_MAX_LENGTH} caracteres o más; si no, 0. Las líneas
	 * {@link #PARAGRAPH_MARK} cuentan como vacías.
	 *
	 * <p>Complejidad: O(n), con n = número total de caracteres de {@code content}.
	 */
	private static int skipKeywordList(List<String> content) {
		int first = 0;
		while (first < content.size() && isBlankOrMark(content.get(first))) {
			first++;
		}
		if (first == content.size() || !compact(content.get(first)).startsWith(KEYWORDS)) {
			return 0;
		}
		int start = first + 1;
		while (start < content.size() && content.get(start).strip().length() < KEYWORD_LINE_MAX_LENGTH) {
			start++;
		}
		return start;
	}

	/**
	 * Regla 1: posición de la línea donde empieza el abstract si la línea tiene un encabezado, o −1. Primero
	 * prueba la forma compacta al inicio de la línea ({@code abstract} y un {@code :} opcional) y después la forma
	 * espaciada {@code A B S T R A C T} en cualquier posición.
	 *
	 * <p>Complejidad: O(l) en tiempo, con l = longitud de la línea (el patrón tiene longitud constante).
	 */
	private static int headerEnd(String line) {
		if (compact(line).startsWith(HEADER)) {
			// Consume las 8 letras de "abstract" saltando espacios, luego espacios y un ":" opcional.
			int position = 0;
			int matched = 0;
			while (matched < HEADER.length()) {
				if (!isSpace(line.charAt(position))) {
					matched++;
				}
				position++;
			}
			position = skipSpaces(line, position);
			if (position < line.length() && line.charAt(position) == ':') {
				position = skipSpaces(line, position + 1);
			}
			return position;
		}
		for (int from = 0; from < line.length(); from++) {
			int end = spacedHeaderEnd(line, from);
			if (end >= 0) {
				return end;
			}
		}
		return -1;
	}

	/**
	 * Si en {@code from} empieza la forma espaciada {@code A B S T R A C T} (cada letra, sin distinguir
	 * mayúsculas, separada de la siguiente por al menos un espacio), devuelve la posición después de la {@code T}
	 * final; si no, −1.
	 *
	 * <p>Complejidad: O(l) en el peor caso, con l = longitud de la línea.
	 */
	private static int spacedHeaderEnd(String line, int from) {
		int position = from;
		for (int k = 0; k < HEADER.length(); k++) {
			if (k > 0) {
				// Entre dos letras debe haber al menos un espacio, para no marcar la palabra "abstract" en prosa.
				int afterSpaces = skipSpaces(line, position);
				if (afterSpaces == position) {
					return -1;
				}
				position = afterSpaces;
			}
			if (position >= line.length()
					|| Character.toLowerCase(line.charAt(position)) != HEADER.charAt(k)) {
				return -1;
			}
			position++;
		}
		return position;
	}

	/**
	 * Regla 4: si la forma compacta de la línea termina en {@code 1.introduction}, devuelve la posición del
	 * {@code 1} en la línea original; si no, −1.
	 *
	 * <p>Complejidad: O(l), con l = longitud de la línea.
	 */
	private static int trailingTerminatorStart(String line) {
		if (!compact(line).endsWith(TRAILING_TERMINATOR)) {
			return -1;
		}
		// Recorre la línea desde el final, emparejando el sufijo carácter por carácter y saltando espacios.
		int matched = 0;
		int position = line.length() - 1;
		while (position >= 0) {
			if (!isSpace(line.charAt(position))) {
				matched++;
				if (matched == TRAILING_TERMINATOR.length()) {
					return position;
				}
			}
			position--;
		}
		return -1;
	}

	/**
	 * Agrega una línea al abstract: sin espacios en los extremos, separada por un espacio, o pegada sin espacio
	 * (y sin el guion) si lo acumulado termina en guion de fin de línea. Ignora las líneas vacías.
	 *
	 * <p>Complejidad: O(l) amortizado, con l = longitud de la línea.
	 */
	private static void append(StringBuilder abstractText, String line) {
		String content = line.strip();
		if (content.isEmpty()) {
			return;
		}
		int length = abstractText.length();
		if (length == 0) {
			abstractText.append(content);
		} else if (abstractText.charAt(length - 1) == '-') {
			// Guion de fin de línea: la palabra continúa en la línea siguiente.
			abstractText.setLength(length - 1);
			abstractText.append(content);
		} else {
			abstractText.append(' ').append(content);
		}
	}

	/** Regla 3: {@code true} si la forma compacta empieza por un terminador. Complejidad: O(l). */
	private static boolean isTerminator(String line) {
		String compact = compact(line);
		for (String terminator : TERMINATORS) {
			if (compact.startsWith(terminator)) {
				return true;
			}
		}
		return false;
	}

	/** {@code true} si la línea solo contiene {@link #PARAGRAPH_MARK}. Complejidad: O(l). */
	private static boolean isParagraphMark(String line) {
		return line.strip().equals(PARAGRAPH_MARK);
	}

	/** {@code true} si la línea está vacía o solo contiene {@link #PARAGRAPH_MARK}. Complejidad: O(l). */
	private static boolean isBlankOrMark(String line) {
		return line.isBlank() || isParagraphMark(line);
	}

	/**
	 * Forma compacta: la línea en minúsculas ({@link Locale#ROOT}) y sin ningún espacio.
	 *
	 * <p>Complejidad: O(l) en tiempo y espacio, con l = longitud de la línea.
	 */
	private static String compact(String line) {
		StringBuilder compact = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (!isSpace(c)) {
				compact.append(c);
			}
		}
		return compact.toString().toLowerCase(Locale.ROOT);
	}

	/**
	 * Separa el texto en líneas por {@code \n}, {@code \r\n} o {@code \r}, con un recorrido manual.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio, con n = longitud del texto.
	 */
	private static List<String> splitLines(String text) {
		List<String> lines = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				lines.add(text.substring(start, i));
				// "\r\n" es un solo salto de línea.
				if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
					i++;
				}
				start = i + 1;
			}
		}
		lines.add(text.substring(start));
		return lines;
	}

	/** Primera posición desde {@code from} que no es un espacio. Complejidad: O(l). */
	private static int skipSpaces(String line, int from) {
		int position = from;
		while (position < line.length() && isSpace(line.charAt(position))) {
			position++;
		}
		return position;
	}

	/** {@code true} para espacios Unicode, incluido el espacio de no separación. Complejidad: O(1). */
	private static boolean isSpace(char c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

}
