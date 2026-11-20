FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG SERVICIO
# Settings globales con un espejo de Maven Central (ver docker/maven/settings.xml).
COPY docker/maven/settings.xml /usr/share/maven/conf/settings.xml
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY common/src common/src
COPY miembros-service/pom.xml miembros-service/pom.xml
COPY miembros-service/src miembros-service/src
COPY clases-service/pom.xml clases-service/pom.xml
COPY clases-service/src clases-service/src
COPY pagos-service/pom.xml pagos-service/pom.xml
COPY pagos-service/src pagos-service/src
# Reutiliza dependencias entre builds del mismo builder. El lock evita escritores simultaneos.
# Reintenta solo errores de resolucion; errores de compilacion siguen fallando inmediatamente.
RUN --mount=type=cache,id=gymtrack-maven-docker,target=/root/.m2,sharing=locked \
    set -eu; \
    test -n "${SERVICIO}"; \
    intento=1; \
    while :; do \
        echo "Construyendo ${SERVICIO}: intento Maven ${intento}/3"; \
        if mvn -B -U -pl "${SERVICIO}" -am package -DskipTests > /tmp/gymtrack-maven.log 2>&1; then \
            cat /tmp/gymtrack-maven.log; \
            break; \
        else \
            codigo=$?; \
            cat /tmp/gymtrack-maven.log; \
        fi; \
        if [ "${intento}" -ge 3 ] || ! grep -Eiq 'Could not (find|transfer|resolve) artifact|Non-resolvable parent POM|Failed to read artifact descriptor' /tmp/gymtrack-maven.log; then \
            exit "${codigo}"; \
        fi; \
        espera=$((intento * 15)); \
        echo "Fallo de resolucion Maven; reintentando en ${espera}s."; \
        sleep "${espera}"; \
        intento=$((intento + 1)); \
    done

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICIO
COPY --from=build --chown=10001:10001 /workspace/${SERVICIO}/target/*.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
