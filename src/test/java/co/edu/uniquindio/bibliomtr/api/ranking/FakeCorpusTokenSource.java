package co.edu.uniquindio.bibliomtr.api.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * {@link CorpusTokenSource} configurable para los tests (REQ-1): devuelve los tokens que el test le asigna, sin
 * ingesta ni preprocesamiento de la spec 001.
 *
 * <p>Se registra con {@code @Import(FakeCorpusTokenSource.Config.class)} como bean {@code @Primary}, de modo
 * que reemplaza al adaptador de producción cuando exista.
 */
public class FakeCorpusTokenSource implements CorpusTokenSource {

	private volatile List<List<String>> abstracts = List.of();

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	@Override
	public List<List<String>> selectableAbstractTokens() {
		return abstracts;
	}

	/**
	 * Fija los tokens de cada abstract.
	 *
	 * <p>Complejidad: O(T) en tiempo y espacio, con T = número total de tokens.
	 *
	 * @param tokensPerAbstract tokens preprocesados por abstract
	 */
	public void setAbstracts(List<List<String>> tokensPerAbstract) {
		List<List<String>> copy = new ArrayList<>(tokensPerAbstract.size());
		for (List<String> tokens : tokensPerAbstract) {
			copy.add(List.copyOf(tokens));
		}
		this.abstracts = List.copyOf(copy);
	}

	/**
	 * Deja el corpus sin abstracts seleccionables (AC-4).
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 */
	public void clear() {
		this.abstracts = List.of();
	}

	/**
	 * Arma un único abstract con {@code termCount} términos distintos {@code term01, term02, …}, donde el
	 * término i (desde 1) aparece i veces; así cada término tiene una frecuencia distinta y el ranking es
	 * {@code term<termCount>, …, term01}.
	 *
	 * <p>Complejidad: O(c²) en tiempo y espacio, con c = {@code termCount}.
	 *
	 * @param termCount número de términos distintos
	 */
	public void setDistinctTerms(int termCount) {
		List<String> tokens = new ArrayList<>();
		for (int i = 1; i <= termCount; i++) {
			tokens.addAll(Collections.nCopies(i, "term%02d".formatted(i)));
		}
		setAbstracts(List.of(tokens));
	}

	/**
	 * Configuración de test que registra el fake como {@link CorpusTokenSource} principal.
	 */
	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		/**
		 * Fake vacío; cada test le asigna sus tokens.
		 *
		 * @return el fake compartido del contexto
		 */
		@Bean
		@Primary
		FakeCorpusTokenSource fakeCorpusTokenSource() {
			return new FakeCorpusTokenSource();
		}

	}

}
