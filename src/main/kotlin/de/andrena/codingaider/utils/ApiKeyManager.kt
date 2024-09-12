package de.andrena.codingaider.utils

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object ApiKeyManager {
    private const val SERVICE_NAME = "CodingAiderApiKeys"

    fun saveApiKey(keyName: String, apiKey: String) {
        val credentialAttributes = createCredentialAttributes(keyName)
        val credentials = Credentials(keyName, apiKey)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun getApiKey(keyName: String): String? {
        val credentialAttributes = createCredentialAttributes(keyName)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun getAllStoredApiKeys(): Map<String, String> {
        return ApiKeyChecker.getAllApiKeyNames().mapNotNull { keyName ->
            getApiKey(keyName)?.let { keyName to it }
        }.toMap()
    }

    private fun createCredentialAttributes(keyName: String): CredentialAttributes {
        return CredentialAttributes(SERVICE_NAME, keyName)
    }
}
