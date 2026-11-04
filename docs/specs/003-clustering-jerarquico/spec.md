# Entorno para el Requerimiento 3: Clustering jerárquico con dendrograma

## 1. Metadata
- Status: Approved
- Updated: 2026-11-02
- Constitution version: 1.2.0
- Required by: none

## 2. Constitution relationship
- Applies:
  - P1: los cuerpos de Single Linkage, Complete Linkage, Average Linkage (UPGMA) y DIANA quedan como stubs que escriben a mano los estudiantes; el andamiaje no contiene lógica de esos algoritmos ni usa librerías de clustering.
  - P2: D3 solo dibuja el dendrograma que devuelve el backend y KaTeX solo muestra fórmulas; ninguna librería calcula fusiones, distancias entre grupos ni alturas.
  - P3: con una fuente de embeddings, la similitud de entrada la calculan los stubs a mano del Requerimiento 1; el clustering no agrega modelos.
  - P5: el clustering usa solo las similitudes del Requerimiento 1, que se calculan sobre el ABSTRACT (y `título [SEP] abstract` solo en las fuentes SPECTER2, por la excepción de la constitución 1.1.0).
  - P6: cada stub indica en su documentación la definición formal (distancia entre grupos, recálculo de la fila y altura de cada fusión) y la notación que el estudiante debe completar.
  - P7: cada algoritmo tiene un contrato de traza con la matriz de distancias entre grupos de cada paso, expuesto por la API y la interfaz.
  - P8: todo elemento público del andamiaje tiene docstring con qué, por qué y complejidad; cada stub pide al estudiante completar la complejidad.
  - P9: `docs/guia-implementacion.md` y `docs/traceability.md` registran requerimiento → módulo → vista → sección del documento técnico `(pendiente)` → tests.
  - P10: tests con casos pequeños verificables a mano y casos de borde aplicables (ver Conflicts).
  - P11: cada ejecución reporta por separado el tiempo de preparación (preprocesamiento y matriz de similitud) y el de cada algoritmo de clustering.
  - P12: la regla de desempate es fija y documentada, de modo que la misma entrada produce siempre el mismo dendrograma; no hay aleatoriedad.
  - P13: algoritmos de clustering, trazas, API y visualización quedan en módulos separados; el preprocesamiento y la similitud se reutilizan del Requerimiento 1 sin reimplementarlos.
- Conflicts: P10 exige "cadenas vacías", "patrón más largo que el texto" y "colisiones de hash", que no aplican a clustering. Resolución acordada (igual que en la spec 001): se cubren solo los casos de borde aplicables (2 artículos, abstracts idénticos, distancias empatadas, 20 artículos), sin enmendar la constitución.

## 3. User stories
| ID | Priority | Actor | Story |
|---|---|---|---|
| US-1 | P1 | Estudiante | As a estudiante, I need un stub por algoritmo aglomerativo (Single, Complete y Average Linkage) con su contrato de entrada, salida y traza, for escribir a mano solo el cuerpo del algoritmo |
| US-2 | P3 | Estudiante | As a estudiante, I need un stub del algoritmo divisivo DIANA con el mismo contrato, for implementarlo de forma recursiva si hay tiempo |
| US-3 | P1 | Estudiante | As a estudiante, I need tests que fallen hasta que implemente cada algoritmo, con casos calculados a mano, for verificar mi implementación sin romper el build del resto del proyecto |
| US-4 | P1 | Usuario de la aplicación | As a usuario, I need agrupar los artículos de la comparación actual con una fuente de similitud elegida y ver un dendrograma por algoritmo, for comparar los agrupamientos que produce cada algoritmo |
| US-5 | P1 | Usuario de la aplicación | As a usuario, I need ver la traza paso a paso de cada algoritmo con la matriz de distancias entre grupos y sus fórmulas, for reproducir el agrupamiento en el documento técnico |
| US-6 | P2 | Usuario de la aplicación | As a usuario, I need ver qué algoritmos o fuentes siguen pendientes sin perder los demás dendrogramas, for usar la aplicación mientras se avanza |
| US-7 | P1 | Estudiante | As a estudiante, I need una sección de la guía de implementación que indique exactamente qué y dónde implementar, for no tener que deducirlo del código |
| US-8 | P2 | Estudiante | As a estudiante, I need el tiempo de preparación y el de cada algoritmo por separado, for contrastar teoría y práctica |

