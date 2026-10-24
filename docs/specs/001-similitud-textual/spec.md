# Entorno para el Requerimiento 1: Similitud textual (clásicos e IA)

## 1. Metadata
- Status: Approved
- Updated: 2026-10-23
- Constitution version: 1.1.0
- Required by: 002-ranking-terminos (fase 7)

## 2. Constitution relationship
- Applies:
  - P1: los cuerpos de los 4 algoritmos clásicos y de las 2 métricas sobre embeddings quedan como stubs que escriben a mano los estudiantes; el andamiaje no contiene lógica de esos algoritmos.
  - P2: PDFBox, ONNX Runtime/DJL y Ollama se usan solo para leer PDF y generar embeddings; Recharts, D3 y KaTeX solo para graficar.
  - P3: los dos modelos (SPECTER2 base y nomic-embed-text) son preentrenados; coseno y distancia euclidiana son stubs a mano.
  - P4: el andamiaje se genera con apoyo de IA generativa y se declara así en la guía; los algoritmos los escriben los estudiantes.
  - P5: solo se ingieren los PDF de `corpus/` y solo se compara su ABSTRACT; la excepción de la constitución 1.1.0 permite que SPECTER2 reciba `título [SEP] abstract`, y nomic-embed-text y los algoritmos clásicos usan solo el abstract.
  - P6: cada stub indica en su documentación la definición formal y la notación que el estudiante debe completar.
  - P7: cada algoritmo tiene un contrato de traza con sus resultados intermedios, expuesto por la API y la interfaz.
  - P8: todo elemento público del andamiaje tiene docstring con qué, por qué y complejidad; cada stub pide al estudiante completar la complejidad.
  - P9: la guía global de implementación y `docs/traceability.md` registran requerimiento → módulo → vista → sección del documento técnico → tests.
  - P10: tests con casos pequeños verificables a mano y casos de borde aplicables (ver Conflicts).
  - P11: cada ejecución reporta por separado el tiempo de preprocesamiento y de cada algoritmo; quedan esqueletos de benchmark.
  - P12: el preprocesamiento es determinista y documentado; versiones de dependencias, imagen de Ollama y modelo quedan fijadas.
  - P13: extracción de PDF, preprocesamiento, algoritmos, evaluación, visualización e interfaz quedan en módulos separados.
- Conflicts: P10 exige "patrón más largo que el texto" y "colisiones de hash", que no aplican a estos algoritmos. Resolución acordada: se cubren solo los casos de borde aplicables (secuencias vacías, abstracts idénticos, abstracts disjuntos, longitudes muy distintas), sin enmendar la constitución.

## 3. User stories
| ID | Priority | Actor | Story |
|---|---|---|---|
| US-1 | P1 | Estudiante | As a estudiante, I need un stub por algoritmo (Levenshtein, Needleman-Wunsch, coseno TF-IDF, Jaccard, coseno y euclidiana sobre embeddings) con su contrato de entrada, salida y traza, for escribir a mano solo el cuerpo del algoritmo |
| US-2 | P1 | Estudiante | As a estudiante, I need tests que fallen hasta que implemente cada algoritmo, con casos calculados a mano y oráculos, for verificar mi implementación sin romper el build del resto del proyecto |
| US-3 | P1 | Usuario de la aplicación | As a usuario, I need que el corpus se ingiera con título, autores, abstract y embeddings de ambos modelos, for comparar artículos sin reprocesar los PDF |
| US-4 | P1 | Usuario de la aplicación | As a usuario, I need seleccionar de 2 a 20 artículos y obtener una matriz de similitud NxN por algoritmo, for comparar los enfoques clásicos e IA |
| US-5 | P1 | Usuario de la aplicación | As a usuario, I need ver la traza paso a paso de un algoritmo para un par de artículos, con sus fórmulas, for reproducir el cálculo en el documento técnico |
| US-6 | P1 | Estudiante | As a estudiante, I need una guía global que indique exactamente qué y dónde implementar, for no tener que deducirlo del código |
| US-7 | P2 | Usuario de la aplicación | As a usuario, I need ver qué algoritmos siguen pendientes de implementar sin perder los resultados de los demás, for usar la aplicación mientras se avanza |
| US-8 | P2 | Estudiante | As a estudiante, I need los tiempos de preprocesamiento y de ejecución por algoritmo y esqueletos de benchmark, for contrastar teoría y práctica |

