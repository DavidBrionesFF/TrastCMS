FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY frontend frontend
COPY src src
RUN mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home --uid 10001 trastcms
WORKDIR /app
COPY --from=build /workspace/target/trastcms-*.jar /app/trastcms.jar
RUN mkdir -p /app/data && chown -R trastcms:trastcms /app
USER trastcms
EXPOSE 8080
ENV TRASTCMS_DATA_DIR=/app/data
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/trastcms.jar"]
