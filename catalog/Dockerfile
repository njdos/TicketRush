FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw clean package -B -DskipTests -Dmaven.javadoc.skip=true

FROM eclipse-temurin:17-jre-alpine AS extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination /app/extracted

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup --gid 10001 --system ticketrush \
    && adduser --uid 10001 --system --ingroup ticketrush --home /home/catalog catalog \
    && mkdir -p /app/dumps /app/logs \
    && chown -R catalog:ticketrush /app

COPY --from=extractor --chown=catalog:ticketrush /app/extracted/dependencies/ ./
COPY --from=extractor --chown=catalog:ticketrush /app/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=catalog:ticketrush /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=catalog:ticketrush /app/extracted/application/ ./

USER catalog:ticketrush
EXPOSE 8080

ENV JDK_JAVA_OPTIONS="\
  # --- Container & Memory Alignment --- \
  -XX:+UseContainerSupport \
  -XX:InitialRAMPercentage=75.0 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:MaxMetaspaceSize=384m \
  -Xss512k \
  \
  # --- G1GC Performance Tuning --- \
  -XX:+UseG1GC \
  -XX:+DisableExplicitGC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:G1PeriodicGCInterval=60000 \
  -XX:+G1PeriodicGCInvokesConcurrent \
  \
  # --- JIT Compiler & Runtime Optimizations --- \
  -XX:-OmitStackTraceInFastThrow \
  \
  # --- Crash Management & Fail-Fast --- \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/dumps/ \
  -XX:+ExitOnOutOfMemoryError \
  \
  # --- System Properties & Security --- \
  -Dfile.encoding=UTF-8 \
  -Dspring.backgroundpreinitializer.ignore=true"

ENV GC_LOG_OPTIONS="\
  -Xlog:gc*,gc+age=trace:file=/app/logs/gc.log:time,uptime,pid:filecount=5,filesize=50M \
  -XX:+XLogAsynchronous"

ENTRYPOINT ["sh", "-c", "java ${GC_LOG_OPTIONS} org.springframework.boot.loader.launch.JarLauncher"]
