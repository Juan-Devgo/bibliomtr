# Plan: Entorno para el Requerimiento 4: Coincidencia exacta de cadenas (KMP, Boyer-Moore, Rabin-Karp)

## 1. Metadata
- Spec: `docs/specs/004-busqueda-patrones/spec.md`
- Updated: 2026-11-03
- Constitution version: 1.2.0

## 2. Constitution check
- P1: los 3 stubs de `algorithms/stringmatching` (`KnuthMorrisPratt`, `BoyerMoore`, `RabinKarp`) solo lanzan `UnsupportedOperationException("TODO estudiante: …")` en `preprocess` y en `search`; las guardas (patrón vacío, texto vacío o más corto) las escribe el estudiante. `ForbiddenStringMatchingUsageTest` exige que existan y prohíbe en `algorithms/stringmatching`, `trace/stringmatching` y `api/stringmatching` (incluido `PendingStubs`) los `.<nombre>(` de REQ-14, `java.util.regex`, `Pattern` y `Matcher` como identificadores, y en los `.ts`/`.tsx` de producción de `web/src/features/pattern-search` (sin `*.test.*` ni `*.spec.*`, más los componentes que importa) los nombres de REQ-14. El código nuevo tampoco usa `TreeMap`, `TreeSet`, `PriorityQueue` ni `.sort(` (`ForbiddenSortUsageTest`).
- P2: Recharts solo grafica desplazamientos, saltos y hashes de la traza; KaTeX solo dibuja fórmulas; JMH solo mide. `highlightSegments.ts` y `traceSteps.ts` solo reorganizan posiciones y pasos que devuelve la API.
- P4: la sección 11 de la guía declara que el andamiaje se generó con IA generativa como apoyo.
- P5: el texto buscado es `Article.getAbstractText()` de los artículos con `abstract_detected = true`; nunca el título ni el PDF.
- P6: el Javadoc de cada stub trae "Qué calcula", "Definición formal" (π; carácter malo, sufijo bueno fuerte y máximo; hash polinomial, h = d^(m−1) mod q y rolling hash), "Preprocesamiento", "Búsqueda", "Traza" y "Casos de borde"; el visor muestra las mismas fórmulas en KaTeX.
- P7: `preprocess` devuelve un record de `trace/stringmatching` con las tablas; `search` devuelve `MatchResult(positions, collisions, trace)` con la traza de la búsqueda; `GET /api/pattern-search/trace` los expone y `features/pattern-search` los muestra paso a paso.
- P8: todo tipo y método público nuevo lleva Javadoc/JSDoc en español con qué, por qué y complejidad; cada stub deja "Complejidad del preprocesamiento: TODO estudiante" y "Complejidad de la búsqueda: TODO estudiante" (`StringMatchingStubScaffoldTest`).
- P9: `docs/guia-implementacion.md` § 11 y filas del Requerimiento 4 en `docs/traceability.md` con la sección `(pendiente)`; `PatternSearchGuideTest` lo verifica. No se tocan § 10 ni las filas del Requerimiento 3.
- P10: tests `student` con los casos a mano de §6 y los bordes: texto vacío, patrón vacío, patrón más largo, colisión de hash (q = 3) y abstracts idénticos.
- P11: la respuesta de búsqueda trae por algoritmo `preprocessingMs` (un solo `preprocess`) y `searchMs` (suma de los `search`), con la traza desactivada; `StringMatchingBenchmark` y `RabinKarpBenchmark` miden cada fase en métodos JMH separados.
- P12: el abstract se busca tal como está guardado; d = 65 536 fijo, q por defecto 101; JMH con `java.util.Random(42)` y parámetros fijos.
- P13: `algorithms/stringmatching`, `trace/stringmatching`, `api/stringmatching`, `config/StringMatchingConfig`, `src/jmh/.../evaluation` y `web/src/features/pattern-search` son módulos separados; `algorithms` y `trace` no importan `api` ni `persistence`; los abstracts se leen con `ArticleRepository` de la spec 001.

## 3. Architecture
Búsqueda (`GET /api/pattern-search?pattern=…&q=…`):
1. `PatternSearchValidator` valida `pattern` (no nulo, 1 a 200 `char`, sin recortar) y `q` (texto opcional; ausente → 101; si viene, entero decimal primo en [2, 1 000 000 007]); si falla, `IllegalArgumentException` → 400 por el handler existente.
2. `PatternSearchService` lee `ArticleRepository.findAllByAbstractDetectedTrueOrderByFileNameAsc()`.
3. Por cada `StringMatchingAlgorithm` (orden KMP, Boyer-Moore, Rabin-Karp), con `MatchOptions(q, false)`:
   - cronometra un solo `preprocess(pattern, options)`; un `UnsupportedOperationException` con prefijo "TODO estudiante:" (`PendingStubs.isPending`) deja el algoritmo PENDIENTE con `<archivo>#preprocess`;
   - por cada abstract cronometra `search(abstract, preprocesado, options)` y suma los tiempos; un pendiente aquí deja PENDIENTE con `<archivo>#search`;
   - `MatchPositionsValidator.validate` revisa cada lista de posiciones; si es inválida lanza `InvalidMatchPositionsException` → 500.
4. `allMatch` = `null` con menos de 2 algoritmos CALCULADO; si no, `true` cuando todos los CALCULADO tienen listas iguales en cada artículo.
5. La respuesta trae los resultados por algoritmo y la lista de artículos con su abstract, para que la interfaz resalte.

Traza (`GET /api/pattern-search/trace?algorithm=…&articleId=…&pattern=…&q=…`): valida `pattern`, `q`, `algorithm` y el artículo (en ese orden); corre `preprocess` y un `search` sobre ese abstract con `MatchOptions(q, true)`; valida las posiciones; devuelve preprocesamiento, traza, posiciones y colisiones, o PENDIENTE con 200.

