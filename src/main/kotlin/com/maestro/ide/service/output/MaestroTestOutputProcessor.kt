package com.maestro.ide.service.output

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key
import com.maestro.common.extension.invokeLater
import java.util.regex.Pattern

/**
 * Handles processing and display of test output from Maestro test execution.
 *
 * This class is responsible for:
 * - Processing standard output, error output, and system messages
 * - Parsing test steps from output
 * - Managing test proxy states based on output
 * - Displaying formatted output in the console
 */
class MaestroTestOutputProcessor(private val consoleView: SMTRunnerConsoleView?) {

    companion object {
        // Process output types
        private val STDOUT = Key.create<String>("STDOUT")
        private val STDERR = Key.create<String>("STDERR")

        // Output parsing patterns
        private const val TEST_STEP_MARKER = "Testing step:"
        private const val ERROR_MARKER = "Error:"
        private const val FAILED_MARKER = "Failed"

        // Error parsing pattern for test failures
        private val ERROR_PATTERN = Pattern.compile(".*?Error:.*?\\((.+)\\).*", Pattern.DOTALL)
    }

    /**
     * Creates a process listener to handle test output in real-time
     */
    fun createProcessListener(
        testPath: String,
        testName: String,
        rootTestProxy: SMTestProxy.SMRootTestProxy?,
        onTermination: (exitCode: Int, testProxy: SMTestProxy, output: String) -> Unit
    ): ProcessAdapter {
        val testProxy = SMTestProxy(testName, false, testPath)
        var hasStartedTest = false
        val output = StringBuilder()

        return object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val text = event.text
                output.append(text)

                invokeLater {
                    // Initialize test proxy if not already done
                    if (!hasStartedTest) {
                        initializeTestProxy(testProxy, rootTestProxy)
                        hasStartedTest = true
                    }

                    // Process and display output
                    processTestOutput(text, outputType, testProxy)
                }
            }

            override fun processTerminated(event: ProcessEvent) {
                invokeLater {
                    onTermination(event.exitCode, testProxy, output.toString())
                }
            }
        }
    }

    /**
     * Processes test output and updates the UI accordingly
     */
    fun processTestOutput(text: String, outputType: Key<*>, testProxy: SMTestProxy) {
        when (outputType) {
            STDOUT -> processStandardOutput(text, testProxy)
            STDERR -> processErrorOutput(text, testProxy)
            else -> displaySystemOutput(text)
        }
    }

    /**
     * Processes a test step from output and creates a child test proxy
     */
    fun processTestStep(text: String, parentProxy: SMTestProxy) {
        val stepName = text.substringAfter(TEST_STEP_MARKER).trim()
        val stepPath = "${parentProxy.locationUrl}:$stepName"
        val stepProxy = SMTestProxy(stepName, false, stepPath)

        parentProxy.addChild(stepProxy)
        stepProxy.setStarted()

        // Check step result based on output content
        if (!containsError(text)) {
            stepProxy.setFinished()
        } else {
            stepProxy.setTestFailed("Step failed", text, false)
            stepProxy.setFinished()
        }
    }

    /**
     * Parses error message from test output
     */
    fun parseErrorMessage(output: String): String {
        val matcher = ERROR_PATTERN.matcher(output)
        return if (matcher.find()) {
            matcher.group(1) ?: "Test execution failed"
        } else {
            "Test execution failed"
        }
    }

    /**
     * Displays a success message in the console
     */
    fun displaySuccessMessage(testName: String) {
        consoleView?.print(
            "\n✅ Test completed successfully: $testName\n",
            ConsoleViewContentType.SYSTEM_OUTPUT
        )
    }

    /**
     * Displays a failure message in the console
     */
    fun displayFailureMessage(testName: String) {
        consoleView?.print(
            "\n❌ Test failed: $testName\n",
            ConsoleViewContentType.ERROR_OUTPUT
        )
    }

    /**
     * Initializes the test proxy and adds it to the test tree
     */
    private fun initializeTestProxy(testProxy: SMTestProxy, rootTestProxy: SMTestProxy.SMRootTestProxy?) {
        rootTestProxy?.addChild(testProxy)
        testProxy.setStarted()
    }

    /**
     * Processes standard output from the test
     */
    private fun processStandardOutput(text: String, testProxy: SMTestProxy) {
        displayNormalOutput(text)

        // Check for test steps in output
        if (containsTestStep(text)) {
            processTestStep(text, testProxy)
        }
    }

    /**
     * Processes error output from the test
     */
    private fun processErrorOutput(text: String, testProxy: SMTestProxy) {
        displayErrorOutput(text)

        // Check for errors and mark test as failed if found
        if (containsError(text)) {
            testProxy.setTestFailed("Test failed", text, false)
        }
    }

    /**
     * Displays normal output in the console
     */
    private fun displayNormalOutput(text: String) {
        consoleView?.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    /**
     * Displays error output in the console
     */
    private fun displayErrorOutput(text: String) {
        consoleView?.print(text, ConsoleViewContentType.ERROR_OUTPUT)
    }

    /**
     * Displays system output in the console
     */
    private fun displaySystemOutput(text: String) {
        consoleView?.print(text, ConsoleViewContentType.SYSTEM_OUTPUT)
    }

    /**
     * Checks if the text contains a test step
     */
    private fun containsTestStep(text: String): Boolean {
        return text.contains(TEST_STEP_MARKER)
    }

    /**
     * Checks if the text contains an error
     */
    private fun containsError(text: String): Boolean {
        return text.contains(ERROR_MARKER) || text.contains(FAILED_MARKER)
    }
}