package de.andrena.codingaider.executors

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.testFramework.replaceService

class ServiceContainerUtil {
    companion object {
        fun <T : Any> replaceService(
            project: Project,
            serviceInterface: Class<T>,
            instance: T,
            parentDisposable: Disposable
        ) {
            project.replaceService(serviceInterface, instance, parentDisposable)
        }
    }

}
