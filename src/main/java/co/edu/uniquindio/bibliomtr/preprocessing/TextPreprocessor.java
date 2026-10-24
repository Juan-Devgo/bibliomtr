package co.edu.uniquindio.bibliomtr.preprocessing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Preprocesamiento determinista del abstract: normalización, tokenización y eliminación de stopwords
 * (spec 001, REQ-12 y AC-12; constitución P12).
 *
 * <p><b>Qué hace.</b> Convierte un texto en la secuencia de símbolos que reciben los algoritmos de
 * similitud:
 * <ol>
 *   <li>{@link #normalize}: minúsculas con {@link Locale#ROOT}, descomposición Unicode NFD sin marcas
 *       diacríticas ({@code "Educación"} → {@code "educacion"}), todo carácter fuera de {@code [a-z0-9]}
 *       pasa a espacio, los espacios consecutivos se colapsan en uno y se quitan los de los extremos.</li>
 *   <li>{@link #tokenize}: corridas maximales de {@code [a-z0-9]} del texto normalizado, en orden de
 *       aparición, descartando las de longitud menor que 2, las formadas solo por dígitos y las stopwords.</li>
 *   <li>{@link #characters}: cada carácter del texto normalizado como un símbolo, espacios incluidos (para la
 *       granularidad {@link EditGranularity#CHARACTERS}).</li>
 * </ol>
 *
 * <p><b>Por qué.</b> Los algoritmos clásicos comparan secuencias de símbolos; si el preprocesamiento
 * cambiara entre ejecuciones, los valores de la documentación no serían reproducibles. Por eso no depende de
 * la configuración regional de la JVM ({@link Locale#ROOT}), no usa expresiones regulares (el recorrido es
 * manual, carácter por carácter) y la lista de stopwords es fija: la lista inglesa de NLTK (179 palabras) en
 * {@code src/main/resources/preprocessing/stopwords-en.txt}. Las entradas de esa lista con apóstrofo (p. ej.
 * {@code "don't"}) nunca coinciden con un token, porque el apóstrofo se convierte en espacio; sus partes
 * ({@code "don"}, {@code "t"}) sí están en la lista o se descartan por longitud.
 *
 * <p>Las instancias son inmutables después de construirse y seguras para uso concurrente.
 *
 * <p>Complejidad de construcción: O(S) en tiempo y espacio, con S = número total de caracteres de la lista de
 * stopwords.
 */
public final class TextPreprocessor {

	/** Recurso del classpath con la lista inglesa de stopwords de NLTK, una palabra por línea. */
	public static final String STOPWORDS_RESOURCE = "/preprocessing/stopwords-en.txt";

	/** Longitud mínima de un token conservado por {@link #tokenize}. */
	public static final int MIN_TOKEN_LENGTH = 2;

	private final Set<String> stopwords;

	/**
	 * Carga la lista de stopwords de {@link #STOPWORDS_RESOURCE} en un {@link HashSet}.
	 *
	 * <p>Complejidad: O(S) en tiempo y espacio, con S = número total de caracteres de la lista.
	 *
	 * @throws IllegalStateException si el recurso no está en el classpath
	 * @throws UncheckedIOException si el recurso no se puede leer
	 */
	public TextPreprocessor() {
		this.stopwords = loadStopwords();
	}

	/**
	 * Normaliza el texto: minúsculas ({@link Locale#ROOT}), NFD sin marcas diacríticas, todo carácter fuera
	 * de {@code [a-z0-9]} → espacio, espacios colapsados y sin espacios en los extremos.
	 *
	 * <p>Ejemplo: {@code "Machine-Learning in Educación, 2023!"} → {@code "machine learning in educacion 2023"}.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio, con n = longitud del texto.
	 *
	 * @param text texto a normalizar; no nulo
	 * @return el texto normalizado; vacío si no contiene ningún carácter de {@code [a-z0-9]}
	 * @throws NullPointerException si {@code text} es nulo
	 */
	public String normalize(String text) {
		Objects.requireNonNull(text, "text");
		// NFD separa cada letra acentuada en la letra base seguida de su marca combinante (é → e + ´).
		String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		StringBuilder normalized = new StringBuilder(decomposed.length());
		boolean pendingSpace = false;
		for (int i = 0; i < decomposed.length(); i++) {
			char c = decomposed.charAt(i);
			if (isCombiningMark(c)) {
				// Las marcas diacríticas se eliminan sin dejar separación: "educación" → "educacion".
				continue;
			}
			if (isAsciiLetterOrDigit(c)) {
				// Un solo espacio entre corridas, y nunca al inicio: colapsa y recorta a la vez.
				if (pendingSpace && normalized.length() > 0) {
					normalized.append(' ');
				}
				pendingSpace = false;
				normalized.append(c);
			} else {
				// Cualquier otro carácter (puntuación, espacio, guion, símbolo) actúa como separador.
				pendingSpace = true;
			}
		}
		return normalized.toString();
	}

	/**
	 * Tokeniza el texto: corridas de {@code [a-z0-9]} de {@link #normalize}(text), en orden de aparición, sin
	 * las de longitud menor que {@link #MIN_TOKEN_LENGTH}, las formadas solo por dígitos ni las stopwords.
	 *
	 * <p>Ejemplo: {@code "Machine-Learning in Educación, 2023: the AI models!"} →
	 * {@code [machine, learning, educacion, ai, models]}.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio, con n = longitud del texto (cada consulta a las stopwords es
	 * O(1) esperado en el {@link HashSet}, sobre tokens de longitud acotada por n).
	 *
	 * @param text texto a tokenizar; no nulo
	 * @return los tokens en orden de aparición, con repeticiones; vacía si no queda ninguno
	 * @throws NullPointerException si {@code text} es nulo
	 */
	public List<String> tokenize(String text) {
		String normalized = normalize(text);
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean onlyDigits = true;
		// Recorrido manual: tras normalize, el único separador posible es el espacio.
		for (int i = 0; i <= normalized.length(); i++) {
			boolean atEnd = i == normalized.length();
			char c = atEnd ? ' ' : normalized.charAt(i);
			if (c != ' ') {
				current.append(c);
				onlyDigits = onlyDigits && c >= '0' && c <= '9';
				continue;
			}
			// Fin de una corrida (o fin del texto): se decide si se conserva.
			if (current.length() > 0) {
				String candidate = current.toString();
				if (candidate.length() >= MIN_TOKEN_LENGTH && !onlyDigits && !stopwords.contains(candidate)) {
					tokens.add(candidate);
				}
				current.setLength(0);
			}
			onlyDigits = true;
		}
		return List.copyOf(tokens);
	}

	/**
	 * Separa {@link #normalize}(text) en caracteres, cada uno como un {@link String} de longitud 1, espacios
	 * incluidos y sin eliminar stopwords.
	 *
	 * <p>Ejemplo: {@code "Gato, Pato"} → {@code [g, a, t, o, " ", p, a, t, o]}.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio, con n = longitud del texto.
	 *
	 * @param text texto a separar; no nulo
	 * @return los caracteres del texto normalizado en orden; vacía si el texto normalizado es vacío
	 * @throws NullPointerException si {@code text} es nulo
	 */
	public List<String> characters(String text) {
		String normalized = normalize(text);
		List<String> symbols = new ArrayList<>(normalized.length());
		for (int i = 0; i < normalized.length(); i++) {
			symbols.add(String.valueOf(normalized.charAt(i)));
		}
		return List.copyOf(symbols);
	}

	/** {@code true} si {@code c} está en {@code [a-z0-9]}. Complejidad: O(1). */
	private static boolean isAsciiLetterOrDigit(char c) {
		return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
	}

	/** {@code true} si {@code c} es una marca combinante Unicode (Mn, Mc o Me). Complejidad: O(1). */
	private static boolean isCombiningMark(char c) {
		int type = Character.getType(c);
		return type == Character.NON_SPACING_MARK
				|| type == Character.COMBINING_SPACING_MARK
				|| type == Character.ENCLOSING_MARK;
	}

	/**
	 * Lee {@link #STOPWORDS_RESOURCE} en UTF-8, una palabra por línea; ignora líneas vacías y espacios en los
	 * extremos.
	 *
	 * <p>Complejidad: O(S) en tiempo y espacio, con S = número total de caracteres de la lista.
	 */
	private static Set<String> loadStopwords() {
		InputStream stream = TextPreprocessor.class.getResourceAsStream(STOPWORDS_RESOURCE);
		if (stream == null) {
			throw new IllegalStateException("No se encontró la lista de stopwords en el classpath: "
					+ STOPWORDS_RESOURCE);
		}
		Set<String> words = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String word = line.strip();
				if (!word.isEmpty()) {
					words.add(word);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer la lista de stopwords " + STOPWORDS_RESOURCE, e);
		}
		return Set.copyOf(words);
	}

}
