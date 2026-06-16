package de.szalkowski.activitylauncher.domain.system

interface RootDetector {
    fun detectSU(): Boolean
}
