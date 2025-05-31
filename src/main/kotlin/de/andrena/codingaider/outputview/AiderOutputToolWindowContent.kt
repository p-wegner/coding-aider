package de.andrena.codingaider.outputview

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.services.CommandSummaryService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities

class AiderOutputToolWindowContent(
    private val project: Project,
    private val contentManager: ContentManager
) {
    private val tabs = ConcurrentHashMap<String, AiderOutputTab>()
    private val tabCounter = AtomicInteger(0)
    private val maxTabs = 10 // Maximum number of tabs to keep open

    fun createTab(
        initialTitle: String,
        initialText: String,
        onAbort: Abortable?,
        commandData: CommandData? = null
    ): AiderOutputTab {
        val displayString = commandData?.let {
            project.getService(CommandSummaryService::class.java).generateSummary(it)
        }
        val tab = AiderOutputTab(
            project,
            initialTitle,
            initialText,
            onAbort,
            displayString,
            commandData
        )
        SwingUtilities.invokeAndWait {


            val tabId = generateTabId(commandData)
            tabs[tabId] = tab

            // Create content for the tab
            val content = ContentFactory.getInstance().createContent(
                tab.component,
                generateTabTitle(commandData),
                false
            ).apply {
                isCloseable = true
                putUserData(TAB_ID_KEY, tabId)

                // Set appropriate icon based on command state
                icon = if (onAbort != null) AllIcons.Process.Step_1 else AllIcons.Process.Step_4
            }

            // Add close listener
            content.manager?.addContentManagerListener(object : com.intellij.ui.content.ContentManagerListener {
                override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                    if (event.content == content) {
                        val removedTabId = event.content.getUserData(TAB_ID_KEY)
                        removedTabId?.let { id ->
                            tabs[id]?.dispose()
                            tabs.remove(id)
                        }
                    }
                }
            })

            contentManager.addContent(content)
            contentManager.setSelectedContent(content)

            // Clean up old tabs if we exceed the limit
            cleanupOldTabs()
        }
        return tab
    }

    private fun generateTabId(commandData: CommandData?): String {
        val timestamp = System.currentTimeMillis()
        val counter = tabCounter.incrementAndGet()
        return "tab_${timestamp}_$counter"
    }

    private fun generateTabTitle(commandData: CommandData?): String {
        val timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
        return when {
            commandData?.structuredMode == true -> "Plan [$timestamp]"
            commandData?.message?.isNotBlank() == true -> {
                val shortMessage = commandData.message.take(30)
                if (commandData.message.length > 30) "$shortMessage... [$timestamp]"
                else "$shortMessage [$timestamp]"
            }

            else -> "Aider [$timestamp]"
        }
    }

    private fun cleanupOldTabs() {
        if (tabs.size > maxTabs) {
            // Remove oldest tabs (by creation order)
            val sortedTabs = tabs.entries.sortedBy { it.key }
            val tabsToRemove = sortedTabs.take(tabs.size - maxTabs)

            tabsToRemove.forEach { (tabId, tab) ->
                // Find and remove the content
                val content = contentManager.contents.find {
                    it.getUserData(TAB_ID_KEY) == tabId
                }
                content?.let { contentManager.removeContent(it, true) }
            }
        }
    }

    fun updateTabProgress(tab: AiderOutputTab, output: String, title: String) {
        SwingUtilities.invokeLater {
            tab.updateProgress(output, title)
        }
    }

    fun setTabFinished(tab: AiderOutputTab) {
        SwingUtilities.invokeLater {
            tab.setProcessFinished()

            // Update tab icon to indicate completion
            val tabId = tabs.entries.find { it.value == tab }?.key
            tabId?.let { id ->
                val content = contentManager.contents.find {
                    it.getUserData(TAB_ID_KEY) == id
                }
                content?.icon = AllIcons.Process.Step_4
            }
        }
    }

    companion object {
        private val TAB_ID_KEY = com.intellij.openapi.util.Key.create<String>("AIDER_TAB_ID")
    }
}
