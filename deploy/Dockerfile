# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre-noble
# fontconfig is needed by the demo profile's placeholder image generator (Java2D)
RUN apt-get update \
    && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 appuser \
    && mkdir -p /app/data/uploads \
    && chown -R appuser /app
WORKDIR /app
COPY --from=build /app/target/tcgshop-*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
