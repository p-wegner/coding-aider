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
        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            logger.debug("Image flavor supported")
            logger.debug("Transfer component: ${support.component?.javaClass?.simpleName}")
            try {
                val image = support.transferable.getTransferData(DataFlavor.imageFlavor) as? Image
                if (image != null) {
                    onImagePasted(image)
                    return true
                }
            } catch (e: Exception) {
                logger.warn("Failed to import image data", e)
            }
        }
        return delegate?.importData(support) ?: false
    }

    override fun canImport(support: TransferSupport): Boolean {
        logger.debug("canImport called with TransferSupport")
        logger.debug("Available flavors: ${support.dataFlavors.joinToString { it.mimeType }}")
        
        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            logger.debug("Image flavor supported")
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
