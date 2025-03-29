# Database Testing Guide for RuneScape MMO

This document provides a step-by-step guide to testing the database implementation for the RuneScape-style MMO.

## Prerequisites

- Docker and Docker Compose installed
- JDK 17 or higher installed
- A web browser for the WebSocket client

## Setup Environment

1. **Start the PostgreSQL database container**

   ```bash
   docker-compose up -d database
   ```

   This will start the PostgreSQL database on port 5434.

2. **Start the server**

   ```bash
   cd server
   ./gradlew run
   ```

   The server will start on port 8080.

3. **Open the WebSocket test client**

   ```bash
   open websocket-client.html
   ```

   This will open the client in your default browser.

## Test Scenarios

### 1. Player Registration

1. In the WebSocket client, click "Connect" to establish a connection to the server.
2. Click the "Register User" preset button (or manually enter `REGISTER:myuser:mypassword`).
3. Click "Send" to send the registration command.
4. You should receive a response like: `REGISTER_SUCCESS:1:myuser`.

### 2. Player Login

1. Click the "Login User" preset button (or manually enter `LOGIN:myuser:mypassword`).
2. Click "Send" to send the login command.
3. You should receive a response like: `LOGIN_SUCCESS:1:myuser`.

### 3. Test Invalid Login

1. Manually enter `LOGIN:myuser:wrongpassword` in the message input.
2. Click "Send" to send the login command.
3. You should receive a response: `LOGIN_FAILED:Invalid username or password`.

### 4. Test Player Movement

1. Make sure you're logged in.
2. Click the "Move Player" preset button (or manually enter `MOVE:3230:3230`).
3. Click "Send" to send the move command.
4. You should receive a response like: `POS:3230:3230:0`.

### 5. Test Persistence

1. After registering, logging in, and moving your player, close the WebSocket connection.
2. Open a new connection.
3. Log in with the same credentials.
4. You should see your player's position has been saved.

## Database Verification

You can verify that data is being saved to the database by connecting to it directly:

```bash
docker exec -it krune-database-1 psql -U postgres -d rsps_dev
```

Then run SQL queries to check data:

```sql
-- Show all players
SELECT * FROM players;

-- Show player skills
SELECT * FROM player_skills;

-- Show player inventory
SELECT * FROM player_inventory;
```

## Running Unit Tests

The project includes database unit tests that you can run:

```bash
cd server
./gradlew test --tests "com.example.rsps.DatabaseTest"
```

## Troubleshooting

- **Connection Issues**: Make sure Docker is running and the database container is up.
- **Port Conflicts**: If port 5434 is already in use, modify docker-compose.yml to use a different port.
- **Database Errors**: Check the server logs for any database connection errors.

## Cleanup

When you're done testing, you can clean up resources:

```bash
docker-compose down
```

This will stop and remove the database container.

## Next Steps

After verifying the database implementation, the next steps are:

1. Implement additional player data persistence (equipment, bank, etc.)
2. Add transaction handling and error recovery
3. Implement backup and restore functionality
4. Optimize database queries for performance 