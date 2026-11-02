# Builds and runs DiviStay in a container (used by Render, Railway, Koyeb, Fly.io and others).
# Step 1: build the jar with Maven
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# Step 2: a small image that only runs it
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV THYMELEAF_CACHE=true
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
