package com.linuxcommandlibrary.app.ui.screens.basicgroups

import com.linuxcommandlibrary.app.data.BasicCommand
import com.linuxcommandlibrary.app.data.BasicGroup

data class BasicGroupsUiState(
    val basicGroups: List<BasicGroup> = emptyList(),
    val collapsedMap: Map<Long, Boolean> = emptyMap(),
    val commandsByGroupId: Map<Long, List<BasicCommand>> = emptyMap(),
)
