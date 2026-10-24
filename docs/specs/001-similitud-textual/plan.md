# Plan: Entorno para el Requerimiento 1: Similitud textual (clásicos e IA)

## 1. Metadata
- Spec: `docs/specs/001-similitud-textual/spec.md`
- Updated: 2026-10-23
- Constitution version: 1.1.0

## 2. Constitution check
- P1: los 6 stubs de `algorithms/similarity` solo lanzan `UnsupportedOperationException("TODO estudiante: …")`. El andamiaje no trae lógica de los algoritmos ni de su normalización a [0,1]. `ForbiddenSimilarityUsageTest` impide librerías y funciones SQL de similitud. El código nuevo de `src/main` y `web/src` no usa `TreeMap`, `TreeSet`, `PriorityQueue` ni ordenamientos de librería, porque `ForbiddenSortUsageTest` de la 002 recorre todo el código de producción.
- P2: PDFBox solo extrae texto, título y autores. ONNX Runtime y el tokenizer de DJL ejecutan SPECTER2. Ollama solo devuelve el vector de nomic-embed-text. KaTeX y MUI solo dibujan, y el mapa de calor es una tabla HTML propia.
- P3: SPECTER2 base (revisión fijada en `scripts/download-model.sh`) y `nomic-embed-text:v1.5` son preentrenados. Coseno y euclidiana son stubs. El andamiaje hace el pooling [CLS] y la normalización L2 de SPECTER2 (Clarifications 2026-10-18).
- P4: `docs/guia-implementacion.md` declara que el andamiaje se generó con apoyo de IA generativa y que los 6 algoritmos los escriben los estudiantes.
- P5: solo se ingieren los PDF de `bibliomtr.corpus.dir`. Los clásicos y nomic usan solo el abstract (`clustering: <abstract>`). SPECTER2 recibe `título [SEP] abstract`, que es la excepción de la constitución 1.1.0.
- P6: el Javadoc de cada stub trae la definición formal, la notación y la normalización sugerida. El visor de traza muestra las mismas fórmulas en KaTeX.
- P7: cada stub devuelve `SimilarityResult(value, similarity, trace)` con un record de traza en `trace/similarity`. `GET /api/similarity/trace` lo expone y `web/src/features/similarity-trace` lo muestra paso a paso.
- P8: todo tipo y método público del andamiaje lleva Javadoc en español con qué, por qué y complejidad. Cada stub deja la complejidad marcada como "Complejidad: TODO estudiante". `StubDocumentationTest`, en la suite estándar, verifica solo que exista la sección "Complejidad:", así sigue en verde cuando el estudiante la completa. `StubScaffoldTest` (`scaffold`) verifica el "TODO estudiante".
- P9: `docs/traceability.md` recibe una fila por módulo. `docs/guia-implementacion.md` enlaza cada algoritmo con su archivo, su vista y sus tests.
- P10: los tests de estudiante cubren casos a mano y los bordes aplicables (vacías, idénticas, disjuntas, longitudes muy distintas), según la resolución de conflicto de la spec.
- P11: `ComparisonResponse` reporta `preprocessingMs` y `elapsedMs` por algoritmo, por separado. `SimilarityBenchmark` (JMH) queda como esqueleto.
- P12: `TextPreprocessor` es determinista (Locale.ROOT, NFD y la lista fija `stopwords-en.txt`). Quedan fijados el tag de la imagen de Ollama y el del modelo, la revisión de SPECTER2, las versiones en `build.gradle.kts` y `package-lock.json`, y la semilla 42 de oráculos y benchmarks.
- P13: `pdf`, `preprocessing`, `embeddings`, `algorithms/similarity`, `trace/similarity`, `evaluation`, `persistence/article`, `api/corpus`, `api/similarity` y `web/src/features/*` son módulos separados. `pdf`, `preprocessing`, `embeddings`, `algorithms`, `trace` y `evaluation` no importan `api` ni `persistence`.

## 3. Architecture
Ingesta (`ApplicationReadyEvent`, si `bibliomtr.ingestion.enabled=true`):
1. `CorpusIngestionRunner` llama a `CorpusIngestionService.ingest(corpus.dir)`. Si el directorio no existe o no tiene `*.pdf`, deja un INFO y termina (AC-10).
2. Por cada PDF se calcula el SHA-256 del contenido. Si `existsByFileSha256` es verdadero, el PDF se salta (AC-11).
3. `PdfArticleExtractor` devuelve título, autores y abstract (este último puede ser null). Sin abstract, el artículo se guarda con `abstract_detected = false` y queda un WARN (AC-9).
4. Antes del primer embedding de la corrida se llama a `NomicEmbedder.ensureModel()` (`POST /api/pull`, con el límite `bibliomtr.ollama.pull-timeout`). El pull queda marcado como hecho solo si `ensureModel()` termina sin excepción. Si falla, ese PDF cuenta como fallido con su ERROR y el siguiente PDF con abstract de la misma corrida vuelve a llamar a `ensureModel()`.
5. `Specter2Embedder.embed(title, abstract)` tokeniza `título [SEP] abstract`, trunca a 512 tokens si hace falta, toma el vector [CLS] y lo normaliza con L2. `NomicEmbedder.embed(abstract)` envía `clustering: <abstract>`. Si el texto se truncó, queda un WARN.
6. Si cualquiera de los dos embeddings falla, el artículo no se guarda y queda un ERROR con el archivo y la causa. Se reintenta en el siguiente arranque porque su SHA-256 no quedó en la base. Si todo sale bien, se guarda el `Article` con los dos vectores.

Comparación (`POST /api/similarity/comparisons`):
1. `ArticleSelectionValidator` revisa la cantidad (2..20), los repetidos y que cada id exista y sea seleccionable. Cualquier falla lanza `InvalidSelectionException`, que se responde con 400.
2. Preprocesamiento, cronometrado como `preprocessingMs`: carga de los artículos, tokens de cada uno (`TextPreprocessor.tokenize`), secuencias de edición según `bibliomtr.similarity.edit-granularity` (WORDS: tokens; CHARACTERS: `TextPreprocessor.characters`) y tokens de todos los seleccionables para el IDF.
3. Por cada uno de los 8 `ComparisonAlgorithm` se cronometra el recorrido de los pares i ≤ j y cada valor se refleja en j, i. Si el stub lanza `UnsupportedOperationException` con prefijo `TODO estudiante:`, el resultado queda PENDIENTE, sin matriz ni tiempo. Una similitud en [−1e-9, 1 + 1e-9] se recorta (clamp) a [0,1] antes de devolverla, para absorber errores de redondeo como 1.0000000000000002. Una similitud NaN o fuera de esa tolerancia lanza `IllegalStateException`, que se responde con 500.

Traza (`GET /api/similarity/trace`): los mismos pasos 1 y 2 para el par, y un solo algoritmo que devuelve `value`, `similarity` y `trace`, o PENDIENTE.

