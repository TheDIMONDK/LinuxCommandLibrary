package com.linuxcommandlibrary.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.data.BasicGroup
import com.linuxcommandlibrary.app.data.BasicGroupMatch
import com.linuxcommandlibrary.app.ui.composables.HighlightedText
import com.linuxcommandlibrary.app.ui.composables.WithScrollbar
import com.linuxcommandlibrary.app.ui.composables.debouncedClickable
import com.linuxcommandlibrary.app.ui.composables.getIconId
import com.linuxcommandlibrary.app.ui.composables.rememberIconPainter
import com.linuxcommandlibrary.app.ui.screens.commandlist.CommandListItem

@Composable
fun SearchScreen(
    searchText: String,
    viewModel: SearchViewModel,
    onNavigate: (NavEvent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(searchText) {
        viewModel.search(searchText)
    }

    SearchContent(
        uiState = uiState,
        searchText = searchText,
        onNavigate = onNavigate,
    )
}

@Composable
fun SearchContent(
    uiState: SearchUiState,
    searchText: String,
    onNavigate: (NavEvent) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val showEmptyMessage = uiState.filteredCommands.isEmpty() && uiState.filteredBasicGroups.isEmpty()

    if (searchText.isNotEmpty() && showEmptyMessage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false, onClick = {})
                .background(MaterialTheme.colorScheme.background),
        ) {
            Text("404 command not found", modifier = Modifier.align(Alignment.Center))
        }
    } else {
        WithScrollbar(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
            ) {
                if (uiState.filteredCommands.isNotEmpty()) {
                    item(key = "header_commands", contentType = "section_header") {
                        SectionHeader("Commands (${uiState.filteredCommands.size})")
                    }
                    items(
                        items = uiState.filteredCommands,
                        key = { "command_${it.id}" },
                        contentType = { "search_command_item" },
                    ) { command ->
                        CommandListItem(
                            command = command,
                            searchText = searchText,
                            onNavigate = onNavigate,
                            isBookmarked = false,
                        )
                    }
                }
                if (uiState.filteredBasicGroups.isNotEmpty()) {
                    item(key = "header_basics", contentType = "section_header") {
                        SectionHeader("Basics (${uiState.filteredBasicGroups.size})")
                    }
                    items(
                        items = uiState.filteredBasicGroups,
                        key = { "basic_${it.groupId}" },
                        contentType = { "search_basic_item" },
                    ) { match ->
                        BasicGroupSearchItem(
                            match = match,
                            searchText = searchText,
                            onNavigate = onNavigate,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun BasicGroupSearchItem(
    match: BasicGroupMatch,
    searchText: String,
    onNavigate: (NavEvent) -> Unit,
) {
    val iconPainter = rememberIconPainter(
        BasicGroup(id = match.groupId, description = match.description).getIconId(),
    )
    ListItem(
        headlineContent = {
            HighlightedText(
                text = match.description,
                pattern = searchText,
            )
        },
        supportingContent = {
            Text(
                text = match.categoryTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        },
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .debouncedClickable {
                onNavigate(
                    NavEvent.ToBasicGroups(
                        categoryId = match.categoryId,
                        expandGroupId = match.groupId,
                    ),
                )
            },
    )
}
