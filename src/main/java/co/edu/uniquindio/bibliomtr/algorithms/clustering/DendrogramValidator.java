package co.edu.uniquindio.bibliomtr.algorithms.clustering;

import java.util.ArrayList;
import java.util.List;

/**
 * Revisa que el dendrograma de un algoritmo implementado cumpla la convención de {@link Merge} (REQ-7, AC-10).
 *
 * <p><b>Qué revisa</b>, con N hojas y en este orden; la primera regla que falla lanza
 * {@link InvalidDendrogramException} con el mensaje "El algoritmo &lt;slug&gt; devolvió un dendrograma inválido:
 * &lt;razón&gt;":
 * <ol>
 *   <li>Hay exactamente N − 1 fusiones.</li>
 *   <li>En la fusión k, 0 ≤ left &lt; right &lt; N + k: solo se fusionan hojas o grupos ya creados.</li>
 *   <li>Ningún grupo se fusiona dos veces.</li>
 *   <li>{@code size} es la suma de los tamaños de los dos grupos.</li>
 *   <li>La altura está en [0,1] con margen {@link #HEIGHT_TOLERANCE}; dentro del margen se recorta a [0,1].</li>
 * </ol>
 *
 * <p><b>Por qué.</b> Es la frontera entre el código del estudiante y la interfaz: un dendrograma mal formado no
 * se puede dibujar, y es mejor un 500 que diga qué fusión está mal. No revisa que las alturas sean monótonas ni
 * que las fusiones sean las correctas: eso lo verifican los tests {@code student}, y revisarlo aquí metería
 * lógica de los algoritmos en el andamiaje (constitución P1).
 *
 * <p>Complejidad: O(N) en tiempo y espacio.
 */
public final class DendrogramValidator {

	/**
	 * Margen de redondeo de punto flotante aceptado alrededor de [0,1] para la altura de una fusión.
	 *
	 * <p><b>Por qué.</b> El promedio de UPGMA de distancias iguales a 1 puede dar 1 + 2⁻⁵². Una altura
	 * h ∈ [−1e-9, 1 + 1e-9] se devuelve como min(1, max(0, h)); fuera de ese intervalo, o NaN, es un error.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio por comprobación.
	 */
	public static final double HEIGHT_TOLERANCE = 1e-9;

	private DendrogramValidator() {
	}

	/**
	 * Valida las fusiones y devuelve una copia con las alturas recortadas a [0,1].
	 *
	 * <p>Complejidad: O(N) en tiempo y espacio.
	 *
	 * @param algorithm slug del algoritmo, para el mensaje (p. ej. {@code single-linkage})
	 * @param leafCount número de hojas N (artículos), N ≥ 1
	 * @param merges    fusiones que devolvió el algoritmo; puede ser nula
	 * @return las mismas fusiones, con las alturas dentro de [0,1]
	 * @throws InvalidDendrogramException si alguna regla no se cumple
	 */
	public static List<Merge> validate(String algorithm, int leafCount, List<Merge> merges) {
		int expected = leafCount - 1;
		if (merges == null || merges.size() != expected) {
			throw invalid(algorithm, "%d fusiones para %d artículos; se esperaban %d"
					.formatted(merges == null ? 0 : merges.size(), leafCount, expected));
		}
		// sizes[g] = artículos del grupo g; used[g] = el grupo g ya fue hijo de una fusión.
		int[] sizes = new int[leafCount + expected];
		boolean[] used = new boolean[leafCount + expected];
		for (int leaf = 0; leaf < leafCount; leaf++) {
			sizes[leaf] = 1;
		}
		List<Merge> checked = new ArrayList<>(expected);
		for (int k = 0; k < expected; k++) {
			Merge merge = merges.get(k);
			if (merge == null) {
				throw invalid(algorithm, "la fusión %d es nula".formatted(k));
			}
			int left = merge.left();
			int right = merge.right();
			if (left < 0 || left >= right || right >= leafCount + k) {
				throw invalid(algorithm, "la fusión %d usa un grupo inexistente o fuera de orden (%d, %d)"
						.formatted(k, left, right));
			}
			for (int group : new int[] {left, right}) {
				if (used[group]) {
					throw invalid(algorithm, "el grupo %d se fusiona dos veces".formatted(group));
				}
				used[group] = true;
			}
			int size = sizes[left] + sizes[right];
			if (merge.size() != size) {
				throw invalid(algorithm, "la fusión %d tiene tamaño %d; se esperaba %d".formatted(k, merge.size(), size));
			}
			sizes[leafCount + k] = size;
			double height = merge.height();
			if (Double.isNaN(height) || height < -HEIGHT_TOLERANCE || height > 1.0 + HEIGHT_TOLERANCE) {
				throw invalid(algorithm, "la fusión %d tiene altura %s fuera de [0,1]".formatted(k, height));
			}
			checked.add(new Merge(left, right, Math.min(1.0, Math.max(0.0, height)), size));
		}
		return List.copyOf(checked);
	}

	/**
	 * Excepción con el prefijo común del mensaje.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	private static InvalidDendrogramException invalid(String algorithm, String reason) {
		return new InvalidDendrogramException(
				"El algoritmo %s devolvió un dendrograma inválido: %s".formatted(algorithm, reason));
	}

}
