/**
 * Requerimiento 1: similitud textual entre abstracts con algoritmos clásicos y con métricas sobre embeddings
 * (spec {@code docs/specs/001-similitud-textual}).
 *
 * <p><b>Qué contiene.</b> Los contratos y los 6 stubs que completan los estudiantes (constitución P1):
 * <ul>
 *   <li>Distancias de edición sobre secuencias de símbolos ({@link
 *       co.edu.uniquindio.bibliomtr.algorithms.similarity.SequenceSimilarity}): {@code Levenshtein} y
 *       {@code NeedlemanWunsch} (con {@link co.edu.uniquindio.bibliomtr.algorithms.similarity.NeedlemanWunschScoring}).</li>
 *   <li>Similitud de conjuntos: {@code Jaccard}, también sobre secuencias.</li>
 *   <li>Similitud con el corpus ({@link co.edu.uniquindio.bibliomtr.algorithms.similarity.CorpusSimilarity}):
 *       {@code TfIdfCosineSimilarity}, con el IDF calculado sobre todos los abstracts seleccionables.</li>
 *   <li>Métricas sobre embeddings ({@link co.edu.uniquindio.bibliomtr.algorithms.similarity.VectorSimilarity}):
 *       {@code CosineSimilarity} y {@code EuclideanDistance}, aplicadas a los vectores de SPECTER2 y de
 *       nomic-embed-text.</li>
 * </ul>
 *
 * <p><b>Por qué así.</b> Cada algoritmo devuelve un {@link
 * co.edu.uniquindio.bibliomtr.algorithms.similarity.SimilarityResult} con el valor crudo, la similitud
 * normalizada a [0,1] y una traza de pasos intermedios (constitución P7, paquete
 * {@code co.edu.uniquindio.bibliomtr.trace.similarity}), para que la API, la interfaz y el documento técnico
 * muestren lo mismo. El andamiaje no contiene la lógica de ningún algoritmo ni de su normalización (REQ-4):
 * cada stub lanza {@code UnsupportedOperationException} con un mensaje que empieza por {@code "TODO estudiante:"}
 * y documenta en su Javadoc la fórmula, la normalización sugerida, la traza, el rango y los casos de borde.
 *
 * <p><b>Guía.</b> Qué implementar, dónde, con qué fórmula y con qué tests se explica en
 * {@code docs/guia-implementacion.md}. Los tests de cada algoritmo se ejecutan con
 * {@code ./gradlew studentTest --tests <Clase>Test}.
 *
 * <p>Este paquete no depende de {@code api} ni de {@code persistence} (constitución P13), ni de librerías o
 * funciones de base de datos que calculen similitud (constitución P1).
 */
package co.edu.uniquindio.bibliomtr.algorithms.similarity;
