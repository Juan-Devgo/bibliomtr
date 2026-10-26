package co.edu.uniquindio.bibliomtr.api.corpus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import co.edu.uniquindio.bibliomtr.embeddings.EmbeddingModel;
import co.edu.uniquindio.bibliomtr.embeddings.NomicEmbedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2Embedder;
import co.edu.uniquindio.bibliomtr.embeddings.Specter2Embedding;

/**
 * Generadores de embeddings falsos para los tests de ingesta (spec 001, REQ-8, REQ-9): sin modelo ONNX ni
 * Ollama, devuelven vectores fijos y registran lo que recibieron.
 * <ul>
 *   <li>{@link FakeSpecter2}: devuelve {@link #SPECTER2_VECTOR}; con la propiedad
 *       {@value #SPECTER2_TRUNCATED_PROPERTY} = {@code true} lo marca como truncado con
 *       {@value #TRUNCATED_TOKEN_COUNT} tokens.</li>
 *   <li>{@link FakeNomic}: devuelve {@link #NOMIC_VECTOR}; con la propiedad {@value #NOMIC_FAILS_PROPERTY} =
 *       {@code true} falla con {@link #NOMIC_FAILURE}. Con la propiedad {@value #NOMIC_PULL_FAILURES_PROPERTY}
 *       = N (0 por defecto), sus N primeras llamadas a {@code ensureModel()} fallan con
 *       {@link #NOMIC_PULL_FAILURE}.</li>
 * </ul>
 * El modo se lee de propiedades porque la ingesta corre durante el arranque, antes de que el test pueda tocar los
 * beans; así cada arranque de {@code SpringApplicationBuilder} elige el suyo con {@code --propiedad=valor}.
 *
 * <p>Se registran con {@link Config} como beans {@code @Primary}, de modo que reemplazan a los
 * {@code specter2Embedder} y {@code nomicEmbedder} de {@code SimilarityConfig}.
 */
public final class FakeEmbedders {

	/** Propiedad que hace que el fake de SPECTER2 marque cada texto como truncado. */
	public static final String SPECTER2_TRUNCATED_PROPERTY = "fake-embedders.specter2-truncated";

	/** Propiedad que hace que el fake de nomic-embed-text falle en cada {@code embed}. */
	public static final String NOMIC_FAILS_PROPERTY = "fake-embedders.nomic-fails";

	/**
	 * Propiedad entera N (0 por defecto): las N primeras llamadas a {@code ensureModel()} del fake de
	 * nomic-embed-text lanzan {@link IllegalStateException} con {@link #NOMIC_PULL_FAILURE}.
	 */
	public static final String NOMIC_PULL_FAILURES_PROPERTY = "fake-embedders.nomic-pull-failures";

	/** Tokens antes de truncar que informa el fake de SPECTER2 en modo truncado. */
	public static final int TRUNCATED_TOKEN_COUNT = 600;

	/** Tokens que informa el fake de SPECTER2 en modo normal. */
	public static final int NORMAL_TOKEN_COUNT = 120;

	/** Mensaje del error del fake de nomic-embed-text en modo falla. */
	public static final String NOMIC_FAILURE = "Ollama no responde";

	/** Mensaje del error de {@code ensureModel()} del fake de nomic-embed-text en sus llamadas que fallan. */
	public static final String NOMIC_PULL_FAILURE = "Ollama no pudo descargar el modelo";

	/** Vector de SPECTER2: e₀ de 768 dimensiones, con norma L2 igual a 1. */
	public static final float[] SPECTER2_VECTOR = unitVector(EmbeddingModel.SPECTER2.dimension(), 0);

	/** Vector de nomic-embed-text: e₁ de 768 dimensiones. */
	public static final float[] NOMIC_VECTOR = unitVector(EmbeddingModel.NOMIC.dimension(), 1);

	private FakeEmbedders() {
	}

	/** Vector canónico eᵢ de la dimensión dada. Complejidad: O(d). */
	private static float[] unitVector(int dimension, int index) {
		float[] vector = new float[dimension];
		vector[index] = 1f;
		return vector;
	}

