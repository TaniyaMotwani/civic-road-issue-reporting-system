FROM node:24-alpine AS frontend-build
WORKDIR /workspace/frontend
RUN corepack enable
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm run build

FROM maven:3.9-eclipse-temurin-17-alpine AS backend-build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=frontend-build /workspace/frontend/dist backend/src/main/resources/static
RUN mvn -B -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S roadwatch && adduser -S roadwatch -G roadwatch
COPY --from=backend-build /workspace/backend/target/civic-road-issues-api-0.1.0-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R roadwatch:roadwatch /app
USER roadwatch
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "/app/app.jar"]
