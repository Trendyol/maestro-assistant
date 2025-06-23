package com.maestro.ide.toolWindow

import com.maestro.ide.service.model.MaestroTest
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Tree model for Maestro test files organized in a folder structure
 */
class MaestroTestTreeModel(tests: List<MaestroTest>) : DefaultTreeModel(DefaultMutableTreeNode("Root")) {

    private val rootNode = root as DefaultMutableTreeNode

    init {
        buildTree(tests)
    }

    /**
     * Build the tree structure from a list of tests
     */
    fun buildTree(tests: List<MaestroTest>) {
        rootNode.removeAllChildren()

        // Map to store each directory path to its node
        val directoryNodes = mutableMapOf<String, DefaultMutableTreeNode>()

        // Sort tests by path for consistent ordering
        val sortedTests = tests.sortedBy { it.path }

        // First pass: create directory structure
        sortedTests.forEach { test ->
            val pathParts = test.path.split('/')
            var currentPath = ""

            // For each directory in the path
            for (i in 0 until pathParts.size - 1) {
                val dirName = pathParts[i]
                val parentPath = currentPath
                currentPath = if (currentPath.isEmpty()) dirName else "$currentPath/$dirName"

                if (!directoryNodes.containsKey(currentPath)) {
                    // Create directory node if it doesn't exist
                    val dirNode = DefaultMutableTreeNode(TestTreeNode(null, dirName, false))

                    if (parentPath.isEmpty()) {
                        // Root directory
                        rootNode.add(dirNode)
                    } else {
                        // Add to parent directory
                        val parentNode = directoryNodes[parentPath]
                        parentNode?.add(dirNode)
                    }

                    directoryNodes[currentPath] = dirNode
                }
            }
        }

        // Second pass: add the files to appropriate directories
        sortedTests.forEach { test ->
            val pathParts = test.path.split('/')
            val fileName = pathParts.last()
            val dirPath = if (pathParts.size > 1) pathParts.dropLast(1).joinToString("/") else ""

            // Create the file node
            val fileNode = DefaultMutableTreeNode(TestTreeNode(test, fileName, true))

            if (dirPath.isEmpty()) {
                // File is at the root
                rootNode.add(fileNode)
            } else {
                // Add to parent directory
                val dirNode = directoryNodes[dirPath]
                dirNode?.add(fileNode)
            }
        }

        // Notify the tree of the structure change
        reload()
    }
}

/**
 * Class representing a node in the test tree
 * @param test The test object (null for directory nodes)
 * @param name The display name of the node
 * @param isFile Whether this node represents a file (rather than a directory)
 */
data class TestTreeNode(
    val test: MaestroTest?,
    val name: String,
    val isFile: Boolean
) {
    override fun toString(): String = name
}