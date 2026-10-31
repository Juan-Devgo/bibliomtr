# Plan: Requerimiento 2: Ranking de términos mediante algoritmos de ordenamiento

## 1. Metadata
- Spec: `docs/specs/002-ranking-terminos/spec.md`
- Updated: 2026-10-23
- Constitution version: 1.1.0

## 2. Constitution check
- P1: `CountingSort`, `RadixSort` y `BucketSort` (con su insertion sort) se escriben a mano con `int[]`, arreglos de `TermFrequency` y `ArrayList`. `TreeMap<String,Integer>` solo aparece en `TermFrequencyCounter` (REQ-2). `ForbiddenSortUsageTest` lo verifica.
- P2: JMH solo mide (`SortingBenchmark`, proceso hijo); Recharts solo dibuja las barras; KaTeX solo muestra fórmulas; Jackson solo lee el JSON de JMH.
- P4: las implementaciones adaptan, sin cambiar la lógica, el código de referencia `docs/referencias/ordenamiento/{CountingSort,RadixSort,BucketSort}.java` (textual del usuario, fuera de `src/`, no se compila). El único cambio de lógica es `Collections.sort` → insertion sort (conflicto P1 de la spec). El uso de IA lo declaran los estudiantes en `docs/documento-tecnico.md`.
- P5: las frecuencias salen solo de los abstracts seleccionables, a través del puerto `CorpusTokenSource` sobre la ingesta de la spec 001.
- P6: el Javadoc de cada algoritmo y `docs/documento-tecnico.md` documentan la definición formal y la notación: conteo `C[v]`, acumulado `C[v] += C[v−1]`, dígito `⌊v/exp⌋ mod 10`, número de cubetas `⌊(max − min)/bucketSize⌋ + 1`, índice `⌊(v − min)/bucketSize⌋`. Las vistas de traza muestran las mismas fórmulas en KaTeX.
- P7: cada `TermSorter` devuelve `SortResult` con su traza (`trace/ranking`), expuesta en `GET /api/ranking/trace/{algorithm}` y mostrada paso a paso en `web/src/features/sort-trace`.
- P8: todo tipo y método público lleva Javadoc en español con qué, por qué y complejidad: Counting O(n+k) en tiempo y espacio, k = frecuencia máxima; Radix O(d·(n+b)) en tiempo y O(n+b) en espacio, d = dígitos del máximo, b = 10; Bucket O(n+m) promedio y O(n²) peor caso en tiempo, O(n+m) en espacio, m = número de cubetas.
- P9: `docs/traceability.md` recibe una fila por módulo, con la sección "Requerimiento 2: Ranking de términos" de `docs/documento-tecnico.md` marcada `(pendiente)`; los estudiantes redactan esa sección.
- P10: tests a mano (AC-9, AC-10, AC-11) y casos de borde aplicables (vacía, un término, frecuencias iguales, ya ordenada, inversa, k ≫ n), según la resolución de conflicto de la spec.
- P11: `RankingResponse` reporta `preprocessingMs` y `elapsedMs` por algoritmo, medidos sin traza; JMH guarda ms ± scoreError junto a la complejidad teórica.
- P12: reutiliza el preprocesador de la 001; los parámetros JMH quedan fijos en las anotaciones de `SortingBenchmark`, en los argumentos de `JmhProcessLauncher` y en `build.gradle.kts`; las dependencias de frontend quedan fijadas en `package-lock.json`.
- P13: `algorithms/ranking` y `trace/ranking` (algoritmos), `evaluation` (tiempos y JMH), `persistence/benchmark`, `api/ranking` y `api/benchmark`, y `web/src/features/*` (interfaz) son módulos separados; `algorithms`, `trace` y `evaluation` no importan `api` ni `persistence`.

## 3. Architecture
Flujo del ranking (`GET /api/ranking`):
1. `RankingService` llama a `CorpusTokenSource.selectableAbstractTokens()`. El adaptador `SelectableAbstractTokenSource` (bean `@Component`) recorre `ArticleRepository.findAllByAbstractDetectedTrueOrderByFileNameAsc()` (`persistence/article`) y aplica `new TextPreprocessor().tokenize(String)` (`preprocessing`) al abstract de cada artículo.
2. `TermFrequencyCounter.count(tokens)` suma en un `TreeMap<String,Integer>` y arma la entrada recorriendo `descendingMap()`, es decir, en orden alfabético descendente. Los pasos 1 y 2 se cronometran juntos como `preprocessingMs`.
3. Por cada `TermSorter` (Counting, Radix, Bucket) se ejecuta `sort(input, SortOptions(bucketSize, false))` sobre la misma lista y se cronometra cada uno como `elapsedMs`.
4. `RankingReader.read(result.output())` lee la salida de derecha a izquierda (frecuencia descendente, empate alfabético ascendente). `RankingReader.sameRanking` compara las tres listas completas para dar `allMatch`, y `RankingReader.top(ranking, n)` corta los N primeros.

Flujo de la traza (`GET /api/ranking/trace/{algorithm}`): pasos 1 y 2, y luego un solo `TermSorter` con `recordTrace = true`. Devuelve entrada, salida, ranking y traza completos.

