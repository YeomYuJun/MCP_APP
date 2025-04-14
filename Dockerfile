FROM eclipse-temurin:17-jdk as builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY src/main/resources/templates /app/templates

# Create data directory for MCP filesystem server
RUN mkdir -p /app/data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
