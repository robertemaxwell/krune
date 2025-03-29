#!/bin/bash

# Test script for database integration

# Start the database container
echo "Starting PostgreSQL database container..."
docker-compose up -d database
sleep 5  # Wait for database to initialize

# Build the server
echo "Building server..."
cd server
./gradlew build
cd ..

# Run the database tests
echo "Running database tests..."
cd server
./gradlew test --tests "com.example.rsps.DatabaseTest"
TEST_RESULT=$?
cd ..

if [ $TEST_RESULT -eq 0 ]; then
    echo "Database tests passed!"
else
    echo "Database tests failed!"
    docker-compose down
    exit 1
fi

# Run the WebSocket tests
echo "Running WebSocket tests..."
cd server
./gradlew test --tests "com.example.rsps.WebSocketTest"
WS_TEST_RESULT=$?
cd ..

if [ $WS_TEST_RESULT -eq 0 ]; then
    echo "WebSocket tests passed!"
else
    echo "WebSocket tests failed!"
    docker-compose down
    exit 1
fi

# Run the server in the background
echo "Starting server..."
cd server
./gradlew run &
SERVER_PID=$!
cd ..
sleep 5  # Wait for server to start

# Test server with curl
echo "Testing server connectivity..."
curl -v -k http://localhost:8080

# Prompt for manual test with WebSocket client
echo ""
echo "Server is running on port 8080."
echo "You can now test the WebSocket connection manually using a WebSocket client."
echo "Connect to: ws://localhost:8080/game"
echo ""
echo "Send these commands to test registration and login:"
echo "1. REGISTER:myuser:mypassword"
echo "2. LOGIN:myuser:mypassword"
echo ""
echo "Press Enter to stop the server and clean up..."
read

# Clean up
kill $SERVER_PID
docker-compose down

echo "All tests completed and resources cleaned up." 