Flujo de la medición (`POST /api/benchmarks`):
1. `BenchmarkService.start()` (synchronized) rechaza con 409 si existe una medición EN_CURSO.
2. Arma la lista con los pasos 1 y 2 y calcula n, k, d, b y m.
3. Guarda un `BenchmarkRun` EN_CURSO y responde 202.
4. En un executor de un hilo, `FrequencyListFile` escribe el TSV y `JmhProcessLauncher` ejecuta `java -jar benchmarks.jar` con límite de 10 min. `JmhResultParser` lee score y scoreError, y la medición queda COMPLETADA o FALLIDA.

Al arrancar, `BenchmarkStartupRecovery` pasa toda medición EN_CURSO a FALLIDA.

Interfaz: `TermRankingPage` agrupa tres vistas: `RankingView` (tabla y barras del top N de cada algoritmo, con tiempos), `SortTraceView` (traza paso a paso con `PagedArray`) y `BenchmarkPanel` (acción, estado, última COMPLETADA e historial). Los datos se cargan con hooks de TanStack Query.

## 4. Design patterns
- Strategy en `TermSorter` (Counting, Radix, Bucket): `RankingService`, `BenchmarkService` y `SortingBenchmark` recorren los tres algoritmos con la misma firma.
- Port/Adapter en `CorpusTokenSource` / `SelectableAbstractTokenSource`: aísla la 002 de la ingesta y el preprocesamiento de la 001, que ya existen (`CorpusIngestionService`, `ArticleRepository` y `TextPreprocessor`); el adaptador `SelectableAbstractTokenSource` se registra como bean en la fase 7 y los tests usan un fake.
- Interfaz `BenchmarkLauncher` con implementación `JmhProcessLauncher`: permite probar 409, FALLIDA y timeout con un launcher falso, sin correr JMH real (unos 60 s por corrida).

