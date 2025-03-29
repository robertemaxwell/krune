FROM amazoncorretto:17-alpine

WORKDIR /app

# Install required packages
RUN apk add --no-cache bash

# Copy the entire project
COPY . .

# Expose the server port
EXPOSE 8080

# Set entrypoint to use Gradle to run the application
ENTRYPOINT ["./gradlew", "run", "--no-daemon"]
