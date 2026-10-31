# Requerimiento 2: Ranking de términos mediante algoritmos de ordenamiento

## 1. Metadata
- Status: Approved
- Updated: 2026-10-23
- Constitution version: 1.1.0
- Required by: none

## 2. Constitution relationship
- Applies:
  - P1: Counting Sort, Radix Sort y Bucket Sort (incluido el insertion sort de cada cubeta) se implementan a mano con arreglos y listas básicas; no se usan `Collections.sort`, `Arrays.sort` ni librerías de ordenamiento en producción. `TreeMap<String,Integer>` se acepta solo como estructura auxiliar para armar la entrada en orden alfabético descendente (REQ-2).
  - P2: JMH se usa solo para medir tiempos; Recharts solo para graficar.
  - P4: las implementaciones se basan en el código de referencia entregado por el usuario; la IA generativa solo lo adapta a pares (término, frecuencia) y a la traza, y su uso lo declaran los estudiantes en el documento técnico.
  - P5: las frecuencias se calculan solo sobre los ABSTRACT del corpus de `corpus/`.
  - P6: cada algoritmo documenta su definición formal, fórmulas y notación (arreglo de conteo, acumulado, dígito `⌊v/exp⌋ mod 10`, índice de cubeta `⌊(v − min)/bucketSize⌋`).
  - P7: cada algoritmo devuelve una traza con sus resultados intermedios, expuesta por la API y la interfaz.
  - P8: todo elemento público tiene docstring con qué, por qué y complejidad, con notación separada por algoritmo: Counting O(n+k), k = frecuencia máxima; Radix O(d·(n+b)), d = dígitos del máximo, b = 10; Bucket O(n+m) promedio y O(n²) peor caso, m = número de cubetas.
  - P9: `docs/traceability.md` registra requerimiento → módulo → vista → sección del documento técnico → tests.
  - P10: tests con casos pequeños verificables a mano y casos de borde aplicables (ver Conflicts).
  - P11: cada ejecución reporta por separado el tiempo de preprocesamiento y el de cada algoritmo; JMH mide los algoritmos y sus resultados quedan guardados y visibles junto a la complejidad teórica.
  - P12: reutiliza el preprocesamiento determinista de la spec 001; parámetros de JMH fijos y documentados.
  - P13: algoritmos, evaluación (JMH), persistencia, API e interfaz quedan en módulos separados.
- Conflicts:
  - P1: el Bucket Sort de referencia usa `Collections.sort` en cada cubeta. Resolución acordada: cada cubeta se ordena con insertion sort escrito a mano, cuya traza se expone.
  - P10: "patrón más largo que el texto" y "colisiones de hash" no aplican a ordenamiento. Resolución acordada: se cubren solo los casos de borde aplicables (lista vacía, un término, frecuencias iguales, ya ordenada, orden inverso, frecuencia máxima mucho mayor que el número de términos), sin enmendar la constitución.

## 3. User stories
| ID | Priority | Actor | Story |
|---|---|---|---|
| US-1 | P1 | Usuario de la aplicación | As a usuario, I need un ranking descendente de los términos más frecuentes del corpus completo, for conocer los conceptos dominantes de los abstracts |
| US-2 | P1 | Estudiante | As a estudiante, I need Counting Sort, Radix Sort y Bucket Sort implementados a partir del código de referencia y aplicados a la lista de frecuencias, for comparar algoritmos de ordenamiento no basados en comparaciones |
| US-3 | P1 | Usuario de la aplicación | As a usuario, I need ver la traza paso a paso de cada algoritmo sobre el corpus, for reproducir el ordenamiento en el documento técnico |
| US-4 | P1 | Usuario de la aplicación | As a usuario, I need lanzar desde la interfaz una medición JMH de los tres algoritmos y ver sus tiempos en milisegundos junto a su complejidad teórica, for contrastar teoría y práctica |
| US-5 | P1 | Estudiante | As a estudiante, I need tests con casos a mano, casos de borde y un oráculo, for verificar que los tres algoritmos ordenan correctamente |
| US-6 | P2 | Usuario de la aplicación | As a usuario, I need consultar el historial de mediciones JMH, for comparar corridas anteriores |

