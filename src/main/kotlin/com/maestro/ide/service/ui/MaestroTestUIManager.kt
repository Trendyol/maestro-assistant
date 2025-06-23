package com.maestro.ide.service.ui

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.maestro.common.extension.invokeLater
import com.maestro.ide.toolWindow.MaestroTestRunConfiguration

/**
 * Manages the test UI integration with IntelliJ's test framework.
 *
 * This class handles:
 * - Creating and configuring test console views
 * - Integrating with the Run tool window
 * - Setting up test locators for navigation
 */
class MaestroTestUIManager(private val project: Project) {

    companion object {
        private const val MAESTRO_TEST_FRAMEWORK_ID = "Maestro"
    }

    /**
     * Creates a test console with SMTestRunner integration
     */
    fun createTestConsole(
        testName: String,
        processHandler: OSProcessHandler
    ): SMTRunnerConsoleView {
        // Create a mock run configuration
        val runConfiguration = MaestroTestRunConfiguration(project, "Maestro Test: $testName")

        // Create console properties for Maestro tests
        val consoleProperties = createConsoleProperties(runConfiguration)

        // Create the console view
        val consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole(
            "MaestroTests",
            processHandler,
            consoleProperties
        )

        return consoleView as SMTRunnerConsoleView
    }

    /**
     * Shows the test console in the Run tool window
     */
    fun showInRunToolWindow(testFileName: String, consoleView: SMTRunnerConsoleView, processHandler: OSProcessHandler) {
        invokeLater {
            val contentDescriptor = RunContentDescriptor(
                consoleView,
                processHandler,
                consoleView.component,
                "Maestro: $testFileName"
            )

            val executor = DefaultRunExecutor.getRunExecutorInstance()
            RunContentManager.getInstance(project).showRunContent(executor, contentDescriptor)
        }
    }

    /**
     * Creates console properties for Maestro tests
     */
    private fun createConsoleProperties(runConfiguration: MaestroTestRunConfiguration): SMTRunnerConsoleProperties {
        return object : SMTRunnerConsoleProperties(
            runConfiguration,
            MAESTRO_TEST_FRAMEWORK_ID,
            DefaultRunExecutor.getRunExecutorInstance()
        ) {
            override fun getTestLocator(): SMTestLocator {
                return createMaestroTestLocator()
            }

            override fun isIdBasedTestTree(): Boolean = true
        }
    }

    /**
     * Creates a test locator for Maestro tests
     */
    private fun createMaestroTestLocator(): SMTestLocator {
        return object : SMTestLocator {
            override fun getLocation(
                protocol: String,
                path: String,
                project: Project,
                scope: GlobalSearchScope
            ): List<Location<out PsiElement>> {
                val projectBasePath = project.basePath ?: return emptyList()

                if (!path.startsWith(projectBasePath)) {
                    return emptyList()
                }

                val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return emptyList()
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()

                return listOf(PsiLocation(psiFile))
            }
        }
    }
}