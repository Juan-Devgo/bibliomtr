package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertMerges;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertNonDecreasingHeights;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertStep;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.assertValidDendrogram;
import static co.edu.uniquindio.bibliomtr.algorithms.clustering.ClusteringCases.merge;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace;
import co.edu.uniquindio.bibliomtr.trace.clustering.LinkageTrace.LinkageStep;

/**
 * Tests de estudiante de {@link SingleLinkage} (spec 003, REQ-8, REQ-16; AC-5 a AC-9; constitución P10).
 *
 * <p>Los valores esperados están calculados a mano con {@code d(k, h) = min(d(i, h), d(j, h))} sobre las matrices de
 * {@link ClusteringCases} y coinciden con la tabla de {@code docs/guia-implementacion.md}, § 10. Fallan hasta
 * que se implemente el algoritmo: {@code ./gradlew studentTest --tests SingleLinkageTest}.
 */
@Tag("student")
class SingleLinkageTest {

	private static final String SLUG = "single-linkage";

	private final SingleLinkage single = new SingleLinkage();

	@Test
	@DisplayName("AC-5: D₄ da (0, 1, 0.2), (2, 4, 0.3), (3, 5, 0.45)")
	void handCaseMerges() {
		ClusteringResult<LinkageTrace> result = single.cluster(ClusteringCases.d4());

		assertMerges(result.merges(), merge(0, 1, 0.2, 2), merge(2, 4, 0.3, 3), merge(3, 5, 0.45, 4));
	}

	@Test
	@DisplayName("AC-5: la traza de D₄ tiene 3 pasos; el paso 1 recalcula [min(0.5, 0.3), min(0.7, 0.6)] = [0.3, 0.6]")
	void handCaseTrace() {
		List<LinkageStep> steps = single.cluster(ClusteringCases.d4()).trace().steps();

		assertThat(steps).hasSize(3);
		assertStep(steps.get(0), List.of(0, 1, 2, 3), List.of(List.of(0), List.of(1), List.of(2), List.of(3)),
				ClusteringCases.d4(), 0, 1, 0.2, 4, List.of(2, 3), 0.3, 0.6);
		assertStep(steps.get(1), List.of(2, 3, 4), List.of(List.of(2), List.of(3), List.of(0, 1)),
				new double[][] {{0, 0.45, 0.3}, {0.45, 0, 0.6}, {0.3, 0.6, 0}}, 2, 4, 0.3, 5, List.of(3), 0.45);
		assertStep(steps.get(2), List.of(3, 5), List.of(List.of(3), List.of(0, 1, 2)),
				new double[][] {{0, 0.45}, {0.45, 0}}, 3, 5, 0.45, 6, List.of());
	}

	@Test
	@DisplayName("AC-6: 2 artículos dan una sola fusión (0, 1, 0.35, 2)")
	void twoArticles() {
		ClusteringResult<LinkageTrace> result = single.cluster(ClusteringCases.twoArticles());

		assertMerges(result.merges(), merge(0, 1, ClusteringCases.TWO_ARTICLES_DISTANCE, 2));
		assertThat(result.trace().steps()).hasSize(1);
	}

	@Test
	@DisplayName("AC-7: los abstracts idénticos 0 y 2 se fusionan primero a altura 0")
	void identicalAbstractsMergeAtZero() {
		ClusteringResult<LinkageTrace> result = single.cluster(ClusteringCases.identicalAbstracts());

		assertMerges(result.merges(), merge(0, 2, 0, 2), merge(1, 3, 0.6, 3));
	}

	@Test
	@DisplayName("AC-8: con todas las distancias en 0.5 gana el par de ids menores: (0, 1), (2, 3), (4, 5)")
	void tiesFollowLexicographicRule() {
		List<Merge> first = single.cluster(ClusteringCases.ties()).merges();
		List<Merge> second = single.cluster(ClusteringCases.ties()).merges();

		assertMerges(first, merge(0, 1, 0.5, 2), merge(2, 3, 0.5, 2), merge(4, 5, 0.5, 4));
		assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("AC-9: 20 artículos dan 19 fusiones válidas, monótonas y deterministas")
	void twentyArticles() {
		List<Merge> merges = single.cluster(ClusteringCases.random20()).merges();

		assertValidDendrogram(SLUG, 20, merges);
		assertNonDecreasingHeights(merges);
		assertThat(single.cluster(ClusteringCases.random20()).merges()).isEqualTo(merges);
	}

}
