package com.maestro.ide.service

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.maestro.common.EventBus
import com.maestro.common.MaestroEvent
import com.maestro.common.extension.invokeLater
import com.maestro.common.extension.setTestStatus
import com.maestro.ide.service.completion.MaestroTestCompletionHandler
import com.maestro.ide.service.model.MaestroTest
import com.maestro.ide.service.model.TestStatus
import com.maestro.ide.service.output.MaestroTestOutputProcessor
import com.maestro.ide.service.ui.MaestroTestUIManager
import com.maestro.ide.service.util.MaestroNotifier
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service responsible for executing Maestro tests and managing their lifecycle.
 *
 * This service handles:
 * - Test execution with proper process management
 * - Integration with IntelliJ's test framework UI
 * - Test queue management and status tracking
 * - Real-time test output processing and display
 */
@Service(Service.Level.PROJECT)
class MaestroTestService(private val project: Project) {

    // Core dependencies
    private val notifier = MaestroNotifier(project)
    private val queueManager = MaestroTestQueueManager(project)
    private val testExecutor = MaestroTestExecutor(project)
    private val uiManager = MaestroTestUIManager(project)

    // Test execution state
    private val _isTestRunnerActive = AtomicBoolean(false)
    val isTestRunnerActive: Boolean
        get() = _isTestRunnerActive.get()

    // Test UI components
    private var consoleView: SMTRunnerConsoleView? = null
    private var processHandler: OSProcessHandler? = null
    private var rootTestProxy: SMTestProxy.SMRootTestProxy? = null
    private var outputProcessor: MaestroTestOutputProcessor? = null
    private var completionHandler: MaestroTestCompletionHandler? = null

    companion object {
        private val LOG = Logger.getInstance(MaestroTestService::class.java)
        @JvmStatic
        fun getInstance(project: Project): MaestroTestService = project.service()
    }

    /**
     * Runs a Maestro test by its model
     */
    fun runTest(test: MaestroTest) {
        runTestInternal(test)
    }

    /**
     * Runs a Maestro test identified by path and associated with a PsiFile
     * This method is kept for backward compatibility
     */
    fun runTest(testPath: String) {
        runTestInternal(MaestroTest(testPath))
    }

    /**
     * Runs multiple Maestro tests
     */
    fun runTests(tests: List<MaestroTest>) {
        // Queue all tests
        tests.forEach { test ->
            queueManager.queueTest(test)

            // Update test status to queued
            updateTestStatus(test.path, TestStatus.QUEUED)
        }

        // Start running if not already running
        if (!isTestRunnerActive) {
            queueManager.nextTest()?.let { runTest(it) }
        }
    }

    /**
     * Stops all running tests
     */
    fun stopTests() {
        // Get all tests in queue before clearing it
        val remainingTests = queueManager.getQueuedTests()

        // Clear the queue
        queueManager.clearQueue()
        _isTestRunnerActive.set(false)

        // Reset status of any queued tests to NOT_RUN
        for (test in remainingTests) {
            updateTestStatus(test.path, TestStatus.NOT_RUN)
        }

        invokeLater(modalityState = ModalityState.defaultModalityState()) {
            if (processHandler != null && !processHandler!!.isProcessTerminated) {
                processHandler!!.destroyProcess()
            }
        }
    }

