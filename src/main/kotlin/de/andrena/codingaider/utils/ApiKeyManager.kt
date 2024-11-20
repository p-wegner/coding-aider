package de.andrena.codingaider.utils

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object ApiKeyManager {
    private const val SERVICE_NAME = "CodingAiderApiKeys"
    private const val CUSTOM_MODEL_PREFIX = "CustomModel:"

    fun saveApiKey(keyName: String, apiKey: String) {
        val credentialAttributes = createCredentialAttributes(keyName)
        val credentials = Credentials(keyName, apiKey)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun saveCustomModelKey(modelName: String, apiKey: String) {
        saveApiKey("$CUSTOM_MODEL_PREFIX$modelName", apiKey)
    }

    fun getApiKey(keyName: String): String? {
        val credentialAttributes = createCredentialAttributes(keyName)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun getCustomModelKey(modelName: String): String? {
        return getApiKey("$CUSTOM_MODEL_PREFIX$modelName")
    }

    fun removeApiKey(keyName: String) {
        val credentialAttributes = createCredentialAttributes(keyName)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    fun removeCustomModelKey(modelName: String) {
        removeApiKey("$CUSTOM_MODEL_PREFIX$modelName")
    }

    private fun createCredentialAttributes(keyName: String): CredentialAttributes {
        return CredentialAttributes("$SERVICE_NAME:$keyName")
    }
}
