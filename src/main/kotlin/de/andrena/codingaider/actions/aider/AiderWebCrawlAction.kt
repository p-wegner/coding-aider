package de.andrena.codingaider.actions.aider

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.*
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import de.andrena.codingaider.command.CommandData
import de.andrena.codingaider.command.CommandOptions
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.executors.api.IDEBasedExecutor
import de.andrena.codingaider.features.documentation.dialogs.GitRepoDocumentationDialog
import de.andrena.codingaider.features.webcrawl.WebCrawlAndGitDialog
import de.andrena.codingaider.inputdialog.AiderMode
import de.andrena.codingaider.services.AiderDocsService.Companion.AIDER_DOCS_FOLDER
import de.andrena.codingaider.services.AiderEditFormat
import de.andrena.codingaider.services.GitRepoCloneService
import de.andrena.codingaider.services.MarkdownConversionService
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.settings.AiderSettings.Companion.getInstance
import de.andrena.codingaider.utils.FileRefresher
import fleet.util.logging.logger
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class AiderWebCrawlAction : AnAction() {


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = WebCrawlAndGitDialog(project)
        dialog.show()
    }

}

