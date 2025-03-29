package com.example.rsps.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * The main game engine responsible for the game loop and state management.
 * Runs on a 600ms tick rate (approximately 1.67 ticks per second).
 */
class GameEngine {
    private val logger = LoggerFactory.getLogger(GameEngine::class.java)
    private val isRunning = AtomicBoolean(false)
    private val engineScope = CoroutineScope(Dispatchers.Default)
    
    // Game state
    private val players = ConcurrentHashMap<String, Player>()
    private val playersByUsername = ConcurrentHashMap<String, String>() // Maps username to sessionId
    private val sessionTimeouts = ConcurrentHashMap<String, Instant>() // Maps sessionId to last activity time
    
    // Tick constants
    companion object {
        const val TICK_RATE_MS = 600L
        const val SESSION_TIMEOUT_MINUTES = 30L // Timeout for inactive sessions
    }
    
    /**
     * Starts the game engine loop
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            logger.info("Starting game engine with tick rate: ${TICK_RATE_MS}ms")
            
            engineScope.launch {
                while (isRunning.get()) {
                    val startTime = System.currentTimeMillis()
                    
                    try {
                        tick()
                    } catch (e: Exception) {
                        logger.error("Error during game tick: ${e.message}", e)
                    }
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    val sleepTime = (TICK_RATE_MS - elapsed).coerceAtLeast(0)
                    
                    if (elapsed > TICK_RATE_MS) {
                        logger.warn("Tick took longer than tick rate: ${elapsed}ms")
                    }
                    
                    delay(sleepTime)
                }
            }
            
            // Start session management task
            engineScope.launch {
                while (isRunning.get()) {
                    try {
                        manageInactiveSessions()
                    } catch (e: Exception) {
                        logger.error("Error during session management: ${e.message}", e)
                    }
                    
                    // Check inactive sessions every minute
                    delay(60000)
                }
            }
        }
    }
    
    /**
     * Stops the game engine
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            logger.info("Stopping game engine")
        }
    }
    
    /**
     * Adds a player to the game
     */
    fun addPlayer(player: Player) {
        players[player.sessionId] = player
        player.username?.let { username ->
            playersByUsername[username] = player.sessionId
        }
        updateSessionActivity(player.sessionId)
        logger.info("Player added: ${player.sessionId}")
    }
    
    /**
     * Removes a player from the game
     */
    fun removePlayer(sessionId: String) {
        val player = players[sessionId]
        player?.username?.let { username ->
            playersByUsername.remove(username)
        }
        players.remove(sessionId)
        sessionTimeouts.remove(sessionId)
        logger.info("Player removed: $sessionId")
    }
    
    /**
     * Gets a player by session ID
     */
    fun getPlayer(sessionId: String): Player? {
        val player = players[sessionId]
        player?.let { updateSessionActivity(sessionId) }
        return player
    }
    
    /**
     * Gets a player by username
     */
    fun getPlayerByUsername(username: String): Player? {
        val sessionId = playersByUsername[username] ?: return null
        return getPlayer(sessionId)
    }
    
    /**
     * Replaces a player in the game engine with an updated player (e.g. after loading from database)
     */
    fun replacePlayer(oldPlayer: Player, newPlayer: Player) {
        // Remove the old player
        removePlayer(oldPlayer.sessionId)
        
        // Add the new player
        addPlayer(newPlayer)
        
        // Update username mapping if username is available
        newPlayer.username?.let { username ->
            playersByUsername[username] = newPlayer.sessionId
        }
        
        logger.info("Player replaced: ${oldPlayer.sessionId} -> ${newPlayer.sessionId}")
    }
    
    /**
     * Updates the last activity time for a session
     */
    fun updateSessionActivity(sessionId: String) {
        sessionTimeouts[sessionId] = Instant.now()
    }
    
    /**
     * Check and remove inactive sessions
     */
    private fun manageInactiveSessions() {
        val now = Instant.now()
        val inactiveSessions = sessionTimeouts.entries
            .filter { (_, lastActivity) -> 
                lastActivity.plus(SESSION_TIMEOUT_MINUTES, ChronoUnit.MINUTES).isBefore(now)
            }
            .map { it.key }
        
        if (inactiveSessions.isNotEmpty()) {
            logger.info("Removing ${inactiveSessions.size} inactive sessions")
            inactiveSessions.forEach { sessionId ->
                val player = players[sessionId]
                if (player != null && player.id != null) {
                    // Save player data before removing
                    // This would normally call the repository, but we'll just log it here
                    logger.info("Would save data for inactive player: ${player.username ?: "unknown"}")
                }
                removePlayer(sessionId)
            }
        }
    }
    
    /**
     * The main game tick logic - processes one frame of game state
     */
    private suspend fun tick() {
        // Process player updates
        players.values.forEach { player ->
            player.update()
        }
        
        // Send updates to all connected players
        players.values.forEach { player ->
            player.sendUpdates()
        }
        
        // Check for player interactions
        processPlayerInteractions()
    }
    
    /**
     * Process interactions between players
     */
    private fun processPlayerInteractions() {
        // This is a simplified placeholder for player interactions
        // In a complete implementation, this would handle:
        // - Players seeing each other
        // - Combat between players
        // - Trading
        // - Communication
        
        // For now, we'll just check if players are in the same area
        val playerList = players.values.toList()
        
        for (i in playerList.indices) {
            val player1 = playerList[i]
            
            // Check for nearby players
            for (j in i + 1 until playerList.size) {
                val player2 = playerList[j]
                
                // Calculate Manhattan distance between players
                val distance = Math.abs(player1.x - player2.x) + Math.abs(player1.y - player2.y)
                
                // If players are on the same plane and within viewing distance
                if (player1.z == player2.z && distance <= 14) {
                    // Players can see each other
                    // In a real implementation, this would add each player to the other's update list
                }
            }
        }
    }
} 