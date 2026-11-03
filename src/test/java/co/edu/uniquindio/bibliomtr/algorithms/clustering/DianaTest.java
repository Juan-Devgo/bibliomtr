package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.EPSILON;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertMerges;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertParentsNotBelowChildren;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertValidDendrogram;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.merge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.clustering.DianaTrace;
import co.edu.uniquindio.bibliomtr.trace.clustering.DianaTrace.DianaIteration;
import co.edu.uniquindio.bibliomtr.trace.clustering.DianaTrace.DianaStep;

/**
 * Tests de estudiante de {@link Diana} (spec 003, REQ-8, REQ-16; AC-5 a AC-9; constitución P10). DIANA es
 * opcional (US-2, P3): estos tests pueden quedar en rojo si no se implementa.
 *
 * <p>Los valores esperados están calculados a mano con diam(C) = max d(a, b) y
 * D(i) = media de d(i, R \ {i}) − media de d(i, S) sobre las matrices de {@link ClusteringCases}, y coinciden con
 * la tabla de {@code docs/guia-implementacion.md}, § 10. Fallan hasta que se implemente el algoritmo:
 * {@code ./gradlew studentTest --tests DianaTest}.
 */
@Tag("student")
class DianaTest {

	private static final String SLUG = "diana";

	private final Diana diana = new Diana();

	@Test
	@DisplayName("AC-5: D₄ se divide a 0.7, 0.5 y 0.2 y da (0, 1, 0.2), (2, 4, 0.5), (3, 5, 0.7)")
	void handCaseMerges() {
		ClusteringResult<DianaTrace> result = diana.cluster(ClusteringCases.d4());

		assertMerges(result.merges(), merge(0, 1, 0.2, 2), merge(2, 4, 0.5, 3), merge(3, 5, 0.7, 4));
	}

	@Test
	@DisplayName("AC-5: la primera división de D₄ elige S = {3} por su media 0.583333 y para con D = [−0.35, −0.35, −0.05]")
	void handCaseFirstSplit() {
		List<DianaStep> steps = diana.cluster(ClusteringCases.d4()).trace().steps();

		assertThat(steps).hasSize(3);
		DianaStep first = steps.get(0);
		assertThat(first.clusters()).isEqualTo(List.of(List.of(0, 1, 2, 3)));
		assertThat(first.diameters()).containsExactly(new double[] {0.7}, within(EPSILON));
		assertThat(first.splitIndex()).isZero();
		assertThat(first.diameter()).isCloseTo(0.7, within(EPSILON));
		assertThat(first.iterations()).hasSize(2);

		DianaIteration start = first.iterations().get(0);
		assertThat(start.splinter()).isEmpty();
		assertThat(start.remainder()).containsExactly(0, 1, 2, 3);
		assertThat(start.averageToRemainder())
				.containsExactly(new double[] {1.4 / 3, 1.1 / 3, 1.25 / 3, 1.75 / 3}, within(EPSILON));
		assertThat(start.averageToSplinter()).containsExactly(new double[] {0, 0, 0, 0}, within(EPSILON));
		assertThat(start.differences())
				.containsExactly(new double[] {1.4 / 3, 1.1 / 3, 1.25 / 3, 1.75 / 3}, within(EPSILON));
		assertThat(start.moved()).isEqualTo(3);

		DianaIteration stop = first.iterations().get(1);
		assertThat(stop.splinter()).containsExactly(3);
		assertThat(stop.remainder()).containsExactly(0, 1, 2);
		assertThat(stop.averageToRemainder()).containsExactly(new double[] {0.35, 0.25, 0.4}, within(EPSILON));
		assertThat(stop.averageToSplinter()).containsExactly(new double[] {0.7, 0.6, 0.45}, within(EPSILON));
		assertThat(stop.differences()).containsExactly(new double[] {-0.35, -0.35, -0.05}, within(EPSILON));
		assertThat(stop.moved()).isEqualTo(-1);

		assertThat(first.splinter()).containsExactly(3);
		assertThat(first.remainder()).containsExactly(0, 1, 2);
	}

	@Test
	@DisplayName("AC-5: las divisiones 2 y 3 de D₄ parten {0,1,2} (S = {2}) y {0,1} (S = {0})")
	void handCaseLaterSplits() {
		List<DianaStep> steps = diana.cluster(ClusteringCases.d4()).trace().steps();

		DianaStep second = steps.get(1);
		assertThat(second.clusters()).isEqualTo(List.of(List.of(0, 1, 2), List.of(3)));
		assertThat(second.diameters()).containsExactly(new double[] {0.5, 0}, within(EPSILON));
		assertThat(second.splitIndex()).isZero();
		assertThat(second.iterations().get(0).moved()).isEqualTo(2);
		assertThat(second.iterations().get(1).differences()).containsExactly(new double[] {-0.3, -0.1}, within(EPSILON));
		assertThat(second.splinter()).containsExactly(2);
		assertThat(second.remainder()).containsExactly(0, 1);

		DianaStep third = steps.get(2);
		assertThat(third.clusters()).isEqualTo(List.of(List.of(0, 1), List.of(2), List.of(3)));
		assertThat(third.diameters()).containsExactly(new double[] {0.2, 0, 0}, within(EPSILON));
		// Con |R| = 1 tras mover el 0 no hay más iteraciones.
		assertThat(third.iterations()).hasSize(1);
		assertThat(third.iterations().get(0).moved()).isZero();
		assertThat(third.splinter()).containsExactly(0);
		assertThat(third.remainder()).containsExactly(1);
	}

	@Test
	@DisplayName("AC-6: 2 artículos dan una sola fusión (0, 1, 0.35, 2)")
	void twoArticles() {
		ClusteringResult<DianaTrace> result = diana.cluster(ClusteringCases.twoArticles());

		assertMerges(result.merges(), merge(0, 1, ClusteringCases.TWO_ARTICLES_DISTANCE, 2));
		assertThat(result.trace().steps()).hasSize(1);
	}

	@Test
	@DisplayName("AC-7: los abstracts idénticos 0 y 2 forman la fusión de altura 0")
	void identicalAbstractsMergeAtZero() {
		ClusteringResult<DianaTrace> result = diana.cluster(ClusteringCases.identicalAbstracts());

		assertMerges(result.merges(), merge(0, 2, 0, 2), merge(1, 3, 0.6, 3));
	}

	@Test
	@DisplayName("AC-8: con todas las distancias en 0.5 se separa siempre el menor índice: (2, 3), (1, 4), (0, 5)")
	void tiesFollowSmallestIndexRule() {
		List<Merge> first = diana.cluster(ClusteringCases.ties()).merges();
		List<Merge> second = diana.cluster(ClusteringCases.ties()).merges();

		assertMerges(first, merge(2, 3, 0.5, 2), merge(1, 4, 0.5, 3), merge(0, 5, 0.5, 4));
		assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("AC-9: 20 artículos dan 19 fusiones válidas y deterministas, sin hijos más altos que su padre")
	void twentyArticles() {
		List<Merge> merges = diana.cluster(ClusteringCases.random20()).merges();

		assertValidDendrogram(SLUG, 20, merges);
		assertParentsNotBelowChildren(20, merges);
		assertThat(diana.cluster(ClusteringCases.random20()).merges()).isEqualTo(merges);
	}

}
