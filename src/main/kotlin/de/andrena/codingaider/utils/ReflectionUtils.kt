package de.andrena.codingaider.utils

import com.intellij.build.BuildTreeConsoleView
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import java.lang.reflect.Method

object ReflectionUtils {
    fun findMethod(clazz: Class<*>, name: String, vararg parameterTypes: Class<*>): Method? {
        return try {
            clazz.getDeclaredMethod(name, *parameterTypes)
                .apply { isAccessible = true }
        } catch (e: Exception) {
            try {
                // Try getting from superclass if not found
                clazz.superclass?.getDeclaredMethod(name, *parameterTypes)
                    ?.apply { isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }
    }
    fun getNodesMapFromBuildView(view: BuildTreeConsoleView): Map<*, *>? {
        return try {
            val nodesMapField = BuildTreeConsoleView::class.java.getDeclaredField("nodesMap")
            nodesMapField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            nodesMapField.get(view) as? Map<*, *>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getTestsMapFromConsoleView(view: SMTRunnerConsoleView): Map<*, *>? {
        return try {
            val nodesMapField = view::class.java.getDeclaredField("testsMap")
            nodesMapField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            nodesMapField.get(view) as? Map<*, *>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
