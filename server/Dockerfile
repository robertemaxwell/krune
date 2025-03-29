FROM gradle:7.5.1-jdk17 as builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar server.jar
EXPOSE 8080
CMD ["java", "-jar", "server.jar"] 