	/**
	 * Una llamada al fake de SPECTER2.
	 *
	 * @param title        título recibido
	 * @param abstractText abstract recibido
	 */
	public record Specter2Call(String title, String abstractText) {
	}

	/** {@link Specter2Embedder} falso: vector fijo, truncado según su modo. */
	public static final class FakeSpecter2 implements Specter2Embedder {

		private final boolean truncated;
		private final List<Specter2Call> calls = new CopyOnWriteArrayList<>();

		FakeSpecter2(boolean truncated) {
			this.truncated = truncated;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>Complejidad: O(d) en tiempo y espacio por la copia del vector.
		 */
		@Override
		public Specter2Embedding embed(String title, String abstractText) {
			calls.add(new Specter2Call(title, abstractText));
			return new Specter2Embedding(SPECTER2_VECTOR.clone(), truncated,
					truncated ? TRUNCATED_TOKEN_COUNT : NORMAL_TOKEN_COUNT);
		}

		/**
		 * Llamadas recibidas, en orden.
		 *
		 * @return copia de las llamadas
		 */
		public List<Specter2Call> calls() {
			return List.copyOf(calls);
		}

	}

	/** {@link NomicEmbedder} falso: vector fijo o falla según su modo, y pull que falla sus N primeras veces. */
	public static final class FakeNomic implements NomicEmbedder {

		private final boolean fails;
		private final int pullFailures;
		private final AtomicInteger ensureModelCalls = new AtomicInteger();
		private final List<String> inputs = new CopyOnWriteArrayList<>();

		FakeNomic(boolean fails, int pullFailures) {
			this.fails = fails;
			this.pullFailures = pullFailures;
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>Complejidad: O(1); cuenta la llamada y falla si es una de las {@code pullFailures} primeras.
		 *
		 * @throws IllegalStateException con {@link #NOMIC_PULL_FAILURE} en las N primeras llamadas
		 */
		@Override
		public void ensureModel() {
			int call = ensureModelCalls.incrementAndGet();
			if (call <= pullFailures) {
				throw new IllegalStateException(NOMIC_PULL_FAILURE);
			}
		}

		/**
		 * {@inheritDoc}
		 *
		 * <p>Complejidad: O(d) en tiempo y espacio por la copia del vector.
		 *
		 * @throws IllegalStateException con {@link #NOMIC_FAILURE} en modo falla
		 */
		@Override
		public float[] embed(String abstractText) {
			inputs.add(abstractText);
			if (fails) {
				throw new IllegalStateException(NOMIC_FAILURE);
			}
			return NOMIC_VECTOR.clone();
		}

		/**
		 * Veces que se llamó a {@link #ensureModel()}.
		 *
		 * @return el número de llamadas
		 */
		public int ensureModelCalls() {
			return ensureModelCalls.get();
		}

		/**
		 * Abstracts recibidos por {@link #embed}, en orden.
		 *
		 * @return copia de los abstracts
		 */
		public List<String> inputs() {
			return List.copyOf(inputs);
		}

	}

	/**
	 * Configuración de test que registra los dos fakes como embedders principales, con el modo que fijan las
	 * propiedades {@value #SPECTER2_TRUNCATED_PROPERTY} y {@value #NOMIC_FAILS_PROPERTY} (ambas {@code false}
	 * por defecto) y {@value #NOMIC_PULL_FAILURES_PROPERTY} ({@code 0} por defecto).
	 */
	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		/**
		 * Fake de SPECTER2 del contexto.
		 *
		 * @param environment propiedades del arranque
		 * @return el fake
		 */
		@Bean
		@Primary
		FakeSpecter2 fakeSpecter2Embedder(Environment environment) {
			return new FakeSpecter2(environment.getProperty(SPECTER2_TRUNCATED_PROPERTY, Boolean.class, false));
		}

		/**
		 * Fake de nomic-embed-text del contexto.
		 *
		 * @param environment propiedades del arranque
		 * @return el fake
		 */
		@Bean
		@Primary
		FakeNomic fakeNomicEmbedder(Environment environment) {
			return new FakeNomic(environment.getProperty(NOMIC_FAILS_PROPERTY, Boolean.class, false),
					environment.getProperty(NOMIC_PULL_FAILURES_PROPERTY, Integer.class, 0));
		}

	}

}
