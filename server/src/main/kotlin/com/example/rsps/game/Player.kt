package com.example.rsps.game

import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

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
    var lastLoginTime: Long = 0
    var creationTime: Long = 0
    
    // Position data
    var x: Int = 3222 // Default starting position
    var y: Int = 3222
    var z: Int = 0    // Plane/level
    
    // Character appearance
    var gender: Int = 0 // 0 for male, 1 for female
    var headModel: Int = 0
    var bodyModel: Int = 18
    var legModel: Int = 26
    var feetModel: Int = 36
    var handModel: Int = 33
    var hairColor: Int = 0
    var bodyColor: Int = 0
    
    // Stats
    var health: Int = 100
    var runEnergy: Int = 100
    var combatLevel: Int = 3
    
    // Movement data
    var needsPositionUpdate = false
    var isRunning = false
    var direction: Int = 0 // 0-7 for the 8 possible directions
    var destinationX: Int? = null
    var destinationY: Int? = null
    private val isMoving = AtomicBoolean(false)
    
    // State flags
    var isBusy = false // Indicates player is busy with an action
    var isInCombat = false
    var isTeleporting = false
    
    // Skills data (will be expanded)
    val skills: MutableMap<Int, Skill> = mutableMapOf()
    
    // Initialize with default skills
    init {
        // Set up default skills (1-23 in RuneScape)
        for (skillId in 0 until 23) {
            skills[skillId] = Skill(id = skillId)
        }
    }
    
    // Inventory (will be expanded)
    val inventory: MutableList<InventoryItem?> = MutableList(28) { null }
    
    // Equipment slots
    val equipment: MutableMap<Int, EquipmentItem> = mutableMapOf()
    
    /**
     * Whether this player is connected to an active session
     */
    val isConnected: Boolean
        get() = session != null
    
    /**
     * Updates the player state during a game tick
     */
    fun update() {
        // Process movement
        processMovement()
        
        // Process combat, skills, etc. if needed
        if (isInCombat) {
            // Process combat logic
        }
        
        // Restore run energy if needed
        if (runEnergy < 100 && !isRunning) {
            runEnergy = (runEnergy + 1).coerceAtMost(100)
        }
    }
    
    /**
     * Process player movement for the current tick
     */
    private fun processMovement() {
        if (isMoving.get() && destinationX != null && destinationY != null) {
            // Calculate movement toward destination
            val dx = destinationX!! - x
            val dy = destinationY!! - y
            
            if (dx == 0 && dy == 0) {
                // Reached destination
                isMoving.set(false)
                destinationX = null
                destinationY = null
                return
            }
            
            // Determine movement direction (simplified)
            val moveX = dx.coerceIn(-1, 1)
            val moveY = dy.coerceIn(-1, 1)
            
            // Update position
            x += moveX
            y += moveY
            
            // Update direction (0-7, clockwise from north)
            direction = when {
                moveX == 0 && moveY > 0 -> 0  // North
                moveX > 0 && moveY > 0 -> 1   // Northeast
                moveX > 0 && moveY == 0 -> 2  // East
                moveX > 0 && moveY < 0 -> 3   // Southeast
                moveX == 0 && moveY < 0 -> 4  // South
                moveX < 0 && moveY < 0 -> 5   // Southwest
                moveX < 0 && moveY == 0 -> 6  // West
                else -> 7                     // Northwest
            }
            
            needsPositionUpdate = true
        }
    }
    
    /**
     * Start movement to a destination
     */
    fun moveTo(destX: Int, destY: Int) {
        if (!isBusy && !isTeleporting) {
            destinationX = destX
            destinationY = destY
            isMoving.set(true)
            needsPositionUpdate = true
            logger.debug("Player $sessionId moving to $destX,$destY")
        }
    }
    
    /**
     * Teleport player to a specific location
     */
    fun teleport(newX: Int, newY: Int, newZ: Int = 0) {
        isTeleporting = true
        
        // Cancel any current movement
        destinationX = null
        destinationY = null
        isMoving.set(false)
        
        // Update position
        x = newX
        y = newY
        z = newZ
        
        needsPositionUpdate = true
        isTeleporting = false
        
        logger.debug("Player $sessionId teleported to $x,$y,$z")
    }
    
    /**
     * Get skill level by skill ID
     */
    fun getSkillLevel(skillId: Int): Int {
        return skills[skillId]?.level ?: 1
    }
    
    /**
     * Add experience to a skill
     */
    fun addExperience(skillId: Int, exp: Double) {
        val skill = skills[skillId] ?: return
        skill.experience += exp
        
        // Calculate new level based on experience
        val newLevel = calculateLevelFromExperience(skill.experience)
        if (newLevel > skill.level) {
            skill.level = newLevel
            // Would send level-up message here
        }
    }
    
    /**
     * Calculate level from experience points
     */
    private fun calculateLevelFromExperience(exp: Double): Int {
        var points = 0
        var output = 0.0
        for (lvl in 1..99) {
            points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0)).toInt()
            output = Math.floor(points / 4.0)
            if (output >= exp) {
                return lvl
            }
        }
        return 99
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
                session?.send(Frame.Text("POSITION:$x:$y:$z:$direction"))
                needsPositionUpdate = false
            }
            
            // Send any other updates needed
            // - Health updates
            // - Skill updates
            // - Inventory updates
            // - Equipment updates
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
        // Command parsing
        when {
            message.startsWith("MOVE:") -> {
                val parts = message.substringAfter("MOVE:").split(",")
                if (parts.size >= 2) {
                    try {
                        val destX = parts[0].toInt()
                        val destY = parts[1].toInt()
                        
                        // Start movement to destination
                        moveTo(destX, destY)
                    } catch (e: NumberFormatException) {
                        logger.warn("Invalid movement command: $message")
                    }
                }
            }
            message.startsWith("TELEPORT:") -> {
                val parts = message.substringAfter("TELEPORT:").split(",")
                if (parts.size >= 3) {
                    try {
                        val newX = parts[0].toInt()
                        val newY = parts[1].toInt()
                        val newZ = parts[2].toInt()
                        
                        // Teleport player
                        teleport(newX, newY, newZ)
                    } catch (e: NumberFormatException) {
                        logger.warn("Invalid teleport command: $message")
                    }
                }
            }
            message.startsWith("RUN:") -> {
                val value = message.substringAfter("RUN:").toBoolean()
                isRunning = value
                session?.send(Frame.Text("RUN_TOGGLE:$isRunning"))
            }
        }
    }
    
    /**
     * Updates the WebSocket session for this player
     */
    fun attachSession(newSessionId: String, newSession: WebSocketSession) {
        session = newSession
        logger.info("Session attached: $newSessionId")
        
        // Mark player as needing a position update to sync with client
        needsPositionUpdate = true
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

/**
 * Represents an equipped item
 */
data class EquipmentItem(
    val itemId: Int,
    val slot: Int
) 