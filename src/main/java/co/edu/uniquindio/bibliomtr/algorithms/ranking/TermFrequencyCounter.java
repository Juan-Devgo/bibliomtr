package co.edu.uniquindio.bibliomtr.algorithms.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cuenta la frecuencia de cada término y arma la entrada de los algoritmos de ordenamiento (REQ-1, REQ-2).
 *
 * <p><b>Qué hace.</b> Para cada término {@code t}, su frecuencia es
 * {@code f(t) = Σ_a |{ i : tokens_a[i] = t }|}, la suma de sus ocurrencias en todos los abstracts {@code a}
 * ya preprocesados. Devuelve la lista de pares (término, frecuencia) en orden alfabético
 * <em>descendente</em>.
 *
 * <p><b>Por qué en ese orden.</b> Los algoritmos ordenan ascendente y de forma estable por frecuencia, y el
 * ranking se lee de derecha a izquierda ({@link RankingReader#read}). Si la entrada va en orden alfabético
 * descendente, la estabilidad deja los empates en ese mismo orden, y al leer de derecha a izquierda quedan
 * en orden alfabético ascendente, que es el desempate pedido. El criterio alfabético es el orden natural de
 * {@code String} ({@link String#compareTo}, el que usa {@link TreeMap} por defecto) sobre los tokens ya
 * preprocesados. {@link TreeMap} es solo una estructura auxiliar para armar la entrada (spec, P1 y REQ-12);
 * no ordena por frecuencia.
 *
 * <p>Complejidad: tiempo O(T · log V) y espacio O(V), con T = número total de tokens y V = número de
 * términos distintos (cada inserción en el árbol rojo-negro cuesta O(log V)).
 */
public final class TermFrequencyCounter {

	/**
	 * Cuenta las ocurrencias de cada término en todos los abstracts y devuelve la entrada de los algoritmos.
	 *
	 * <p>Complejidad: tiempo O(T · log V) y espacio O(V), con T = número total de tokens y V = número de
	 * términos distintos.
	 *
	 * @param tokensPerAbstract tokens preprocesados de cada abstract seleccionable
	 * @return pares (término, frecuencia) en orden alfabético descendente; vacía si no hay tokens
	 */
	public List<TermFrequency> count(List<List<String>> tokensPerAbstract) {
		// Suma de ocurrencias por término; el árbol mantiene las claves en orden natural de String.
		TreeMap<String, Integer> frequencies = new TreeMap<>();
		for (List<String> tokens : tokensPerAbstract) {
			for (String token : tokens) {
				frequencies.merge(token, 1, Integer::sum);
			}
		}
		// Recorrido de mayor a menor clave: orden alfabético descendente (REQ-2).
		List<TermFrequency> input = new ArrayList<>(frequencies.size());
		for (Map.Entry<String, Integer> entry : frequencies.descendingMap().entrySet()) {
			input.add(new TermFrequency(entry.getKey(), entry.getValue()));
		}
		return List.copyOf(input);
	}

}
