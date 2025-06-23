package com.maestro.ide.service.model

import com.intellij.openapi.project.Project
import com.maestro.ide.service.MaestroTestService

/**
 * Represents a Maestro test file with execution metadata.
 *
 * @property path Relative path to the test file from project root
 * @property status Current execution status of the test
 * @property commandLineArgs Additional CLI arguments to pass to maestro command
 */
data class MaestroTest(
    val path: String,
    var status: TestStatus = TestStatus.NOT_RUN,
    var commandLineArgs: String = ""
) {
    /**
     * Gets the filename of this test
     */
    val fileName: String
        get() = path.substringAfterLast('/')

    /**
     * Gets the directory containing this test
     */
    val directory: String
        get() = path.substringBeforeLast('/', "")

    /**
     * Checks if this test is currently running
     */
    val isRunning: Boolean
        get() = status == TestStatus.RUNNING

    /**
     * Checks if this test has completed (passed or failed)
     */
    val isCompleted: Boolean
        get() = status == TestStatus.PASSED || status == TestStatus.FAILED
}

/**
 * Extension function to run this test using the project's test service
 * @param project The IntelliJ project context
 */
fun MaestroTest.run(project: Project) {
    MaestroTestService.getInstance(project).runTest(this)
}
