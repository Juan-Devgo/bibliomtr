package co.edu.uniquindio.bibliomtr.api.similarity;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import co.edu.uniquindio.bibliomtr.algorithms.similarity.CorpusSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.SequenceSimilarity;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.SimilarityResult;
import co.edu.uniquindio.bibliomtr.algorithms.similarity.VectorSimilarity;

/**
 * Algoritmos de similitud falsos para los tests de API (spec 001, REQ-14, REQ-20; AC-13, AC-15, AC-17, AC-19):
 * así esos tests no dependen de que los estudiantes hayan implementado algo.
 *
 * <p><b>Modos por estrategia</b> ({@link Strategy}):
 * <ul>
 *   <li>{@link Mode#IMPLEMENTED}: entradas iguales dan similitud 1 y distintas, el valor fijo
 *       {@link Strategy#similarity()} de la estrategia (simétrico por construcción). {@code value} es la misma
 *       similitud y la traza es un {@link FakeTrace} con los tamaños de las entradas.</li>
 *   <li>{@link Mode#PENDING}: lanza {@link UnsupportedOperationException} con el mismo prefijo "TODO estudiante:"
 *       que los stubs.</li>
 *   <li>{@link Mode#OUT_OF_RANGE}: devuelve similitud {@value #OUT_OF_RANGE_SIMILARITY}, para probar el control de
 *       [0,1].</li>
 *   <li>{@link Mode#ROUNDING_ABOVE_ONE}: devuelve similitud 1 + 1e-12, dentro de la tolerancia de redondeo.</li>
 * </ul>
 * Por defecto todas están implementadas. Registra el tamaño del último corpus que recibió TF-IDF y la última
 * secuencia de edición que recibió Levenshtein.
 *
 * <p>Se registra con {@code @Import(FakeSimilarityCatalog.Config.class)}: un bean {@code FakeSimilarityCatalog}
 * configurable y un {@link SimilarityCatalog} {@code @Primary} que delega en él y reemplaza al de
 * {@code SimilarityConfig}.
 */
public class FakeSimilarityCatalog {

	/** Similitud fuera de rango del modo {@link Mode#OUT_OF_RANGE}. */
	public static final double OUT_OF_RANGE_SIMILARITY = 1.5;

	/** Similitud del modo {@link Mode#ROUNDING_ABOVE_ONE}: 1 más un error de redondeo dentro de la tolerancia. */
	public static final double ROUNDING_ABOVE_ONE_SIMILARITY = 1.0 + 1e-12;

	/** Comportamiento de una estrategia falsa. */
	public enum Mode {
		/** Devuelve valores fijos en [0,1]. */
		IMPLEMENTED,
		/** Lanza "TODO estudiante: …" como un stub sin implementar. */
		PENDING,
		/** Devuelve una similitud mayor que 1. */
		OUT_OF_RANGE,
		/** Devuelve 1 + 1e-12, como un coseno con error de redondeo. */
		ROUNDING_ABOVE_ONE
	}

	/** Las 6 estrategias del catálogo, con la similitud fija entre entradas distintas. */
	public enum Strategy {
		LEVENSHTEIN("Levenshtein", 0.25),
		NEEDLEMAN_WUNSCH("NeedlemanWunsch", 0.5),
		TFIDF_COSINE("TfIdfCosineSimilarity", 0.125),
		JACCARD("Jaccard", 0.375),
		COSINE("CosineSimilarity", 0.75),
		EUCLIDEAN("EuclideanDistance", 0.625);

		private final String stubClass;
		private final double similarity;

		Strategy(String stubClass, double similarity) {
			this.stubClass = stubClass;
			this.similarity = similarity;
		}

		/**
		 * Similitud fija entre entradas distintas.
		 *
		 * @return valor en (0,1)
		 */
		public double similarity() {
			return similarity;
		}
	}

	/**
	 * Traza falsa: solo los tamaños de las entradas.
	 *
	 * @param strategy estrategia que la produjo
	 * @param sizeA    longitud de la primera entrada
	 * @param sizeB    longitud de la segunda entrada
	 */
	public record FakeTrace(String strategy, int sizeA, int sizeB) {
	}

	private final Map<Strategy, Mode> modes = new EnumMap<>(Strategy.class);
	private volatile int lastCorpusSize = -1;
	private volatile List<String> lastEditSequence = List.of();

