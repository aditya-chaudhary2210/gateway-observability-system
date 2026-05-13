FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
COPY integration ./integration
COPY platform ./platform
COPY services ./services
RUN mvn -q -pl services/log-ingestion-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /workspace/services/log-ingestion-service/target/*-exec.jar /opt/app/app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/opt/app/app.jar"]
