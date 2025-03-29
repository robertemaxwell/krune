package com.example.rsps.game

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.slf4j.LoggerFactory

/**
 * Represents a player in the game world
 */
class Player(
    val sessionId: String,
    private val session: WebSocketSession
) {
    private val logger = LoggerFactory.getLogger(Player::class.java)
    
    // Position data
    var x: Int = 3222 // Default starting position
    var y: Int = 3222
    var z: Int = 0    // Plane/level
    
    // Movement data
    var needsPositionUpdate = false
    
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
        try {
            if (needsPositionUpdate) {
                // In a real implementation, this would serialize proper game packets
                // For now, we'll just send simple text messages
                session.send(Frame.Text("POS:$x,$y,$z"))
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
} 