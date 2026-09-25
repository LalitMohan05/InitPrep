ARG SERVICE
FROM maven:3.9.11-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /workspace
COPY backend/${SERVICE}/pom.xml ./pom.xml
COPY backend/${SERVICE}/src ./src
RUN mvn -B -DskipTests package \
    && find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -exec cp {} /workspace/application.jar \; \
    && test -s /workspace/application.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/application.jar ./application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