## 4. Requirements
- REQ-1 (US-1): La frecuencia de un término es su número total de ocurrencias en todos los abstracts seleccionables del corpus, tras el preprocesamiento de la spec 001.
- REQ-2 (US-1): El ranking ordena los términos por frecuencia descendente; los términos con igual frecuencia quedan en orden alfabético ascendente. Para lograrlo, la entrada de los algoritmos se arma en orden alfabético descendente recorriendo un `TreeMap<String,Integer>` de frecuencias con el orden natural de `String` (`compareTo`, el de TreeMap por defecto) sobre los tokens ya preprocesados, documentado; los algoritmos ordenan ascendente y de forma estable por frecuencia, como el código de referencia, y el ranking se obtiene leyendo su salida de derecha a izquierda.
- REQ-3 (US-1): La API recibe `N` (entero ≥ 1, default 20) y devuelve los N primeros términos del ranking con su frecuencia; si `N` es mayor que el número de términos, devuelve todos los términos sin error. Los algoritmos ordenan siempre la lista completa.
- REQ-4 (US-1): Una solicitud con `N` o `bucketSize` no entero o menor que 1 se rechaza con HTTP 400 y un mensaje en español.
- REQ-5 (US-1): La interfaz muestra el top N como tabla y como gráfico de barras.
- REQ-6 (US-2): Counting Sort sigue el código de referencia: arreglo de conteo de tamaño max+1, conteo acumulado y construcción de la salida recorriendo la entrada de derecha a izquierda (estable).
- REQ-7 (US-2): Radix Sort sigue el código de referencia: LSD en base 10, una pasada de Counting Sort estable por dígito hasta el dígito más significativo del máximo.
- REQ-8 (US-2): Bucket Sort sigue el código de referencia: mínimo y máximo, `⌊(max − min)/bucketSize⌋ + 1` cubetas, distribución por `⌊(v − min)/bucketSize⌋`, ordenamiento de cada cubeta con insertion sort escrito a mano y concatenación.
- REQ-9 (US-2): `bucketSize` es un parámetro de la API (entero ≥ 1) con default 5, documentado.
- REQ-10 (US-2): Los tres algoritmos ordenan pares (término, frecuencia) usando la frecuencia como clave, y producen exactamente el mismo ranking para la misma entrada.
- REQ-11 (US-2): Una petición de ranking ejecuta los tres algoritmos sobre la misma lista y responde el top N de cada algoritmo, un indicador de si coinciden calculado sobre las listas completas, y en milisegundos el tiempo de preprocesamiento y el de cada algoritmo por separado, medidos con la grabación de traza desactivada.
- REQ-12 (US-2): El código de producción no usa `Collections.sort`, `Arrays.sort`, `List.sort`, `Stream.sorted` ni librerías de ordenamiento; `TreeMap<String,Integer>` se permite solo para armar la entrada de REQ-2.
- REQ-13 (US-3): Counting Sort expone en su traza el máximo, el arreglo de conteo, el arreglo acumulado y la colocación de cada elemento en la salida.
- REQ-14 (US-3): Radix Sort expone en su traza, por cada pasada, el exponente, el dígito de cada elemento, el conteo, el acumulado y el arreglo resultante.
- REQ-15 (US-3): Bucket Sort expone en su traza el mínimo, el máximo, el número de cubetas, el contenido de cada cubeta antes y después del insertion sort, y el arreglo concatenado.
- REQ-16 (US-3): La traza se genera sobre la lista de frecuencias del corpus completo y se pide en un endpoint aparte por algoritmo, no en la respuesta del ranking; ese endpoint acepta `bucketSize` opcional (entero ≥ 1, default 5, mismo HTTP 400 de REQ-4), que solo afecta a Bucket Sort; la interfaz la muestra paso a paso, y todo arreglo de más de 50 elementos se pagina de a 50 mostrando su longitud total.
- REQ-17 (US-4): La interfaz tiene una acción que lanza una medición JMH de los tres algoritmos sobre la lista de frecuencias del corpus completo.
- REQ-18 (US-4): JMH usa parámetros fijos y documentados: 1 fork, 5 iteraciones de calentamiento de 1 s, 10 iteraciones de medición de 1 s, modo tiempo promedio, unidad milisegundos, `bucketSize` default, grabación de traza desactivada.
- REQ-19 (US-4): Solo una medición puede estar en curso; una solicitud mientras otra corre se rechaza con HTTP 409 y el mensaje "Ya hay una medición en curso".
- REQ-20 (US-4): Una medición tiene estado EN_CURSO, COMPLETADA o FALLIDA; la interfaz muestra el estado y deshabilita la acción mientras está EN_CURSO.
- REQ-21 (US-4): Una medición que falla o supera 10 minutos queda FALLIDA con un mensaje en español, y la última medición COMPLETADA sigue visible. Al arrancar la aplicación, toda medición EN_CURSO pasa a FALLIDA con el mensaje "Medición interrumpida por reinicio de la aplicación".
- REQ-22 (US-4): El resultado de una medición COMPLETADA muestra por algoritmo el tiempo promedio como ms ± error, donde el error es el scoreError de JMH (semiamplitud del intervalo de confianza al 99,9 %), el número de términos n, los parámetros propios del algoritmo (Counting: k = frecuencia máxima; Radix: d = dígitos del máximo y b = 10; Bucket: m = número de cubetas) y su complejidad teórica.
- REQ-23 (US-4): Sin ninguna medición COMPLETADA, la interfaz muestra "Sin mediciones JMH" y la acción para lanzarla.
- REQ-24 (US-5): Cada algoritmo tiene tests con casos pequeños calculados a mano y casos de borde: lista vacía, un término, todas las frecuencias iguales, ya ordenada, orden inverso y frecuencia máxima mucho mayor que el número de términos.
- REQ-25 (US-5): Existen tests oráculo, solo en `src/test/**/oracle/`, que comparan el ranking de cada algoritmo contra un ordenamiento de la librería estándar con el mismo criterio.
- REQ-26 (US-5): Existe un test que verifica que los tres algoritmos producen el mismo ranking sobre el corpus; se omite con Assumptions de JUnit, indicando el motivo en el reporte, cuando `corpus/` no tiene PDF, y corre cuando el corpus está presente.
- REQ-27 (US-6): Cada medición se guarda en la base de datos con fecha, estado, parámetros JMH, n y los resultados por algoritmo con sus parámetros propios (k; d y b; m).
- REQ-28 (US-6): La interfaz lista todas las mediciones anteriores en una lista completa sin paginar, de la más reciente a la más antigua.

