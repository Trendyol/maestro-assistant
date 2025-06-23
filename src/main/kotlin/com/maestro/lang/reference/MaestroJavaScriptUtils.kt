package com.maestro.lang.reference

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utility class for JavaScript file handling and parsing.
 * Contains common functionality shared between output references and value resolution.
 */
object MaestroJavaScriptUtils {
    private val LOG = Logger.getInstance(MaestroJavaScriptUtils::class.java)

    /**
     * Finds all runScript commands in the file text
     * Returns a list of script paths
     */
    fun findScriptPathsInFile(fileText: String): List<String> {
        val result = mutableListOf<String>()

        val scriptPatterns = listOf(
            "runScript:\\s*([^\\s\"']+)",  // No quotes
            "runScript:\\s*\"([^\"]+)\"",  // Double quotes
            "runScript:\\s*'([^']+)'"      // Single quotes
        )

        for (pattern in scriptPatterns) {
            val regex = Regex(pattern)
            val matches = regex.findAll(fileText)
            for (match in matches) {
                match.groups[1]?.value?.let { result.add(it) }
            }
        }

        return result
    }

    /**
     * Resolves a script path relative to the current directory
     * Handles "../" notation correctly
     */
    fun resolveScriptPath(currentDir: VirtualFile, scriptPath: String): VirtualFile? {
        var currentDirCopy = currentDir
        var remainingPath = scriptPath

        // Handle "../" parts at the beginning of the path
        while (remainingPath.startsWith("../")) {
            currentDirCopy = currentDirCopy.parent ?: return null
            remainingPath = remainingPath.substring(3)
        }

        // Handle "./" at the beginning
        if (remainingPath.startsWith("./")) {
            remainingPath = remainingPath.substring(2)
        }

        // Split the remaining path into parts
        val pathParts = remainingPath.split('/')

        // Navigate through the path parts
        var currentFile: VirtualFile = currentDirCopy
        for (part in pathParts) {
            if (part.isEmpty()) continue
            currentFile = currentFile.findChild(part) ?: return null
        }

        return currentFile
    }

    /**
     * Find all JavaScript files in a directory and its subdirectories
     */
    fun findJsFiles(dir: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()

        // Add JS files in current directory
        result.addAll(dir.children.filter { it.extension == "js" })

        // Search in subdirectories
        dir.children
            .filter { it.isDirectory }
            .forEach { result.addAll(findJsFiles(it)) }

        return result
    }

    /**
     * Find the top-level variable definition in the JavaScript file
     * Example: output.homepage = { ... }
     */
    fun findTopLevelDefinition(fileText: String, topLevelVar: String): MatchResult? {
        val patterns = listOf(
            "output\\.$topLevelVar\\s*=\\s*\\{",          // output.homepage = {
            "output\\[\"$topLevelVar\"\\]\\s*=\\s*\\{",   // output["homepage"] = {
            "output\\['$topLevelVar'\\]\\s*=\\s*\\{"      // output['homepage'] = {
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(fileText)
            if (match != null) {
                return match
            }
        }

        return null
    }

    /**
     * Finds a property pattern match in object content
     */
    fun findPropertyMatch(content: String, propertyName: String, suffix: String): MatchResult? {
        val patterns = listOf(
            "(\\b$propertyName)\\s*:$suffix",         // propertyName:
            "\"($propertyName)\"\\s*:$suffix",        // "propertyName":
            "'($propertyName)'\\s*:$suffix"           // 'propertyName':
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(content)
            if (match != null) {
                return match
            }
        }

        return null
    }

    /**
     * Extracts the content of a JavaScript object, handling nested braces correctly
     */
    fun extractObjectContent(text: String, openBracePos: Int): String {
        if (openBracePos >= text.length || text[openBracePos] != '{') {
            return ""
        }

        try {
            var openBraces = 1
            var closeBraces = 0
            var endPos = openBracePos + 1

            // Find the matching closing brace
            while (endPos < text.length && openBraces > closeBraces) {
                when (text[endPos]) {
                    '{' -> openBraces++
                    '}' -> closeBraces++
                }
                endPos++
            }

            if (openBraces != closeBraces) {
                return ""
            }

            // Extract the object content
            return text.substring(openBracePos, endPos)
        } catch (e: Exception) {
            LOG.debug("Error extracting object content", e)
            return ""
        }
    }

    /**
     * Navigate through nested object paths to find a specific property value
     * Example: from homepage, find homepage.section.accountTab
     */
    fun findNestedPropertyValue(
        fileText: String,
        pathParts: List<String>,
        topLevelMatch: MatchResult
    ): String? {
        // Start with the top level object
        val objectStartPos = fileText.indexOf('{', topLevelMatch.range.last)
        if (objectStartPos == -1) return null

        var currentContent = extractObjectContent(fileText, objectStartPos)
        var currentStartPos = objectStartPos

        // For paths with intermediate levels (e.g., homepage.section.accountTab),
        // navigate through each level except the last one
        for (i in 1 until pathParts.size - 1) {
            val intermediatePart = pathParts[i]

            // Find the intermediate property
            val intermMatch = findPropertyMatch(currentContent, intermediatePart, "\\{") ?: return null

            // Find the start of the nested object
            val nestedObjectStartPos = currentContent.indexOf('{', intermMatch.range.last - 1)
            if (nestedObjectStartPos == -1) return null

            // Extract the nested object content
            val absoluteNestedStartPos = currentStartPos + nestedObjectStartPos
            val nestedContent = extractObjectContent(fileText, absoluteNestedStartPos)

            // Update current object for the next iteration
            currentContent = nestedContent
            currentStartPos = absoluteNestedStartPos
        }

        // Now search for the final property in the current object
        val propertyName = pathParts.last()
        val propMatch = findPropertyMatch(currentContent, propertyName, "") ?: return null

        // Get the value after the colon
        val valueStart = currentContent.indexOf(':', propMatch.range.last) + 1
        if (valueStart == 0) return null

        // Find where the value ends (at comma, closing brace, or end of string)
        var valueEnd = valueStart
        var inString = false
        var stringDelimiter = '"'
        var braceCount = 0

        while (valueEnd < currentContent.length) {
            val char = currentContent[valueEnd]

            // Handle string literals
            if (char == '"' || char == '\'') {
                if (!inString) {
                    inString = true
                    stringDelimiter = char
                } else if (char == stringDelimiter && currentContent.getOrNull(valueEnd - 1) != '\\') {
                    inString = false
                }
            }

            // Count braces only when not in a string
            if (!inString) {
                when (char) {
                    '{' -> braceCount++
                    '}' -> {
                        if (braceCount == 0) {
                            break
                        }
                        braceCount--
                    }

                    ',' -> if (braceCount == 0) {
                        break
                    }
                }
            }

            valueEnd++
        }

        // Extract the value excluding leading/trailing whitespace
        return currentContent.substring(valueStart, valueEnd).trim()
    }

    /**
     * Formats a value for display in a hint
     * Shortens long strings, formats quotes properly, etc.
     */
    fun formatValueForHint(value: String?): String? {
        if (value == null) return null

        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        // Remove quotes from string literals
        val unquoted = when {
            (trimmed.startsWith("\"") && trimmed.endsWith("\"")) -> trimmed.substring(1, trimmed.length - 1)
            (trimmed.startsWith("'") && trimmed.endsWith("'")) -> trimmed.substring(1, trimmed.length - 1)
            else -> trimmed
        }

        // Truncate long values
        return if (unquoted.length > 30) {
            "${unquoted.take(27)}..."
        } else {
            unquoted
        }
    }
}