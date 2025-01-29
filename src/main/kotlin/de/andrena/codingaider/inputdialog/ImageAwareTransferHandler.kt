package de.andrena.codingaider.inputdialog

import com.intellij.openapi.diagnostic.Logger
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import javax.swing.JComponent
import javax.swing.TransferHandler

class ImageAwareTransferHandler(
    private val delegate: TransferHandler?,
    private val onImagePasted: (Image) -> Unit
) : TransferHandler() {
    private val logger = Logger.getInstance(ImageAwareTransferHandler::class.java)

    override fun importData(support: TransferSupport): Boolean {
        logger.debug("importData called with TransferSupport")
        logger.debug("Available flavors: ${support.dataFlavors.joinToString { it.mimeType }}")
        
        try {
            // Try direct image data first
            if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                logger.debug("Processing image flavor")
                val image = support.transferable.getTransferData(DataFlavor.imageFlavor) as? Image
                if (image != null) {
                    onImagePasted(image)
                    return true
                }
            }
            
            // Try file list flavor for image files
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                logger.debug("Processing file list flavor")
                val fileList = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
                fileList?.firstOrNull()?.let { file ->
                    if (file is java.io.File && isImageFile(file.name)) {
                        logger.debug("Found image file: ${file.name}")
                        val image = javax.imageio.ImageIO.read(file)
                        if (image != null) {
                            onImagePasted(image)
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to import image data", e)
        }
        
        return delegate?.importData(support) ?: false
    }

    private fun isImageFile(fileName: String): Boolean {
        val extension = fileName.lowercase().substringAfterLast('.', "")
        return extension in setOf("png", "jpg", "jpeg", "gif", "bmp")
    }

    override fun canImport(support: TransferSupport): Boolean {
        logger.debug("canImport called with TransferSupport")
        logger.debug("Available flavors: ${support.dataFlavors.joinToString { it.mimeType }}")
        
        // Check for direct image data
        val hasImageFlavor = support.isDataFlavorSupported(DataFlavor.imageFlavor)
        // Check for file lists that might contain images
        val hasFileFlavor = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
        
        if (hasImageFlavor || hasFileFlavor) {
            logger.debug("Image or file flavor supported")
            return true
        }
        
        val delegateResult = delegate?.canImport(support) ?: false
        logger.debug("Delegating to handler ${delegate?.javaClass?.simpleName}, result: $delegateResult")
        return delegateResult
    }

    override fun importData(comp: JComponent, t: Transferable): Boolean {
        if (canImport(comp, t.transferDataFlavors)) {
            try {
                val image = t.getTransferData(DataFlavor.imageFlavor) as? Image
                if (image != null) {
                    onImagePasted(image)
                    return true
                }
            } catch (e: UnsupportedFlavorException) {
                e.printStackTrace()
            }
        }
        return delegate?.importData(comp, t) ?: false
    }

    override fun canImport(comp: JComponent?, flavors: Array<DataFlavor>): Boolean {
        logger.debug("canImport called with component ${comp?.javaClass?.simpleName}")
        logger.debug("Available flavors: ${flavors.joinToString { it.mimeType }}")
        val canImportImage = flavors.any { it == DataFlavor.imageFlavor }
        val delegateCanImport = delegate?.canImport(comp, flavors) ?: false
        logger.debug("Can import image: $canImportImage, delegate can import: $delegateCanImport")
        return canImportImage || delegateCanImport
    }

    override fun getSourceActions(c: JComponent?): Int {
        return delegate?.getSourceActions(c) ?: COPY_OR_MOVE
    }
}
