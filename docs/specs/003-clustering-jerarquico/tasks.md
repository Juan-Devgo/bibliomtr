# Tasks: Entorno para el Requerimiento 3: Clustering jerárquico con dendrograma

## Phase 1: Contratos, stubs y validación del dendrograma
- [ ] T001 Crear el Javadoc del módulo en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/package-info.java` (REQ-4)
- [ ] T002 Crear `Merge`, `ClusteringResult` y `HierarchicalClustering` en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/` (REQ-3)
- [ ] T003 Crear `LinkageTrace` y `DianaTrace` con copias defensivas en `src/main/java/co/edu/uniquindio/bibliomtr/trace/clustering/` y su `package-info.java` (REQ-16)
- [ ] T004 Crear los stubs `SingleLinkage`, `CompleteLinkage` y `AverageLinkage` con Javadoc (Qué calcula, Fórmula, Desempate, Traza, Rango, Casos de borde, Complejidad: TODO estudiante) en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/` (REQ-1, REQ-4, REQ-5, REQ-6, AC-1)
- [ ] T005 Crear el stub `Diana` con el mismo Javadoc y la regla de divisiones en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/Diana.java` (REQ-2, REQ-4, REQ-5, REQ-6, AC-1)
- [ ] T006 Crear `InvalidDendrogramException` y `DendrogramValidator` en `src/main/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/` (REQ-7, AC-10)
- [ ] T007 Crear `DendrogramValidatorTest` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/DendrogramValidatorTest.java` (REQ-7, AC-10)
- [ ] T008 Crear `ClusteringStubScaffoldTest` con `@Tag("scaffold")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/ClusteringStubScaffoldTest.java` (REQ-1, REQ-2, REQ-6, AC-1)
- [ ] T009 Crear `ClusteringStubDocumentationTest` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/ClusteringStubDocumentationTest.java` (REQ-4, REQ-5, AC-18)
- [ ] T010 Crear `ForbiddenClusteringUsageTest` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/ForbiddenClusteringUsageTest.java` (REQ-6, REQ-15, AC-2)
- [ ] T011 Actualizar las descripciones de `studentTest` y `scaffoldTest` en `build.gradle.kts` y verificar `./gradlew test` en verde y `./gradlew scaffoldTest` en verde (REQ-9, AC-3)

## Phase 2: Tests de estudiante
- [ ] T012 Crear `ClusteringCases` con D₄, 2 artículos, idénticos, empates, 20 artículos con `Random(42)` y aserciones estructurales en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/ClusteringCases.java` (REQ-8)
- [ ] T013 Crear `SingleLinkageTest` con `@Tag("student")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/SingleLinkageTest.java` (REQ-8, REQ-16, AC-5, AC-6, AC-7, AC-8, AC-9)
- [ ] T014 Crear `CompleteLinkageTest` con `@Tag("student")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/CompleteLinkageTest.java` (REQ-8, REQ-16, AC-5, AC-6, AC-7, AC-8, AC-9)
- [ ] T015 Crear `AverageLinkageTest` con `@Tag("student")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/AverageLinkageTest.java` (REQ-8, REQ-16, AC-5, AC-6, AC-7, AC-8, AC-9)
- [ ] T016 Crear `DianaTest` con `@Tag("student")` en `src/test/java/co/edu/uniquindio/bibliomtr/algorithms/clustering/DianaTest.java` (REQ-8, REQ-16, AC-5, AC-6, AC-7, AC-8, AC-9)
- [ ] T017 Verificar que `./gradlew studentTest --tests '*LinkageTest' --tests DianaTest` falla en todos los casos y `./gradlew test` sigue en verde (REQ-9, AC-3, AC-4)

