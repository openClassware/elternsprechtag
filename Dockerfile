# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1: Build
# JDK 25 + Maven (via Wrapper) bauen das Vaadin-Production-JAR. Der schwere
# Frontend-Bundling-Schritt läuft hier auf dem Build-Host/Runner über das
# `production`-Profil — nicht zur Laufzeit im Container.
# ---------------------------------------------------------------------------
# Java-Version an einer Stelle pflegen (Build- und Runtime-Stage teilen sie).
ARG JAVA_VERSION=25

FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /app

# Zuerst nur Wrapper + POM kopieren, damit die Dependency-Schicht gecacht wird
# und nicht bei jeder Quellcode-Änderung neu aufgelöst werden muss.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -Pproduction dependency:go-offline || true

# Quellcode kopieren und das Production-JAR bauen.
COPY src/ src/
RUN ./mvnw -B -Pproduction clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2: Runtime
# Schlankes JRE-Image mit ausschliesslich dem fertigen Fat-JAR.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre AS runtime
WORKDIR /app

# Als Nicht-Root laufen (Angriffsfläche minimieren).
RUN useradd --system --user-group --no-create-home app
# Schreibbares Verzeichnis für eine optionale Logdatei (LOGGING_FILE_NAME). Ohne die Variable
# bleibt es leer, und die Anwendung loggt wie gehabt nur auf die Standardausgabe. Ein Volume,
# das hier eingehängt wird, übernimmt beim ersten Anlegen Eigentümer `app` von diesem Verzeichnis.
RUN mkdir /app/logs && chown app:app /app/logs
USER app

# spring-boot-repackage hinterlässt genau ein *.jar (das Original wird *.jar.original).
COPY --from=build --chown=app:app /app/target/*.jar app.jar

EXPOSE 8080

# SPRING_PROFILES_ACTIVE (Deploy setzt `demo`) und die restliche Konfiguration
# werden zur Laufzeit aus Umgebungsvariablen aufgelöst (siehe application.properties).
ENTRYPOINT ["java", "-jar", "app.jar"]
