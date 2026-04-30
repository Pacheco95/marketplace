FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
