FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the JAR file
COPY build/libs/*.jar app.jar

# Expose the server port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"] 