Integración con la 002: la 001 expone `ArticleRepository.findAllByAbstractDetectedTrueOrderByFileNameAsc()` y `TextPreprocessor.tokenize(String)`, que usará el adaptador `SelectableAbstractTokenSource` (tarea T083 de la 002). La 001 no registra ningún bean `CorpusTokenSource`.

Interfaz: `SimilarityPage` reúne `ArticleSelector`, `SimilarityResults` (un mapa de calor `Heatmap` o el aviso de pendiente por algoritmo, con los tiempos) y `SimilarityTraceView` (algoritmo y par; al hacer clic en una celda del mapa de calor se elige el par). Los datos se cargan con hooks de TanStack Query.

## 4. Design patterns
- Strategy en `SequenceSimilarity`, `CorpusSimilarity` y `VectorSimilarity`: `ComparisonService` y `SimilarityBenchmark` llaman a los 6 stubs con una firma por tipo de entrada, y los tests de API los reemplazan por algoritmos falsos.
- Registro `SimilarityCatalog` (record con las 6 estrategias, bean en `SimilarityConfig`): un solo bean `@Primary` de test sustituye todos los algoritmos para AC-13, AC-15, AC-17 y AC-19. Así esos tests no dependen de que los estudiantes hayan implementado algo.
- Interfaces `Specter2Embedder` y `NomicEmbedder` con implementaciones `Specter2OnnxEmbedder` y `NomicOllamaEmbedder`: los IT de ingesta usan fakes, sin modelo ONNX ni Ollama.

## 5. Code structure
Backend, producción (`src/main/java/co/edu/uniquindio/bibliomtr/`):
- create: `preprocessing/TextPreprocessor.java` - `normalize`, `tokenize` y `characters`.
- create: `preprocessing/EditGranularity.java` - enum WORDS, CHARACTERS.
- create: `src/main/resources/preprocessing/stopwords-en.txt` - lista inglesa de NLTK (179 palabras), una por línea, copiada una vez.
- create: `pdf/ExtractedArticle.java`, `pdf/PdfArticleExtractor.java`, `pdf/AbstractLocator.java` - extracción y heurísticas.
- delete: `pdf/LargestFontLineStripper.java` - el título ya no usa la línea de mayor fuente, porque el respaldo es el nombre del archivo.
- create: `embeddings/EmbeddingModel.java` - enum SPECTER2 y NOMIC, ambos de 768 dimensiones.
- create: `embeddings/VectorNormalization.java` - `l2Normalize(float[])`.
- create: `embeddings/Specter2Input.java` - arma `título [SEP] abstract`.
- create: `embeddings/Specter2Embedding.java`, `embeddings/Specter2Embedder.java`, `embeddings/Specter2OnnxEmbedder.java` - SPECTER2 con carga perezosa del modelo.
- create: `embeddings/NomicEmbedder.java`, `embeddings/NomicOllamaEmbedder.java` - cliente `RestClient` de Ollama.
- create: `algorithms/similarity/package-info.java` - Javadoc del módulo del Requerimiento 1 y enlace a la guía.
- create: `algorithms/similarity/SimilarityResult.java`, `SequenceSimilarity.java`, `CorpusSimilarity.java`, `VectorSimilarity.java` - contratos.
- create: `algorithms/similarity/NeedlemanWunschScoring.java` - match, mismatch y gap con `DEFAULT = (1, −1, −1)`.
- create: `algorithms/similarity/Levenshtein.java`, `NeedlemanWunsch.java`, `Jaccard.java`, `TfIdfCosineSimilarity.java`, `CosineSimilarity.java`, `EuclideanDistance.java` - los 6 stubs.
- create: `trace/similarity/LevenshteinTrace.java`, `NeedlemanWunschTrace.java`, `JaccardTrace.java`, `TfIdfCosineTrace.java`, `CosineTrace.java`, `EuclideanTrace.java` - contratos de traza.
- create: `persistence/article/Article.java`, `persistence/article/ArticleRepository.java` - entidad y repositorio.
- create: `src/main/resources/db/migration/V3__create_article_table.sql` - tabla `article`.
- create: `api/corpus/CorpusIngestionService.java`, `api/corpus/IngestionReport.java`, `api/corpus/CorpusIngestionRunner.java` - ingesta.
- create: `api/corpus/ArticleController.java`, `api/corpus/ArticleSummary.java` - `GET /api/articles`.
- create: `api/similarity/ComparisonAlgorithm.java` - enum de los 8 resultados con slug, etiqueta, archivo y método del stub.
- create: `api/similarity/AlgorithmStatus.java` - CALCULADO y PENDIENTE.
- create: `api/similarity/SimilarityCatalog.java` - record con las 6 estrategias.
- create: `api/similarity/ArticleSelectionValidator.java`, `api/similarity/InvalidSelectionException.java` - validación.
- create: `api/similarity/ComparisonService.java`, `api/similarity/ComparisonController.java`, `api/similarity/ComparisonDtos.java` - comparación y traza.
- create: `config/SimilarityConfig.java` - bean `SimilarityCatalog` con los stubs y `NeedlemanWunschScoring.DEFAULT`; beans `RestClient` de Ollama `ollamaRestClient` (conexión y lectura con `timeout`) y `ollamaPullRestClient` (conexión con `timeout`, lectura con `pullTimeout`); bean `nomicEmbedder` (`NomicOllamaEmbedder` con los dos clientes y `bibliomtr.ollama.model`) y bean `specter2Embedder` (`Specter2OnnxEmbedder` con `bibliomtr.embeddings.model-path` y `tokenizer-path`).
- modify: `config/BibliomtrProperties.java` - agrega `Ingestion(boolean enabled)`, `Ollama(URI baseUrl, String model, Duration timeout, Duration pullTimeout)` y `Similarity(EditGranularity editGranularity)`.
- modify: `api/ApiExceptionHandler.java` - agrega 400 para `MissingServletRequestParameterException` y `HttpMessageNotReadableException`. `InvalidSelectionException` extiende `IllegalArgumentException` y la cubre el handler existente.
- modify: `src/main/resources/application.properties` - `bibliomtr.ingestion.enabled=true`, `bibliomtr.ollama.base-url=${BIBLIOMTR_OLLAMA_URL:http://localhost:11434}`, `bibliomtr.ollama.model=nomic-embed-text:v1.5`, `bibliomtr.ollama.timeout=60s`, `bibliomtr.ollama.pull-timeout=15m`, `bibliomtr.similarity.edit-granularity=WORDS`.

