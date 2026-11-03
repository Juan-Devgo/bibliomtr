# Plan: Entorno para el Requerimiento 3: Clustering jerárquico con dendrograma

## 1. Metadata
- Spec: `docs/specs/003-clustering-jerarquico/spec.md`
- Updated: 2026-11-02
- Constitution version: 1.2.0

## 2. Constitution check
- P1: los 4 stubs de `algorithms/clustering` (`SingleLinkage`, `CompleteLinkage`, `AverageLinkage`, `Diana`) solo lanzan `UnsupportedOperationException("TODO estudiante: …")`. El andamiaje no calcula distancias entre grupos, fusiones ni alturas. `ForbiddenClusteringUsageTest` impide librerías de clustering en Java y en `web/`. El código nuevo no usa `TreeMap`, `TreeSet`, `PriorityQueue`, `.sort(` ni `.toSorted(`, porque `ForbiddenSortUsageTest` (spec 002) recorre `src/main/java` y `web/src`.
- P2: D3 (`d3-hierarchy` para la disposición y `d3-scale` para el eje de altura) solo dibuja las fusiones que devuelve la API; KaTeX solo dibuja fórmulas. `features/clustering/dendrogramTree.ts` solo convierte la lista de fusiones en el árbol que pide D3: no compara distancias ni decide fusiones.
- P3: la similitud de las fuentes de embeddings la calculan los stubs del Requerimiento 1; el clustering no agrega modelos.
- P5: la matriz de entrada sale de `ComparisonService`, que solo usa el abstract (y `título [SEP] abstract` en SPECTER2).
- P6: el Javadoc de cada stub trae "Qué calcula", "Fórmula" (distancia entre grupos y recálculo de Lance-Williams, o diámetro y distancia media en DIANA), "Desempate", "Traza", "Rango", "Casos de borde" y "Complejidad: TODO estudiante". El visor de traza muestra las mismas fórmulas en KaTeX.
- P7: cada stub devuelve `ClusteringResult(merges, trace)` con un record de `trace/clustering` (`LinkageTrace` o `DianaTrace`); `GET /api/clustering/trace` la expone y `features/clustering` la muestra paso a paso.
- P8: todo tipo y método público nuevo lleva Javadoc en español con qué, por qué y complejidad. `ClusteringStubDocumentationTest` exige las secciones; `ClusteringStubScaffoldTest` (`scaffold`) exige "Complejidad: TODO estudiante".
- P9: `docs/guia-implementacion.md` recibe la sección "Requerimiento 3" y `docs/traceability.md` una fila por módulo con la sección `(pendiente)`; `ClusteringGuideTest` lo verifica.
- P10: tests de estudiante con la matriz de 4 puntos a mano y los bordes aplicables (2 artículos, idénticos, empates, 20 artículos), según la resolución de conflicto de la spec.
- P11: `ClusteringResponse.preparationMs` (preprocesamiento + matriz de la fuente) y `elapsedMs` por algoritmo, por separado. Sin JMH (fuera de alcance).
- P12: regla de desempate fija (pares de ids menores en orden lexicográfico; comparaciones exactas `<`), sin aleatoriedad; la matriz de 20 artículos de los tests usa `java.util.Random(42)`.
- P13: `algorithms/clustering`, `trace/clustering`, `api/clustering` y `web/src/features/clustering` son módulos nuevos; `algorithms` y `trace` no importan `api` ni `persistence`. El preprocesamiento y la similitud se reutilizan desde `ComparisonService`.

## 3. Architecture
Clustering (`POST /api/clustering`):
1. `ClusteringService` resuelve la fuente con `ComparisonAlgorithm.fromSource` (400 si es nula, vacía o desconocida).
2. `ComparisonService.similarityMatrix(articleIds, source)` valida la selección con `ArticleSelectionValidator` (400), prepara los artículos y calcula solo la matriz de esa fuente, con el mismo recorrido i ≤ j, la misma tolerancia y la misma detección de PENDIENTE que `compare`.
3. Si la fuente está PENDIENTE, la respuesta trae `source.status = PENDIENTE`, su `pendingLocation`, `distances = null` y `results = []` (REQ-19).
4. Si no, `ClusteringService` arma `D[i][j] = 1 − S[i][j]` para i ≠ j y `D[i][i] = 0` (REQ-10, REQ-3); `preparationMs` = preprocesamiento + tiempo de la matriz.
5. Por cada `ClusteringAlgorithm` se cronometra `cluster(copia de D)`. Un `TODO estudiante:` deja el resultado PENDIENTE (REQ-18). Con resultado, `DendrogramValidator.validate` revisa las fusiones y recorta alturas dentro de la tolerancia; si son inválidas lanza `InvalidDendrogramException`, que `ApiExceptionHandler` responde con 500 y mensaje en español (REQ-7). La traza no se incluye en esta respuesta.

