package com.example.rsps.game

import com.example.rsps.WorldObjects
import com.example.rsps.WorldRegions
import com.example.rsps.initDatabase
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorldRepositoryTest {
    
    private lateinit var worldRepository: WorldRepository
    
    @BeforeEach
    fun setup() {
        // Initialize the database connection
        initDatabase()
        
        // Clear the world tables
        transaction {
            WorldObjects.deleteAll()
            WorldRegions.deleteAll()
        }
        
        // Create repository
        worldRepository = WorldRepository()
    }
    
    @AfterEach
    fun cleanup() {
        // Clean up after test
        transaction {
            WorldObjects.deleteAll()
            WorldRegions.deleteAll()
        }
    }
    
    @Test
    fun testCreateRegion() {
        // Create a test region
        val region = worldRepository.createRegion(
            x = 3000,
            y = 3000,
            z = 0,
            name = "Test Region",
            isWilderness = true,
            isPvp = true
        )
        
        // Verify region was created
        assertNotNull(region, "Region should be created successfully")
        assertEquals(3000, region.x)
        assertEquals(3000, region.y)
        assertEquals(0, region.z)
        assertEquals("Test Region", region.name)
        assertEquals(true, region.isWilderness)
        assertEquals(true, region.isPvp)
    }
    
    @Test
    fun testFindRegionByCoordinates() {
        // Create a test region
        val created = worldRepository.createRegion(x = 3100, y = 3200, name = "Coordinate Test")
        assertNotNull(created)
        
        // Find the region by coordinates
        val found = worldRepository.findRegionByCoordinates(x = 3100, y = 3200)
        
        // Verify region was found
        assertNotNull(found, "Region should be found by coordinates")
        assertEquals("Coordinate Test", found.name)
    }
    
    @Test
    fun testUpdateRegion() {
        // Create a test region
        val created = worldRepository.createRegion(
            x = 3200,
            y = 3200,
            name = "Old Region Name",
            isWilderness = false
        )
        assertNotNull(created)
        
        // Update the region
        val updateSuccess = worldRepository.updateRegion(
            id = created.id.value,
            name = "New Region Name",
            isWilderness = true
        )
        
        // Verify update was successful
        assertTrue(updateSuccess, "Update should be successful")
        
        // Retrieve the updated region
        val updated = worldRepository.findRegionById(created.id.value)
        assertNotNull(updated)
        assertEquals("New Region Name", updated.name)
        assertEquals(true, updated.isWilderness)
    }
    
    @Test
    fun testCreateWorldObject() {
        // Create a test region first
        val region = worldRepository.createRegion(x = 3300, y = 3300, name = "Object Test Region")
        assertNotNull(region)
        
        // Create a test world object
        val worldObject = worldRepository.createWorldObject(
            objectId = 1234,
            x = 3305,
            y = 3305,
            z = 0,
            rotation = 1,
            regionId = region.id.value,
            respawnTime = 50
        )
        
        // Verify world object was created
        assertNotNull(worldObject, "World object should be created successfully")
        assertEquals(1234, worldObject.objectId)
        assertEquals(3305, worldObject.x)
        assertEquals(3305, worldObject.y)
        assertEquals(0, worldObject.z)
        assertEquals(1, worldObject.rotation)
        assertEquals(50, worldObject.respawnTime)
        assertNotNull(worldObject.region)
        assertEquals(region.id, worldObject.region?.id)
    }
    
    @Test
    fun testFindWorldObjectsByCoordinates() {
        // Create some test world objects
        worldRepository.createWorldObject(objectId = 1001, x = 3400, y = 3400)
        worldRepository.createWorldObject(objectId = 1002, x = 3400, y = 3400)
        worldRepository.createWorldObject(objectId = 1003, x = 3500, y = 3500)
        
        // Find objects by coordinates
        val foundObjects = worldRepository.findWorldObjectsByCoordinates(x = 3400, y = 3400)
        
        // Verify correct objects were found
        assertEquals(2, foundObjects.size, "Should find 2 objects at the coordinates")
        assertTrue(foundObjects.any { it.objectId == 1001 })
        assertTrue(foundObjects.any { it.objectId == 1002 })
    }
    
    @Test
    fun testFindWorldObjectsByRegion() {
        // Create a test region
        val region = worldRepository.createRegion(x = 3600, y = 3600, name = "Region for Objects")
        assertNotNull(region)
        
        // Create test world objects in that region
        worldRepository.createWorldObject(objectId = 2001, x = 3605, y = 3605, regionId = region.id.value)
        worldRepository.createWorldObject(objectId = 2002, x = 3610, y = 3610, regionId = region.id.value)
        
        // Create an object in a different location
        worldRepository.createWorldObject(objectId = 2003, x = 3700, y = 3700)
        
        // Find objects by region
        val foundObjects = worldRepository.findWorldObjectsByRegion(region.id.value)
        
        // Verify correct objects were found
        assertEquals(2, foundObjects.size, "Should find 2 objects in the region")
        assertTrue(foundObjects.any { it.objectId == 2001 })
        assertTrue(foundObjects.any { it.objectId == 2002 })
    }
    
    @Test
    fun testUpdateWorldObject() {
        // Create a test world object
        val created = worldRepository.createWorldObject(
            objectId = 3001,
            x = 3800,
            y = 3800,
            rotation = 0,
            respawnTime = 0
        )
        assertNotNull(created)
        
        // Update the world object
        val updateSuccess = worldRepository.updateWorldObject(
            id = created.id.value,
            rotation = 2,
            respawnTime = 100
        )
        
        // Verify update was successful
        assertTrue(updateSuccess, "Update should be successful")
        
        // Retrieve the updated world object
        val updated = worldRepository.findWorldObjectById(created.id.value)
        assertNotNull(updated)
        assertEquals(2, updated.rotation)
        assertEquals(100, updated.respawnTime)
    }
    
    @Test
    fun testDeleteWorldObject() {
        // Create a test world object
        val created = worldRepository.createWorldObject(objectId = 4001, x = 3900, y = 3900)
        assertNotNull(created)
        
        // Delete the world object
        val deleteSuccess = worldRepository.deleteWorldObject(created.id.value)
        
        // Verify deletion was successful
        assertTrue(deleteSuccess, "Deletion should be successful")
        
        // Try to find the deleted world object
        val found = worldRepository.findWorldObjectById(created.id.value)
        assertNull(found, "World object should be deleted")
    }
} 