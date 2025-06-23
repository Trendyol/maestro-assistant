package com.maestro.ide.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.maestro.ide.service.model.MaestroTest
import java.io.File

/**
 * Handles the core execution of Maestro tests.
 *
 * This class is responsible for:
 * - Building command lines for test execution
 * - Managing process handlers
 * - Ensuring test files are synchronized before execution
 */
class MaestroTestExecutor(private val project: Project) {

    companion object {
        private const val MAESTRO_COMMAND = "maestro"
        private const val MAESTRO_TEST_COMMAND = "test"
    }

    /**
     * Builds the command line for executing a Maestro test
     */
    fun buildCommandLine(test: MaestroTest): GeneralCommandLine {
        val projectBasePath = project.basePath ?: throw IllegalStateException("Project has no base path")
        val absoluteTestPath = File(projectBasePath, test.path).absolutePath

        val commandLine = GeneralCommandLine()
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withExePath(MAESTRO_COMMAND)
            .withWorkDirectory(projectBasePath)

        // Build command parameters
        val parameters = mutableListOf(MAESTRO_TEST_COMMAND)

        // Add any additional CLI arguments
        if (test.commandLineArgs.isNotEmpty()) {
            parameters.addAll(test.commandLineArgs.split(" ").filter { it.isNotBlank() })
        }

        // Add the test path as the last parameter
        parameters.add(absoluteTestPath)

        return commandLine.withParameters(parameters)
    }

    /**
     * Creates a process handler for the given command
     */
    fun createProcessHandler(commandLine: GeneralCommandLine): OSProcessHandler {
        return OSProcessHandler(commandLine)
    }

    /**
     * Refreshes the test file in the virtual file system to ensure
     * any recent changes are written to disk before running the test
     */
    fun refreshTestFile(testPath: String) {
        val projectPath = project.basePath ?: return
        val absolutePath = if (testPath.startsWith(projectPath)) testPath else "$projectPath/$testPath"
        val file = File(absolutePath)

        // Find the virtual file in the file system
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return

        // Ensure all pending changes are saved to disk
        ApplicationManager.getApplication().invokeAndWait {
            // Save any unsaved document changes for this file
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            if (document != null) {
                FileDocumentManager.getInstance().saveDocument(document)
            }

            // Refresh the file from disk to ensure VFS is in sync
            virtualFile.refresh(false, false)
        }
    }
}