package de.andrena.codingaider.executors

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.docker.DockerContainerManager
import de.andrena.codingaider.executors.api.AiderProcessInteractor
import de.andrena.codingaider.executors.api.CommandSubject
import de.andrena.codingaider.executors.api.DefaultAiderProcessInteractor
import de.andrena.codingaider.executors.strategies.AiderExecutionStrategy
import de.andrena.codingaider.executors.strategies.DockerAiderExecutionStrategy
import de.andrena.codingaider.executors.strategies.NativeAiderExecutionStrategy
import de.andrena.codingaider.inputdialog.AiderMode
+import de.andrena.codingaider.services.ClipboardEditService
import de.andrena.codingaider.services.FileExtractorService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.services.sidecar.AiderProcessManager
import de.andrena.codingaider.services.sidecar.SidecarProcessInitializer
import de.andrena.codingaider.settings.AiderProjectSettings
 import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
 import de.andrena.codingaider.utils.ApiKeyChecker
 import de.andrena.codingaider.utils.DefaultApiKeyChecker
 import java.io.File
 import java.util.concurrent.TimeUnit
 
+// Instruction prompt for the LLM when pluginBasedEdits is enabled (duplicated from AiderExecutionStrategy for now)
+// TODO: Consider moving this to a shared location if it grows more complex
+private const val DIFF_INSTRUCTION_PROMPT = """When providing code changes, please use the following exact format in a single block:
+<path/to/filename.ext>
+```diff
+<<<<<<< SEARCH
+<content to search for>
+=======
+<replacement content>
+>>>>>>> REPLACE
+```"""
+
 class CommandExecutor(
     private val commandData: CommandData,
     private val project: Project,
     private val apiKeyChecker: ApiKeyChecker = DefaultApiKeyChecker()
 ) :
     CommandSubject by GenericCommandSubject() {
     private val logger = Logger.getInstance(CommandExecutor::class.java)
     private val settings = getInstance()
+    private val clipboardEditService = project.service<ClipboardEditService>() // Inject ClipboardEditService
     private val commandLogger = CommandLogger(project, settings, commandData)
     private var process: Process? = null
     private var isAborted = false
@@ -160,7 +171,12 @@
     }
 
     private fun buildSidecarCommandString(commandData: CommandData): String {
-        return when (commandData.aiderMode) {
+        val originalMessage = when (commandData.aiderMode) {
             AiderMode.NORMAL -> commandData.message
             AiderMode.STRUCTURED -> project.service<AiderPlanService>().createAiderPlanSystemPrompt(commandData)
             AiderMode.ARCHITECT -> "/architect ${commandData.message}"
             else -> ""
+        }
+        return if (settings.pluginBasedEdits) {
+            "/ask ${DIFF_INSTRUCTION_PROMPT}\n\n${originalMessage}"
+        } else {
+            originalMessage
         }
     }
 
@@ -184,7 +200,12 @@
             val exitCode = process.exitValue()
             val status = if (exitCode == 0) "executed successfully" else "failed with exit code $exitCode"
             val finalOutput = commandLogger.prependCommandToOutput("$output\nAider command $status")
-            notifyObservers { it.onCommandComplete(finalOutput, exitCode) }
+
+            if (exitCode == 0 && settings.pluginBasedEdits) {
+                clipboardEditService.processText(output.toString())
+            }
+
+            notifyObservers { it.onCommandComplete(finalOutput, exitCode) } // Notify after potential edits
             return finalOutput
         }
     }
