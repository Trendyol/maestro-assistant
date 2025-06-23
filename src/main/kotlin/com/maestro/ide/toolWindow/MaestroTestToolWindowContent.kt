package com.maestro.ide.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.maestro.common.EventBus
import com.maestro.common.MaestroEvent
import com.maestro.common.extension.getAllTests
import com.maestro.common.extension.getRelativePath
import com.maestro.common.icon.MaestroIcons
import com.maestro.ide.service.MaestroTestService
import com.maestro.ide.service.model.MaestroTest
import com.maestro.ide.toolWindow.actions.MaestroToolWindowActions
import com.maestro.ide.toolWindow.execution.MaestroTestExecutionManager
import com.maestro.ide.toolWindow.status.MaestroTestStatusManager
import java.awt.BorderLayout
import java.awt.Event
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Main content panel for the Maestro test tool window.
 *
 * This component coordinates between specialized managers to provide:
 * - Tree view of all the Maestro test files in the project
 * - Search and filtering capabilities for tests
 * - Test execution controls and status display
 * - Real-time test status updates and visual feedback
 */
class MaestroTestToolWindowContent(private val project: Project) : Disposable {

    // Core dependencies
    private val testService = MaestroTestService.getInstance(project)

    // Specialized managers
    private lateinit var executionManager: MaestroTestExecutionManager
    private lateinit var statusManager: MaestroTestStatusManager
    private lateinit var actionsFactory: MaestroToolWindowActions

    // UI Components
    private val mainPanel = JPanel(BorderLayout())
    private val toolbarPanel = JPanel(BorderLayout())
    private val topPanel = JPanel(BorderLayout(0, 5))
    private val statusPanel = JPanel(BorderLayout())

