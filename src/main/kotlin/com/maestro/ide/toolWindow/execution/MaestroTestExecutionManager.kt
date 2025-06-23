package com.maestro.ide.toolWindow.execution

import com.maestro.ide.service.MaestroTestService
import com.maestro.ide.service.model.MaestroTest
import com.maestro.ide.toolWindow.TestTreeNode
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Handles test execution logic for the Maestro test tool window.
 *
 * This class is responsible for:
 * - Running individual tests
 * - Running multiple selected tests
 * - Collecting tests from directory selections
 * - Applying command line arguments to tests
 */
class MaestroTestExecutionManager(private val testService: MaestroTestService) {

    /**
     * Runs a single test with the given command line arguments
     */
    fun runSingleTest(test: MaestroTest, commandLineArgs: String) {
        // Create a copy of the test with the command line arguments to avoid mutation
        val testToRun = test.copy(commandLineArgs = commandLineArgs)
        testService.runTest(testToRun)
    }

    /**
     * Runs multiple tests with the given command line arguments
     */
    fun runTests(tests: List<MaestroTest>, commandLineArgs: String) {
        if (testService.isTestRunnerActive || tests.isEmpty()) return

        // Apply command line arguments to all tests
        val testsToRun = tests.map { it.copy(commandLineArgs = commandLineArgs) }
        testService.runTests(testsToRun)
    }

    /**
     * Collects tests from selected tree paths
     */
    fun collectTestsFromSelection(selectionPaths: Array<TreePath>): List<MaestroTest> {
        val selectedTests = mutableListOf<MaestroTest>()

        for (path in selectionPaths) {
            val node = path.lastPathComponent as? DefaultMutableTreeNode ?: continue
            val userObject = node.userObject as? TestTreeNode ?: continue

            if (userObject.isFile && userObject.test != null) {
                // Direct file selection
                selectedTests.add(userObject.test)
            } else if (!userObject.isFile) {
                // Directory selection - add all test files within this directory
                selectedTests.addAll(collectTestsInNode(node))
            }
        }

        return selectedTests
    }

    /**
     * Recursively collects all test files within a directory node
     */
    private fun collectTestsInNode(node: DefaultMutableTreeNode): List<MaestroTest> {
        val tests = mutableListOf<MaestroTest>()
        val queue = ArrayDeque<DefaultMutableTreeNode>()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val userObject = current.userObject as? TestTreeNode

            if (userObject != null && userObject.isFile && userObject.test != null) {
                tests.add(userObject.test)
            }

            // Add all child nodes to the queue
            for (i in 0 until current.childCount) {
                queue.add(current.getChildAt(i) as DefaultMutableTreeNode)
            }
        }

        return tests
    }

    /**
     * Stops all running tests
     */
    fun stopTests() {
        testService.stopTests()
    }
}