Benchmark, build y despliegue:
- create: `src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/SimilarityBenchmark.java` - esqueleto con 6 `@Benchmark`.
- modify: `build.gradle.kts` - `tasks.test` excluye las etiquetas `student` y `scaffold`. Se registran las tareas `studentTest` (`includeTags("student")`) y `scaffoldTest` (`includeTags("scaffold")`), con las clases y el classpath de `sourceSets.test`, fuera de `check`. En `tasks.withType<Test>` se declaran como entradas los archivos que leen `StubDocumentationTest`, `StubScaffoldTest`, `ForbiddenSortUsageTest`, `ForbiddenSimilarityUsageTest` e `ImplementationGuideTest`: `inputs.files(fileTree("src/main/java"), fileTree("src/main/resources") { exclude("models/**") }, fileTree("web/src"), fileTree("docs"), file("build.gradle.kts")).withPropertyName("scannedSources").withPathSensitivity(PathSensitivity.RELATIVE)`.
- modify: `compose.dev.yaml` - servicio `ollama` con imagen `ollama/ollama:0.12.6`, puerto `11434:11434` y volumen `ollama-data:/root/.ollama`.
- modify: `compose.yaml` - en `api`, `BIBLIOMTR_OLLAMA_URL=http://ollama:11434` y `depends_on: ollama`.
- modify: `README.md` - Ollama, `./gradlew studentTest`, `./gradlew scaffoldTest`, enlace a la guía y `./gradlew jmhJar && java -jar build/libs/benchmarks.jar SimilarityBenchmark` en lugar de `./gradlew jmh`.

Backend, tests (`src/test/java/co/edu/uniquindio/bibliomtr/`):
- create: `src/test/resources/config/application.properties` - `bibliomtr.ingestion.enabled=false` para todos los tests. Solo pisa esa clave, porque `classpath:/config/` tiene precedencia sobre `classpath:/`.
- create: `preprocessing/TextPreprocessorTest.java`.
- create: `pdf/AbstractLocatorTest.java`, `pdf/PdfArticleExtractorTest.java`, `pdf/SyntheticPdf.java` (genera PDF con PDFBox en un directorio temporal).
- create: `embeddings/VectorNormalizationTest.java`, `embeddings/Specter2InputTest.java`, `embeddings/Specter2OnnxEmbedderTest.java` (con Assumptions), `embeddings/NomicOllamaEmbedderTest.java` (con un `MockRestServiceServer` por cliente: el de embed y el de pull).
- create: `algorithms/similarity/LevenshteinTest.java`, `NeedlemanWunschTest.java`, `JaccardTest.java`, `TfIdfCosineSimilarityTest.java`, `CosineSimilarityTest.java`, `EuclideanDistanceTest.java`, todos con `@Tag("student")`.
- create: `oracle/LevenshteinOracleTest.java`, `oracle/VectorMetricsOracleTest.java`, con `@Tag("student")`.
- create: `algorithms/similarity/StubScaffoldTest.java`, con `@Tag("scaffold")`.
- create: `algorithms/similarity/StubDocumentationTest.java`, `algorithms/similarity/ForbiddenSimilarityUsageTest.java`, `docs/ImplementationGuideTest.java`, en el suite estándar.
- create: `api/corpus/FakeEmbedders.java` - fakes `@Primary` configurables (vector fijo, truncado, falla, y `fake-embedders.nomic-pull-failures` = N: `ensureModel()` falla en sus N primeras llamadas) y un `@TestConfiguration` anidado `Config`.
- create: `api/corpus/CorpusIngestionIT.java` - `PostgreSQLContainer` estático y arranques con `SpringApplicationBuilder`, como `BenchmarkRestartIT`.
- create: `api/similarity/FakeSimilarityCatalog.java` - catálogo `@Primary` con algoritmos falsos configurables (implementado o pendiente).
- create: `api/similarity/ComparisonApiIT.java` - `@SpringBootTest` + `TestcontainersConfiguration` + `@AutoConfigureMockMvc` + fakes, con artículos insertados por el repositorio.

Frontend (`web/src/`):
- create: `types/similarity.ts` - tipos de artículos, comparación y trazas.
- create: `api/useArticles.ts`, `api/useComparison.ts`, `api/usePairTrace.ts` - hooks.
- create: `components/Heatmap.tsx` (+ `Heatmap.test.tsx`), `components/PagedMatrix.tsx` (+ `PagedMatrix.test.tsx`).
- create: `features/article-selection/ArticleSelector.tsx` (+ `ArticleSelector.test.tsx`).
- create: `features/similarity-matrix/SimilarityResults.tsx`, `features/similarity-matrix/AlgorithmResultCard.tsx` (+ `SimilarityResults.test.tsx`).
- create: `features/similarity-trace/SimilarityTraceView.tsx`, `LevenshteinTraceView.tsx`, `NeedlemanWunschTraceView.tsx`, `JaccardTraceView.tsx`, `TfIdfCosineTraceView.tsx`, `CosineTraceView.tsx`, `EuclideanTraceView.tsx` (+ `SimilarityTraceView.test.tsx`).
- create: `pages/SimilarityPage.tsx`.
- modify: `App.tsx` - pestaña `similarity`, "Similitud textual".

Documentación:
- create: `docs/guia-implementacion.md` - por algoritmo: archivo, método, fórmula, normalización sugerida, traza esperada, casos a mano, comando de test y decisiones pendientes, más la declaración de uso de IA.
- modify: `docs/traceability.md` - filas del Requerimiento 1.

## 6. Interfaces and data
Preprocesamiento (`preprocessing`):
```java
public final class TextPreprocessor {
    public TextPreprocessor();                       // carga stopwords-en.txt en un HashSet
    public String normalize(String text);            // minúsculas (Locale.ROOT), NFD sin marcas, todo carácter fuera de [a-z0-9] → espacio, espacios colapsados, trim
    public List<String> tokenize(String text);       // corridas [a-z0-9] de normalize(text) en orden; descarta longitud < 2, solo dígitos y stopwords
    public List<String> characters(String text);     // cada carácter de normalize(text) como String, espacios incluidos
}
public enum EditGranularity { WORDS, CHARACTERS }
```
- El recorrido de `tokenize` es manual, carácter por carácter, sin expresiones regulares.

PDF (`pdf`):
```java
public record ExtractedArticle(String title, List<String> authors, String abstractText) {}   // abstractText null si no se detecta
public final class PdfArticleExtractor { public ExtractedArticle extract(Path pdf) throws IOException; }
public final class AbstractLocator {
    public static final String PARAGRAPH_MARK = "¶";                  // línea que solo contiene esta marca = fin de párrafo
    public Optional<String> locate(String firstPagesText);
}
```
- Texto: `PDFTextStripper` sobre las páginas 1 y 2, con `setParagraphEnd("\n" + PARAGRAPH_MARK + "\n")`, de modo que cada fin de párrafo queda como una línea que solo contiene `¶`.
- Abstract. Las reglas se aplican en este orden; "forma compacta" de una línea = la línea en minúsculas y sin espacios.
  1. Encabezado. Vale el primero que aparezca de estos dos:
     - Una línea cuya forma compacta empieza por `abstract` ("Abstract", "ABSTRACT", "Abstract:"). El abstract empieza después de `abstract` y de un `:` opcional.
     - La forma espaciada `A B S T R A C T` (letras separadas por espacios, sin distinguir mayúsculas) en cualquier posición de una línea, también a mitad de línea, como en "A R T I C L E I N F O A B S T R A C T". El abstract empieza después de la `T` final.
  2. Lista de keywords junto al encabezado: si la primera línea no vacía después del encabezado es un terminador `keywords`, se salta esa línea y las siguientes de menos de 60 caracteres (la lista de keywords). El abstract empieza en la primera línea de 60 caracteres o más.
  3. Terminador: el abstract termina antes de la primera línea cuya forma compacta empieza por `keywords`, `indexterms`, `introduction`, `1.introduction`, `1introduction` o `1|introduction`. Esto cubre "Key words", "K E Y W O R D S" y "1 | INTRODUC TION".
  4. Terminador al final de una línea: si la forma compacta de una línea termina en `1.introduction` ("…learning. 1. Introduction"), el abstract incluye esa línea hasta antes de "1." y termina ahí.
  5. Sin terminador: si no se encuentra ninguno, el abstract termina en la primera línea `¶` después de su inicio.
  6. Limpieza: se quitan las líneas `¶` y el guion de fin de línea (`-\n`), y las líneas se unen con espacio. Si no hay encabezado o el resultado tiene menos de 100 caracteres, el abstract es null.