## 4. Requirements
- REQ-1 (US-1): Existen 6 stubs de algoritmo: Levenshtein, Needleman-Wunsch, coseno con TF-IDF, Jaccard, similitud coseno entre vectores y distancia euclidiana entre vectores; cada uno lanza `UnsupportedOperationException` con un mensaje que empieza por "TODO estudiante:" hasta ser implementado.
- REQ-2 (US-1): Cada stub documenta en español qué debe calcular, la fórmula a completar, la traza que debe devolver, el rango del resultado y la complejidad que el estudiante debe declarar.
- REQ-3 (US-1): Levenshtein y Needleman-Wunsch operan sobre una secuencia genérica de símbolos; la elección palabras o caracteres queda como punto de decisión explícito y documentado para los estudiantes.
- REQ-4 (US-1): El andamiaje no contiene ninguna implementación de los 6 algoritmos ni la delega a librerías o a la base de datos.
- REQ-5 (US-2): Cada algoritmo tiene tests con casos pequeños calculados a mano y casos de borde aplicables (secuencias vacías, idénticas, disjuntas, longitudes muy distintas), válidos a nivel de secuencia de símbolos.
- REQ-6 (US-2): Existen tests oráculo, solo en `src/test/**/oracle/`, que comparan Levenshtein contra `levenshtein()` de Postgres y las métricas de vectores contra los operadores de pgvector.
- REQ-7 (US-2): Los tests de los stubs están separados: el comando de test estándar pasa con los stubs sin implementar y un comando aparte ejecuta los tests de los estudiantes.
- REQ-8 (US-3): Al arrancar, la aplicación ingiere los PDF de `corpus/`, extrae título, autores y abstract, y los guarda en la base de datos.
- REQ-9 (US-3): La ingesta genera y guarda dos embeddings por artículo: con SPECTER2 base (`allenai/specter2_base` sin adaptador, exportado a ONNX, versión fijada por commit) sobre `título [SEP] abstract`, tomando el vector del token [CLS] con normalización L2; y con nomic-embed-text (Ollama local) sobre `clustering: <abstract>`.
- REQ-9a (US-3): Si el texto de entrada de SPECTER2 supera 512 tokens, se trunca a 512 y el artículo queda marcado como truncado en la base y en el log de ingesta.
- REQ-10 (US-3): Un artículo sin abstract detectado queda marcado y no aparece como seleccionable.
- REQ-11 (US-3): Los PDF del corpus no se versionan en git.
- REQ-12 (US-3): El preprocesamiento de texto (normalización, tokenización, stopwords) es determinista y está documentado.
- REQ-13 (US-4): La API lista los artículos seleccionables y recibe de 2 a 20 identificadores distintos para comparar.
- REQ-14 (US-4): La respuesta de comparación contiene, por cada uno de los 8 resultados (4 clásicos + 2 modelos × 2 métricas), una matriz NxN de similitudes en [0,1].
- REQ-15 (US-4): Una solicitud con menos de 2 artículos, más de 20, identificadores repetidos o inexistentes se rechaza con HTTP 400 y un mensaje en español.
- REQ-16 (US-5): La API devuelve, para un par de artículos y un algoritmo, el resultado y su traza de pasos intermedios.
- REQ-17 (US-5): La interfaz web permite seleccionar artículos, muestra un mapa de calor por algoritmo y un visor de traza paso a paso con las fórmulas en KaTeX.
- REQ-18 (US-6): `docs/guia-implementacion.md` lista por algoritmo: archivo, método, fórmula, traza esperada, tests a ejecutar y decisiones pendientes, y declara el uso de IA generativa en el andamiaje.
- REQ-19 (US-6): `docs/traceability.md` tiene una fila por módulo de este requerimiento.
- REQ-20 (US-7): Un algoritmo no implementado aparece en la respuesta con estado PENDIENTE y sin valores, y los demás algoritmos se devuelven con normalidad.
- REQ-21 (US-7): La interfaz muestra "Pendiente de implementar: <archivo>#<método>" para cada algoritmo con estado PENDIENTE.
- REQ-22 (US-8): Cada comparación reporta en milisegundos el tiempo de preprocesamiento y el tiempo de ejecución de cada algoritmo, por separado.
- REQ-23 (US-8): Existe un esqueleto de benchmark por algoritmo, listo para ejecutarse cuando el algoritmo esté implementado.

