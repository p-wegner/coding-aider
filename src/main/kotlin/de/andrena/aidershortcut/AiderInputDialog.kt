package de.andrena.aidershortcut

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.*
import java.awt.*
import java.io.File

class AiderInputDialog(private val project: Project, files: List<String>) : DialogWrapper(project) {
    private val inputTextArea = JTextArea(5, 30)
    private val yesCheckBox = JCheckBox("Add --yes flag", false)
    private val commandOptions = arrayOf("--mini", "--sonnet", "--4o", "--deepseek") // Added --deepseek
    private val commandComboBox = JComboBox(commandOptions)
    private val additionalArgsField = JTextField(20)
    private val readOnlyFilesPanel = JPanel(GridLayout(0, 1)).apply {
        preferredSize = Dimension(300, 200)
    }
    private val readOnlyFilesScrollPane = JScrollPane(readOnlyFilesPanel).apply {
        verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    }
    private val readOnlyToggleMap = mutableMapOf<String, JCheckBox>()
    private val modeToggle = JCheckBox("Shell Mode", false)
    private val messageLabel = JLabel("Enter your message:")
    private val historyComboBox = JComboBox<String>()
    private val historyHandler = AiderHistoryHandler(project.basePath ?: "")

    init {
        title = "Aider Command"
        init()
        setupReadOnlyFiles(files)
        loadHistory()
    }

    private fun setupReadOnlyFiles(files: List<String>) {
        for (file in files) {
            val fileName = File(file).name
            val checkBox = JCheckBox(fileName)
            checkBox.toolTipText = file
            readOnlyToggleMap[file] = checkBox
            readOnlyFilesPanel.add(checkBox)
        }
    }

    private fun loadHistory() {
        val historyFile = File("${project.basePath}/.aider.input.history")
        if (historyFile.exists()) {
            historyFile.readLines().forEach { line ->
                historyComboBox.addItem(line)
            }
        }
        historyComboBox.addItem("Select previous command...")
        historyComboBox.addActionListener {
            if (historyComboBox.selectedIndex > 0) {
                inputTextArea.text = historyComboBox.selectedItem as String
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            weightx = 1.0
            weighty = 0.0
            gridx = 0
            gridy = 0
            gridwidth = GridBagConstraints.REMAINDER
        }

        // Mode Toggle
        panel.add(modeToggle, gbc)

        // History Combo Box
        gbc.gridy++
        panel.add(historyComboBox, gbc)

        // Message Label
        gbc.gridy++
        panel.add(messageLabel, gbc)

        // Input Text Area
        gbc.gridy++
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        panel.add(JScrollPane(inputTextArea), gbc)

        // Yes Flag Checkbox
        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(yesCheckBox, gbc)

        // Command Combo Box
        gbc.gridy++
        gbc.gridwidth = 1
        panel.add(JLabel("Select command:"), gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        panel.add(commandComboBox, gbc)

        // Additional Args Field
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = 1
        panel.add(JLabel("Additional arguments:"), gbc)
        gbc.gridx = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        panel.add(additionalArgsField, gbc)

        // Read-only Files
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = GridBagConstraints.REMAINDER
        panel.add(JLabel("Select Read-only files:"), gbc)

        gbc.gridy++
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        panel.add(readOnlyFilesScrollPane, gbc)

        // Add listener to toggle visibility of input text area and change label
        modeToggle.addActionListener {
            val isShellMode = modeToggle.isSelected
            inputTextArea.isVisible = !isShellMode
            messageLabel.text = if (isShellMode) "Shell mode enabled" else "Enter your message:"
        }

        return panel
    }

    fun getInputText(): String = inputTextArea.text
    fun isYesFlagChecked(): Boolean = yesCheckBox.isSelected
    fun getSelectedCommand(): String = commandComboBox.selectedItem as String
    fun getAdditionalArgs(): String = additionalArgsField.text

    fun getReadOnlyFiles(): List<String> {
        return readOnlyToggleMap.filter { it.value.isSelected }.keys.toList()
    }

    fun isShellMode(): Boolean = modeToggle.isSelected

    fun addToHistory(command: String) {
        historyHandler.addToHistory(command.removePrefix("+"))
    }
}
