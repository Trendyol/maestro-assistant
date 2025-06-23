package com.maestro.lang.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.maestro.lang.schema.MaestroFileDetector
import com.maestro.lang.syntax.MaestroSyntax
import org.jetbrains.yaml.psi.*
import java.util.regex.Pattern

/**
 * Provides syntax highlighting and error annotations for Maestro YAML files.
 * Also suppresses schema validation errors for command parts after "---" separator.
 */
class MaestroYamlAnnotator : Annotator {
    companion object {
        private val LOG = Logger.getInstance(MaestroYamlAnnotator::class.java)
        private val STRING_INTERPOLATION_PATTERN = Pattern.compile("\\$\\{([^}]*)}")
        private val OUTPUT_REFERENCE_PATTERN = Pattern.compile("\\$\\{output\\.[^}]*}")
        private val FILE_EXTENSION_PATTERN = Pattern.compile("\\.(js|yaml|yml)$")
    }

    fun AnnotationHolder.annotate(text: String, textRange: TextRange) {
        when (val syntax = MaestroSyntax.createFromValue(text)) {
            is MaestroSyntax.Command -> {
                newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .textAttributes(syntax.textAttributesKey)
                    .range(textRange)
                    .create()
            }

            MaestroSyntax.Dash, MaestroSyntax.TripleDash -> syntax.textAttributesKey
            MaestroSyntax.InvalidSyntax -> newAnnotation(HighlightSeverity.ERROR, "Unknown Maestro command: $text")
                .range(textRange)
                .create()
        }
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.containingFile?.virtualFile?.let {
            if (MaestroFileDetector.isMaestroFile(it)) it else null
        } ?: return

        runCatching {
            when (element) {
                is YAMLKeyValue -> {
                    onYamlKeyValue(holder, element)
                }

                is YAMLSequenceItem -> {
                    onYamlSequenceItem(holder, element)
                }

                is YAMLScalar -> {
                    onYamlScalar(holder, element)
                }

                else -> Unit
            }
        }.onFailure {
            LOG.error("Error annotating Maestro YAML file ${element.text}", it)
        }
    }

    private fun onYamlKeyValue(holder: AnnotationHolder, element: YAMLKeyValue) {
        val keyText = element.keyText
        val keyElement = element.key ?: return

        // Skip validation for key-values under commands that accept undefined sub-commands
        if (isUnderCommandThatAcceptsUndefinedSubcommands(element)) {
            return
        }

        // Check if this is a subcommand (not a root command) and validate against parent's allowed subcommands
        validateSubCommandForParent(element, holder)

        // Validate that required commands have at least one subcommand or value
        validateRequiredSubCommandsOrValues(element, holder)

        holder.annotate(keyText, keyElement.textRange)
    }

    private fun onYamlSequenceItem(holder: AnnotationHolder, element: YAMLSequenceItem) {
        element.text.length.takeIf { it > 1 } ?: return
        val text = element.text.substring(2)
        if (element.text.contains(":")) return
        // Skip validation for elements under commands that accept raw values
        if (isUnderCommandThatAcceptsUndefinedSubcommands(element)) {
            return
        }
        val textRange =
            TextRange(element.textRange.startOffset + 2, element.textRange.startOffset + text.lastIndex + 3)
        holder.annotate(text, textRange)
    }

    private fun onYamlScalar(holder: AnnotationHolder, element: YAMLScalar) {
        val scalarText = element.textValue
        // Don't process empty values
        if (scalarText.isBlank()) return

        // Check if this scalar is a value for a command that accepts file references
        val parent = element.parent
        if (parent is YAMLKeyValue) {
            val commandText = parent.keyText
            val command = MaestroSyntax.createFromValue(commandText)

            // Handle file references
            if (command is MaestroSyntax.Command && command.acceptsFileReferences && FILE_EXTENSION_PATTERN.matcher(
                    scalarText
                ).find()
            ) {
                val fileRefRange = TextRange(element.textRange.startOffset, element.textRange.endOffset)
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .textAttributes(MaestroSyntax.FILE_REFERENCE_KEY)
                    .range(fileRefRange)
                    .create()
                return
            }
        }

        // Handle string interpolation
        val matcher = STRING_INTERPOLATION_PATTERN.matcher(scalarText)
        while (matcher.find()) {
            val interpolationStart = element.textRange.startOffset + matcher.start()
            val interpolationEnd = element.textRange.startOffset + matcher.end()
            val interpolationRange = TextRange(interpolationStart, interpolationEnd)

            // Check if this is an output reference
            val interpolationText = matcher.group()
            val attributesKey = if (OUTPUT_REFERENCE_PATTERN.matcher(interpolationText).matches()) {
                MaestroSyntax.OUTPUT_REFERENCE_KEY
            } else {
                MaestroSyntax.STRING_INTERPOLATION_KEY
            }

            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .textAttributes(attributesKey)
                .range(interpolationRange)
                .create()
        }
    }

