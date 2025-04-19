package de.andrena.codingaider.executors.strategies

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderOutputSummaryService
import de.andrena.codingaider.services.plans.AiderPlanService
import de.andrena.codingaider.settings.AiderSettings
import de.andrena.codingaider.settings.CustomLlmProviderService

// Instruction prompt for the LLM when pluginBasedEdits is enabled
private const val DIFF_INSTRUCTION_PROMPT = """When providing code changes, please use the following exact format in a single block:
<path/to/filename.ext>
