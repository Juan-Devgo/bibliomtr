# Constitution
Version: 1.2.0

## 1. Mission
Ejercicio académico de Análisis de Algoritmos (Universidad del Quindío, Ingeniería de Sistemas y Computación) que compara IA generativa (embeddings) y algoritmos clásicos de similitud textual en bibliometría sobre los abstracts de 20 artículos científicos. El código es evidencia de aprendizaje: MUST demostrar comprensión algorítmica, matemática y estadística, y nunca ser una caja negra.

## 2. Core principles
- P1. Los algoritmos solicitados MUST implementarse de forma explícita con estructuras básicas del lenguaje; MUST NOT usarse librerías o funciones de alto nivel que los implementen directamente (p. ej. `scipy.cluster.hierarchy`, `sklearn` para similitud o clustering, `str.find`).
- P2. Las librerías MUST usarse solo para tareas auxiliares: lectura de PDF, graficación y carga de modelos de embeddings.
- P3. Los modelos de embeddings MUST ser preentrenados, y la métrica de similitud sobre sus vectores MUST implementarse a mano.
- P4. La IA generativa MUST usarse solo como apoyo, MUST NOT reemplazar el diseño algorítmico ni el análisis formal, y su uso MUST declararse explícitamente.
- P5. El corpus MUST ser únicamente los 20 artículos PDF entregados por el docente, y el procesamiento MUST usar estrictamente el ABSTRACT de cada artículo. Exception: el modelo SPECTER2 MAY recibir el título antepuesto al abstract (`título [SEP] abstract`), y ningún otro algoritmo ni modelo MUST usar el título.
- P6. Cada algoritmo MUST corresponder fielmente a su definición formal, con las fórmulas documentadas (notación incluida) en docstrings o comentarios y verificables a mano contra los casos de estudio.
- P7. El código MUST NOT ocultar pasos: las implementaciones MUST exponer los resultados intermedios (matrices, tablas, hashes, desplazamientos) para reproducirlos en la documentación.
- P8. Todo módulo, clase y función pública MUST tener nombres descriptivos y un docstring que indique qué hace, por qué y su complejidad en tiempo y espacio, y los pasos no triviales MUST llevar comentarios.
- P9. Cada módulo MUST mapearse a un requerimiento y a su demostración paso a paso en el documento técnico, y esa correspondencia MUST estar registrada en `docs/traceability.md`. El documento técnico lo redactan los estudiantes: el flujo SDD y los agentes MUST NOT redactarlo, y solo registran la correspondencia con las secciones marcadas `(pendiente)`.
- P10. Cada algoritmo MUST tener tests unitarios con casos pequeños verificables a mano y casos de borde: cadenas vacías, patrón más largo que el texto, colisiones de hash y abstracts idénticos.
- P11. El preprocesamiento y la ejecución de cada algoritmo MUST medirse en tiempo por separado, y los resultados MUST quedar disponibles para contrastar teoría y práctica.
- P12. El preprocesamiento (normalización, tokenización, stopwords, etc.) MUST ser determinista y estar documentado, y las dependencias y semillas aleatorias MUST estar fijadas.
- P13. El código MUST separarse en módulos independientes de extracción de PDF, preprocesamiento, algoritmos, evaluación, visualización e interfaz.

## 3. Amendments
| Date | Version | Change |
|---|---|---|
| 2026-10-16 | 1.0.0 | Initial version |
| 2026-10-23 | 1.1.0 | P5: excepción para que SPECTER2 reciba `título [SEP] abstract` |
| 2026-10-31 | 1.2.0 | P9: el documento técnico lo redactan los estudiantes; el flujo SDD solo registra la trazabilidad |
