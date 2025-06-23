package com.maestro.lang.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import com.maestro.lang.schema.MaestroFileDetector
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Contributes reference providers for Maestro YAML files.
 */
class MaestroReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java)
                .with(object : PatternCondition<YAMLScalar>("isMaestroFile") {
                    override fun accepts(o: YAMLScalar, context: ProcessingContext?): Boolean {
                        val virtualFile = o.containingFile?.virtualFile ?: return false
                        return MaestroFileDetector.isMaestroFile(virtualFile)
                    }
                }),
            MaestroReferenceProvider()
        )
    }
}