## 4. Requirements
- REQ-1 (US-1): Existen 3 stubs aglomerativos: Single Linkage, Complete Linkage y Average Linkage (UPGMA); cada uno lanza `UnsupportedOperationException` con un mensaje que empieza por "TODO estudiante:" hasta ser implementado.
- REQ-2 (US-2): Existe un stub del algoritmo divisivo DIANA (top-down, recursivo) que lanza `UnsupportedOperationException` con un mensaje que empieza por "TODO estudiante:"; su implementación es opcional.
- REQ-3 (US-1, US-2): Cada stub recibe una matriz de distancias N×N simétrica, con diagonal 0 y valores en [0,1], y devuelve un dendrograma de exactamente N − 1 fusiones (grupo A, grupo B, altura, tamaño del grupo resultante) más su traza.
- REQ-4 (US-1, US-2): Cada stub documenta en español la definición formal de la distancia entre grupos (mínima, máxima, promedio; en DIANA, diámetro y distancia media para elegir el grupo disidente), la fórmula de recálculo, la traza que debe devolver, el rango de las alturas y la complejidad que el estudiante debe declarar.
- REQ-5 (US-1, US-2): La regla de desempate es fija y documentada en cada stub: ante distancias iguales gana el par de grupos con los identificadores menores en orden lexicográfico (en DIANA, el grupo y el elemento de menor identificador).
- REQ-6 (US-1, US-2): El andamiaje no contiene ninguna implementación de los 4 algoritmos ni la delega a librerías, a la base de datos o a la interfaz.
- REQ-7 (US-1, US-2): El andamiaje valida el dendrograma que devuelve un algoritmo implementado (N − 1 fusiones, cada grupo fusionado una sola vez, tamaños coherentes, alturas en [0,1]) y responde HTTP 500 con un mensaje en español si no es válido.
- REQ-8 (US-3): Cada algoritmo tiene tests con casos pequeños calculados a mano y casos de borde aplicables: 2 artículos (una sola fusión), abstracts idénticos (altura 0), distancias empatadas (desempate de REQ-5) y 20 artículos (19 fusiones).
- REQ-9 (US-3): Los tests de los stubs están separados: el comando de test estándar pasa con los stubs sin implementar y el comando de tests de estudiantes ejecuta los tests de clustering.
- REQ-10 (US-4): La distancia de entrada es d = 1 − s, donde s es la similitud en [0,1] de la fuente elegida, calculada por el Requerimiento 1 sin reimplementar el preprocesamiento ni la similitud.
- REQ-11 (US-4): La API recibe los identificadores de 2 a 20 artículos distintos y una de las 8 fuentes de similitud del Requerimiento 1, y devuelve para cada uno de los 4 algoritmos su dendrograma y su estado; la traza de un algoritmo se pide aparte con los mismos artículos y la misma fuente.
- REQ-12 (US-4): Una solicitud con menos de 2 artículos, más de 20, identificadores repetidos o inexistentes, o una fuente desconocida, se rechaza con HTTP 400 y un mensaje en español.
- REQ-13 (US-4): La página de similitud textual tiene una sección de clustering que aparece tras una comparación, agrupa los mismos artículos de esa comparación y ofrece un selector con las 8 fuentes, con SPECTER2 (coseno) por defecto.
- REQ-14 (US-4): La sección muestra los dendrogramas de los 4 algoritmos a la vez en una cuadrícula de 2×2 (una columna en pantallas angostas), con el mismo eje de altura [0, mayor altura de las fusiones de los 4 resultados] y una hoja por artículo etiquetada con su título.
- REQ-15 (US-4): La interfaz solo dibuja el dendrograma que devuelve la API; no calcula fusiones, alturas, cortes ni grupos.
- REQ-16 (US-5): La traza de cada algoritmo contiene, por cada paso, la matriz de distancias entre los grupos vigentes, el par fusionado (o el grupo dividido en DIANA), su distancia y la fila de distancias recalculada.
- REQ-17 (US-5): La interfaz muestra la traza de un algoritmo paso a paso con la fórmula de su distancia entre grupos en KaTeX.
- REQ-18 (US-6): Un algoritmo no implementado aparece con estado PENDIENTE, sin dendrograma ni traza, y la interfaz muestra "Pendiente de implementar: <archivo>#<método>"; los demás algoritmos se devuelven con normalidad.
- REQ-19 (US-6): Si la fuente de similitud elegida está pendiente de implementar, la respuesta lo indica con su ubicación "<archivo>#<método>", no devuelve dendrogramas y la interfaz muestra el aviso en la sección.
- REQ-20 (US-7): `docs/guia-implementacion.md` tiene una sección del Requerimiento 3 que lista por algoritmo: archivo, método, fórmula, traza esperada, regla de desempate, tests a ejecutar y caso a mano, y declara el uso de IA generativa en el andamiaje.
- REQ-21 (US-7): `docs/traceability.md` tiene una fila por módulo de este requerimiento, con la sección del documento técnico marcada `(pendiente)`.
- REQ-22 (US-8): Cada solicitud de clustering reporta en milisegundos el tiempo de preparación (preprocesamiento y matriz de similitud) y el tiempo de ejecución de cada algoritmo, por separado.

