# Build the Vue application, then package it into Spring Boot's static resources.
FROM node:20-alpine AS frontend-build
WORKDIR /workspace/fronted
COPY fronted/package.json fronted/package-lock.json ./
RUN npm ci
COPY fronted/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS backend-build
WORKDIR /workspace/career-core
COPY career-core/pom.xml career-core/mvnw ./
COPY career-core/.mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY career-core/ ./
COPY --from=frontend-build /workspace/fronted/dist ./src/main/resources/static
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=backend-build /workspace/career-core/target/career-core-*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