	/** Crea el catálogo con todas las estrategias implementadas. */
	public FakeSimilarityCatalog() {
		reset();
	}

	/** Vuelve a dejar todas las estrategias implementadas y olvida lo registrado. */
	public synchronized void reset() {
		for (Strategy strategy : Strategy.values()) {
			modes.put(strategy, Mode.IMPLEMENTED);
		}
		lastCorpusSize = -1;
		lastEditSequence = List.of();
	}

	/**
	 * Cambia el modo de una estrategia.
	 *
	 * @param strategy estrategia
	 * @param mode     nuevo modo
	 */
	public synchronized void setMode(Strategy strategy, Mode mode) {
		modes.put(strategy, mode);
	}

	/**
	 * Deja una sola estrategia implementada y las demás pendientes (AC-17).
	 *
	 * @param implemented la estrategia que queda implementada
	 */
	public synchronized void onlyImplemented(Strategy implemented) {
		for (Strategy strategy : Strategy.values()) {
			modes.put(strategy, strategy == implemented ? Mode.IMPLEMENTED : Mode.PENDING);
		}
	}

	/**
	 * Número de documentos del último corpus que recibió TF-IDF.
	 *
	 * @return el tamaño, o −1 si no se llamó
	 */
	public int lastCorpusSize() {
		return lastCorpusSize;
	}

	/**
	 * Última secuencia de edición (primer argumento) que recibió Levenshtein.
	 *
	 * @return la secuencia; vacía si no se llamó
	 */
	public List<String> lastEditSequence() {
		return lastEditSequence;
	}

	/**
	 * Catálogo cuyas 6 estrategias delegan en este fake.
	 *
	 * @return el catálogo
	 */
	public SimilarityCatalog catalog() {
		SequenceSimilarity<FakeTrace> levenshtein = (a, b) -> {
			lastEditSequence = List.copyOf(a);
			return result(Strategy.LEVENSHTEIN, a.equals(b), a.size(), b.size());
		};
		SequenceSimilarity<FakeTrace> needlemanWunsch =
				(a, b) -> result(Strategy.NEEDLEMAN_WUNSCH, a.equals(b), a.size(), b.size());
		CorpusSimilarity<FakeTrace> tfIdfCosine = (a, b, corpus) -> {
			lastCorpusSize = corpus.size();
			return result(Strategy.TFIDF_COSINE, a.equals(b), a.size(), b.size());
		};
		SequenceSimilarity<FakeTrace> jaccard = (a, b) -> result(Strategy.JACCARD, a.equals(b), a.size(), b.size());
		VectorSimilarity<FakeTrace> cosine =
				(a, b) -> result(Strategy.COSINE, Arrays.equals(a, b), a.length, b.length);
		VectorSimilarity<FakeTrace> euclidean =
				(a, b) -> result(Strategy.EUCLIDEAN, Arrays.equals(a, b), a.length, b.length);
		return new SimilarityCatalog(levenshtein, needlemanWunsch, tfIdfCosine, jaccard, cosine, euclidean);
	}

	/** Resultado de una estrategia según su modo. */
	private SimilarityResult<FakeTrace> result(Strategy strategy, boolean equal, int sizeA, int sizeB) {
		Mode mode;
		synchronized (this) {
			mode = modes.get(strategy);
		}
		double similarity = switch (mode) {
			case PENDING -> throw new UnsupportedOperationException(
					"TODO estudiante: implementar " + strategy.stubClass + ".compare (ver docs/guia-implementacion.md)");
			case OUT_OF_RANGE -> OUT_OF_RANGE_SIMILARITY;
			case ROUNDING_ABOVE_ONE -> ROUNDING_ABOVE_ONE_SIMILARITY;
			case IMPLEMENTED -> equal ? 1.0 : strategy.similarity();
		};
		return new SimilarityResult<>(similarity, similarity, new FakeTrace(strategy.name(), sizeA, sizeB));
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
		FakeSimilarityCatalog fakeSimilarityCatalog() {
			return new FakeSimilarityCatalog();
		}

		/**
		 * Catálogo principal del contexto.
		 *
		 * @param fake fake configurable
		 * @return el catálogo que delega en el fake
		 */
		@Bean
		@Primary
		SimilarityCatalog primaryFakeSimilarityCatalog(FakeSimilarityCatalog fake) {
			return fake.catalog();
		}

	}

}
