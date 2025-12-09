# ---------- BUILD STAGE ----------
FROM maven:3.8.5-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only pom.xml first (for dependency caching)
COPY sales/pom.xml .

RUN mvn dependency:go-offline -B

# Copy full backend source
COPY sales/. .

# Build the jar
RUN mvn package -DskipTests -B

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway uses PORT env variable)
EXPOSE 8080

# Default entrypoint for Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Optional: set default port if PORT env variable not provided
# Spring Boot will read this in application.properties as: server.port=${PORT:8080}
ENV PORT=8080
