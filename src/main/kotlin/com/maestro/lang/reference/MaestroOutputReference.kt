package com.maestro.lang.reference

import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.FakePsiElement
import com.maestro.common.extension.invokeLater

/**
 * Reference to an output variable like ${output.accountText.surveyWebView}
 */
class MaestroOutputReference(
    element: PsiElement,
    private val rangeInElement: TextRange,
    private val variablePath: String
) : PsiReference {
    private val myElement = element

    override fun getElement(): PsiElement = myElement

    override fun getRangeInElement(): TextRange = rangeInElement

    /**
     * Main method to resolve a reference to its declaration
     * For example ${output.homepage.accountTab} → find accountTab in the JS file
     */
    override fun resolve(): PsiElement? {
        val project = element.project
        val currentFile = element.containingFile?.virtualFile ?: return null
        val currentDir = currentFile.parent ?: return null

        // Get the script paths from runScript directives if available
        val psiFile = PsiManager.getInstance(project).findFile(currentFile)
        val scriptPaths = psiFile?.let { MaestroJavaScriptUtils.findScriptPathsInFile(it.text) } ?: emptyList()

        // Find JavaScript files based on the script paths + general directory search
        val jsFiles = if (scriptPaths.isNotEmpty()) {
            scriptPaths.mapNotNull { MaestroJavaScriptUtils.resolveScriptPath(currentDir, it) } +
                    MaestroJavaScriptUtils.findJsFiles(currentDir)
        } else {
            MaestroJavaScriptUtils.findJsFiles(currentDir)
        }

        // Extract parts of variable path (e.g., ["homepage", "accountTab"])
        val pathParts = variablePath.split('.')
        val topLevelVar = pathParts.firstOrNull() ?: return null

        // For each JS file, search for the definition pattern
        for (jsFile in jsFiles) {
            try {
                val psiFile = PsiManager.getInstance(project).findFile(jsFile) ?: continue
                val fileText = psiFile.text

                // Find the top-level variable definition
                val topLevelMatch = MaestroJavaScriptUtils.findTopLevelDefinition(fileText, topLevelVar) ?: continue

                // If we only have the top-level variable, return that element
                if (pathParts.size == 1) {
                    return createPreciseElement(psiFile, topLevelMatch.range.first, topLevelVar.length)
                }

                // Navigate through nested object paths to find the exact property
                return findNestedProperty(psiFile, fileText, pathParts, topLevelMatch)
                    ?: createPreciseElement(psiFile, topLevelMatch.range.first, topLevelVar.length) // Fallback
            } catch (e: Exception) {
                // If anything goes wrong, continue to the next file
                continue
            }
        }

        return null
    }

    /**
     * Navigate through nested object paths to find a specific property
     * Example: from homepage, find homepage.section.accountTab
     */
    private fun findNestedProperty(
        psiFile: PsiElement,
        fileText: String,
        pathParts: List<String>,
        topLevelMatch: MatchResult
    ): PsiElement? {
        // Start with the top level object
        val objectStartPos = fileText.indexOf('{', topLevelMatch.range.last)
        if (objectStartPos == -1) return null

        var currentContent = MaestroJavaScriptUtils.extractObjectContent(fileText, objectStartPos)
        var currentStartPos = objectStartPos

        // For paths with intermediate levels (e.g., homepage.section.accountTab),
        // navigate through each level except the last one
        for (i in 1 until pathParts.size - 1) {
            val intermediatePart = pathParts[i]

            // Find the intermediate property
            val intermMatch = MaestroJavaScriptUtils.findPropertyMatch(currentContent, intermediatePart, "\\{") ?: break

            // Find the start of the nested object
            val nestedObjectStartPos = currentContent.indexOf('{', intermMatch.range.last - 1)
            if (nestedObjectStartPos == -1) break

            // Extract the nested object content
            val absoluteNestedStartPos = currentStartPos + nestedObjectStartPos
            val nestedContent = MaestroJavaScriptUtils.extractObjectContent(fileText, absoluteNestedStartPos)

            // Update current object for the next iteration
            currentContent = nestedContent
            currentStartPos = absoluteNestedStartPos
        }

        // Now search for the final property in the current object
        val propertyName = pathParts.last()
        val propMatch = MaestroJavaScriptUtils.findPropertyMatch(currentContent, propertyName, "") ?: return null

        // Get the position of the captured group (the property name itself)
        val captureGroup = propMatch.groups[1]
        val propPosition = if (captureGroup != null) {
            currentStartPos + captureGroup.range.first
        } else {
            currentStartPos + propMatch.range.first
        }

        // Create a precise element with position exactly at the property name
        return createPreciseElement(psiFile, propPosition, propertyName.length)
    }

    /**
     * Creates a precise PsiElement at the specified position in the file
     */
    private fun createPreciseElement(psiFile: PsiElement, offset: Int, length: Int): PsiElement {
        return object : FakePsiElement(), NavigationItem {
            override fun getTextRange(): TextRange = TextRange.from(offset, length)
            override fun getParent(): PsiElement = psiFile
            override fun getContainingFile() = psiFile.containingFile
            override fun getProject() = psiFile.project
            override fun getText(): String = psiFile.containingFile.text.substring(offset, offset + length)

            override fun getName(): String? = null

            override fun navigate(requestFocus: Boolean) {
                val file = psiFile.containingFile.virtualFile
                invokeLater {
                    val project = psiFile.project
                    val descriptor = OpenFileDescriptor(project, file, offset)
                    // Only navigate when explicitly requested (e.g., Cmd+B)
                    if (requestFocus) {
                        descriptor.navigate(true)
                    }
                }
            }

            override fun canNavigate(): Boolean = true

            override fun canNavigateToSource(): Boolean = true
        }
    }

    override fun getCanonicalText(): String = variablePath

    override fun handleElementRename(newElementName: String): PsiElement = element

    override fun bindToElement(element: PsiElement): PsiElement = myElement

    override fun isReferenceTo(element: PsiElement): Boolean = false

    override fun isSoft(): Boolean = true
}