- Título: se usa `PDDocumentInformation.getTitle()` salvo que esté vacío, contenga `_` o tenga menos de 3 palabras separadas por espacios. En esos casos, el título es el nombre del archivo sin extensión y sin el prefijo `^\d+\.\s*`. Ya no se usa la línea de mayor fuente.
- Autores: `getAuthor()` separado por `;`, `,` y ` and `, con trim y sin vacíos. Si no hay, lista vacía.

Embeddings (`embeddings`):
```java
public record Specter2Embedding(float[] vector, boolean truncated, int tokenCount) {}       // tokenCount antes de truncar
public interface Specter2Embedder { Specter2Embedding embed(String title, String abstractText); }
public final class Specter2OnnxEmbedder implements Specter2Embedder {
    public static final int MAX_TOKENS = 512;
    Specter2Encoding encode(String text);   // paquete; ids con truncado a 512 incluidos [CLS] y [SEP]
}
public interface NomicEmbedder { void ensureModel(); float[] embed(String abstractText); }
public final class NomicOllamaEmbedder implements NomicEmbedder {
    public NomicOllamaEmbedder(RestClient restClient, RestClient pullRestClient, String model);   // pullRestClient solo para /api/pull
}
public final class Specter2Input { public static String of(String title, String abstractText); }   // title + " [SEP] " + abstractText
public final class VectorNormalization { public static float[] l2Normalize(float[] v); }           // v / ‖v‖₂; IllegalArgumentException si ‖v‖₂ = 0
```
- `Specter2OnnxEmbedder` carga `OrtSession` y `HuggingFaceTokenizer` la primera vez que se llama a `embed`, con un bloque `synchronized`. Las entradas del modelo son `input_ids`, `attention_mask` y `token_type_ids`; la salida es `last_hidden_state[0][0]`, a la que se aplica `l2Normalize`.
- `NomicOllamaEmbedder`:
  - `ensureModel()` hace `POST {baseUrl}/api/pull` con `{"model": "<model>", "stream": false}` a través de `pullRestClient`, cuyo límite de lectura es `bibliomtr.ollama.pull-timeout` (por defecto 15m), porque bajar `nomic-embed-text` (~274 MB) supera los 60 s de `bibliomtr.ollama.timeout`. `embed` usa `restClient`, con `bibliomtr.ollama.timeout`.
  - `embed` hace `POST {baseUrl}/api/embed` con `{"model": "<model>", "input": "clustering: <abstract>"}` y lee `embeddings[0]`.
  - Si la dimensión no es 768, lanza `IllegalStateException`.

Algoritmos (`algorithms/similarity`, `trace/similarity`):
```java
public record SimilarityResult<T>(double value, double similarity, T trace) {}   // value crudo; similarity en [0,1]
public interface SequenceSimilarity<T> { SimilarityResult<T> compare(List<String> a, List<String> b); }
public interface CorpusSimilarity<T> { SimilarityResult<T> compare(List<String> a, List<String> b, List<List<String>> corpus); }
public interface VectorSimilarity<T> { SimilarityResult<T> compare(float[] a, float[] b); }
public record NeedlemanWunschScoring(int match, int mismatch, int gap) { public static final NeedlemanWunschScoring DEFAULT = new NeedlemanWunschScoring(1, -1, -1); }
public final class Levenshtein implements SequenceSimilarity<LevenshteinTrace> {}
public final class NeedlemanWunsch implements SequenceSimilarity<NeedlemanWunschTrace> { public NeedlemanWunsch(NeedlemanWunschScoring scoring); }
public final class Jaccard implements SequenceSimilarity<JaccardTrace> {}
public final class TfIdfCosineSimilarity implements CorpusSimilarity<TfIdfCosineTrace> {}
public final class CosineSimilarity implements VectorSimilarity<CosineTrace> {}
public final class EuclideanDistance implements VectorSimilarity<EuclideanTrace> {}

public record LevenshteinTrace(List<String> a, List<String> b, int[][] matrix, int distance, int maxLength) {}
public record NeedlemanWunschTrace(List<String> a, List<String> b, NeedlemanWunschScoring scoring, int[][] scores,
                                   String[][] moves, List<AlignedPair> alignment, int score, int minScore, int maxScore) {
    public record AlignedPair(String fromA, String fromB) {}   // null = gap; moves: "DIAG", "UP", "LEFT"
}
public record JaccardTrace(List<String> setA, List<String> setB, List<String> intersection, List<String> union) {}   // orden de primera aparición
public record TfIdfCosineTrace(List<String> vocabulary, int documentCount, int[] documentFrequency, double[] idf,
                               double[] tfA, double[] tfB, double[] weightsA, double[] weightsB,
                               double dotProduct, double normA, double normB) {}   // arreglos alineados con vocabulary (unión de a y b)
public record CosineTrace(int dimension, double[] products, double dotProduct, double normA, double normB) {}
public record EuclideanTrace(int dimension, double[] squaredDifferences, double sumOfSquares) {}
```
- Cuerpo de cada stub: `throw new UnsupportedOperationException("TODO estudiante: implementar <Clase>.compare (ver docs/guia-implementacion.md)");`.
- El Javadoc de cada stub tiene las secciones "Qué calcula", "Fórmula", "Normalización sugerida", "Traza", "Rango", "Casos de borde" y "Complejidad: TODO estudiante". En Levenshtein y Needleman-Wunsch agrega "Granularidad": la decide el estudiante en `bibliomtr.similarity.edit-granularity` de `application.properties`.
- Fórmulas y normalización sugeridas (m = |a|, n = |b|):
  - Levenshtein: D[i][0] = i, D[0][j] = j, D[i][j] = min(D[i−1][j] + 1, D[i][j−1] + 1, D[i−1][j−1] + [aᵢ ≠ bⱼ]); value = D[m][n]; similarity = 1 − D[m][n]/max(m, n), y 1 si m = n = 0.
  - Needleman-Wunsch: F[i][0] = i·g, F[0][j] = j·g, F[i][j] = max(F[i−1][j−1] + s(aᵢ, bⱼ), F[i−1][j] + g, F[i][j−1] + g); value = F[m][n]. Con (1, −1, −1): maxScore = 2·min(m, n) − max(m, n) y minScore = −max(m, n), porque min(m, n) sustituciones más max(m, n) − min(m, n) gaps ya alcanzan −max(m, n), y el óptimo nunca queda por debajo. similarity = (score − minScore)/(maxScore − minScore); 1 si m = n = 0 y 0 si solo una secuencia es vacía.
  - Jaccard: J = |A ∩ B| / |A ∪ B| sobre los conjuntos de símbolos; value = similarity = J, y 1 si A = B = ∅.
  - Coseno TF-IDF: tf(t, d) = f₍t,d₎ / |d|, idf(t) = ln(N / df(t)) con N = |corpus|, w = tf·idf, cos = Σ wₐ·w_b / (‖wₐ‖·‖w_b‖); value = similarity = cos. Vale 1 si a y b son vacías y 0 si alguna norma es 0 en otro caso.
  - Coseno: cos = Σ aᵢbᵢ / (‖a‖·‖b‖); value = cos; similarity = (1 + cos)/2.
  - Euclidiana: d = √Σ(aᵢ − bᵢ)²; value = d; similarity = 1/(1 + d).
  - Vectores: si las dimensiones difieren o son 0, `IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0")`.