    private val treeModel = MaestroTestTreeModel(emptyList())
    private val testTree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        cellRenderer = MaestroTestTreeCellRenderer()
        selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    }

    private val searchField = SearchTextField()
    private val argsField = SearchTextField().apply {
        textEditor.emptyText.text = "Additional Maestro command line arguments"
        toolTipText = "E.g., --include-tags=highest,event"
    }
    private val statusLabel = JLabel("Ready")

    // State
    private val allTests = mutableListOf<MaestroTest>()
    private var toolbar: ActionToolbar? = null
    private var uiRefreshTimer: Timer? = null

    // Constants
    companion object {
        private const val DOUBLE_CLICK_COUNT = 2
        private const val UI_REFRESH_INTERVAL_MS = 500
    }

    // Event handling
    private val eventListener: (MaestroEvent) -> Unit = { event ->
        when (event) {
            is MaestroEvent.OnTestStatusChanged -> handleTestStatusChanged(event)
        }
    }

    init {
        initializeComponent()
    }

    fun getContent(): JComponent = mainPanel

    override fun dispose() {
        cleanup()
    }

    /**
     * Initializes all component setup in the correct order
     */
    private fun initializeComponent() {
        initializeManagers()
        setupUI()
        setupActions()
        refreshTestList()
        setupUIRefreshTimer()
        registerEventListeners()
    }

    /**
     * Initializes the specialized manager components
     */
    private fun initializeManagers() {
        executionManager = MaestroTestExecutionManager(testService)
        statusManager = MaestroTestStatusManager(statusLabel, testService)
        actionsFactory = MaestroToolWindowActions(
            project = project,
            onRefresh = ::refreshTestList,
            onRunSelected = ::runSelectedTests,
            onStop = ::stopTests,
            canRunTests = { testTree.selectionPaths != null }
        )
    }

    /**
     * Handles cleanup when the component is disposed
     */
    private fun cleanup() {
        executionManager.stopTests()
        uiRefreshTimer?.stop()
        uiRefreshTimer = null
    }

    /**
     * Handles test status change events
     */
    private fun handleTestStatusChanged(event: MaestroEvent.OnTestStatusChanged) {
        val relativePath = project.getRelativePath(event.filePath)

        // Find and update the test in our list
        findTestByPath(relativePath, event.filePath)?.let { test ->
            test.status = event.status
            refreshUI()
        }
    }

    /**
     * Finds a test by its relative or absolute path
     */
    private fun findTestByPath(relativePath: String, absolutePath: String): MaestroTest? {
        return allTests.find { test ->
            test.path == relativePath || "${project.basePath}/${test.path}" == absolutePath
        }
    }

    /**
     * Refreshes the UI components
     */
    private fun refreshUI() {
        testTree.repaint()
        statusManager.updateStatusLabel(allTests)
    }

    /**
     * Registers event listeners for test status updates
     */
    private fun registerEventListeners() {
        EventBus.onEvent(eventListener)
    }

    /**
     * Sets up a timer to refresh the UI for running tests
     */
    private fun setupUIRefreshTimer() {
        uiRefreshTimer = Timer(UI_REFRESH_INTERVAL_MS) {
            // If we have any running tests, repaint the list to show animation effect
            if (allTests.any { it.status.isActive }) {
                refreshUI()
            }
        }
        uiRefreshTimer?.start()
    }

    /**
     * Sets up the main UI components
     */
    private fun setupUI() {
        setupSearchField()
        setupToolbar()
        setupTestList()
        setupPanels()
        setupPopupMenu()
        setupKeyboardShortcuts()
    }

    /**
     * Sets up the search field and arguments field
     */
    private fun setupSearchField() {
        searchField.addKeyboardListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                filterTests(searchField.text)
            }
        })
        topPanel.add(searchField, BorderLayout.NORTH)
        topPanel.add(argsField, BorderLayout.SOUTH)
        toolbarPanel.add(topPanel, BorderLayout.CENTER)
    }

    /**
     * Sets up the toolbar with actions
     */
    private fun setupToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(actionsFactory.createRefreshAction())
            add(actionsFactory.createRunSelectedAction())
            add(actionsFactory.createStopAction())
        }

        toolbar = ActionManager.getInstance().createActionToolbar(
            "MaestroTestToolWindow",
            actionGroup,
            true
        )
        toolbar?.apply {
            toolbarPanel.add(component, BorderLayout.EAST)
            targetComponent = mainPanel
        }
    }

    /**
     * Sets up the test tree with mouse listener
     */
    private fun setupTestList() {
        testTree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == DOUBLE_CLICK_COUNT) {
                    handleDoubleClick()
                }
            }
        })
    }

    /**
     * Handles double-click events on the test tree
     */
    private fun handleDoubleClick() {
        val path = testTree.selectionPath ?: return
        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val userObject = node.userObject as? TestTreeNode ?: return

        // Only handle double-clicks on files, not directories
        if (userObject.isFile && userObject.test != null) {
            executionManager.runSingleTest(userObject.test, argsField.text)
        }
    }

    /**
     * Sets up the main panels and layout
     */
    private fun setupPanels() {
        toolbarPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        mainPanel.add(toolbarPanel, BorderLayout.NORTH)
        mainPanel.add(JBScrollPane(testTree), BorderLayout.CENTER)
        statusPanel.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)
        statusPanel.add(statusLabel, BorderLayout.WEST)
        mainPanel.add(statusPanel, BorderLayout.SOUTH)
    }

    /**
     * Sets up the context popup menu
     */
    private fun setupPopupMenu() {
        val popupMenu = JPopupMenu().apply {
            add(JMenuItem("Run Selected Tests", MaestroIcons.RUN_ICON).apply {
                addActionListener { runSelectedTests() }
            })
        }
        testTree.componentPopupMenu = popupMenu
    }

    /**
     * Sets up keyboard shortcuts
     */
    private fun setupKeyboardShortcuts() {
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.CTRL_MASK)
        testTree.registerKeyboardAction(
            { runSelectedTests() },
            keyStroke,
            JComponent.WHEN_FOCUSED
        )
    }

    /**
     * Stops any running tests
     */
    private fun stopTests() {
        executionManager.stopTests()
        refreshUI()
    }

    /**
     * Runs the selected tests
     */
    private fun runSelectedTests() {
        val selectionPaths = testTree.selectionPaths ?: return
        val selectedTests = executionManager.collectTestsFromSelection(selectionPaths)

        executionManager.runTests(selectedTests, argsField.text)
        refreshUI()
    }

    /**
     * Filters the tests based on search text
     */
    private fun filterTests(searchText: String) {
        val filteredTests = allTests.filter {
            searchText.isEmpty() || it.path.contains(searchText, ignoreCase = true)
        }

        // Rebuild tree with filtered tests
        treeModel.buildTree(filteredTests)

        // Expand all nodes to show search results
        if (searchText.isNotEmpty()) {
            expandAllNodes(testTree, 0, testTree.rowCount)
        }
    }

    /**
     * Expands all nodes in the tree recursively
     */
    private fun expandAllNodes(tree: JTree, startingRow: Int, rowCount: Int) {
        for (i in startingRow until rowCount) {
            tree.expandRow(i)
        }

        // If the row count changed after expanding, expand the new rows too
        if (rowCount != tree.rowCount) {
            expandAllNodes(tree, rowCount, tree.rowCount)
        }
    }

    /**
     * Refreshes the test list from the project
     */
    private fun refreshTestList() {
        allTests.clear()
        allTests.addAll(project.getAllTests())
        treeModel.buildTree(allTests)

        // Expand root level directories
        val root = testTree.model.root as DefaultMutableTreeNode
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i) as DefaultMutableTreeNode
            testTree.expandPath(TreePath(arrayOf(root, child)))
        }

        statusManager.updateStatusLabel(allTests)
    }

    // Legacy method kept for compatibility - remove when no longer needed
    private fun setupActions() {
        // This method is now handled by setupKeyboardShortcuts()
    }
}
