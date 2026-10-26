package co.edu.uniquindio.bibliomtr.embeddings;

/**
 * Modelos preentrenados de embeddings del Requerimiento 1 (spec 001, REQ-9; constitución P3).
 *
 * <p><b>Qué representa.</b> Cada constante identifica un modelo que solo genera vectores; la métrica de
 * similitud sobre ellos (coseno y euclidiana) la implementan a mano los estudiantes en
 * {@code algorithms/similarity}.
 * <ul>
 *   <li>{@link #SPECTER2}: {@code allenai/specter2_base} sin adaptador, exportado a ONNX y ejecutado con ONNX
 *       Runtime. Recibe {@code título [SEP] abstract} (excepción de la constitución 1.1.0, P5).</li>
 *   <li>{@link #NOMIC}: {@code nomic-embed-text} servido por Ollama local. Recibe
 *       {@code clustering: <abstract>}.</li>
 * </ul>
 *
 * <p><b>Por qué.</b> Las dimensiones fijas permiten validar cada vector antes de guardarlo en la columna
 * {@code vector(768)} y detectar un modelo equivocado (p. ej. otra versión) en vez de guardar datos inválidos.
 *
 * <p>Complejidad: O(1) en tiempo y espacio para todos sus métodos.
 */
public enum EmbeddingModel {

	/** SPECTER2 base: vector del token [CLS] con normalización L2, 768 dimensiones. */
	SPECTER2("allenai/specter2_base", 768),

	/** nomic-embed-text v1.5 vía Ollama, 768 dimensiones. */
	NOMIC("nomic-embed-text", 768);

	private final String modelName;
	private final int dimension;

	EmbeddingModel(String modelName, int dimension) {
		this.modelName = modelName;
		this.dimension = dimension;
	}

	/**
	 * Nombre público del modelo preentrenado, para mensajes y documentación.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return p. ej. {@code allenai/specter2_base}
	 */
	public String modelName() {
		return modelName;
	}

	/**
	 * Número de componentes del vector que produce el modelo.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @return 768 en ambos modelos
	 */
	public int dimension() {
		return dimension;
	}
}
