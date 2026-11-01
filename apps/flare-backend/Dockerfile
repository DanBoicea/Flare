# Flare back end (task A11). Build from the repo root, because the JAR also packs data/knowledge-base:
#   docker build -f apps/flare-backend/Dockerfile -t flare-backend .
# or simply `docker compose up --build` from the repo root (compose.yaml).
# No secrets are baked in: every setting comes from env vars at run time (apps/flare-backend/.env.example).

# --- Build: the Maven wrapper, Java 21 ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build/apps/flare-backend

# Dependencies first, so they stay cached until pom.xml changes.
COPY apps/flare-backend/mvnw apps/flare-backend/pom.xml ./
COPY apps/flare-backend/.mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY data/knowledge-base /build/data/knowledge-base
COPY apps/flare-backend/src src
RUN ./mvnw -B -q -DskipTests package && cp target/flare-backend-*.jar /build/app.jar

# --- Run: Java 21 runtime only, as a non-root user ---
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 flare
WORKDIR /app
COPY --from=build /build/app.jar app.jar
USER flare

# demo by default (A11); a local run sets SPRING_PROFILES_ACTIVE=local in its env file.
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
