package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte la salida ascendente de los algoritmos en el ranking, corta el top N y compara rankings
 * (REQ-2, REQ-3, REQ-10).
 *
 * <p><b>Por qué.</b> Los algoritmos ordenan ascendente y estable sobre una entrada en orden alfabético
 * descendente ({@link TermFrequencyCounter}). Leer la salida de derecha a izquierda invierte ambos órdenes a
 * la vez: la frecuencia queda descendente y los empates, que estaban en orden alfabético descendente, quedan
 * en orden alfabético ascendente.
 */
public final class RankingReader {

	private RankingReader() {
	}

	/**
	 * Lee la salida de un algoritmo de derecha a izquierda: {@code R[i] = S[n − 1 − i]}.
	 *
	 * <p>Complejidad: O(n) en tiempo y espacio.
	 *
	 * @param ascending salida de un {@link TermSorter}, ascendente y estable por frecuencia
	 * @return ranking por frecuencia descendente con empates en orden alfabético ascendente (inmutable)
	 */
	public static List<TermFrequency> read(List<TermFrequency> ascending) {
		List<TermFrequency> ranking = new ArrayList<>(ascending.size());
		for (int i = ascending.size() - 1; i >= 0; i--) {
			ranking.add(ascending.get(i));
		}
		return List.copyOf(ranking);
	}

	/**
	 * Devuelve los {@code min(n, tamaño)} primeros términos del ranking; si {@code n} supera el número de
	 * términos devuelve todos sin error (REQ-3).
	 *
	 * <p>Complejidad: O(min(n, tamaño)) en tiempo y espacio.
	 *
	 * @param ranking ranking completo
	 * @param n       número de términos pedidos, mayor o igual a 1
	 * @return los primeros términos del ranking (inmutable)
	 * @throws IllegalArgumentException si {@code n < 1}
	 */
	public static List<TermFrequency> top(List<TermFrequency> ranking, int n) {
		if (n < 1) {
			throw new IllegalArgumentException("El parámetro n debe ser un entero mayor o igual a 1");
		}
		int limit = Math.min(n, ranking.size());
		List<TermFrequency> top = new ArrayList<>(limit);
		for (int i = 0; i < limit; i++) {
			top.add(ranking.get(i));
		}
		return List.copyOf(top);
	}

	/**
	 * Indica si todos los rankings son iguales elemento a elemento (mismo término y misma frecuencia en
	 * cada posición) y del mismo tamaño. Con cero o un ranking devuelve {@code true}.
	 *
	 * <p>Complejidad: O(r · n) en tiempo y O(1) en espacio, con r = número de rankings y n = su tamaño.
	 *
	 * @param rankings rankings completos a comparar
	 * @return {@code true} si coinciden todos
	 */
	public static boolean sameRanking(List<List<TermFrequency>> rankings) {
		if (rankings.isEmpty()) {
			return true;
		}
		List<TermFrequency> reference = rankings.getFirst();
		for (int r = 1; r < rankings.size(); r++) {
			List<TermFrequency> other = rankings.get(r);
			if (other.size() != reference.size()) {
				return false;
			}
			for (int i = 0; i < reference.size(); i++) {
				if (!reference.get(i).equals(other.get(i))) {
					return false;
				}
			}
		}
		return true;
	}

}
