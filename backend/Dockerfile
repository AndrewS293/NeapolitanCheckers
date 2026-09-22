
# Build stage
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven files first so Docker can cache dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN ./mvnw clean package -DskipTests


# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built Spring Boot jar
COPY --from=build /app/target/*.jar app.jar

# Render provides the PORT environment variable.
# Spring Boot will be configured to use it.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
