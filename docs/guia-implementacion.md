# Guía de implementación de los algoritmos

Cubre el Requerimiento 1, similitud textual (secciones 1 a 9), y el Requerimiento 3, clustering jerárquico (sección 10). Trazabilidad: [`docs/traceability.md`](traceability.md).

Requerimiento 1. Spec: [`docs/specs/001-similitud-textual/spec.md`](specs/001-similitud-textual/spec.md). Plan: [`plan.md`](specs/001-similitud-textual/plan.md).

Esta guía dice exactamente qué y dónde implementar. El andamiaje (ingesta de PDF, preprocesamiento, embeddings, API, interfaz y tests) ya está hecho. A los estudiantes les toca escribir a mano el cuerpo del método `compare` de 6 clases y declarar su complejidad (constitución P1, P3 y P8).

## 1. Flujo de trabajo

1. Abrir el stub del algoritmo en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/`. Su Javadoc trae las mismas secciones de esta guía: Qué calcula, Fórmula, Normalización sugerida, Traza, Rango, Casos de borde, Complejidad y, en las distancias de edición, Granularidad.
2. Reemplazar el `throw new UnsupportedOperationException("TODO estudiante: …")` por la implementación. Solo se permiten estructuras básicas de Java: arreglos, `List`, `HashMap`, `HashSet`, `LinkedHashSet` y ciclos. No se permiten Commons Text, Lucene, Smile, Commons Math ni funciones de la base de datos (ver la sección 4).
3. Devolver un `SimilarityResult(value, similarity, trace)`. `value` es el valor crudo del algoritmo, `similarity` la similitud normalizada a [0,1] y `trace` el record de traza con los pasos intermedios (constitución P7).
4. Reemplazar `Complejidad: TODO estudiante` en el Javadoc de la clase y del método por la complejidad en tiempo y espacio, con su justificación. Conservar la palabra `Complejidad:`, que la revisa `StubDocumentationTest`.
5. Ejecutar los tests del algoritmo con `./gradlew studentTest --tests <Clase>Test` hasta que pasen. Después, ejecutar `./gradlew test` y comprobar que la suite estándar sigue en verde.
6. Revisar el resultado en la interfaz: pestaña "Similitud textual", mapa de calor del algoritmo y visor de traza paso a paso.

Mientras un método lance la excepción con el prefijo `TODO estudiante:`, la API devuelve ese algoritmo con estado PENDIENTE y la interfaz muestra "Pendiente de implementar: <archivo>#compare". Los demás algoritmos se calculan con normalidad.

## 2. Resumen

| Algoritmo | Archivo | Método | Traza | Test |
|---|---|---|---|---|
| Levenshtein | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Levenshtein.java` | `compare(List<String> a, List<String> b)` | `LevenshteinTrace` | `./gradlew studentTest --tests LevenshteinTest` |
| Needleman-Wunsch | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/NeedlemanWunsch.java` | `compare(List<String> a, List<String> b)` | `NeedlemanWunschTrace` | `./gradlew studentTest --tests NeedlemanWunschTest` |
| Jaccard | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Jaccard.java` | `compare(List<String> a, List<String> b)` | `JaccardTrace` | `./gradlew studentTest --tests JaccardTest` |
| Coseno TF-IDF | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/TfIdfCosineSimilarity.java` | `compare(List<String> a, List<String> b, List<List<String>> corpus)` | `TfIdfCosineTrace` | `./gradlew studentTest --tests TfIdfCosineSimilarityTest` |
| Similitud coseno (embeddings) | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/CosineSimilarity.java` | `compare(float[] a, float[] b)` | `CosineTrace` | `./gradlew studentTest --tests CosineSimilarityTest` |
| Distancia euclidiana (embeddings) | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/EuclideanDistance.java` | `compare(float[] a, float[] b)` | `EuclideanTrace` | `./gradlew studentTest --tests EuclideanDistanceTest` |

Los records de traza están en `src/main/java/co/edu/uniquindio/bibliomtr/trace/similarity/`. Coseno y euclidiana se aplican a los vectores de los dos modelos, SPECTER2 y nomic-embed-text, así que la API devuelve 8 resultados por comparación: 4 clásicos y 2 modelos × 2 métricas.

## 3. Contexto común

### Entradas
- Los algoritmos clásicos reciben los tokens del abstract que produce `TextPreprocessor` (`src/main/java/co/edu/uniquindio/bibliomtr/preprocessing/`). El preprocesamiento es determinista (constitución P12): minúsculas con `Locale.ROOT`, descomposición NFD sin tildes, todo carácter fuera de `[a-z0-9]` pasa a espacio, y se descartan los tokens de menos de 2 caracteres, los que solo tienen dígitos y las stopwords de `stopwords-en.txt` (lista inglesa de NLTK, 179 palabras). Por ejemplo, "Machine-Learning in Educación, 2023: the AI models!" da `[machine, learning, educacion, ai, models]`.
- Levenshtein y Needleman-Wunsch reciben palabras o caracteres según la granularidad configurada (ver la sección 5).
- El corpus de TF-IDF son los tokens de todos los abstracts seleccionables, no solo de los artículos elegidos.
- Coseno y euclidiana reciben los embeddings de 768 dimensiones guardados en la ingesta. Los de SPECTER2 ya vienen normalizados con L2.

### Cómo los usa la API
- `ComparisonService` recorre los pares i ≤ j de los artículos elegidos y refleja cada valor en (j, i), porque los 6 algoritmos son simétricos. La diagonal también se calcula.
- La similitud devuelta debe quedar en [0,1]. Un valor en [−1e-9, 1 + 1e-9] se recorta a [0,1] (`ComparisonService.SIMILARITY_TOLERANCE`) para absorber errores de redondeo. Un NaN o un valor fuera de ese margen responde HTTP 500.
- Cualquier otra excepción del algoritmo también responde 500. Una vez implementado, el método no debe lanzar mensajes con el prefijo `TODO estudiante:`.
- La traza se devuelve como JSON en `GET /api/similarity/trace?algorithm=<slug>&a=<id>&b=<id>`, y la interfaz la muestra paso a paso con las fórmulas en KaTeX (`web/src/features/similarity-trace/`).

## 4. Prohibiciones

`ForbiddenSimilarityUsageTest`, que está en `./gradlew test`, recorre `src/main` y falla ante:
- imports de `org.apache.commons.text`, `org.apache.lucene`, `smile.`, `org.apache.commons.math3` o `info.debatty`;
- SQL o cadenas con `levenshtein(`, `similarity(`, `strpos(`, `position(`, `like`, `ilike`, `tsvector`, `tsquery`, `pg_trgm`, `fuzzystrmatch` o los operadores `<=>`, `<->` y `<#>` de pgvector;
- métodos de repositorio que terminen en `Like` o `Containing`;
- dependencias `commons-text`, `lucene`, `smile` o `commons-math` en `build.gradle.kts`.

Además, `ForbiddenSortUsageTest` (spec 002) prohíbe `TreeMap`, `TreeSet`, `PriorityQueue` y los ordenamientos de librería en todo el código de producción.

## 5. Algoritmos

Notación: m = |a| y n = |b|.

### 5.1 Levenshtein — `Levenshtein`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Levenshtein.java`
- **Método:** `compare(List<String> a, List<String> b)`, de `SequenceSimilarity<LevenshteinTrace>`.
- **Qué calcula:** el número mínimo de inserciones, eliminaciones y sustituciones de un símbolo, cada una de costo 1, que transforman `a` en `b`.

**Fórmula.** Matriz D de (m + 1) × (n + 1):

```
D[i][0] = i
D[0][j] = j
D[i][j] = min( D[i−1][j] + 1,  D[i][j−1] + 1,  D[i−1][j−1] + [aᵢ ≠ bⱼ] )
value   = D[m][n]
```

**Normalización sugerida.** `similarity = 1 − D[m][n] / max(m, n)`, y 1 si m = n = 0.

**Traza esperada.** `LevenshteinTrace(a, b, matrix, distance, maxLength)`: las dos secuencias, la matriz D completa, la distancia D[m][n] y max(m, n).

**Casos a mano** (`LevenshteinTest`):

| Entrada | value | similarity | Otros |
|---|---|---|---|
| g,a,t,o vs p,a,t,o | 1 | 0.75 | matriz D de 5×5 (abajo), maxLength 4 |
| [] vs [] | 0 | 1 | matriz `[[0]]` |
| [] vs [a,b,c], en ambos órdenes | 3 | 0 | matriz `[[0, 1, 2, 3]]` |
| idénticas [machine, learning, education] | 0 | 1 | |
| disjuntas [a,b,c] vs [x,y,z] | 3 | 0 | |
| longitudes muy distintas [a] vs 50 × a | 49 | 0.02 | maxLength 50 |

Matriz D de g,a,t,o (filas) contra p,a,t,o (columnas):

|   | ε | p | a | t | o |
|---|---|---|---|---|---|
| ε | 0 | 1 | 2 | 3 | 4 |
| g | 1 | 1 | 2 | 3 | 4 |
| a | 2 | 2 | 1 | 2 | 3 |
| t | 3 | 3 | 2 | 1 | 2 |
| o | 4 | 4 | 3 | 2 | 1 |

**Oráculo:** `./gradlew studentTest --tests LevenshteinOracleTest` compara contra `levenshtein()` de `fuzzystrmatch` en un Postgres de Testcontainers, con 200 pares de cadenas de longitud 0 a 50 sobre el alfabeto a–e generadas con `Random(42)`. Requiere Docker.

**Tests:** `./gradlew studentTest --tests LevenshteinTest`

**Vista:** `web/src/features/similarity-trace/LevenshteinTraceView.tsx`, con los pasos secuencias, matriz D, distancia y similitud.

**Decisiones pendientes:** granularidad (sección 6.1) y complejidad en tiempo y espacio en función de m y n (sección 6.2).

### 5.2 Needleman-Wunsch — `NeedlemanWunsch`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/NeedlemanWunsch.java`
- **Método:** `compare(List<String> a, List<String> b)`, de `SequenceSimilarity<NeedlemanWunschTrace>`. El constructor recibe `NeedlemanWunschScoring`, y la aplicación usa `NeedlemanWunschScoring.DEFAULT` = (match = 1, mismatch = −1, gap = −1).
- **Qué calcula:** el puntaje del mejor alineamiento global de `a` y `b` y el alineamiento que lo alcanza.

**Fórmula.** Con g = gap y s(x, y) = match si x = y, mismatch si x ≠ y. Matriz F de (m + 1) × (n + 1):

```
F[i][0] = i · g
F[0][j] = j · g
F[i][j] = max( F[i−1][j−1] + s(aᵢ, bⱼ),   (DIAG)
               F[i−1][j] + g,             (UP)
               F[i][j−1] + g )            (LEFT)
value   = score = F[m][n]
```

El alineamiento se reconstruye desde [m][n] siguiendo los movimientos hasta [0][0].

**Normalización sugerida** con (1, −1, −1):
- `maxScore = 2 · min(m, n) − max(m, n)`: min(m, n) coincidencias y max(m, n) − min(m, n) gaps.
- `minScore = −max(m, n)`: min(m, n) sustituciones más max(m, n) − min(m, n) gaps ya alcanzan −max(m, n), y el óptimo nunca queda por debajo.
- `similarity = (score − minScore) / (maxScore − minScore)`. Vale 1 si m = n = 0 y 0 si solo una secuencia es vacía, porque ahí maxScore = minScore.

**Traza esperada.** `NeedlemanWunschTrace(a, b, scoring, scores, moves, alignment, score, minScore, maxScore)`:
- las secuencias y el esquema de puntajes;
- la matriz F (`scores`) y la matriz de movimientos (`moves`), ambas de (m + 1) × (n + 1), con `"DIAG"`, `"UP"` o `"LEFT"`;
- el alineamiento como lista de `AlignedPair(fromA, fromB)`, con `null` en el lado del gap;
- `score`, `minScore` y `maxScore`.

El contrato no fija cómo desempatar movimientos con el mismo puntaje, y los tests no lo revisan.

**Casos a mano** (`NeedlemanWunschTest`, con (1, −1, −1)):

| Entrada | score (value) | maxScore | minScore | similarity |
|---|---|---|---|---|
| g,a,t,o vs p,a,t,o | 2 | 4 | −4 | 0.75 |
| disjuntas [a,b,c] vs [x,y,z] | −3 | 3 | −3 | 0 |
| [] vs [] | 0 | 0 | 0 | 1 |
| [] vs [a,b,c], en ambos órdenes | −3 | −3 | −3 | 0 |
| idénticas [a,b,c] vs [a,b,c] | 3 | 3 | −3 | 1 |
| longitudes muy distintas [a,b] vs [a, 9 × x] | −8 | −6 | −10 | 0.5 |

Matriz F de g,a,t,o (filas) contra p,a,t,o (columnas):

|   | ε | p | a | t | o |
|---|---|---|---|---|---|
| ε | 0 | −1 | −2 | −3 | −4 |
| g | −1 | −1 | −2 | −3 | −4 |
| a | −2 | −2 | 0 | −1 | −2 |
| t | −3 | −3 | −1 | 1 | 0 |
| o | −4 | −4 | −2 | 0 | 2 |

El alineamiento óptimo es (g, p), (a, a), (t, t), (o, o): una sustitución y tres coincidencias, −1 + 3 = 2. En el caso [a,b] vs [a, 9 × x], el óptimo alinea (a, a) y (b, x) y deja 8 gaps: 1 − 1 − 8 = −8, y similarity = (−8 + 10) / (−6 + 10) = 0.5.

**Tests:** `./gradlew studentTest --tests NeedlemanWunschTest`

**Vista:** `web/src/features/similarity-trace/NeedlemanWunschTraceView.tsx`, con los pasos secuencias y puntajes, matriz F, movimientos, alineamiento, puntaje, min/max y similitud.

**Decisiones pendientes:** granularidad (sección 6.1) y complejidad en tiempo y espacio en función de m y n (sección 6.2).

### 5.3 Jaccard — `Jaccard`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/Jaccard.java`
- **Método:** `compare(List<String> a, List<String> b)`, de `SequenceSimilarity<JaccardTrace>`.
- **Qué calcula:** la proporción de símbolos distintos compartidos. Las repeticiones no cuentan, porque se comparan los conjuntos A = símbolos de `a` y B = símbolos de `b`.

**Fórmula.**

```
J(A, B) = |A ∩ B| / |A ∪ B|
```

**Normalización sugerida.** Ninguna: `value = similarity = J`, y J = 1 si A = B = ∅.

**Traza esperada.** `JaccardTrace(setA, setB, intersection, union)`, todas en orden de primera aparición. La unión lleva primero los de A y luego los nuevos de B.

**Casos a mano** (`JaccardTest`):

| Entrada | J | Traza |
|---|---|---|
| {a,b,c} vs {b,c,d} | 2/4 = 0.5 | intersección [b,c], unión [a,b,c,d] |
| duplicados [a,a,b] vs [a,b,b] | 1 | setA [a,b], setB [a,b] |
| [] vs [] | 1 | |
| [] vs [a,b] | 0 | |
| idénticas [graph, neural, network] | 1 | |
| disjuntas [a,b,c] vs [x,y,z] | 0 | intersección [], unión [a,b,c,x,y,z] |
| longitudes muy distintas [a] vs [a..j] | 1/10 = 0.1 | |

**Tests:** `./gradlew studentTest --tests JaccardTest`

**Vista:** `web/src/features/similarity-trace/JaccardTraceView.tsx`, con los pasos conjuntos, intersección, unión y J.

**Decisiones pendientes:** complejidad en tiempo y espacio en función de |a| y |b| (sección 6.2).

### 5.4 Coseno TF-IDF — `TfIdfCosineSimilarity`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/TfIdfCosineSimilarity.java`
- **Método:** `compare(List<String> a, List<String> b, List<List<String>> corpus)`, de `CorpusSimilarity<TfIdfCosineTrace>`.
- **Qué calcula:** el coseno entre los vectores de pesos tf · idf de los dos documentos, sobre el vocabulario del par (la unión de los términos de `a` y `b`). El IDF se calcula con `corpus`, que en la aplicación son todos los abstracts seleccionables, para que el valor de un par no dependa de qué otros artículos se eligieron.

**Fórmula.** Con N = |corpus|, f(t, d) = apariciones de t en d y df(t) = documentos del corpus que contienen t:

```
tf(t, d)  = f(t, d) / |d|
idf(t)    = ln(N / df(t))
w(t, d)   = tf(t, d) · idf(t)
cos(a, b) = Σₜ w(t, a) · w(t, b) / (‖w(·, a)‖₂ · ‖w(·, b)‖₂)
```

**Normalización sugerida.** Ninguna: los pesos son no negativos, así que `value = similarity = cos` ya está en [0, 1]. Vale 1 si `a` y `b` son vacías, y 0 si alguna norma es 0 en otro caso.

**Traza esperada.** `TfIdfCosineTrace(vocabulary, documentCount, documentFrequency, idf, tfA, tfB, weightsA, weightsB, dotProduct, normA, normB)`. Los arreglos van alineados con `vocabulary`, cuyo orden no está fijado: los tests buscan la posición de cada término.

**Casos a mano** (`TfIdfCosineSimilarityTest`). Corpus d1 = [a,b], d2 = [b,c], d3 = [c,d], así que N = 3, df(a) = 1, df(b) = 2 y df(c) = 2. Entre d1 y d2:

| Término | df | idf | tf(d1) | tf(d2) | w(d1) | w(d2) |
|---|---|---|---|---|---|---|
| a | 1 | ln 3 ≈ 1.098612 | 0.5 | 0 | 0.549306 | 0 |
| b | 2 | ln 1.5 ≈ 0.405465 | 0.5 | 0.5 | 0.202733 | 0.202733 |
| c | 2 | ln 1.5 ≈ 0.405465 | 0 | 0.5 | 0 | 0.202733 |

Producto punto 0.041100, ‖w(d1)‖ = 0.585523, ‖w(d2)‖ = 0.286707 y cos = 0.041100 / (0.585523 · 0.286707) ≈ 0.244830.

| Entrada | cos |
|---|---|
| d1 vs d2 | ≈ 0.244830 |
| idénticas, d1 vs d1 | 1 |
| disjuntas, d1 vs d3 | 0 |
| [] vs [], corpus [[], [a,b]] | 1 |
| [] vs [a,b], corpus [[], [a,b]] | 0 |
| longitudes muy distintas, [a,b] vs 5 × [a,b] | 1, porque tf divide por \|d\| |

Un término presente en todos los documentos tiene idf 0 y no aporta.

**Tests:** `./gradlew studentTest --tests TfIdfCosineSimilarityTest`

**Vista:** `web/src/features/similarity-trace/TfIdfCosineTraceView.tsx`, con los pasos vocabulario, df e idf, tf, pesos, producto punto y normas, y coseno.

**Decisiones pendientes:** complejidad en tiempo y espacio en función de |a|, |b|, N y el tamaño total del corpus (sección 6.2).

### 5.5 Similitud coseno entre embeddings — `CosineSimilarity`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/CosineSimilarity.java`
- **Método:** `compare(float[] a, float[] b)`, de `VectorSimilarity<CosineTrace>`. Se aplica a SPECTER2 (`specter2-cosine`) y a nomic-embed-text (`nomic-cosine`).
- **Qué calcula:** el coseno del ángulo entre dos vectores de dimensión d producidos por el mismo modelo.

**Fórmula.**

```
cos(a, b) = Σᵢ aᵢ bᵢ / (‖a‖₂ · ‖b‖₂),   con ‖v‖₂ = √(Σᵢ vᵢ²)
value     = cos
```

**Normalización sugerida.** `similarity = (1 + cos) / 2`.

**Traza esperada.** `CosineTrace(dimension, products, dotProduct, normA, normB)`: d, los productos aᵢ bᵢ, el producto punto y las dos normas.

**Casos a mano** (`CosineSimilarityTest`):

| Entrada | cos (value) | similarity | Traza |
|---|---|---|---|
| (1,0) vs (1,1) | 1/√2 ≈ 0.707107 | ≈ 0.853553 | products [1, 0], dotProduct 1, normA 1, normB √2 ≈ 1.414214 |
| idénticos (1,2,3) vs (1,2,3) | 1 | 1 | |
| opuestos (1,0) vs (−1,0) | −1 | 0 | |
| ortogonales (1,0) vs (0,1) | 0 | 0.5 | |
| dimensiones distintas o 0 | `IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0")` | | |

**Oráculo:** `./gradlew studentTest --tests VectorMetricsOracleTest` compara `1 − (a <=> b)` de pgvector con el coseno, sobre vectores de 768 dimensiones generados con `Random(42)` y tolerancia 1e-4. Requiere Docker.

**Tests:** `./gradlew studentTest --tests CosineSimilarityTest`

**Vista:** `web/src/features/similarity-trace/CosineTraceView.tsx`, con los pasos productos (paginados con `PagedArray`), producto punto, normas, coseno y similitud.

**Decisiones pendientes:** complejidad en tiempo y espacio en función de d (sección 6.2).

### 5.6 Distancia euclidiana entre embeddings — `EuclideanDistance`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/similarity/EuclideanDistance.java`
- **Método:** `compare(float[] a, float[] b)`, de `VectorSimilarity<EuclideanTrace>`. Se aplica a SPECTER2 (`specter2-euclidean`) y a nomic-embed-text (`nomic-euclidean`).
- **Qué calcula:** la longitud del segmento entre dos vectores de dimensión d producidos por el mismo modelo.

**Fórmula.**

```
d(a, b) = √( Σᵢ (aᵢ − bᵢ)² )
value   = d
```

**Normalización sugerida.** `similarity = 1 / (1 + d)`.

**Traza esperada.** `EuclideanTrace(dimension, squaredDifferences, sumOfSquares)`: d (la dimensión), las diferencias al cuadrado (aᵢ − bᵢ)² y su suma.

**Casos a mano** (`EuclideanDistanceTest`):

| Entrada | d (value) | similarity | Traza |
|---|---|---|---|
| (0,0) vs (3,4) | 5 | 1/6 ≈ 0.166667 | squaredDifferences [9, 16], sumOfSquares 25 |
| idénticos (1,2,3) vs (1,2,3) | 0 | 1 | |
| dimensiones distintas o 0 | `IllegalArgumentException("Los vectores deben tener la misma dimensión mayor que 0")` | | |

**Oráculo:** `./gradlew studentTest --tests VectorMetricsOracleTest` compara `a <-> b` de pgvector con la distancia, con las mismas entradas y tolerancia del coseno.

**Tests:** `./gradlew studentTest --tests EuclideanDistanceTest`

**Vista:** `web/src/features/similarity-trace/EuclideanTraceView.tsx`, con los pasos diferencias al cuadrado (paginadas con `PagedArray`), suma, d y similitud.

**Decisiones pendientes:** complejidad en tiempo y espacio en función de d (sección 6.2).

## 6. Decisiones pendientes

### 6.1 Granularidad de las distancias de edición
Levenshtein y Needleman-Wunsch reciben una secuencia genérica de símbolos, así que el algoritmo no cambia con la granularidad. Los estudiantes deciden si los símbolos son palabras o caracteres. La decisión se configura en `bibliomtr.similarity.edit-granularity` de `src/main/resources/application.properties`:
- `WORDS` (valor por defecto): los tokens de `TextPreprocessor.tokenize`.
- `CHARACTERS`: cada carácter de `TextPreprocessor.normalize`, espacios incluidos (`TextPreprocessor.characters`).

Con `CHARACTERS`, las matrices llegan a unos 4 millones de celdas y las trazas a varios MB. Por eso el valor por defecto es `WORDS`. La respuesta de comparación informa la granularidad usada en `editGranularity`. La decisión y su justificación deben quedar en el documento técnico. Los casos a mano de la sección 5 valen para cualquier granularidad, porque están escritos a nivel de secuencia de símbolos.

### 6.2 Complejidad
Cada stub deja `Complejidad: TODO estudiante` en el Javadoc de la clase y de `compare`. Se reemplaza por la complejidad en tiempo y espacio, en función de las variables que indica cada stub, con su justificación (constitución P8). La complejidad declarada se contrasta con las mediciones del benchmark de la sección 8 (constitución P11).

## 7. Comandos de prueba

| Comando | Qué ejecuta | Con los stubs sin implementar |
|---|---|---|
| `./gradlew test` | suite estándar, sin las etiquetas `student` ni `scaffold` | verde |
| `./gradlew studentTest` | tests de estudiante de los 6 algoritmos de similitud, los 2 oráculos y los 4 de clustering (sección 10) | todos fallan |
| `./gradlew studentTest --tests <Clase>Test` | tests de un algoritmo | fallan hasta implementarlo |
| `./gradlew scaffoldTest` | `StubScaffoldTest` y `ClusteringStubScaffoldTest`: los stubs siguen sin implementar | verde |

`scaffoldTest` describe el andamiaje recién entregado: deja de pasar para cada algoritmo que se implementa, y es lo esperado. `studentTest` y `scaffoldTest` no forman parte de `check`, así que `./gradlew build` no los ejecuta. Los oráculos y los IT usan Testcontainers y requieren Docker.

## 8. Benchmark

`src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/SimilarityBenchmark.java` tiene un `@Benchmark` por algoritmo: `levenshtein`, `needlemanWunsch`, `jaccard`, `tfIdfCosine`, `cosine` y `euclidean`. Las entradas son fijas y se generan con `Random(42)`: dos secuencias de 200 símbolos de un alfabeto de 50, un corpus de 20 secuencias y dos vectores de 768 normalizados con L2. Los parámetros son 1 fork, 5 iteraciones de calentamiento de 1 s, 10 de medición de 1 s y tiempo promedio en ms.

Una vez implementados los algoritmos:

```bash
./gradlew jmhJar
java -jar build/libs/benchmarks.jar SimilarityBenchmark                # los 6
java -jar build/libs/benchmarks.jar SimilarityBenchmark.levenshtein    # uno solo
```

No se usa `./gradlew jmh`, porque también ejecutaría `SortingBenchmark` (spec 002), que exige `inputFile`. Mientras un stub no esté implementado, su medición falla al ejecutarse, pero `./gradlew build jmhJar` sigue en verde. Además, cada comparación de la API reporta `preprocessingMs` y `elapsedMs` por algoritmo, por separado.

## 9. Uso de IA generativa

Declaración según la constitución P4:
- El andamiaje de este requerimiento se generó con apoyo de IA generativa (Claude, de Anthropic), dentro del flujo de especificación, plan y tareas de `docs/specs/001-similitud-textual/`. Eso incluye la ingesta de PDF, el preprocesamiento, la integración de embeddings, la persistencia, la API, la interfaz web, los contratos y stubs, los tests, el benchmark y esta guía. Las decisiones quedaron registradas y aprobadas en la spec y el plan.
- Los cuerpos de los 6 algoritmos (`compare` de `Levenshtein`, `NeedlemanWunsch`, `Jaccard`, `TfIdfCosineSimilarity`, `CosineSimilarity` y `EuclideanDistance`) y su análisis de complejidad no los genera la IA: los escriben a mano los estudiantes.
- Los valores esperados de los casos a mano se calcularon con las fórmulas de la sección 5 y pueden verificarse a mano. Los oráculos de Postgres y pgvector sirven de referencia independiente.
- El andamiaje del Requerimiento 3, clustering jerárquico (sección 10), también se generó con apoyo de IA generativa (Claude, de Anthropic), dentro del flujo de `docs/specs/003-clustering-jerarquico/`: contratos, stubs, validación del dendrograma, trazas, API, interfaz (dendrogramas con D3 y visor de traza), tests y esta sección. Los cuerpos de `cluster` de `SingleLinkage`, `CompleteLinkage`, `AverageLinkage` y `Diana` y su complejidad los escriben a mano los estudiantes. Los valores esperados del caso a mano D₄ se calcularon con las fórmulas de la sección 10 y pueden verificarse a mano.
- Si los estudiantes usan IA generativa como apoyo al implementar o analizar, deben declararlo en el documento técnico, indicando en qué y cómo. La IA no reemplaza el diseño algorítmico ni el análisis formal.

## 10. Requerimiento 3: Clustering jerárquico

Spec: [`docs/specs/003-clustering-jerarquico/spec.md`](specs/003-clustering-jerarquico/spec.md). Plan: [`plan.md`](specs/003-clustering-jerarquico/plan.md).

A los estudiantes les toca escribir a mano el cuerpo del método `cluster` de 3 algoritmos aglomerativos (Single, Complete y Average Linkage) y, de forma opcional, del divisivo DIANA, y declarar su complejidad. El resto ya está hecho: la matriz de distancias, la validación del dendrograma, la API, la sección "Clustering jerárquico" de la página de similitud textual (4 dendrogramas en cuadrícula 2×2 y visor de traza) y los tests.

### 10.1 Flujo de trabajo

1. Abrir el stub en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/`. Su Javadoc trae las secciones Qué calcula, Fórmula, Desempate, Traza, Rango, Caso a mano, Casos de borde y Complejidad.
2. Reemplazar el `throw new UnsupportedOperationException("TODO estudiante: …")` por la implementación, solo con arreglos, `List`, `HashMap`, `HashSet` y ciclos (secciones 4 y 10.3).
3. Devolver un `ClusteringResult(merges, trace)`: las N − 1 fusiones del dendrograma y el record de traza.
4. Reemplazar `Complejidad: TODO estudiante` en el Javadoc de la clase y de `cluster` por la complejidad en tiempo y espacio en función de N, con su justificación.
5. Ejecutar `./gradlew studentTest --tests <Clase>Test` hasta que pase; luego `./gradlew test`.
6. Revisar el resultado en la interfaz: pestaña "Similitud textual", comparar artículos y bajar a "Clustering jerárquico".

Mientras un método lance la excepción con el prefijo `TODO estudiante:`, la API devuelve ese algoritmo con estado PENDIENTE y la interfaz muestra "Pendiente de implementar: <archivo>#cluster". Si la fuente de similitud elegida (un stub del Requerimiento 1) tampoco está implementada, la sección lo avisa y no dibuja dendrogramas.

| Algoritmo | Archivo | Método | Traza | Test |
|---|---|---|---|---|
| Single Linkage | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/SingleLinkage.java` | `cluster(double[][] distances)` | `LinkageTrace` | `./gradlew studentTest --tests SingleLinkageTest` |
| Complete Linkage | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/CompleteLinkage.java` | `cluster(double[][] distances)` | `LinkageTrace` | `./gradlew studentTest --tests CompleteLinkageTest` |
| Average Linkage (UPGMA) | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/AverageLinkage.java` | `cluster(double[][] distances)` | `LinkageTrace` | `./gradlew studentTest --tests AverageLinkageTest` |
| DIANA (opcional) | `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/Diana.java` | `cluster(double[][] distances)` | `DianaTrace` | `./gradlew studentTest --tests DianaTest` |

Los records de traza están en `src/main/java/co/edu/uniquindio/bibliomtr/trace/clustering/`.

**Por qué estos algoritmos.** Single, Complete y Average cubren las tres definiciones clásicas de distancia entre grupos (mínima, máxima y promedio) con la misma estructura: una matriz de distancias que se actualiza con la fórmula de Lance-Williams, O(N³) en tiempo con la búsqueda directa del mínimo y O(N²) en espacio. Single tiende a encadenar grupos, Complete forma grupos compactos y Average (UPGMA) queda entre ambos y es el más usado con distancias de texto. Ward se descartó porque supone distancias euclidianas al cuadrado, y d = 1 − s no lo es en general. DIANA es el contraste top-down: cada división cuesta O(|C|²) por iteración, y es opcional.

### 10.2 Contexto común

**Entrada.** `distances` es una matriz N×N, con N de 2 a 20, simétrica, de diagonal 0 y valores en [0,1]. La arma `ClusteringService` a partir de la matriz de similitud de la fuente elegida del Requerimiento 1 (SPECTER2 coseno por defecto), sin reimplementar el preprocesamiento ni la similitud: d(i, j) = 1 − s(i, j) para i ≠ j y d(i, i) = 0. La fila i es el artículo i de la solicitud. Cada algoritmo recibe su propia copia.

**Salida: lista de fusiones estilo SciPy.** Las hojas son 0..N−1. La fusión k (0 ≤ k < N − 1) crea el grupo N + k. `Merge(left, right, height, size)` con `left < right`, `height` = distancia entre los dos grupos y `size` = artículos del grupo nuevo. La interfaz dibuja exactamente estas fusiones con D3, sin recalcular nada.

**Validación.** `DendrogramValidator` revisa que haya N − 1 fusiones, que cada fusión use hojas o grupos ya creados, que ningún grupo se fusione dos veces, que `size` sea la suma de los hijos y que la altura esté en [0,1] (se recorta un margen de 1e-9). Si algo falla, la API responde HTTP 500 con "El algoritmo <slug> devolvió un dendrograma inválido: <razón>". No revisa que las fusiones sean las correctas: eso lo hacen los tests `student`.

**API.** `POST /api/clustering` con `{"articleIds": [...], "source": "<slug>"}` devuelve las distancias, el tiempo de preparación y los 4 dendrogramas con su tiempo; `GET /api/clustering/trace?algorithm=<slug>&source=<slug>&articleIds=1,2,3` devuelve además la traza de uno.

**Caso a mano D₄** (el de todos los tests y la guía):

|   | 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| 0 | 0 | 0.2 | 0.5 | 0.7 |
| 1 | 0.2 | 0 | 0.3 | 0.6 |
| 2 | 0.5 | 0.3 | 0 | 0.45 |
| 3 | 0.7 | 0.6 | 0.45 | 0 |

**Casos de borde de los tests:** 2 artículos a distancia 0.35 dan la única fusión (0, 1, 0.35, 2); los abstracts idénticos 0 y 2 (d = 0) se fusionan a altura 0; con 4 artículos y todas las distancias en 0.5 se aplica la regla de desempate; con 20 artículos aleatorios (`Random(42)`) hay 19 fusiones válidas, la última de tamaño 20, y dos ejecuciones dan lo mismo.

### 10.3 Prohibiciones

`ForbiddenClusteringUsageTest`, en `./gradlew test`, falla si el código de producción importa librerías de clustering (Smile, Weka, el clustering de Commons Math, `com.apporiented`, Spark MLlib, Tribuo o JSAT), si `build.gradle.kts` las declara o si `web/` depende de paquetes como `ml-hclust`. D3 solo dibuja. `ForbiddenSortUsageTest` sigue prohibiendo `TreeMap`, `TreeSet`, `PriorityQueue` y los ordenamientos de librería.

### 10.4 Single Linkage — `SingleLinkage`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/SingleLinkage.java`
- **Método:** `cluster(double[][] distances)`, de `HierarchicalClustering<LinkageTrace>`.
- **Qué calcula:** el dendrograma aglomerativo en el que la distancia entre dos grupos es la mínima entre sus artículos.

**Fórmula.** En cada paso se fusionan los dos grupos activos más cercanos, i y j, en k = i ∪ j, y se recalcula su distancia a cada grupo h que queda (Lance-Williams):

```
d(k, h) = min( d(i, h), d(j, h) )
```

**Desempate.** Entre los pares (p, q), p < q, de distancia mínima gana el de menor p y, a igual p, el de menor q (orden lexicográfico de los ids de grupo). Se compara con `<` exacto, sin margen.

**Traza esperada.** `LinkageTrace(steps)`, un `LinkageStep` por fusión: `clusters` (ids de los grupos activos antes de fusionar: los que quedan conservan su orden y el nuevo va al final), `members` (artículos de cada grupo, en orden creciente), `distances` (matriz entre esos grupos), `left`, `right`, `distance`, `newCluster`, `remaining` (`clusters` sin `left` ni `right`) y `updatedRow` (d(newCluster, h) para cada h de `remaining`).

**Caso a mano** (D₄): fusiones (0, 1, 0.2, 2), (2, 4, 0.3, 3), (3, 5, 0.45, 4). Paso 1: `updatedRow` = [min(0.5, 0.3), min(0.7, 0.6)] = [0.3, 0.6] frente a `remaining` = [2, 3]. Paso 2: `clusters` = [2, 3, 4] y `updatedRow` = [0.45]. Con todas las distancias en 0.5: (0, 1), (2, 3), (4, 5).

**Tests:** `./gradlew studentTest --tests SingleLinkageTest`

**Vista:** `web/src/features/clustering/LinkageTraceView.tsx`, con la matriz entre grupos, el par elegido y la fila recalculada de cada fusión.

### 10.5 Complete Linkage — `CompleteLinkage`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/CompleteLinkage.java`
- **Método:** `cluster(double[][] distances)`, de `HierarchicalClustering<LinkageTrace>`.
- **Qué calcula:** el dendrograma aglomerativo en el que la distancia entre dos grupos es la máxima entre sus artículos.

**Fórmula.**

```
d(k, h) = max( d(i, h), d(j, h) )
```

**Desempate.** El mismo de Single Linkage: par (p, q) lexicográficamente menor, con `<` exacto.

**Traza esperada.** `LinkageTrace`, con los mismos campos de Single Linkage.

**Caso a mano** (D₄): fusiones (0, 1, 0.2, 2), (2, 3, 0.45, 2), (4, 5, 0.7, 4). Paso 1: `updatedRow` = [max(0.5, 0.3), max(0.7, 0.6)] = [0.5, 0.7]. Con todas las distancias en 0.5: (0, 1), (2, 3), (4, 5).

**Tests:** `./gradlew studentTest --tests CompleteLinkageTest`

**Vista:** `web/src/features/clustering/LinkageTraceView.tsx`.

### 10.6 Average Linkage (UPGMA) — `AverageLinkage`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/AverageLinkage.java`
- **Método:** `cluster(double[][] distances)`, de `HierarchicalClustering<LinkageTrace>`.
- **Qué calcula:** el dendrograma aglomerativo en el que la distancia entre dos grupos es el promedio de las distancias entre todos sus pares de artículos.

**Fórmula.** Con |i| y |j| el número de artículos de cada grupo:

```
d(k, h) = ( |i| · d(i, h) + |j| · d(j, h) ) / ( |i| + |j| )
```

**Desempate.** El mismo de Single Linkage: par (p, q) lexicográficamente menor, con `<` exacto.

**Traza esperada.** `LinkageTrace`, con los mismos campos de Single Linkage.

**Caso a mano** (D₄): fusiones (0, 1, 0.2, 2), (2, 4, 0.4, 3), (3, 5, 0.583333, 4). Paso 1: `updatedRow` = [(0.5 + 0.3)/2, (0.7 + 0.6)/2] = [0.4, 0.65]. Paso 2: d(5, 3) = (1 · 0.45 + 2 · 0.65)/3 = 1.75/3 = 0.583333. Con todas las distancias en 0.5: (0, 1), (2, 3), (4, 5).

**Tests:** `./gradlew studentTest --tests AverageLinkageTest`

**Vista:** `web/src/features/clustering/LinkageTraceView.tsx`.

### 10.7 DIANA (divisivo, opcional) — `Diana`

- **Archivo:** `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/Diana.java`
- **Método:** `cluster(double[][] distances)`, de `HierarchicalClustering<DianaTrace>`.
- **Qué calcula:** un dendrograma top-down. Empieza con un grupo con todos los artículos y divide en dos, siempre, el grupo de mayor diámetro, hasta que cada artículo queda solo. Puede implementarse recursivo o con una pila.

**Fórmula.**

```
diam(C) = max { d(a, b) : a, b ∈ C }
S₀      = { el i de C con mayor distancia media al resto de C },   R₀ = C \ S₀
D(i)    = media de d(i, R \ {i}) − media de d(i, S),   i ∈ R
mientras max D(i) > 0: mover a S el i de mayor D(i)
```

La división de C se registra a altura diam(C). Las fusiones se devuelven en el orden inverso de las divisiones (la última división es la fusión 0), con la misma convención de ids que los aglomerativos.

**Desempate.** Se divide el grupo de mayor diámetro (empate: el grupo cuyo menor artículo es menor). S₀ y cada movimiento eligen, a igual valor, el artículo de menor índice. Un artículo se mueve solo si D(i) > 0 estricto.

**Traza esperada.** `DianaTrace(steps)`, un `DianaStep` por división: `clusters` (grupos activos antes de dividir; el dividido se reemplaza en su posición por la parte con el menor artículo seguida de la otra), `diameters`, `splitIndex`, `diameter`, `iterations`, `splinter` y `remainder`. Cada `DianaIteration` trae `splinter`, `remainder`, `averageToRemainder`, `averageToSplinter`, `differences` (alineados con `remainder`) y `moved` (el artículo movido, o −1 al terminar). En la primera iteración `splinter` está vacío, `averageToSplinter` es 0 y `differences` = `averageToRemainder`.

**Caso a mano** (D₄):
1. División de {0,1,2,3} con diámetro 0.7: medias iniciales [0.466667, 0.366667, 0.416667, 0.583333], así que S = {3}. Luego D = [−0.35, −0.35, −0.05] y termina.
2. División de {0,1,2} con diámetro 0.5: S = {2}; luego D = [−0.3, −0.1].
3. División de {0,1} con diámetro 0.2: S = {0}.

Fusiones: (0, 1, 0.2, 2), (2, 4, 0.5, 3), (3, 5, 0.7, 4). Con todas las distancias en 0.5: (2, 3), (1, 4), (0, 5).

**Tests:** `./gradlew studentTest --tests DianaTest`

**Vista:** `web/src/features/clustering/DianaTraceView.tsx`, con los grupos y diámetros, las iteraciones del grupo disidente y las dos partes de cada división.

### 10.8 Comandos y tiempos

| Comando | Qué ejecuta | Con los stubs sin implementar |
|---|---|---|
| `./gradlew studentTest --tests '*LinkageTest' --tests DianaTest` | los tests de estudiante de clustering | todos fallan |
| `./gradlew scaffoldTest --tests ClusteringStubScaffoldTest` | los 4 stubs siguen sin implementar | verde |
| `./gradlew test` | incluye `DendrogramValidatorTest`, `ClusteringStubDocumentationTest`, `ForbiddenClusteringUsageTest` y `ClusteringApiIT` | verde |

Cada solicitud de clustering reporta `preparationMs` (preprocesamiento y matriz de similitud) y el `elapsedMs` de cada algoritmo, por separado. La complejidad declarada en el Javadoc se contrasta con esos tiempos en el documento técnico.
