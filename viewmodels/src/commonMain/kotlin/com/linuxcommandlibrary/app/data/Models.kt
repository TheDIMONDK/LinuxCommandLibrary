package com.linuxcommandlibrary.app.data

import com.linuxcommandlibrary.shared.TipSectionElement

data class BasicCategory(
    val id: String,
    val title: String,
)

data class BasicGroup(
    val id: Long,
    val description: String,
)

data class BasicCommand(
    val id: Long,
    val command: String,
    val mans: String,
)

data class CommandInfo(
    val id: Long,
    val name: String,
)

data class CommandSectionInfo(
    val id: Long,
    val title: String,
    val content: String,
    val parsedContent: List<TipSectionElement> = emptyList(),
)

fun CommandSectionInfo.getSortPriority(): Int = com.linuxcommandlibrary.shared.getSectionSortPriority(title)
