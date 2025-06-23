package com.maestro.lang.reference

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import java.io.File

/**
 * Reference to a file from runScript, evalScript or runFlow command
 */
class MaestroFileReference(element: PsiElement, private val filePath: String) : PsiReference {
    private val myElement = element

    override fun getElement(): PsiElement = myElement

    override fun getRangeInElement(): TextRange = TextRange(0, element.textLength)

    override fun resolve(): PsiElement? {
        val project = element.project
        val dir = element.containingFile?.virtualFile?.parent ?: return null

        // Look for the file relative to the current file's directory
        // First try with exact path
        var targetFile = dir.findFileByRelativePath(filePath)

        // If not found, try with just the filename (searching in nearby directories)
        if (targetFile == null) {
            val fileName = filePath.substringAfterLast('/')
            targetFile = findFile(dir, fileName)
        }

        if (targetFile == null) return null
        return PsiManager.getInstance(project).findFile(targetFile)
    }

    private fun findFile(dir: VirtualFile, path: String): VirtualFile? {
        // First try direct path
        val file = dir.findFileByRelativePath(path)
        if (file != null) return file

        // Check if any file in this directory matches the name
        val fileName = path.substringAfterLast('/')
        val matchingFile = dir.children.firstOrNull { it.name == fileName }
        if (matchingFile != null) return matchingFile

        // Try searching in subdirectories
        return dir.children
            .filter { it.isDirectory }
            .firstNotNullOfOrNull { findFile(it, path) }
    }

    override fun getCanonicalText(): String = filePath

    override fun handleElementRename(newElementName: String): PsiElement = element

    override fun bindToElement(element: PsiElement): PsiElement = myElement

    override fun isReferenceTo(element: PsiElement): Boolean {
        if (element !is PsiFile) return false
        return element.name == File(filePath).name
    }

    override fun isSoft(): Boolean = true
}