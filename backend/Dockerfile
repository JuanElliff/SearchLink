# Stage 1: Build con Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Descarga dependencias primero (capa cacheada si pom.xml no cambia)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compila el proyecto
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime mínimo
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
