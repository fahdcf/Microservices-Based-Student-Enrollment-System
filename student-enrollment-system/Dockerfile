FROM eclipse-temurin:17-jre
ARG SERVICE
WORKDIR /app
COPY ${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar app.jar
USER 10001
ENTRYPOINT ["java","-Xms64m","-Xmx256m","-jar","/app/app.jar"]
