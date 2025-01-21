package de.andrena.codingaider.inputdialog

import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.JComponent
import javax.swing.TransferHandler

class ImageAwareTransferHandler(
    private val delegate: TransferHandler?,
    private val onImagePasted: (Image) -> Unit
) : TransferHandler() {

    override fun importData(support: TransferSupport): Boolean {
        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                val image = support.transferable.getTransferData(DataFlavor.imageFlavor) as? Image
                if (image != null) {
                    onImagePasted(image)
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return delegate?.importData(support) ?: false
    }

    override fun canImport(support: TransferSupport): Boolean {
        if (support.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            return true
        }
        return delegate?.canImport(support) ?: false
    }

    // Keep the old methods for backward compatibility
    override fun importData(comp: JComponent, t: Transferable): Boolean {
        return importData(TransferSupport(comp, t))
    }

}
