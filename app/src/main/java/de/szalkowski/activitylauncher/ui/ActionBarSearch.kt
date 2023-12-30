package de.szalkowski.activitylauncher.ui

interface ActionBarSearch {
    var actionBarSearchText: String
    var onActionBarSearchListener: ((String) -> Unit)?
}