#!/bin/bash

# Make sure wscat is installed
if ! command -v wscat &> /dev/null
then
    echo "wscat could not be found, installing..."
    npm install -g wscat
fi

# Print test commands
echo "WebSocket Test Client"
echo "====================="
echo "Commands to test:"
echo "1. REGISTER:myuser:mypassword - Register a new user"
echo "2. LOGIN:myuser:mypassword - Login with registered user"
echo "3. MOVE:3230:3230 - Move player to coordinates"
echo ""

# Connect to WebSocket server
wscat -c ws://localhost:8080/game 