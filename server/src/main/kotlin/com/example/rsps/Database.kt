package com.example.rsps

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory

// Player table definition
object Players : IntIdTable() {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 128)
    val x = integer("x").default(3222)  // default starting position
    val y = integer("y").default(3222)
    val z = integer("z").default(0)
    val health = integer("health").default(100)
    val createdAt = long("created_at").default(System.currentTimeMillis())
    val lastLogin = long("last_login").nullable()
}

// Player skills table
object PlayerSkills : IntIdTable() {
    val playerId = reference("player_id", Players)
    val skillId = integer("skill_id")
    val level = integer("level").default(1)
    val experience = double("experience").default(0.0)
}

// Player inventory table
object PlayerInventory : IntIdTable() {
    val playerId = reference("player_id", Players)
    val slot = integer("slot")
    val itemId = integer("item_id")
    val amount = integer("amount")
}

// Items definition table
object Items : IntIdTable() {
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val equipable = bool("equipable").default(false)
    val stackable = bool("stackable").default(false)
    val value = integer("value").default(0)
    val highAlchValue = integer("high_alch_value").nullable()
    val lowAlchValue = integer("low_alch_value").nullable()
    val weight = double("weight").default(0.0)
}

// World regions table
object WorldRegions : IntIdTable() {
    val x = integer("x")
    val y = integer("y")
    val z = integer("z").default(0)
    val name = varchar("name", 100).nullable()
    val isWilderness = bool("is_wilderness").default(false)
    val isPvp = bool("is_pvp").default(false)
}

// World objects table (trees, rocks, etc.)
object WorldObjects : IntIdTable() {
    val objectId = integer("object_id")
    val x = integer("x")
    val y = integer("y")
    val z = integer("z").default(0)
    val rotation = integer("rotation").default(0)
    val regionId = reference("region_id", WorldRegions).nullable()
    val respawnTime = integer("respawn_time").default(0) // In ticks
}

// Player entity class
class PlayerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(Players)
    
    var username by Players.username
    var passwordHash by Players.passwordHash
    var x by Players.x
    var y by Players.y
    var z by Players.z
    var health by Players.health
    var createdAt by Players.createdAt
    var lastLogin by Players.lastLogin
}

// Player skill entity class
class PlayerSkillEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerSkillEntity>(PlayerSkills)
    
    var player by PlayerEntity referencedOn PlayerSkills.playerId
    var skillId by PlayerSkills.skillId
    var level by PlayerSkills.level
    var experience by PlayerSkills.experience
}

// Player inventory item entity class
class PlayerInventoryItemEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerInventoryItemEntity>(PlayerInventory)
    
    var player by PlayerEntity referencedOn PlayerInventory.playerId
    var slot by PlayerInventory.slot
    var itemId by PlayerInventory.itemId
    var amount by PlayerInventory.amount
}

// Item definition entity class
class ItemEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ItemEntity>(Items)
    
    var name by Items.name
    var description by Items.description
    var equipable by Items.equipable
    var stackable by Items.stackable
    var value by Items.value
    var highAlchValue by Items.highAlchValue
    var lowAlchValue by Items.lowAlchValue
    var weight by Items.weight
}

// World region entity class
class WorldRegionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorldRegionEntity>(WorldRegions)
    
    var x by WorldRegions.x
    var y by WorldRegions.y
    var z by WorldRegions.z
    var name by WorldRegions.name
    var isWilderness by WorldRegions.isWilderness
    var isPvp by WorldRegions.isPvp
}

// World object entity class
class WorldObjectEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorldObjectEntity>(WorldObjects)
    
    var objectId by WorldObjects.objectId
    var x by WorldObjects.x
    var y by WorldObjects.y
    var z by WorldObjects.z
    var rotation by WorldObjects.rotation
    var region by WorldRegionEntity optionalReferencedOn WorldObjects.regionId
    var respawnTime by WorldObjects.respawnTime
}

fun initDatabase() {
    val logger = LoggerFactory.getLogger("Database")
    
    // Read database configuration from environment variables or use defaults
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5434/rsps_dev"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
    
    // Configure HikariCP
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = dbUser
        password = dbPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }
    
    // Create the data source
    val dataSource = HikariDataSource(hikariConfig)
    
    // Run flyway migrations
    try {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .cleanDisabled(false)
            .load()
        
        // Clean the database and then migrate
        flyway.clean()
        flyway.migrate()
        logger.info("Database migration completed successfully")
    } catch (e: Exception) {
        logger.error("Database migration failed: ${e.message}", e)
        throw e
    }
    
    // Connect Exposed to the data source
    val db = Database.connect(dataSource)
    
    // Create tables if they don't exist
    transaction {
        SchemaUtils.create(
            Players,
            PlayerSkills,
            PlayerInventory,
            Items,
            WorldRegions,
            WorldObjects
        )
    }
    
    logger.info("Database initialized successfully")
} 