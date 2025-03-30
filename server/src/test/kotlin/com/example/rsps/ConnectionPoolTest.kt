package com.example.rsps

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

class ConnectionPoolTest {
    
    @Test
    fun testConnectionPoolPerformance() {
        // Configure HikariCP
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5434/rsps_dev"
            username = "postgres"
            password = "postgres"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
        
        // Create the data source
        val dataSource = HikariDataSource(hikariConfig)
        
        // Connect Exposed to the data source
        val db = Database.connect(dataSource)
        
        // Time for first connection (should be longer)
        val firstConnectionTime = measureTimeMillis {
            transaction {
                exec("SELECT 1") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            }
        }
        println("First connection time: $firstConnectionTime ms")
        
        // Time for subsequent connections (should be faster due to pooling)
        val subsequentConnectionTimes = (1..10).map {
            measureTimeMillis {
                transaction {
                    exec("SELECT 1") { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            }
        }
        
        val avgSubsequentTime = subsequentConnectionTimes.average()
        println("Average subsequent connection time: $avgSubsequentTime ms")
        
        // The first connection should be significantly longer than subsequent ones
        // if connection pooling is working correctly
        assertTrue(firstConnectionTime > avgSubsequentTime,
            "First connection time ($firstConnectionTime ms) should be longer than average subsequent time ($avgSubsequentTime ms)")
        
        // Close the data source
        dataSource.close()
    }
} 