Base de datos (`V3__create_article_table.sql`):
```sql
article(id bigserial PK, file_name text NOT NULL, file_sha256 varchar(64) NOT NULL UNIQUE,
  title text NOT NULL, authors text[] NOT NULL, abstract_text text NULL, abstract_detected boolean NOT NULL,
  specter2_embedding vector(768) NULL, specter2_truncated boolean NOT NULL, specter2_token_count int NULL,
  nomic_embedding vector(768) NULL, ingested_at timestamptz NOT NULL,
  CHECK (abstract_detected = (abstract_text IS NOT NULL)),
  CHECK (NOT abstract_detected OR (specter2_embedding IS NOT NULL AND nomic_embedding IS NOT NULL)))
```
- `Article`: los vectores se mapean como `float[]` con `@JdbcTypeCode(SqlTypes.VECTOR) @Array(length = 768)` y `authors` como `String[]`.
- `ArticleRepository extends JpaRepository<Article, Long>`: `existsByFileSha256(String)`, `findAllByAbstractDetectedTrueOrderByFileNameAsc()` y `findAllByIdInAndAbstractDetectedTrue(Collection<Long>)`. Ningún método usa `Like` ni `Containing`.

Ingesta (`api/corpus`):
```java
public record IngestionReport(int ingested, int alreadyPresent, int withoutAbstract, int failed) {}
public class CorpusIngestionService { public IngestionReport ingest(Path corpusDir); }
public class CorpusIngestionRunner { @EventListener(ApplicationReadyEvent.class) void onReady(); }   // solo si bibliomtr.ingestion.enabled
```
- Mensajes de log:
  - INFO "Corpus vacío o inexistente: <dir>".
  - WARN "Sin abstract detectado: <archivo>".
  - WARN "<archivo>: texto de SPECTER2 truncado a 512 tokens (<n> tokens)".
  - ERROR "No se pudo ingerir <archivo>: <causa>".
  - INFO con el `IngestionReport`.

API REST:

| Método y ruta | Parámetros | Respuesta |
|---|---|---|
| `GET /api/articles` | ninguno | 200 `List<ArticleSummary>` seleccionables, por `file_name` |
| `POST /api/similarity/comparisons` | cuerpo `ComparisonRequest` | 200 `ComparisonResponse` o 400 |
| `GET /api/similarity/trace` | `algorithm` (slug), `a` y `b` (ids; `a = b` permitido) | 200 `PairTraceResponse` o 400 |

```java
record ArticleSummary(long id, String title, List<String> authors, String fileName, boolean specter2Truncated) {}
record ComparisonRequest(List<Long> articleIds) {}
record ComparisonResponse(List<ArticleSummary> articles, String editGranularity, double preprocessingMs, List<AlgorithmMatrix> results) {}
record AlgorithmMatrix(String algorithm, String label, String status, double[][] matrix, Double elapsedMs, String pendingLocation) {}
record PairTraceResponse(String algorithm, String label, ArticleSummary articleA, ArticleSummary articleB, String editGranularity,
                         String status, Double value, Double similarity, Object trace, Double elapsedMs, String pendingLocation) {}
```
- `ComparisonAlgorithm`, en este orden de respuesta: `levenshtein`, `needleman-wunsch`, `tfidf-cosine`, `jaccard`, `specter2-cosine`, `specter2-euclidean`, `nomic-cosine`, `nomic-euclidean`. `pendingLocation()` = `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/<Clase>.java#compare`. `fromSlug` lanza `IllegalArgumentException("Algoritmo desconocido: <x>. Valores válidos: …")`.
- Las matrices siguen el orden de `articleIds` de la solicitud. En PENDIENTE, `matrix`, `elapsedMs`, `value`, `similarity` y `trace` son null.
- Rango de la similitud: `ComparisonService.SIMILARITY_TOLERANCE = 1e-9`, una constante con Javadoc (qué es: margen de redondeo de punto flotante aceptado alrededor de [0,1]; por qué: un coseno correcto sobre vectores idénticos puede dar 1 + 2⁻⁵²; complejidad O(1)). Se aplica en `compare` y en `trace`:
  - s ∈ [−tolerancia, 1 + tolerancia] → se devuelve min(1, max(0, s)).
  - NaN o fuera de ese intervalo → `IllegalStateException("El algoritmo <slug> devolvió <s> fuera de [0,1] para los artículos <a> y <b>")`, que se responde con 500.
- Mensajes 400 en español, revisados en este orden:
  - "El cuerpo debe ser un JSON con articleIds: lista de enteros" si `articleIds` contiene algún id null, p. ej. `[1, null]`. Se revisa antes que la cantidad.
  - "Debe seleccionar entre 2 y 20 artículos; se recibieron <n>". Una lista nula cuenta como 0.
  - "Los identificadores de artículo no deben repetirse: <ids>".
  - "Artículos inexistentes o no seleccionables: <ids>".
  - "Falta el parámetro <nombre>".
  - "El cuerpo debe ser un JSON con articleIds: lista de enteros".
  - El handler existente cubre `a` o `b` no numéricos y un algoritmo desconocido.

