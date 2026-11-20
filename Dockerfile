FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG SERVICIO
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY miembros-service/pom.xml miembros-service/pom.xml
COPY miembros-service/src miembros-service/src
COPY clases-service/pom.xml clases-service/pom.xml
COPY clases-service/src clases-service/src
COPY pagos-service/pom.xml pagos-service/pom.xml
COPY pagos-service/src pagos-service/src
RUN --mount=type=cache,target=/root/.m2/repository \
    test -n "${SERVICIO}" && \
    mvn -B -ntp -pl "${SERVICIO}" -am package -DskipTests \
      -Dmaven.wagon.http.retryHandler.count=5 \
      -Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
      -Dmaven.wagon.rto=120000

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICIO
COPY --from=build --chown=10001:10001 /workspace/${SERVICIO}/target/*.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
