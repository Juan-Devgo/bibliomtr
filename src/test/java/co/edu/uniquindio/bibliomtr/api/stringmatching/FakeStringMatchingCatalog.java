package co.edu.uniquindio.bibliomtr.api.stringmatching;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchOptions;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchResult;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatcher;

/**
 * Algoritmos de búsqueda de patrones falsos para los tests de API (spec 004, REQ-1, REQ-8, REQ-28, REQ-29; AC-3,
 * AC-12, AC-20, AC-27): así esos tests no dependen de que los estudiantes hayan implementado KMP, Boyer-Moore o
 * Rabin-Karp. Los stubs de {@code src/main} no se tocan.
 *
 * <p><b>Búsqueda falsa.</b> Fuerza bruta de test: compara el patrón carácter a carácter en cada s = 0 … n − m. No es
 * ninguno de los 3 algoritmos del requerimiento; solo da posiciones correctas para probar el andamiaje. El fake de
 * Rabin-Karp informa como "colisiones simuladas" los s ≤ n − m cuya ventana empieza por el primer carácter del
 * patrón y que no son ocurrencias, para que el número de colisiones por artículo sea comprobable a mano.
 *
 * <p><b>Modos por algoritmo</b> ({@link StringMatchingAlgorithm}):
 * <ul>
 *   <li>{@link Mode#IMPLEMENTED}: posiciones de la fuerza bruta.</li>
 *   <li>{@link Mode#PENDING_PREPROCESS}: {@code preprocess} lanza "TODO estudiante: …" como un stub.</li>
 *   <li>{@link Mode#PENDING_SEARCH}: {@code preprocess} responde y {@code search} lanza "TODO estudiante: …".</li>
 *   <li>{@link Mode#INVALID}: las posiciones correctas con la primera repetida al final ([p₀, …, p₀]), o [−1] si
 *       no hay ocurrencias.</li>
 *   <li>{@link Mode#PARTIAL}: solo la primera ocurrencia, válida pero distinta de las demás (para
 *       {@code allMatch} = false).</li>
 * </ul>
 * Por defecto todos están implementados.
 *
 * <p><b>Espía.</b> Registra cada llamada ({@link Call}) con el algoritmo, el método, la instancia preprocesada que
 * devolvió o recibió y las opciones, para comprobar un solo {@code preprocess} y un {@code search} por abstract con
 * la misma instancia y la traza desactivada (AC-27).
 *
 * <p>Se registra con {@code @Import(FakeStringMatchingCatalog.Config.class)}: un bean
 * {@code FakeStringMatchingCatalog} configurable y un {@link StringMatchingCatalog} {@code @Primary} que delega en
 * él y reemplaza al de {@code StringMatchingConfig}.
 */
public class FakeStringMatchingCatalog {

	/** Comportamiento de un algoritmo falso. */
	public enum Mode {
		/** Devuelve las posiciones correctas. */
		IMPLEMENTED,
		/** {@code preprocess} lanza "TODO estudiante: …". */
		PENDING_PREPROCESS,
		/** {@code search} lanza "TODO estudiante: …". */
		PENDING_SEARCH,
		/** Devuelve posiciones repetidas (o fuera de rango si no hay ocurrencias). */
		INVALID,
		/** Devuelve solo la primera ocurrencia. */
		PARTIAL
	}

	/**
	 * Preprocesamiento falso; la API lo expone como JSON en la traza.
	 *
	 * @param algorithm slug del algoritmo que lo produjo
	 * @param pattern   patrón recibido
	 * @param q         módulo recibido
	 */
	public record FakePreprocessing(String algorithm, String pattern, long q) {
	}

	/**
	 * Traza falsa: solo se devuelve con {@code recordTrace = true}.
	 *
	 * @param algorithm  slug del algoritmo que la produjo
	 * @param textLength n del texto buscado
	 * @param windows    número de desplazamientos probados, n − m + 1 (0 si n &lt; m)
	 */
	public record FakeTrace(String algorithm, int textLength, int windows) {
	}

