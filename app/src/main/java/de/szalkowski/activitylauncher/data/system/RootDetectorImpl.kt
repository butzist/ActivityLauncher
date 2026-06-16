package de.szalkowski.activitylauncher.data.system

import de.szalkowski.activitylauncher.domain.system.RootDetector
import java.io.File
import javax.inject.Inject

class RootDetectorImpl @Inject constructor() : RootDetector {
    override fun detectSU(): Boolean {
        val dirs = System.getenv("PATH").orEmpty().split(":").map { dir -> File(dir, "su") }

        return dirs.any { path -> path.exists() && path.canExecute() && path.isFile }
    }
}
