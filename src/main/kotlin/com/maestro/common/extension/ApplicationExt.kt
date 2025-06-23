package com.maestro.common.extension

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState

/**
 * Invokes a function on the EDT
 */
fun invokeLater(modalityState: ModalityState = ModalityState.defaultModalityState(), action: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(action, modalityState)
}