## 5. Acceptance criteria
- AC-1 (US-1): Given el andamiaje recién entregado, when se invoca cualquiera de los 6 algoritmos, then lanza `UnsupportedOperationException` con un mensaje que empieza por "TODO estudiante:".
- AC-2 (US-1): Given el código de producción, when se revisa, then no existe lógica de los 6 algoritmos ni llamadas a librerías o funciones de base de datos que los calculen.
- AC-3 (US-1): Given el stub de Levenshtein o Needleman-Wunsch, when se lee su documentación, then indica que la granularidad (palabras o caracteres) es una decisión del estudiante y dónde se configura.
- AC-4 (US-2): Given los stubs sin implementar, when se ejecuta el comando de test estándar, then termina en verde.
- AC-5 (US-2): Given los stubs sin implementar, when se ejecuta el comando de tests de estudiantes, then todos los tests de algoritmos fallan.
- AC-6 (US-2): Given un algoritmo implementado correctamente, when se ejecutan sus tests de estudiante, then pasan los casos a mano y los casos de borde (vacías, idénticas, disjuntas, longitudes muy distintas).
- AC-7 (US-2): Given un caso a mano conocido (p. ej. secuencias `g,a,t,o` y `p,a,t,o`, distancia de Levenshtein 1), when el test lo evalúa, then compara contra el valor documentado en la guía.
- AC-8 (US-3): Given PDF válidos en `corpus/`, when la aplicación arranca, then cada artículo con abstract detectado queda guardado con título, autores, abstract y los dos embeddings.
- AC-8a (US-3): Given un artículo cuyo texto para SPECTER2 supera 512 tokens, when se ingiere, then el embedding se calcula con los primeros 512 tokens, el artículo queda marcado como truncado y el vector tiene norma L2 igual a 1.
- AC-9 (US-3): Given un PDF sin abstract detectable, when se ingiere, then queda marcado como sin abstract y no aparece en la lista de seleccionables.
- AC-10 (US-3): Given `corpus/` vacío o inexistente, when la aplicación arranca, then arranca y la lista de seleccionables está vacía.
- AC-11 (US-3): Given el corpus ya ingerido, when la aplicación se reinicia, then no se duplican artículos.
- AC-12 (US-3): Given el mismo abstract, when se preprocesa dos veces, then produce exactamente los mismos tokens.
- AC-13 (US-4): Given 3 artículos seleccionables, when se solicita su comparación, then la respuesta contiene 8 matrices 3x3.
- AC-14 (US-4): Given una solicitud con 1 artículo, 21 artículos, ids repetidos o un id inexistente, when se envía, then responde HTTP 400 con mensaje en español.
- AC-15 (US-5): Given un par de artículos y un algoritmo implementado, when se pide su traza, then la API devuelve el resultado y los pasos intermedios, y la interfaz los muestra paso a paso con la fórmula en KaTeX.
- AC-16 (US-6): Given `docs/guia-implementacion.md`, when un estudiante la lee, then encuentra para cada uno de los 6 algoritmos el archivo, método, fórmula, traza esperada y comando de test.
- AC-17 (US-7): Given que solo Jaccard está implementado, when se compara un conjunto de artículos, then Jaccard devuelve su matriz y los otros 5 algoritmos aparecen con estado PENDIENTE.
- AC-18 (US-7): Given un algoritmo con estado PENDIENTE, when la interfaz muestra resultados, then muestra "Pendiente de implementar: <archivo>#<método>" para ese algoritmo.
- AC-19 (US-8): Given una comparación, when responde, then incluye el tiempo de preprocesamiento y el tiempo de ejecución de cada algoritmo en milisegundos, por separado.
- AC-20 (US-8): Given los esqueletos de benchmark, when se ejecutan con algoritmos sin implementar, then no rompen el comando de build estándar.

## 6. Out of scope
- Implementar la lógica de Levenshtein, Needleman-Wunsch, coseno TF-IDF, Jaccard, similitud coseno y distancia euclidiana (la escriben los estudiantes).
- Elegir la granularidad palabras/caracteres de las distancias de edición (decisión diferida a los estudiantes).
- Clustering, dendrogramas y demás requerimientos del proyecto.
- Análisis de tiempos con JMH (requerimiento aparte); aquí solo quedan los esqueletos.
- Subida de PDF desde la interfaz, autenticación y APIs de embeddings remotas con clave.
- Versionar los PDF del corpus.

