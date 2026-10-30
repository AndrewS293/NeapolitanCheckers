
# Build stage
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy root Maven configuration
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Copy module POMs so Maven can resolve the multi-module project
COPY gamelogic/pom.xml gamelogic/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY frontend/pom.xml frontend/pom.xml

# Download dependencies and resolve the parent/module relationships
RUN ./mvnw -pl backend -am dependency:go-offline -B

# Copy all project source code
COPY gamelogic gamelogic
COPY backend backend

# Build backend and its required modules
RUN ./mvnw -pl backend -am clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot executable JAR
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
