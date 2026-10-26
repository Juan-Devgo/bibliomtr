package co.edu.uniquindio.bibliomtr.trace.similarity;

import java.util.List;

/**
 * Traza del coseno con TF-IDF: vocabulario del par, frecuencias de documento, IDF, TF, pesos y coseno.
 *
 * <p><b>Qué guarda.</b> {@code vocabulary} es la unión de los términos de {@code a} y {@code b}; todos los
 * arreglos están alineados con ella (la posición k corresponde a {@code vocabulary.get(k)}):
 * <ul>
 *   <li>{@code documentCount} = N, número de documentos del corpus.</li>
 *   <li>{@code documentFrequency[k]} = df(t), documentos del corpus que contienen t.</li>
 *   <li>{@code idf[k]} = ln(N / df(t)).</li>
 *   <li>{@code tfA[k]}, {@code tfB[k]} = f(t, d) / |d|.</li>
 *   <li>{@code weightsA[k]}, {@code weightsB[k]} = tf · idf.</li>
 *   <li>{@code dotProduct} = Σ wₐ · w_b; {@code normA} = ‖wₐ‖₂; {@code normB} = ‖w_b‖₂.</li>
 * </ul>
 *
 * <p>Complejidad: construcción O(V) en tiempo y espacio por las copias defensivas, con V = |vocabulary|.
 *
 * @param vocabulary        unión de los términos de a y b
 * @param documentCount     N, número de documentos del corpus
 * @param documentFrequency df por término
 * @param idf               idf por término
 * @param tfA               tf por término en a
 * @param tfB               tf por término en b
 * @param weightsA          pesos tf · idf de a
 * @param weightsB          pesos tf · idf de b
 * @param dotProduct        producto punto de los pesos
 * @param normA             norma euclidiana de los pesos de a
 * @param normB             norma euclidiana de los pesos de b
 */
public record TfIdfCosineTrace(List<String> vocabulary, int documentCount, int[] documentFrequency, double[] idf,
		double[] tfA, double[] tfB, double[] weightsA, double[] weightsB,
		double dotProduct, double normA, double normB) {

	/**
	 * Copia defensiva de la lista y los arreglos para que la traza sea inmutable.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio.
	 */
	public TfIdfCosineTrace {
		vocabulary = List.copyOf(vocabulary);
		documentFrequency = documentFrequency.clone();
		idf = idf.clone();
		tfA = tfA.clone();
		tfB = tfB.clone();
		weightsA = weightsA.clone();
		weightsB = weightsB.clone();
	}

	/**
	 * Frecuencias de documento.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de df
	 */
	@Override
	public int[] documentFrequency() {
		return documentFrequency.clone();
	}

	/**
	 * IDF por término.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de idf
	 */
	@Override
	public double[] idf() {
		return idf.clone();
	}

	/**
	 * TF de a.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de tf en a
	 */
	@Override
	public double[] tfA() {
		return tfA.clone();
	}

	/**
	 * TF de b.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de tf en b
	 */
	@Override
	public double[] tfB() {
		return tfB.clone();
	}

	/**
	 * Pesos de a.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de los pesos de a
	 */
	@Override
	public double[] weightsA() {
		return weightsA.clone();
	}

	/**
	 * Pesos de b.
	 *
	 * <p>Complejidad: O(V) en tiempo y espacio (copia).
	 *
	 * @return copia de los pesos de b
	 */
	@Override
	public double[] weightsB() {
		return weightsB.clone();
	}

}
