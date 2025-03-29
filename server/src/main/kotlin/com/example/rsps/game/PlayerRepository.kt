package com.example.rsps.game

import com.example.rsps.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.*

/**
 * Repository for player database operations
 */
class PlayerRepository {
    private val logger = LoggerFactory.getLogger(PlayerRepository::class.java)
    
    /**
     * Creates a new player in the database
     */
    fun createPlayer(username: String, password: String): PlayerEntity? {
        return try {
            transaction {
                val passwordHash = hashPassword(password)
                PlayerEntity.new {
                    this.username = username
                    this.passwordHash = passwordHash
                    this.createdAt = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create player: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds a player by username
     */
    fun findPlayerByUsername(username: String): PlayerEntity? {
        return try {
            transaction {
                PlayerEntity.find { Players.username eq username }.firstOrNull()
            }
        } catch (e: Exception) {
            logger.error("Failed to find player by username: ${e.message}", e)
            null
        }
    }
    
    /**
     * Authenticates a player using username and password
     */
    fun authenticatePlayer(username: String, password: String): PlayerEntity? {
        return try {
            transaction {
                val passwordHash = hashPassword(password)
                PlayerEntity.find { 
                    (Players.username eq username) and (Players.passwordHash eq passwordHash) 
                }.firstOrNull()
            }
        } catch (e: Exception) {
            logger.error("Failed to authenticate player: ${e.message}", e)
            null
        }
    }
    
    /**
     * Saves player data to the database
     */
    fun savePlayer(player: Player): Boolean {
        return try {
            transaction {
                val entity = player.id?.let { 
                    PlayerEntity.findById(it)
                } ?: PlayerEntity.new {
                    username = player.username ?: "unnamed_${UUID.randomUUID()}"
                    passwordHash = "" // This would be set during registration/authentication
                    createdAt = System.currentTimeMillis()
                }
                
                // Update entity fields from player object
                entity.x = player.x
                entity.y = player.y
                entity.z = player.z
                entity.health = player.health
                entity.lastLogin = System.currentTimeMillis()
                
                // Ensure player has this entity's ID for future updates
                player.id = entity.id.value
                
                // Save skills
                savePlayerSkills(player)
                
                // Save inventory
                savePlayerInventory(player)
                
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to save player data: ${e.message}", e)
            false
        }
    }
    
    /**
     * Loads player data from the database
     */
    fun loadPlayer(id: Int): Player? {
        return try {
            transaction {
                val entity = PlayerEntity.findById(id) ?: return@transaction null
                
                // Create new player object without an active session
                val sessionId = "db_${UUID.randomUUID()}"
                val player = Player(sessionId, null)
                
                // Set player properties from entity
                player.id = entity.id.value
                player.username = entity.username
                player.x = entity.x
                player.y = entity.y
                player.z = entity.z
                player.health = entity.health
                
                // Load skills
                loadPlayerSkills(player)
                
                // Load inventory
                loadPlayerInventory(player)
                
                player
            }
        } catch (e: Exception) {
            logger.error("Failed to load player: ${e.message}", e)
            null
        }
    }
    
    /**
     * Saves player skills to the database
     */
    private fun savePlayerSkills(player: Player) {
        transaction {
            val playerId = player.id ?: return@transaction
            
            // Get existing skills
            val existingSkills = PlayerSkillEntity.find { 
                PlayerSkills.playerId eq playerId 
            }.associateBy { it.skillId }
            
            // Update or create skills
            for ((skillId, skill) in player.skills) {
                val skillEntity = existingSkills[skillId] ?: PlayerSkillEntity.new {
                    this.player = PlayerEntity[playerId]
                    this.skillId = skillId
                }
                
                skillEntity.level = skill.level
                skillEntity.experience = skill.experience
            }
        }
    }
    
    /**
     * Loads player skills from the database
     */
    private fun loadPlayerSkills(player: Player) {
        transaction {
            val playerId = player.id ?: return@transaction
            
            // Clear existing skills
            player.skills.clear()
            
            // Load skills from database
            PlayerSkillEntity.find { 
                PlayerSkills.playerId eq playerId 
            }.forEach { entity ->
                player.skills[entity.skillId] = Skill(
                    id = entity.skillId,
                    level = entity.level,
                    experience = entity.experience
                )
            }
        }
    }
    
    /**
     * Saves player inventory to the database
     */
    private fun savePlayerInventory(player: Player) {
        transaction {
            val playerId = player.id ?: return@transaction
            
            // Get existing inventory items
            val existingItems = PlayerInventoryItemEntity.find { 
                PlayerInventory.playerId eq playerId 
            }.associateBy { it.slot }
            
            // Update or create inventory items
            player.inventory.forEachIndexed { slot, item ->
                if (item != null) {
                    val itemEntity = existingItems[slot] ?: PlayerInventoryItemEntity.new {
                        this.player = PlayerEntity[playerId]
                        this.slot = slot
                        this.itemId = item.itemId
                        this.amount = item.amount
                    }
                    
                    itemEntity.itemId = item.itemId
                    itemEntity.amount = item.amount
                } else {
                    // Delete item if it exists in the database but is null in player inventory
                    existingItems[slot]?.delete()
                }
            }
        }
    }
    
    /**
     * Loads player inventory from the database
     */
    private fun loadPlayerInventory(player: Player) {
        transaction {
            val playerId = player.id ?: return@transaction
            
            // Clear inventory
            for (i in player.inventory.indices) {
                player.inventory[i] = null
            }
            
            // Load inventory items from database
            PlayerInventoryItemEntity.find { 
                PlayerInventory.playerId eq playerId 
            }.forEach { entity ->
                if (entity.slot in player.inventory.indices) {
                    player.inventory[entity.slot] = InventoryItem(
                        itemId = entity.itemId,
                        amount = entity.amount
                    )
                }
            }
        }
    }
    
    /**
     * Hashes a password for secure storage
     */
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
} 