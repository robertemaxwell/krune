package com.example.rsps

import com.example.rsps.game.GameEngine
import com.example.rsps.game.PlayerRepository
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebSocketTest {
    private var server: NettyApplicationEngine? = null
    private val serverPort = 8081
    private val testDbUrl = "jdbc:postgresql://localhost:5434/rsps_dev"
    
    @Before
    fun setup() {
        // Connect to test database
        Database.connect(
            url = testDbUrl,
            driver = "org.postgresql.Driver",
            user = "postgres",
            password = "postgres"
        )
        
        // Create tables
        transaction {
            SchemaUtils.create(Players, PlayerSkills, PlayerInventory)
        }
        
        // Start test server
        server = embeddedServer(Netty, port = serverPort) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            
            val gameEngine = GameEngine()
            val playerRepository = PlayerRepository()
            
            gameEngine.start()
            
            routing {
                webSocket("/game") {
                    val sessionId = "test-session"
                    
                    // Create and handle WebSocket connection as in Main.kt
                    // (Simplified for testing)
                    
                    val connectionMessage = incoming.receive() as? Frame.Text
                    if (connectionMessage != null) {
                        val text = connectionMessage.readText()
                        
                        if (text.startsWith("REGISTER:")) {
                            val parts = text.substringAfter("REGISTER:").split(":")
                            if (parts.size >= 2) {
                                val username = parts[0]
                                val password = parts[1]
                                
                                val entity = playerRepository.createPlayer(username, password)
                                if (entity != null) {
                                    send(Frame.Text("REGISTER_SUCCESS:${entity.id.value}:$username"))
                                } else {
                                    send(Frame.Text("REGISTER_FAILED:Registration failed"))
                                }
                            }
                        } else if (text.startsWith("LOGIN:")) {
                            val parts = text.substringAfter("LOGIN:").split(":")
                            if (parts.size >= 2) {
                                val username = parts[0]
                                val password = parts[1]
                                
                                val entity = playerRepository.authenticatePlayer(username, password)
                                if (entity != null) {
                                    send(Frame.Text("LOGIN_SUCCESS:${entity.id.value}:$username"))
                                } else {
                                    send(Frame.Text("LOGIN_FAILED:Invalid username or password"))
                                }
                            }
                        }
                    }
                }
            }
        }.start()
    }
    
    @After
    fun tearDown() {
        // Clean up tables
        transaction {
            PlayerInventory.deleteAll()
            PlayerSkills.deleteAll()
            Players.deleteAll()
        }
        
        // Stop server
        server?.stop(0, 0)
    }
    
    @Test
    fun testRegisterAndLogin() = runBlocking {
        // Create WebSocket client
        val client = HttpClient {
            install(WebSockets)
        }
        
        withTimeout(10000) {
            client.webSocket(method = HttpMethod.Get, host = "localhost", port = serverPort, path = "/game") {
                // Test registration
                send(Frame.Text("REGISTER:testuser:testpass"))
                
                // Receive registration response
                val registrationResponse = (incoming.receive() as Frame.Text).readText()
                assertTrue(registrationResponse.startsWith("REGISTER_SUCCESS"))
                println("Registration successful: $registrationResponse")
            }
            
            // Test login with the registered user
            client.webSocket(method = HttpMethod.Get, host = "localhost", port = serverPort, path = "/game") {
                send(Frame.Text("LOGIN:testuser:testpass"))
                
                // Receive login response
                val loginResponse = (incoming.receive() as Frame.Text).readText()
                assertTrue(loginResponse.startsWith("LOGIN_SUCCESS"))
                println("Login successful: $loginResponse")
            }
            
            // Test login with wrong password
            client.webSocket(method = HttpMethod.Get, host = "localhost", port = serverPort, path = "/game") {
                send(Frame.Text("LOGIN:testuser:wrongpass"))
                
                // Receive login response
                val loginResponse = (incoming.receive() as Frame.Text).readText()
                assertEquals("LOGIN_FAILED:Invalid username or password", loginResponse)
                println("Login failed as expected: $loginResponse")
            }
        }
        
        client.close()
    }
} 