# Build stage: JDK 21 matches the Gradle toolchain in build.gradle.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Resolve the Gradle distribution and dependencies first so they are cached
# separately from source changes. gradlew is stored without the executable bit.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# bootJar skips the tests, so no database is needed at build time.
RUN ./gradlew --no-daemon bootJar

# Runtime stage: JRE only.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
# Cloudtype recommends a non-root user (uid/gid 1000).
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
