# RuneScape 317–Style MMO (Modern Kotlin + WebGL)

## Table of Contents
1. [Overview](#overview)
2. [Tech Stack Summary](#tech-stack-summary)
3. [Repository Structure](#repository-structure)
4. [Server Setup](#server-setup)
5. [Database Setup](#database-setup)
6. [Client Setup](#client-setup)
7. [Running the Project Locally](#running-the-project-locally)
8. [Docker & Containerization](#docker--containerization)
9. [Kubernetes & Scaling](#kubernetes--scaling)
10. [Gameplay & Architecture Concepts](#gameplay--architecture-concepts)
11. [Next Steps & Roadmap](#next-steps--roadmap)
12. [Contributing](#contributing)
13. [License](#license)
14. [CI/CD Pipeline](#ci-cd-pipeline)

## Overview

This project aims to recreate the RuneScape 317 experience using a modern stack for performance, scalability, and maintainability:
* Server written in Kotlin (running on the JVM).
* Real-time networking via WebSockets (instead of the old Java socket approach).
* Web-based client (HTML5 + WebGL) so players can just open a browser and play—no desktop downloads, no Java applets.
* Modern dev and ops tools (Docker, possibly Kubernetes, CI/CD, etc.).

This README details how to get the project running locally on a Mac (though it should be broadly applicable to Linux/Windows as well) and how each component fits together.

## Tech Stack Summary

### 1. Server
* Kotlin (JVM 17+)
* Ktor for HTTP/WebSocket server (or Netty if you prefer bare metal)
* Coroutines for concurrency and game loop scheduling

### 2. Database
* PostgreSQL (recommended) or MySQL
* Exposed (Kotlin ORM) or alternative (Hibernate, raw SQL, etc.)

### 3. Client
* TypeScript + Babylon.js (or Three.js) for 3D in the browser
* Real-time communication via WebSockets to the Kotlin server

### 4. Caching (optional)
* Redis for ephemeral data or caching frequently accessed info

### 5. Containerization
* Docker images for both server and client
* Potential use of Kubernetes for scaling

### 6. Observability
* Prometheus + Grafana for metrics and dashboards
* Logs aggregated with something like ELK or Grafana Loki (optional)

## Repository Structure

A possible directory layout might look like this:

```
root/
 ├─ server/
 │   ├─ src/
 │   │   ├─ main/
 │   │   │   ├─ kotlin/
 │   │   │   │   └─ com/example/rsps/...
 │   │   │   └─ resources/
 │   │   └─ test/
 │   ├─ build.gradle.kts
 │   ├─ Dockerfile
 │   └─ README.md  (this file or a pointer to root README)
 ├─ client/
 │   ├─ src/
 │   │   └─ index.ts
 │   ├─ package.json
 │   ├─ tsconfig.json
 │   ├─ webpack.config.js (or similar bundler config)
 │   ├─ Dockerfile
 │   └─ public/
 │       └─ index.html
 ├─ docker-compose.yml
 ├─ kubernetes/
 │   └─ (YAML files for deployments, services, etc.)
 └─ README.md  (main project README)
```

You can adapt as needed, but separating the server and client folders keeps concerns neatly in their own spaces.

## Server Setup

Below are detailed steps on how to configure and run the server portion on your local machine.

### 1. Prerequisites
* Java 17 or newer installed (OpenJDK or similar).
* Kotlin (though you typically don't need a separate install if you're using Gradle/IntelliJ).
* Gradle (installed or just use the Gradle wrapper in the repository).
* IntelliJ IDEA (recommended) or another IDE that supports Kotlin.

### 2. Build Script (Gradle)

In server/build.gradle.kts (example snippet):

```kotlin
plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")
    implementation("io.ktor:ktor-server-websockets:2.2.4")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.5.4")
    // Logging, test, etc. (Logback, JUnit)
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.rsps.MainKt")
}
```

### 3. Ktor Server Example

In server/src/main/kotlin/com/example/rsps/Main.kt, you could have:

```kotlin
package com.example.rsps

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(30)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/game") {
                // Notify on connect
                send(Frame.Text("Connected to RuneScape 317–style server!"))

                // Handle incoming frames
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val clientMessage = frame.readText()
                        println("Received: $clientMessage")

                        // Echo back for now (placeholder for real game logic)
                        send(Frame.Text("Echo: $clientMessage"))
                    }
                }
            }

            // You can add standard REST endpoints here if needed (e.g. for login, account creation)
        }
    }.start(wait = true)
}
```

### 4. Game Loop & Additional Classes

You'll likely have more files for your:
* GameLoop (runs on a fixed tick cycle).
* World data structures.
* Player classes, NPC logic, Combat system, etc.

An extremely simplified game loop might look like:

```kotlin
package com.example.rsps.game

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

object GameLoop {
    private const val TICK_RATE_MS = 600L

    suspend fun runGameLoop() {
        while (true) {
            val elapsed = measureTimeMillis {
                // 1. Process pending commands from players
                // 2. Update NPCs, movement, etc.
                // 3. Send updates to connected clients
            }

            val sleepTime = TICK_RATE_MS - elapsed
            if (sleepTime > 0) {
                delay(sleepTime)
            }
        }
    }
}
```

Then in your main(), you could launch it as a coroutine (using runBlocking {} or top-level coroutines with a structured approach).

## Database Setup

Below are the recommended steps if you're on macOS.

### 1. Install PostgreSQL

```bash
brew install postgresql
brew services start postgresql
```

### 2. Create a database (e.g., rsps_dev)

```bash
createdb rsps_dev
```

### 3. Configure your connection
* By default, localhost:5432, username likely your macOS username, no password, or you can create a separate user.
* In Ktor, you might store these details in application.conf or environment variables.

Example of a simple Exposed init:

```kotlin
import org.jetbrains.exposed.sql.Database

fun initDatabase() {
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/rsps_dev",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "mysecretpassword"
    )
}
```

Then call initDatabase() on startup before the server begins accepting connections.

## Client Setup

A reference approach is TypeScript + Babylon.js. The structure might be:

```
client/
 ├─ package.json
 ├─ tsconfig.json
 ├─ src/
 │   └─ index.ts
 ├─ public/
 │   └─ index.html
 └─ ...
```

### 1. Install Node & Dependencies

On macOS:

```bash
brew install node
cd client
npm install
```

Make sure your package.json includes:

```json
{
  "name": "rsps-client",
  "version": "1.0.0",
  "scripts": {
    "build": "tsc && webpack",
    "start": "webpack-dev-server --open"
  },
  "dependencies": {
    "@babylonjs/core": "^6.0.0"
    // add more as needed
  },
  "devDependencies": {
    "typescript": "^4.9.5",
    "webpack": "^5.0.0",
    "webpack-cli": "^5.0.0",
    "webpack-dev-server": "^4.0.0"
  }
}
```

(Versions are just examples; you'd pick the latest stable.)

### 2. Basic tsconfig.json

```json
{
  "compilerOptions": {
    "target": "ES6",
    "module": "ESNext",
    "outDir": "dist",
    "strict": true,
    "esModuleInterop": true
  },
  "include": ["src/**/*"]
}
```

### 3. Webpack Config (e.g. webpack.config.js)

```javascript
const path = require("path");

module.exports = {
  entry: "./src/index.ts",
  output: {
    filename: "bundle.js",
    path: path.resolve(__dirname, "dist"),
  },
  mode: "development",
  devtool: "inline-source-map",
  resolve: {
    extensions: [".ts", ".js"],
  },
  module: {
    rules: [
      {
        test: /\.ts$/,
        use: "ts-loader",
        exclude: /node_modules/,
      },
    ],
  },
  devServer: {
    static: {
      directory: path.join(__dirname, "public"),
    },
    port: 3000,
    hot: true,
  },
};
```

### 4. Minimal index.html

```html
<!DOCTYPE html>
<html>
<head>
  <title>RuneScape 317 in the Browser</title>
  <meta charset="UTF-8">
</head>
<body>
  <canvas id="gameCanvas" width="800" height="600"></canvas>
  <script src="bundle.js"></script>
</body>
</html>
```

### 5. Example index.ts with Babylon.js & WebSocket

```typescript
import { Engine, Scene, ArcRotateCamera, HemisphericLight, MeshBuilder, Vector3 } from "@babylonjs/core";

window.addEventListener("DOMContentLoaded", () => {
  const canvas = document.getElementById("gameCanvas") as HTMLCanvasElement;
  const engine = new Engine(canvas, true);
  const scene = new Scene(engine);

  // Create a basic camera
  const camera = new ArcRotateCamera("camera", Math.PI / 2, Math.PI / 4, 10, Vector3.Zero(), scene);
  camera.attachControl(canvas, true);

  // Create a basic light
  const light = new HemisphericLight("light", new Vector3(0, 1, 0), scene);

  // Simple sphere
  const sphere = MeshBuilder.CreateSphere("sphere", { diameter: 2 }, scene);
  sphere.position.y = 1;

  // Simple ground
  MeshBuilder.CreateGround("ground", { width: 6, height: 6 }, scene);

  // Render loop
  engine.runRenderLoop(() => {
    scene.render();
  });

  // Resizing
  window.addEventListener("resize", () => {
    engine.resize();
  });

  // WebSocket to our Ktor server at ws://localhost:8080/game
  const socket = new WebSocket("ws://localhost:8080/game");

  socket.onopen = () => {
    console.log("Connected to server!");
    socket.send("Hello from the client!");
  };

  socket.onmessage = (event) => {
    console.log("Received:", event.data);
  };
});
```

## Running the Project Locally

### 1. Start the Database
* Ensure PostgreSQL is running:

```bash
brew services start postgresql
```

* If needed, create DB:

```bash
createdb rsps_dev
```

### 2. Start the Server

In a separate terminal:

```bash
cd server
./gradlew run
```

* By default, the server listens on http://localhost:8080 for HTTP and ws://localhost:8080/game for WebSockets.
* If everything is working, you should see console logs about the server starting.

### 3. Start the Client

In another terminal:

```bash
cd client
npm install
npm run build
npm run start
```

* This launches webpack-dev-server on http://localhost:3000 by default.
* Open a browser at http://localhost:3000.
* The client should connect to the server's WebSocket endpoint, log "Connected to server!" in the browser console, and you'll see logs on the server side.

## Docker & Containerization

For production or staging, you might want to run everything in Docker.

### 1. Server Dockerfile

In server/Dockerfile:

```dockerfile
# 1) Build stage
FROM gradle:7.5.1-jdk17 as builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 2) Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/server-all.jar server.jar
EXPOSE 8080
CMD ["java", "-jar", "server.jar"]
```

Then:

```bash
cd server
docker build -t rsps-server .
docker run -p 8080:8080 rsps-server
```

### 2. Client Dockerfile

In client/Dockerfile:

```dockerfile
# 1) Build stage
FROM node:19 as builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 2) Serve with nginx
FROM nginx:stable-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Then:

```bash
cd client
docker build -t rsps-client .
docker run -p 80:80 rsps-client
```

Now your game client is served at http://localhost:80. Make sure your server's WebSocket URL inside index.ts points to the correct hostname/port for production (not localhost if hosting separately).

## Kubernetes & Scaling

If you want auto-scaling or multi-world deployments:
1. Push both images (rsps-server, rsps-client) to a container registry (Docker Hub, ECR, GCR, etc.).
2. K8s deployments:
   * server-deployment.yaml with multiple replicas of your game server behind a LoadBalancer or NodePort.
   * client-deployment.yaml to serve the client or rely on a CDN.
3. Consider advanced networking for the real-time data. Typically, you'd route the TCP/WebSocket connections through a Kubernetes Service.
4. Statefulness: The game server might be stateful if you maintain a single "world." You can either keep all players on a single server or design multi-world logic.

## Gameplay & Architecture Concepts

### 1. Tick System:
* Classic RuneScape uses a ~600ms cycle. You can keep that or choose a different interval. Each tick updates NPCs, players, combat, etc.

### 2. WebSocket Protocol:
* Decide on a custom packet structure (binary or JSON).
* For instance, define an "opcode" system, reminiscent of old RS 317 packets, or just use JSON for easier debugging.
* E.g., {"type":"move","x":123,"y":456}

### 3. Authentication:
* Typically, you'd have a login endpoint or WebSocket handshake.
* Validate username/password in the database.
* Return a session token or keep it in memory until the user disconnects.

### 4. World Data:
* Store region data, item definitions, NPC spawn data, etc., in the database or external config.
* Cache these in memory (Redis or in-app memory) for faster access.

### 5. Rendering:
* The biggest leap from old 317 is rewriting or converting the 3D assets for WebGL.
* You'll need to handle animations, model loading, texturing, UI, etc., in the browser.

## Next Steps & Roadmap

- [ ] Implement Real Game Logic
  - [ ] Movement
  - [ ] Combat systems
  - [ ] Skill systems
  - [ ] Bank/inventory
  - [ ] NPC dialogues
  - [ ] Quests
- [ ] Asset Conversion
  - [ ] Convert or replicate old 317 asset files into a format readable by Babylon.js/Three.js
- [ ] Authentication Service
  - [ ] Create a separate microservice for auth
  - [ ] Username/password verification
  - [ ] Email verification
- [ ] More Microservices (if needed)
  - [ ] Chat service
  - [ ] Multiple world servers
- [ ] Security
  - [ ] SSL/TLS termination for WebSocket (wss://)
  - [ ] Protection against DDoS
  - [ ] Protection against invalid packets
- [ ] Load Testing & Monitoring
  - [ ] Integrate Prometheus metrics
  - [ ] Set up Grafana dashboards
  - [ ] Monitor CPU usage, memory, number of players

## Contributing

We welcome contributions! To get started:
1. Fork the repo and clone locally.
2. Create a feature branch for your changes.
3. Test thoroughly (server + client).
4. Submit a Pull Request with a clear description of what you changed and why.

We're especially interested in help on:
- [ ] Game engine features (combat, NPC AI, pathfinding)
- [ ] WebGL asset pipelines (loading and rendering 317-era models)
- [ ] UI/UX for the client (classic RS interface or modern spin?)

## License

This project is not affiliated with or endorsed by Jagex Ltd. It's a fan-made reimplementation inspired by the "RuneScape 317" era, using entirely custom code. All references to RuneScape are strictly for context.

You may release the project under an open-source license (e.g., MIT, GPL, etc.), but ensure you comply with any relevant intellectual property considerations for game assets, names, or trademarks. Check your local laws and Jagex's stance on private servers.

(Replace this section with the actual open-source license text you wish to use.)

## CI/CD Pipeline

The project uses GitHub Actions for continuous integration and deployment. The pipeline includes:

### Automated Workflow

1. **Build and Test Server**
   - Builds the Java/Kotlin server using Gradle
   - Runs server tests
   - Saves build artifacts

2. **Build and Test Client**
   - Installs Node.js dependencies
   - Builds the client application
   - Runs client tests
   - Saves build artifacts

3. **Docker Build**
   - Builds Docker images for both server and client
   - Pushes images to DockerHub registry (on main branch only)

4. **Deployment** (configured when ready)
   - Automates deployment to chosen environment

### Setup Requirements

For the CI/CD pipeline to work properly, add these secrets to your GitHub repository:

- `DOCKER_HUB_USERNAME`: Your Docker Hub username
- `DOCKER_HUB_TOKEN`: Your Docker Hub access token

### Manual Triggers

You can manually trigger the workflow from the Actions tab in GitHub.

---

Thanks for checking out this modern RuneScape 317–style project! We hope this guide helps you get everything running smoothly on macOS (or any other OS) with a modern Kotlin + WebGL architecture.