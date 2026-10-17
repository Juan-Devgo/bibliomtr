# bibliomtr

Ejercicio académico de Análisis de Algoritmos (Universidad del Quindío): comparación de algoritmos clásicos de similitud textual y embeddings sobre los abstracts de 20 artículos científicos.

- Principios: [`docs/constitution.md`](docs/constitution.md)
- Instrucciones para agentes y stack: [`AGENTS.md`](AGENTS.md)
- Trazabilidad requerimiento → módulo → documento técnico: [`docs/traceability.md`](docs/traceability.md)

## Requisitos

Java 25, Node 24, Docker.

## Primer uso

```bash
./scripts/download-model.sh   # modelo all-MiniLM-L6-v2 (ONNX) en src/main/resources/models/
cp /ruta/a/los/pdf/*.pdf corpus/
```

## Desarrollo

```bash
./gradlew bootRun             # levanta Postgres (compose.dev.yaml) y la API en :8080
cd web && npm run dev         # frontend en :5173
```

- Swagger UI: http://localhost:8080/api/swagger-ui
- Salud: http://localhost:8080/api/actuator/health

## Pruebas y mediciones

```bash
./gradlew test                # JUnit 5 + Testcontainers (requiere Docker)
./gradlew jmh                 # benchmarks JMH
```

## Stack completo

```bash
docker compose up --build     # db, api (:8080) y web (:5173)
```
