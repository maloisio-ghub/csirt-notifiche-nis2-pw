# ---- Stadio 1: compilazione con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Prima solo il pom: le dipendenze vengono scaricate una volta e messe in cache
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Poi i sorgenti e la compilazione (i test si eseguono in sviluppo, non nel build dell'immagine)
COPY src src
RUN mvn -q -B -DskipTests package

# ---- Stadio 2: esecuzione con il solo JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Utente senza privilegi: l'applicazione non gira come root
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]