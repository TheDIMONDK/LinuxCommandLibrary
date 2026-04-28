package com.linuxcommandlibrary.app.ui.screens.commanddetail

import com.linuxcommandlibrary.app.data.CommandSectionInfo

data class CommandDetailUiState(
    val sections: List<CommandSectionInfo> = emptyList(),
    val expandedSectionsMap: Map<Long, Boolean> = emptyMap(),
    val isBookmarked: Boolean = false,
    val showBookmarkDialog: Boolean = false,
    val seeAlsoCommands: List<String> = emptyList(),
) {
    fun isAllExpanded(): Boolean = expandedSectionsMap.all { it.value }
}
