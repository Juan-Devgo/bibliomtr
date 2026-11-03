/**
 * Requerimiento 4: coincidencia exacta de cadenas en los abstracts del corpus (spec
 * {@code docs/specs/004-busqueda-patrones}).
 *
 * <p><b>Qué contiene.</b> El contrato de dos fases
 * {@link co.edu.uniquindio.bibliomtr.algorithms.stringmatching.StringMatcher} y los 3 stubs que completan los
 * estudiantes (constitución P1):
 * <ul>
 *   <li>{@code KnuthMorrisPratt}: función de prefijo π (longitud del prefijo propio más largo que también es
 *       sufijo); la búsqueda nunca retrocede el índice del texto (REQ-4).</li>
 *   <li>{@code BoyerMoore}: regla del carácter malo y regla del sufijo bueno (fuerte), comparación de derecha a
 *       izquierda y desplazamiento igual al máximo de las dos reglas, mínimo 1 (REQ-5).</li>
 *   <li>{@code RabinKarp}: hash polinomial modular con d = 65 536 fijo y q primo (101 por defecto),
 *       h = d^(m−1) mod q, rolling hash y verificación carácter a carácter de cada coincidencia de hash
 *       (REQ-6).</li>
 * </ul>
 *
 * <p><b>Dos fases.</b> {@code preprocess(patrón, opciones)} corre una sola vez por búsqueda y devuelve las tablas
 * del algoritmo (un record de {@code co.edu.uniquindio.bibliomtr.trace.stringmatching}); {@code search(texto,
 * preprocesado, opciones)} corre por cada abstract reutilizándolas y devuelve un
 * {@link co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchResult} con las posiciones, las colisiones de
 * hash (solo Rabin-Karp) y la traza (constitución P7). Así se mide cada fase por separado (constitución P11).
 *
 * <p><b>Contrato común.</b> Posiciones base 0 en {@code char}, ascendentes, incluidas las solapadas; comparación
 * {@code char} a {@code char} sin normalizar (distingue mayúsculas y tildes, REQ-3); patrón vacío →
 * {@code IllegalArgumentException}; texto vacío o más corto que el patrón → lista vacía (REQ-2). Estas guardas
 * también las escribe el estudiante.
 *
 * <p><b>Andamiaje.</b> El paquete no contiene la lógica de ningún algoritmo ni la delega a funciones nativas de
 * búsqueda de cadenas, expresiones regulares o librerías (REQ-7; lo verifica
 * {@code ForbiddenStringMatchingUsageTest}). Solo valida las posiciones que devuelve un algoritmo implementado
 * ({@link co.edu.uniquindio.bibliomtr.algorithms.stringmatching.MatchPositionsValidator}) sin buscar el patrón
 * (REQ-8).
 *
 * <p><b>Guía.</b> Qué implementar, dónde, con qué definición formal, traza esperada, caso a mano y tests se explica
 * en {@code docs/guia-implementacion.md}, § 11. Los tests de cada algoritmo se ejecutan con
 * {@code ./gradlew studentTest --tests <Clase>Test}.
 *
 * <p>Este paquete no depende de {@code api} ni de {@code persistence} (constitución P13).
 */
package co.edu.uniquindio.bibliomtr.algorithms.stringmatching;
