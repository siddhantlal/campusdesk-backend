FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system campusdesk && useradd --system --gid campusdesk --home-dir /app campusdesk
WORKDIR /app
COPY --from=build --chown=campusdesk:campusdesk /build/target/campusdesk-1.0.0.jar app.jar
COPY --chown=campusdesk:campusdesk scripts/container-start.sh /app/start.sh
USER campusdesk
EXPOSE 8080
ENTRYPOINT ["sh", "/app/start.sh"]