## 5. Code structure
Backend, producción (`src/main/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/ranking/package-info.java` - Javadoc del módulo del Requerimiento 2 y referencia a `docs/referencias/ordenamiento/`.
- create: `algorithms/ranking/TermFrequency.java` - par (término, frecuencia).
- create: `algorithms/ranking/TermFrequencyCounter.java` - conteo con `TreeMap` y entrada en orden alfabético descendente (`compareTo` natural de `String`).
- create: `algorithms/ranking/SortAlgorithm.java` - enum COUNTING, RADIX, BUCKET con slug, complejidad teórica y cálculo de sus parámetros (k; d y b; m).
- create: `algorithms/ranking/SortOptions.java` - `bucketSize` y `recordTrace`.
- create: `algorithms/ranking/SortResult.java` - salida ordenada más traza (null si `recordTrace = false`).
- create: `algorithms/ranking/TermSorter.java` - contrato Strategy.
- create: `algorithms/ranking/CountingSort.java` - adapta `docs/referencias/ordenamiento/CountingSort.java`.
- create: `algorithms/ranking/RadixSort.java` - adapta `docs/referencias/ordenamiento/RadixSort.java`, LSD base 10.
- create: `algorithms/ranking/BucketSort.java` - adapta `docs/referencias/ordenamiento/BucketSort.java`, con insertion sort estable a mano por cubeta.
- create: `algorithms/ranking/RankingReader.java` - lectura de derecha a izquierda, top N y comparación de rankings.
- create: `trace/ranking/CountingSortTrace.java`, `trace/ranking/RadixSortTrace.java`, `trace/ranking/BucketSortTrace.java` - registros inmutables de la traza.
- create: `evaluation/Stopwatch.java` - cronometraje con `System.nanoTime`, resultado en ms (`double`).
- create: `evaluation/FrequencyListFile.java` - escribe y lee la lista en TSV `term\tfrequency`, en orden de entrada.
- create: `evaluation/BenchmarkLauncher.java`, `evaluation/JmhProcessLauncher.java`, `evaluation/JmhResultParser.java`, `evaluation/JmhScore.java`, `evaluation/BenchmarkTimeoutException.java`, `evaluation/JmhParameters.java` - ejecución y lectura de JMH.
- create: `persistence/benchmark/BenchmarkStatus.java`, `persistence/benchmark/BenchmarkRun.java`, `persistence/benchmark/BenchmarkAlgorithmResult.java`, `persistence/benchmark/BenchmarkRunRepository.java` - historial de mediciones.
- create: `api/ranking/CorpusTokenSource.java` - puerto.
- create: `api/ranking/SelectableAbstractTokenSource.java` - adaptador `@Component`, único bean `CorpusTokenSource` de `src/main`, sobre `ArticleRepository.findAllByAbstractDetectedTrueOrderByFileNameAsc()` (`persistence/article`) y `TextPreprocessor.tokenize(String)` (`preprocessing`) de la spec 001.
- create: `api/ranking/RankingService.java`, `api/ranking/RankingController.java`, `api/ranking/RankingDtos.java` - ranking y traza.
- create: `api/benchmark/BenchmarkService.java`, `api/benchmark/BenchmarkStartupRecovery.java`, `api/benchmark/BenchmarkController.java`, `api/benchmark/BenchmarkDtos.java`, `api/benchmark/BenchmarkInProgressException.java` - mediciones.
- create: `api/ApiExceptionHandler.java` - errores 400 y 409 en español (si la 001 ya lo creó: modify, agregando estos casos).
- create: `config/BenchmarkConfig.java` - bean `ExecutorService` de un hilo, `benchmarkExecutor`, con `shutdownNow` al cerrar.
- modify: `config/BibliomtrProperties.java` - agrega `Benchmark(@NotNull Path jar, @NotNull Duration timeout)`.
- create: `src/main/resources/db/migration/V<siguiente libre>__create_benchmark_tables.sql` - tablas `benchmark_run` y `benchmark_algorithm_result`.
- modify: `src/main/resources/application.properties` - `bibliomtr.benchmark.jar=${BIBLIOMTR_BENCHMARK_JAR:build/libs/benchmarks.jar}` y `bibliomtr.benchmark.timeout=10m`.

Benchmark, build y despliegue:
- create: `src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/SortingBenchmark.java` - tres `@Benchmark` sobre la lista leída de `inputFile`.
- modify: `build.gradle.kts` - bloque `jmh` con `fork = 1`, `warmupIterations = 5`, `warmup = "1s"`, `iterations = 10`, `timeOnIteration = "1s"`, `benchmarkMode = listOf("avgt")`, `timeUnit = "ms"`; `bootJar` como `app.jar` y `jmhJar` como `benchmarks.jar`.
- modify: `Dockerfile` - `./gradlew bootJar jmhJar -x test`; copia `app.jar` y `benchmarks.jar` a `/app/`; `ENV BIBLIOMTR_BENCHMARK_JAR=/app/benchmarks.jar`.

Backend, tests (`src/test/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/ranking/TermFrequencyCounterTest.java`, `algorithms/ranking/CountingSortTest.java`, `algorithms/ranking/RadixSortTest.java`, `algorithms/ranking/BucketSortTest.java`, `algorithms/ranking/RankingReaderTest.java`, `algorithms/ranking/SortAlgorithmTest.java`, `algorithms/ranking/SortEdgeCases.java` (fuente compartida de casos de borde para `@MethodSource`).
- create: `algorithms/ranking/ForbiddenSortUsageTest.java` - análisis estático del código de producción.
- create: `oracle/SortingOracleTest.java` - comparación con un ordenamiento de la librería estándar.
- create: `evaluation/FrequencyListFileTest.java`, `evaluation/JmhResultParserTest.java` y `src/test/resources/jmh/sample-result.json`.
- create: `api/ranking/FakeCorpusTokenSource.java` - fake configurable con un `@TestConfiguration` anidado (`Config`) que lo registra como bean `@Primary`.
- create: `api/ranking/RankingControllerTest.java` (`@WebMvcTest` con el fake).
- modify: `BibliomtrApplicationTests.java` - importa `FakeCorpusTokenSource.Config` mientras no exista `SelectableAbstractTokenSource`; el import se quita cuando se crea el adaptador. Desde entonces arranca el contexto completo con `TestcontainersConfiguration`, sin `FakeCorpusTokenSource` y con la ingesta apagada, y verifica que el bean `CorpusTokenSource` es un `SelectableAbstractTokenSource`.
- create: `api/ranking/CorpusRankingConsistencyTest.java` - test sobre el corpus real con Assumptions: `@SpringBootTest(properties = {"bibliomtr.ingestion.enabled=true", "bibliomtr.corpus.dir=corpus"})` con `TestcontainersConfiguration` y `FakeEmbedders.Config` (`src/test/java/co/edu/uniquindio/bibliomtr/api/corpus/FakeEmbedders.java`), sin `FakeCorpusTokenSource`.
- create: `api/benchmark/FakeBenchmarkLauncher.java` - modos: completa, falla, timeout y bloquea hasta un `CountDownLatch`.
- create: `api/benchmark/BenchmarkServiceIT.java` - `@SpringBootTest` + `TestcontainersConfiguration` + fakes.
- create: `api/benchmark/BenchmarkRestartIT.java` - `PostgreSQLContainer` estático y dos arranques sucesivos con `SpringApplicationBuilder` sobre la misma base.

Frontend (`web/`):
- modify: `package.json` - devDependencies `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`; script `"test": "vitest run"`.
- modify: `vite.config.ts` - bloque `test` (`environment: 'jsdom'`, `setupFiles: './src/test/setup.ts'`).
- create: `src/test/setup.ts` - registra `@testing-library/jest-dom` y un helper `renderWithQuery` con `QueryClient` sin reintentos.
- modify: `src/api/client.ts` - agrega `apiPost<T>`; ante un error, lanza `ApiError(status, message)` con el `detail` en español del ProblemDetail.
- create: `src/types/ranking.ts`, `src/types/benchmark.ts` - tipos de respuesta y traza.
- create: `src/api/useTermRanking.ts`, `src/api/useSortTrace.ts`, `src/api/useBenchmarks.ts`, `src/api/useStartBenchmark.ts` - hooks.
- create: `src/components/PagedArray.tsx` (+ `PagedArray.test.tsx`) - paginación de 50 y "longitud N".
- create: `src/features/term-ranking/RankingView.tsx`, `RankingTable.tsx`, `RankingBarChart.tsx`, `RankingView.test.tsx`.
- create: `src/features/sort-trace/SortTraceView.tsx`, `CountingTraceView.tsx`, `RadixTraceView.tsx`, `BucketTraceView.tsx`, `SortTraceView.test.tsx`.
- create: `src/features/sorting-benchmark/BenchmarkPanel.tsx`, `BenchmarkResultCard.tsx`, `BenchmarkHistory.tsx`, `BenchmarkPanel.test.tsx`, `BenchmarkHistory.test.tsx`.
- create: `src/pages/TermRankingPage.tsx` - compone las tres vistas.
- modify: `src/App.tsx` - MUI `Tabs` entre `HomePage` y `TermRankingPage`.

Documentación:
- modify: `docs/traceability.md` - filas del Requerimiento 2.

## 6. Interfaces and data
Algoritmos (`algorithms/ranking`, `trace/ranking`):
```java
public record TermFrequency(String term, int frequency) {}
public final class TermFrequencyCounter {
    /** Entrada de los algoritmos: alfabético descendente (descendingMap de TreeMap<String,Integer>). */
    public List<TermFrequency> count(List<List<String>> tokensPerAbstract);
}
public record SortOptions(int bucketSize, boolean recordTrace) { public static final int DEFAULT_BUCKET_SIZE = 5; }
public record SortResult<T>(List<TermFrequency> output, T trace) {}   // accesor output(), no sorted(): ver ForbiddenSortUsageTest
public interface TermSorter<T> {
    SortAlgorithm algorithm();
    /** Orden ascendente y estable por frequency; no modifica input. */
    SortResult<T> sort(List<TermFrequency> input, SortOptions options);
}
public final class CountingSort implements TermSorter<CountingSortTrace> {}   // ignora options.bucketSize()
public final class RadixSort implements TermSorter<RadixSortTrace> {}         // ignora options.bucketSize()
public final class BucketSort implements TermSorter<BucketSortTrace> {}       // usa options.bucketSize()
public enum SortAlgorithm {
    COUNTING("counting", "O(n+k)"), RADIX("radix", "O(d·(n+b))"), BUCKET("bucket", "O(n+m) promedio, O(n²) peor caso");
    public static SortAlgorithm fromSlug(String slug);              // IllegalArgumentException si es desconocido
    public AlgorithmParameters parameters(List<TermFrequency> input, int bucketSize);
}
public record AlgorithmParameters(Integer k, Integer d, Integer b, Integer m) {}   // los que no aplican van en null; lista vacía: 0
public final class RankingReader {
    public static List<TermFrequency> read(List<TermFrequency> ascending);   // de derecha a izquierda
    public static List<TermFrequency> top(List<TermFrequency> ranking, int n); // min(n, tamaño)
    public static boolean sameRanking(List<List<TermFrequency>> rankings);   // igualdad elemento a elemento
}
public record CountingSortTrace(List<TermFrequency> input, int max, int[] count, int[] cumulative,
                                List<Placement> placements, List<TermFrequency> output) {
    public record Placement(int inputIndex, TermFrequency element, int outputIndex) {}
}
public record RadixSortTrace(List<TermFrequency> input, int max, List<Pass> passes, List<TermFrequency> output) {
    public record Pass(int exp, int[] digits, int[] count, int[] cumulative, List<TermFrequency> result) {}
}
public record BucketSortTrace(List<TermFrequency> input, int min, int max, int bucketSize, int bucketCount,
                              List<Bucket> buckets, List<TermFrequency> concatenated) {
    public record Bucket(int index, List<TermFrequency> before, List<TermFrequency> after) {}
}
```
- `count` en `CountingSortTrace` es el arreglo tras contar (AC-10: `[0,2,0,1,1]`) y `cumulative` el arreglo tras acumular (`[0,2,2,3,4]`). `placements` sigue el orden del recorrido de derecha a izquierda.
- Lista vacía: los tres devuelven `output = []` sin excepción. La traza tiene listas vacías, `max`/`min` en 0, `count`/`cumulative` vacíos, `passes` vacío y `bucketCount` en 0. La referencia lee `array[0]` sin comprobar; aquí se agrega esa guarda.
- Insertion sort de cada cubeta: desplaza mientras `previo.frequency > clave.frequency` (estricto), de modo que queda estable.

Evaluación (`evaluation`):
```java
public final class Stopwatch { public static <T> Timed<T> time(Supplier<T> work); public record Timed<T>(T value, double millis) {} }
public final class FrequencyListFile { public static void write(Path file, List<TermFrequency> input); public static List<TermFrequency> read(Path file); }
public record JmhParameters(int forks, int warmupIterations, int warmupSeconds, int measurementIterations,
                            int measurementSeconds, String mode, String timeUnit, int bucketSize, boolean traceEnabled) {
    public static final JmhParameters FIXED = new JmhParameters(1, 5, 1, 10, 1, "avgt", "ms", 5, false);
}
public record JmhScore(SortAlgorithm algorithm, double scoreMs, double scoreErrorMs) {}
public interface BenchmarkLauncher { List<JmhScore> run(List<TermFrequency> input, Duration timeout) throws BenchmarkTimeoutException; }
public final class JmhResultParser { public List<JmhScore> parse(Path resultJson); }   // primaryMetric.score y primaryMetric.scoreError
```
- Comando de `JmhProcessLauncher`: `<java.home>/bin/java -jar <bibliomtr.benchmark.jar> SortingBenchmark -f 1 -wi 5 -w 1s -i 10 -r 1s -bm avgt -tu ms -rf json -rff <tmp>/result.json -p inputFile=<tmp>/input.tsv`. El directorio temporal se crea con `Files.createTempDirectory("bibliomtr-jmh-")` y se borra al terminar. Si `waitFor(timeout)` vence, el launcher hace `destroyForcibly()` y lanza `BenchmarkTimeoutException`. Si el código de salida es distinto de 0, lanza `IllegalStateException` con las últimas líneas de la salida.
- `SortingBenchmark`: `@State(Scope.Benchmark)`, `@Fork(1)`, `@Warmup(iterations = 5, time = 1)`, `@Measurement(iterations = 10, time = 1)`, `@BenchmarkMode(Mode.AverageTime)`, `@OutputTimeUnit(TimeUnit.MILLISECONDS)` y `@Param("") String inputFile`. `@Setup(Level.Trial)` lee el TSV y, si `inputFile` está vacío, lanza "Falta el parámetro inputFile". Los métodos `countingSort()`, `radixSort()` y `bucketSort()` llaman a `sort(input, new SortOptions(5, false))` y devuelven el resultado.

API REST:

| Método y ruta | Parámetros | Respuesta |
|---|---|---|
| `GET /api/ranking` | `n` (int ≥ 1, default 20), `bucketSize` (int ≥ 1, default 5) | 200 `RankingResponse` |
| `GET /api/ranking/trace/{algorithm}` | `algorithm` ∈ {`counting`, `radix`, `bucket`}, `bucketSize` (int ≥ 1, default 5) | 200 `SortTraceResponse` |
| `POST /api/benchmarks` | ninguno | 202 `BenchmarkRunDto` (EN_CURSO) o 409 |
| `GET /api/benchmarks` | ninguno | 200 `List<BenchmarkRunDto>`, todas, por `startedAt` descendente |

```java
record RankingResponse(int termCount, int n, int bucketSize, double preprocessingMs, boolean allMatch, List<AlgorithmRanking> algorithms) {}
record AlgorithmRanking(String algorithm, List<TermFrequency> top, double elapsedMs) {}
record SortTraceResponse(String algorithm, int bucketSize, List<TermFrequency> input,
                         @JsonProperty("sorted") List<TermFrequency> output,   // JSON conserva "sorted"
                         List<TermFrequency> ranking, Object trace) {}   // trace: CountingSortTrace | RadixSortTrace | BucketSortTrace
record BenchmarkRunDto(long id, Instant startedAt, Instant finishedAt, String status, String message,
                       JmhParameters parameters, Integer termCount, List<BenchmarkResultDto> results) {}
record BenchmarkResultDto(String algorithm, double scoreMs, double scoreErrorMs, AlgorithmParameters parameters, String complexity) {}
```
- Validación: `@RequestParam @Min(1) int` con validación de método de Spring. `ApiExceptionHandler` traduce `MethodArgumentTypeMismatchException` y `HandlerMethodValidationException` a 400 `ProblemDetail`, con `detail` = "El parámetro <n|bucketSize> debe ser un entero mayor o igual a 1". Un slug desconocido da 400 con "Algoritmo desconocido: <x>. Valores válidos: counting, radix, bucket". `BenchmarkInProgressException` da 409 con "Ya hay una medición en curso".
- Mensajes de FALLIDA: "La medición superó el límite de 10 minutos", "La medición falló: <causa>" y "Medición interrumpida por reinicio de la aplicación".
- `BenchmarkService`:
  - `start()`: synchronized. Revisa `existsByStatus(EN_CURSO)`, arma la entrada, calcula `AlgorithmParameters` con `bucketSize = 5`, guarda EN_CURSO y luego envía la tarea a `benchmarkExecutor`.
  - `history()`: `findAllByOrderByStartedAtDesc()`.
- `BenchmarkStartupRecovery`: escucha `ApplicationReadyEvent` y pasa cada medición de `findAllByStatus(EN_CURSO)` a FALLIDA con `finishedAt = now`.

Base de datos (`V<siguiente libre>__create_benchmark_tables.sql`):
```sql
benchmark_run(id bigserial PK, started_at timestamptz NOT NULL, finished_at timestamptz NULL,
  status varchar(12) NOT NULL CHECK (status IN ('EN_CURSO','COMPLETADA','FALLIDA')), message text NULL,
  forks int NOT NULL, warmup_iterations int NOT NULL, warmup_seconds int NOT NULL,
  measurement_iterations int NOT NULL, measurement_seconds int NOT NULL, mode varchar(8) NOT NULL,
  time_unit varchar(4) NOT NULL, bucket_size int NOT NULL, trace_enabled boolean NOT NULL, term_count int NOT NULL)
benchmark_algorithm_result(id bigserial PK, run_id bigint NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
  algorithm varchar(8) NOT NULL, score_ms double precision NOT NULL, score_error_ms double precision NOT NULL,
  max_frequency int NULL, max_digits int NULL, radix_base int NULL, bucket_count int NULL, complexity varchar(64) NOT NULL)
```
- El historial se ordena con un método derivado (`ORDER BY started_at DESC`), que no está en la lista de AGENTS.md ni en REQ-12.

`ForbiddenSortUsageTest`, criterio documentado en su Javadoc: recorre los `*.java` de `src/main/java` y los `*.ts`/`*.tsx` de `web/src` que no sean tests, quitando comentarios, y falla ante cualquiera de estos patrones:
- `Collections\.sort\s*\(`, `Arrays\.sort\s*\(`, `Arrays\.parallelSort\s*\(` y `\.sorted\s*\(` (Stream.sorted).
- `\.sort\s*\(` cuyo primer argumento es un comparador: `null`, `Comparator.`, una lambda `->` o una referencia `::`. Así se detectan `List.sort` y las llamadas `.sort(` sobre `List`/`Comparator`, sin marcar `TermSorter.sort(input, options)`.
- En TS, `\.sort\s*\(` y `\.toSorted\s*\(` (`Array.prototype`).
- Imports de librerías de ordenamiento: `com.google.common.collect.Ordering` y `org.apache.commons.collections4.ComparatorUtils`.
- `TreeMap`, `TreeSet` o `PriorityQueue` fuera de `algorithms/ranking/TermFrequencyCounter.java`.

`docs/referencias/` queda fuera del recorrido. Ningún record ni método del código de producción se llama `sorted`, para que el patrón `\.sorted\s*\(` no necesite excepciones.

Frontend:
- `useTermRanking(n, bucketSize)`: GET `/api/ranking`.
- `useSortTrace(algorithm, bucketSize)`: GET `/api/ranking/trace/{algorithm}`, habilitado al elegir un algoritmo.
- `useBenchmarks()`: GET `/api/benchmarks`, con `refetchInterval` de 2000 ms si algún elemento está EN_CURSO y sin refresco en otro caso.
- `useStartBenchmark()`: POST que invalida `['benchmarks']`.
- `PagedArray<T>({ items, renderItem, label })`: con 50 o menos elementos los muestra todos; con más de 50 pagina de a 50 con el rango "0–49" y "longitud N".
- `RankingView`: con `termCount = 0` muestra "Sin términos en el corpus". En otro caso, por algoritmo: `RankingTable` (MUI Table: posición, término, frecuencia), `RankingBarChart` (Recharts `BarChart` con `width`/`height` explícitos) y `elapsedMs`, además de `preprocessingMs` y `allMatch`. El error de `ApiError` se muestra tal cual.
- `SortTraceView`: selector de algoritmo y `bucketSize`, con los pasos en orden.
  - Counting: entrada, máximo, conteo, acumulado, colocaciones, salida, ranking.
  - Radix: entrada, máximo, una tarjeta por pasada (exp, dígitos, conteo, acumulado, resultado), ranking.
  - Bucket: entrada, mínimo y máximo, número de cubetas, cubetas antes y después, concatenado, ranking.
  - Cada paso lleva su fórmula en `react-katex` y todo arreglo pasa por `PagedArray`.
- `BenchmarkPanel`:
  - Botón "Lanzar medición JMH", deshabilitado si hay una medición EN_CURSO, y el estado de la medición más reciente.
  - El mensaje de FALLIDA y del 409 se muestra en un `Alert`.
  - `BenchmarkResultCard` muestra la COMPLETADA más reciente, por algoritmo: `scoreMs ± scoreErrorMs ms`, n, sus parámetros (k; d y b; m) y la complejidad.
  - Sin ninguna COMPLETADA muestra "Sin mediciones JMH" junto al botón.
- `BenchmarkHistory`: lista MUI con todas las mediciones en el orden recibido (sin paginar ni reordenar), con fecha, estado y resultados.

## 8. Traceability
| Requirement | Component / file | Verified by (AC) |
|---|---|---|
| REQ-1 | `TermFrequencyCounter`, `CorpusTokenSource`, `SelectableAbstractTokenSource`; `TermFrequencyCounterTest`, `RankingControllerTest`, `BibliomtrApplicationTests`, `CorpusRankingConsistencyTest` | AC-1, AC-2 |
| REQ-2 | `TermFrequencyCounter`, `RankingReader`; `TermFrequencyCounterTest`, `RankingReaderTest` | AC-1 |
| REQ-3 | `RankingService`, `RankingController`, `RankingReader.top`; `RankingControllerTest` | AC-2, AC-22 |
| REQ-4 | `RankingController`, `ApiExceptionHandler`; `RankingControllerTest` | AC-3 |
| REQ-5 | `RankingView`, `RankingTable`, `RankingBarChart`; `RankingView.test.tsx` | AC-4, AC-5 |
| REQ-6 | `CountingSort`; `CountingSortTest` | AC-10, AC-17, AC-18 |
| REQ-7 | `RadixSort`; `RadixSortTest` | AC-11, AC-17, AC-18 |
| REQ-8 | `BucketSort`; `BucketSortTest` | AC-9, AC-17, AC-18 |
| REQ-9 | `SortOptions`, `RankingController`; `RankingControllerTest` | AC-3, AC-25 |
| REQ-10 | `TermSorter`, `RankingReader.sameRanking`; `RankingReaderTest`, `SortingOracleTest` | AC-6, AC-19 |
| REQ-11 | `RankingService`, `Stopwatch`, `RankingResponse`; `RankingControllerTest` | AC-6, AC-7 |
| REQ-12 | `ForbiddenSortUsageTest` | AC-8 |
| REQ-13 | `CountingSortTrace`; `CountingSortTest` | AC-10 |
| REQ-14 | `RadixSortTrace`; `RadixSortTest` | AC-11 |
| REQ-15 | `BucketSortTrace`; `BucketSortTest` | AC-9 |
| REQ-16 | `RankingController` (traza), `SortTraceView`, `PagedArray`; `RankingControllerTest`, `PagedArray.test.tsx`, `SortTraceView.test.tsx` | AC-12, AC-23, AC-25 |
| REQ-17 | `BenchmarkController`, `BenchmarkService`, `JmhProcessLauncher`, `SortingBenchmark`, `BenchmarkPanel`; `BenchmarkServiceIT`, `BenchmarkPanel.test.tsx` | AC-13 |
| REQ-18 | `JmhParameters.FIXED`, `SortingBenchmark`, `JmhProcessLauncher`, `build.gradle.kts`; `BenchmarkServiceIT` | AC-13 |
| REQ-19 | `BenchmarkService`, `BenchmarkInProgressException`, `ApiExceptionHandler`; `BenchmarkServiceIT` | AC-14 |
| REQ-20 | `BenchmarkStatus`, `BenchmarkPanel`, `useBenchmarks`; `BenchmarkServiceIT`, `BenchmarkPanel.test.tsx` | AC-13 |
| REQ-21 | `BenchmarkService`, `JmhProcessLauncher`, `BenchmarkStartupRecovery`; `BenchmarkServiceIT`, `BenchmarkRestartIT`, `BenchmarkPanel.test.tsx` | AC-15, AC-24 |
| REQ-22 | `JmhResultParser`, `SortAlgorithm.parameters`, `BenchmarkResultCard`; `JmhResultParserTest`, `SortAlgorithmTest`, `BenchmarkPanel.test.tsx` | AC-13 |
| REQ-23 | `BenchmarkPanel`; `BenchmarkPanel.test.tsx` | AC-16 |
| REQ-24 | `CountingSortTest`, `RadixSortTest`, `BucketSortTest`, `SortEdgeCases` | AC-9, AC-10, AC-11, AC-17, AC-18 |
| REQ-25 | `oracle/SortingOracleTest` | AC-19 |
| REQ-26 | `CorpusRankingConsistencyTest` | AC-26 |
| REQ-27 | `BenchmarkRun`, `BenchmarkAlgorithmResult`, migración Flyway; `BenchmarkServiceIT`, `BenchmarkRestartIT` | AC-20, AC-21 |
| REQ-28 | `BenchmarkController` (GET), `BenchmarkHistory`; `BenchmarkServiceIT`, `BenchmarkHistory.test.tsx` | AC-20, AC-27 |

Tests por AC (given - when - then de la spec):

| AC | Test (archivo) | Comprobación |
|---|---|---|
| AC-1 | `TermFrequencyCounterTest`, `RankingReaderTest` | tokens `model`×42, `data`×30, `graph`×30 → entrada [model 42, graph 30, data 30]; ranking model, data, graph |
| AC-2 | `RankingControllerTest`, `BibliomtrApplicationTests` | fake con 25 términos, sin `n` → 20 términos por algoritmo; contexto completo sin `FakeCorpusTokenSource` e ingesta apagada → arranca y el bean `CorpusTokenSource` es `SelectableAbstractTokenSource` |
| AC-3 | `RankingControllerTest` | `n` = 0, `n` = abc, `bucketSize` = 0 y `bucketSize` = abc → 400 con `detail` en español |
| AC-4 | `RankingControllerTest`, `RankingView.test.tsx` | fake vacío → 200, `termCount` 0 y `top` vacíos; la vista muestra "Sin términos en el corpus" |
| AC-5 | `RankingView.test.tsx` | respuesta con 3 términos → tabla con 3 filas y 3 barras |
| AC-6 | `RankingReaderTest` (los tres algoritmos sobre la misma lista), `RankingControllerTest` | rankings completos iguales; `allMatch` es true |
| AC-7 | `RankingControllerTest` | respuesta con `top` por algoritmo, sin campo `trace`, `preprocessingMs` ≥ 0 y `elapsedMs` ≥ 0 por algoritmo; el servicio usa `recordTrace = false` (lo verifica un `TermSorter` espía) |
| AC-8 | `ForbiddenSortUsageTest` | ningún patrón prohibido en el código de producción |
| AC-9 | `BucketSortTest` | [5,3,8,3] con `bucketSize` 2 → 3 cubetas: [3,3], [5], [8] |
| AC-10 | `CountingSortTest` | [4,1,3,1] → conteo [0,2,0,1,1] y acumulado [0,2,2,3,4] |
| AC-11 | `RadixSortTest` | [170,45,75,90,802,24,2,66] → pasadas con exp 1, 10 y 100; tras exp = 1: [170,90,802,2,24,45,75,66] |
| AC-12 | `PagedArray.test.tsx`, `SortTraceView.test.tsx` | 88 elementos → páginas 0–49 y 50–87 con "longitud 88"; los pasos aparecen en el orden definido |
| AC-13 | `BenchmarkServiceIT`, `BenchmarkPanel.test.tsx` | POST → EN_CURSO; con el launcher falso, COMPLETADA con 3 resultados (ms ± error, n, k / d y b / m, complejidad); la vista muestra el botón deshabilitado en EN_CURSO y los resultados al completar |
| AC-14 | `BenchmarkServiceIT` | launcher bloqueado → el segundo POST da 409 con "Ya hay una medición en curso" |
| AC-15 | `BenchmarkServiceIT`, `BenchmarkPanel.test.tsx` | una COMPLETADA y luego un launcher que falla o lanza timeout → FALLIDA con mensaje en español; la COMPLETADA sigue en el historial y en la vista |
| AC-16 | `BenchmarkPanel.test.tsx` | historial sin COMPLETADA → "Sin mediciones JMH" y botón habilitado |
| AC-17 | `CountingSortTest`, `RadixSortTest`, `BucketSortTest` con `SortEdgeCases` | un término, frecuencias iguales (estable), ya ordenada, inversa, k ≫ n ([1, 1000]) |
| AC-18 | `CountingSortTest`, `RadixSortTest`, `BucketSortTest` | [] → [] sin excepción |
| AC-19 | `oracle/SortingOracleTest` | ranking de cada algoritmo = `List.sort` con `Comparator` (frecuencia descendente, término ascendente) sobre listas pequeñas y aleatorias con semilla fija |
| AC-20 | `BenchmarkServiceIT`, `BenchmarkHistory.test.tsx` | tres mediciones → GET en orden de más reciente a más antigua, con fecha, estado y resultados; la vista las lista en ese orden |
| AC-21 | `BenchmarkRestartIT` | medición guardada, segundo arranque → el historial la conserva |
| AC-22 | `RankingControllerTest` | fake con 10 términos, sin `n` y con `n` = 11 → 10 términos |
| AC-23 | `RankingControllerTest` | GET de la traza de cada algoritmo → traza completa sobre toda la lista |
| AC-24 | `BenchmarkRestartIT` | fila EN_CURSO, segundo arranque → FALLIDA con "Medición interrumpida por reinicio de la aplicación"; el POST siguiente da 202 |
| AC-25 | `RankingControllerTest` | traza bucket con 10 → ⌊(max − min)/10⌋ + 1 cubetas; sin parámetro → 5; con 0 o abc → 400; counting y radix dan la misma traza con 5 y con 10 |
| AC-26 | `CorpusRankingConsistencyTest` | sin PDF en `corpus/` → omitido por Assumptions en un `@BeforeAll` estático, con el motivo, antes de arrancar el contexto; con PDF → arranque con `bibliomtr.ingestion.enabled=true`, `bibliomtr.corpus.dir=corpus` y `FakeEmbedders.Config`, ingesta real de los PDF, `RankingService.rank(20, 5)` con `termCount` > 0, `allMatch` true (rankings completos iguales) y los tres `top` iguales |
| AC-27 | `BenchmarkServiceIT`, `BenchmarkHistory.test.tsx` | 30 mediciones → 30 en una sola lista, en orden descendente |

Comandos: `./gradlew test` (backend; `*IT` usa Testcontainers) y `npm test` en `web/`.

## 9. Decisions and risks
- Ejecución de JMH: `jmhJar` corre como proceso hijo `java -jar`. Se rechazó el `Runner` dentro del proceso porque el fork de JMH falla con el fat jar de Spring Boot y porque saca los benchmarks de `src/jmh`.
- Entrada de JMH en TSV temporal: JMH mide exactamente la lista que calcula la API. Se rechazó que el benchmark relea el corpus porque duplicaría la ingesta de la 001.
- Puerto `CorpusTokenSource`: algoritmos, API (con fake), JMH e interfaz se construyen sin la 001. Se rechazó esperar la 001 completa o duplicar su preprocesamiento (fuera de alcance).
- `recordTrace` como bandera en `SortOptions`: un solo cuerpo por algoritmo garantiza que lo medido y lo trazado sean el mismo código. Se rechazaron dos métodos por algoritmo porque permiten que diverjan.
- Accesor `SortResult.output()` (antes `sorted()`) y componente `SortTraceResponse.output` con `@JsonProperty("sorted")`: el accesor `sorted()` chocaba con el patrón de `Stream.sorted` en `ForbiddenSortUsageTest`. Se rechazó dejar el record pattern en `RankingService`, porque es un rodeo que exige justificación, y se rechazó agregar una excepción al test, porque debilita la verificación de AC-8.
- Valores de estado EN_CURSO, COMPLETADA y FALLIDA en español: la spec los fija como contrato. El resto de identificadores va en inglés.
- Vitest + Testing Library en `web/` para los AC de interfaz; el comando `npm test` ya está en AGENTS.md.
- Risk: el adaptador depende de nombres de la 001 - `SelectableAbstractTokenSource` es el único punto que los usa: `ArticleRepository.findAllByAbstractDetectedTrueOrderByFileNameAsc()` (`persistence/article`) y `new TextPreprocessor().tokenize(String)` (`preprocessing`).
- Risk: sin un bean `CorpusTokenSource` en `src/main`, `bootRun` falla con `NoSuchBeanDefinitionException` - `SelectableAbstractTokenSource` es `@Component` y `BibliomtrApplicationTests` arranca el contexto sin `FakeCorpusTokenSource` y verifica que el bean real existe.
- Risk: la ingesta de la 001 al arrancar puede exigir Ollama y el modelo ONNX en los tests - `src/test/resources/config/application.properties` apaga la ingesta en todos los tests (`BenchmarkServiceIT`, `BenchmarkRestartIT` y `BibliomtrApplicationTests` arrancan sin ingerir). Por eso `CorpusRankingConsistencyTest` la enciende con `bibliomtr.ingestion.enabled=true` y `bibliomtr.corpus.dir=corpus`, y usa `FakeEmbedders.Config` en lugar de Ollama y SPECTER2; exige `termCount` > 0 para no comparar tres rankings vacíos.
- Risk: el número de la migración Flyway depende de las migraciones de la 001 - se usa el siguiente número libre al implementar y nunca se edita una migración aplicada.
- Risk: `jmhJar` del plugin `me.champeau.jmh` 0.7.3 no incluye el código de `src/main` o sus dependencias - se verifica ejecutando `java -jar build/libs/benchmarks.jar -l`, que debe listar `SortingBenchmark`.
- Risk: Recharts no dibuja dentro de `ResponsiveContainer` en jsdom - `RankingBarChart` usa `width`/`height` explícitos.
- Risk: `exp *= 10` desborda `int` con un máximo cercano a 2³¹ - las frecuencias del corpus están varios órdenes por debajo; el Javadoc de `RadixSort` declara el límite.
