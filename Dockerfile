FROM maven:3.6-openjdk-15 AS build

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src src/
RUN mvn package

FROM openjdk:15-slim

RUN set -ex \
    && useradd --system --shell /bin/false --home /app --user-group app \
    && mkdir ~app \
    && chown -R app:app ~app
USER app

COPY --from=build /build/target/trino-lsp.jar /app/trino-lsp.jar
ENTRYPOINT ["java", "-jar", "/app/trino-lsp.jar"]