## 5. Acceptance criteria
- AC-1 (US-1): Given abstracts preprocesados con tokens `model` ×42, `data` ×30 y `graph` ×30, when se pide el ranking, then la entrada de los algoritmos es [`model` 42, `graph` 30, `data` 30] y el ranking es `model` (42), `data` (30), `graph` (30).
- AC-2 (US-1): Given el corpus ingerido, when se pide el ranking sin `N`, then responde los 20 primeros términos con su frecuencia.
- AC-3 (US-1): Given un corpus con 10 términos, when se pide con `N` = 0, `N` = "abc", `bucketSize` = 0 o `bucketSize` = "abc", then responde HTTP 400 con mensaje en español.
- AC-4 (US-1): Given `corpus/` vacío o sin abstracts seleccionables, when se pide el ranking, then responde un ranking vacío sin error y la interfaz muestra "Sin términos en el corpus".
- AC-5 (US-1): Given un ranking, when la interfaz lo muestra, then presenta el top N como tabla y como gráfico de barras.
- AC-6 (US-2): Given la misma lista de frecuencias, when se ejecutan los tres algoritmos, then los tres rankings completos son idénticos y el indicador de coincidencia es verdadero.
- AC-7 (US-2): Given una petición de ranking, when responde, then incluye el top N de cada algoritmo, sin trazas, y el tiempo de preprocesamiento y el de cada algoritmo en milisegundos, por separado, medidos con la grabación de traza desactivada.
- AC-8 (US-2): Given el código de producción, when se revisa, then no hay llamadas a `Collections.sort`, `Arrays.sort`, `List.sort`, `Stream.sorted` ni a librerías de ordenamiento.
- AC-9 (US-2): Given frecuencias [5, 3, 8, 3] y `bucketSize` = 2, when se ejecuta Bucket Sort, then crea 3 cubetas, [3, 3] en la cubeta 0, [5] en la 1 y [8] en la 2.
- AC-10 (US-3): Given frecuencias [4, 1, 3, 1], when se ejecuta Counting Sort, then la traza muestra conteo [0, 2, 0, 1, 1] y acumulado [0, 2, 2, 3, 4].
- AC-11 (US-3): Given frecuencias [170, 45, 75, 90, 802, 24, 2, 66], when se ejecuta Radix Sort, then la traza tiene 3 pasadas (exp = 1, 10, 100) y el arreglo tras la pasada de exp = 1 es [170, 90, 802, 2, 24, 45, 75, 66].
- AC-12 (US-3): Given el ranking del corpus, when el usuario abre la traza de un algoritmo, then la interfaz muestra sus pasos en orden y un arreglo de 88 elementos aparece en 2 páginas (0-49 y 50-87) con "longitud 88".
- AC-13 (US-4): Given ninguna medición en curso, when el usuario lanza una medición, then su estado pasa a EN_CURSO, la acción queda deshabilitada y al terminar el estado es COMPLETADA y muestra por algoritmo ms ± scoreError, n, sus parámetros propios (k; d y b; m) y su complejidad teórica (O(n+k), O(d·(n+b)), O(n+m)).
- AC-14 (US-4): Given una medición EN_CURSO, when se solicita otra, then responde HTTP 409 con "Ya hay una medición en curso".
- AC-15 (US-4): Given una medición que falla o supera 10 minutos, when termina, then queda FALLIDA con mensaje en español y la última medición COMPLETADA sigue visible.
- AC-16 (US-4): Given ninguna medición COMPLETADA, when el usuario abre la vista de tiempos, then ve "Sin mediciones JMH" y la acción para lanzarla.
- AC-17 (US-5): Given cada algoritmo, when se ejecutan sus tests, then pasan los casos a mano y los casos de borde (vacía, un término, frecuencias iguales, ya ordenada, inversa, k ≫ n).
- AC-18 (US-5): Given una lista vacía, when se ejecuta cualquiera de los tres algoritmos, then devuelve una lista vacía sin excepción.
- AC-19 (US-5): Given los tests oráculo, when se ejecutan, then el ranking de cada algoritmo es igual al del ordenamiento de la librería estándar.
- AC-20 (US-6): Given tres mediciones realizadas, when el usuario abre el historial, then las ve ordenadas de la más reciente a la más antigua con su fecha, estado y resultados.
- AC-21 (US-6): Given mediciones guardadas, when la aplicación se reinicia, then el historial se conserva.
- AC-22 (US-1): Given un corpus con 10 términos, when se pide el ranking sin `N` o con `N` = 11, then responde los 10 términos sin error.
- AC-23 (US-3): Given el corpus ingerido, when se pide la traza de un algoritmo en su endpoint, then responde la traza completa de ese algoritmo sobre la lista de frecuencias del corpus completo.
- AC-24 (US-4): Given una medición EN_CURSO, when la aplicación se reinicia, then esa medición queda FALLIDA con "Medición interrumpida por reinicio de la aplicación" y se puede lanzar una nueva sin HTTP 409.
- AC-25 (US-3): Given el corpus ingerido, when se pide la traza de Bucket Sort con `bucketSize` = 10, then la traza tiene ⌊(max − min)/10⌋ + 1 cubetas; sin `bucketSize` usa 5; con `bucketSize` = 0 o "abc" responde HTTP 400; en Counting y Radix `bucketSize` no cambia la traza.
- AC-26 (US-5): Given `corpus/` sin PDF, when se ejecuta el comando de test estándar, then el test sobre el corpus aparece omitido con el motivo en el reporte y el build no falla; given el corpus presente, then el test se ejecuta.
- AC-27 (US-6): Given 30 mediciones guardadas, when el usuario abre el historial, then ve las 30 en una sola lista sin paginar, de la más reciente a la más antigua.

