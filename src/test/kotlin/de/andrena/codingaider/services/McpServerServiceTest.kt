package de.andrena.codingaider.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.andrena.codingaider.command.FileData
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class McpServerServiceTest : BasePlatformTestCase() {
    
    private lateinit var mcpServerService: McpServerService
    
    override fun setUp() {
        super.setUp()
        mcpServerService = McpServerService(project)
    }
    
    override fun tearDown() {
        mcpServerService.dispose()
        super.tearDown()
    }
    
    @Test
    fun testServerStartsAndStops() = runBlocking {
        assertFalse("Server should not be running initially", mcpServerService.isServerRunning())
        
        mcpServerService.startServer()
        delay(1000) // Give server time to start
        
        assertTrue("Server should be running after start", mcpServerService.isServerRunning())
        assertTrue("Server port should be valid", mcpServerService.getServerPort() > 0)
        
        mcpServerService.stopServer()
        delay(500) // Give server time to stop
        
        assertFalse("Server should not be running after stop", mcpServerService.isServerRunning())
    }
    
    @Test
    fun testHealthEndpoint() = runBlocking {
        mcpServerService.startServer()
        delay(1000) // Give server time to start
        
        val port = mcpServerService.getServerPort()
        val url = URL("http://localhost:$port/health")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            assertEquals("Health endpoint should return 200", 200, responseCode)
            
            val response = connection.inputStream.bufferedReader().readText()
            assertTrue("Health response should contain port info", response.contains(port.toString()))
        } finally {
            connection.disconnect()
        }
    }
    
    @Test
    fun testStatusEndpoint() = runBlocking {
        mcpServerService.startServer()
        delay(1000) // Give server time to start
        
        val port = mcpServerService.getServerPort()
        val url = URL("http://localhost:$port/status")
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.requestMethod = "GET"
            val responseCode = connection.responseCode
            assertEquals("Status endpoint should return 200", 200, responseCode)
            
            val response = connection.inputStream.bufferedReader().readText()
            assertTrue("Status response should contain running status", response.contains("\"running\":true"))
            assertTrue("Status response should contain port", response.contains("\"port\":$port"))
        } finally {
            connection.disconnect()
        }
    }
}
