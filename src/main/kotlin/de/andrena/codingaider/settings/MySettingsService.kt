package de.andrena.codingaider.settings

import com.intellij.openapi.components.Service

@Service(Service.Level.PROJECT)
class MySettingsService {
    fun getSettings() = AiderSettings.getInstance()

}