## Phase 3: API de clustering
- [ ] T018 Agregar `similarityMatrix` y `SourceMatrix` reutilizando el código privado de `compare` en `src/main/java/co/edu/uniquindio/bibliomtr/api/similarity/ComparisonService.java` (REQ-10)
- [ ] T019 Agregar `fromSource` en `src/main/java/co/edu/uniquindio/bibliomtr/api/similarity/ComparisonAlgorithm.java` (REQ-12, AC-12)
- [ ] T020 Crear `ClusteringAlgorithm` y `ClusteringCatalog` en `src/main/java/co/edu/uniquindio/bibliomtr/api/clustering/` (REQ-11, REQ-18)
- [ ] T021 Crear el bean `ClusteringCatalog` con los 4 stubs en `src/main/java/co/edu/uniquindio/bibliomtr/config/ClusteringConfig.java` (REQ-11)
- [ ] T022 Crear `ClusteringDtos` en `src/main/java/co/edu/uniquindio/bibliomtr/api/clustering/ClusteringDtos.java` (REQ-11, REQ-19, REQ-22)
- [ ] T023 Crear `ClusteringService` (D = 1 − S, diagonal 0, PENDIENTE de fuente y de algoritmo, validación y tiempos) en `src/main/java/co/edu/uniquindio/bibliomtr/api/clustering/ClusteringService.java` (REQ-7, REQ-10, REQ-11, REQ-16, REQ-18, REQ-19, REQ-22)
- [ ] T024 Crear `ClusteringController` con `POST /api/clustering` y `GET /api/clustering/trace` en `src/main/java/co/edu/uniquindio/bibliomtr/api/clustering/ClusteringController.java` (REQ-11, REQ-16)
- [ ] T025 Manejar `InvalidDendrogramException` como 500 con mensaje en español en `src/main/java/co/edu/uniquindio/bibliomtr/api/ApiExceptionHandler.java` (REQ-7, AC-10)
- [ ] T026 Crear `FakeClusteringCatalog` en `src/test/java/co/edu/uniquindio/bibliomtr/api/clustering/FakeClusteringCatalog.java` (REQ-11, REQ-18)
- [ ] T027 Crear `ClusteringApiIT` en `src/test/java/co/edu/uniquindio/bibliomtr/api/clustering/ClusteringApiIT.java` (REQ-7, REQ-10, REQ-11, REQ-12, REQ-16, REQ-18, REQ-19, REQ-22, AC-10, AC-11, AC-12, AC-15, AC-16, AC-17, AC-19)
- [ ] T028 Verificar `./gradlew test` en verde, incluido `ComparisonApiIT` (REQ-10, AC-3)

## Phase 4: Sección de dendrogramas en la interfaz
- [ ] T029 Crear los tipos en `web/src/types/clustering.ts` (REQ-11, REQ-16)
- [ ] T030 Crear `useClustering` y `useClusteringTrace` en `web/src/api/` (REQ-13, REQ-16)
- [ ] T031 Crear `toHierarchy` en `web/src/features/clustering/dendrogramTree.ts` y su test `dendrogramTree.test.ts` (REQ-15, AC-14)
- [ ] T032 Crear el componente D3 en `web/src/components/Dendrogram.tsx` y su test `Dendrogram.test.tsx` (REQ-14, REQ-15, AC-14)
- [ ] T033 Crear `SourceSelector` y `DendrogramCard` en `web/src/features/clustering/` (REQ-13, REQ-14, REQ-18, REQ-22)
- [ ] T034 Crear las respuestas de ejemplo con D₄ en `web/src/test/clusteringFixtures.ts` (REQ-14, REQ-16)
- [ ] T035 Crear `ClusteringSection` con selector, tiempos, aviso de fuente pendiente y cuadrícula 2×2 en `web/src/features/clustering/ClusteringSection.tsx` (REQ-13, REQ-14, REQ-18, REQ-19, REQ-22)
- [ ] T036 Crear `ClusteringSection.test.tsx` en `web/src/features/clustering/` (REQ-13, REQ-14, REQ-18, REQ-19, REQ-22, AC-13, AC-14, AC-16, AC-17, AC-19)
- [ ] T037 Agregar `ClusteringSection` tras el visor de traza en `web/src/pages/SimilarityPage.tsx` (REQ-13)
- [ ] T038 Verificar `npm test`, `npm run lint` y `npm run build` en `web/` y `./gradlew test` (`ForbiddenSortUsageTest`, `ForbiddenClusteringUsageTest`) en verde (REQ-15, AC-2)

## Phase 5: Visor de traza del clustering
- [ ] T039 Crear las fórmulas KaTeX en `web/src/features/clustering/clusteringFormulas.ts` (REQ-17)
- [ ] T040 Crear `LinkageTraceView` y `DianaTraceView` en `web/src/features/clustering/` (REQ-16, REQ-17)
- [ ] T041 Crear `ClusteringTraceView` y conectar "Ver traza" de las tarjetas en `web/src/features/clustering/ClusteringTraceView.tsx` y `ClusteringSection.tsx` (REQ-16, REQ-17, REQ-18)
- [ ] T042 Crear `ClusteringTraceView.test.tsx` en `web/src/features/clustering/` (REQ-17, AC-15)
- [ ] T043 Verificar `npm test`, `npm run lint` y `npm run build` en `web/` (REQ-17)

## Phase 6: Guía, trazabilidad y README
- [ ] T044 Agregar la sección "## 10. Requerimiento 3: Clustering jerárquico" con un apartado por stub, el título general y la declaración de IA en `docs/guia-implementacion.md` (REQ-20, AC-18)
- [ ] T045 Agregar las filas del Requerimiento 3 con la sección `(pendiente)` en `docs/traceability.md` (REQ-21, AC-18)
- [ ] T046 Crear `ClusteringGuideTest` en `src/test/java/co/edu/uniquindio/bibliomtr/docs/ClusteringGuideTest.java` (REQ-20, REQ-21, AC-18)
- [ ] T047 Agregar la sección de clustering y los comandos de test en `README.md` (REQ-20)
- [ ] T048 Verificar `./gradlew test`, `./gradlew scaffoldTest` y `npm test` en verde (REQ-9, AC-3)
