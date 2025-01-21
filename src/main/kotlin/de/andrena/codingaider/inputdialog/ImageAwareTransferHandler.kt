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

    override fun importData(comp: JComponent, t: Transferable): Boolean {
        if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            val image = t.getTransferData(DataFlavor.imageFlavor) as? Image
            if (image != null) {
                onImagePasted(image)
                return true
            }
        }
        return delegate?.importData(comp, t) ?: false
    }

    override fun canImport(comp: JComponent?, transferFlavors: Array<out DataFlavor>?): Boolean {
        if (transferFlavors?.contains(DataFlavor.imageFlavor) == true) {
            return true
        }
        return delegate?.canImport(comp, transferFlavors) ?: false
    }
}
