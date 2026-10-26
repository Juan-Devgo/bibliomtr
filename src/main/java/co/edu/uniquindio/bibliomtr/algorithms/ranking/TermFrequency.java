package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.Objects;

/**
 * Par (término, frecuencia) que ordenan los tres algoritmos del Requerimiento 2 (REQ-10).
 *
 * <p><b>Qué es.</b> {@code term} es un token ya preprocesado (spec 001) y {@code frequency} su número total
 * de ocurrencias en los abstracts seleccionables del corpus (REQ-1). La frecuencia es la clave de
 * ordenamiento; el término viaja con ella para que el ranking conserve qué palabra corresponde a cada valor.
 *
 * <p><b>Por qué.</b> El código de referencia ordena {@code int[]}; envolver la clave en un par permite
 * aplicar los mismos pasos sin perder el término y comprobar la estabilidad en los empates.
 *
 * <p>Complejidad: construcción O(1) en tiempo y espacio.
 *
 * @param term      término preprocesado, no nulo
 * @param frequency número de ocurrencias, mayor o igual a 0 (Counting Sort indexa un arreglo con ella)
 */
public record TermFrequency(String term, int frequency) {

	/**
	 * Valida el par: el término no puede ser nulo y la frecuencia no puede ser negativa, porque los tres
	 * algoritmos usan la frecuencia como índice o como dígitos de un entero no negativo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public TermFrequency {
		Objects.requireNonNull(term, "El término no puede ser nulo");
		if (frequency < 0) {
			throw new IllegalArgumentException("La frecuencia no puede ser negativa: " + frequency);
		}
	}

}
