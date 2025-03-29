#!/bin/bash

# Test script for HTTP endpoints

echo "Testing server connectivity..."
curl -v http://localhost:8080

echo ""
echo "Note: For WebSocket testing, you can use a browser-based WebSocket client like:"
echo "- https://www.piesocket.com/websocket-tester"
echo "- https://websocketking.com/"
echo ""
echo "Connect to: ws://localhost:8080/game"
echo ""
echo "Commands to test:"
echo "1. REGISTER:myuser:mypassword - Register a new user"
echo "2. LOGIN:myuser:mypassword - Login with registered user" 
echo "3. MOVE:3230:3230 - Move player to coordinates" 