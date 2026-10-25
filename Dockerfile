# One image serves the API and the Angular app (ADR 0003).
# Stages: build the SPA with Node, build the jar with Maven (the SPA copied into static/), run on a JRE.

FROM node:24-alpine AS frontend
WORKDIR /frontend
# Dependencies in their own layer: only re-installed when the lockfile changes.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npx ng build

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY backend/src/ src/
COPY --from=frontend /frontend/dist/ src/main/resources/static/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S quiz && adduser -S quiz -G quiz
COPY --from=backend --chown=quiz:quiz /backend/target/tech-quiz-backend-*.jar app.jar
# Default questions, so the image also works with `docker run`. docker-compose.yml mounts ./questions
# over them, so edited files only need `docker compose restart app`.
COPY --chown=quiz:quiz questions/ questions/
USER quiz

ENV QUIZ_QUESTIONS_PATH=/app/questions
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
