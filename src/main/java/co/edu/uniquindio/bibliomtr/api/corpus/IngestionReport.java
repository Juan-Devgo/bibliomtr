package co.edu.uniquindio.bibliomtr.api.corpus;

/**
 * Resumen de una corrida de la ingesta del corpus (spec 001, REQ-8): cuántos PDF terminaron en cada caso.
 *
 * <p><b>Por qué.</b> Se registra en el log al final de cada arranque, de modo que se ve de un vistazo si algún
 * artículo quedó sin abstract o falló y se reintentará en el siguiente arranque.
 *
 * <p>Complejidad: O(1) en tiempo y espacio.
 *
 * @param ingested        PDF nuevos guardados con abstract y los dos embeddings
 * @param alreadyPresent  PDF saltados porque su SHA-256 ya estaba en la base (AC-11)
 * @param withoutAbstract PDF nuevos guardados sin abstract detectado, no seleccionables (AC-9)
 * @param failed          PDF no guardados por un error de lectura o de embeddings; se reintentan al reiniciar
 */
public record IngestionReport(int ingested, int alreadyPresent, int withoutAbstract, int failed) {

	/** Corrida sin PDF: corpus vacío o inexistente (AC-10). */
	public static final IngestionReport EMPTY = new IngestionReport(0, 0, 0, 0);

}