## 5. Acceptance criteria
- AC-1 (US-1, US-2): Given el andamiaje recién entregado, when se invoca cualquiera de los 4 algoritmos, then lanza `UnsupportedOperationException` con un mensaje que empieza por "TODO estudiante:".
- AC-2 (US-1, US-2): Given el código de producción y el de la interfaz, when se revisan, then no existe lógica de los 4 algoritmos ni llamadas a librerías que calculen clustering.
- AC-3 (US-3): Given los stubs sin implementar, when se ejecuta el comando de test estándar, then termina en verde.
- AC-4 (US-3): Given los stubs sin implementar, when se ejecuta el comando de tests de estudiantes, then todos los tests de clustering fallan.
- AC-5 (US-3): Given la matriz de 4 puntos de la guía, when un algoritmo implementado correctamente la agrupa, then sus fusiones y alturas coinciden con las calculadas a mano en la guía para ese algoritmo.
- AC-6 (US-3): Given 2 artículos con distancia d, when se agrupan con cualquiera de los 4 algoritmos, then el dendrograma tiene una sola fusión de altura d y tamaño 2.
- AC-7 (US-3): Given dos artículos con abstracts idénticos, when se agrupan, then se fusionan a altura 0.
- AC-8 (US-3): Given una matriz con todas las distancias fuera de la diagonal iguales, when se agrupa, then las fusiones siguen la regla de desempate de REQ-5 y dos ejecuciones devuelven el mismo dendrograma.
- AC-9 (US-3): Given 20 artículos, when se agrupan con un algoritmo implementado, then el dendrograma tiene 19 fusiones y la última tiene tamaño 20.
- AC-10 (US-1): Given un algoritmo que devuelve un dendrograma inválido (p. ej. 3 fusiones para 3 artículos o un grupo fusionado dos veces), when se pide el clustering, then la API responde HTTP 500 con un mensaje en español.
- AC-11 (US-4): Given 5 artículos comparados y la fuente SPECTER2 (coseno) implementada, when se pide el clustering, then la respuesta contiene 4 resultados con un dendrograma de 4 fusiones por cada algoritmo implementado.
- AC-12 (US-4): Given una solicitud con 1 artículo, 21 artículos, ids repetidos, un id inexistente o una fuente desconocida, when se envía, then responde HTTP 400 con mensaje en español.
- AC-13 (US-4): Given una comparación hecha en la página de similitud textual, when el usuario cambia la fuente en la sección de clustering, then se dibujan los 4 resultados para esa fuente con los mismos artículos de la comparación.
- AC-14 (US-4): Given dendrogramas de algoritmos distintos, when se muestran, then comparten el eje de altura [0, mayor altura de sus fusiones] y cada hoja muestra el título de su artículo.
- AC-15 (US-5): Given un algoritmo implementado, when el usuario abre su traza, then la interfaz muestra cada paso con la matriz entre grupos, el par fusionado, su distancia, la fila recalculada y la fórmula en KaTeX.
- AC-16 (US-6): Given que solo Average Linkage está implementado, when se pide el clustering, then Average devuelve su dendrograma y los otros 3 aparecen como "Pendiente de implementar: <archivo>#<método>".
- AC-17 (US-6): Given una fuente de similitud cuyo stub del Requerimiento 1 no está implementado, when se pide el clustering con esa fuente, then la sección muestra que la fuente está pendiente con su "<archivo>#<método>" y no dibuja dendrogramas.
- AC-18 (US-7): Given `docs/guia-implementacion.md`, when un estudiante la lee, then encuentra para cada uno de los 4 algoritmos el archivo, método, fórmula, traza esperada, regla de desempate, caso a mano y comando de test.
- AC-19 (US-8): Given una solicitud de clustering, when responde, then incluye el tiempo de preparación y el de cada algoritmo implementado en milisegundos, por separado.

