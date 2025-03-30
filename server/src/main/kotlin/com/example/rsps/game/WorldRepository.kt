package com.example.rsps.game

import com.example.rsps.WorldObjectEntity
import com.example.rsps.WorldObjects
import com.example.rsps.WorldRegionEntity
import com.example.rsps.WorldRegions
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Repository for world state database operations
 */
class WorldRepository {
    private val logger = LoggerFactory.getLogger(WorldRepository::class.java)
    
    //
    // Region methods
    //
    
    /**
     * Creates a new world region
     */
    fun createRegion(
        x: Int,
        y: Int,
        z: Int = 0,
        name: String? = null,
        isWilderness: Boolean = false,
        isPvp: Boolean = false
    ): WorldRegionEntity? {
        return try {
            transaction {
                WorldRegionEntity.new {
                    this.x = x
                    this.y = y
                    this.z = z
                    this.name = name
                    this.isWilderness = isWilderness
                    this.isPvp = isPvp
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create region: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds a region by ID
     */
    fun findRegionById(id: Int): WorldRegionEntity? {
        return try {
            transaction {
                WorldRegionEntity.findById(id)
            }
        } catch (e: Exception) {
            logger.error("Failed to find region by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds a region by coordinates
     */
    fun findRegionByCoordinates(x: Int, y: Int, z: Int = 0): WorldRegionEntity? {
        return try {
            transaction {
                WorldRegionEntity.find { 
                    (WorldRegions.x eq x) and (WorldRegions.y eq y) and (WorldRegions.z eq z)
                }.firstOrNull()
            }
        } catch (e: Exception) {
            logger.error("Failed to find region by coordinates: ${e.message}", e)
            null
        }
    }
    
    /**
     * Gets all regions
     */
    fun getAllRegions(): List<WorldRegionEntity> {
        return try {
            transaction {
                WorldRegionEntity.all().toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to get all regions: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Updates a region
     */
    fun updateRegion(
        id: Int,
        name: String? = null,
        isWilderness: Boolean? = null,
        isPvp: Boolean? = null
    ): Boolean {
        return try {
            transaction {
                val region = WorldRegionEntity.findById(id) ?: return@transaction false
                
                name?.let { region.name = it }
                isWilderness?.let { region.isWilderness = it }
                isPvp?.let { region.isPvp = it }
                
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to update region: ${e.message}", e)
            false
        }
    }
    
    /**
     * Deletes a region
     */
    fun deleteRegion(id: Int): Boolean {
        return try {
            transaction {
                val region = WorldRegionEntity.findById(id) ?: return@transaction false
                region.delete()
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to delete region: ${e.message}", e)
            false
        }
    }
    
    //
    // World Object methods
    //
    
    /**
     * Creates a new world object
     */
    fun createWorldObject(
        objectId: Int,
        x: Int,
        y: Int,
        z: Int = 0,
        rotation: Int = 0,
        regionId: Int? = null,
        respawnTime: Int = 0
    ): WorldObjectEntity? {
        return try {
            transaction {
                WorldObjectEntity.new {
                    this.objectId = objectId
                    this.x = x
                    this.y = y
                    this.z = z
                    this.rotation = rotation
                    this.region = regionId?.let { WorldRegionEntity.findById(it) }
                    this.respawnTime = respawnTime
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create world object: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds a world object by ID
     */
    fun findWorldObjectById(id: Int): WorldObjectEntity? {
        return try {
            transaction {
                WorldObjectEntity.findById(id)
            }
        } catch (e: Exception) {
            logger.error("Failed to find world object by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds world objects by coordinates
     */
    fun findWorldObjectsByCoordinates(x: Int, y: Int, z: Int = 0): List<WorldObjectEntity> {
        return try {
            transaction {
                WorldObjectEntity.find { 
                    (WorldObjects.x eq x) and (WorldObjects.y eq y) and (WorldObjects.z eq z)
                }.toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to find world objects by coordinates: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Finds world objects by region
     */
    fun findWorldObjectsByRegion(regionId: Int): List<WorldObjectEntity> {
        return try {
            transaction {
                WorldObjectEntity.find { 
                    WorldObjects.regionId eq regionId
                }.toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to find world objects by region: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Updates a world object
     */
    fun updateWorldObject(
        id: Int,
        rotation: Int? = null,
        respawnTime: Int? = null
    ): Boolean {
        return try {
            transaction {
                val worldObject = WorldObjectEntity.findById(id) ?: return@transaction false
                
                rotation?.let { worldObject.rotation = it }
                respawnTime?.let { worldObject.respawnTime = it }
                
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to update world object: ${e.message}", e)
            false
        }
    }
    
    /**
     * Deletes a world object
     */
    fun deleteWorldObject(id: Int): Boolean {
        return try {
            transaction {
                val worldObject = WorldObjectEntity.findById(id) ?: return@transaction false
                worldObject.delete()
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to delete world object: ${e.message}", e)
            false
        }
    }
    
    /**
     * Gets all world objects
     */
    fun getAllWorldObjects(): List<WorldObjectEntity> {
        return try {
            transaction {
                WorldObjectEntity.all().toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to get all world objects: ${e.message}", e)
            emptyList()
        }
    }
} 