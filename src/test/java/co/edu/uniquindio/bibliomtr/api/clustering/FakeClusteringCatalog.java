package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringResult;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.HierarchicalClustering;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.Merge;

/**
 * Algoritmos de clustering falsos para los tests de API (spec 003, REQ-11, REQ-18; AC-10, AC-11, AC-16): así
 * esos tests no dependen de que los estudiantes hayan implementado algo.
 *
 * <p><b>Modos por algoritmo</b> ({@link ClusteringAlgorithm}):
 * <ul>
 *   <li>{@link Mode#IMPLEMENTED}: dendrograma en cadena válido, fusión 0 = (0, 1) y fusión k = (k + 1, N + k − 1),
 *       con altura d(0, k + 1) y tamaño k + 2; la traza es un {@link FakeTrace}.</li>
 *   <li>{@link Mode#PENDING}: lanza {@link UnsupportedOperationException} con el mismo prefijo "TODO estudiante:"
 *       que los stubs.</li>
 *   <li>{@link Mode#INVALID}: la misma cadena con la última fusión repetida (N fusiones para N artículos).</li>
 * </ul>
 * Por defecto todos están implementados. Registra la última matriz de distancias recibida.
 *
 * <p>Se registra con {@code @Import(FakeClusteringCatalog.Config.class)}: un bean {@code FakeClusteringCatalog}
 * configurable y un {@link ClusteringCatalog} {@code @Primary} que delega en él y reemplaza al de
 * {@code ClusteringConfig}.
 */
public class FakeClusteringCatalog {

	/** Comportamiento de un algoritmo falso. */
	public enum Mode {
		/** Devuelve un dendrograma válido. */
		IMPLEMENTED,
		/** Lanza "TODO estudiante: …" como un stub sin implementar. */
		PENDING,
		/** Devuelve N fusiones para N artículos. */
		INVALID
	}

	/**
	 * Traza falsa: el algoritmo y el número de artículos.
	 *
	 * @param algorithm slug del algoritmo que la produjo
	 * @param leaves    número de artículos
	 */
	public record FakeTrace(String algorithm, int leaves) {
	}

	private final Map<ClusteringAlgorithm, Mode> modes = new EnumMap<>(ClusteringAlgorithm.class);
	private volatile double[][] lastDistances;

	/** Crea el catálogo con todos los algoritmos implementados. */
	public FakeClusteringCatalog() {
		reset();
	}

	/** Vuelve a dejar todos los algoritmos implementados y olvida lo registrado. */
	public synchronized void reset() {
		for (ClusteringAlgorithm algorithm : ClusteringAlgorithm.values()) {
			modes.put(algorithm, Mode.IMPLEMENTED);
		}
		lastDistances = null;
	}

	/**
	 * Cambia el modo de un algoritmo.
	 *
	 * @param algorithm algoritmo
	 * @param mode      nuevo modo
	 */
	public synchronized void setMode(ClusteringAlgorithm algorithm, Mode mode) {
		modes.put(algorithm, mode);
	}

	/**
	 * Deja un solo algoritmo implementado y los demás pendientes (AC-16).
	 *
	 * @param implemented el algoritmo que queda implementado
	 */
	public synchronized void onlyImplemented(ClusteringAlgorithm implemented) {
		for (ClusteringAlgorithm algorithm : ClusteringAlgorithm.values()) {
			modes.put(algorithm, algorithm == implemented ? Mode.IMPLEMENTED : Mode.PENDING);
		}
	}

	/**
	 * Última matriz de distancias recibida por cualquier algoritmo.
	 *
	 * @return una copia de la matriz, o {@code null} si no se llamó
	 */
	public double[][] lastDistances() {
		double[][] distances = lastDistances;
		return distances == null ? null : copy(distances);
	}

	/**
	 * Catálogo cuyos 4 algoritmos delegan en este fake.
	 *
	 * @return el catálogo
	 */
	public ClusteringCatalog catalog() {
		return new ClusteringCatalog(strategy(ClusteringAlgorithm.SINGLE_LINKAGE),
				strategy(ClusteringAlgorithm.COMPLETE_LINKAGE), strategy(ClusteringAlgorithm.AVERAGE_LINKAGE),
				strategy(ClusteringAlgorithm.DIANA));
	}

	/** Estrategia falsa de un algoritmo según su modo. */
	private HierarchicalClustering<FakeTrace> strategy(ClusteringAlgorithm algorithm) {
		return distances -> {
			lastDistances = copy(distances);
			Mode mode;
			synchronized (this) {
				mode = modes.get(algorithm);
			}
			if (mode == Mode.PENDING) {
				throw new UnsupportedOperationException(
						"TODO estudiante: implementar " + algorithm.pendingLocation() + " (ver docs/guia-implementacion.md)");
			}
			List<Merge> merges = chain(distances);
			if (mode == Mode.INVALID) {
				merges.add(merges.getLast());
			}
			return new ClusteringResult<>(merges, new FakeTrace(algorithm.slug(), distances.length));
		};
	}

	/** Dendrograma en cadena válido para N hojas. */
	private static List<Merge> chain(double[][] distances) {
		int n = distances.length;
		List<Merge> merges = new ArrayList<>(n - 1);
		merges.add(new Merge(0, 1, distances[0][1], 2));
		for (int k = 1; k < n - 1; k++) {
			merges.add(new Merge(k + 1, n + k - 1, distances[0][k + 1], k + 2));
		}
		return merges;
	}

	/** Copia profunda de una matriz. */
	private static double[][] copy(double[][] matrix) {
		double[][] copy = new double[matrix.length][];
		for (int i = 0; i < matrix.length; i++) {
			copy[i] = matrix[i].clone();
		}
		return copy;
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
		FakeClusteringCatalog fakeClusteringCatalog() {
			return new FakeClusteringCatalog();
		}

		/**
		 * Catálogo principal del contexto.
		 *
		 * @param fake fake configurable
		 * @return el catálogo que delega en el fake
		 */
		@Bean
		@Primary
		ClusteringCatalog primaryFakeClusteringCatalog(FakeClusteringCatalog fake) {
			return fake.catalog();
		}

	}

}