Pruebas (Gradle):
- `./gradlew test` corre todo menos las etiquetas `student` y `scaffold`.
- `./gradlew studentTest` corre los tests de algoritmos y oráculos.
- `./gradlew scaffoldTest` corre `StubScaffoldTest`.
- Las tres tareas declaran la entrada `scannedSources` (ver §5, `build.gradle.kts`): cambiar solo un Javadoc, un `.md` de `docs/` o un archivo de `web/src` deja la tarea desactualizada y los tests se ejecutan sin `--rerun`.
- `ForbiddenSimilarityUsageTest` recorre `src/main/java` y `src/main/resources` sin comentarios y falla ante:
  - Imports de `org.apache.commons.text`, `org.apache.lucene`, `smile.`, `org.apache.commons.math3` e `info.debatty`.
  - En cadenas y SQL: `levenshtein(`, `similarity(`, `strpos(`, `position(`, ` like `, `ilike`, `tsvector`, `tsquery`, `pg_trgm`, `fuzzystrmatch`, `<=>`, `<->` y `<#>`.
  - Nombres de métodos de repositorio que terminen en `Like` o `Containing`.
  - Las coordenadas `commons-text`, `lucene`, `smile` y `commons-math` en `build.gradle.kts`.

JMH (`SimilarityBenchmark`): `@State(Scope.Benchmark)` y las mismas anotaciones de `SortingBenchmark`. `@Setup(Level.Trial)` arma, con `java.util.Random(42)`, dos secuencias de 200 tokens de un alfabeto de 50, un corpus de 20 secuencias y dos vectores de 768 normalizados con L2. Los métodos `levenshtein()`, `needlemanWunsch()`, `jaccard()`, `tfIdfCosine()`, `cosine()` y `euclidean()` devuelven el `SimilarityResult`. Se ejecuta con `java -jar build/libs/benchmarks.jar SimilarityBenchmark`.

Frontend:
- `useArticles()`: GET `/api/articles`.
- `useComparison()`: mutación POST `/api/similarity/comparisons`.
- `usePairTrace(algorithm, a, b)`: GET `/api/similarity/trace`, habilitado cuando los tres valores están definidos.
- `ArticleSelector`: lista MUI con casillas y título. El botón "Comparar" se habilita con 2 a 20 artículos elegidos, y el `ApiError` se muestra en un `Alert`.
- `Heatmap({ labels, matrix, onSelectCell })`:
  - `<table>` N×N con encabezados `#id`.
  - Cada celda muestra el valor con 3 decimales y tiene fondo `hsl(210, 80%, <95 − 55·v>%)`.
  - `aria-label` "fila i, columna j: v".
- `AlgorithmResultCard`: con CALCULADO muestra el `Heatmap` y "Ejecución: <elapsedMs> ms"; con PENDIENTE, "Pendiente de implementar: <pendingLocation>".
- `SimilarityResults`: "Preprocesamiento: <preprocessingMs> ms" y las 8 tarjetas en el orden recibido.
- `PagedMatrix({ matrix, rowLabels, colLabels })`: bloques de 25×25 con navegación y el texto "filas i–j, columnas k–l de R×C".
- `SimilarityTraceView`: selector de algoritmo y par. Muestra la vista de traza del algoritmo o el aviso de pendiente.
- Pasos de cada vista, todos con `TraceStep`, que lleva la fórmula en `react-katex`:
  - Levenshtein: secuencias, matriz D con `PagedMatrix`, distancia, similitud.
  - Needleman-Wunsch: secuencias y puntajes, matriz F, movimientos, alineamiento, puntaje, min/max, similitud.
  - Jaccard: conjuntos, intersección, unión, J.
  - TF-IDF: vocabulario, df e idf, tf, pesos, producto punto y normas, coseno.
  - Coseno: productos con `PagedArray`, producto punto, normas, coseno, similitud.
  - Euclidiana: diferencias al cuadrado con `PagedArray`, suma, d, similitud.

## 8. Traceability
| Requirement | Component / file | Verified by (AC) |
|---|---|---|
| REQ-1 | 6 stubs de `algorithms/similarity`, `SimilarityResult`, contratos y trazas; `StubScaffoldTest` | AC-1 |
| REQ-2 | Javadoc de los 6 stubs; `StubDocumentationTest` | AC-3, AC-16 |
| REQ-3 | `SequenceSimilarity` sobre `List<String>`, `EditGranularity`, `BibliomtrProperties.Similarity`; `StubDocumentationTest` | AC-3 |
| REQ-4 | stubs sin lógica; `ForbiddenSimilarityUsageTest`, `StubScaffoldTest` | AC-1, AC-2 |
| REQ-5 | `LevenshteinTest`, `NeedlemanWunschTest`, `JaccardTest`, `TfIdfCosineSimilarityTest`, `CosineSimilarityTest`, `EuclideanDistanceTest` | AC-6, AC-7 |
| REQ-6 | `oracle/LevenshteinOracleTest`, `oracle/VectorMetricsOracleTest` | AC-6 |
| REQ-7 | `build.gradle.kts` (`test`, `studentTest`, `scaffoldTest`, entrada `scannedSources`), `@Tag` | AC-4, AC-5 |
| REQ-8 | `CorpusIngestionRunner`, `CorpusIngestionService`, `PdfArticleExtractor`, `Article`, V3; `CorpusIngestionIT`, `PdfArticleExtractorTest` | AC-8, AC-10, AC-11 |
| REQ-9 | `Specter2OnnxEmbedder`, `Specter2Input`, `VectorNormalization`, `NomicOllamaEmbedder`, `SimilarityConfig` (`ollamaPullRestClient`), `bibliomtr.ollama.pull-timeout`; `Specter2InputTest`, `VectorNormalizationTest`, `NomicOllamaEmbedderTest`, `CorpusIngestionIT` | AC-8 |
| REQ-9a | `Specter2OnnxEmbedder.encode`, `specter2_truncated`, log WARN; `Specter2OnnxEmbedderTest`, `CorpusIngestionIT` | AC-8a |
| REQ-10 | `AbstractLocator`, `abstract_detected`, `findAllByAbstractDetectedTrue…`; `AbstractLocatorTest`, `CorpusIngestionIT` | AC-9 |
| REQ-11 | `.gitignore` (`/corpus/*`, ya existente); verificación con `git check-ignore` | — |
| REQ-12 | `TextPreprocessor`, `stopwords-en.txt`; `TextPreprocessorTest` | AC-12 |
| REQ-13 | `ArticleController`, `ComparisonController`; `ComparisonApiIT`, `CorpusIngestionIT` | AC-13, AC-14 |
| REQ-14 | `ComparisonService`, `ComparisonAlgorithm`, `AlgorithmMatrix`; `ComparisonApiIT` | AC-13 |
| REQ-15 | `ArticleSelectionValidator`, `InvalidSelectionException`, `ApiExceptionHandler`; `ComparisonApiIT` | AC-14 |
| REQ-16 | `ComparisonService.trace`, `PairTraceResponse`; `ComparisonApiIT` | AC-15 |
| REQ-17 | `ArticleSelector`, `Heatmap`, `SimilarityResults`, `SimilarityTraceView`, vistas de traza, `PagedMatrix`; sus tests | AC-13, AC-15 |
| REQ-18 | `docs/guia-implementacion.md`; `ImplementationGuideTest` | AC-7, AC-16 |
| REQ-19 | `docs/traceability.md`; `ImplementationGuideTest` | AC-16 |
| REQ-20 | `ComparisonService` (detección de `TODO estudiante:`), `AlgorithmStatus`; `ComparisonApiIT` | AC-17 |
| REQ-21 | `AlgorithmResultCard`, `SimilarityTraceView`; `SimilarityResults.test.tsx` | AC-18 |
| REQ-22 | `ComparisonService` con `Stopwatch`, `ComparisonResponse`; `ComparisonApiIT`, `SimilarityResults.test.tsx` | AC-19 |
| REQ-23 | `SimilarityBenchmark`, comando del benchmark en `README.md`; `./gradlew build jmhJar` | AC-20 |

