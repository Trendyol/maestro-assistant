package com.maestro.lang

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider
import com.intellij.openapi.vfs.VirtualFile
import com.maestro.lang.schema.MaestroFileDetector
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.annotations.NotNull

/**
 * File type detector for Maestro test files.
 * This keeps the files as YAML type but marks them for special Maestro processing.
 */
class MaestroYamlFileTypeDetector : FileTypeOverrider {
    companion object {
        private val LOG = Logger.getInstance(MaestroYamlFileTypeDetector::class.java)
    }
    
    // ThreadLocal to prevent recursive calls
    private val isProcessing = ThreadLocal.withInitial { false }
    
    override fun getOverriddenFileType(@NotNull file: VirtualFile): FileType? {
        // Quick check for YAML extension without file content analysis
        val extension = file.extension?.lowercase()
        if (extension != "yaml" && extension != "yml") {
            return null
        }
        
        // Prevent recursion
        if (isProcessing.get()) {
            return null
        }
        
        try {
            isProcessing.set(true)
            
            // Only check valid files
            if (!file.isValid) {
                return null
            }
            
            // Fast path: Check folder first - if in Maestro folder, it's definitely a Maestro file
            if (MaestroFileDetector.isInMaestroFolder(file)) {
                return YAMLFileType.YML
            }
            
            // If it's a Maestro file based on content, keep it as YAML
            if (MaestroFileDetector.isMaestroFile(file)) {
                return YAMLFileType.YML
            }
        } catch (e: Exception) {
            LOG.debug("Error in Maestro file type detection: ${e.message}")
        } finally {
            isProcessing.set(false)
        }
        
        return null
    }
}