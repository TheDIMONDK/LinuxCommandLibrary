package com.linuxcommandlibrary.app.ui.screens.commands

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.ui.composables.AppIcon
import com.linuxcommandlibrary.app.ui.composables.InlineSearchField
import com.linuxcommandlibrary.app.ui.composables.PaneTopBar
import com.linuxcommandlibrary.app.ui.composables.SearchOverlayBox
import com.linuxcommandlibrary.app.ui.composables.SearchState
import com.linuxcommandlibrary.app.ui.composables.rememberIconPainter
import com.linuxcommandlibrary.app.ui.screens.commanddetail.CommandDetailScreen
import com.linuxcommandlibrary.app.ui.screens.commanddetail.CommandDetailViewModel
import com.linuxcommandlibrary.app.ui.screens.commandlist.CommandListScreen
import com.linuxcommandlibrary.app.ui.screens.commandlist.CommandListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun CommandsPaneScreen(
    navigator: ThreePaneScaffoldNavigator<String>,
    searchState: SearchState,
    pendingSelection: String?,
    onSelectionConsumed: () -> Unit,
    scope: CoroutineScope,
    onNavigate: (NavEvent) -> Unit,
) {
    val listViewModel: CommandListViewModel = koinInject()

    LaunchedEffect(pendingSelection) {
        val name = pendingSelection ?: return@LaunchedEffect
        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, name)
        onSelectionConsumed()
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        listPane = {
            AnimatedPane(
                enterTransition = fadeIn(),
                exitTransition = fadeOut(),
            ) {
                // While a pendingSelection is being routed to the detail pane,
                // skip the list to avoid a one-frame flash of the search
                // overlay when crossing tabs from another search result.
                if (pendingSelection == null) {
                    CommandsListPane(
                        listViewModel = listViewModel,
                        searchState = searchState,
                        selectedName = navigator.currentDestination?.contentKey,
                        onNavigate = onNavigate,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane(
                enterTransition = fadeIn(),
                exitTransition = fadeOut(),
            ) {
                val selected = navigator.currentDestination?.contentKey
                if (selected == null) {
                    EmptyDetailPlaceholder("Select a command")
                } else {
                    val koinScope = currentKoinScope()
                    val detailViewModel = remember(selected, koinScope) {
                        koinScope.get<CommandDetailViewModel> { parametersOf(selected) }
                    }
                    CommandsDetailPane(
                        commandName = selected,
                        viewModel = detailViewModel,
                        onBack = { scope.launch { navigator.navigateBack() } },
                        onNavigate = onNavigate,
                    )
                }
            }
        },
    )
}

@Composable
private fun CommandsListPane(
    listViewModel: CommandListViewModel,
    searchState: SearchState,
    selectedName: String?,
    onNavigate: (NavEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        InlineSearchField(searchState = searchState, placeholder = "Search")
        SearchOverlayBox(searchState = searchState, onNavigate = onNavigate) {
            CommandListScreen(
                viewModel = listViewModel,
                onNavigate = onNavigate,
                selectedName = selectedName,
            )
        }
    }
}

@Composable
private fun CommandsDetailPane(
    commandName: String,
    viewModel: CommandDetailViewModel,
    onBack: () -> Unit,
    onNavigate: (NavEvent) -> Unit,
) {
    val uiState by viewModel.state.collectAsState()
    val isAllExpanded = uiState.isAllExpanded()
    val expandPainter = rememberIconPainter(
        if (isAllExpanded) AppIcon.COLLAPSE_ALL else AppIcon.EXPAND_ALL,
    )
    val bookmarkPainter = rememberIconPainter(
        if (uiState.isBookmarked) AppIcon.BOOKMARK else AppIcon.BOOKMARK_BORDER,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        PaneTopBar(
            title = commandName,
            onBack = onBack,
            actions = {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    onClick = { viewModel.onToggleAllExpanded() },
                ) {
                    Icon(
                        painter = expandPainter,
                        contentDescription = if (isAllExpanded) "Collapse all" else "Expand all",
                    )
                }
                IconButton(
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    onClick = {
                        if (uiState.isBookmarked) viewModel.removeBookmark() else viewModel.addBookmark()
                    },
                ) {
                    Icon(
                        painter = bookmarkPainter,
                        contentDescription = if (uiState.isBookmarked) "Remove bookmark" else "Add bookmark",
                    )
                }
            },
        )
        CommandDetailScreen(viewModel = viewModel, onNavigate = onNavigate)
    }
}

@Composable
private fun EmptyDetailPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
