package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;

import java.util.ArrayList;
import java.util.List;

import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpPreprocessing.PrefixStep;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace;
import co.edu.uniquindio.bibliomtr.trace.stringmatching.KmpTrace.KmpComparison;

/**
 * Knuth-Morris-Pratt (KMP) (Requerimiento 4). STUB: los cuerpos de {@link #preprocess} y {@link #search} los
 * escribe el estudiante (constitución P1; ver {@code docs/guia-implementacion.md}, § 11).
 *
 * <p><b>Qué calcula.</b> Las posiciones iniciales de todas las ocurrencias exactas de un patrón P (|P| = m) en un
 * texto T (|T| = n), incluidas las solapadas. Ante un desajuste aprovecha lo ya comparado: el patrón se desliza
 * según la función de prefijo π y el índice del texto i nunca retrocede.
 *
 * <p><b>Definición formal.</b>
 * <pre>
 *   π[i] = max { k : k &lt; i + 1 y P[0..k−1] = P[i−k+1..i] }   (π[0] = 0)
 * </pre>
 * Es la longitud del prefijo propio más largo de P[0..i] que también es sufijo de P[0..i]. En la búsqueda, con j
 * caracteres del patrón ya coincidentes, si T[i] = P[j] se avanza i y j; si T[i] ≠ P[j] y j &gt; 0 se aplica el
 * retroceso j ← π[j − 1] sin mover i; si j = 0 se avanza i. Al completar j = m hay una ocurrencia en s = i − m + 1
 * y se aplica j ← π[m − 1] para hallar las solapadas. El desplazamiento del patrón en cada comparación es
 * s = i − j.
 *
 * <p><b>Preprocesamiento.</b> {@code preprocess(P)} lanza {@code IllegalArgumentException} si P es nulo o vacío y
 * calcula π en O(m) con el mismo esquema: para cada i = 1 … m − 1 se prueba k = π[i − 1] y, mientras k &gt; 0 y
 * P[k] ≠ P[i], k ← π[k − 1]; π[i] = k + 1 si P[k] = P[i], si no 0. Devuelve {@link KmpPreprocessing} con un
 * {@code PrefixStep} por i (los k probados y π[i]) si {@code options.recordTrace()}; si no, {@code steps} vacía.
 *
 * <p><b>Búsqueda.</b> {@code search(T, preprocesado)} recorre T una vez con i de 0 a n − 1 usando π, sin volver a
 * calcularla. Devuelve las posiciones ascendentes y {@code collisions} vacía.
 *
 * <p><b>Traza.</b> {@link KmpTrace}: un {@code KmpComparison} por comparación T[i] vs P[j] con i, j, s = i − j, los
 * dos caracteres, si coincidieron, {@code fallbackTo} (j nuevo tras j ← π[j − 1], o {@code null}) y
 * {@code occurrence} (s de la ocurrencia que completa, o {@code null}); más la lista de ocurrencias. Con
 * {@code recordTrace = false} la traza es {@code null}.
 *
 * <p><b>Caso a mano.</b> T = "abababca", P = "abab": π = [0, 0, 1, 2]; 9 comparaciones (i, j, s): (0,0,0)
 * (1,1,0) (2,2,0) (3,3,0) (4,2,2) (5,3,2) (6,2,4) (6,0,6) (7,0,7); posiciones [0, 2].
 *
 * <p><b>Casos de borde.</b> Patrón vacío → {@code IllegalArgumentException}. Texto vacío o n &lt; m → [].
 * "abc"/"abc" → [0]; "aaaa"/"aa" → [0, 1, 2] (solapadas); "Algoritmo algoritmo"/"algoritmo" → [10] (distingue
 * mayúsculas); "canción cancion"/"cancion" → [8] (distingue tildes); "a b c"/" " → [1, 3]. Dos abstracts
 * idénticos con el mismo preprocesado dan las mismas posiciones.
 *
 * <p><b>Complejidad del preprocesamiento: TODO estudiante</b> (tiempo y espacio en función de m; justifique por qué
 * el ciclo interno no hace el costo cuadrático).
 *
 * <p><b>Complejidad de la búsqueda: TODO estudiante</b> (tiempo y espacio en función de n; acote el número de
 * comparaciones).
 */
