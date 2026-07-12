# syntax=docker/dockerfile:1

# ---- Stage 1: build -------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Cache de dependencies: copia so o wrapper + pom.xml primeiro
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

# Agora copia o codigo e empacota
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw -B -q clean package -DskipTests \
    && mv target/*.war target/app.war

# ---- Stage 2: runtime -------------------------------------------------------
# distroless: sem shell/package manager/apk -> sem CVEs de pacotes de SO acumulados.
# Sem shell também significa sem HEALTHCHECK aqui (wget/curl nao existem); a checagem
# de saude (GET /actuator/health) e feita de fora do container (ver docker-compose.yaml).
FROM gcr.io/distroless/java21-debian12 AS runtime
WORKDIR /app

COPY --from=build --chown=nonroot:nonroot /workspace/target/app.war app.war
USER nonroot

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.war"]
