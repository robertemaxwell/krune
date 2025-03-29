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
    // Initialize database connection
    initDatabase()
    
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