## 6. Out of scope
- Implementar la lógica de Single, Complete y Average Linkage y de DIANA (la escriben los estudiantes).
- Ward y otros métodos de enlace.
- Métricas de calidad o exactitud del agrupamiento (p. ej. correlación cofenética); la comparación entre algoritmos queda cualitativa en el documento técnico.
- Cortar el dendrograma por altura o por número de grupos, y colorear grupos.
- Mediciones con JMH.
- Agrupar artículos distintos de los de la comparación actual o combinar varias fuentes de similitud.

## 7. Clarifications
- 2026-11-02. Q: ¿Qué numeración aplica? A: Es el Requerimiento 3 de la numeración vigente (el PDF del docente lo llama Requerimiento 2); spec 003.
- 2026-11-02. Q: ¿Qué tres algoritmos aglomerativos? A: Single, Complete y Average Linkage (UPGMA): los tres sirven con cualquier matriz d = 1 − s de las 8 fuentes y comparten la fórmula de Lance-Williams. Ward se descarta porque solo es válido con distancias euclidianas.
- 2026-11-02. Q: ¿Algoritmo top-down? A: DIANA como stub opcional (P3), divisivo y recursivo, solo con la matriz de distancias y determinista.
- 2026-11-02. Q: ¿Sobre qué matriz se agrupa? A: El usuario elige 1 de las 8 fuentes del Requerimiento 1, SPECTER2 (coseno) por defecto; si la fuente está pendiente se avisa.
- 2026-11-02. Q: ¿Métrica de exactitud? A: Ninguna; la comparación es cualitativa en el documento técnico.
- 2026-11-02. Q: ¿Qué artículos se agrupan? A: Los mismos de la comparación actual (2 a 20); el backend recalcula la matriz de la fuente elegida.
- 2026-11-02. Q: ¿Qué traza devuelve cada algoritmo? A: La matriz de distancias entre grupos de cada paso, el par fusionado (o el grupo dividido), su distancia y la fila recalculada.
- 2026-11-02. Q: ¿Corte del dendrograma? A: No; la interfaz solo dibuja.
- 2026-11-02. Q: ¿Disposición de los dendrogramas? A: Los 4 a la vez en una cuadrícula de 2×2 con escala de altura común [0,1].
- 2026-11-04. Q: ¿Sigue siendo [0,1] la escala común? A: No; con distancias de embeddings todas las fusiones caían en [0, 0.05] y los grupos no se distinguían. Ahora el eje es [0, M], con M la mayor altura de las fusiones de los resultados CALCULADOS redondeada hacia arriba a un valor limpio (1 si ninguna es positiva), común a las 4 tarjetas. Reemplaza la respuesta anterior.
- 2026-11-02. Q: ¿La traza viene con los dendrogramas? A: No; se pide aparte por algoritmo, con los mismos artículos y fuente.
- 2026-11-02. Q: ¿Cómo se manejan los tests rojos y el aviso de pendiente? A: Igual que en la spec 001: etiqueta de tests de estudiante aparte, estado PENDIENTE con "<archivo>#<método>".