Traza (`GET /api/clustering/trace`): los mismos pasos 1 a 4 y un solo algoritmo, que devuelve fusiones, traza y tiempo, o PENDIENTE.

Interfaz: `SimilarityPage` agrega, tras el visor de traza del Requerimiento 1, `ClusteringSection` con los artículos de la comparación actual. La sección tiene un selector de fuente (por defecto `specter2-cosine`), "Preparación: <ms>", la cuadrícula 2×2 de `DendrogramCard` (un `Dendrogram` D3 o el aviso de pendiente, con "Ejecución: <ms>") y `ClusteringTraceView` (selector de algoritmo y pasos). Un botón "Ver traza" de cada tarjeta elige ese algoritmo en el visor. Los datos se cargan con `useClustering` y `useClusteringTrace` (TanStack Query, clave = ids + fuente [+ algoritmo]).

## 4. Design patterns
- Strategy en `HierarchicalClustering<T>`: el servicio llama a los 4 stubs con una sola firma y los tests de API los reemplazan por algoritmos falsos.
- Registro `ClusteringCatalog` (record con las 4 estrategias, bean en `ClusteringConfig`): un solo bean `@Primary` de test (`FakeClusteringCatalog`) sustituye los 4 algoritmos, igual que `SimilarityCatalog` en la 001.

