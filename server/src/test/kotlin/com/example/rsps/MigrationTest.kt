package com.example.rsps

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationTest {
    
    @Test
    fun testFlywayMigrations() {
        // Configure HikariCP
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5434/rsps_dev"
            username = "postgres"
            password = "postgres"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        
        // Create the data source
        val dataSource = HikariDataSource(hikariConfig)
        
        // Run flyway migrations
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
        
        val migrationResult = flyway.migrate()
        println("Applied ${migrationResult.migrationsExecuted} migrations")
        
        // Connect Exposed to the data source
        val db = Database.connect(dataSource)
        
        // Check if tables exist
        val tables = transaction {
            exec("""
                SELECT table_name 
                FROM information_schema.tables 
                WHERE table_schema = 'public'
            """) { rs ->
                generateSequence {
                    if (rs.next()) rs.getString(1) else null
                }.toList()
            } ?: emptyList()
        }
        
        println("Tables found: ${tables.joinToString()}")
        
        // Check specific tables
        assertTrue(tables.contains("players"), "Players table not found")
        assertTrue(tables.contains("items"), "Items table not found")
        assertTrue(tables.contains("world_regions"), "World regions table not found")
        
        // Check if sample data was inserted
        val itemCount = transaction {
            exec("SELECT COUNT(*) FROM items") { rs ->
                rs.next()
                rs.getInt(1)
            } ?: 0
        }
        
        println("Found $itemCount items in the database")
        assertEquals(5, itemCount, "Expected 5 sample items in the database")
        
        dataSource.close()
    }
} 