## 7. Clarifications
- 2026-10-18. Q: ¿Qué implementan los estudiantes y qué el andamiaje? A: Los estudiantes escriben solo el cuerpo de los 4 algoritmos clásicos y de las 2 métricas sobre embeddings; preprocesamiento, pooling + L2 de los embeddings y detección del abstract los implementa el andamiaje.
- 2026-10-18. Q: ¿Cuáles son los 4 algoritmos clásicos? A: Levenshtein, Needleman-Wunsch, coseno con TF-IDF y Jaccard.
- 2026-10-18. Q: ¿Cómo se aplica P10? A: Solo casos de borde aplicables (vacías, idénticas, disjuntas, longitudes muy distintas); sin enmendar la constitución.
- 2026-10-18. Q: ¿Distancia de edición por palabras o caracteres? A: Se definirá al implementar; los stubs operan sobre secuencias genéricas y los casos a mano son válidos a nivel de secuencia.
- 2026-10-18. Q: ¿Segundo modelo de IA? A: Ollama local con nomic-embed-text, servicio en Compose, versión fijada, sin clave.
- 2026-10-18. Q: ¿Métricas sobre embeddings? A: Coseno y euclidiana, ambas como stubs, aplicadas a ambos modelos (4 resultados IA por par).
- 2026-10-18. Q: ¿Ingesta y persistencia del corpus? A: Al arrancar desde `corpus/`; título, autores y abstract en Postgres y embeddings precalculados en pgvector (solo almacenamiento); PDF fuera de git; artículos sin abstract marcados y excluidos.
- 2026-10-18. Q: ¿Cómo se manejan los tests rojos? A: Etiquetados como tests de estudiante y ejecutados con una tarea aparte; el comando de test estándar queda en verde.
- 2026-10-18. Q: ¿Quién define los valores esperados? A: El andamiaje, con casos calculados a mano y oráculos en `src/test/**/oracle/` (Postgres `levenshtein()`, operadores de pgvector).
- 2026-10-18. Q: ¿Cómo se miden los tiempos? A: Por ejecución, preprocesamiento y cada algoritmo por separado en la respuesta, más esqueletos de benchmark JMH.
- 2026-10-18. Q: ¿Qué recibe y devuelve una comparación? A: De 2 a 20 artículos distintos, una matriz NxN por algoritmo en [0,1]; la traza detallada se pide por par y algoritmo; entradas inválidas responden 400.
- 2026-10-18. Q: ¿Qué se muestra con un stub sin implementar? A: Resultado parcial con estado PENDIENTE; la interfaz muestra "Pendiente de implementar: <archivo>#<método>".
- 2026-10-18. Q: ¿Alcance del frontend? A: Interfaz completa: selector, mapa de calor por algoritmo y visor de traza paso a paso con KaTeX.
- 2026-10-18. Q: ¿Dónde vive la guía para estudiantes? A: Global en `docs/guia-implementacion.md`, más filas en `docs/traceability.md`.
- 2026-10-18. Q: ¿Uso de IA generativa (P4)? A: El andamiaje es asistido por IA y se declara; los algoritmos los escriben los estudiantes.
- 2026-10-23. Q: ¿Qué modelo ONNX reemplaza a all-MiniLM-L6-v2? A: SPECTER2 base (`allenai/specter2_base`, sin adaptador), 768 dimensiones, hasta 512 tokens; Word2Vec/FastText no son obligatorios y se descartan.
- 2026-10-23. Q: ¿Qué texto recibe SPECTER2? A: `título [SEP] abstract`; implica enmendar P5 (constitución 1.1.0) con una excepción solo para SPECTER2. nomic-embed-text y los clásicos usan solo el abstract.
- 2026-10-23. Q: ¿Pooling de SPECTER2 y abstracts largos? A: Vector del token [CLS] con normalización L2 (reemplaza el mean pooling); si supera 512 tokens se trunca y se marca el artículo.
- 2026-10-23. Q: ¿Prefijo de nomic-embed-text? A: `clustering: ` antepuesto al abstract.
