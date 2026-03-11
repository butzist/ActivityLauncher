package de.szalkowski.activitylauncher.services

import java.io.File
import javax.inject.Inject

interface RootDetectionService {
    fun detectSU(): Boolean
}

class RootDetectionServiceImpl @Inject constructor() : RootDetectionService {
    override fun detectSU(): Boolean {
        val dirs = System.getenv("PATH").orEmpty().split(":").map { dir -> File(dir, "su") }

        return dirs.any { path -> path.exists() && path.canExecute() && path.isFile }
    }
}
