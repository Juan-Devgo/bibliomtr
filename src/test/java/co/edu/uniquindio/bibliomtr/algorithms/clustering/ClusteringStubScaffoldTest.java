package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprueba que los 4 stubs de clustering siguen sin implementar (spec 003, REQ-1, REQ-2, REQ-6 y AC-1, AC-2):
 * cada uno lanza {@code UnsupportedOperationException} con un mensaje que empieza por {@code "TODO estudiante:"},
 * el cuerpo de {@code cluster} no tiene más sentencia que ese {@code throw} y su Javadoc deja la complejidad
 * pendiente con {@code "Complejidad: TODO estudiante"}.
 *
 * <p><b>Por qué la etiqueta {@code scaffold}.</b> Describe el andamiaje recién entregado y deja de pasar en
 * cuanto un estudiante implementa un algoritmo; por eso no corre en {@code ./gradlew test} y se ejecuta con
 * {@code ./gradlew scaffoldTest}.
 *
 * <p>Complejidad: O(L) por stub en tiempo y espacio, con L = longitud del archivo.
 */
@Tag("scaffold")
class ClusteringStubScaffoldTest {

	private static final String TODO_PREFIX = "TODO estudiante:";
	private static final String PENDING_COMPLEXITY = "Complejidad: TODO estudiante";
	private static final String METHOD_SIGNATURE = "cluster(double[][] distances) {";

	/** Matriz D₄ de la guía. */
	private static final double[][] D4 = {
			{0, 0.2, 0.5, 0.7},
			{0.2, 0, 0.3, 0.6},
			{0.5, 0.3, 0, 0.45},
			{0.7, 0.6, 0.45, 0}};

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"})
	@DisplayName("AC-1: cada stub lanza UnsupportedOperationException con \"TODO estudiante:\"")
	void stubIsPending(String className) {
		HierarchicalClustering<?> algorithm = switch (className) {
			case "SingleLinkage" -> new SingleLinkage();
			case "CompleteLinkage" -> new CompleteLinkage();
			case "AverageLinkage" -> new AverageLinkage();
			default -> new Diana();
		};

		assertThatThrownBy(() -> algorithm.cluster(D4))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageStartingWith(TODO_PREFIX);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"})
	@DisplayName("AC-2: el cuerpo de cluster solo lanza la excepción, sin lógica del algoritmo")
	void stubBodyOnlyThrows(String className) throws IOException {
		String source = ClusteringStubDocumentationTest.source(className);
		int start = source.indexOf(METHOD_SIGNATURE);
		assertThat(start).as("firma de cluster en %s", className).isNotNegative();
		int end = source.indexOf("\n\t}", start);
		String body = source.substring(start + METHOD_SIGNATURE.length(), end).strip();

		assertThat(body).startsWith("throw new UnsupportedOperationException(");
		assertThat(body.chars().filter(c -> c == ';').count()).as("sentencias de %s", className).isEqualTo(1);
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = {"SingleLinkage", "CompleteLinkage", "AverageLinkage", "Diana"})
	@DisplayName("AC-1: el Javadoc de cada stub deja la complejidad como \"Complejidad: TODO estudiante\"")
	void stubJavadocLeavesComplexityPending(String className) throws IOException {
		assertThat(ClusteringStubDocumentationTest.classJavadoc(className)).contains(PENDING_COMPLEXITY);
	}

}
