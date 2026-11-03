package co.edu.uniquindio.bibliomtr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uniquindio.bibliomtr.algorithms.clustering.AverageLinkage;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.CompleteLinkage;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.Diana;
import co.edu.uniquindio.bibliomtr.algorithms.clustering.SingleLinkage;
import co.edu.uniquindio.bibliomtr.api.clustering.ClusteringCatalog;

/**
 * Infraestructura del Requerimiento 3, clustering jerárquico (spec 003): catálogo de los 4 algoritmos.
 *
 * <p><b>Por qué aquí.</b> El módulo {@code algorithms} no conoce Spring; los tests de API reemplazan el catálogo
 * por un fake {@code @Primary} sin algoritmos implementados.
 */
@Configuration(proxyBeanMethods = false)
public class ClusteringConfig {

	/**
	 * Catálogo con los 4 stubs de {@code algorithms/clustering} que escriben los estudiantes (REQ-11).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el catálogo de producción
	 */
	@Bean
	public ClusteringCatalog clusteringCatalog() {
		return new ClusteringCatalog(new SingleLinkage(), new CompleteLinkage(), new AverageLinkage(), new Diana());
	}

}