## 6. Out of scope
- Mediciones JMH sobre listas sintéticas o tamaños distintos al corpus completo.
- Editar los parámetros de JMH desde la interfaz.
- Encolar mediciones JMH.
- N-gramas, frecuencia documental o ponderaciones (TF-IDF) como criterio de ranking.
- Algoritmos de ordenamiento por comparación como competidores en la medición.
- Radix Sort en bases distintas de 10.
- Cambios al preprocesamiento o a la ingesta definidos en la spec 001.

## 7. Clarifications
- 2026-10-23. Q: ¿Quién escribe los tres algoritmos? A: Implementación completa basada en el código de referencia del usuario, adaptada a pares (término, frecuencia) y con traza.
- 2026-10-23. Q: ¿Cómo se ordena cada cubeta de Bucket Sort, dado que `Collections.sort` choca con P1? A: Con insertion sort escrito a mano, con traza expuesta.
- 2026-10-23. Q: ¿Qué es la frecuencia y cómo se desempata? A: Ocurrencias totales en todos los abstracts tras preprocesar; orden descendente; empates en orden alfabético ascendente.
- 2026-10-23. Q: ¿Tamaño del listado? A: Los algoritmos ordenan todos los términos; la API acepta N (default 20) y la interfaz muestra tabla y gráfico de barras del top N.
- 2026-10-23. Q: ¿Dónde quedan los resultados JMH? A: Guardados y servidos por la API; la interfaz los muestra con ms ± error y la complejidad teórica.
- 2026-10-23. Q: ¿Sobre qué entradas corre JMH? A: Solo la lista de frecuencias del corpus completo.
- 2026-10-23. Q: ¿Qué traza se muestra? A: La del corpus completo, paginando o colapsando arreglos largos.
- 2026-10-23. Q: ¿Cómo se fija `bucketSize`? A: Parámetro de la API, entero ≥ 1, default 5; JMH usa el default.
- 2026-10-23. Q: ¿Cómo se aplica P10? A: Solo casos de borde aplicables (vacía, un término, frecuencias iguales, ya ordenada, inversa, k ≫ n); sin enmendar la constitución.
- 2026-10-23. Q: ¿Cómo se ejecutan los algoritmos? A: Los tres en una misma petición, con tres rankings, indicador de coincidencia y tiempos por separado.
- 2026-10-23. Q: ¿Base de Radix Sort? A: Base 10, LSD, como el código de referencia.
- 2026-10-23. Q: ¿Cómo se lanza JMH? A: Desde la interfaz.
- 2026-10-23. Q: ¿Mediciones concurrentes? A: Solo una a la vez; la segunda se rechaza con HTTP 409.
- 2026-10-23. Q: ¿Qué mediciones se conservan? A: Historial completo en la base de datos.
- 2026-10-23. Q: ¿Parámetros de JMH? A: Fijos y documentados: 1 fork, 5 × 1 s de calentamiento, 10 × 1 s de medición, tiempo promedio en ms.
- 2026-10-23. Q: ¿Qué pasa si la medición falla? A: Límite de 10 minutos; queda FALLIDA con mensaje en español y se conserva la última COMPLETADA.
- 2026-10-23. Q: ¿Cómo se obtiene el orden descendente con desempate alfabético si los algoritmos de referencia ordenan ascendente y REQ-12 prohíbe las funciones de ordenamiento de la librería? A: Los algoritmos ordenan ascendente y estable, como la referencia (las trazas de AC-9/10/11 no cambian); la entrada se arma en orden alfabético descendente recorriendo un `TreeMap<String,Integer>` (estructura auxiliar aceptada) y el ranking se lee de derecha a izquierda.
- 2026-10-23. Q: ¿Qué pasa si N, incluido el default 20, supera el número de términos? A: Se devuelven todos los términos sin error; solo `N` o `bucketSize` no entero o menor que 1 da HTTP 400.
- 2026-10-23. Q: ¿Cómo se entregan la traza y los rankings, y qué miden los tiempos? A: La petición de ranking devuelve el top N de cada algoritmo, el indicador de coincidencia calculado sobre las listas completas y tiempos medidos con la grabación de traza desactivada (igual que JMH); la traza completa se pide en un endpoint aparte por algoritmo.
- 2026-10-23. Q: ¿Qué pasa con una medición EN_CURSO si la aplicación se reinicia? A: Al arrancar, pasa a FALLIDA con el mensaje "Medición interrumpida por reinicio de la aplicación".
- 2026-10-23. Q: ¿Qué notación de complejidad se usa, dado que k significaba frecuencia máxima en Counting Sort y número de cubetas en Bucket Sort? A: Notación separada por algoritmo: Counting O(n+k), k = frecuencia máxima; Radix O(d·(n+b)), d = dígitos del máximo, b = 10; Bucket O(n+m) promedio y O(n²) peor caso, m = número de cubetas. REQ-22 muestra por algoritmo n y sus propios parámetros.
- 2026-10-23. Q: ¿Qué criterio alfabético desempata? A: El orden natural de `String` (`compareTo`, el de TreeMap por defecto) sobre los tokens ya preprocesados, documentado.
- 2026-10-23. Q: ¿Qué error de JMH se muestra? A: El scoreError de JMH (semiamplitud del intervalo de confianza al 99,9 %), mostrado como ms ± error.
- 2026-10-23. Q: ¿Desde qué tamaño se pagina un arreglo de la traza? A: Un arreglo de más de 50 elementos se pagina de a 50 y se muestra su longitud total.
- 2026-10-23. Q: ¿El historial se pagina o tiene límite? A: Lista completa sin paginar, de la más reciente a la más antigua.
- 2026-10-23. Q: ¿El endpoint de traza recibe `bucketSize`? A: Sí, opcional (entero ≥ 1, default 5, mismo HTTP 400 de REQ-4); solo afecta a Bucket Sort.
- 2026-10-23. Q: ¿Qué hace el test sobre el corpus si los PDF no están (no se versionan)? A: Se omite con Assumptions de JUnit, con el motivo en el reporte, cuando `corpus/` no tiene PDF; corre cuando el corpus está presente.
