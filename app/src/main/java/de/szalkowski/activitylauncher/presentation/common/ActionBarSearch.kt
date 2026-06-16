package de.szalkowski.activitylauncher.presentation.common

interface ActionBarSearch {
    var actionBarSearchText: String
    var onActionBarSearchListener: ((String) -> Unit)?
    var isSearching: Boolean
}
