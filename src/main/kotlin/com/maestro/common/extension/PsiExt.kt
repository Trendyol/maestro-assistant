package com.maestro.common.extension

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import com.maestro.ide.service.model.TestStatus

/**
 * Extension functions for IntelliJ PSI elements to simplify test status management.
 */

// User data key for storing test status in virtual files
private val TEST_STATUS_KEY = Key.create<TestStatus>("MAESTRO_TEST_STATUS")

/**
 * Gets the relative path of this PSI file from the project root.
 *
 * @return Relative path from project root, or null if virtual file is not available
 */
fun PsiFile.getRelativePath(): String? {
    val virtualFile = virtualFile ?: return null
    return project.getRelativePath(virtualFile.path)
}

/**
 * Associates a test status with this PSI file.
 *
 * The status is stored as user data in the virtual file and can be
 * retrieved later using getTestStatus().
 *
 * @param status The test status to associate with this file
 */
fun PsiFile.setTestStatus(status: TestStatus) {
    virtualFile?.putUserData(TEST_STATUS_KEY, status)
}

/**
 * Retrieves the test status associated with this PSI file.
 *
 * @return The test status, or null if no status has been set or virtual file is unavailable
 */
fun PsiFile.getTestStatus(): TestStatus? {
    return virtualFile?.getUserData(TEST_STATUS_KEY)
}
