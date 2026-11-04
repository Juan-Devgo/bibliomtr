# Imagen del servicio api. El modelo ONNX debe estar descargado antes del build
# (scripts/download-model.sh).
# Se construyen dos artefactos: app.jar (la API) y benchmarks.jar (JMH), que la API
# lanza como proceso hijo para las mediciones del Requerimiento 2 (spec 002, REQ-17).
# El modelo también se copia como archivo a /app/models y las variables BIBLIOMTR_EMBEDDINGS_*
# apuntan ahí: dentro del jar no es un archivo y se cargaría entero en el heap (437 MB, más que
# -Xmx768m de compose.prod.yaml); como archivo, ONNX Runtime lo lee en memoria nativa.
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar jmhJar -x test

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar app.jar
COPY --from=build /workspace/build/libs/benchmarks.jar benchmarks.jar
COPY --from=build /workspace/src/main/resources/models/specter2-base models/specter2-base
ENV BIBLIOMTR_BENCHMARK_JAR=/app/benchmarks.jar \
    BIBLIOMTR_EMBEDDINGS_MODELPATH=file:/app/models/specter2-base/model.onnx \
    BIBLIOMTR_EMBEDDINGS_TOKENIZERPATH=file:/app/models/specter2-base/tokenizer.json
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
