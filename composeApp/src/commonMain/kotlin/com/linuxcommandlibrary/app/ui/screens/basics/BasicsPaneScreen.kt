package com.linuxcommandlibrary.app.ui.screens.basics

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.data.BasicsRepository
import com.linuxcommandlibrary.app.ui.composables.InlineSearchField
import com.linuxcommandlibrary.app.ui.composables.PaneTopBar
import com.linuxcommandlibrary.app.ui.composables.SearchOverlayBox
import com.linuxcommandlibrary.app.ui.composables.SearchState
import com.linuxcommandlibrary.app.ui.screens.basiccategories.BasicCategoriesScreen
import com.linuxcommandlibrary.app.ui.screens.basiccategories.BasicCategoriesViewModel
import com.linuxcommandlibrary.app.ui.screens.basicgroups.BasicEditorScreen
import com.linuxcommandlibrary.app.ui.screens.basicgroups.BasicEditorViewModel
import com.linuxcommandlibrary.app.ui.screens.basicgroups.BasicGroupsScreen
import com.linuxcommandlibrary.app.ui.screens.basicgroups.BasicGroupsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.currentKoinScope
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun BasicsPaneScreen(
    navigator: ThreePaneScaffoldNavigator<String>,
    searchState: SearchState,
    pendingSelection: String?,
    onSelectionConsumed: () -> Unit,
    pendingExpandGroupId: Long?,
    onExpandConsumed: () -> Unit,
    scope: CoroutineScope,
    onNavigate: (NavEvent) -> Unit,
) {
    val categoriesViewModel: BasicCategoriesViewModel = koinInject()
    val basicsRepository: BasicsRepository = koinInject()

    val categories by categoriesViewModel.basicCategories.collectAsState()
    var selectedTitle by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pendingSelection) {
        val id = pendingSelection ?: return@LaunchedEffect
        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id)
        onSelectionConsumed()
    }

    LaunchedEffect(navigator.currentDestination?.contentKey, categories) {
        val id = navigator.currentDestination?.contentKey
        if (id != null) {
            val match = categories.firstOrNull { it.id == id }
            if (match != null) selectedTitle = match.title
        }
    }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        listPane = {
            AnimatedPane(
                enterTransition = fadeIn(),
                exitTransition = fadeOut(),
            ) {
                if (pendingSelection == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        InlineSearchField(searchState = searchState, placeholder = "Search")
                        SearchOverlayBox(searchState = searchState, onNavigate = onNavigate) {
                            BasicCategoriesScreen(
                                viewModel = categoriesViewModel,
                                onNavigate = onNavigate,
                                selectedId = navigator.currentDestination?.contentKey,
                            )
                        }
                    }
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
                val selectedId = navigator.currentDestination?.contentKey
                if (selectedId == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Select a category",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val koinScope = currentKoinScope()
                    val usesCardLayout = basicsRepository.usesCardLayout(selectedId)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        PaneTopBar(
                            title = selectedTitle,
                            onBack = { scope.launch { navigator.navigateBack() } },
                        )
                        if (usesCardLayout) {
                            val editorViewModel = remember(selectedId, koinScope) {
                                koinScope.get<BasicEditorViewModel> { parametersOf(selectedId) }
                            }
                            BasicEditorScreen(viewModel = editorViewModel, onNavigate = onNavigate)
                            // Card-layout categories have no collapsible groups; drop the
                            // pending id so it doesn't trigger on a later non-card visit.
                            LaunchedEffect(pendingExpandGroupId, selectedId) {
                                if (pendingExpandGroupId != null) onExpandConsumed()
                            }
                        } else {
                            val groupsViewModel = remember(selectedId, koinScope) {
                                koinScope.get<BasicGroupsViewModel> { parametersOf(selectedId) }
                            }
                            BasicGroupsScreen(
                                viewModel = groupsViewModel,
                                onNavigate = onNavigate,
                                focusGroupId = pendingExpandGroupId,
                                onFocusConsumed = onExpandConsumed,
                            )
                        }
                    }
                }
            }
        },
    )
}
