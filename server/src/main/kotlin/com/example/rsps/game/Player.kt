package com.example.rsps.game

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.slf4j.LoggerFactory
import java.lang.reflect.Field

/**
 * Represents a player in the game world
 */
class Player(
    val sessionId: String,
    private var session: WebSocketSession?
) {
    private val logger = LoggerFactory.getLogger(Player::class.java)
    
    // Database ID (null for players not yet saved)
    var id: Int? = null
    
    // Account information
    var username: String? = null
    
    // Position data
    var x: Int = 3222 // Default starting position
    var y: Int = 3222
    var z: Int = 0    // Plane/level
    
    // Stats
    var health: Int = 100
    
    // Movement data
    var needsPositionUpdate = false
    
    // Skills data (will be expanded)
    val skills: MutableMap<Int, Skill> = mutableMapOf()
    
    // Inventory (will be expanded)
    val inventory: MutableList<InventoryItem?> = MutableList(28) { null }
    
    /**
     * Whether this player is connected to an active session
     */
    val isConnected: Boolean
        get() = session != null
    
    /**
     * Updates the player state during a game tick
     */
    fun update() {
        // Process movement, combat, etc.
        // This will be expanded as more game systems are implemented
    }
    
    /**
     * Sends updates to the client
     */
    suspend fun sendUpdates() {
        if (!isConnected) return
        
        try {
            if (needsPositionUpdate) {
                // In a real implementation, this would serialize proper game packets
                // For now, we'll just send simple text messages
                session?.send(Frame.Text("POS:$x,$y,$z"))
                needsPositionUpdate = false
            }
        } catch (e: ClosedSendChannelException) {
            logger.warn("Failed to send update to player $sessionId - connection closed")
        } catch (e: Exception) {
            logger.error("Error sending update to player $sessionId: ${e.message}", e)
        }
    }
    
    /**
     * Handle incoming message from client
     */
    suspend fun handleMessage(message: String) {
        // Very simple command parsing for now
        when {
            message.startsWith("MOVE:") -> {
                val parts = message.substringAfter("MOVE:").split(",")
                if (parts.size >= 2) {
                    try {
                        val newX = parts[0].toInt()
                        val newY = parts[1].toInt()
                        
                        // Update position
                        x = newX
                        y = newY
                        needsPositionUpdate = true
                        
                        logger.debug("Player $sessionId moved to $x,$y,$z")
                    } catch (e: NumberFormatException) {
                        logger.warn("Invalid movement command: $message")
                    }
                }
            }
        }
    }
    
    /**
     * Updates the WebSocket session for this player
     */
    fun attachSession(newSessionId: String, newSession: WebSocketSession) {
        // Update session field using reflection since it's a private val
        try {
            val field = this.javaClass.getDeclaredField("session")
            field.isAccessible = true
            field.set(this, newSession)
            
            // Set session ID (hacky but effective for this implementation)
            val sessionIdField = this.javaClass.getDeclaredField("sessionId")
            sessionIdField.isAccessible = true
            sessionIdField.set(this, newSessionId)
            
            logger.info("Session attached: $newSessionId")
            
            // Mark player as needing a position update to sync with client
            needsPositionUpdate = true
        } catch (e: Exception) {
            logger.error("Failed to attach session: ${e.message}", e)
        }
    }
}

/**
 * Represents a player skill
 */
data class Skill(
    val id: Int,
    var level: Int = 1,
    var experience: Double = 0.0
)

/**
 * Represents an item in the player's inventory
 */
data class InventoryItem(
    val itemId: Int,
    var amount: Int = 1
) 