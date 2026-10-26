package co.edu.uniquindio.bibliomtr.api.corpus;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import co.edu.uniquindio.bibliomtr.config.BibliomtrProperties;

/**
 * Lanza la ingesta del corpus cuando la aplicación termina de arrancar (spec 001, REQ-8, AC-8).
 *
 * <p><b>Por qué condicionado.</b> Solo existe si {@code bibliomtr.ingestion.enabled=true}. Los tests la
 * desactivan en {@code src/test/resources/config/application.properties} para no leer {@code corpus/} ni cargar
 * modelos, y solo {@code CorpusIngestionIT} la activa.
 *
 * <p><b>Por qué en {@link ApplicationReadyEvent}.</b> En ese momento Flyway ya creó la tabla y todos los beans
 * están listos. La ingesta corre en el hilo de arranque, así que al terminar {@code SpringApplication.run} el
 * corpus ya está en la base.
 */
@Component
@ConditionalOnBooleanProperty("bibliomtr.ingestion.enabled")
public class CorpusIngestionRunner {

	private final CorpusIngestionService service;
	private final BibliomtrProperties properties;

	/**
	 * Runner sobre el servicio de ingesta y el directorio {@code bibliomtr.corpus.dir}.
	 *
	 * <p>Complejidad: O(1) en tiempo y espacio.
	 *
	 * @param service    servicio de ingesta
	 * @param properties propiedades {@code bibliomtr.*}
	 */
	public CorpusIngestionRunner(CorpusIngestionService service, BibliomtrProperties properties) {
		this.service = service;
		this.properties = properties;
	}

	/**
	 * Ingesta los PDF de {@code bibliomtr.corpus.dir}. Los errores por PDF los registra el servicio y no impiden
	 * el arranque.
	 *
	 * <p>Complejidad: la de {@link CorpusIngestionService#ingest}.
	 */
	@EventListener(ApplicationReadyEvent.class)
	void onReady() {
		service.ingest(properties.corpus().dir());
	}

}
