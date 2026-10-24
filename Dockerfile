# Imagen del servicio api. El modelo ONNX debe estar descargado antes del build
# (scripts/download-model.sh) para quedar empaquetado en el jar.
# Se construyen dos artefactos: app.jar (la API) y benchmarks.jar (JMH), que la API
# lanza como proceso hijo para las mediciones del Requerimiento 2 (spec 002, REQ-17).
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
ENV BIBLIOMTR_BENCHMARK_JAR=/app/benchmarks.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
