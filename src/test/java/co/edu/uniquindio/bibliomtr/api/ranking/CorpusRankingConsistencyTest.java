package co.edu.uniquindio.bibliomtr.api.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import co.edu.uniquindio.bibliomtr.TestcontainersConfiguration;
import co.edu.uniquindio.bibliomtr.algorithms.ranking.TermFrequency;
import co.edu.uniquindio.bibliomtr.api.corpus.FakeEmbedders;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.AlgorithmRanking;
import co.edu.uniquindio.bibliomtr.api.ranking.RankingDtos.RankingResponse;

/**
 * Consistencia de los tres algoritmos sobre el corpus real de {@code corpus/} (spec 002, REQ-1, REQ-10, REQ-26;
 * AC-6, AC-26).
 *
 * <p><b>Montaje.</b> Contexto completo con Postgres real ({@link TestcontainersConfiguration}), la ingesta
 * encendida sobre {@value #CORPUS_DIR} y los embeddings falsos de {@link FakeEmbedders.Config} en lugar de
 * SPECTER2 y Ollama: los vectores no intervienen en el ranking, solo los abstracts. No usa
 * {@code FakeCorpusTokenSource}, así que los tokens salen de {@link SelectableAbstractTokenSource}.
 *
 * <p><b>Omisión.</b> Los PDF del corpus no se versionan. Si {@value #CORPUS_DIR} no tiene ningún {@code *.pdf},
 * el {@code @BeforeAll} estático aborta la clase con {@link Assumptions} antes de que se arranque el contexto, y el
 * reporte la muestra omitida con el motivo, sin que el build falle.
 */
@SpringBootTest(properties = {"bibliomtr.ingestion.enabled=true", "bibliomtr.corpus.dir=" + CorpusRankingConsistencyTest.CORPUS_DIR})
@Import({TestcontainersConfiguration.class, FakeEmbedders.Config.class})
class CorpusRankingConsistencyTest {

	/** Directorio del corpus, relativo al directorio de trabajo de Gradle (la raíz del proyecto). */
	static final String CORPUS_DIR = "corpus";

	@Autowired
	private RankingService rankingService;

	/** Omite la clase si el corpus no tiene PDF (AC-26). */
	@BeforeAll
	static void requireCorpusPdfs() throws IOException {
		Path corpus = Path.of(CORPUS_DIR);
		Assumptions.assumeTrue(hasPdf(corpus),
				"Sin PDF en " + corpus.toAbsolutePath() + ": se omite el test de consistencia sobre el corpus (REQ-26)");
	}

	@Test
	@DisplayName("AC-6, AC-26: sobre el corpus real los tres algoritmos dan el mismo ranking completo y el mismo top 20")
	void threeAlgorithmsAgreeOnCorpus() {
		RankingResponse response = rankingService.rank(20, 5);

		// Un corpus sin términos daría tres rankings vacíos, iguales de forma trivial.
		assertThat(response.termCount()).isPositive();
		assertThat(response.allMatch()).isTrue();

		List<AlgorithmRanking> algorithms = response.algorithms();
		assertThat(algorithms).extracting(AlgorithmRanking::algorithm).containsExactly("counting", "radix", "bucket");
		List<TermFrequency> countingTop = algorithms.getFirst().top();
		assertThat(countingTop).hasSize(Math.min(20, response.termCount()));
		for (AlgorithmRanking algorithm : algorithms) {
			assertThat(algorithm.top()).as(algorithm.algorithm()).containsExactlyElementsOf(countingTop);
		}
	}

	/**
	 * Indica si el directorio tiene algún archivo regular {@code *.pdf} (sin distinguir mayúsculas), con el mismo
	 * criterio que la ingesta.
	 */
	private static boolean hasPdf(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			return false;
		}
		try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
			for (Path entry : entries) {
				String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.endsWith(".pdf") && Files.isRegularFile(entry)) {
					return true;
				}
			}
		}
		return false;
	}

}
