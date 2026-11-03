package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Random;

import co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace.LinkageStep;

/**
 * Matrices y aserciones compartidas por los tests de estudiante de clustering (spec 003, REQ-8; constitución
 * P10 y P12).
 *
 * <p><b>Matrices</b> (todas simétricas, de diagonal 0 y valores en [0,1]):
 * <ul>
 *   <li>{@link #d4()}: la matriz de 4 artículos de {@code docs/guia-implementacion.md}, § 10, con los resultados
 *       calculados a mano (AC-5).</li>
 *   <li>{@link #twoArticles()}: 2 artículos a distancia {@value #TWO_ARTICLES_DISTANCE} (AC-6).</li>
 *   <li>{@link #identicalAbstracts()}: 3 artículos; 0 y 2 tienen abstracts idénticos (d = 0) y el resto está a
 *       0.6 (AC-7).</li>
 *   <li>{@link #ties()}: 4 artículos con todas las distancias en 0.5, exacto en binario, para la regla de
 *       desempate (AC-8).</li>
 *   <li>{@link #random20()}: 20 artículos con {@code Random(42)} (AC-9).</li>
 * </ul>
 * Cada método devuelve una copia nueva, porque el algoritmo puede modificar su entrada.
 *
 * <p>Complejidad: O(N²) en tiempo y espacio por matriz.
 */
final class ClusteringCases {

	/** Tolerancia de las alturas y distancias calculadas. */
	static final double EPSILON = 1e-9;

	/** Distancia del caso de 2 artículos. */
	static final double TWO_ARTICLES_DISTANCE = 0.35;

	private ClusteringCases() {
	}

	/** Matriz D₄ de la guía. */
	static double[][] d4() {
		return new double[][] {
				{0, 0.2, 0.5, 0.7},
				{0.2, 0, 0.3, 0.6},
				{0.5, 0.3, 0, 0.45},
				{0.7, 0.6, 0.45, 0}};
	}

	/** 2 artículos a distancia 0.35. */
	static double[][] twoArticles() {
		return new double[][] {{0, TWO_ARTICLES_DISTANCE}, {TWO_ARTICLES_DISTANCE, 0}};
	}

	/** 3 artículos; 0 y 2 idénticos. */
	static double[][] identicalAbstracts() {
		return new double[][] {{0, 0.6, 0}, {0.6, 0, 0.6}, {0, 0.6, 0}};
	}

	/** 4 artículos con todas las distancias en 0.5. */
	static double[][] ties() {
		double[][] matrix = new double[4][4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				matrix[i][j] = i == j ? 0 : 0.5;
			}
		}
		return matrix;
	}

	/** 20 artículos con distancias de {@code Random(42)} en [0,1). */
	static double[][] random20() {
		Random random = new Random(42);
		double[][] matrix = new double[20][20];
		for (int i = 0; i < 20; i++) {
			for (int j = i + 1; j < 20; j++) {
				double value = random.nextDouble();
				matrix[i][j] = value;
				matrix[j][i] = value;
			}
		}
		return matrix;
	}

	/** Fusión esperada, para escribir los casos en una línea. */
	static Merge merge(int left, int right, double height, int size) {
		return new Merge(left, right, height, size);
	}

	/**
	 * Compara las fusiones con las esperadas: ids y tamaños exactos, alturas con {@link #EPSILON}.
	 *
	 * <p>Complejidad: O(N) en tiempo y O(1) en espacio.
	 */
	static void assertMerges(List<Merge> actual, Merge... expected) {
		assertThat(actual).as("fusiones").hasSize(expected.length);
		for (int k = 0; k < expected.length; k++) {
			Merge merge = actual.get(k);
			assertThat(merge.left()).as("left de la fusión %d", k).isEqualTo(expected[k].left());
			assertThat(merge.right()).as("right de la fusión %d", k).isEqualTo(expected[k].right());
			assertThat(merge.height()).as("altura de la fusión %d", k).isCloseTo(expected[k].height(), within(EPSILON));
			assertThat(merge.size()).as("tamaño de la fusión %d", k).isEqualTo(expected[k].size());
		}
	}

	/**
	 * Dendrograma bien formado de N hojas: lo acepta {@link DendrogramValidator} y la última fusión tiene los N
	 * artículos.
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 */
	static void assertValidDendrogram(String algorithm, int leafCount, List<Merge> merges) {
		DendrogramValidator.validate(algorithm, leafCount, merges);
		assertThat(merges).hasSize(leafCount - 1);
		assertThat(merges.getLast().size()).isEqualTo(leafCount);
	}

	/**
	 * Alturas no decrecientes: Single, Complete y Average Linkage son monótonos.
	 *
	 * <p>Complejidad: O(N) en tiempo y O(1) en espacio.
	 */
	static void assertNonDecreasingHeights(List<Merge> merges) {
		for (int k = 1; k < merges.size(); k++) {
			assertThat(merges.get(k).height()).as("altura de la fusión %d", k)
					.isGreaterThanOrEqualTo(merges.get(k - 1).height());
		}
	}

	/**
	 * La altura de cada fusión es mayor o igual que la de sus hijos (DIANA: el diámetro de un grupo no es menor
	 * que el de sus partes).
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 */
	static void assertParentsNotBelowChildren(int leafCount, List<Merge> merges) {
		double[] heights = new double[leafCount + merges.size()];
		for (int k = 0; k < merges.size(); k++) {
			Merge merge = merges.get(k);
			assertThat(merge.height()).as("altura de la fusión %d frente a sus hijos", k)
					.isGreaterThanOrEqualTo(heights[merge.left()])
					.isGreaterThanOrEqualTo(heights[merge.right()]);
			heights[leafCount + k] = merge.height();
		}
	}

	/**
	 * Compara un paso de una traza aglomerativa con los valores a mano.
	 *
	 * <p>Complejidad: O(k²) en tiempo y O(1) en espacio, con k = grupos vigentes.
	 */
	static void assertStep(LinkageStep step, List<Integer> clusters, List<List<Integer>> members,
			double[][] distances, int left, int right, double distance, int newCluster, List<Integer> remaining,
			double... updatedRow) {
		assertThat(step.clusters()).as("grupos vigentes").isEqualTo(clusters);
		assertThat(step.members()).as("artículos de cada grupo").isEqualTo(members);
		double[][] actual = step.distances();
		assertThat(actual).as("matriz entre grupos").hasNumberOfRows(distances.length);
		for (int i = 0; i < distances.length; i++) {
			assertThat(actual[i]).as("fila %d de la matriz entre grupos", i).containsExactly(distances[i], within(EPSILON));
		}
		assertThat(step.left()).as("left").isEqualTo(left);
		assertThat(step.right()).as("right").isEqualTo(right);
		assertThat(step.distance()).as("distancia").isCloseTo(distance, within(EPSILON));
		assertThat(step.newCluster()).as("grupo nuevo").isEqualTo(newCluster);
		assertThat(step.remaining()).as("grupos que quedan").isEqualTo(remaining);
		assertThat(step.updatedRow()).as("fila recalculada").containsExactly(updatedRow, within(EPSILON));
	}

}
