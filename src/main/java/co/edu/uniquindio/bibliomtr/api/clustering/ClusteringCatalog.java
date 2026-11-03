package co.edu.uniquindio.bibliomtr.api.clustering;

import java.util.Objects;

import co.edu.uniquindio.bibliomtr.algorithms.clustering.HierarchicalClustering;

/**
 * Registro de las 4 estrategias de clustering jerárquico (spec 003, REQ-11, REQ-18; patrón Strategy del plan).
 *
 * <p><b>Por qué un registro.</b> {@code ClusteringService} recibe un solo bean con los 4 algoritmos; en
 * producción son los stubs de {@code algorithms/clustering} ({@code ClusteringConfig}), y los tests de API lo
 * reemplazan por un catálogo falso {@code @Primary} para no depender de que los estudiantes hayan implementado
 * algo. Las trazas son de tipo comodín porque la API las expone como JSON sin interpretarlas.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param singleLinkage   aglomerativo con la distancia mínima
 * @param completeLinkage aglomerativo con la distancia máxima
 * @param averageLinkage  aglomerativo con la distancia promedio (UPGMA)
 * @param diana           divisivo DIANA
 */
public record ClusteringCatalog(HierarchicalClustering<?> singleLinkage, HierarchicalClustering<?> completeLinkage,
		HierarchicalClustering<?> averageLinkage, HierarchicalClustering<?> diana) {

	/**
	 * Exige las 4 estrategias.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public ClusteringCatalog {
		Objects.requireNonNull(singleLinkage, "singleLinkage");
		Objects.requireNonNull(completeLinkage, "completeLinkage");
		Objects.requireNonNull(averageLinkage, "averageLinkage");
		Objects.requireNonNull(diana, "diana");
	}

	/**
	 * Estrategia de un algoritmo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param algorithm algoritmo pedido
	 * @return su estrategia
	 */
	public HierarchicalClustering<?> strategy(ClusteringAlgorithm algorithm) {
		return switch (algorithm) {
			case SINGLE_LINKAGE -> singleLinkage;
			case COMPLETE_LINKAGE -> completeLinkage;
			case AVERAGE_LINKAGE -> averageLinkage;
			case DIANA -> diana;
		};
	}

}