Tests por AC (given - when - then de la spec):

| AC | Test (archivo) | Comprobación |
|---|---|---|
| AC-1 | `StubScaffoldTest` (`scaffold`) | cada uno de los 6 stubs lanza `UnsupportedOperationException` con mensaje que empieza por "TODO estudiante:" y su Javadoc contiene "Complejidad: TODO estudiante" |
| AC-2 | `ForbiddenSimilarityUsageTest` | ningún import, SQL, método derivado ni dependencia prohibida en el código de producción |
| AC-3 | `StubDocumentationTest` | el Javadoc de Levenshtein y Needleman-Wunsch tiene "Granularidad" y `bibliomtr.similarity.edit-granularity`; los 6 tienen Qué calcula, Fórmula, Normalización sugerida, Traza, Rango, Casos de borde y "Complejidad:" (sin exigir "TODO estudiante", para que siga en verde después de implementar) |
| AC-4 | `./gradlew test` | verde con los stubs sin implementar |
| AC-5 | `./gradlew studentTest` | todos los tests `student` fallan con los stubs |
| AC-6 | los 6 `*Test` de algoritmos y los 2 oráculos | bordes: vacías (Levenshtein [] vs [] → 0 y sim 1; [] vs [a,b,c] → 3 y sim 0), idénticas → sim 1, disjuntas ([a,b,c] vs [x,y,z]: Levenshtein 3 y sim 0; NW −3 y sim 0; Jaccard 0), longitudes muy distintas ([a] vs 50×a: Levenshtein 49 y sim 0.02; NW [a,b] vs [a, 9×x]: score −8, maxScore −6, minScore −10 y sim 0.5; Jaccard [a] vs [a..j] = 0.1); coseno opuestos (1,0),(−1,0) → −1 y sim 0; oráculos: `levenshtein()` de Postgres sobre cadenas de longitud ≤ 50 del alfabeto a–e con `Random(42)`, y `1 − (a <=> b)` y `a <-> b` sobre vectores de 768 con `Random(42)`, tolerancia 1e-4 |
| AC-7 | `LevenshteinTest`, `NeedlemanWunschTest`, `JaccardTest`, `TfIdfCosineSimilarityTest`, `CosineSimilarityTest`, `EuclideanDistanceTest` | g,a,t,o vs p,a,t,o: Levenshtein 1, sim 0.75 y matriz 5×5; NW score 2, maxScore 4, minScore −4 y sim 0.75; Jaccard {a,b,c} vs {b,c,d} = 0.5; TF-IDF con corpus [[a,b],[b,c],[c,d]] entre d1 y d2 ≈ 0.244830; coseno (1,0),(1,1) = 0.707107 y sim 0.853553; euclidiana (0,0),(3,4) = 5 y sim 1/6; los mismos valores que la tabla de la guía |
| AC-8 | `CorpusIngestionIT`, `PdfArticleExtractorTest`, `NomicOllamaEmbedderTest`, `Specter2InputTest` | 2 PDF sintéticos con título, autores y abstract → 2 filas con título, autores, abstract y dos vectores de 768. Título de metadatos válido → se conserva; con `_` o de 2 palabras → nombre de archivo sin prefijo. Autores "Ana Gómez and Luis Pérez; Eva Ruiz, Juan Díaz" → 4 autores; Ollama recibe `clustering: <abstract>` y SPECTER2 `título [SEP] abstract`; `/api/pull` va solo por `pullRestClient` y `/api/embed` solo por `restClient` |
| AC-8a | `Specter2OnnxEmbedderTest` (Assumptions), `CorpusIngestionIT` | texto de más de 512 tokens → `encode` da 512 ids que terminan en [SEP], `truncated = true` y ‖v‖₂ = 1 ± 1e-5; con el fake truncado → `specter2_truncated = true` y WARN en el log |
| AC-9 | `AbstractLocatorTest`, `PdfArticleExtractorTest`, `CorpusIngestionIT` | PDF sin encabezado Abstract → `abstract_detected = false` y ausente de `GET /api/articles`. Un caso sintético por regla del abstract: (1) "A R T I C L E I N F O A B S T R A C T" a mitad de línea; (2) encabezado, "Keywords:" y líneas cortas antes del abstract, que no entran en él; (3) "K E Y W O R D S" y "1 \| INTRODUC TION" como terminadores; (4) "… learning. 1. Introduction" al final de una línea; (5) sin terminador, corte en `¶`, y en `PdfArticleExtractorTest` un PDF con un salto de párrafo vertical; (6) resultado de menos de 100 caracteres → vacío |
| AC-10 | `CorpusIngestionIT` | directorio inexistente y directorio vacío → arranca y `GET /api/articles` = [] |
| AC-11 | `CorpusIngestionIT` | dos arranques sobre la misma base → mismo número de filas; un fake de nomic que falla en el primer arranque → ERROR en el log, sin fila, y la fila aparece en el segundo arranque; 2 PDF con `fake-embedders.nomic-pull-failures=1` → `ensureModel()` llamado 2 veces en la corrida, 1 fila, 1 ERROR, y el PDF fallido se guarda en el segundo arranque |
| AC-12 | `TextPreprocessorTest` | "Machine-Learning in Educación, 2023: the AI models!" → [machine, learning, educacion, ai, models], igual en dos llamadas y en dos instancias |
| AC-13 | `ComparisonApiIT`, `SimilarityResults.test.tsx` | 3 artículos y catálogo falso implementado → 8 resultados CALCULADO con matrices 3×3 simétricas en [0,1]; un fake que devuelve 1 + 1e-12 → CALCULADO con 1.0; un fake que devuelve 1.5 → 500; la vista dibuja 8 mapas de calor 3×3 |
| AC-14 | `ComparisonApiIT` | 1 id, 21 ids, ids repetidos y un id inexistente → 400 con los mensajes en español; sin cuerpo → 400; `[1, null]` → 400 "El cuerpo debe ser un JSON con articleIds: lista de enteros" |
| AC-15 | `ComparisonApiIT`, `SimilarityTraceView.test.tsx` | par y algoritmo implementado (fake) → 200 con `value`, `similarity` y `trace`; la vista muestra los pasos de Levenshtein para gato/pato en orden con fórmula KaTeX |
| AC-16 | `ImplementationGuideTest` | la guía tiene, por cada algoritmo, el archivo, `compare`, Fórmula, Traza y `./gradlew studentTest --tests <Clase>Test`; además, la sección "Uso de IA generativa" y una fila por módulo en `docs/traceability.md` |
| AC-17 | `ComparisonApiIT` | catálogo con solo Jaccard implementado → Jaccard CALCULADO con matriz; los otros 7 resultados (5 algoritmos) PENDIENTE con `pendingLocation` |
| AC-18 | `SimilarityResults.test.tsx` | resultado PENDIENTE → "Pendiente de implementar: src/main/java/…/Levenshtein.java#compare" |
| AC-19 | `ComparisonApiIT`, `SimilarityResults.test.tsx` | `preprocessingMs` ≥ 0 y `elapsedMs` ≥ 0 por algoritmo calculado; la vista muestra ambos en ms |
| AC-20 | `./gradlew build jmhJar` | compila `SimilarityBenchmark` con los stubs sin implementar; `build` no ejecuta JMH |