    /**
     * Checks if the element is under a command that accepts undefined subcommands.
     * This applies to both YAMLKeyValue and YAMLSequenceItem elements.
     */
    private fun isUnderCommandThatAcceptsUndefinedSubcommands(element: PsiElement): Boolean {
        when (element) {
            // For sequence items, check its parent sequence
            is YAMLSequenceItem -> {
                val parent = element.parent?.parent
                if (parent is YAMLKeyValue) {
                    val commandText = parent.keyText
                    val command = MaestroSyntax.createFromValue(commandText)
                    if (command is MaestroSyntax.Command && command.acceptUndefinedSubCommands) {
                        return true
                    }
                }
            }

            // For key-value pairs, check both direct parent and sequence parent
            is YAMLKeyValue -> {
                // Check direct parent command
                val directParent = element.parent?.parent
                if (directParent is YAMLKeyValue) {
                    val parentCommandText = directParent.keyText
                    val parentCommand = MaestroSyntax.createFromValue(parentCommandText)
                    if (parentCommand is MaestroSyntax.Command && parentCommand.acceptUndefinedSubCommands) {
                        return true
                    }
                }

                // Check if this key-value is in a mapping inside a sequence item
                val sequenceItemParent = element.parent?.parent
                if (sequenceItemParent is YAMLSequenceItem) {
                    val sequenceParent = sequenceItemParent.parent?.parent
                    if (sequenceParent is YAMLKeyValue) {
                        val commandText = sequenceParent.keyText
                        val command = MaestroSyntax.createFromValue(commandText)
                        if (command is MaestroSyntax.Command && command.acceptUndefinedSubCommands) {
                            return true
                        }
                    }
                }
            }

            // For any other element types, default to false
            else -> return false
        }

        return false
    }

    /**
     * Validates if a subcommand is allowed for its parent command
     * This checks against the allowedSubCommands property of the parent command
     */
    private fun validateSubCommandForParent(element: YAMLKeyValue, holder: AnnotationHolder) {
        val keyText = element.keyText
        val keyElement = element.key ?: return

        // Find the parent command
        val parent = element.parent?.parent
        if (parent is YAMLKeyValue) {
            val parentCommandText = parent.keyText
            val parentCommand = MaestroSyntax.createFromValue(parentCommandText)

            // Check if this is a subcommand that's not allowed by the parent
            if (parentCommand is MaestroSyntax.Command &&
                !parentCommand.acceptUndefinedSubCommands &&
                parentCommand.allowedSubCommands.isNotEmpty()
            ) {

                val isAllowed = parentCommand.allowedSubCommands.any {
                    it.key == keyText
                }

                if (!isAllowed) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Subcommand '$keyText' is not allowed for '${parentCommand.key}'. " +
                                "Allowed subcommands: ${parentCommand.allowedSubCommands.joinToString(", ") { it.key }}"
                    )
                        .range(keyElement.textRange)
                        .create()
                }
            }
        }

        // Also check for subcommands within sequence items
        val sequenceParent = element.parent?.parent
        if (sequenceParent is YAMLSequenceItem) {
            val sequenceParentCommand = sequenceParent.parent?.parent
            if (sequenceParentCommand is YAMLKeyValue) {
                val parentCommandText = sequenceParentCommand.keyText
                val parentCommand = MaestroSyntax.createFromValue(parentCommandText)

                if (parentCommand is MaestroSyntax.Command &&
                    !parentCommand.acceptUndefinedSubCommands &&
                    parentCommand.allowedSubCommands.isNotEmpty()
                ) {

                    val isAllowed = parentCommand.allowedSubCommands.any {
                        it.key == keyText
                    }

                    if (!isAllowed) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            "Subcommand '$keyText' is not allowed for '${parentCommand.key}'. " +
                                    "Allowed subcommands: ${parentCommand.allowedSubCommands.joinToString(", ") { it.key }}"
                        )
                            .range(keyElement.textRange)
                            .create()
                    }
                }
            }
        }
    }

    /**
     * Validates that commands which require at least one subcommand or a value have one
     */
    private fun validateRequiredSubCommandsOrValues(element: YAMLKeyValue, holder: AnnotationHolder) {
        val keyText = element.keyText
        val command = MaestroSyntax.createFromValue(keyText)

        if (command !is MaestroSyntax.Command) return
        val requiredSubCommandOrValue = command.allowedSubCommands.isNotEmpty() || command.acceptRawValue
        // Only check commands that explicitly require subcommands or values
        if (requiredSubCommandOrValue) {
            val keyElement = element.key ?: return
            val value = element.value

            // Check if the command has a value
            val hasValue = when {
                // Direct scalar value
                value is YAMLScalar && value.textValue.isNotBlank() -> true
                // Mapping with subcommands
                value is YAMLMapping && value.keyValues.isNotEmpty() -> true
                value is YAMLSequence && value.items.isNotEmpty() -> true
                // Otherwise no value
                else -> false
            }

            if (!hasValue) {
                val recommended = when {
                    command.acceptsFileReferences -> {
                        if (command.allowedSubCommands.any { it == MaestroSyntax.Command.File }) {
                            "Add 'file: path/to/file.yml' or other allowed subcommands"
                        } else {
                            "Add a file path (e.g. 'flows/my-flow.yml') or other allowed subcommands"
                        }
                    }

                    (command.allowedSubCommands).isNotEmpty() -> {
                        "Add one of: " + command.allowedSubCommands.joinToString(", ") { it.key }
                    }

                    command.acceptRawValue -> {
                        "Add required value or subcommand"
                    }

                    else -> {
                        "Add a subcommand"
                    }
                }

                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Command '$keyText' requires at least one subcommand or value. $recommended"
                )
                    .range(element.textRange)
                    .create()
            }
        } else if (element.value == null) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                "Command '$keyText' does not accept semicolon"
            )
                .range(element.textRange)
                .create()

        }
    }
}