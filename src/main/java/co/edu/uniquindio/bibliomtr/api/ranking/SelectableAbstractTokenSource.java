package co.edu.uniquindio.bibliomtr.api.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.uniquindio.bibliomtr.persistence.article.Article;
import co.edu.uniquindio.bibliomtr.persistence.article.ArticleRepository;
import co.edu.uniquindio.bibliomtr.preprocessing.TextPreprocessor;

/**
 * Adaptador de producción del puerto {@link CorpusTokenSource} sobre la ingesta y el preprocesamiento de la
 * spec 001 (REQ-1, AC-2; constitución P5, P12).
 *
 * <p><b>Qué hace.</b> Lee los artículos seleccionables (con abstract detectado) de
 * {@link ArticleRepository#findAllByAbstractDetectedTrueOrderByFileNameAsc()} y aplica
 * {@link TextPreprocessor#tokenize(String)} al abstract de cada uno. El título no se usa: el Requerimiento 2
 * trabaja estrictamente sobre el ABSTRACT (P5).
 *
 * <p><b>Por qué.</b> Es el único punto de la 002 que conoce nombres de la 001, de modo que algoritmos, API y
 * mediciones solo dependen del puerto. Reutiliza el mismo preprocesador determinista (minúsculas
 * {@code Locale.ROOT}, NFD sin diacríticos, corridas de {@code [a-z0-9]}, sin stopwords de NLTK, longitud ≥ 2 y
 * sin tokens solo numéricos) que usan los algoritmos de similitud, así que las frecuencias de REQ-1 cuentan los
 * mismos tokens. El orden por nombre de archivo lo da la consulta; el conteo de frecuencias no depende de él.
 *
 * <p>Complejidad de construcción: O(S) en tiempo y espacio por la carga de la lista de stopwords, con
 * S = número total de caracteres de la lista.
 */
@Component
public class SelectableAbstractTokenSource implements CorpusTokenSource {

	private final ArticleRepository repository;
	private final TextPreprocessor preprocessor = new TextPreprocessor();

	/**
	 * Adaptador sobre el repositorio de artículos de la spec 001.
	 *
	 * <p>Complejidad: O(S) en tiempo y espacio, por la carga de las stopwords del preprocesador.
	 *
	 * @param repository artículos guardados por la ingesta
	 */
	public SelectableAbstractTokenSource(ArticleRepository repository) {
		this.repository = Objects.requireNonNull(repository, "El repositorio de artículos no puede ser nulo");
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Una lista de tokens por artículo seleccionable, en el orden de nombre de archivo de la consulta.
	 *
	 * <p>Complejidad: O(A log A) en la base por la consulta ordenada, más O(L) en tiempo y espacio en Java por
	 * la tokenización, con A = número de artículos seleccionables y L = total de caracteres de sus abstracts
	 * (el espacio de la consulta incluye los dos vectores de 768 dimensiones por artículo).
	 *
	 * @return tokens de cada abstract seleccionable; vacía si no hay artículos seleccionables
	 */
	@Override
	public List<List<String>> selectableAbstractTokens() {
		List<Article> articles = repository.findAllByAbstractDetectedTrueOrderByFileNameAsc();
		List<List<String>> tokensPerAbstract = new ArrayList<>(articles.size());
		for (Article article : articles) {
			// Solo el abstract entra al preprocesamiento (P5); el título queda fuera.
			tokensPerAbstract.add(preprocessor.tokenize(article.getAbstractText()));
		}
		return tokensPerAbstract;
	}

}