    /**
     * Runs a Maestro test internally
     */
    private fun runTestInternal(test: MaestroTest) {
        _isTestRunnerActive.set(true)

        // Execute the test in a background task
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Running Maestro Test", true) {
            override fun run(indicator: ProgressIndicator) {
                executeTest(test)
            }
        })
    }

    /**
     * Execute a test with the given path and arguments
     */
    private fun executeTest(test: MaestroTest) {
        try {
            LOG.info("Starting execution of test: ${test.path}")

            // Prepare test execution
            testExecutor.refreshTestFile(test.path)
            updateTestStatus(test.path, TestStatus.RUNNING)

            // Build and execute command
            val commandLine = testExecutor.buildCommandLine(test)
            LOG.info("Built command line: ${commandLine.commandLineString}")

            processHandler = testExecutor.createProcessHandler(commandLine)

            // Set up UI components
            setupTestUI(test.path, File(test.path).name)

            // Wait for process completion
            processHandler?.waitFor()

            // Handle completion
            handleTestCompletion(test.path)
        } catch (e: Exception) {
            LOG.warn("Error executing test: ${test.path}", e)
            handleTestExecutionError(test.path, e)
        }
    }

    /**
     * Sets up the test UI components
     */
    private fun setupTestUI(testPath: String, testFileName: String) {
        ApplicationManager.getApplication().invokeAndWait({
            // Create test console and UI components
            consoleView = uiManager.createTestConsole(testFileName, processHandler!!)
            rootTestProxy = consoleView?.resultsViewer?.testsRootNode

            // Properly configure root node for best framework integration
            rootTestProxy?.apply {
                setTestsReporterAttached()
                setStarted()
            }

            // Show in Run toolwindow
            uiManager.showInRunToolWindow(testFileName, consoleView!!, processHandler!!)

            // Initialize processors
            outputProcessor = MaestroTestOutputProcessor(consoleView)
            completionHandler = MaestroTestCompletionHandler(consoleView)

            // Setup process listener
            val processListener = outputProcessor!!.createProcessListener(
                testPath, testFileName, rootTestProxy
            ) { exitCode, testProxy, output ->
                handleProcessTermination(exitCode, testProxy, output)
            }

            processHandler!!.addProcessListener(processListener)
            processHandler!!.startNotify()
        }, ModalityState.defaultModalityState())
    }

    /**
     * Handles test completion status and cleanup
     */
    private fun handleTestCompletion(testPath: String) {
        val exitCode = processHandler?.takeIf { it.isProcessTerminated }?.exitCode ?: -1
        val status = if (exitCode == 0) TestStatus.PASSED else TestStatus.FAILED

        invokeLater(modalityState = ModalityState.defaultModalityState()) {
            // Update status in our own tracking system
            updateTestStatus(testPath, status)

            // Reset runner state
            _isTestRunnerActive.set(false)

            // Run next test if available after a small delay to ensure UI updates properly
            invokeLater(modalityState = ModalityState.nonModal()) {
                queueManager.nextTest()?.let { runTest(it) }
            }
        }
    }

    /**
     * Handles errors during test execution
     */
    private fun handleTestExecutionError(testPath: String, e: Exception) {
        invokeLater(modalityState = ModalityState.defaultModalityState()) {
            notifier.showError("Maestro Test Failure", "Failed to run test: ${e.message}")
            updateTestStatus(testPath, TestStatus.FAILED)
            _isTestRunnerActive.set(false)

            // Log error to console if available
            consoleView?.print(
                "Error executing Maestro test: ${e.message}\n${e.stackTraceToString()}\n",
                ConsoleViewContentType.ERROR_OUTPUT
            )

            // Ensure test suite is marked as finished
            rootTestProxy?.setFinished()
        }
    }

    /**
     * Handles process termination and delegates to completion handler
     */
    private fun handleProcessTermination(exitCode: Int, testProxy: SMTestProxy, output: String) {
        invokeLater(modalityState = ModalityState.defaultModalityState()) {
            if (exitCode == 0) {
                completionHandler?.handleSuccessfulCompletion(testProxy, rootTestProxy)
            } else {
                val errorMessage = outputProcessor?.parseErrorMessage(output) ?: "Test execution failed"
                completionHandler?.handleTestFailure(testProxy, rootTestProxy, exitCode, errorMessage)
            }
        }
    }

    /**
     * Updates test status and notifies listeners
     */
    private fun updateTestStatus(testPath: String, status: TestStatus) {
        invokeLater(modalityState = ModalityState.defaultModalityState()) {
            // Locate the file by path
            val projectPath = project.basePath
            if (projectPath != null) {
                val absolutePath = if (testPath.startsWith(projectPath)) testPath else "$projectPath/$testPath"
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(absolutePath)

                if (virtualFile != null) {
                    // Get the PsiFile and update its status
                    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    if (psiFile != null) {
                        psiFile.setTestStatus(status)

                        // Force line markers to be repainted
                        invokeLater {
                            DaemonCodeAnalyzer.getInstance(project)
                                .restart(psiFile)
                        }
                    }
                }
            }

            // Notify event listeners about the status change
            EventBus.sendEvent(MaestroEvent.OnTestStatusChanged(testPath, status))
        }
    }
}
