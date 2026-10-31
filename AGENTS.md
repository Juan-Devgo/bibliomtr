# AGENTS.md

## Workflow
- Todos los archivos SDD viven en `docs/`.
- Jerarquía: `docs/constitution.md` > `docs/specs/###-name/spec.md` > `plan.md` > `tasks.md` > código. Cada nivel debe cumplir el de arriba.
- Lee la constitución antes de planificar o programar. No se repite aquí.
- Idioma: documentación, docstrings/Javadoc y comentarios en español; identificadores de código en inglés.
- Requerimientos (numeración vigente): 1 similitud textual (spec 001), 2 ranking de términos con ordenamiento (spec 002, agregado con prioridad), 3 clustering jerárquico con dendrograma, 4 coincidencia exacta de cadenas (KMP, Boyer-Moore, Rabin-Karp), 5 despliegue y documentación técnica. El número de spec no tiene que coincidir con el del requerimiento; la correspondencia vive en `docs/traceability.md`.
- El documento técnico (`docs/documento-tecnico.md`) lo redactan los estudiantes, no los agentes. El flujo SDD solo cubre constitución, specs, planes, tareas, trazabilidad y la guía de implementación; en trazabilidad la sección del documento queda `(pendiente)`.

## Stack
- Backend: Java 25 (toolchain en `build.gradle.kts`), Spring Boot 4.1.x, Gradle Kotlin DSL (`./gradlew`), paquete base `co.edu.uniquindio.bibliomtr`.
- Backend libs: Spring Web MVC, Validation, Data JPA, Actuator, springdoc-openapi, Lombok. Migraciones: Flyway.
- PDF: Apache PDFBox, solo para extraer texto, título y autores.
- Algoritmos: Java puro. MUST NOT usar Commons Text, Lucene, Smile ni Commons Math.
- Embeddings: SPECTER2 exportado a ONNX, ejecutado con ONNX Runtime (el tokenizer de DJL solo tokeniza), y Ollama local con `nomic-embed-text` vía HTTP (imagen y modelo con versión fijada). Solo generan vectores; la métrica de similitud se implementa a mano.
- Base de datos: PostgreSQL con extensión `vector`, imagen `pgvector/pgvector` en desarrollo y tests. Solo almacenamiento.
- Pruebas: JUnit 5, AssertJ, Testcontainers (Postgres real).
- Mediciones: JMH con calentamiento, solo en los requerimientos que lo piden (p. ej. Req. 2, ranking de términos).
- Frontend (`web/`): React + TypeScript + Vite, npm, TanStack Query, D3 (solo dibuja el dendrograma que devuelve el backend), KaTeX (`react-katex`), Recharts, MUI y shadcn/ui.
- Despliegue: Docker Compose con `api`, `web`, `db` y `ollama`; en producción, Postgres gestionado con pgvector en RDS.

## Commands
- Backend dev: `./gradlew bootRun`
- Backend test: `./gradlew test`
- Backend build: `./gradlew build`
- Frontend (en `web/`): `npm install`, `npm run dev`, `npm run build`, `npm run lint`, `npm test`
- Stack completo: `docker compose up`

## Structure
Monorepo: backend en la raíz (Gradle), frontend en `web/`.
```
bibliomtr/
├── build.gradle.kts, settings.gradle.kts, gradlew   # backend
├── compose.yaml                                     # api + web + db + ollama
├── Dockerfile                                       # imagen de api
├── docs/                                            # constitution.md, specs/###-name/, traceability.md, guia-implementacion.md, documento técnico (estudiantes)
├── src/
│   ├── main/
│   │   ├── java/co/edu/uniquindio/bibliomtr/
│   │   │   ├── pdf/            # extracción con PDFBox (texto, título, autores)
│   │   │   ├── preprocessing/  # normalización, tokenización, stopwords (determinista)
│   │   │   ├── algorithms/     # un subpaquete por algoritmo/requerimiento, Java puro
│   │   │   ├── trace/          # objetos de traza compartidos (pasos intermedios)
│   │   │   ├── embeddings/     # carga del modelo ONNX/DJL y generación de vectores
│   │   │   ├── evaluation/     # medición de tiempos y comparación teoría/práctica
│   │   │   ├── persistence/    # entidades JPA y repositorios (solo almacenamiento)
│   │   │   ├── api/            # controllers, DTOs, manejo de errores
│   │   │   └── config/         # configuración Spring
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/   # Flyway: V<n>__descripcion.sql
│   │       └── models/         # modelo ONNX y tokenizer (si se versiona local)
│   ├── test/java/…             # espejo de main; oracle/ = tests con funciones prohibidas
│   └── jmh/java/…              # benchmarks JMH
└── web/
    ├── Dockerfile
    └── src/
        ├── api/                # cliente HTTP y hooks TanStack Query
        ├── components/         # componentes reutilizables (dendrograma, traza, fórmulas)
        ├── features/           # una carpeta por requerimiento/vista
        ├── pages/
        └── types/              # tipos TS de las respuestas y trazas de la API
```
- Un módulo por requerimiento; cada módulo se mapea a su demostración en el documento técnico, registrada en `docs/traceability.md` (ver constitución P9).
- Los módulos `pdf`, `preprocessing`, `algorithms`, `evaluation` no dependen de `api` ni de `persistence`.

## Conventions
- Diseño clave: cada algoritmo MUST devolver el resultado más un objeto de traza con sus pasos intermedios. La API lo expone como JSON y React lo muestra paso a paso: código, interfaz y documento técnico muestran lo mismo.
- Naming Java: paquetes `lowercase`, clases `PascalCase`, métodos y variables `camelCase`, constantes `UPPER_SNAKE_CASE`; tests `<Clase>Test.java`, junto al paquete espejo en `src/test`.
- Naming BD: tablas y columnas `snake_case`; migraciones `V<n>__descripcion.sql`; nunca editar una migración ya aplicada.
- Naming frontend: directorios `kebab-case`, componentes `PascalCase.tsx`, hooks y utilidades `camelCase.ts`.
- Commits: Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).
- Ramas: `feat/<nombre>`, `fix/<nombre>`.

## PostgreSQL prohibido (siempre activo)
En código de producción (incluye consultas nativas, JPQL y métodos derivados de Spring Data como `findBy…Containing`/`…Like`) MUST NOT usarse:
1. `strpos`, `position`
2. `LIKE`, `ILIKE`
3. Full-text search (`tsvector`, `tsquery`)
4. `pg_trgm`
5. `fuzzystrmatch` (`levenshtein`)
6. Operadores de pgvector (`<=>`, `<->`, `<#>`): el cálculo ocurriría dentro de la base y no se podría mostrar paso a paso.

- pgvector es solo almacenamiento de embeddings; todo cálculo se hace en Java.
- Excepción: se permiten únicamente en tests (`src/test/**/oracle/`), como referencia para verificar los algoritmos en Java puro. Nunca en `src/main`.

## Maintaining this file
- Si encuentras algo importante para cada llamada, o un flujo repetido (p. ej. una librería de UI como shadcn), pregunta al usuario si se agrega aquí. Nunca agregar sin aprobación.
- Prefiere un puntero de una línea a un skill o doc en vez de explicar.
- Mantén el archivo pequeño: une duplicados, borra líneas obsoletas, mueve detalles a docs o skills.
