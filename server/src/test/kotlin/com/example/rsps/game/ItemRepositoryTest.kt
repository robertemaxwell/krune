package com.example.rsps.game

import com.example.rsps.Items
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

class ItemRepositoryTest {
    
    private lateinit var itemRepository: ItemRepository
    
    @BeforeEach
    fun setup() {
        // Initialize the database connection
        initDatabase()
        
        // Clear the items table
        transaction {
            Items.deleteAll()
        }
        
        // Create repository
        itemRepository = ItemRepository()
    }
    
    @AfterEach
    fun cleanup() {
        // Clean up after test
        transaction {
            Items.deleteAll()
        }
    }
    
    @Test
    fun testCreateItem() {
        // Create a test item
        val item = itemRepository.createItem(
            name = "Test Sword",
            description = "A test sword for unit testing",
            equipable = true,
            stackable = false,
            value = 100,
            highAlchValue = 60,
            lowAlchValue = 40,
            weight = 1.5
        )
        
        // Verify item was created
        assertNotNull(item, "Item should be created successfully")
        assertEquals("Test Sword", item.name)
        assertEquals("A test sword for unit testing", item.description)
        assertEquals(true, item.equipable)
        assertEquals(false, item.stackable)
        assertEquals(100, item.value)
        assertEquals(60, item.highAlchValue)
        assertEquals(40, item.lowAlchValue)
        assertEquals(1.5, item.weight)
    }
    
    @Test
    fun testFindItemById() {
        // Create a test item
        val created = itemRepository.createItem(name = "Test Item", value = 50)
        assertNotNull(created)
        
        // Find the item by ID
        val found = itemRepository.findItemById(created.id.value)
        
        // Verify item was found
        assertNotNull(found, "Item should be found by ID")
        assertEquals("Test Item", found.name)
        assertEquals(50, found.value)
    }
    
    @Test
    fun testFindItemsByName() {
        // Create test items
        itemRepository.createItem(name = "Iron Sword")
        itemRepository.createItem(name = "Steel Sword")
        itemRepository.createItem(name = "Iron Dagger")
        
        // Find items by partial name
        val foundItems = itemRepository.findItemsByName("Iron")
        
        // Verify correct items were found
        assertEquals(2, foundItems.size, "Should find 2 items with 'Iron' in the name")
        assertTrue(foundItems.any { it.name == "Iron Sword" })
        assertTrue(foundItems.any { it.name == "Iron Dagger" })
    }
    
    @Test
    fun testUpdateItem() {
        // Create a test item
        val created = itemRepository.createItem(
            name = "Old Name",
            description = "Old description",
            value = 10
        )
        assertNotNull(created)
        
        // Update the item
        val updateSuccess = itemRepository.updateItem(
            id = created.id.value,
            name = "New Name",
            description = "New description",
            value = 20
        )
        
        // Verify update was successful
        assertTrue(updateSuccess, "Update should be successful")
        
        // Retrieve the updated item
        val updated = itemRepository.findItemById(created.id.value)
        assertNotNull(updated)
        assertEquals("New Name", updated.name)
        assertEquals("New description", updated.description)
        assertEquals(20, updated.value)
    }
    
    @Test
    fun testDeleteItem() {
        // Create a test item
        val created = itemRepository.createItem(name = "Item to delete")
        assertNotNull(created)
        
        // Delete the item
        val deleteSuccess = itemRepository.deleteItem(created.id.value)
        
        // Verify deletion was successful
        assertTrue(deleteSuccess, "Deletion should be successful")
        
        // Try to find the deleted item
        val found = itemRepository.findItemById(created.id.value)
        assertNull(found, "Item should be deleted")
    }
    
    @Test
    fun testGetAllItems() {
        // Create test items
        itemRepository.createItem(name = "Item 1")
        itemRepository.createItem(name = "Item 2")
        itemRepository.createItem(name = "Item 3")
        
        // Get all items
        val allItems = itemRepository.getAllItems()
        
        // Verify all items were retrieved
        assertEquals(3, allItems.size, "Should find all 3 items")
        assertTrue(allItems.any { it.name == "Item 1" })
        assertTrue(allItems.any { it.name == "Item 2" })
        assertTrue(allItems.any { it.name == "Item 3" })
    }
} 