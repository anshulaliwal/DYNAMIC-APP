# ================================
# Stage 1: Build Spring Boot app
# ================================
FROM maven:3.9-eclipse-temurin-17 AS builder

# Root workdir
WORKDIR /app

# Copy entire repo
COPY . .

# Move into Spring Boot project (where pom.xml exists)
WORKDIR /app/dynamic-app

# Build the application
RUN mvn clean package -DskipTests


# ================================
# Stage 2: Run Spring Boot app
# ================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/dynamic-app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
