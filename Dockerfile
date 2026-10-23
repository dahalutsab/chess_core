# Small, low-memory image for the UChess backend:
#   1. build the jar with Maven and unpack it (an unpacked jar starts faster and uses less memory than a nested one)
#   2. jlink a Java runtime containing only the JDK modules the application uses
#   3. copy both onto plain Alpine Linux, so the image carries no JDK, Maven or build tools
#
# Build:  docker build -t uchess-backend .
# Run:    docker run -p 8181:8181 -m 512m --cpus 2 -e SPRING_PROFILES_ACTIVE=docker -v uchess-files:/data uchess-backend

# ---- 1. Build ----
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
# Dependencies are downloaded in their own layer, which is reused until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/chess_engine-0.0.1-SNAPSHOT.jar extract --destination /app \
 && mv /app/chess_engine-0.0.1-SNAPSHOT.jar /app/app.jar

# ---- 2. Minimal Java runtime ----
# Module list from: jdeps --ignore-missing-deps --recursive --multi-release 21 --print-module-deps
#                     --class-path 'lib/*' app.jar
# plus modules that are only loaded reflectively: jdk.crypto.ec (TLS for SMTP and database SSL) and java.naming.
FROM eclipse-temurin:21-jdk-alpine AS jre
RUN jlink \
      --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql.rowset,jdk.crypto.ec,jdk.jfr,jdk.unsupported \
      --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
      --output /jre

# ---- 3. Runtime ----
FROM alpine:3.20
ENV JAVA_HOME=/opt/jre \
    PATH=/opt/jre/bin:$PATH \
    # Uploaded files (e.g. bot avatars) are stored here; mount a volume to keep them
    APP_FILEPATH_LINUX=/data/ \
    # Low-footprint JVM settings: the serial GC has the smallest memory overhead, the heap scales with the container's
    # memory limit and is handed back to the OS after garbage collection (the free-ratio flags), and thread stacks,
    # code cache and metaspace are capped well below their defaults.
    JAVA_OPTS="-XX:+UseSerialGC -XX:MaxRAMPercentage=70 -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -Xss512k -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError"

COPY --from=jre /jre /opt/jre
RUN addgroup -S uchess && adduser -S -G uchess uchess \
 && mkdir -p /data && chown uchess:uchess /data

WORKDIR /app
COPY --from=build --chown=uchess:uchess /app ./
USER uchess

EXPOSE 8181
VOLUME /data
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
