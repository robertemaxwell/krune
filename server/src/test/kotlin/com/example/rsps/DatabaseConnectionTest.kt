package com.example.rsps

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DatabaseConnectionTest {
    
    @Test
    fun testDatabaseConnection() {
        // Connect to database
        val db = Database.connect(
            url = "jdbc:postgresql://localhost:5434/rsps_dev",
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "postgres"
        )
        
        // Simple query to check connection
        val isConnected = transaction {
            exec("SELECT 1") { rs ->
                rs.next()
                rs.getInt(1) == 1
            } ?: false
        }
        
        assertTrue(isConnected, "Database connection failed")
        println("Successfully connected to the database")
    }
} 