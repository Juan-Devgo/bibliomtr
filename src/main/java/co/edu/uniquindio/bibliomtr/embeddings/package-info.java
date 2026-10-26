/**
 * Generación de embeddings del Requerimiento 1 (spec 001, REQ-9 y REQ-9a): SPECTER2 base exportado a ONNX y
 * ejecutado con ONNX Runtime (vector [CLS] con normalización L2, sobre {@code título [SEP] abstract}) y
 * nomic-embed-text servido por Ollama local (sobre {@code clustering: <abstract>}). Los modelos solo generan
 * vectores; la métrica de similitud no vive aquí: se implementa a mano en {@code algorithms/similarity}
 * (constitución P3). Este paquete no depende de {@code api} ni de {@code persistence} (P13).
 */
package co.edu.uniquindio.bibliomtr.embeddings;
