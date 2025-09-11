package com.maestro.ide.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
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
        private val LOG = Logger.getInstance(MaestroTestExecutor::class.java)
        private const val MAESTRO_COMMAND = "maestro"
        private const val MAESTRO_TEST_COMMAND = "test"
        private const val PL_APP_ID = "com.trendyol.milla.android.stage"
        private const val DEFAULT_APP_ID = "trendyol.com.stage"
        private const val PL_APP_PATH_INDICATOR = "pl-app"
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

        // Enhanced debug logging
        LOG.info("=== MAESTRO TEST EXECUTOR DEBUG ===")
        LOG.info("Test path: '${test.path}'")
        LOG.info("Command line args: '${test.commandLineArgs}'")
        LOG.info("Project base path: '$projectBasePath'")
        
        // Check if command line arguments already contain APP_ID
        val hasAppIdInArgs = test.commandLineArgs.contains("APP_ID=", ignoreCase = true)
        LOG.info("Command line args contain APP_ID: $hasAppIdInArgs")
        
        // Add APP_ID parameter based on test path only if not already provided in args
        if (!hasAppIdInArgs) {
            // Normalize path separators for cross-platform compatibility
            val normalizedPath = test.path.replace('\\', '/')
            val normalizedProjectPath = projectBasePath.replace('\\', '/')
            
            // Check both the relative test path and the project base path for pl-app indicator
            val testPathContainsPlApp = normalizedPath.contains(PL_APP_PATH_INDICATOR, ignoreCase = true)
            val projectPathContainsPlApp = normalizedProjectPath.contains(PL_APP_PATH_INDICATOR, ignoreCase = true)
            val containsPlApp = testPathContainsPlApp || projectPathContainsPlApp
            
            val appId = if (containsPlApp) {
                PL_APP_ID
            } else {
                DEFAULT_APP_ID
            }
            
            LOG.info("Normalized test path: '$normalizedPath'")
            LOG.info("Normalized project path: '$normalizedProjectPath'")
            LOG.info("Test path contains '$PL_APP_PATH_INDICATOR': $testPathContainsPlApp")
            LOG.info("Project path contains '$PL_APP_PATH_INDICATOR': $projectPathContainsPlApp")
            LOG.info("Overall contains '$PL_APP_PATH_INDICATOR': $containsPlApp")
            LOG.info("Selected appId: $appId")
            
            parameters.add("-e")
            parameters.add("APP_ID=$appId")
        } else {
            LOG.info("APP_ID already provided in command line arguments: '${test.commandLineArgs}'")
        }

        // Add any additional CLI arguments
        if (test.commandLineArgs.isNotEmpty()) {
            parameters.addAll(test.commandLineArgs.split(" ").filter { it.isNotBlank() })
        }

        // Add the test path as the last parameter
        parameters.add(absoluteTestPath)

        val finalCommandLine = commandLine.withParameters(parameters)
        LOG.info("Final command line: ${finalCommandLine.commandLineString}")
        LOG.info("Final parameters: $parameters")
        
        // Check for multiple APP_ID parameters in final command
        val appIdParams = parameters.filter { it.startsWith("APP_ID=") }
        if (appIdParams.size > 1) {
            LOG.warn("Multiple APP_ID parameters found: $appIdParams - Last one will be used!")
        }
        
        LOG.info("=== END MAESTRO TEST EXECUTOR DEBUG ===")
        
        return finalCommandLine
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