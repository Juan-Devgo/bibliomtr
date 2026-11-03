package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado de la búsqueda de un patrón en un texto: posiciones, colisiones de hash y traza (REQ-2, REQ-25).
 *
 * <p><b>Qué guarda.</b>
 * <ul>
 *   <li>{@code positions}: posiciones iniciales de las ocurrencias exactas, base 0 en {@code char}, ascendentes e
 *       incluidas las solapadas. El andamiaje las revisa con {@link MatchPositionsValidator}; por eso una lista
 *       nula o con elementos nulos se conserva tal cual, para que el validador la rechace con su mensaje.</li>
 *   <li>{@code collisions}: solo en Rabin-Karp, los desplazamientos s cuya ventana tiene el mismo hash que el
 *       patrón con texto distinto, ascendentes; en KMP y Boyer-Moore queda vacía ({@code null} → lista
 *       vacía).</li>
 *   <li>{@code trace}: pasos intermedios de la búsqueda (constitución P7), un record de
 *       {@code co.edu.uniquindio.bibliomtr.trace.stringmatching}; {@code null} si
 *       {@code MatchOptions.recordTrace()} es {@code false}.</li>
 * </ul>
 *
 * <p>Complejidad: construcción O(k + c) en tiempo y espacio por las copias, con k = posiciones y c = colisiones.
 *
 * @param positions  posiciones de las ocurrencias
 * @param collisions colisiones de hash (Rabin-Karp)
 * @param trace      traza de la búsqueda, o {@code null}
 * @param <T>        tipo de la traza del algoritmo
 */
public record MatchResult<T>(List<Integer> positions, List<Integer> collisions, T trace) {

	/**
	 * Copias inmodificables que conservan {@code null}; {@code collisions} nula pasa a lista vacía.
	 *
	 * <p>Complejidad: O(k + c) en tiempo y espacio.
	 */
	public MatchResult {
		positions = positions == null ? null : Collections.unmodifiableList(new ArrayList<>(positions));
		collisions = collisions == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(collisions));
	}

}
