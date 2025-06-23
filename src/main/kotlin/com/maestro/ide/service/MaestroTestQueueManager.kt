package com.maestro.ide.service

import com.intellij.openapi.project.Project
import com.maestro.common.extension.getAllTests
import com.maestro.ide.service.model.MaestroTest
import java.util.concurrent.LinkedBlockingQueue

/**
 * Manages the queue of Maestro tests waiting to be executed.
 *
 * This manager provides:
 * - Thread-safe test queuing for sequential execution
 * - Queue inspection and management operations
 * - Integration with project test discovery
 */
class MaestroTestQueueManager(private val project: Project) {

    // Thread-safe queue for pending test executions
    private val testQueue = LinkedBlockingQueue<MaestroTest>()

    // Reference to all project tests for validation
    private val allProjectTests: List<MaestroTest> = project.getAllTests()

    /**
     * Adds a test to the execution queue
     * @param test The test to queue for execution
     */
    fun queueTest(test: MaestroTest) {
        testQueue.offer(test)
    }
    
    /**
     * Retrieves and removes the next test from the queue
     * @return The next test to execute, or null if queue is empty
     */
    fun nextTest(): MaestroTest? {
        return testQueue.poll()
    }
    
    /**
     * Removes all tests from the queue
     */
    fun clearQueue() {
        testQueue.clear()
    }

    /**
     * Returns a snapshot of all tests currently in the queue
     * @return List of queued tests (does not modify the queue)
     */
    fun getQueuedTests(): List<MaestroTest> {
        return testQueue.toList()
    }

    /**
     * Checks if the queue is empty
     * @return true if no tests are queued, false otherwise
     */
    fun isEmpty(): Boolean {
        return testQueue.isEmpty()
    }

    /**
     * Gets the number of tests currently in the queue
     * @return Number of queued tests
     */
    fun size(): Int {
        return testQueue.size
    }
}
