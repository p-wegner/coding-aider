package de.andrena.codingaider.services.mcp.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.mcp.McpTool
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.services.mcp.McpToolArgumentException
import de.andrena.codingaider.services.mcp.McpToolExecutionException
import de.andrena.codingaider.settings.AiderSettings
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

/**
 * MCP tool for creating structured development plans using Aider's planning system
 */
class CreateAiderPlanTool(private val project: Project) : McpTool {
    
    private val settings by lazy { AiderSettings.getInstance() }
    
    override fun getName(): String = "create_aider_plan"
    
    override fun getDescription(): String = 
        "Create a structured development plan using Coding Aider's planning system. " +
        "Automatically generates a plan with checklist and enables step-by-step execution."
    
    override fun getInputSchema(): Tool.Input = Tool.Input(
        properties = buildJsonObject {
            putJsonObject("message") {
                put("type", "string")
                put("description", "The feature or task description to create a plan for")
            }
            putJsonObject("files") {
                put("type", "array")
                put("default", JsonArray(emptyList()))
                put("description", "Files to include in the planning context and execution steps. For reliable output," +
                        " ensure all files are relevant for the task or serve as a good example of used conventions libraries or patterns.")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("filePath") {
                            put("type", "string")
                        }
                        putJsonObject("isReadOnly") {
                            put("type", "boolean")
                            put("default", false)
                        }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("filePath"))
                    }
                }
            }
        },
        required = listOf("message")
    )
    
    override suspend fun execute(arguments: JsonObject): CallToolResult {
        return try {
            val message = arguments["message"]?.jsonPrimitive?.content
                ?: throw McpToolArgumentException(getName(), "message parameter is required")
            
            val filesArray = arguments["files"]?.jsonArray ?: JsonArray(emptyList())
            val fileDataList = filesArray.map { fileElement ->
                val fileObj = fileElement.jsonObject
                val filePath = fileObj["filePath"]?.jsonPrimitive?.content
                    ?: throw McpToolArgumentException(getName(), "filePath is required for each file")
                val isReadOnly = fileObj["isReadOnly"]?.jsonPrimitive?.booleanOrNull ?: false
                FileData(filePath, isReadOnly)
            }
            
            // Create CommandData with structured mode using settings defaults
            val commandData = CommandData(
                message = message,
                useYesFlag = true, // For automation
                llm = settings.llm,
                additionalArgs = settings.additionalArgs,
                files = fileDataList,
                lintCmd = settings.lintCmd,
                deactivateRepoMap = settings.deactivateRepoMap,
                editFormat = settings.editFormat,
                projectPath = project.basePath ?: "",
                aiderMode = AiderMode.STRUCTURED, // This triggers plan creation
                options = CommandOptions(
                    autoCommit = when (settings.autoCommits) {
                        AiderSettings.AutoCommitSetting.ON -> true
                        AiderSettings.AutoCommitSetting.OFF -> false
                        AiderSettings.AutoCommitSetting.DEFAULT -> null
                    },
                    dirtyCommits = when (settings.dirtyCommits) {
                        AiderSettings.DirtyCommitSetting.ON -> true
                        AiderSettings.DirtyCommitSetting.OFF -> false
                        AiderSettings.DirtyCommitSetting.DEFAULT -> null
                    },
                    promptAugmentation = settings.promptAugmentation
                )
            )
            
            // Execute using existing infrastructure
            val executor = IDEBasedExecutor(project, commandData)
            val output = executor.execute()
            
            CallToolResult(
                content = listOf(
                    TextContent("Plan creation started successfully"),
                    TextContent("Message: $message"),
                    TextContent("Files: ${fileDataList.size} file(s) included"),
                    TextContent("Mode: STRUCTURED (plan will be created in .coding-aider-plans directory)"),
                    TextContent("LLM: ${settings.llm}"),
                    TextContent("Execution initiated with output: $output")
                )
            )
        } catch (e: McpToolArgumentException) {
            throw e
        } catch (e: Exception) {
            throw McpToolExecutionException(getName(), e.message ?: "Unknown error", e)
        }
    }
}