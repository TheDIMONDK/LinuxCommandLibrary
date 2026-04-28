package com.linuxcommandlibrary.app.ui.screens.basicgroups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.data.BasicCommand
import com.linuxcommandlibrary.app.data.BasicGroup
import com.linuxcommandlibrary.app.ui.composables.CommandView
import com.linuxcommandlibrary.app.ui.composables.WithScrollbar
import com.linuxcommandlibrary.app.ui.composables.getIconId
import com.linuxcommandlibrary.app.ui.composables.rememberIconPainter
import com.linuxcommandlibrary.shared.getCommandList

@Composable
fun BasicGroupsScreen(
    viewModel: BasicGroupsViewModel,
    onNavigate: (NavEvent) -> Unit = {},
    focusGroupId: Long? = null,
    onFocusConsumed: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val toggleCollapse = remember(viewModel) { viewModel::toggleCollapse }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.basicGroups, focusGroupId) {
        val gid = focusGroupId ?: return@LaunchedEffect
        if (uiState.basicGroups.isEmpty()) return@LaunchedEffect
        val index = uiState.basicGroups.indexOfFirst { it.id == gid }
        if (index >= 0) {
            viewModel.expand(gid)
            listState.animateScrollToItem(index)
            onFocusConsumed()
        }
    }

    BasicGroupsContent(
        uiState = uiState,
        listState = listState,
        toggleCollapse = toggleCollapse,
        onNavigate = onNavigate,
    )
}

@Composable
fun BasicGroupsContent(
    uiState: BasicGroupsUiState,
    listState: LazyListState,
    toggleCollapse: (Long) -> Unit,
    onNavigate: (NavEvent) -> Unit,
) {
    SelectionContainer {
        WithScrollbar(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = uiState.basicGroups,
                    key = { it.id },
                    contentType = { "basic_group_item" },
                ) { basicGroup ->
                    BasicGroupColumn(
                        basicGroup = basicGroup,
                        commands = uiState.commandsByGroupId[basicGroup.id] ?: emptyList(),
                        isExpanded = !(uiState.collapsedMap[basicGroup.id] ?: true),
                        onToggleCollapse = { toggleCollapse(basicGroup.id) },
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

@Composable
fun BasicGroupColumn(
    basicGroup: BasicGroup,
    commands: List<BasicCommand> = emptyList(),
    isExpanded: Boolean,
    onToggleCollapse: () -> Unit,
    onNavigate: (NavEvent) -> Unit = {},
) {
    val painter = rememberIconPainter(basicGroup.getIconId())

    ListItem(
        headlineContent = {
            Text(
                text = basicGroup.description,
                maxLines = 3,
            )
        },
        leadingContent = {
            Icon(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        },
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { onToggleCollapse() },
    )

    if (isExpanded) {
        ExpandedGroupContent(
            commands = commands,
            onNavigate = onNavigate,
        )
    }
}

@Composable
private fun ExpandedGroupContent(
    commands: List<BasicCommand>,
    onNavigate: (NavEvent) -> Unit,
) {
    commands.forEach { basicCommand ->
        val elements = remember(basicCommand.command, basicCommand.mans) {
            basicCommand.command.getCommandList(basicCommand.mans)
        }
        CommandView(
            command = basicCommand.command,
            elements = elements,
            onNavigate = onNavigate,
        )
    }
}
