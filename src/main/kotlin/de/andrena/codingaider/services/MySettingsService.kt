package de.andrena.codingaider.services

import com.intellij.openapi.components.Service
import de.andrena.codingaider.settings.AiderSettings

@Service(Service.Level.PROJECT)
class MySettingsService {
    fun getSettings() = AiderSettings.getInstance()

}