## 5. Code structure
Backend, producción (`src/main/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/clustering/package-info.java` - Javadoc del módulo del Requerimiento 3 y enlace a la guía.
- create: `algorithms/clustering/HierarchicalClustering.java` - contrato Strategy.
- create: `algorithms/clustering/Merge.java` - una fusión del dendrograma.
- create: `algorithms/clustering/ClusteringResult.java` - fusiones + traza.
- create: `algorithms/clustering/SingleLinkage.java`, `CompleteLinkage.java`, `AverageLinkage.java`, `Diana.java` - los 4 stubs.
- create: `algorithms/clustering/DendrogramValidator.java` - validación estructural y recorte de alturas.
- create: `algorithms/clustering/InvalidDendrogramException.java` - dendrograma inválido (500).
- create: `trace/clustering/package-info.java`, `LinkageTrace.java`, `DianaTrace.java` - contratos de traza.
- create: `api/clustering/ClusteringAlgorithm.java` - enum de los 4 algoritmos con slug, etiqueta, archivo y método del stub.
- create: `api/clustering/ClusteringCatalog.java` - record con las 4 estrategias.
- create: `api/clustering/ClusteringService.java`, `ClusteringController.java`, `ClusteringDtos.java` - clustering y traza.
- create: `config/ClusteringConfig.java` - bean `ClusteringCatalog` con los 4 stubs.
- modify: `api/similarity/ComparisonService.java` - método público `similarityMatrix` y record `SourceMatrix`; `compare` reutiliza el mismo código privado.
- modify: `api/similarity/ComparisonAlgorithm.java` - `fromSource(String)` con el mensaje de fuente desconocida.
- modify: `api/ApiExceptionHandler.java` - `InvalidDendrogramException` → 500 `ProblemDetail` con título "Error interno" y el mensaje en `detail`.
- modify: `build.gradle.kts` - descripciones de `studentTest` y `scaffoldTest` sin nombrar solo similitud.

Backend, tests (`src/test/java/co/edu/uniquindio/bibliomtr/`):
- create: `algorithms/clustering/ClusteringCases.java` - matrices compartidas (D₄ a mano, 2 puntos, idénticos, empates, 20 con `Random(42)`) y aserciones estructurales.
- create: `algorithms/clustering/SingleLinkageTest.java`, `CompleteLinkageTest.java`, `AverageLinkageTest.java`, `DianaTest.java` - `@Tag("student")`.
- create: `algorithms/clustering/ClusteringStubScaffoldTest.java` - `@Tag("scaffold")`.
- create: `algorithms/clustering/ClusteringStubDocumentationTest.java`, `ForbiddenClusteringUsageTest.java`, `DendrogramValidatorTest.java` - suite estándar.
- create: `api/clustering/FakeClusteringCatalog.java` - algoritmos falsos configurables (implementado, pendiente, inválido) que registran la última matriz recibida.
- create: `api/clustering/ClusteringApiIT.java` - `@SpringBootTest` con Postgres, `FakeSimilarityCatalog` y `FakeClusteringCatalog`.
- create: `docs/ClusteringGuideTest.java` - guía y trazabilidad del Requerimiento 3.

Frontend (`web/src/`):
- create: `types/clustering.ts` - tipos de las respuestas y trazas.
- create: `api/useClustering.ts`, `api/useClusteringTrace.ts` - hooks.
- create: `components/Dendrogram.tsx` (+ `Dendrogram.test.tsx`) - dibujo D3.
- create: `features/clustering/dendrogramTree.ts` (+ `dendrogramTree.test.ts`) - fusiones → árbol de D3.
- create: `features/clustering/ClusteringSection.tsx`, `DendrogramCard.tsx`, `SourceSelector.tsx` (+ `ClusteringSection.test.tsx`).
- create: `features/clustering/ClusteringTraceView.tsx`, `LinkageTraceView.tsx`, `DianaTraceView.tsx`, `clusteringFormulas.ts` (+ `ClusteringTraceView.test.tsx`).
- create: `test/clusteringFixtures.ts` - respuestas de ejemplo con D₄.
- modify: `pages/SimilarityPage.tsx` - agrega `ClusteringSection` tras el visor de traza.

Documentación:
- modify: `docs/guia-implementacion.md` - título general y sección "## 10. Requerimiento 3: Clustering jerárquico" con un `###` por stub; la sección de IA generativa nombra también el Requerimiento 3.
- modify: `docs/traceability.md` - filas del Requerimiento 3.
- modify: `README.md` - sección de clustering y comandos de test.

## 6. Interfaces and data
Algoritmos (`algorithms/clustering`):
```java
public interface HierarchicalClustering<T> { ClusteringResult<T> cluster(double[][] distances); }
public record Merge(int left, int right, double height, int size) {}
public record ClusteringResult<T>(List<Merge> merges, T trace) {}          // List.copyOf
public final class SingleLinkage implements HierarchicalClustering<LinkageTrace> {}
public final class CompleteLinkage implements HierarchicalClustering<LinkageTrace> {}
public final class AverageLinkage implements HierarchicalClustering<LinkageTrace> {}
public final class Diana implements HierarchicalClustering<DianaTrace> {}
public final class DendrogramValidator {
    public static final double HEIGHT_TOLERANCE = 1e-9;
    public static List<Merge> validate(String algorithm, int leafCount, List<Merge> merges);   // devuelve las fusiones con alturas recortadas
}
public class InvalidDendrogramException extends IllegalStateException { public InvalidDendrogramException(String message); }
```
- Entrada: `distances` N×N, N ≥ 2, simétrica, diagonal 0, valores en [0,1]; la fila/columna i es el artículo i de la solicitud.
- Convención de identificadores (estilo SciPy): hojas 0..N−1; la fusión k (0 ≤ k < N − 1) crea el grupo N + k. `Merge(left, right, height, size)` con `left < right`, `height` = distancia entre los dos grupos, `size` = artículos del grupo nuevo.
- Desempate (aglomerativos): entre los pares de grupos activos (p, q), p < q, de distancia mínima gana el de menor p y, a igual p, el de menor q. Se compara con `<` exacto, sin margen.
- Fórmulas (Lance-Williams, grupo nuevo k = i ∪ j frente a otro grupo h):
  - Single: d(k, h) = min(d(i, h), d(j, h)).
  - Complete: d(k, h) = max(d(i, h), d(j, h)).
  - Average (UPGMA): d(k, h) = (|i|·d(i, h) + |j|·d(j, h)) / (|i| + |j|).
- DIANA:
  1. Empieza con un grupo con todos los artículos. Mientras haya un grupo con al menos 2 artículos, divide el de mayor diámetro diam(C) = max d(a, b), a, b ∈ C (empate: el grupo cuyo menor artículo es menor).
  2. Grupo disidente S: arranca con el artículo de mayor distancia media al resto de C (empate: el de menor índice). Luego, por cada artículo i del resto R, D(i) = media de d(i, R \ {i}) − media de d(i, S); se mueve a S el de mayor D(i) si D(i) > 0 (empate: el de menor índice) y se repite; termina cuando ningún D(i) es positivo.
  3. La división de C a altura diam(C) se registra; el método se implementa recursivo o con una pila, a elección del estudiante.
  4. Las fusiones se devuelven en el orden inverso de las divisiones (la última división es la fusión 0), con la convención de identificadores de arriba.
- Rango: alturas en [0,1]. Single, Complete y Average dan alturas no decrecientes.
- Cuerpo de cada stub: `throw new UnsupportedOperationException("TODO estudiante: implementar <Clase>.cluster (ver docs/guia-implementacion.md)");`.
- `DendrogramValidator.validate` lanza `InvalidDendrogramException("El algoritmo <slug> devolvió un dendrograma inválido: <razón>")` cuando:
  - `merges` es nulo o su tamaño no es N − 1 ("<k> fusiones para <N> artículos; se esperaban <N − 1>").
  - en la fusión k, `left` o `right` no cumplen 0 ≤ left < right < N + k ("la fusión <k> usa un grupo inexistente o fuera de orden").
  - un grupo aparece como hijo dos veces ("el grupo <id> se fusiona dos veces").
  - `size` ≠ tamaño(left) + tamaño(right) ("la fusión <k> tiene tamaño <s>; se esperaba <t>").
  - `height` es NaN o está fuera de [−1e-9, 1 + 1e-9] ("la fusión <k> tiene altura <h> fuera de [0,1]"); dentro de esa tolerancia se devuelve min(1, max(0, h)).

Trazas (`trace/clustering`), con copias defensivas como en `trace/similarity`:
```java
public record LinkageTrace(List<LinkageStep> steps) {
    public record LinkageStep(List<Integer> clusters, List<List<Integer>> members, double[][] distances,
                              int left, int right, double distance, int newCluster,
                              List<Integer> remaining, double[] updatedRow) {}
}
public record DianaTrace(List<DianaStep> steps) {
    public record DianaStep(List<List<Integer>> clusters, double[] diameters, int splitIndex, double diameter,
                            List<DianaIteration> iterations, List<Integer> splinter, List<Integer> remainder) {}
    public record DianaIteration(List<Integer> splinter, List<Integer> remainder, double[] averageToRemainder,
                                 double[] averageToSplinter, double[] differences, int moved) {}
}
```
- `LinkageStep` (uno por fusión): `clusters` = ids de los grupos activos antes de fusionar, en orden creciente (los que quedan conservan su orden y el nuevo va al final); `members` = artículos de cada uno, en orden creciente; `distances` = matriz entre esos grupos; `remaining` = `clusters` sin `left` ni `right`; `updatedRow` = d(newCluster, h) para cada h de `remaining`, alineado.
- `DianaStep` (uno por división): `clusters` = grupos activos antes de dividir, con sus artículos en orden creciente; el grupo dividido se reemplaza en su posición por la parte con el menor artículo seguida de la otra. `diameters` alineado con `clusters` (0 en los de un artículo). `DianaIteration`: arreglos alineados con `remainder`; en la primera iteración `splinter` está vacío, `averageToSplinter` es 0 y `differences` = `averageToRemainder`; `moved` = artículo movido a S, o −1 al terminar.

Caso a mano D₄ (guía, tests, fixtures del frontend):
```
      0     1     2     3
0   0     0.2   0.5   0.7
1   0.2   0     0.3   0.6
2   0.5   0.3   0     0.45
3   0.7   0.6   0.45  0
```
| Algoritmo | Fusión 0 | Fusión 1 | Fusión 2 | `updatedRow` del paso 1 |
|---|---|---|---|---|
| Single | (0, 1, 0.2, 2) | (2, 4, 0.3, 3) | (3, 5, 0.45, 4) | [0.3, 0.6] |
| Complete | (0, 1, 0.2, 2) | (2, 3, 0.45, 2) | (4, 5, 0.7, 4) | [0.5, 0.7] |
| Average | (0, 1, 0.2, 2) | (2, 4, 0.4, 3) | (3, 5, 0.583333, 4) | [0.4, 0.65] |
| DIANA | (0, 1, 0.2, 2) | (2, 4, 0.5, 3) | (3, 5, 0.7, 4) | — |

DIANA sobre D₄: división 1 de {0,1,2,3} con diámetro 0.7; medias iniciales [0.466667, 0.366667, 0.416667, 0.583333] → S = {3}; luego D = [−0.35, −0.35, −0.05] y termina. División 2 de {0,1,2} con diámetro 0.5 → S = {2}. División 3 de {0,1} con diámetro 0.2.

Empates (4 artículos, todas las distancias 0.5): Single, Complete y Average dan (0, 1), (2, 3), (4, 5); DIANA da (2, 3), (1, 4), (0, 5), todas a altura 0.5.

API REST:

| Método y ruta | Parámetros | Respuesta |
|---|---|---|
| `POST /api/clustering` | cuerpo `ClusteringRequest` | 200 `ClusteringResponse`, 400 o 500 |
| `GET /api/clustering/trace` | `algorithm` (slug), `source` (slug de la 001), `articleIds` (ids separados por comas) | 200 `ClusteringTraceResponse`, 400 o 500 |

```java
record ClusteringRequest(List<Long> articleIds, String source) {}
record SourceSummary(String source, String label, String status, String pendingLocation) {}
record ClusteringResponse(List<ArticleSummary> articles, SourceSummary source, Double preparationMs,
                          double[][] distances, List<DendrogramResult> results) {}
record DendrogramResult(String algorithm, String label, String status, List<Merge> merges, Double elapsedMs,
                        String pendingLocation) {}
record ClusteringTraceResponse(String algorithm, String label, List<ArticleSummary> articles, SourceSummary source,
                               Double preparationMs, double[][] distances, String status, List<Merge> merges,
                               Object trace, Double elapsedMs, String pendingLocation) {}
```
- `ClusteringAlgorithm`, en este orden de respuesta: `single-linkage` ("Single Linkage"), `complete-linkage` ("Complete Linkage"), `average-linkage` ("Average Linkage (UPGMA)"), `diana` ("DIANA (divisivo)"). `pendingLocation()` = `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/<Clase>.java#cluster`. `fromSlug` lanza `IllegalArgumentException("Algoritmo de clustering desconocido: <x>. Valores válidos: …")`.
- `ComparisonAlgorithm.fromSource(String)`: nula o vacía → `IllegalArgumentException("Debe indicar la fuente de similitud (source)")`; desconocida → `"Fuente de similitud desconocida: <x>. Valores válidos: …"`.
- `ComparisonService.similarityMatrix(List<Long> articleIds, ComparisonAlgorithm source)` → `record SourceMatrix(List<ArticleSummary> articles, AlgorithmMatrix matrix, double preprocessingMs)`; aplica `validateComparison` (mismos mensajes 400 que la 001).
- Orden de validación: fuente, luego selección de artículos, luego algoritmo (en la traza). `articleIds` ausente en la traza → "Falta el parámetro articleIds" (handler existente).
- Fuente PENDIENTE: `source.status = "PENDIENTE"`, `preparationMs`, `distances`, `merges`, `trace`, `elapsedMs` y `status` del algoritmo en null; `results = []`.
- Algoritmo PENDIENTE: `merges`, `trace` y `elapsedMs` null, `pendingLocation` con la ubicación del stub.
- `ApiExceptionHandler`: `InvalidDendrogramException` → 500 con `title` "Error interno" y `detail` = mensaje.

Frontend:
- `types/clustering.ts`: `ClusteringAlgorithmSlug`, `CLUSTERING_ALGORITHMS`, `Merge`, `SourceSummary`, `ClusteringResponse`, `DendrogramResult`, `ClusteringTraceResponse`, `LinkageTrace`, `DianaTrace`, `DEFAULT_SOURCE = 'specter2-cosine'`.
- `useClustering(articleIds, source)`: `useQuery` con `apiPost('/api/clustering', { articleIds, source })`, clave `['clustering', articleIds, source]`, habilitada con 2 o más ids.
- `useClusteringTrace(algorithm, source, articleIds)`: `useQuery` con `apiGet('/api/clustering/trace?…')`, habilitada cuando el algoritmo está elegido.
- `dendrogramTree.ts`: `toHierarchy(merges, leafLabels): DendrogramNode`, con `DendrogramNode = { id, label, height, size, children? }`; las hojas tienen altura 0; el nodo N + k toma `merges[k]` con hijos `left` y `right`, en ese orden. Lanza `Error` si las fusiones no forman un solo árbol. No usa `.sort(`.
- `Dendrogram({ merges, labels, ariaLabel })`: SVG de ancho adaptable (viewBox) con `d3.hierarchy` + `d3.cluster` para la posición horizontal de las hojas, `d3.scaleLinear().domain([0, 1])` para la altura (eje con marcas 0, 0.25, 0.5, 0.75, 1), enlaces en codo y una etiqueta por hoja `#<i> <título recortado a 40 caracteres>` con `<title>` del título completo. No llama a `hierarchy.sort`.
- `DendrogramCard`: con CALCULADO, "Ejecución: <ms>", el `Dendrogram` y el botón "Ver traza"; con PENDIENTE, "Pendiente de implementar: <pendingLocation>".
- `SourceSelector`: MUI `Select` con las 8 fuentes de `SIMILARITY_ALGORITHMS` y sus etiquetas.
- `ClusteringSection({ articles })`: título "Clustering jerárquico", `SourceSelector`, estado de carga y error (`Alert`), "Preparación: <ms>", aviso "La fuente <label> está pendiente de implementar: <pendingLocation>" si la fuente está PENDIENTE, y la cuadrícula `display: grid` con `gridTemplateColumns` `1fr` en `xs` y `repeat(2, 1fr)` desde `md`. Cambiar la fuente limpia el algoritmo elegido del visor.
- `ClusteringTraceView`: selector del algoritmo, matriz de distancias de entrada con `PagedMatrix` y los pasos con `TraceStep` (fórmula KaTeX de `clusteringFormulas.ts`):
  - Linkage, por fusión: matriz entre grupos (`PagedMatrix`, encabezados `G<id> {miembros}`), par elegido y distancia, fila recalculada con la fórmula del algoritmo.
  - DIANA, por división: grupos y diámetros, iteraciones (tabla de medias y diferencias, artículo movido o "fin"), partes resultantes.
  - PENDIENTE: "Pendiente de implementar: <pendingLocation>".
- `SimilarityPage`: `<ClusteringSection articles={response.articles} />` tras el visor de traza, separado con `Divider`.

## 8. Traceability
| Requirement | Component / file | Verified by (AC) |
|---|---|---|
| REQ-1 | `SingleLinkage`, `CompleteLinkage`, `AverageLinkage`; `ClusteringStubScaffoldTest` | AC-1 |
| REQ-2 | `Diana`; `ClusteringStubScaffoldTest` | AC-1 |
| REQ-3 | `HierarchicalClustering`, `Merge`, `ClusteringResult`; tests `student` | AC-5, AC-6, AC-9 |
| REQ-4 | Javadoc de los 4 stubs; `ClusteringStubDocumentationTest` | AC-18 |
| REQ-5 | Javadoc "Desempate"; `ClusteringCases.ties`, tests `student` | AC-8 |
| REQ-6 | stubs sin lógica; `ForbiddenClusteringUsageTest`, `ClusteringStubScaffoldTest` | AC-1, AC-2 |
| REQ-7 | `DendrogramValidator`, `InvalidDendrogramException`, `ApiExceptionHandler`; `DendrogramValidatorTest`, `ClusteringApiIT` | AC-10 |
| REQ-8 | `SingleLinkageTest`, `CompleteLinkageTest`, `AverageLinkageTest`, `DianaTest`, `ClusteringCases` | AC-5, AC-6, AC-7, AC-8, AC-9 |
| REQ-9 | `@Tag("student")`/`@Tag("scaffold")`, tareas de `build.gradle.kts` | AC-3, AC-4 |
| REQ-10 | `ComparisonService.similarityMatrix`, `ClusteringService` (D = 1 − S); `ClusteringApiIT` | AC-11 |
| REQ-11 | `ClusteringController`, `ClusteringDtos`, `ClusteringAlgorithm`; `ClusteringApiIT` | AC-11 |
| REQ-12 | `ComparisonAlgorithm.fromSource`, `ArticleSelectionValidator`; `ClusteringApiIT` | AC-12 |
| REQ-13 | `ClusteringSection`, `SourceSelector`, `SimilarityPage`; `ClusteringSection.test.tsx` | AC-13 |
| REQ-14 | `ClusteringSection`, `DendrogramCard`, `Dendrogram`; `ClusteringSection.test.tsx`, `Dendrogram.test.tsx` | AC-14 |
| REQ-15 | `dendrogramTree.ts`, `Dendrogram`; `dendrogramTree.test.ts`, `ForbiddenClusteringUsageTest` | AC-2, AC-14 |
| REQ-16 | `LinkageTrace`, `DianaTrace`, `GET /api/clustering/trace`; tests `student`, `ClusteringApiIT` | AC-5, AC-15 |
| REQ-17 | `ClusteringTraceView`, `LinkageTraceView`, `DianaTraceView`, `clusteringFormulas.ts`; `ClusteringTraceView.test.tsx` | AC-15 |
| REQ-18 | `ClusteringService` (detección de `TODO estudiante:`), `DendrogramCard`; `ClusteringApiIT`, `ClusteringSection.test.tsx` | AC-16 |
| REQ-19 | `ClusteringService`, `SourceSummary`, `ClusteringSection`; `ClusteringApiIT`, `ClusteringSection.test.tsx` | AC-17 |
| REQ-20 | `docs/guia-implementacion.md` § 10; `ClusteringGuideTest` | AC-18 |
| REQ-21 | `docs/traceability.md`; `ClusteringGuideTest` | AC-18 |
| REQ-22 | `ClusteringService` con `Stopwatch`; `ClusteringApiIT`, `ClusteringSection.test.tsx` | AC-19 |

Tests por AC (given - when - then de la spec):

| AC | Test (archivo) | Comprobación |
|---|---|---|
| AC-1 | `ClusteringStubScaffoldTest` (`scaffold`) | cada stub lanza `UnsupportedOperationException` con "TODO estudiante:" sobre D₄ y su Javadoc contiene "Complejidad: TODO estudiante" |
| AC-2 | `ForbiddenClusteringUsageTest` | sin imports Java de `smile.`, `weka.`, `org.apache.commons.math3.ml.clustering`, `com.apporiented`, `org.apache.spark.ml.clustering` ni `org.tribuo.clustering`; `web/package.json` sin `ml-hclust`, `hclust`, `clusterfck`, `density-clustering`, `ml-kmeans`; `web/src` sin imports de esos paquetes; los stubs no tienen más sentencias que el `throw` (verificado en `scaffold`) |
| AC-3 | `./gradlew test` | verde con los 4 stubs sin implementar |
| AC-4 | `./gradlew studentTest --tests '*Linkage*' --tests DianaTest` | todos fallan con los stubs |
| AC-5 | los 4 tests `student` | D₄: fusiones y alturas de la tabla de §6 (tolerancia 1e-9); linkage: 3 pasos, `clusters` del paso 1 = [0, 1, 2, 3], `updatedRow` del paso 1 de la tabla; DIANA: 3 pasos, diámetros 0.7, 0.5 y 0.2, `moved` de la primera división = 3 y luego −1 |
| AC-6 | los 4 tests `student` | 2 artículos con d = 0.35 → [(0, 1, 0.35, 2)] |
| AC-7 | los 4 tests `student` | 3 artículos con d(0, 2) = 0 y el resto 0.6 → la primera fusión es (0, 2, 0, 2) |
| AC-8 | los 4 tests `student` | 4 artículos a 0.5 → fusiones de la regla de §6; dos llamadas dan listas iguales |
| AC-9 | los 4 tests `student` | 20 artículos (`Random(42)`, simétrica, diagonal 0) → 19 fusiones, la última de tamaño 20, `DendrogramValidator.validate` sin excepción; linkage con alturas no decrecientes; DIANA con altura de cada fusión mayor o igual que la de sus hijos; dos llamadas dan listas iguales |
| AC-10 | `DendrogramValidatorTest`, `ClusteringApiIT` | 3 fusiones para 3 artículos, grupo repetido, `left ≥ right`, tamaño erróneo, altura 1.5 o NaN → `InvalidDendrogramException` con su razón; 1 + 1e-12 → 1.0; un fake inválido → HTTP real 500 con `detail` "El algoritmo single-linkage devolvió un dendrograma inválido: …" |
| AC-11 | `ClusteringApiIT` | 5 artículos, fuente `specter2-cosine` y fakes implementados → 4 resultados CALCULADO con 4 fusiones; `distances` 5×5 con diagonal 0 y 1 − 0.75 = 0.25 fuera de ella; el fake de clustering recibió esa misma matriz; la traza de `average-linkage` → 200 con `merges` y `trace` |
| AC-12 | `ClusteringApiIT` | 1 id, 21 ids, ids repetidos, id inexistente → 400 con los mensajes de la 001; `source` desconocida → 400 "Fuente de similitud desconocida: …"; sin `source` → 400 "Debe indicar la fuente de similitud (source)"; traza con algoritmo desconocido → 400 "Algoritmo de clustering desconocido: …" |
| AC-13 | `ClusteringSection.test.tsx` | con `fetch` simulado: la sección pide `specter2-cosine` con los ids de la comparación; al elegir `jaccard` vuelve a pedir con `source: 'jaccard'` y dibuja los 4 resultados recibidos |
| AC-14 | `Dendrogram.test.tsx`, `ClusteringSection.test.tsx`, `dendrogramTree.test.ts` | D₄ de Complete → 4 hojas con los títulos, 3 nodos internos y un eje con 0 y 1; las 4 tarjetas comparten el dominio [0,1]; `toHierarchy` da la raíz con tamaño 4 y altura 0.7, y lanza con fusiones que no forman un árbol |
| AC-15 | `ClusteringTraceView.test.tsx`, `ClusteringApiIT` | traza de Single sobre D₄ (fixture) → 3 pasos en orden con la fórmula `\min` en KaTeX, la matriz entre grupos y la fila [0.3, 0.6]; traza de DIANA → 3 divisiones con diámetros |
| AC-16 | `ClusteringApiIT`, `ClusteringSection.test.tsx` | solo `average-linkage` implementado → 1 CALCULADO y 3 PENDIENTE con `pendingLocation`; la vista muestra "Pendiente de implementar: src/main/java/…/SingleLinkage.java#cluster" |
| AC-17 | `ClusteringApiIT`, `ClusteringSection.test.tsx` | `CosineSimilarity` pendiente en `FakeSimilarityCatalog` → `source.status` PENDIENTE con `…/CosineSimilarity.java#compare`, `results` vacío; la vista muestra el aviso y ningún dendrograma |
| AC-18 | `ClusteringGuideTest` | la guía tiene, por cada stub, la ruta, `cluster(`, "Fórmula", "Traza", "Desempate", sus valores de D₄ y `./gradlew studentTest --tests <Clase>Test`; `docs/traceability.md` tiene filas del Requerimiento 3 para `algorithms.clustering`, `trace.clustering`, `api.clustering` y vistas web, con "(pendiente)" |
| AC-19 | `ClusteringApiIT`, `ClusteringSection.test.tsx` | `preparationMs` ≥ 0 y `elapsedMs` ≥ 0 por algoritmo calculado; la vista muestra "Preparación" y "Ejecución" en ms |

Comandos: `./gradlew test`, `./gradlew studentTest`, `./gradlew scaffoldTest` y `npm test`, `npm run lint`, `npm run build` en `web/`.

## 9. Decisions and risks
- Tres aglomerativos Single, Complete y Average: comparten Lance-Williams y aceptan cualquier d = 1 − s. Se rechazó Ward porque solo es válido con distancias euclidianas.
- Salida común en lista de fusiones estilo SciPy, también para DIANA: un solo validador, un solo componente D3 y la misma convención en la guía. Se rechazó un árbol de nodos porque obligaría a los aglomerativos a construirlo a mano y al validador a recorrerlo recursivamente.
- La traza se pide aparte (`GET /api/clustering/trace`), por decisión del usuario; recalcula la matriz de la fuente, que con N ≤ 20 cuesta lo mismo que en la 001.
- `ComparisonService.similarityMatrix` reutiliza validación, preprocesamiento y matriz de la 001 y solo calcula la fuente elegida. Se rechazó llamar a `compare`, que calcularía las 8 matrices.
- La diagonal de D se fija en 0 en el andamiaje: es la definición de la entrada (REQ-3), no lógica del clustering, y evita que una similitud propia de 1 − 1e-16 rompa los empates.
- El validador no exige alturas monótonas: no lo pide la spec y DIANA solo es monótona por diámetro. La monotonía de los aglomerativos se prueba en los tests `student` (AC-9).
- Las comparaciones de empate son exactas (`<`); los casos de empate usan 0.5, que es exacto en binario y se conserva en el promedio de UPGMA.
- `dendrogramTree.ts` vive en el frontend porque D3 necesita un árbol; solo reorganiza las fusiones recibidas. Se rechazó que la API devuelva el árbol, para no duplicar la salida de los stubs.
- Risk: `d3.hierarchy(...).sort` y `Array.prototype.sort` están prohibidos por `ForbiddenSortUsageTest` - el orden de las hojas es el de los hijos `left` y `right`; `d3.cluster` no necesita ordenar.
- Risk: D3 en jsdom no calcula tamaños de texto - el SVG usa `viewBox` y posiciones calculadas sin `getBBox`; los tests verifican nodos y etiquetas, no píxeles.
- Risk: con 20 hojas las etiquetas se superponen - se recortan a 40 caracteres, se rotan −90° bajo el eje y el título completo va en `<title>`.
- Risk: `ComparisonService` cambia y la 001 podría romperse - `compare` y `trace` siguen usando el mismo código privado, y `ComparisonApiIT` sigue en la suite estándar.
