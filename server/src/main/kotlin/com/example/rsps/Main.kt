package com.example.rsps

import com.example.rsps.game.GameEngine
import com.example.rsps.game.Player
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

fun main() {
    val logger = LoggerFactory.getLogger("com.example.rsps.Main")
    logger.info("Initializing RuneScape 317 style server...")
    
    // Initialize database connection
    logger.info("Setting up database connection...")
    initDatabase()
    
    // Initialize game engine
    logger.info("Starting game engine...")
    val gameEngine = GameEngine()
    gameEngine.start()
    
    logger.info("Starting WebSocket server...")
    embeddedServer(Netty, port = 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(30)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }

        routing {
            webSocket("/game") {
                // Generate a session ID for this connection
                val sessionId = UUID.randomUUID().toString()
                logger.info("New connection: $sessionId")
                
                try {
                    // Create player and add to the game
                    val player = Player(sessionId, this)
                    gameEngine.addPlayer(player)
                    
                    // Notify client of successful connection
                    send(Frame.Text("CONNECTED:$sessionId"))
                    
                    // Handle incoming messages
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            logger.debug("Received from $sessionId: $message")
                            
                            // Let the player handle this message
                            player.handleMessage(message)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error in connection $sessionId: ${e.message}", e)
                } finally {
                    // Remove player when connection closes
                    logger.info("Connection closed: $sessionId")
                    gameEngine.removePlayer(sessionId)
                }
            }

            // You can add standard REST endpoints here if needed (e.g. for login, account creation)
        }
    }.start(wait = true)
} 