Comandos: `./gradlew test`, `./gradlew studentTest`, `./gradlew scaffoldTest` y `npm test` en `web/`.

## 9. Decisions and risks
- La normalización a [0,1] la escribe el estudiante dentro del stub (`SimilarityResult.similarity`). Se rechazó un `SimilarityNormalizer` del andamiaje porque contendría parte de la definición del algoritmo (P1, REQ-4).
- La granularidad por defecto es WORDS en `bibliomtr.similarity.edit-granularity`. Se rechazó CHARACTERS por defecto porque da matrices de hasta 4 millones de celdas y trazas de varios MB, y se rechazó una constante en código porque cambiarla exigiría recompilar.
- El IDF se calcula sobre todos los abstracts seleccionables, para que el valor de un par no dependa de la selección. Se rechazó el IDF sobre los N elegidos porque con N = 2 todo término compartido tiene idf 0.
- Las etiquetas `student` y `scaffold` con tareas Gradle propias mantienen verde `./gradlew test` antes y después de implementar. Se rechazó un source set aparte porque exige más configuración sin ganar separación.
- Needleman-Wunsch usa (1, −1, −1) fijo en `NeedlemanWunschScoring.DEFAULT`. Se rechazaron puntajes configurables por propiedad porque romperían los valores esperados de los tests.
- Si un embedding falla, el artículo no se guarda y se reintenta en el siguiente arranque. Se rechazó fallar el arranque, porque bloquearía también los clásicos, y guardar sin vector, porque exigiría un estado que la spec no define.
- Los embeddings se prueban con fakes, `MockRestServiceServer` y SPECTER2 real bajo Assumptions. Se rechazó Testcontainers con Ollama porque descarga unos 300 MB por corrida y requiere red.
- Los stubs siempre devuelven traza, sin bandera `recordTrace`. La traza (matriz y conjuntos) es parte del cálculo y no cambia la complejidad teórica; se rechazó la bandera de la 002 por simplicidad para los estudiantes.
- Se calculan los pares i ≤ j y se reflejan, porque los 6 algoritmos son simétricos. La diagonal se calcula y no se fija en 1, para no meter lógica de los algoritmos en el andamiaje.
- El mapa de calor es una tabla HTML propia. Se rechazó D3 porque AGENTS.md lo reserva al dendrograma, y Recharts porque no tiene heatmap.
- Una excepción de un stub distinta de `TODO estudiante:` se propaga como 500. La spec solo define PENDIENTE.
- Risk: la heurística del abstract puede fallar en alguno de los 20 PDF reales. La primera versión acertó en 15 de 20: #5, #6 y #7 quedaron sin abstract, y #9 y #14 tomaron un texto que no es el abstract, lo que viola P5. Las reglas 1 a 5 del abstract cubren esos 5 casos, y la regla del título descarta metadatos con `_` o de menos de 3 palabras - al implementar se vuelve a pasar la extracción por los 20 PDF y se revisa `GET /api/articles`. Un artículo que aun así falle queda marcado según REQ-10.
- Risk: `ollama/ollama:0.12.6` puede no publicar el endpoint `/api/embed` con este contrato - al implementar se verifica con `curl` y, si no, se fija explícitamente otra versión estable en ambos compose.
- Risk: `model.onnx` de SPECTER2 pesa unos 440 MB y se carga en memoria desde el classpath - la carga es perezosa (solo si hay PDF por ingerir) y `Specter2OnnxEmbedder` usa la ruta en disco cuando `Resource.isFile()`.
- Risk: código nuevo que use `TreeMap` u ordenamientos haría fallar `ForbiddenSortUsageTest` de la 002 - el preprocesamiento usa `HashSet` y `LinkedHashSet`, y el orden de los artículos lo da `ORDER BY file_name` en el repositorio.
- Risk: los IT de la 002 y `BibliomtrApplicationTests` arrancan el contexto completo - `src/test/resources/config/application.properties` desactiva la ingesta en todos los tests y solo `CorpusIngestionIT` la activa con `--bibliomtr.ingestion.enabled=true`.
- Risk: `./gradlew jmh` también ejecuta `SortingBenchmark`, que exige `inputFile`, y falla - la guía y `README.md` indican `./gradlew jmhJar && java -jar build/libs/benchmarks.jar SimilarityBenchmark`; `README.md` ya no recomienda `./gradlew jmh`.
- Risk: Gradle no registra como entradas de test los archivos fuente que leen `StubDocumentationTest`, `StubScaffoldTest`, `ForbiddenSortUsageTest`, `ForbiddenSimilarityUsageTest` e `ImplementationGuideTest`; un cambio que solo toca Javadoc o `docs/` no cambia los `.class` y la tarea queda UP-TO-DATE sin volver a verificar - `tasks.withType<Test>` declara `scannedSources` con `PathSensitivity.RELATIVE` y `models/` excluido (el modelo ONNX pesa ~440 MB y no lo lee ningún test de análisis). Se rechazó `outputs.upToDateWhen { false }` porque obliga a correr toda la suite en cada build.
- Risk: `POST /api/pull` con el límite de lectura de 60 s se corta mientras Ollama baja `nomic-embed-text` (~274 MB), y si el pull se marcara como hecho antes de llamarlo, los demás PDF de la corrida fallarían en `/api/embed` sin reintentar el pull - el pull usa su propio cliente con `bibliomtr.ollama.pull-timeout` (15m) y se marca como hecho solo si termina bien; si falla, el siguiente PDF lo reintenta. Se rechazó subir `bibliomtr.ollama.timeout` a 15 min porque también retrasaría la detección de un `/api/embed` colgado.
- Risk: `levenshtein()` de `fuzzystrmatch` admite como máximo 255 caracteres - el oráculo usa cadenas de longitud ≤ 50 y crea la extensión solo en el contenedor del test.
