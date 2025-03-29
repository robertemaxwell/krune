package com.example.rsps

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

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

// Player appearance table
object PlayerAppearance : IntIdTable() {
    val playerId = reference("player_id", Players).uniqueIndex()
    val gender = integer("gender").default(0)  // 0 for male, 1 for female
    val headModel = integer("head_model").default(0)
    val bodyModel = integer("body_model").default(18)
    val legModel = integer("leg_model").default(26)
    val feetModel = integer("feet_model").default(36)
    val handModel = integer("hand_model").default(33)
    val hairColor = integer("hair_color").default(0)
    val bodyColor = integer("body_color").default(0)
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

// Player equipment table
object PlayerEquipment : IntIdTable() {
    val playerId = reference("player_id", Players)
    val slot = integer("slot")
    val itemId = integer("item_id")
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

// Player appearance entity class
class PlayerAppearanceEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerAppearanceEntity>(PlayerAppearance)
    
    var player by PlayerEntity referencedOn PlayerAppearance.playerId
    var gender by PlayerAppearance.gender
    var headModel by PlayerAppearance.headModel
    var bodyModel by PlayerAppearance.bodyModel
    var legModel by PlayerAppearance.legModel
    var feetModel by PlayerAppearance.feetModel
    var handModel by PlayerAppearance.handModel
    var hairColor by PlayerAppearance.hairColor
    var bodyColor by PlayerAppearance.bodyColor
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

// Player equipment item entity class
class PlayerEquipmentItemEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEquipmentItemEntity>(PlayerEquipment)
    
    var player by PlayerEntity referencedOn PlayerEquipment.playerId
    var slot by PlayerEquipment.slot
    var itemId by PlayerEquipment.itemId
}

fun initDatabase() {
    // Read database configuration from environment variables or use defaults
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5434/rsps_dev"
    val dbUser = System.getenv("DB_USER") ?: "postgres"
    val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
    
    val db = Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )
    
    // Create tables if they don't exist
    transaction {
        SchemaUtils.create(
            Players,
            PlayerAppearance,
            PlayerSkills,
            PlayerInventory,
            PlayerEquipment
        )
    }
    
    println("Database initialized successfully")
} 