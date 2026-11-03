package co.edu.uniquindio.bibliomtr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.BoyerMoore;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.KnuthMorrisPratt;
import co.edu.uniquindio.bibliomtr.algorithms.stringmatching.RabinKarp;
import co.edu.uniquindio.bibliomtr.api.stringmatching.StringMatchingCatalog;

/**
 * Infraestructura del Requerimiento 4, coincidencia exacta de cadenas (spec 004): catálogo de los 3 algoritmos.
 *
 * <p><b>Por qué aquí.</b> El módulo {@code algorithms} no conoce Spring; los tests de API reemplazan el catálogo
 * por un fake {@code @Primary} sin algoritmos implementados.
 */
@Configuration(proxyBeanMethods = false)
public class StringMatchingConfig {

	/**
	 * Catálogo con los 3 stubs de {@code algorithms/stringmatching} que escriben los estudiantes (REQ-15).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return el catálogo de producción
	 */
	@Bean
	public StringMatchingCatalog stringMatchingCatalog() {
		return new StringMatchingCatalog(new KnuthMorrisPratt(), new BoyerMoore(), new RabinKarp());
	}

}
