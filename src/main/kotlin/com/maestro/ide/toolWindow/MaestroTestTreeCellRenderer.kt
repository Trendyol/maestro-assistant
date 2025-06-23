package com.maestro.ide.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.maestro.ide.service.model.TestStatus
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

/**
 * Custom cell renderer for the test tree
 */
class MaestroTestTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
        tree: JTree,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ) {
        if (value !is DefaultMutableTreeNode) return
        
        val userObject = value.userObject
        if (userObject !is TestTreeNode) {
            append(userObject.toString())
            return
        }

       val node = userObject
       
       if (node.isFile) {
           // This is a test file node
           val test = node.test
           if (test != null) {
               icon = test.status.icon
               val style = when (test.status) {
                   TestStatus.QUEUED -> SimpleTextAttributes.GRAY_ATTRIBUTES
                   else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
               }
               
               append(node.name, style)
           } else {
               // Shouldn't happen but handle just in case
               append(node.name)
               icon = AllIcons.FileTypes.Text
           }
       } else {
           // This is a directory node
           append(node.name)
           icon = AllIcons.Nodes.Folder
       }
   }
}