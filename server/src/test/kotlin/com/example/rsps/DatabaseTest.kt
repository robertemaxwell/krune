package com.example.rsps

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DatabaseTest {
    
    @Before
    fun setup() {
        // Connect to test database
        val dbUrl = "jdbc:postgresql://localhost:5434/rsps_dev"
        val dbUser = "postgres"
        val dbPassword = "postgres"
        
        Database.connect(
            url = dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )
        
        // Create tables
        transaction {
            SchemaUtils.create(Players, PlayerSkills, PlayerInventory)
        }
    }
    
    @After
    fun tearDown() {
        // Clean up tables
        transaction {
            PlayerInventory.deleteAll()
            PlayerSkills.deleteAll()
            Players.deleteAll()
        }
    }
    
    @Test
    fun testCreatePlayer() {
        transaction {
            // Create a new player
            val entity = PlayerEntity.new {
                username = "testplayer"
                passwordHash = "testhash"
                x = 3000
                y = 3000
                createdAt = System.currentTimeMillis()
            }
            
            // Verify player was created
            val found = PlayerEntity.findById(entity.id)
            assertNotNull(found)
            assertEquals("testplayer", found.username)
            assertEquals(3000, found.x)
            assertEquals(3000, found.y)
        }
    }
    
    @Test
    fun testPlayerWithSkills() {
        transaction {
            // Create a player
            val player = PlayerEntity.new {
                username = "skillplayer"
                passwordHash = "skillhash"
                createdAt = System.currentTimeMillis()
            }
            
            // Add some skills
            val woodcutting = PlayerSkillEntity.new {
                this.player = player
                this.skillId = 1 // Assuming 1 = woodcutting
                this.level = 5
                this.experience = 125.0
            }
            
            val mining = PlayerSkillEntity.new {
                this.player = player
                this.skillId = 2 // Assuming 2 = mining
                this.level = 3
                this.experience = 75.0
            }
            
            // Verify skills were created
            val skills = PlayerSkillEntity.find { PlayerSkills.playerId eq player.id }
            assertEquals(2, skills.count())
            
            val foundWoodcutting = skills.first { it.skillId == 1 }
            assertEquals(5, foundWoodcutting.level)
            assertEquals(125.0, foundWoodcutting.experience)
        }
    }
    
    @Test
    fun testPlayerInventory() {
        transaction {
            // Create a player
            val player = PlayerEntity.new {
                username = "invplayer"
                passwordHash = "invhash"
                createdAt = System.currentTimeMillis()
            }
            
            // Add inventory items
            val sword = PlayerInventoryItemEntity.new {
                this.player = player
                this.slot = 0
                this.itemId = 1291 // Bronze sword
                this.amount = 1
            }
            
            val coins = PlayerInventoryItemEntity.new {
                this.player = player
                this.slot = 1
                this.itemId = 995 // Coins
                this.amount = 1000
            }
            
            // Verify items were added
            val items = PlayerInventoryItemEntity.find { PlayerInventory.playerId eq player.id }
            assertEquals(2, items.count())
            
            val foundCoins = items.first { it.itemId == 995 }
            assertEquals(1, foundCoins.slot)
            assertEquals(1000, foundCoins.amount)
        }
    }
} 