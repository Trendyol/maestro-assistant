package com.maestro.ide.service.completion

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import java.lang.reflect.Field

/**
 * Handles test completion logic and state management for Maestro tests.
 *
 * This class is responsible for:
 * - Managing successful test completion workflows
 * - Handling test failure scenarios
 * - Setting proper test proxy states using reflection when needed
 * - Coordinating between child tests, main tests, and root proxies
 */
class MaestroTestCompletionHandler(private val consoleView: SMTRunnerConsoleView?) {

    /**
     * Handles successful test completion by marking all components as passed
     */
    fun handleSuccessfulCompletion(testProxy: SMTestProxy, rootTestProxy: SMTestProxy.SMRootTestProxy?) {
        // Mark individual test steps as passed
        markChildTestsPassed(testProxy)

        // Mark test itself as passed
        finalizeTestProxy(testProxy)
        forceTestPassedState(testProxy)

        // Mark root as passed - important for test state display
        finalizeRootTestProxy(testProxy, rootTestProxy)

        // Display success message
        displaySuccessMessage(testProxy.name)
    }

    /**
     * Handles test failure by setting appropriate states and messages
     */
    fun handleTestFailure(
        testProxy: SMTestProxy,
        rootTestProxy: SMTestProxy.SMRootTestProxy?,
        exitCode: Int,
        errorMessage: String
    ) {
        testProxy.setTestFailed(
            "Test failed with exit code: $exitCode",
            errorMessage,
            false
        )
        testProxy.setFinished()

        // Make sure the root test is also marked as finished
        rootTestProxy?.let { root ->
            // Add the test proxy as child of root if not already there
            if (testProxy.parent == null) {
                root.addChild(testProxy)
            }

            if (!root.isFinal) {
                root.setFinished()
            }
        }

        displayFailureMessage(testProxy.name)
    }

    /**
     * Marks all child tests as passed
     */
    private fun markChildTestsPassed(testProxy: SMTestProxy) {
        testProxy.children.forEach { stepTest ->
            if (!stepTest.isFinal) {
                stepTest.setFinished()
            }
            forceTestPassedState(stepTest)
        }
    }

    /**
     * Finalizes a test proxy if not already finished
     */
    private fun finalizeTestProxy(testProxy: SMTestProxy) {
        if (!testProxy.isFinal) {
            testProxy.setFinished()
        }
    }

    /**
     * Finalizes the root test proxy and ensures the test is properly attached
     */
    private fun finalizeRootTestProxy(testProxy: SMTestProxy, rootTestProxy: SMTestProxy.SMRootTestProxy?) {
        rootTestProxy?.let { root ->
            // Add the test proxy as child of root if not already there
            if (testProxy.parent == null) {
                root.addChild(testProxy)
            }

            // Finish the root node properly
            if (!root.isFinal) {
                root.setFinished()
            }
            forceTestPassedState(root)
        }
    }

    /**
     * Uses reflection to force a test proxy to have a PASSED state rather than TERMINATED
     */
    private fun forceTestPassedState(testProxy: SMTestProxy) {
        try {
            // First approach: Try to call the direct setPassedIfNotRunning method if it exists
            try {
                val setPassedMethod = testProxy.javaClass.getDeclaredMethod("setPassedIfNotRunning")
                setPassedMethod.isAccessible = true
                setPassedMethod.invoke(testProxy)
                return // If this succeeds, we're done
            } catch (e: Exception) {
                // Continue with other approaches if this doesn't work
            }

            // Second approach: Call the setFinished method first for proper state transition
            if (!testProxy.isFinal) {
                runCatching {
                    testProxy.setFinished()
                }
            }

            // Third approach: Direct state field manipulation
            val stateField: Field = SMTestProxy::class.java.getDeclaredField("myState")
            stateField.isAccessible = true

            // Get the TestPassedState field from SMTestProxy class
            val passedStateField: Field =
                Class.forName("com.intellij.execution.testframework.sm.runner.states.TestPassedState")
                    .getDeclaredField("INSTANCE")
            passedStateField.isAccessible = true
            val passedState = passedStateField.get(null)

            // Set the state to PASSED
            stateField.set(testProxy, passedState)

            // Try to set duration if applicable
            runCatching {
                val myDurationField = SMTestProxy::class.java.getDeclaredField("myDuration")
                myDurationField.isAccessible = true
                if (myDurationField.get(testProxy) == null) {
                    // Set a minimal duration if none exists
                    myDurationField.set(testProxy, 1L)
                }
            }
        } catch (e: Exception) {
            // Log the exception and provide fallback
            consoleView?.print(
                "\nFailed to set test state to PASSED: ${e.message}\n",
                ConsoleViewContentType.SYSTEM_OUTPUT
            )

            // Fallback to just adding a success message if reflection fails
            consoleView?.print(
                "\n[PASSED] ${testProxy.name}\n",
                ConsoleViewContentType.SYSTEM_OUTPUT
            )
        }
    }

    /**
     * Displays a success message in the console
     */
    private fun displaySuccessMessage(testName: String) {
        consoleView?.print(
            "\n✅ Test completed successfully: $testName\n",
            ConsoleViewContentType.SYSTEM_OUTPUT
        )
    }

    /**
     * Displays a failure message in the console
     */
    private fun displayFailureMessage(testName: String) {
        consoleView?.print(
            "\n❌ Test failed: $testName\n",
            ConsoleViewContentType.ERROR_OUTPUT
        )
    }
}