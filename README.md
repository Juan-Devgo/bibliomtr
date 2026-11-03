# bibliomtr

Ejercicio académico de Análisis de Algoritmos (Universidad del Quindío): comparación de algoritmos clásicos de similitud textual y embeddings, ranking de términos, clustering jerárquico y búsqueda exacta de patrones sobre los abstracts de 20 artículos científicos.

- Principios: [`docs/constitution.md`](docs/constitution.md)
- Instrucciones para agentes y stack: [`AGENTS.md`](AGENTS.md)
- Trazabilidad requerimiento → módulo → documento técnico: [`docs/traceability.md`](docs/traceability.md)
- Guía de implementación de los algoritmos de similitud (Requerimiento 1), de clustering jerárquico (Requerimiento 3) y de búsqueda de patrones (Requerimiento 4): [`docs/guia-implementacion.md`](docs/guia-implementacion.md)

## Requisitos

Java 25, Node 24, Docker.

## Primer uso

```bash
./scripts/download-model.sh   # exporta SPECTER2 base a ONNX en src/main/resources/models/ (requiere python3)
cp /ruta/a/los/pdf/*.pdf corpus/
```

## Desarrollo

```bash
./gradlew bootRun             # levanta Postgres y Ollama (compose.dev.yaml) y la API en :8080
cd web && npm run dev         # frontend en :5173
```

- Al arrancar, la API ingiere los PDF de `corpus/` (título, autores, abstract y embeddings de SPECTER2 y nomic-embed-text). El embedding de nomic-embed-text lo genera Ollama local (`ollama/ollama:0.12.6`, puerto 11434); la API descarga el modelo `nomic-embed-text:v1.5` con `POST /api/pull` la primera vez. La URL se configura con `BIBLIOMTR_OLLAMA_URL` (por defecto `http://localhost:11434`).
- Swagger UI: http://localhost:8080/api/swagger-ui
- Salud: http://localhost:8080/api/actuator/health

## Pruebas y mediciones

```bash
./gradlew test                # JUnit 5 + Testcontainers (requiere Docker)
./gradlew studentTest         # tests de estudiante de similitud, de clustering y de búsqueda de patrones (fallan hasta implementarlos)
./gradlew studentTest --tests LevenshteinTest   # tests de un solo algoritmo
./gradlew studentTest --tests '*LinkageTest' --tests DianaTest   # tests de estudiante del clustering jerárquico
./gradlew studentTest --tests KnuthMorrisPrattTest --tests BoyerMooreTest --tests RabinKarpTest --tests StringMatchingOracleTest   # tests de estudiante de la búsqueda de patrones
./gradlew scaffoldTest        # comprueba que los stubs del andamiaje siguen sin implementar
./gradlew jmhJar && java -jar build/libs/benchmarks.jar SimilarityBenchmark   # benchmarks JMH de similitud
./gradlew jmhJar && mkdir -p build/jmh-results && java -jar build/libs/benchmarks.jar '(StringMatchingBenchmark|RabinKarpBenchmark)' -rf json -rff build/jmh-results/string-matching.json   # benchmarks JMH de búsqueda de patrones
```

Qué implementar en cada algoritmo de similitud, con sus fórmulas, casos a mano, tests y benchmark: [`docs/guia-implementacion.md`](docs/guia-implementacion.md).

## Clustering jerárquico (Requerimiento 3)

Tras comparar artículos en la pestaña "Similitud textual", la sección "Clustering jerárquico" agrupa esos mismos artículos con Single, Complete y Average Linkage (UPGMA) y con DIANA (divisivo, opcional), sobre la distancia d = 1 − s de la fuente de similitud elegida (SPECTER2 coseno por defecto). Muestra los 4 dendrogramas en una cuadrícula 2×2, con el tiempo de preparación y el de cada algoritmo, y la traza paso a paso de cada uno.

- API: `POST /api/clustering` con `{"articleIds": [..], "source": "specter2-cosine"}` y `GET /api/clustering/trace?algorithm=single-linkage&source=specter2-cosine&articleIds=1,2,3`.
- Stubs a implementar: `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/` (`SingleLinkage`, `CompleteLinkage`, `AverageLinkage`, `Diana`). Mientras no estén implementados, aparecen como "Pendiente de implementar".
- Fórmulas, desempate, trazas, caso a mano D₄ y tests: [`docs/guia-implementacion.md`](docs/guia-implementacion.md) § 10.

## Búsqueda de patrones (Requerimiento 4)

La pestaña "Búsqueda Patrones" busca un patrón de 1 a 200 caracteres (sin recortar, distinguiendo mayúsculas y tildes) en los abstracts del corpus con Knuth-Morris-Pratt, Boyer-Moore y Rabin-Karp. Muestra por algoritmo las posiciones de cada ocurrencia (incluidas las solapadas), el total, las colisiones de Rabin-Karp y los tiempos de preprocesamiento y de búsqueda por separado; resalta las ocurrencias en el abstract elegido y muestra la traza paso a paso de cada algoritmo. Rabin-Karp usa d = 65 536 y un q primo opcional (101 por defecto).

- API: `GET /api/pattern-search?pattern=systems&q=101` y `GET /api/pattern-search/trace?algorithm=kmp&articleId=1&pattern=systems`.
- Stubs a implementar: `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/` (`KnuthMorrisPratt`, `BoyerMoore`, `RabinKarp`), métodos `preprocess` y `search`. Mientras no estén implementados, aparecen como "Pendiente de implementar".
- Definiciones formales, trazas, casos a mano, ejemplo sobre un fragmento real, tests y benchmark JMH: [`docs/guia-implementacion.md`](docs/guia-implementacion.md) § 11.

## Stack completo

```bash
docker compose up --build     # db, ollama (:11434), api (:8080) y web (:5173)
```
