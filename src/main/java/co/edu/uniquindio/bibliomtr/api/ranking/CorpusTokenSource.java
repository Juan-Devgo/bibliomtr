package co.edu.uniquindio.bibliomtr.api.ranking;

import java.util.List;

/**
 * Puerto (patrón Port/Adapter) que entrega los tokens preprocesados de los abstracts seleccionables del
 * corpus (REQ-1; constitución P5).
 *
 * <p><b>Por qué.</b> Aísla el Requerimiento 2 de la ingesta y del preprocesamiento de la spec 001: el ranking,
 * la traza y las mediciones dependen solo de esta interfaz. El adaptador de producción
 * ({@code SelectableAbstractTokenSource}) se escribe sobre el preprocesador y el repositorio de artículos de
 * la 001; los tests usan un fake.
 */
public interface CorpusTokenSource {

	/**
	 * Tokens ya preprocesados (normalizados, tokenizados y sin stopwords, de forma determinista) de cada
	 * abstract seleccionable del corpus.
	 *
	 * <p>Complejidad: depende del adaptador; como mínimo O(T) en tiempo y espacio, con T = número total de
	 * tokens.
	 *
	 * @return una lista de tokens por abstract; vacía si el corpus no tiene abstracts seleccionables
	 */
	List<List<String>> selectableAbstractTokens();

}
