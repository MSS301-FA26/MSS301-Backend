FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY booking-service/pom.xml booking-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

COPY . .

ARG SERVICE
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl ${SERVICE} -am package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG SERVICE
COPY --from=build /workspace/${SERVICE}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
