FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/text-to-learn-api-0.0.1-SNAPSHOT.jar app.jar

ENV PORT=5000
EXPOSE 5000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
