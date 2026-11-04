package co.edu.uniquindio.bibliomtr.algorithms.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.edu.uniquindio.bibliomtr.trace.similarity.TfIdfCosineTrace;

/**
 * Similitud coseno entre los vectores TF-IDF de dos documentos (Requerimiento 1, algoritmo clásico). STUB: el
 * cuerpo de {@link #compare} lo escribe el estudiante (constitución P1; ver {@code docs/guia-implementacion.md}).
 *
 * <p><b>Qué calcula.</b> Representa cada documento como un vector de pesos tf · idf sobre el vocabulario del
 * par (la unión de los términos de {@code a} y {@code b}) y devuelve el coseno del ángulo entre ambos. El
 * corpus del IDF son todos los abstracts seleccionables, de modo que el valor de un par no depende de qué
 * otros artículos se eligieron.
 *
 * <p><b>Fórmula.</b> Con N = |corpus|, f(t, d) = apariciones de t en d y df(t) = documentos del corpus que
 * contienen t:
 * <pre>
 *   tf(t, d) = f(t, d) / |d|
 *   idf(t)   = ln(N / df(t))
 *   w(t, d)  = tf(t, d) · idf(t)
 *   cos(a, b) = Σₜ w(t, a) · w(t, b) / (‖w(·, a)‖₂ · ‖w(·, b)‖₂)
 * </pre>
 *
 * <p><b>Normalización sugerida.</b> Ninguna: los pesos son no negativos, así que {@code value = similarity =
 * cos} ya está en [0, 1]. Vale 1 si {@code a} y {@code b} son vacías, y 0 si alguna norma es 0 en otro caso.
 *
 * <p><b>Traza.</b> {@link TfIdfCosineTrace}: vocabulario, N, df, idf, tf de cada documento, pesos, producto
 * punto y normas, con los arreglos alineados con el vocabulario.
 *
 * <p><b>Rango.</b> {@code value} = {@code similarity} ∈ [0, 1].
 *
 * <p><b>Casos de borde.</b> Ambas vacías: 1. Una vacía: 0. Idénticas (con algún término de idf &gt; 0): 1.
 * Disjuntas: 0. Longitudes muy distintas con las mismas proporciones ([a,b] y 5 × [a,b]): 1, porque tf divide
 * por |d|. Un término presente en todos los documentos tiene idf 0 y no aporta.
 *
 * <p><b>Complejidad: TODO estudiante</b> (tiempo y espacio en función de |a|, |b|, N y el tamaño total del
 * corpus).
 */
public final class TfIdfCosineSimilarity implements CorpusSimilarity<TfIdfCosineTrace> {

	/**
	 * Calcula el coseno entre los vectores TF-IDF de {@code a} y {@code b} con el IDF de {@code corpus}, y la
	 * traza.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param a      tokens del primer documento (no nulo; puede ser vacío)
	 * @param b      tokens del segundo documento (no nulo; puede ser vacío)
	 * @param corpus tokens de cada documento del corpus del IDF (no nulo)
	 * @return {@code value} = {@code similarity} = cos y la traza {@link TfIdfCosineTrace}
	 */
	@Override
	public SimilarityResult<TfIdfCosineTrace> compare(List<String> a, List<String> b, List<List<String>> corpus) {

	  int documentCount = corpus.size();

		// Vocabulario del par: unión de los términos de a y b, en orden de primera aparición.
		Set<String> vocabularySet = new LinkedHashSet<>(a);
		vocabularySet.addAll(b);
		List<String> vocabulary = new ArrayList<>(vocabularySet);
		int size = vocabulary.size();

		// df(t): en cuántos documentos del corpus aparece t (cada documento cuenta una vez).
		int[] documentFrequency = new int[size];
		for (List<String> document : corpus) {
			Set<String> terms = new HashSet<>(document);
			for (int k = 0; k < size; k++) {
				if (terms.contains(vocabulary.get(k))) {
					documentFrequency[k]++;
				}
			}
		}

		// idf(t) = ln(N / df(t)). Un término que no está en el corpus (df = 0) no tiene idf definido: se toma 0
		// para que no aporte (nunca ocurre en la aplicación, donde a y b son parte del corpus).
		double[] idf = new double[size];
		for (int k = 0; k < size; k++) {
			idf[k] = documentFrequency[k] == 0 ? 0.0 : Math.log((double) documentCount / documentFrequency[k]);
		}

		// tf(t, d) = f(t, d) / |d| y peso w = tf · idf, para cada documento del par.
		double[] tfA = termFrequency(a, vocabulary);
		double[] tfB = termFrequency(b, vocabulary);
		double[] weightsA = new double[size];
		double[] weightsB = new double[size];
		double dotProduct = 0.0;
		double sumSquaresA = 0.0;
		double sumSquaresB = 0.0;
		for (int k = 0; k < size; k++) {
			weightsA[k] = tfA[k] * idf[k];
			weightsB[k] = tfB[k] * idf[k];
			dotProduct += weightsA[k] * weightsB[k];
			sumSquaresA += weightsA[k] * weightsA[k];
			sumSquaresB += weightsB[k] * weightsB[k];
		}
		double normA = Math.sqrt(sumSquaresA);
		double normB = Math.sqrt(sumSquaresB);

		// cos = (wa · wb) / (‖wa‖ ‖wb‖). Dos documentos vacíos son idénticos; una norma 0 en otro caso da 0.
		double cosine;
		if (a.isEmpty() && b.isEmpty()) {
			cosine = 1.0;
		} else if (normA == 0.0 || normB == 0.0) {
			cosine = 0.0;
		} else {
			// Los pesos no son negativos, así que cos ∈ [0, 1]; el min absorbe el error de redondeo (1.0000000000000002).
			cosine = Math.min(1.0, dotProduct / (normA * normB));
		}
		
		TfIdfCosineTrace trace = new TfIdfCosineTrace(vocabulary, documentCount, documentFrequency, idf, tfA, tfB, weightsA, weightsB, dotProduct, normA, normB);
		return new SimilarityResult<>(cosine, cosine, trace);
	}

	private static double[] termFrequency(List<String> document, List<String> vocabulary) {

	  Map<String, Integer> counts = new HashMap<>();
		for (String term : document) {
			counts.merge(term, 1, Integer::sum);
		}
		
		double[] tf = new double[vocabulary.size()];
		if (document.isEmpty()) {
			return tf;
		}
		
		for (int k = 0; k < tf.length; k++) {
			Integer count = counts.get(vocabulary.get(k));
			tf[k] = count == null ? 0.0 : (double) count / document.size();
		}
		
		return tf;
	}

}