	/**
	 * Una llamada registrada por el espía.
	 *
	 * @param algorithm     algoritmo llamado
	 * @param method        {@code preprocess} o {@code search}
	 * @param preprocessed  instancia devuelta por {@code preprocess} o recibida por {@code search}
	 * @param recordTrace   {@code recordTrace} de las opciones recibidas
	 * @param q             q de las opciones recibidas
	 */
	public record Call(StringMatchingAlgorithm algorithm, String method, FakePreprocessing preprocessed,
			boolean recordTrace, long q) {
	}

	private final Map<StringMatchingAlgorithm, Mode> modes = new EnumMap<>(StringMatchingAlgorithm.class);
	private final List<Call> calls = new ArrayList<>();

	/** Crea el catálogo con todos los algoritmos implementados. */
	public FakeStringMatchingCatalog() {
		reset();
	}

	/** Vuelve a dejar todos los algoritmos implementados y olvida las llamadas. */
	public synchronized void reset() {
		for (StringMatchingAlgorithm algorithm : StringMatchingAlgorithm.values()) {
			modes.put(algorithm, Mode.IMPLEMENTED);
		}
		calls.clear();
	}

	/**
	 * Cambia el modo de un algoritmo.
	 *
	 * @param algorithm algoritmo
	 * @param mode      nuevo modo
	 */
	public synchronized void setMode(StringMatchingAlgorithm algorithm, Mode mode) {
		modes.put(algorithm, mode);
	}

	/**
	 * Deja un solo algoritmo implementado y los demás pendientes en {@code preprocess} (AC-20).
	 *
	 * @param implemented el algoritmo que queda implementado
	 */
	public synchronized void onlyImplemented(StringMatchingAlgorithm implemented) {
		for (StringMatchingAlgorithm algorithm : StringMatchingAlgorithm.values()) {
			modes.put(algorithm, algorithm == implemented ? Mode.IMPLEMENTED : Mode.PENDING_PREPROCESS);
		}
	}

	/**
	 * Llamadas registradas a un algoritmo, en orden.
	 *
	 * @param algorithm algoritmo
	 * @return una copia de sus llamadas
	 */
	public synchronized List<Call> calls(StringMatchingAlgorithm algorithm) {
		List<Call> selected = new ArrayList<>();
		for (Call call : calls) {
			if (call.algorithm() == algorithm) {
				selected.add(call);
			}
		}
		return selected;
	}

	/**
	 * Catálogo cuyos 3 algoritmos delegan en este fake.
	 *
	 * @return el catálogo
	 */
	public StringMatchingCatalog catalog() {
		return new StringMatchingCatalog(strategy(StringMatchingAlgorithm.KMP),
				strategy(StringMatchingAlgorithm.BOYER_MOORE), strategy(StringMatchingAlgorithm.RABIN_KARP));
	}

