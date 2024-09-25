package de.andrena.codingaider.messages

import com.intellij.util.messages.Topic

interface PersistentFilesChangedTopic {
    fun onPersistentFilesChanged()

    companion object {
        val PERSISTENT_FILES_CHANGED_TOPIC = Topic.create("Persistent Files Changed", PersistentFilesChangedTopic::class.java)
    }
}
