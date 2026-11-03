package co.edu.uniquindio.bibliomtr.evaluation;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.BoyerMoore;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.KnuthMorrisPratt;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchOptions;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchResult;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMoorePreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.BoyerMooreTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace;

/**
 * Benchmark JMH de KMP y Boyer-Moore del Requerimiento 4 (spec 004, REQ-30 y AC-22; constitución P11 y P12).
 *
 * <p><b>Qué mide.</b> El tiempo promedio por operación, en microsegundos, de cada fase por separado
 * (constitución P11): el preprocesamiento del patrón ({@code kmpPreprocess}, {@code boyerMoorePreprocess}) y la
 * búsqueda en el texto con el patrón ya preprocesado ({@code kmpSearch}, {@code boyerMooreSearch}). La búsqueda
 * recibe las tablas de un {@code @State} por algoritmo ({@link KmpState}, {@link BoyerMooreState}) que preprocesa
 * una vez por trial en su {@code @Setup}, fuera de la medición. Rabin-Karp se mide en {@link RabinKarpBenchmark}
 * porque es el único que usa q: así KMP y Boyer-Moore no repiten mediciones con un q que ignoran.
 *
 * <p><b>Entradas fijas (constitución P12).</b> {@link Input} combina n ∈ {1 000, 10 000, 100 000},
 * m ∈ {5, 50} y dos tipos de texto:
 * <ul>
 *   <li>{@code RANDOM}: n caracteres uniformes de {@value Input#ALPHABET} generados con
 *       {@code java.util.Random(42)}; el patrón es el fragmento del propio texto que empieza en la mitad,
 *       texto[n/2, n/2 + m), así que hay al menos 1 ocurrencia;</li>
 *   <li>{@code REPETITIVE}: "a"·n con el patrón "a"·(m − 1) + "b", que no aparece y fuerza muchas comparaciones
 *       en cada alineación.</li>
 * </ul>
 * Las búsquedas usan {@code MatchOptions(101, false)}: sin traza, para no medir el costo de grabarla (REQ-29).
 *
 * <p><b>Parámetros.</b> 1 fork, 5 iteraciones de calentamiento de 1 s, 10 iteraciones de medición de 1 s, modo
 * {@code avgt}, unidad µs. El JSON de resultados trae por método los parámetros n, m y textType, {@code score} y
 * {@code scoreError}.
 *
 * <p><b>Stubs sin implementar.</b> Mientras un algoritmo sea un stub, su {@code preprocess} lanza
 * {@code UnsupportedOperationException("TODO estudiante: …")}: el método de preprocesamiento falla al medirse y
 * el de búsqueda falla en el {@code @Setup} de su estado. Este archivo solo se compila con
 * {@code ./gradlew jmhJar}; {@code build} no ejecuta JMH. Ejecución, con los 3 algoritmos implementados:
 * {@code ./gradlew jmhJar && mkdir -p build/jmh-results && java -jar build/libs/benchmarks.jar
 * '(StringMatchingBenchmark|RabinKarpBenchmark)' -rf json -rff build/jmh-results/string-matching.json}.
 *
 * <p><b>Por qué devolver el resultado.</b> JMH consume el valor devuelto por cada {@code @Benchmark}; así el JIT
 * no puede eliminar el preprocesamiento ni la búsqueda como código muerto.
 *
 * <p>Complejidad: la de cada algoritmo, que declaran los estudiantes en su stub.
 */
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class StringMatchingBenchmark {

	/** Opciones de las mediciones: q por defecto (KMP y Boyer-Moore lo ignoran) y sin traza. */
	static final MatchOptions OPTIONS = MatchOptions.defaults();

	/** Tipo de texto de entrada. */
	public enum TextType {
		/** Caracteres uniformes de a–z y espacio; el patrón se toma de la mitad del texto. */
		RANDOM,
		/** "a"·n con el patrón "a"·(m − 1) + "b". */
		REPETITIVE
	}

	/**
	 * Texto y patrón de una combinación (n, m, tipo de texto), generados una vez por trial. También la usa
	 * {@link RabinKarpBenchmark}, para que los 3 algoritmos midan exactamente las mismas entradas.
	 */
	@State(Scope.Benchmark)
	public static class Input {

		/** Semilla fija del texto aleatorio (constitución P12). */
		static final long SEED = 42L;

		/** Alfabeto del texto aleatorio: a–z y espacio. */
		static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz ";

		/** Longitud n del texto. */
		@Param({"1000", "10000", "100000"})
		public int n;

		/** Longitud m del patrón. */
		@Param({"5", "50"})
		public int m;

		/** Tipo de texto. */
		@Param({"RANDOM", "REPETITIVE"})
		public TextType textType;

		/** Texto donde se busca. */
		public String text;

		/** Patrón buscado. */
		public String pattern;

		/**
		 * Genera el texto y el patrón de la combinación de parámetros, fuera de la medición.
		 *
		 * <p>Complejidad: O(n) en tiempo y espacio.
		 */
		@Setup(Level.Trial)
		public void prepare() {
			char[] chars = new char[n];
			if (textType == TextType.RANDOM) {
				// Paso 1: n caracteres uniformes con la semilla fija.
				Random random = new Random(SEED);
				for (int i = 0; i < n; i++) {
					chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
				}
				text = new String(chars);
				// Paso 2: el patrón es el fragmento que empieza en la mitad: hay al menos 1 ocurrencia.
				pattern = text.substring(n / 2, n / 2 + m);
			} else {
				// "a"·n y "a"·(m − 1) + "b": el patrón no aparece, cada alineación compara hasta la "b".
				Arrays.fill(chars, 'a');
				text = new String(chars);
				pattern = "a".repeat(m - 1) + "b";
			}
		}

	}

	/** Tablas de KMP del patrón de {@link Input}, preprocesadas una vez por trial para medir solo la búsqueda. */
	@State(Scope.Benchmark)
	public static class KmpState {

		/** Algoritmo medido. */
		public final KnuthMorrisPratt kmp = new KnuthMorrisPratt();

		/** Resultado del preprocesamiento del patrón. */
		public KmpPreprocessing preprocessed;

		/**
		 * Preprocesa el patrón fuera de la medición.
		 *
		 * <p>Complejidad: la de {@code KnuthMorrisPratt.preprocess}.
		 *
		 * @param input texto y patrón del trial
		 */
		@Setup(Level.Trial)
		public void preprocess(Input input) {
			preprocessed = kmp.preprocess(input.pattern, OPTIONS);
		}

	}

	/** Tablas de Boyer-Moore del patrón de {@link Input}, preprocesadas una vez por trial. */
	@State(Scope.Benchmark)
	public static class BoyerMooreState {

		/** Algoritmo medido. */
		public final BoyerMoore boyerMoore = new BoyerMoore();

		/** Resultado del preprocesamiento del patrón. */
		public BoyerMoorePreprocessing preprocessed;

		/**
		 * Preprocesa el patrón fuera de la medición.
		 *
		 * <p>Complejidad: la de {@code BoyerMoore.preprocess}.
		 *
		 * @param input texto y patrón del trial
		 */
		@Setup(Level.Trial)
		public void preprocess(Input input) {
			preprocessed = boyerMoore.preprocess(input.pattern, OPTIONS);
		}

	}

	private final KnuthMorrisPratt kmp = new KnuthMorrisPratt();
	private final BoyerMoore boyerMoore = new BoyerMoore();

	/**
	 * Preprocesamiento de KMP: la función de prefijo π del patrón.
	 *
	 * @param input texto y patrón del trial
	 * @return las tablas, consumidas por JMH
	 */
	@Benchmark
	public KmpPreprocessing kmpPreprocess(Input input) {
		return kmp.preprocess(input.pattern, OPTIONS);
	}

	/**
	 * Búsqueda de KMP con π ya calculada.
	 *
	 * @param input texto y patrón del trial
	 * @param state tablas preprocesadas
	 * @return el resultado, consumido por JMH
	 */
	@Benchmark
	public MatchResult<KmpTrace> kmpSearch(Input input, KmpState state) {
		return state.kmp.search(input.text, state.preprocessed, OPTIONS);
	}

	/**
	 * Preprocesamiento de Boyer-Moore: tablas del carácter malo y del sufijo bueno.
	 *
	 * @param input texto y patrón del trial
	 * @return las tablas, consumidas por JMH
	 */
	@Benchmark
	public BoyerMoorePreprocessing boyerMoorePreprocess(Input input) {
		return boyerMoore.preprocess(input.pattern, OPTIONS);
	}

	/**
	 * Búsqueda de Boyer-Moore con las tablas ya calculadas.
	 *
	 * @param input texto y patrón del trial
	 * @param state tablas preprocesadas
	 * @return el resultado, consumido por JMH
	 */
	@Benchmark
	public MatchResult<BoyerMooreTrace> boyerMooreSearch(Input input, BoyerMooreState state) {
		return state.boyerMoore.search(input.text, state.preprocessed, OPTIONS);
	}

}
