package co.edu.uniquindio.bibliomtr.embeddings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * SPECTER2 base ({@code allenai/specter2_base}, sin adaptador) exportado a ONNX y ejecutado con ONNX Runtime
 * (spec 001, REQ-9, REQ-9a y AC-8a).
 *
 * <p><b>Qué hace.</b> Para {@code título [SEP] abstract}:
 * <ol>
 *   <li>Tokeniza con el tokenizer WordPiece del modelo (DJL, {@code tokenizer.json}), que antepone [CLS] y
 *       agrega [SEP] al final, sin truncar ni rellenar.</li>
 *   <li>Si hay más de {@link #MAX_TOKENS} tokens, conserva los primeros 511 y termina la secuencia con [SEP],
 *       como el truncado de HuggingFace: el modelo nunca ve más de 512 posiciones
 *       ({@code max_position_embeddings}).</li>
 *   <li>Ejecuta el modelo con {@code input_ids}, {@code attention_mask} y {@code token_type_ids} y toma
 *       {@code last_hidden_state[0][0]}, el vector del token [CLS] (pooling de SPECTER2).</li>
 *   <li>Lo normaliza con L2 ({@link VectorNormalization#l2Normalize}).</li>
 * </ol>
 *
 * <p><b>Por qué.</b> El modelo solo genera el vector (constitución P2 y P3); el pooling [CLS] y la norma L2 son
 * parte del andamiaje (Clarifications 2026-10-18 y 2026-10-23), y las métricas sobre el vector son stubs de los
 * estudiantes. El truncado es explícito, en lugar de delegarlo al tokenizer, para conocer el número de tokens
 * antes de truncar y marcar el artículo (REQ-9a).
 *
 * <p><b>Carga perezosa.</b> El tokenizer y la sesión ONNX (unos 440 MB) se cargan la primera vez que se usan,
 * dentro de un bloque {@code synchronized}; si no hay PDF por ingerir, nunca se cargan. Si el modelo es un
 * archivo en disco ({@link Resource#isFile()}), ONNX Runtime lo lee desde su ruta sin copiarlo al heap de la JVM.
 *
 * <p>Las instancias son seguras para uso concurrente: {@link OrtSession#run} y el tokenizer admiten llamadas
 * concurrentes, y la carga está sincronizada.
 */
public final class Specter2OnnxEmbedder implements Specter2Embedder, AutoCloseable {

	/** Máximo de tokens de SPECTER2, incluidos [CLS] y [SEP] (REQ-9a). */
	public static final int MAX_TOKENS = 512;

	private static final Logger log = LoggerFactory.getLogger(Specter2OnnxEmbedder.class);

	private static final String INPUT_IDS = "input_ids";
	private static final String ATTENTION_MASK = "attention_mask";
	private static final String TOKEN_TYPE_IDS = "token_type_ids";
	private static final String LAST_HIDDEN_STATE = "last_hidden_state";

	private final Resource modelPath;
	private final Resource tokenizerPath;

	private final Object lock = new Object();
	private volatile HuggingFaceTokenizer tokenizer;
	private volatile OrtSession session;
	private OrtEnvironment environment;

	/**
	 * Crea el embedder sin cargar nada todavía.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param modelPath     {@code model.onnx} de SPECTER2 ({@code scripts/download-model.sh}); no nulo
	 * @param tokenizerPath {@code tokenizer.json} de SPECTER2; no nulo
	 */
	public Specter2OnnxEmbedder(Resource modelPath, Resource tokenizerPath) {
		this.modelPath = Objects.requireNonNull(modelPath, "modelPath");
		this.tokenizerPath = Objects.requireNonNull(tokenizerPath, "tokenizerPath");
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Complejidad: O(c) para tokenizar, con c = caracteres del texto, más la inferencia del modelo
	 * (O(L²·h + L·h²) por capa, con L ≤ 512 tokens y h = 768); O(L·h) en espacio para la salida.
	 *
	 * @throws IllegalStateException si el modelo o el tokenizer no están disponibles, si ONNX Runtime falla o
	 *                               si la salida no tiene {@link EmbeddingModel#SPECTER2} dimensiones
	 */
	@Override
	public Specter2Embedding embed(String title, String abstractText) {
		Specter2Encoding encoding = encode(Specter2Input.of(title, abstractText));
		float[] cls = runClsVector(encoding);
		return new Specter2Embedding(VectorNormalization.l2Normalize(cls), encoding.truncated(),
				encoding.tokenCount());
	}

	/**
	 * Tokeniza {@code text} y lo trunca a {@link #MAX_TOKENS} tokens, conservando [CLS] al inicio y [SEP] al
	 * final. Visible en el paquete para probar el truncado sin cargar el modelo ONNX.
	 *
	 * <p>Complejidad: O(c) en tiempo y espacio, con c = caracteres del texto.
	 *
	 * @param text texto completo ({@code título [SEP] abstract}); no nulo
	 * @return los ids, máscaras y datos del truncado
	 */
	Specter2Encoding encode(String text) {
		Objects.requireNonNull(text, "text");
		Encoding full = tokenizer().encode(text);
		long[] ids = full.getIds();
		long[] mask = full.getAttentionMask();
		long[] types = full.getTypeIds();
		int tokenCount = ids.length;
		if (tokenCount <= MAX_TOKENS) {
			return new Specter2Encoding(ids, mask, types, false, tokenCount);
		}
		// Truncado: los primeros 511 tokens ([CLS] incluido) y el [SEP] final de la codificación completa.
		long sep = ids[tokenCount - 1];
		long[] truncatedIds = Arrays.copyOf(ids, MAX_TOKENS);
		truncatedIds[MAX_TOKENS - 1] = sep;
		return new Specter2Encoding(truncatedIds, Arrays.copyOf(mask, MAX_TOKENS), Arrays.copyOf(types, MAX_TOKENS),
				true, tokenCount);
	}

	/**
	 * Cierra la sesión ONNX y el tokenizer, si se cargaron. Spring lo llama al cerrar el contexto.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio (libera la memoria nativa del modelo).
	 */
	@Override
	public void close() {
		synchronized (lock) {
			if (session != null) {
				try {
					session.close();
				} catch (OrtException e) {
					log.warn("No se pudo cerrar la sesión ONNX de SPECTER2: {}", e.getMessage());
				}
				session = null;
			}
			if (tokenizer != null) {
				tokenizer.close();
				tokenizer = null;
			}
		}
	}

	/** Ejecuta el modelo y devuelve {@code last_hidden_state[0][0]} sin normalizar. */
	private float[] runClsVector(Specter2Encoding encoding) {
		OrtSession activeSession = session();
		Map<String, OnnxTensor> inputs = new HashMap<>();
		try {
			// Lote de tamaño 1: tensores de forma [1, L].
			inputs.put(INPUT_IDS, OnnxTensor.createTensor(environment, new long[][] {encoding.inputIds()}));
			inputs.put(ATTENTION_MASK, OnnxTensor.createTensor(environment, new long[][] {encoding.attentionMask()}));
			if (activeSession.getInputNames().contains(TOKEN_TYPE_IDS)) {
				inputs.put(TOKEN_TYPE_IDS, OnnxTensor.createTensor(environment, new long[][] {encoding.tokenTypeIds()}));
			}
			try (OrtSession.Result result = activeSession.run(inputs)) {
				Optional<OnnxValue> output = result.get(LAST_HIDDEN_STATE);
				if (output.isEmpty()) {
					throw new IllegalStateException("El modelo SPECTER2 no tiene la salida " + LAST_HIDDEN_STATE);
				}
				// Forma [1, L, 768]: el token 0 es [CLS].
				float[][][] hidden = (float[][][]) output.get().getValue();
				float[] cls = hidden[0][0];
				int expected = EmbeddingModel.SPECTER2.dimension();
				if (cls.length != expected) {
					throw new IllegalStateException(
							"SPECTER2 devolvió " + cls.length + " dimensiones; se esperaban " + expected);
				}
				return cls.clone();
			}
		} catch (OrtException e) {
			throw new IllegalStateException("No se pudo ejecutar SPECTER2: " + e.getMessage(), e);
		} finally {
			for (OnnxTensor tensor : inputs.values()) {
				tensor.close();
			}
		}
	}

	/** Tokenizer cargado de forma perezosa, sin truncado ni relleno. */
	private HuggingFaceTokenizer tokenizer() {
		HuggingFaceTokenizer current = tokenizer;
		if (current != null) {
			return current;
		}
		synchronized (lock) {
			if (tokenizer == null) {
				requireExists(tokenizerPath);
				// El truncado lo hace encode, para conocer el número de tokens antes de truncar.
				Map<String, String> options = Map.of("addSpecialTokens", "true", "truncation", "false",
						"padding", "false");
				try (InputStream in = tokenizerPath.getInputStream()) {
					tokenizer = HuggingFaceTokenizer.newInstance(in, options);
				} catch (IOException e) {
					throw new UncheckedIOException("No se pudo leer el tokenizer de SPECTER2: " + tokenizerPath, e);
				}
			}
			return tokenizer;
		}
	}

	/** Sesión ONNX cargada de forma perezosa, desde la ruta en disco si es posible. */
	private OrtSession session() {
		OrtSession current = session;
		if (current != null) {
			return current;
		}
		synchronized (lock) {
			if (session == null) {
				requireExists(modelPath);
				log.info("Cargando SPECTER2 desde {}", modelPath);
				environment = OrtEnvironment.getEnvironment();
				try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
					session = modelPath.isFile()
							? environment.createSession(modelPath.getFile().getAbsolutePath(), options)
							: environment.createSession(modelPath.getContentAsByteArray(), options);
				} catch (OrtException e) {
					throw new IllegalStateException("No se pudo cargar el modelo SPECTER2: " + e.getMessage(), e);
				} catch (IOException e) {
					throw new UncheckedIOException("No se pudo leer el modelo SPECTER2: " + modelPath, e);
				}
			}
			return session;
		}
	}

	private static void requireExists(Resource resource) {
		if (!resource.exists()) {
			throw new IllegalStateException(
					"No existe " + resource + "; ejecute scripts/download-model.sh para obtener SPECTER2");
		}
	}

	/**
	 * Entrada del modelo para un texto, ya truncada.
	 *
	 * @param inputIds      ids de los tokens, con [CLS] al inicio y [SEP] al final; a lo sumo 512
	 * @param attentionMask 1 por cada token (no hay relleno)
	 * @param tokenTypeIds  0 por cada token (una sola secuencia)
	 * @param truncated     {@code true} si el texto tenía más de 512 tokens
	 * @param tokenCount    número de tokens antes de truncar, con [CLS] y [SEP]
	 */
	record Specter2Encoding(long[] inputIds, long[] attentionMask, long[] tokenTypeIds, boolean truncated,
			int tokenCount) {
	}
}