Interfaz: pestaña "Búsqueda Patrones" (`App.tsx`, carga diferida de `PatternSearchPage`). La página contiene `PatternSearchForm` (patrón, q opcional, "Buscar"), `PatternSearchResults` (una tabla por algoritmo con ocurrencias, posiciones, colisiones en Rabin-Karp, tiempos, total y aviso de pendiente; indicador de coincidencia si no es null; "Sin abstracts en el corpus"), `AlgorithmSelector`, `HighlightedAbstract` (tramos de `highlightSegments`) y `PatternTraceView` (traza del algoritmo y artículo elegidos). Los datos se cargan con `usePatternSearch` y `usePatternTrace` (TanStack Query).

## 4. Design patterns
- Strategy en `StringMatcher<P, T>`: el servicio corre los 3 algoritmos con una sola firma de dos fases y los tests de API los reemplazan por un catálogo falso.
- Registro `StringMatchingCatalog` (record con las 3 estrategias, bean en `StringMatchingConfig`): un solo bean `@Primary` de test (`FakeStringMatchingCatalog`) sustituye los 3 algoritmos y registra las llamadas (espía), igual que `ClusteringCatalog`.

## 5. Code structure
Backend, producción (`src/main/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/stringmatching/package-info.java` - Javadoc del módulo del Requerimiento 4 y enlace a la guía § 11.
- create: `algorithms/stringmatching/StringMatcher.java` - contrato Strategy de dos fases.
- create: `algorithms/stringmatching/MatchOptions.java` - q, d fijo y grabación de traza.
- create: `algorithms/stringmatching/MatchResult.java` - posiciones, colisiones y traza.
- create: `algorithms/stringmatching/KnuthMorrisPratt.java`, `BoyerMoore.java`, `RabinKarp.java` - los 3 stubs.
- create: `algorithms/stringmatching/MatchPositionsValidator.java` - validación de posiciones sin buscar el patrón.
- create: `algorithms/stringmatching/InvalidMatchPositionsException.java` - posiciones inválidas (500).
- create: `trace/stringmatching/package-info.java`, `KmpPreprocessing.java`, `KmpTrace.java`, `BoyerMoorePreprocessing.java`, `BoyerMooreTrace.java`, `RabinKarpPreprocessing.java`, `RabinKarpTrace.java` - contratos de preprocesamiento y traza.
- create: `api/stringmatching/PendingStubs.java` - detección del prefijo "TODO estudiante:" carácter a carácter, dentro del paquete escaneado.
- create: `api/stringmatching/StringMatchingAlgorithm.java` - enum con slug, etiqueta, archivo del stub y `pendingLocation(method)`.
- create: `api/stringmatching/StringMatchingCatalog.java` - record con las 3 estrategias.
- create: `api/stringmatching/PatternSearchValidator.java` - reglas de patrón y q (primalidad por división de prueba).
- create: `api/stringmatching/PatternSearchService.java`, `PatternSearchController.java`, `PatternSearchDtos.java` - búsqueda y traza.
- create: `config/StringMatchingConfig.java` - bean `StringMatchingCatalog` con los 3 stubs.
- modify: `api/ApiExceptionHandler.java` - `InvalidMatchPositionsException` → 500 con título "Error interno".
- modify: `build.gradle.kts` - descripciones de `studentTest` y `scaffoldTest` que nombran también la búsqueda de patrones.

Backend, tests (`src/test/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/stringmatching/StringMatchingCases.java` - casos compartidos, `run(matcher, text, pattern, options)` y aserciones.
- create: `algorithms/stringmatching/GuideFragment.java` - archivo PDF, fragmento literal, patrón, posiciones y tablas a mano del ejemplo de la guía.
- create: `algorithms/stringmatching/KnuthMorrisPrattTest.java`, `BoyerMooreTest.java`, `RabinKarpTest.java` - `@Tag("student")`.
- create: `algorithms/stringmatching/StringMatchingStubScaffoldTest.java` - `@Tag("scaffold")`.
- create: `algorithms/stringmatching/StringMatchingStubDocumentationTest.java`, `ForbiddenStringMatchingUsageTest.java`, `MatchPositionsValidatorTest.java` - suite estándar.
- create: `oracle/StringMatchingOracleTest.java` - `@Tag("student")`, contra `String.indexOf`.
- create: `oracle/GuideFragmentCorpusTest.java` - suite estándar; el fragmento sale del abstract real (se omite sin el PDF).
- create: `api/stringmatching/FakeStringMatchingCatalog.java` - algoritmos falsos configurables y espía de llamadas.
- create: `api/stringmatching/PatternSearchApiIT.java` - `@SpringBootTest` con Postgres y el catálogo falso.
- create: `api/stringmatching/PendingStubsTest.java` - prefijo exacto, sin `startsWith`.
- create: `api/stringmatching/PatternSearchDtosTest.java` - `lastOccurrence` de Boyer-Moore como lista ordenada en el JSON.
- create: `docs/PatternSearchGuideTest.java` - guía § 11 y trazabilidad del Requerimiento 4.

Benchmarks (`src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/`):
- create: `StringMatchingBenchmark.java` - KMP y Boyer-Moore, preprocesamiento y búsqueda.
- create: `RabinKarpBenchmark.java` - Rabin-Karp con `@Param q`.

Frontend (`web/src/`):
- create: `types/patternSearch.ts` - tipos de respuestas y trazas.
- create: `api/usePatternSearch.ts`, `api/usePatternTrace.ts` - hooks.
- create: `features/pattern-search/highlightSegments.ts` (+ `highlightSegments.test.ts`) - posiciones → tramos resaltados.
- create: `features/pattern-search/PatternSearchForm.tsx`, `PatternSearchResults.tsx`, `AlgorithmOccurrencesTable.tsx`, `AlgorithmSelector.tsx`, `HighlightedAbstract.tsx`, `PatternSearchView.tsx` (+ `PatternSearchView.test.tsx`).
- create: `features/pattern-search/traceSteps.ts` (+ `traceSteps.test.ts`) - traza → pasos uniformes del visor.
- create: `features/pattern-search/StepNavigator.tsx`, `AlignedWindow.tsx`, `patternFormulas.ts`, `KmpTraceView.tsx`, `BoyerMooreTraceView.tsx`, `RabinKarpTraceView.tsx`, `TraceChart.tsx`, `PatternTraceView.tsx` (+ `PatternTraceView.test.tsx`).
- create: `pages/PatternSearchPage.tsx` - página de la pestaña.
- create: `test/patternSearchFixtures.ts` - respuestas y trazas de los casos a mano.
- create: `App.test.tsx` - la pestaña existe y abre la página.
- modify: `App.tsx` - pestaña `pattern-search` "Búsqueda Patrones" con carga diferida.

Documentación:
- modify: `docs/guia-implementacion.md` - sección "## 11. Requerimiento 4: Coincidencia exacta de cadenas" al final, sin tocar § 10.
- modify: `docs/traceability.md` - filas del Requerimiento 4, sin tocar las del Requerimiento 3.
- modify: `README.md` - sección de búsqueda de patrones, comandos de test y de JMH.

## 6. Interfaces and data
Algoritmos (`algorithms/stringmatching`):
```java
public interface StringMatcher<P, T> {
    P preprocess(String pattern, MatchOptions options);
    MatchResult<T> search(String text, P preprocessed, MatchOptions options);
}
public record MatchOptions(long q, boolean recordTrace) {          // compacto: q ≥ 2, si no IllegalArgumentException
    public static final long D = 65_536L;
    public static final long DEFAULT_Q = 101L;
    public static final long MAX_Q = 1_000_000_007L;
    public static MatchOptions defaults();                         // (101, false)
    public static MatchOptions tracing(long q);                    // (q, true)
}
public record MatchResult<T>(List<Integer> positions, List<Integer> collisions, T trace) {}
    // copias inmodificables que conservan null; collisions null → List.of(); trace null si recordTrace = false
public final class KnuthMorrisPratt implements StringMatcher<KmpPreprocessing, KmpTrace> {}
public final class BoyerMoore implements StringMatcher<BoyerMoorePreprocessing, BoyerMooreTrace> {}
public final class RabinKarp implements StringMatcher<RabinKarpPreprocessing, RabinKarpTrace> {}
public final class MatchPositionsValidator {
    public static void validate(String algorithm, long articleId, int textLength, int patternLength, List<Integer> positions);
}
public class InvalidMatchPositionsException extends IllegalStateException { public InvalidMatchPositionsException(String message); }
```
- Cuerpo de cada método de stub: `throw new UnsupportedOperationException("TODO estudiante: implementar <Clase>.<método> (ver docs/guia-implementacion.md § 11)");`.
- Contrato que implementa el estudiante: `preprocess` lanza `IllegalArgumentException` si el patrón es nulo o vacío; `search` devuelve `positions` vacía si el texto está vacío o n < m; posiciones base 0 en `char`, ascendentes, con solapadas; comparación `char` a `char` sin normalizar. `collisions` solo la llena Rabin-Karp (posiciones s con hash igual y texto distinto, ascendentes). Rabin-Karp usa `long` y `Math.floorMod`; con q ≤ 1 000 000 007 y d = 65 536 los productos caben en `long`.
- `MatchPositionsValidator.validate` lanza `InvalidMatchPositionsException("El algoritmo <slug> devolvió posiciones inválidas en el artículo <id>: <razón>")`; razones: "el resultado es nulo" (lo usa el servicio), "la lista de posiciones es nula", "la posición <k> es nula", "la posición <p> está fuera de [0, <n − m>]" (con n < m toda posición es inválida), "la posición <p> no es mayor que la anterior <q>" (desorden o repetido). Solo compara enteros; no lee el texto ni el patrón.

Preprocesamiento y trazas (`trace/stringmatching`, records con copias defensivas; los mapas con `LinkedHashMap` inmodificable para conservar el orden):
```java
public record KmpPreprocessing(String pattern, int[] pi, List<PrefixStep> steps) {
    public record PrefixStep(int i, char character, List<Integer> candidates, int value) {}
}
public record KmpTrace(List<KmpComparison> comparisons, List<Integer> occurrences) {
    public record KmpComparison(int i, int j, int shift, char textChar, char patternChar, boolean matched,
                                Integer fallbackTo, Integer occurrence) {}
}
public record BoyerMoorePreprocessing(String pattern, Map<Character, Integer> lastOccurrence, int[] borderPositions,
                                      int[] goodSuffixShift, int patternBorder, int matchShift) {}
public record BoyerMooreTrace(List<Alignment> alignments, List<Integer> occurrences) {
    public record Alignment(int shift, List<CharComparison> comparisons, Integer mismatchIndex, boolean occurrence,
                            Integer badCharacterShift, Integer goodSuffixShift, int appliedShift) {}
    public record CharComparison(int j, int textIndex, char textChar, char patternChar, boolean matched) {}
}
public record RabinKarpPreprocessing(String pattern, long d, long q, long h, long patternHash) {}
public record RabinKarpTrace(long d, long q, long h, long patternHash, Long initialWindowHash, List<Window> windows,
                             List<Integer> collisions, List<Integer> occurrences) {
    public record Window(int shift, long hash, boolean hashMatches, Boolean verified, boolean collision) {}
}
```
- `PrefixStep` (uno por i = 1 … m − 1): `candidates` = valores de k probados en orden (k inicial = π[i − 1], luego cada π[k − 1]); `value` = π[i]. Con `recordTrace = false`, `steps` = `List.of()`.
- `KmpComparison`: `shift` = s = i − j lo calcula el backend; `fallbackTo` = j nuevo tras j ← π[j − 1] (desajuste con j > 0 o tras una ocurrencia completa), si no `null`; `occurrence` = s de la ocurrencia que completa esta comparación, si no `null`.
- `BoyerMoorePreprocessing`: `lastOccurrence` solo con los caracteres del patrón, en orden de primera aparición (ausente = −1); `borderPositions` = arreglo f de m + 1 posiciones del preprocesamiento de bordes; `goodSuffixShift[j]` = salto por desajuste en j (regla fuerte); `patternBorder` = borde(P); `matchShift` = m − borde(P).
- `Alignment`: `comparisons` de derecha a izquierda; con desajuste: `mismatchIndex` = j, `badCharacterShift` = j − last(T[s + j]), `goodSuffixShift` = goodSuffixShift[j], `appliedShift` = max(las dos, 1); con ocurrencia: `mismatchIndex` y `badCharacterShift` null, `goodSuffixShift` = `appliedShift` = `matchShift`.
- `Window`: `verified` = `null` si `hashMatches` es false; `collision` = `hashMatches && !verified`.

Casos a mano (tests, guía y fixtures del frontend):
- T = "abababca", P = "abab" → posiciones [0, 2].
  - KMP: π = [0, 0, 1, 2]; 9 comparaciones con (i, j, s): (0,0,0) (1,1,0) (2,2,0) (3,3,0) (4,2,2) (5,3,2) (6,2,4) (6,0,6) (7,0,7); `matched` = [T, T, T, T, T, T, F, F, T]; `fallbackTo` = 2 en la 4.ª y la 6.ª (ocurrencias en s = 0 y 2) y 0 en la 7.ª.
  - Boyer-Moore: last = {a: 2, b: 3}; goodSuffixShift = [2, 2, 4, 1]; borderPositions = [2, 3, 4, 4, 5]; borde = 2; matchShift = 2; alineaciones s = 0 (ocurrencia, salto 2), s = 2 (ocurrencia, salto 2), s = 4 (desajuste en j = 3 con 'a', carácter malo 3 − 2 = 1, sufijo bueno 1, salto 1).
  - Rabin-Karp con q = 101: d mod q = 88, h = 25, hash(P) = 48, ventanas s = 0 … 4 con hashes [48, 92, 48, 94, 21]; coincidencias de hash en s = 0 y 2, ambas verificadas; sin colisiones.
- Colisión: T = "abba", P = "ab", q = 3 → h = 1, hash(P) = 0, ventanas [0, 1, 0]; s = 2 es colisión; posiciones [0], colisiones [2].
- Bordes (`StringMatchingCases`): ("", "ab") → []; ("ab", "abc") → []; ("abc", "abc") → [0]; ("abc", "xy") → []; ("abcab", "ab") → [0, 3]; ("aaaa", "aa") → [0, 1, 2]; ("Algoritmo algoritmo", "algoritmo") → [10]; ("canción cancion", "cancion") → [8]; ("a b c", " ") → [1, 3]; patrón "" → `IllegalArgumentException`; dos abstracts idénticos con el mismo preprocesado → posiciones iguales.
- Ejemplo de la guía (`GuideFragment`): se extrae una vez con `PdfArticleExtractor.extract` el abstract de un PDF de `corpus/`; se copia literal un fragmento de hasta 300 `char` y un patrón en inglés de m ≤ 12 que aparece al menos 2 veces; se fijan el nombre del PDF, el fragmento, el patrón, las posiciones y, calculados a mano, π, last, goodSuffixShift, matchShift, h y hash(P) con q = 101.

API REST:

| Método y ruta | Parámetros | Respuesta |
|---|---|---|
| `GET /api/pattern-search` | `pattern` (texto), `q` (texto opcional) | 200 `PatternSearchResponse`, 400 o 500 |
| `GET /api/pattern-search/trace` | `algorithm` (slug), `articleId` (entero), `pattern`, `q` (opcional) | 200 `PatternTraceResponse`, 400 o 500 |

```java
record PatternSearchResponse(String pattern, long q, long d, int articleCount, Boolean allMatch,
                             List<AlgorithmOccurrences> algorithms, List<ArticleText> articles) {}
record ArticleText(long articleId, String title, String abstractText) {}
record AlgorithmOccurrences(String algorithm, String label, String status, String pendingLocation,
                            Double preprocessingMs, Double searchMs, Integer totalOccurrences,
                            List<ArticleOccurrences> articles) {}
record ArticleOccurrences(long articleId, String title, List<Integer> positions, int count, Integer collisions) {}
record PatternTraceResponse(String algorithm, String label, long articleId, String title, String abstractText,
                            String pattern, long q, String status, String pendingLocation, List<Integer> positions,
                            List<Integer> collisions, Object preprocessing, Object trace) {}
```
- `StringMatchingAlgorithm`, en este orden: `kmp` ("Knuth-Morris-Pratt (KMP)"), `boyer-moore` ("Boyer-Moore"), `rabin-karp` ("Rabin-Karp"). `pendingLocation(method)` = `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/<Clase>.java#<method>` con `method` ∈ {`preprocess`, `search`}. `fromSlug` lanza `IllegalArgumentException("Algoritmo de búsqueda de patrones desconocido: <x>. Valores válidos: kmp, boyer-moore, rabin-karp")`.
- `PatternSearchValidator`: `String pattern(String raw)` y `long q(String raw)`. Mensajes: nulo o vacío → "El patrón no puede estar vacío"; más de 200 → "El patrón tiene <n> caracteres; el máximo es 200"; q no decimal, fuera de rango o compuesto → "El parámetro q debe ser un número primo entre 2 y 1000000007; se recibió: <x>". `q` se convierte con `Long.parseLong` (atrapando `NumberFormatException`) y la primalidad se prueba con división por 2 y por los impares hasta √q.
- Artículo de la traza: `findAllByIdInAndAbstractDetectedTrue(List.of(id))` vacío → "El artículo <id> no existe o no tiene abstract". `articleId` no entero → mensaje existente "El parámetro articleId debe ser un entero mayor o igual a 1"; `algorithm` ausente → "Falta el parámetro algorithm".
- Algoritmo PENDIENTE en la búsqueda: `status` "PENDIENTE", `pendingLocation`, tiempos y `totalOccurrences` null, `articles` = []. En la traza: 200 con `status` "PENDIENTE", `pendingLocation`, y `positions`, `collisions`, `preprocessing`, `trace` null.
- CALCULADO: `collisions` por artículo solo en `rabin-karp` (null en los demás); `totalOccurrences` = suma de `count`; tiempos en ms con `Stopwatch`.
- Corpus sin abstracts: `articleCount` 0, `articles` [] y cada algoritmo con `articles` []; `allMatch` sigue la regla de §3.
- `PendingStubs` (package-private en `api/stringmatching`): `static final String PREFIX = "TODO estudiante:"`; `static boolean isPending(UnsupportedOperationException e)` compara el prefijo carácter a carácter (sin `.startsWith(`).
- `preprocessing` de la traza: el record del algoritmo, salvo Boyer-Moore, que se expone como `BoyerMoorePreprocessingView(pattern, List<LastOccurrenceEntry(char character, int index)> lastOccurrence, borderPositions, goodSuffixShift, patternBorder, matchShift)`: la tabla del carácter malo viaja como lista en orden de primera aparición, porque un objeto JSON reordena las claves numéricas en JavaScript. El record de `trace/stringmatching` conserva el `Map` que usa el estudiante.
- `ApiExceptionHandler`: `InvalidMatchPositionsException` → 500 con `title` "Error interno" y `detail` = mensaje.

JMH (comando único, documentado en la guía):
`./gradlew jmhJar && mkdir -p build/jmh-results && java -jar build/libs/benchmarks.jar '(StringMatchingBenchmark|RabinKarpBenchmark)' -rf json -rff build/jmh-results/string-matching.json`
- Ambas clases: `@Fork(1)`, `@Warmup(iterations = 5, time = 1 s)`, `@Measurement(iterations = 10, time = 1 s)`, `Mode.AverageTime`, `TimeUnit.MICROSECONDS`, `MatchOptions` con `recordTrace = false`.
- Entradas (`@State` de entrada con `@Param n` {1000, 10000, 100000}, `@Param m` {5, 50}, `@Param textType` {RANDOM, REPETITIVE}): RANDOM = n caracteres de "abcdefghijklmnopqrstuvwxyz " con `Random(42)` y patrón = texto[n/2, n/2 + m); REPETITIVE = "a"·n y patrón = "a"·(m − 1) + "b".
- `StringMatchingBenchmark`: un `@State` por algoritmo (`KmpState`, `BoyerMooreState`) que preprocesa en su `@Setup`; métodos `kmpPreprocess`, `kmpSearch`, `boyerMoorePreprocess`, `boyerMooreSearch`.
- `RabinKarpBenchmark`: `@Param q` {101, 1000000007}; métodos `rabinKarpPreprocess`, `rabinKarpSearch`.
- El JSON trae por método (algoritmo y fase) los parámetros n, m, textType y q, `score` y `scoreError` en µs.

Frontend:
- `types/patternSearch.ts`: `StringMatchingSlug`, `STRING_MATCHING_ALGORITHMS` (slug y etiqueta en el orden de la API), `PatternSearchResponse`, `AlgorithmOccurrences`, `ArticleOccurrences`, `ArticleText`, `PatternTraceResponse`, `KmpPreprocessing`, `KmpTrace`, `BoyerMoorePreprocessing`, `BoyerMooreTrace`, `RabinKarpPreprocessing`, `RabinKarpTrace`.
- `usePatternSearch(params: { pattern: string; q?: string } | null)`: `useQuery` con `apiGet('/api/pattern-search?' + new URLSearchParams(...))`, clave `['pattern-search', pattern, q]`, habilitada con `params` no nulo. `usePatternTrace(algorithm, articleId, pattern, q)`: clave `['pattern-trace', …]`, habilitada con algoritmo y artículo elegidos.
- `highlightSegments(textLength, positions, patternLength): Segment[]` con `Segment = { start, end, highlighted, starts: number[] }`; recorre `positions` (ya ascendentes) una vez: une en un tramo las ocurrencias que se solapan o se tocan (p ≤ fin del tramo), recorta el fin a `textLength` y rellena los huecos con tramos no resaltados. "aaaa", [0, 1, 2], m = 2 → [{0, 4, true, [0, 1, 2]}]. O(k) con k = número de posiciones; sin `.sort(`.
- `HighlightedAbstract`: dibuja cada tramo con `text.slice(start, end)`; los resaltados en `<mark>` y una marca (`aria-label` "Inicio de ocurrencia en <p>") en cada inicio. Índices UTF-16, iguales a los `char` de Java.
- `defaultAlgorithm(response)` en `AlgorithmSelector.tsx`: primer CALCULADO en el orden KMP, Boyer-Moore, Rabin-Karp; el selector solo ofrece los CALCULADO.
- `PatternSearchResults`: "Sin abstracts en el corpus" si `articleCount` es 0; por algoritmo, `AlgorithmOccurrencesTable` (título, número de ocurrencias, posiciones, colisiones en Rabin-Karp; fila seleccionable), "Preprocesamiento: <ms> ms", "Búsqueda: <ms> ms", "Total: <n>" o "Pendiente de implementar: <pendingLocation>"; "Coinciden todas las posiciones: Sí/No" solo si `allMatch` no es null; errores de la API en `Alert` con el `detail`.
- `traceSteps.ts`: `toFrames(response): TraceFrame[]` con `TraceFrame = { shift, compared: { textIndex, patternIndex, matched }[], mismatchIndex?, occurrence, nextShift?, label }`. KMP: un frame por comparación (`shift` = s de la traza, `nextShift` = s de la siguiente). Boyer-Moore: uno por alineación (`nextShift` = s + `appliedShift`). Rabin-Karp: uno por ventana (`compared` vacío, ventana [s, s + m) con hash, verificación y colisión; `nextShift` = s + 1).
- `StepNavigator`: botones "Primero", "Anterior", "Siguiente", "Último" y "Paso k de N".
- `AlignedWindow`: texto de [max(0, s − 10), min(n, s + m + 10)) con el patrón alineado debajo en s; resalta los caracteres comparados (coincidencia o desajuste) y muestra el salto al paso siguiente. Lee caracteres por índice (`text[i]`).
- `KmpTraceView` (tabla π y pasos de construcción), `BoyerMooreTraceView` (tablas del carácter malo, del sufijo bueno y salto tras ocurrencia), `RabinKarpTraceView` (d, q, h, hash del patrón, hash inicial y lista de colisiones), cada una con `StepNavigator`, `AlignedWindow`, fórmulas de `patternFormulas.ts` en `TraceStep`/`BlockMath` y `TraceChart`.
- `TraceChart` (Recharts): KMP x = número de comparación, y = s; Boyer-Moore x = alineación, y = salto aplicado; Rabin-Karp x = s, y = hash de la ventana, `ReferenceLine` en hash(P) y puntos de colisión marcados.
- `PatternTraceView`: con PENDIENTE muestra "Pendiente de implementar: <pendingLocation>".

## 7. Test-first
- No aplica: `AGENTS.md` no define test-first. Los tests por AC están en la tabla de § 8.

## 8. Traceability
| Requirement | Component / file | Verified by (AC) |
|---|---|---|
| REQ-1 | `StringMatcher`, 3 stubs, `PatternSearchService`; `StringMatchingStubScaffoldTest`, `PatternSearchApiIT` | AC-1, AC-27 |
| REQ-2 | `MatchResult`, contrato de los stubs; `StringMatchingCases`, tests `student` | AC-5, AC-6, AC-7 |
| REQ-3 | Javadoc de los stubs; `StringMatchingCases` | AC-8 |
| REQ-4 | Javadoc de `KnuthMorrisPratt`; `StringMatchingStubDocumentationTest`, `KnuthMorrisPrattTest` | AC-5, AC-23 |
| REQ-5 | Javadoc de `BoyerMoore`, `BoyerMoorePreprocessing`; `StringMatchingStubDocumentationTest`, `BoyerMooreTest` | AC-23, AC-24 |
| REQ-6 | Javadoc de `RabinKarp`, `MatchOptions`; `StringMatchingStubDocumentationTest`, `RabinKarpTest` | AC-9, AC-23 |
| REQ-7 | stubs sin lógica; `StringMatchingStubScaffoldTest`, `ForbiddenStringMatchingUsageTest` | AC-1, AC-2 |
| REQ-8 | `MatchPositionsValidator`, `InvalidMatchPositionsException`, `ApiExceptionHandler`; `MatchPositionsValidatorTest`, `PatternSearchApiIT` | AC-3 |
| REQ-9 | `StringMatchingCases`, `KnuthMorrisPrattTest`, `BoyerMooreTest`, `RabinKarpTest` | AC-5, AC-6, AC-7, AC-8, AC-10, AC-24 |
| REQ-10 | `RabinKarpTest` | AC-9 |
| REQ-11 | `KnuthMorrisPrattTest` | AC-5, AC-16 |
| REQ-12 | `oracle/StringMatchingOracleTest`, `oracle/GuideFragmentCorpusTest` | AC-11 |
| REQ-13 | `@Tag("student")`/`@Tag("scaffold")`, `build.gradle.kts` | AC-4 |
| REQ-14 | `ForbiddenStringMatchingUsageTest` | AC-2 |
| REQ-15 | `PatternSearchController`, `PatternSearchService`, `PatternSearchDtos`, `PatternSearchValidator`; `PatternSearchApiIT` | AC-12, AC-25 |
| REQ-16 | `PatternSearchService` (`allMatch`, `totalOccurrences`); `PatternSearchApiIT` | AC-12, AC-20 |
| REQ-17 | `PatternSearchValidator`; `PatternSearchApiIT`, `PatternSearchView.test.tsx` | AC-13 |
| REQ-18 | `PatternSearchService`, `PatternSearchResults`; `PatternSearchApiIT`, `PatternSearchView.test.tsx` | AC-14 |
| REQ-19 | `App.tsx`, `PatternSearchPage`, `PatternSearchForm`; `App.test.tsx`, `PatternSearchView.test.tsx` | AC-14, AC-15 |
| REQ-20 | `AlgorithmOccurrencesTable`, `AlgorithmSelector`, `HighlightedAbstract`, `highlightSegments.ts`; `highlightSegments.test.ts`, `PatternSearchView.test.tsx` | AC-15 |
| REQ-21 | `highlightSegments.ts`, `traceSteps.ts`; `ForbiddenStringMatchingUsageTest`, `PatternSearchView.test.tsx` | AC-2, AC-15 |
| REQ-22 | `PatternSearchService.trace`, `PatternSearchController`; `PatternSearchApiIT` | AC-19, AC-26 |
| REQ-23 | `KmpPreprocessing`, `KmpTrace`; `KnuthMorrisPrattTest`, `PatternTraceView.test.tsx` | AC-5, AC-16 |
| REQ-24 | `BoyerMoorePreprocessing`, `BoyerMooreTrace`, `PatternSearchDtos.BoyerMoorePreprocessingView`; `BoyerMooreTest`, `PatternSearchDtosTest`, `PatternTraceView.test.tsx` | AC-17, AC-24 |
| REQ-25 | `RabinKarpPreprocessing`, `RabinKarpTrace`; `RabinKarpTest`, `PatternTraceView.test.tsx` | AC-9, AC-18 |
| REQ-26 | `PatternTraceView`, `StepNavigator`, `AlignedWindow`, vistas por algoritmo, `patternFormulas.ts`, `traceSteps.ts`; `traceSteps.test.ts`, `PatternTraceView.test.tsx` | AC-16, AC-17, AC-18 |
| REQ-27 | `TraceChart`; `PatternTraceView.test.tsx` | AC-16, AC-17, AC-18 |
| REQ-28 | `PendingStubs`, `StringMatchingAlgorithm.pendingLocation`, `PatternSearchResults`, `PatternTraceView`; `PendingStubsTest`, `PatternSearchApiIT`, `PatternSearchView.test.tsx`, `PatternTraceView.test.tsx` | AC-20, AC-26 |
| REQ-29 | `PatternSearchService` con `Stopwatch` y `MatchOptions(q, false)`; `PatternSearchApiIT`, `PatternSearchView.test.tsx` | AC-21, AC-27 |
| REQ-30 | `StringMatchingBenchmark`, `RabinKarpBenchmark` | AC-22 |
| REQ-31 | `docs/guia-implementacion.md` § 11; `PatternSearchGuideTest` | AC-22, AC-23 |
| REQ-32 | `GuideFragment`, guía § 11; tests `student`, `GuideFragmentCorpusTest`, `PatternSearchGuideTest` | AC-11, AC-23 |
| REQ-33 | `docs/traceability.md`; `PatternSearchGuideTest` | AC-23 |

Tests por AC (given - when - then de la spec):

| AC | Test (archivo) | Comprobación |
|---|---|---|
| AC-1 | `StringMatchingStubScaffoldTest` (`scaffold`) | `preprocess("abab", defaults)` y `search("abababca", null, defaults)` de cada stub lanzan `UnsupportedOperationException` con "TODO estudiante:"; el cuerpo de cada método es un solo `throw`; el Javadoc trae las dos "Complejidad … TODO estudiante" |
| AC-2 | `ForbiddenStringMatchingUsageTest`; `StringMatchingStubScaffoldTest` | Java (sin comentarios ni cadenas): ningún `.indexOf(` … `.regionMatches(`; `java.util.regex` en el archivo crudo; `\bPattern\b`/`\bMatcher\b`. TypeScript (producción de `web/src/features/pattern-search` y los `web/src/components` que importa, sin comentarios ni cadenas): ninguno de los 13 nombres como identificador completo. Resto de `web/src` y `web/package.json`: sin `fuse.js`, `flexsearch`, `lunr`, `elasticlunr`, `minisearch`, `fuzzysort`, `match-sorter`. Autoprueba del detector: `list.contains(x)`, `s.matches("a")`, `import java.util.regex.Pattern;`, `Pattern.compile("ab")`, `Matcher m = null;`, `xs.includes(1)`, `new RegExp('a')` fallan; `map.containsKey(c)`, `map.getOrDefault(c, -1)`, `StringMatcher`, `searchMs`, `"a.split(b)"` y `// t.indexOf(x)` pasan |
| AC-3 | `MatchPositionsValidatorTest`, `PatternSearchApiIT` | [1, 0], [0, 0], [−1], [n − m + 1], null y [null] → excepción con su razón; [0, 2] con n = 8, m = 4 → sin excepción; fake inválido → HTTP real 500 con `detail` "El algoritmo kmp devolvió posiciones inválidas en el artículo …" |
| AC-4 | `./gradlew test`; `./gradlew studentTest --tests KnuthMorrisPrattTest --tests BoyerMooreTest --tests RabinKarpTest --tests StringMatchingOracleTest` | verde con los stubs; todos los tests de estudiante fallan |
| AC-5 | `KnuthMorrisPrattTest`, `BoyerMooreTest`, `RabinKarpTest` | "abababca"/"abab" → [0, 2]; KMP: π y las 9 comparaciones (i, j, s, `matched`, `fallbackTo`) de §6, `i` nunca disminuye aquí ni en 50 textos `Random(42)` |
| AC-6 | los 3 tests `student` | "aaaa"/"aa" → [0, 1, 2] |
| AC-7 | los 3 tests `student` | texto vacío, patrón más largo, patrón ausente → []; patrón "" → `IllegalArgumentException` |
| AC-8 | los 3 tests `student` | "Algoritmo algoritmo"/"algoritmo" → [10]; "canción cancion"/"cancion" → [8] |
| AC-9 | `RabinKarpTest` | "abba"/"ab", q = 3 → ventana s = 2 con `collision` true, `collisions` = [2], posiciones [0]; "abababca"/"abab", q = 101 → h = 25, hash(P) = 48, hashes [48, 92, 48, 94, 21] |
| AC-10 | los 3 tests `student` | dos abstracts idénticos con el mismo preprocesado → posiciones iguales |
| AC-11 | `oracle/StringMatchingOracleTest` (`student`), `oracle/GuideFragmentCorpusTest` | 200 textos `Random(42)` (alfabetos "ab" y a–z con espacio, n de 0 a 200, patrones de 1 a 6 tomados del texto o al azar; Rabin-Karp con q ∈ {3, 101, 1000000007}) y el fragmento → posiciones = las de `String.indexOf`; el fragmento es parte del abstract extraído y sus posiciones por `indexOf` son las de `GuideFragment` (se omite con `Assumptions` sin el PDF) |
| AC-12 | `PatternSearchApiIT` | abstracts con `GuideFragment.FRAGMENT` y fakes implementados → posiciones de `GuideFragment` en los 3, `collisions` en Rabin-Karp, `totalOccurrences` y `allMatch` true |
| AC-13 | `PatternSearchApiIT`, `PatternSearchView.test.tsx` | `pattern=`, 201 `char`, `q=1`, `q=100`, `q=abc` → 400 con mensaje de §6; la vista muestra el `detail` |
| AC-14 | `PatternSearchApiIT`, `PatternSearchView.test.tsx` | sin artículos seleccionables → 200, `articleCount` 0, listas vacías; la vista muestra "Sin abstracts en el corpus" |
| AC-15 | `highlightSegments.test.ts`, `PatternSearchView.test.tsx` | "aaaa", [0, 1, 2], m = 2 → un tramo [0, 4) con inicios [0, 1, 2]; [0, 2] con m = 4 en 8 → [0, 6) y [6, 8); la vista resalta las posiciones de KMP por defecto, al elegir Rabin-Karp resalta las suyas, y con posiciones del fixture que no son ocurrencias reales resalta esas mismas |
| AC-16 | `traceSteps.test.ts`, `PatternTraceView.test.tsx` | KMP: frames con s = [0, 0, 0, 0, 2, 2, 4, 6, 7]; la vista muestra π, al pulsar "Siguiente" y "Último" el patrón se alinea en el s del paso y el gráfico KMP existe |
| AC-17 | `traceSteps.test.ts`, `PatternTraceView.test.tsx` | Boyer-Moore: frames con s = [0, 2, 4] y `nextShift` [2, 4, 5]; la vista muestra {a: 2, b: 3}, [2, 2, 4, 1], los saltos por regla y el gráfico |
| AC-18 | `traceSteps.test.ts`, `PatternTraceView.test.tsx` | Rabin-Karp: 5 frames; con la traza de colisión la vista muestra d, q, h, hash(P), el gráfico y la colisión en s = 2 listada |
| AC-19 | `PatternSearchApiIT` | traza con artículo inexistente o sin abstract, `algorithm=x`, patrón vacío, 201 `char`, `q` 1, 100, abc → 400 con mensaje en español |
| AC-20 | `PatternSearchApiIT`, `PatternSearchView.test.tsx` | solo KMP implementado → KMP CALCULADO, Boyer-Moore y Rabin-Karp PENDIENTE con `…/BoyerMoore.java#preprocess` y `…/RabinKarp.java#preprocess`, `allMatch` null; fake pendiente en `search` → `#search`; la vista muestra "Pendiente de implementar: …" y no muestra el indicador |
| AC-21 | `PatternSearchApiIT`, `PatternSearchView.test.tsx` | `preprocessingMs` ≥ 0 y `searchMs` ≥ 0 en los CALCULADO, null en los PENDIENTE; la vista muestra ambos en ms |
| AC-22 | ejecución manual del comando de §6; `PatternSearchGuideTest` | el JSON trae los 6 métodos con n, m, textType (y q en Rabin-Karp), `score` y `scoreError` en µs; la guía documenta el comando y `build/jmh-results/string-matching.json`; `./gradlew jmhJar` compila |
| AC-23 | `PatternSearchGuideTest` | § 11 existe; por cada stub: ruta, `preprocess(`, `search(`, "Definición formal", "Traza", "Caso a mano", `./gradlew studentTest --tests <Clase>Test` y los valores de §6; el ejemplo trae el PDF, el fragmento, el patrón, las posiciones y las tablas de `GuideFragment`; declaración de IA generativa; filas del Requerimiento 4 con "(pendiente)" para `algorithms.stringmatching`, `trace.stringmatching`, `api.stringmatching`, `evaluation` y `pattern-search` |
| AC-24 | `BoyerMooreTest` | `preprocess("abab")` → last {a: 2, b: 3} (un carácter ausente no está en el mapa), goodSuffixShift [2, 2, 4, 1], `patternBorder` 2, `matchShift` 2 |
| AC-25 | `PatternSearchApiIT`, los 3 tests `student`, `PatternSearchView.test.tsx` | `pattern=%20` → posiciones de los espacios de cada abstract; ("a b c", " ") → [1, 3]; la vista envía " " sin recortar |
| AC-26 | `PatternSearchApiIT`, `PatternTraceView.test.tsx` | traza de un algoritmo pendiente → 200, `status` PENDIENTE, `trace` null y `pendingLocation`; la vista muestra "Pendiente de implementar: …" |
| AC-27 | `PatternSearchApiIT` | el espía registra 1 `preprocess` y un `search` por abstract, todos con la misma instancia preprocesada, y `recordTrace` false |

Comandos: `./gradlew test`, `./gradlew studentTest`, `./gradlew scaffoldTest`, `./gradlew jmhJar` y `npm test`, `npm run lint`, `npm run build` en `web/`.

## 9. Decisions and risks
- Dos fases `preprocess`/`search` con el preprocesado como parámetro: permite un solo preprocesamiento por búsqueda (REQ-1) y medir cada fase aparte (REQ-29, REQ-30). Se rechazó un `search(text, pattern)` único porque repetiría el preprocesamiento por abstract.
- Los records de preprocesamiento viven en `trace/stringmatching`: son a la vez la entrada de `search` y la traza del preprocesamiento (P7), sin duplicar tipos.
- `GET` con parámetros y `q` como texto validado a mano (decisión del usuario): `q=abc` da el mensaje propio de §6 y no el genérico de enteros.
- El escaneo por nombre en TypeScript usa identificador completo y excluye comentarios y cadenas (decisión del usuario): `searchMs`, `allMatch` o `mismatchIndex` no fallan; los archivos `*.test.*` no son producción y no se escanean.
- `PendingStubs` en `api/stringmatching` con comparación carácter a carácter: así todo el código de la API del requerimiento queda bajo el escaneo de REQ-14. Se rechazó dejarlo en `api/` con `.startsWith(` porque quedaba fuera del escaneo. Las specs 001 y 003 no cambian.
- `lastOccurrence` como lista en la API y como `Map` en el record del estudiante: el estudiante consulta last(c) en O(1) y el JSON conserva el orden. Se rechazó anotar el record de `trace` con Jackson para no acoplar ese paquete a la serialización.
- JMH en dos clases (decisión del usuario): `q` solo es parámetro de Rabin-Karp, así KMP y Boyer-Moore no repiten mediciones con un q que no usan.
- `allMatch` con 2 o más CALCULADO y corpus vacío es `true` (no hay artículo en desacuerdo).
- La respuesta de búsqueda incluye los abstracts (20 × ~2 000 `char`): la interfaz resalta sin otra petición.
- Risk: las posiciones son índices UTF-16 - el frontend usa `text.length`, `text[i]` y `slice`; nunca `Array.from` ni el operador de propagación sobre cadenas, que separan por puntos de código.
- Risk: Spring podría recortar el patrón - no hay `StringTrimmerEditor` en el proyecto y `PatternSearchApiIT` lo verifica con `pattern=%20` (AC-25).
- Risk: el PDF del fragmento no está en CI - `GuideFragmentCorpusTest` se omite con `Assumptions`; el fragmento literal sigue en `GuideFragment` y lo usan los tests `student`.
- Risk: trazas largas (KMP hasta 2n comparaciones con n ≈ 2 000) - el visor muestra un paso a la vez y el gráfico recibe un punto por paso.
- Risk: Recharts en jsdom no mide tamaños - los tests verifican el contenedor por `aria-label` y los datos pasados, no píxeles.
- Risk: `java.util.regex` se busca en el archivo crudo - los Javadoc de los módulos escaneados no la nombran (dicen "expresiones regulares").
- Risk: un `@State` JMH con un stub sin implementar falla en `@Setup` - la guía indica ejecutar JMH solo con los 3 algoritmos implementados; `build` no ejecuta JMH.
