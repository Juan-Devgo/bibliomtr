package co.edu.uniquindio.bibliomtr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.bibliomtr.api.ranking.CorpusTokenSource;
import co.edu.uniquindio.bibliomtr.api.ranking.SelectableAbstractTokenSource;

/**
 * Arranque del contexto completo con Postgres real ({@link TestcontainersConfiguration}), sin
 * {@code FakeCorpusTokenSource} y con la ingesta apagada ({@code src/test/resources/config/application.properties})
 * (spec 002, REQ-1, AC-2).
 *
 * <p>Verifica que {@code src/main} registra su propio {@link CorpusTokenSource}: sin él, {@code bootRun} fallaría
 * con {@code NoSuchBeanDefinitionException} al crear {@code RankingService}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BibliomtrApplicationTests {

	@Autowired
	private CorpusTokenSource corpusTokenSource;

	@Test
	@DisplayName("AC-2: el contexto completo arranca y el CorpusTokenSource es SelectableAbstractTokenSource")
	void contextLoadsWithProductionCorpusTokenSource() {
		assertThat(corpusTokenSource).isInstanceOf(SelectableAbstractTokenSource.class);
	}

}