public final class KnuthMorrisPratt implements StringMatcher<KmpPreprocessing, KmpTrace> {

	/**
	 * Calcula la función de prefijo π del patrón.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param pattern patrón no vacío
	 * @param options grabación de traza (q no se usa)
	 * @return π y, si se graba la traza, los pasos de su construcción
	 * @throws IllegalArgumentException si el patrón es nulo o vacío
	 */
	@Override
	public KmpPreprocessing preprocess(String pattern, MatchOptions options) {
  	if (pattern == null || pattern.isEmpty()) {
  		throw new IllegalArgumentException("El patrón no puede ser nulo ni vacío");
  	}
  	int m = pattern.length();
  	int[] pi = new int[m]; // π[0] = 0 siempre: un prefijo propio de un solo carácter es vacío
  	List<PrefixStep> steps = new ArrayList<>();

  	for (int i = 1; i < m; i++) {
  		char character = pattern.charAt(i);
  		int k = pi[i - 1]; // longitud del mejor borde de P[0..i−1]; se intenta extenderlo con P[i]
  		// Los candidatos solo se guardan si se pidió traza: así la medición de tiempos no paga por ellos.
  		List<Integer> candidates = options.recordTrace() ? new ArrayList<>() : null;
  		while (true) {
  			if (candidates != null) {
  				candidates.add(k);
  			}
  			if (pattern.charAt(k) == character) {
  				pi[i] = k + 1; // el borde de longitud k se extiende
  				break;
  			}
  			if (k == 0) {
  				pi[i] = 0; // ya no hay borde más corto que probar
  				break;
  			}
  			k = pi[k - 1]; // siguiente borde más largo de P[0..k−1]
  		}
  		if (candidates != null) {
  			steps.add(new PrefixStep(i, character, candidates, pi[i]));
  		}
  	}
  	return new KmpPreprocessing(pattern, pi, steps);
	}

	/**
	 * Busca el patrón preprocesado en el texto sin retroceder el índice del texto.
	 *
	 * <p>Complejidad: TODO estudiante.
	 *
	 * @param text         texto donde se busca
	 * @param preprocessed resultado de {@link #preprocess}
	 * @param options      grabación de traza (q no se usa)
	 * @return posiciones ascendentes, colisiones vacías y la traza {@link KmpTrace} (o {@code null})
	 */
	@Override
	public MatchResult<KmpTrace> search(String text, KmpPreprocessing preprocessed, MatchOptions options) {
  	String pattern = preprocessed.pattern();
  	int[] pi = preprocessed.pi();
  	int n = text.length();
  	int m = pattern.length();
  	List<Integer> positions = new ArrayList<>();
  	List<KmpComparison> comparisons = new ArrayList<>();

  	if (n >= m) {
  		int i = 0; // índice en el texto: nunca disminuye
  		int j = 0; // caracteres de P ya coincidentes con el texto que termina en i − 1
  		while (i < n) {
  			int comparedI = i;
  			int comparedJ = j;
  			char textChar = text.charAt(i);
  			char patternChar = pattern.charAt(j);
  			int shift = i - j; // alineación actual del patrón sobre el texto
  			boolean matched = textChar == patternChar;
  			Integer fallbackTo = null;
  			Integer occurrence = null;

  			if (matched) {
  				if (j == m - 1) {
  					// Patrón completo: ocurrencia en s. j ← π[m−1] reutiliza el borde para hallar las solapadas.
  					occurrence = shift;
  					positions.add(shift);
  					j = pi[m - 1];
  					fallbackTo = j;
  				} else {
  					j++;
  				}
  				i++;
  			} else if (j > 0) {
  				j = pi[j - 1]; // desajuste: el patrón se desliza sin retroceder i
  				fallbackTo = j;
  			} else {
  				i++; // desajuste con j = 0: no hay nada que reutilizar
  			}
  			if (options.recordTrace()) {
  				comparisons.add(new KmpComparison(comparedI, comparedJ, shift, textChar, patternChar, matched,
  						fallbackTo, occurrence));
  			}
  		}
  	}
  	KmpTrace trace = options.recordTrace() ? new KmpTrace(comparisons, positions) : null;
  	return new MatchResult<>(positions, List.of(), trace);
	}

}