	/** Estrategia falsa de un algoritmo según su modo. */
	private StringMatcher<FakePreprocessing, FakeTrace> strategy(StringMatchingAlgorithm algorithm) {
		return new StringMatcher<>() {

			@Override
			public FakePreprocessing preprocess(String pattern, MatchOptions options) {
				Mode mode = mode(algorithm);
				if (mode == Mode.PENDING_PREPROCESS) {
					throw pending(algorithm, StringMatchingAlgorithm.PREPROCESS_METHOD);
				}
				if (pattern == null || pattern.isEmpty()) {
					throw new IllegalArgumentException("El patrón no puede estar vacío");
				}
				FakePreprocessing preprocessed = new FakePreprocessing(algorithm.slug(), pattern, options.q());
				record(new Call(algorithm, StringMatchingAlgorithm.PREPROCESS_METHOD, preprocessed,
						options.recordTrace(), options.q()));
				return preprocessed;
			}

			@Override
			public MatchResult<FakeTrace> search(String text, FakePreprocessing preprocessed, MatchOptions options) {
				record(new Call(algorithm, StringMatchingAlgorithm.SEARCH_METHOD, preprocessed, options.recordTrace(),
						options.q()));
				Mode mode = mode(algorithm);
				if (mode == Mode.PENDING_SEARCH) {
					throw pending(algorithm, StringMatchingAlgorithm.SEARCH_METHOD);
				}
				String pattern = preprocessed.pattern();
				List<Integer> positions = bruteForce(text, pattern);
				List<Integer> collisions = algorithm == StringMatchingAlgorithm.RABIN_KARP
						? simulatedCollisions(text, pattern, positions)
						: null;
				if (mode == Mode.INVALID) {
					positions = invalid(positions);
				} else if (mode == Mode.PARTIAL && positions.size() > 1) {
					positions = List.of(positions.getFirst());
				}
				int windows = Math.max(0, text.length() - pattern.length() + 1);
				FakeTrace trace = options.recordTrace() ? new FakeTrace(algorithm.slug(), text.length(), windows) : null;
				return new MatchResult<>(positions, collisions, trace);
			}
		};
	}

	/** Modo actual de un algoritmo. */
	private synchronized Mode mode(StringMatchingAlgorithm algorithm) {
		return modes.get(algorithm);
	}

	/** Registra una llamada. */
	private synchronized void record(Call call) {
		calls.add(call);
	}

	/** Excepción de un stub sin implementar en el método dado. */
	private static UnsupportedOperationException pending(StringMatchingAlgorithm algorithm, String method) {
		return new UnsupportedOperationException("TODO estudiante: implementar " + algorithm.pendingLocation(method)
				+ " (ver docs/guia-implementacion.md § 11)");
	}

	/** Fuerza bruta de test: todos los s en [0, n − m] con T[s..s+m) = P, carácter a carácter. */
	static List<Integer> bruteForce(String text, String pattern) {
		List<Integer> positions = new ArrayList<>();
		int n = text.length();
		int m = pattern.length();
		for (int s = 0; s + m <= n; s++) {
			int j = 0;
			while (j < m && text.charAt(s + j) == pattern.charAt(j)) {
				j++;
			}
			if (j == m) {
				positions.add(s);
			}
		}
		return positions;
	}

	/** Colisiones simuladas: s ≤ n − m con T[s] = P[0] que no son ocurrencias. */
	private static List<Integer> simulatedCollisions(String text, String pattern, List<Integer> positions) {
		List<Integer> collisions = new ArrayList<>();
		int next = 0;
		for (int s = 0; s + pattern.length() <= text.length(); s++) {
			boolean occurrence = next < positions.size() && positions.get(next) == s;
			if (occurrence) {
				next++;
			} else if (text.charAt(s) == pattern.charAt(0)) {
				collisions.add(s);
			}
		}
		return collisions;
	}

	/** Posiciones inválidas: la primera repetida al final, o [−1] si no hay ocurrencias. */
	private static List<Integer> invalid(List<Integer> positions) {
		if (positions.isEmpty()) {
			return List.of(-1);
		}
		List<Integer> repeated = new ArrayList<>(positions);
		repeated.add(positions.getFirst());
		return repeated;
	}

	/** Configuración de test que registra el fake y el catálogo {@code @Primary} que delega en él. */
	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		/**
		 * Fake configurable del contexto.
		 *
		 * @return el fake
		 */
		@Bean
		FakeStringMatchingCatalog fakeStringMatchingCatalog() {
			return new FakeStringMatchingCatalog();
		}

		/**
		 * Catálogo principal del contexto.
		 *
		 * @param fake fake configurable
		 * @return el catálogo que delega en el fake
		 */
		@Bean
		@Primary
		StringMatchingCatalog primaryFakeStringMatchingCatalog(FakeStringMatchingCatalog fake) {
			return fake.catalog();
		}

	}

}
