# Tasks: Entorno para el Requerimiento 4: Coincidencia exacta de cadenas (KMP, Boyer-Moore, Rabin-Karp)

## Phase 1: Contratos, stubs y validación de posiciones
- [x] T001 Crear el Javadoc del módulo en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/package-info.java` (REQ-4, REQ-5, REQ-6)
- [x] T002 Crear `StringMatcher`, `MatchOptions` y `MatchResult` en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/` (REQ-1, REQ-2, REQ-6, REQ-29)
- [x] T003 Crear `KmpPreprocessing`, `KmpTrace`, `BoyerMoorePreprocessing`, `BoyerMooreTrace`, `RabinKarpPreprocessing`, `RabinKarpTrace` con copias defensivas y su `package-info.java` en `src/main/java/co/edu/uniquindio/bibliomtr/trace/stringmatching/` (REQ-23, REQ-24, REQ-25)
- [x] T004 Crear el stub `KnuthMorrisPratt` con Javadoc (Qué calcula, Definición formal, Preprocesamiento, Búsqueda, Traza, Casos de borde, las dos Complejidad: TODO estudiante) en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/KnuthMorrisPratt.java` (REQ-1, REQ-2, REQ-3, REQ-4, REQ-7, AC-1)
- [x] T005 Crear el stub `BoyerMoore` con el mismo Javadoc y el ejemplo "abab" → [2, 2, 4, 1] en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/BoyerMoore.java` (REQ-1, REQ-2, REQ-3, REQ-5, REQ-7, AC-1)
- [x] T006 Crear el stub `RabinKarp` con el mismo Javadoc, d = 65 536 y q = 101 en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/RabinKarp.java` (REQ-1, REQ-2, REQ-3, REQ-6, REQ-7, AC-1)
- [x] T007 Crear `MatchPositionsValidatorTest` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/MatchPositionsValidatorTest.java` (REQ-8, AC-3)
- [x] T008 Crear `InvalidMatchPositionsException` y `MatchPositionsValidator` en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/` (REQ-8, AC-3)
- [x] T009 Crear `StringMatchingStubScaffoldTest` con `@Tag("scaffold")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/StringMatchingStubScaffoldTest.java` (REQ-1, REQ-7, AC-1, AC-2)
- [x] T010 Crear `StringMatchingStubDocumentationTest` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/StringMatchingStubDocumentationTest.java` (REQ-4, REQ-5, REQ-6, AC-23)
- [x] T011 Crear `ForbiddenStringMatchingUsageTest` con la verificación de Java y la autoprueba del detector en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/ForbiddenStringMatchingUsageTest.java` (REQ-7, REQ-14, AC-2)
- [x] T012 Actualizar las descripciones de `studentTest` y `scaffoldTest` en `build.gradle.kts` y verificar `./gradlew test` y `./gradlew scaffoldTest` en verde (REQ-13, AC-4)
- [x] T013 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `feat: contratos y stubs de la búsqueda de patrones (fase 1 de la spec 004)` con solo los archivos de la fase y `docs/specs/004-busqueda-patrones/` (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 2: Tests de estudiante y fragmento de la guía
- [x] T014 Crear `StringMatchingCases` con los casos a mano, los bordes, abstracts idénticos y `run(matcher, text, pattern, options)` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/StringMatchingCases.java` (REQ-2, REQ-3, REQ-9, AC-5, AC-6, AC-7, AC-8, AC-10, AC-25)
- [x] T015 Extraer una vez con `PdfArticleExtractor.extract` el abstract de un PDF de `corpus/`, elegir un fragmento literal de hasta 300 caracteres y un patrón en inglés de m ≤ 12 con al menos 2 apariciones, calcular a mano posiciones, π, carácter malo, sufijo bueno, salto tras ocurrencia, h y hash(P) con q = 101 y fijarlos en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/GuideFragment.java` (REQ-32)
- [x] T016 Crear `KnuthMorrisPrattTest` con `@Tag("student")` (π, 9 comparaciones, i no decreciente, bordes, fragmento) en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/KnuthMorrisPrattTest.java` (REQ-4, REQ-9, REQ-11, REQ-23, REQ-32, AC-5, AC-6, AC-7, AC-8, AC-10, AC-25)
- [x] T017 Crear `BoyerMooreTest` con `@Tag("student")` (tablas de "abab", alineaciones s = 0, 2, 4, bordes, fragmento) en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/BoyerMooreTest.java` (REQ-5, REQ-9, REQ-24, REQ-32, AC-5, AC-6, AC-7, AC-8, AC-10, AC-24, AC-25)
- [x] T018 Crear `RabinKarpTest` con `@Tag("student")` (h, hash(P), hashes de ventanas, colisión con q = 3, bordes, fragmento) en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/RabinKarpTest.java` (REQ-6, REQ-9, REQ-10, REQ-25, REQ-32, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10, AC-25)
- [x] T019 Crear `StringMatchingOracleTest` con `@Tag("student")` contra `String.indexOf` en `src/test/java/co/edu/uniquindio/bibliomtr/oracle/StringMatchingOracleTest.java` (REQ-12, REQ-32, AC-11)
- [x] T020 Crear `GuideFragmentCorpusTest` con omisión por `Assumptions` sin el PDF en `src/test/java/co/edu/uniquindio/bibliomtr/oracle/GuideFragmentCorpusTest.java` (REQ-12, REQ-32, AC-11)
- [x] T021 Verificar que `./gradlew studentTest --tests KnuthMorrisPrattTest --tests BoyerMooreTest --tests RabinKarpTest --tests StringMatchingOracleTest` falla en todos los casos y `./gradlew test` sigue en verde (REQ-13, AC-4)
- [x] T022 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `test: tests de estudiante y fragmento de la guía de búsqueda de patrones (fase 2 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 3: API de búsqueda de patrones
- [x] T023 Crear `PendingStubs` en `src/main/java/co/edu/uniquindio/bibliomtr/api/PendingStubs.java` (REQ-28)
- [x] T024 Crear `StringMatchingAlgorithm` y `StringMatchingCatalog` en `src/main/java/co/edu/uniquindio/bibliomtr/api/stringmatching/` (REQ-15, REQ-22, REQ-28)
- [x] T025 Crear el bean `StringMatchingCatalog` con los 3 stubs en `src/main/java/co/edu/uniquindio/bibliomtr/config/StringMatchingConfig.java` (REQ-15)
- [x] T026 Crear `PatternSearchValidator` en `src/main/java/co/edu/uniquindio/bibliomtr/api/stringmatching/PatternSearchValidator.java` (REQ-15, REQ-17, REQ-22, AC-13, AC-19, AC-25)
- [x] T027 Crear `PatternSearchDtos` en `src/main/java/co/edu/uniquindio/bibliomtr/api/stringmatching/PatternSearchDtos.java` (REQ-15, REQ-16, REQ-22, REQ-28, REQ-29)
- [x] T028 Crear `PatternSearchService` (un preprocesamiento, una búsqueda por abstract, PENDIENTE con método, validación de posiciones, `allMatch`, tiempos y traza) en `src/main/java/co/edu/uniquindio/bibliomtr/api/stringmatching/PatternSearchService.java` (REQ-1, REQ-8, REQ-15, REQ-16, REQ-18, REQ-22, REQ-28, REQ-29, AC-27)
- [x] T029 Crear `PatternSearchController` con `GET /api/pattern-search` y `GET /api/pattern-search/trace` en `src/main/java/co/edu/uniquindio/bibliomtr/api/stringmatching/PatternSearchController.java` (REQ-15, REQ-22)
- [x] T030 Manejar `InvalidMatchPositionsException` como 500 con mensaje en español en `src/main/java/co/edu/uniquindio/bibliomtr/api/ApiExceptionHandler.java` (REQ-8, AC-3)
- [x] T031 Crear `FakeStringMatchingCatalog` (implementado, pendiente en `preprocess`, pendiente en `search`, inválido; espía de llamadas) en `src/test/java/co/edu/uniquindio/bibliomtr/api/stringmatching/FakeStringMatchingCatalog.java` (REQ-1, REQ-28, REQ-29)
- [x] T032 Crear `PatternSearchApiIT` en `src/test/java/co/edu/uniquindio/bibliomtr/api/stringmatching/PatternSearchApiIT.java` (REQ-1, REQ-8, REQ-15, REQ-16, REQ-17, REQ-18, REQ-22, REQ-28, REQ-29, AC-3, AC-12, AC-13, AC-14, AC-19, AC-20, AC-21, AC-25, AC-26, AC-27)
- [x] T033 Verificar `./gradlew test` en verde, incluidos `ForbiddenStringMatchingUsageTest`, `ForbiddenSortUsageTest`, `ComparisonApiIT` y `ClusteringApiIT` (REQ-14, AC-2, AC-4)
- [x] T034 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `feat: API de búsqueda de patrones (fase 3 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 4: Pestaña "Búsqueda Patrones"
- [x] T035 Crear los tipos en `web/src/types/patternSearch.ts` (REQ-15, REQ-22)
- [x] T036 Crear `usePatternSearch` y `usePatternTrace` en `web/src/api/` (REQ-15, REQ-22)
- [x] T037 Crear `highlightSegments.test.ts` en `web/src/features/pattern-search/` (REQ-20, REQ-21, AC-15)
- [x] T038 Crear `highlightSegments` en `web/src/features/pattern-search/highlightSegments.ts` (REQ-20, REQ-21, AC-15)
- [x] T039 Crear las respuestas de ejemplo de búsqueda (implementados, solo KMP, corpus vacío, posiciones de fixture que no son ocurrencias) en `web/src/test/patternSearchFixtures.ts` (REQ-15, REQ-20, REQ-28)
- [x] T040 Crear `PatternSearchForm` con patrón, q opcional y "Buscar" sin recortar el patrón en `web/src/features/pattern-search/PatternSearchForm.tsx` (REQ-19, AC-25)
- [x] T041 Crear `AlgorithmOccurrencesTable` y `PatternSearchResults` (tiempos, total, colisiones, pendiente, indicador, "Sin abstracts en el corpus", errores) en `web/src/features/pattern-search/` (REQ-16, REQ-18, REQ-20, REQ-28, REQ-29, AC-13, AC-14, AC-20, AC-21)
- [x] T042 Crear `AlgorithmSelector` con `defaultAlgorithm` y `HighlightedAbstract` en `web/src/features/pattern-search/` (REQ-20, REQ-21, AC-15)
- [x] T043 Crear `PatternSearchView` en `web/src/features/pattern-search/PatternSearchView.tsx` y la página `web/src/pages/PatternSearchPage.tsx` (REQ-19, REQ-20)
- [x] T044 Crear `PatternSearchView.test.tsx` en `web/src/features/pattern-search/` (REQ-17, REQ-18, REQ-19, REQ-20, REQ-21, REQ-28, REQ-29, AC-13, AC-14, AC-15, AC-20, AC-21, AC-25)
- [x] T045 Crear `App.test.tsx` en `web/src/App.test.tsx` y agregar la pestaña "Búsqueda Patrones" con carga diferida en `web/src/App.tsx` (REQ-19)
- [x] T046 Agregar a `ForbiddenStringMatchingUsageTest` la verificación por nombre de `web/src/features/pattern-search` y de los `web/src/components` que importa, y la de librerías de búsqueda en `web/src` y `web/package.json`, en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/stringmatching/ForbiddenStringMatchingUsageTest.java` (REQ-14, REQ-21, AC-2)
- [x] T047 Verificar `npm test`, `npm run lint` y `npm run build` en `web/` y `./gradlew test` en verde (REQ-14, REQ-21, AC-2)
- [x] T048 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `feat: pestaña Búsqueda Patrones (fase 4 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 5: Visor de traza de la búsqueda de patrones
- [x] T049 Agregar a `web/src/test/patternSearchFixtures.ts` las trazas de "abababca"/"abab" de los 3 algoritmos, la de colisión "abba"/"ab" con q = 3 y una pendiente (REQ-23, REQ-24, REQ-25, REQ-28)
- [x] T050 Crear `traceSteps.test.ts` en `web/src/features/pattern-search/` (REQ-26, AC-16, AC-17, AC-18)
- [x] T051 Crear `toFrames` en `web/src/features/pattern-search/traceSteps.ts` (REQ-21, REQ-26, AC-16, AC-17, AC-18)
- [x] T052 Crear las fórmulas KaTeX en `web/src/features/pattern-search/patternFormulas.ts` (REQ-26)
- [x] T053 Crear `StepNavigator` y `AlignedWindow` en `web/src/features/pattern-search/` (REQ-26, AC-16, AC-17)
- [x] T054 Crear `TraceChart` con Recharts en `web/src/features/pattern-search/TraceChart.tsx` (REQ-27, AC-16, AC-17, AC-18)
- [x] T055 Crear `KmpTraceView`, `BoyerMooreTraceView` y `RabinKarpTraceView` en `web/src/features/pattern-search/` (REQ-23, REQ-24, REQ-25, REQ-26, REQ-27, AC-16, AC-17, AC-18)
- [x] T056 Crear `PatternTraceView` con el aviso de pendiente y conectarlo al algoritmo y artículo elegidos en `web/src/features/pattern-search/PatternTraceView.tsx` y `PatternSearchView.tsx` (REQ-22, REQ-26, REQ-28, AC-26)
- [x] T057 Crear `PatternTraceView.test.tsx` en `web/src/features/pattern-search/` (REQ-23, REQ-24, REQ-25, REQ-26, REQ-27, REQ-28, AC-16, AC-17, AC-18, AC-26)
- [x] T058 Verificar `npm test`, `npm run lint` y `npm run build` en `web/` y `./gradlew test` en verde (REQ-14, REQ-21, AC-2)
- [x] T059 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `feat: visor de traza de la búsqueda de patrones (fase 5 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 6: Benchmarks JMH
- [x] T060 Crear `StringMatchingBenchmark` (KMP y Boyer-Moore, un `@State` por algoritmo, preprocesamiento y búsqueda por separado) en `src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/StringMatchingBenchmark.java` (REQ-30, AC-22)
- [x] T061 Crear `RabinKarpBenchmark` con `@Param q` en `src/jmh/java/co/edu/uniquindio/bibliomtr/evaluation/RabinKarpBenchmark.java` (REQ-30, AC-22)
- [x] T062 Verificar `./gradlew jmhJar` y `./gradlew test` en verde (REQ-30, AC-4)
- [x] T063 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `feat: benchmarks JMH de la búsqueda de patrones (fase 6 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## Phase 7: Guía, trazabilidad y README
- [ ] T064 Agregar la sección "## 11. Requerimiento 4: Coincidencia exacta de cadenas" con un apartado por stub, el ejemplo del fragmento real, el comando JMH y la declaración de IA, sin tocar § 10, en `docs/guia-implementacion.md` (REQ-31, REQ-32, AC-22, AC-23)
- [ ] T065 Agregar las filas del Requerimiento 4 con la sección `(pendiente)`, sin tocar las del Requerimiento 3, en `docs/traceability.md` (REQ-33, AC-23)
- [ ] T066 Crear `PatternSearchGuideTest` en `src/test/java/co/edu/uniquindio/bibliomtr/docs/PatternSearchGuideTest.java` (REQ-31, REQ-32, REQ-33, AC-22, AC-23)
- [ ] T067 Agregar la sección de búsqueda de patrones, los comandos de test y el de JMH en `README.md` (REQ-30, REQ-31)
- [ ] T068 Verificar `./gradlew test`, `./gradlew scaffoldTest` y `npm test` en verde, incluidos `ImplementationGuideTest` y `ClusteringGuideTest` (REQ-13, REQ-33, AC-4)
- [ ] T069 Marcar las tareas de la fase en `docs/specs/004-busqueda-patrones/tasks.md` y crear el commit `docs: guía, trazabilidad y README de la búsqueda de patrones (fase 7 de la spec 004)` con solo los archivos de la fase (sin `AGENTS.md`), terminado en `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`
