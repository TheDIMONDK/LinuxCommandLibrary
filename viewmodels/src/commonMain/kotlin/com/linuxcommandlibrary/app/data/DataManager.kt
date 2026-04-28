package com.linuxcommandlibrary.app.data

import com.linuxcommandlibrary.app.platform.defaultAutoExpandCommandSections
import com.linuxcommandlibrary.shared.platform.PreferencesStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DataManager(private val prefs: PreferencesStorage) {

    private val _bookmarkNames = MutableStateFlow(loadBookmarks())

    /**
     * Live set of bookmarked command names. Updated on every add/remove so any
     * collector (CommandListViewModel, CommandDetailViewModel, etc.) sees the
     * change without an explicit refresh call.
     */
    val bookmarkNames: StateFlow<Set<String>> = _bookmarkNames.asStateFlow()

    private fun loadBookmarks(): Set<String> {
        val bookmarksChain = prefs.getString(KEY_BOOKMARKS_V2, "")
        return bookmarksChain.split(",").filter { it.isNotBlank() }.toSet()
    }

    private fun saveBookmarkNames(names: Set<String>) {
        prefs.putString(KEY_BOOKMARKS_V2, names.joinToString(separator = ","))
    }

    fun addBookmark(name: String) {
        val updated = _bookmarkNames.value + name
        _bookmarkNames.value = updated
        saveBookmarkNames(updated)
    }

    fun removeBookmark(name: String) {
        val updated = _bookmarkNames.value - name
        _bookmarkNames.value = updated
        saveBookmarkNames(updated)
    }

    fun hasBookmark(name: String): Boolean = _bookmarkNames.value.contains(name)

    fun setAutoExpandSections(autoExpand: Boolean) {
        prefs.putBoolean(KEY_AUTO_EXPAND_SECTIONS, autoExpand)
    }

    fun isAutoExpandSections(): Boolean = prefs.getBoolean(KEY_AUTO_EXPAND_SECTIONS, defaultAutoExpandCommandSections)

    companion object {
        const val KEY_BOOKMARKS_V2 = "KEY_BOOKMARKS_V2"
        const val KEY_AUTO_EXPAND_SECTIONS = "auto_expand_sections"
    }
}
