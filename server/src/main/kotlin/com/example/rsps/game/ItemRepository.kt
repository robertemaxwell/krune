package com.example.rsps.game

import com.example.rsps.ItemEntity
import com.example.rsps.Items
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Repository for item definitions database operations
 */
class ItemRepository {
    private val logger = LoggerFactory.getLogger(ItemRepository::class.java)
    
    /**
     * Creates a new item definition in the database
     */
    fun createItem(
        name: String,
        description: String? = null,
        equipable: Boolean = false,
        stackable: Boolean = false,
        value: Int = 0,
        highAlchValue: Int? = null,
        lowAlchValue: Int? = null,
        weight: Double = 0.0
    ): ItemEntity? {
        return try {
            transaction {
                ItemEntity.new {
                    this.name = name
                    this.description = description
                    this.equipable = equipable
                    this.stackable = stackable
                    this.value = value
                    this.highAlchValue = highAlchValue
                    this.lowAlchValue = lowAlchValue
                    this.weight = weight
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to create item: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds an item by ID
     */
    fun findItemById(id: Int): ItemEntity? {
        return try {
            transaction {
                ItemEntity.findById(id)
            }
        } catch (e: Exception) {
            logger.error("Failed to find item by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Finds items by name (case insensitive)
     */
    fun findItemsByName(name: String): List<ItemEntity> {
        return try {
            transaction {
                ItemEntity.find { 
                    Items.name.like("%${name.lowercase()}%")
                    // Using ILIKE would be better but it's PostgreSQL specific
                    // For now we'll use LIKE with lowercase conversion
                }.toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to find items by name: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Updates an existing item
     */
    fun updateItem(
        id: Int,
        name: String? = null,
        description: String? = null,
        equipable: Boolean? = null,
        stackable: Boolean? = null,
        value: Int? = null,
        highAlchValue: Int? = null,
        lowAlchValue: Int? = null,
        weight: Double? = null
    ): Boolean {
        return try {
            transaction {
                val item = ItemEntity.findById(id) ?: return@transaction false
                
                name?.let { item.name = it }
                description?.let { item.description = it }
                equipable?.let { item.equipable = it }
                stackable?.let { item.stackable = it }
                value?.let { item.value = it }
                highAlchValue?.let { item.highAlchValue = it }
                lowAlchValue?.let { item.lowAlchValue = it }
                weight?.let { item.weight = it }
                
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to update item: ${e.message}", e)
            false
        }
    }
    
    /**
     * Deletes an item by ID
     */
    fun deleteItem(id: Int): Boolean {
        return try {
            transaction {
                val item = ItemEntity.findById(id) ?: return@transaction false
                item.delete()
                true
            }
        } catch (e: Exception) {
            logger.error("Failed to delete item: ${e.message}", e)
            false
        }
    }
    
    /**
     * Gets all items
     */
    fun getAllItems(): List<ItemEntity> {
        return try {
            transaction {
                ItemEntity.all().toList()
            }
        } catch (e: Exception) {
            logger.error("Failed to get all items: ${e.message}", e)
            emptyList()
        }
    }
} 