# bibliomtr

Ejercicio académico de Análisis de Algoritmos (Universidad del Quindío): comparación de algoritmos clásicos de similitud textual y embeddings sobre los abstracts de 20 artículos científicos.

- Principios: [`docs/constitution.md`](docs/constitution.md)
- Instrucciones para agentes y stack: [`AGENTS.md`](AGENTS.md)
- Trazabilidad requerimiento → módulo → documento técnico: [`docs/traceability.md`](docs/traceability.md)
- Guía de implementación de los algoritmos de similitud (Requerimiento 1): [`docs/guia-implementacion.md`](docs/guia-implementacion.md)

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
./gradlew studentTest         # tests de estudiante de los algoritmos de similitud (fallan hasta implementarlos)
./gradlew studentTest --tests LevenshteinTest   # tests de un solo algoritmo
./gradlew scaffoldTest        # comprueba que los stubs del andamiaje siguen sin implementar
./gradlew jmhJar && java -jar build/libs/benchmarks.jar SimilarityBenchmark   # benchmarks JMH de similitud
```

Qué implementar en cada algoritmo de similitud, con sus fórmulas, casos a mano, tests y benchmark: [`docs/guia-implementacion.md`](docs/guia-implementacion.md).

## Stack completo

```bash
docker compose up --build     # db, ollama (:11434), api (:8080) y web (:5173)
```
