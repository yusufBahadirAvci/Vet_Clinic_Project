# ---- Build stage: Maven wrapper + JDK 21 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so they are cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in CI and with ./mvnw verify (Testcontainers needs Docker, which is not available here).
RUN ./mvnw -B -q package -DskipTests \
    && cp target/vetclinic-*.jar app.jar

# ---- Runtime stage: JRE 21 only ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
# Same locale as .mvn/jvm.config (CLAUDE.md rule 15).
ENTRYPOINT ["java", "-Duser.language=en", "-Duser.country=US", "-jar", "/app/app.jar"]
