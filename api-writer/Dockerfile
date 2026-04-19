# ÉTAPE 1 : La compilation (Build)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 1. Copier SEULEMENT le pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests

# On lance la compilation
RUN mvn clean package -DskipTests

# ÉTAPE 2 : L'image finale (Runtime)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# On récupère le JAR depuis l'étape précédente (l'alias "build")
COPY --from=build /app/target/*.jar app.jar

# On lance l'app
ENTRYPOINT ["java", "-jar", "app.jar"]