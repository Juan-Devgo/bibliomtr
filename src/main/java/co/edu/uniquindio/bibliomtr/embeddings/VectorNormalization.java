package co.edu.uniquindio.bibliomtr.embeddings;

import java.util.Objects;

/**
 * Normalización L2 de vectores de embeddings (spec 001, REQ-9 y AC-8a).
 *
 * <p><b>Qué hace.</b> Divide cada componente entre la norma euclidiana del vector:
 * <pre>
 *   ‖v‖₂ = √(Σᵢ vᵢ²),   l2Normalize(v)ᵢ = vᵢ / ‖v‖₂
 * </pre>
 * de modo que el resultado tiene ‖·‖₂ = 1 (salvo redondeo de {@code float}).
 *
 * <p><b>Por qué.</b> El vector del token [CLS] de SPECTER2 se guarda normalizado (Clarifications 2026-10-23),
 * así todos los embeddings de ese modelo quedan sobre la esfera unitaria y la distancia euclidiana entre ellos
 * no depende de la magnitud de la salida de la red. Es parte del pooling del andamiaje, no de las métricas de
 * similitud, que siguen siendo stubs de los estudiantes (constitución P3).
 *
 * <p>La suma de cuadrados se acumula en {@code double} para reducir el error de redondeo con 768 componentes.
 */
public final class VectorNormalization {

	private VectorNormalization() {
	}

	/**
	 * Devuelve una copia de {@code v} con norma L2 igual a 1; no modifica {@code v}.
	 *
	 * <p>Ejemplo: {@code (3, 4)} tiene norma 5 y queda {@code (0.6, 0.8)}.
	 *
	 * <p>Complejidad: O(d) en tiempo y espacio, con d = dimensión del vector.
	 *
	 * @param v vector a normalizar; no nulo
	 * @return un vector nuevo {@code v / ‖v‖₂}
	 * @throws NullPointerException     si {@code v} es nulo
	 * @throws IllegalArgumentException si ‖v‖₂ = 0 (vector vacío o de ceros), porque la división no está
	 *                                  definida
	 */
	public static float[] l2Normalize(float[] v) {
		Objects.requireNonNull(v, "v");
		// Paso 1: ‖v‖₂² = Σ vᵢ², acumulado en double.
		double sumOfSquares = 0.0;
		for (float component : v) {
			sumOfSquares += (double) component * component;
		}
		double norm = Math.sqrt(sumOfSquares);
		if (norm == 0.0) {
			throw new IllegalArgumentException("No se puede normalizar un vector de norma 0");
		}
		// Paso 2: cada componente dividida entre la norma.
		float[] normalized = new float[v.length];
		for (int i = 0; i < v.length; i++) {
			normalized[i] = (float) (v[i] / norm);
		}
		return normalized;
	}
}
