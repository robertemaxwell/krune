package com.example.rsps

import com.example.rsps.game.GameEngine
import com.example.rsps.game.Player
import com.example.rsps.game.PlayerRepository
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
    
    // Create player repository
    val playerRepository = PlayerRepository()
    
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
                            
                            // Handle different message types
                            handleMessage(message, player, gameEngine, playerRepository)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error in connection $sessionId: ${e.message}", e)
                } finally {
                    // Save player data before disconnect
                    val player = gameEngine.getPlayer(sessionId)
                    if (player != null && player.id != null) {
                        logger.info("Saving player data for ${player.username ?: "unknown"}")
                        playerRepository.savePlayer(player)
                    }
                    
                    // Remove player when connection closes
                    logger.info("Connection closed: $sessionId")
                    gameEngine.removePlayer(sessionId)
                }
            }

            // REST endpoint for player status (optional)
            get("/status") {
                // This would return server status information
                // Implementation omitted for brevity
            }
        }
    }.start(wait = true)
}

/**
 * Handle different message types from client
 */
private suspend fun DefaultWebSocketServerSession.handleMessage(
    message: String,
    player: Player,
    gameEngine: GameEngine,
    playerRepository: PlayerRepository
) {
    val logger = LoggerFactory.getLogger("com.example.rsps.MessageHandler")
    
    when {
        // Handle login requests
        message.startsWith("LOGIN:") -> {
            val parts = message.substringAfter("LOGIN:").split(":")
            if (parts.size >= 2) {
                val username = parts[0]
                val password = parts[1]
                
                // Check if player is already logged in
                val existingPlayer = gameEngine.getPlayerByUsername(username)
                if (existingPlayer != null) {
                    send(Frame.Text("LOGIN_FAILED:Already logged in"))
                    return
                }
                
                // Authenticate player
                val entity = playerRepository.authenticatePlayer(username, password)
                if (entity != null) {
                    // Load player data from database
                    val dbPlayer = playerRepository.loadPlayer(entity.id.value)
                    if (dbPlayer != null) {
                        // Update session info
                        dbPlayer.attachSession(player.sessionId, this)
                        
                        // Replace player in game engine
                        gameEngine.replacePlayer(player, dbPlayer)
                        
                        // Send success message
                        send(Frame.Text("LOGIN_SUCCESS:${entity.id.value}:$username"))
                        logger.info("Player $username logged in")
                        
                        // Send initial game state
                        dbPlayer.needsPositionUpdate = true // Force position update
                        dbPlayer.sendUpdates()
                    } else {
                        send(Frame.Text("LOGIN_FAILED:Could not load player data"))
                    }
                } else {
                    send(Frame.Text("LOGIN_FAILED:Invalid username or password"))
                }
            }
        } 
        // Handle registration requests
        message.startsWith("REGISTER:") -> {
            val parts = message.substringAfter("REGISTER:").split(":")
            if (parts.size >= 2) {
                val username = parts[0]
                val password = parts[1]
                
                // Validate username and password
                if (username.length < 3 || username.length > 12) {
                    send(Frame.Text("REGISTER_FAILED:Username must be 3-12 characters"))
                    return
                }
                
                if (password.length < 5) {
                    send(Frame.Text("REGISTER_FAILED:Password must be at least 5 characters"))
                    return
                }
                
                // Check if username exists
                val existing = playerRepository.findPlayerByUsername(username)
                if (existing != null) {
                    send(Frame.Text("REGISTER_FAILED:Username already exists"))
                } else {
                    // Create new player
                    val entity = playerRepository.createPlayer(username, password)
                    if (entity != null) {
                        // Set player data
                        player.id = entity.id.value
                        player.username = username
                        player.creationTime = System.currentTimeMillis()
                        player.lastLoginTime = System.currentTimeMillis()
                        
                        // Initialize default skills
                        for (skillId in 0 until 23) {
                            player.skills[skillId] = com.example.rsps.game.Skill(id = skillId)
                        }
                        
                        // Save to database
                        playerRepository.savePlayer(player)
                        
                        // Update username mapping in game engine
                        gameEngine.updateSessionActivity(player.sessionId)
                        
                        // Send success message
                        send(Frame.Text("REGISTER_SUCCESS:${entity.id.value}:$username"))
                        logger.info("New player registered: $username")
                        
                        // Send initial game state
                        player.needsPositionUpdate = true
                        player.sendUpdates()
                    } else {
                        send(Frame.Text("REGISTER_FAILED:Registration failed"))
                    }
                }
            }
        }
        // Handle logout requests
        message.startsWith("LOGOUT") -> {
            if (player.id != null) {
                logger.info("Player ${player.username} logging out")
                
                // Save player data
                playerRepository.savePlayer(player)
                
                // Send logout confirmation
                send(Frame.Text("LOGOUT_SUCCESS"))
                
                // Don't remove the player yet - client will disconnect and that will trigger removal
            }
        }
        // Let player handle gameplay messages
        else -> {
            player.handleMessage(message)
            
            // Update session activity timestamp
            gameEngine.updateSessionActivity(player.sessionId)
        }
    }
} 