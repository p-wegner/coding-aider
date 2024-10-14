package de.andrena.codingaider.actions.aider

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.ui.ImageUtil
import de.andrena.codingaider.command.FileData
import de.andrena.codingaider.services.PersistentFileService
import de.andrena.codingaider.utils.FileRefresher
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class AiderClipboardImageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val clipboard = CopyPasteManager.getInstance()

        if (clipboard.areDataFlavorsAvailable(DataFlavor.imageFlavor)) {
            val image: Image? = clipboard.contents?.getTransferData(DataFlavor.imageFlavor) as Image?
            if (image != null) {
                saveImageToFile(project, image)
            } else {
                showNotification(project, "No image found in clipboard", NotificationType.WARNING)
            }
        } else {
            showNotification(project, "Clipboard does not contain an image", NotificationType.WARNING)
        }
    }

    private fun saveImageToFile(project: Project, image: Image) {
        val projectRoot = project.basePath ?: "."
        val imagesPath = "$projectRoot/.aider-docs/images"
        File(imagesPath).mkdirs()

        val bufferedImage = toBufferedImage(image)
        val fileName = "clipboard_image_${UUID.randomUUID()}.png"
        val filePath = "$imagesPath/$fileName"
        val file = File(filePath)

        ImageIO.write(bufferedImage, "png", file)

        // Refresh the file and add it to PersistentFileManager
        refreshAndAddFile(project, filePath)

        showNotification(
            project,
            "Image saved and added to persistent files: $fileName",
            NotificationType.INFORMATION
        )
    }

    private fun toBufferedImage(image: Image): BufferedImage {
        if (image is BufferedImage) {
            return image
        }

        // Create a buffered image with transparency
        val bufferedImage = ImageUtil.createImage(
            image.getWidth(null), image.getHeight(null),
            BufferedImage.TYPE_INT_ARGB
        )

        // Draw the image on to the buffered image
        val graphics2D = bufferedImage.createGraphics()
        graphics2D.drawImage(image, 0, 0, null)
        graphics2D.dispose()

        return bufferedImage
    }

    private fun refreshAndAddFile(project: Project, filePath: String) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
        val persistentFileService = project.getService(PersistentFileService::class.java)
        persistentFileService.addFile(FileData(filePath, true))
        if (virtualFile != null) {
            FileRefresher.refreshFiles(arrayOf(virtualFile))
        }
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Aider Clipboard Image")
            .createNotification(content, type)
            .notify(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
