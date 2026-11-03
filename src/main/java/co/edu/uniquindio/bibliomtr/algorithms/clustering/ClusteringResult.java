package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import java.util.List;

/**
 * Resultado de un algoritmo de clustering jerárquico del Requerimiento 3: dendrograma y traza (REQ-3, REQ-16).
 *
 * <p><b>Qué guarda.</b>
 * <ul>
 *   <li>{@code merges}: las N − 1 fusiones del dendrograma, en el orden de creación de los grupos (ver
 *       {@link Merge}). El andamiaje las revisa con {@link DendrogramValidator} antes de devolverlas.</li>
 *   <li>{@code trace}: los pasos intermedios del cálculo (constitución P7), un record de
 *       {@code co.edu.uniquindio.bibliomtr.trace.clustering}.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> La API dibuja el dendrograma solo con {@code merges} y el visor de traza muestra
 * {@code trace} paso a paso, igual que el documento técnico.
 *
 * <p>Complejidad: construcción O(N) en tiempo y espacio por la copia de la lista.
 *
 * @param merges fusiones del dendrograma; {@code null} se conserva para que el validador lo rechace con su mensaje
 * @param trace  traza de pasos intermedios
 * @param <T>    tipo de la traza del algoritmo
 */
public record ClusteringResult<T>(List<Merge> merges, T trace) {

	/**
	 * Copia defensiva de las fusiones.
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 */
	public ClusteringResult {
		merges = merges == null ? null : List.copyOf(merges);
	}

}
