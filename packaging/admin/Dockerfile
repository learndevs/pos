# Retail POS Admin Server as a container. Build context = this folder after `./build.sh` copied admin.jar in.
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 posadmin
WORKDIR /app
COPY admin.jar /app/admin.jar
USER posadmin
ENV ADMIN_ADDRESS=0.0.0.0 \
    ADMIN_PORT=8081 \
    JAVA_TOOL_OPTIONS="-Xmx384m -XX:+UseSerialGC"
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s CMD wget -qO- http://127.0.0.1:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/admin.jar"]
