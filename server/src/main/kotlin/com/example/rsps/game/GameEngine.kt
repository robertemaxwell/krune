package com.example.rsps.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

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
    
    // Tick constants
    companion object {
        const val TICK_RATE_MS = 600L
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
        logger.info("Player added: ${player.sessionId}")
    }
    
    /**
     * Removes a player from the game
     */
    fun removePlayer(sessionId: String) {
        players.remove(sessionId)
        logger.info("Player removed: $sessionId")
    }
    
    /**
     * Gets a player by session ID
     */
    fun getPlayer(sessionId: String): Player? {
        return players[sessionId]
    }
    
    /**
     * Replaces a player in the game engine with an updated player (e.g. after loading from database)
     */
    fun replacePlayer(oldPlayer: Player, newPlayer: Player) {
        // Remove the old player
        removePlayer(oldPlayer.sessionId)
        
        // Add the new player
        addPlayer(newPlayer)
        
        logger.info("Player replaced: ${oldPlayer.sessionId} -> ${newPlayer.sessionId}")
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
    }
} 