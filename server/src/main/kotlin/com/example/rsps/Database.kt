package com.example.rsps

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

object Players : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val x = integer("x").default(3222)  // default starting position
    val y = integer("y").default(3222)
    val z = integer("z").default(0)

    override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
    val db = Database.connect(
        url = "jdbc:postgresql://localhost:5432/rsps_dev",
        driver = "org.postgresql.Driver",
        user = "robertmaxwell",  // Assuming current user as DB username
        password = ""            // No password for local development
    )
    
    // Create tables if they don't exist
    transaction {
        SchemaUtils.create(Players)
    }
    
    println("Database initialized")
} 