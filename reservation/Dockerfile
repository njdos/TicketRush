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
    && adduser --uid 10001 --system --ingroup ticketrush --home /home/reservation reservation \
    && mkdir -p /app/dumps /app/logs \
    && chown -R reservation:ticketrush /app

COPY --from=extractor --chown=reservation:ticketrush /app/extracted/dependencies/ ./
COPY --from=extractor --chown=reservation:ticketrush /app/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=reservation:ticketrush /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=reservation:ticketrush /app/extracted/application/ ./

USER reservation:ticketrush
EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java ${GC_LOG_OPTIONS} ${JDK_JAVA_OPTIONS} org.springframework.boot.loader.launch.JarLauncher"]
