package com.linuxcommandlibrary.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linuxcommandlibrary.app.data.CommandsRepository
import com.linuxcommandlibrary.app.platform.AppNavHost
import com.linuxcommandlibrary.app.platform.rememberOpenAppAction
import com.linuxcommandlibrary.app.ui.AppIcons
import com.linuxcommandlibrary.app.ui.composables.AppIcon
import com.linuxcommandlibrary.app.ui.composables.PaneTopBar
import com.linuxcommandlibrary.app.ui.composables.rememberIconPainter
import com.linuxcommandlibrary.app.ui.composables.rememberSearchState
import com.linuxcommandlibrary.app.ui.screens.AppInfoDialog
import com.linuxcommandlibrary.app.ui.screens.basicgroups.BasicGroupDetailPane
import com.linuxcommandlibrary.app.ui.screens.basics.BasicsPaneScreen
import com.linuxcommandlibrary.app.ui.screens.commanddetail.CommandDetailPane
import com.linuxcommandlibrary.app.ui.screens.commands.CommandsPaneScreen
import com.linuxcommandlibrary.app.ui.screens.tips.TipsScreen
import com.linuxcommandlibrary.app.ui.screens.tips.TipsViewModel
import com.linuxcommandlibrary.app.ui.theme.LinuxTheme
import com.linuxcommandlibrary.app.ui.theme.LocalCustomColors
import com.linuxcommandlibrary.shared.platform.ReviewHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private sealed class InitialSelection {
    data class Command(val name: String) : InitialSelection()
    data class Basics(val id: String, val title: String) : InitialSelection()
    data class SearchQuery(val query: String) : InitialSelection()
}

private data class DeeplinkResult(val route: Route, val selection: InitialSelection?)

internal sealed class TabStackEntry {
    internal data class Command(val name: String) : TabStackEntry()
    internal data class BasicGroup(val categoryId: String, val expandGroupId: Long?) : TabStackEntry()
}

// '|'-separated encoding relies on slugs never containing '|'.
internal fun TabStackEntry.encode(): String = when (this) {
    is TabStackEntry.Command -> "c|$name"
    is TabStackEntry.BasicGroup -> "b|$categoryId|${expandGroupId ?: ""}"
}

internal fun decodeTabStackEntry(s: String): TabStackEntry {
    val parts = s.split('|')
    return when (parts[0]) {
        "c" -> TabStackEntry.Command(parts[1])
        "b" -> TabStackEntry.BasicGroup(
            parts[1],
            parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.toLongOrNull(),
        )
        else -> error("invalid tab stack entry: $s")
    }
}

@Composable
fun App(
    initialDeeplink: String? = null,
    darkMode: Boolean = isSystemInDarkTheme(),
) {
    val reviewHandler: ReviewHandler = koinInject()
    val commandsRepository: CommandsRepository = koinInject()
    LaunchedEffect(Unit) {
        reviewHandler.incrementAppStartCount()
        reviewHandler.requestReviewIfNeeded()
        withContext(Dispatchers.Default) {
            commandsRepository.getCommands()
        }
    }

    LinuxTheme(darkMode = darkMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            LinuxApp(initialDeeplink = initialDeeplink)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalComposeUiApi::class)
@Composable
fun LinuxApp(initialDeeplink: String? = null) {
    val navController = rememberNavController()
    val deeplinkResult = remember(initialDeeplink) {
        parseDeeplink(initialDeeplink) ?: DeeplinkResult(Route.Basics, null)
    }
    val initialRoute = deeplinkResult.route

    val initialSearchQuery = (deeplinkResult.selection as? InitialSelection.SearchQuery)?.query.orEmpty()
    val searchState = rememberSearchState(initialText = initialSearchQuery)
    val openAppAction = rememberOpenAppAction()
    val scope = rememberCoroutineScope()

    // Initialize navigators with the deep-linked detail pane up-front so we don't flash the
    // list pane for one frame before navigating; this also makes the first composition
    // render the final UI, which screenshot tooling depends on.
    val initialCommandName = (deeplinkResult.selection as? InitialSelection.Command)?.name
    val initialBasicId = (deeplinkResult.selection as? InitialSelection.Basics)?.id
    val commandsNavigator = rememberListDetailPaneScaffoldNavigator(
        initialDestinationHistory = if (initialCommandName != null) {
            listOf(
                ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null),
                ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, initialCommandName),
            )
        } else {
            listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null))
        },
    )
    val basicsNavigator = rememberListDetailPaneScaffoldNavigator(
        initialDestinationHistory = if (initialBasicId != null) {
            listOf(
                ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null),
                ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, initialBasicId),
            )
        } else {
            listOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null))
        },
    )
    var pendingCommandSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBasicSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExpandGroupId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Survives consume/clear of pendingExpandGroupId so the search overlay can still
    // highlight the matching result on the basicsNavigator path.
    var lastBasicsGroupId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Per-tab cross-type detail stack: opening a different-type detail (e.g. a command
    // from the basics tab) layers on top of the originating tab so back returns to the
    // previous screen of that tab. Wide layouts prefer tab-switching, so stacks stay empty there.
    val tabStackSaver = remember {
        listSaver<SnapshotStateList<TabStackEntry>, String>(
            save = { it.map { entry -> entry.encode() } },
            restore = { saved ->
                mutableStateListOf<TabStackEntry>().apply { addAll(saved.map { decodeTabStackEntry(it) }) }
            },
        )
    }
    val tipsStack = rememberSaveable(saver = tabStackSaver) { mutableStateListOf<TabStackEntry>() }
    val basicsStack = rememberSaveable(saver = tabStackSaver) { mutableStateListOf<TabStackEntry>() }
    val commandsStack = rememberSaveable(saver = tabStackSaver) { mutableStateListOf<TabStackEntry>() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination
    // Before the NavHost processes startDestination the backstack is empty, which would
    // leave every tab unselected on the first frame (visible to screenshot tooling and as
    // a brief flash on launch). Fall back to initialRoute until the NavController catches up.
    val isOnTips = currentRoute?.hasRoute<Route.Tips>() ?: (initialRoute is Route.Tips)
    val isOnCommands = currentRoute?.hasRoute<Route.Commands>() ?: (initialRoute is Route.Commands)
    val isOnBasics = currentRoute?.hasRoute<Route.Basics>() ?: (initialRoute is Route.Basics)

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    val isWideLayout = layoutType != NavigationSuiteType.NavigationBar

    // First-level detail in the originating tab routes through that tab's navigator
    // (via pending* state); once a cross-type entry is on the stack, further details of
    // either type layer on top via the stack so chained "see also" stays in the tab.
    val onNavigate: (NavEvent) -> Unit = { event ->
        when (event) {
            is NavEvent.ToCommand -> when {
                isOnCommands -> if (commandsStack.isEmpty()) {
                    pendingCommandSelection = event.commandName
                } else {
                    commandsStack.add(TabStackEntry.Command(event.commandName))
                }
                isOnBasics -> basicsStack.add(TabStackEntry.Command(event.commandName))
                isOnTips -> tipsStack.add(TabStackEntry.Command(event.commandName))
                // currentRoute not yet resolved — race during init.
                else -> {
                    pendingCommandSelection = event.commandName
                    if (currentRoute?.hasRoute<Route.Commands>() != true) {
                        navController.navigate(Route.Commands) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }

            is NavEvent.ToBasicGroups -> {
                lastBasicsGroupId = event.expandGroupId
                when {
                    isOnBasics -> if (basicsStack.isEmpty()) {
                        pendingBasicSelection = event.categoryId
                        pendingExpandGroupId = event.expandGroupId
                    } else {
                        basicsStack.add(TabStackEntry.BasicGroup(event.categoryId, event.expandGroupId))
                    }
                    isOnTips -> tipsStack.add(
                        TabStackEntry.BasicGroup(event.categoryId, event.expandGroupId),
                    )
                    isOnCommands -> commandsStack.add(
                        TabStackEntry.BasicGroup(event.categoryId, event.expandGroupId),
                    )
                    // currentRoute not yet resolved — race during init.
                    else -> {
                        pendingBasicSelection = event.categoryId
                        pendingExpandGroupId = event.expandGroupId
                        if (currentRoute?.hasRoute<Route.Basics>() != true) {
                            navController.navigate(Route.Basics) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }

            is NavEvent.OpenAction -> openAppAction(event.action)
        }
    }

    // PopUntilContentChange so chained see-also detail screens pop one at a time;
    // the default PopUntilScaffoldValueChange treats Detail("ls") and Detail("rm")
    // as the same scaffold value and pops both together.
    val backBehavior = BackNavigationBehavior.PopUntilContentChange
    val tipsStackBack = isOnTips && tipsStack.isNotEmpty()
    val basicsStackBack = isOnBasics && basicsStack.isNotEmpty()
    val commandsStackBack = isOnCommands && commandsStack.isNotEmpty()
    val basicsNavBack = isOnBasics && basicsStack.isEmpty() && basicsNavigator.canNavigateBack(backBehavior)
    val commandsNavBack = isOnCommands && commandsStack.isEmpty() && commandsNavigator.canNavigateBack(backBehavior)
    BackHandler(
        enabled = tipsStackBack || basicsStackBack || commandsStackBack || basicsNavBack || commandsNavBack,
    ) {
        when {
            tipsStackBack -> tipsStack.removeAt(tipsStack.lastIndex)
            basicsStackBack -> basicsStack.removeAt(basicsStack.lastIndex)
            commandsStackBack -> commandsStack.removeAt(commandsStack.lastIndex)
            basicsNavBack -> scope.launch { basicsNavigator.navigateBack(backBehavior) }
            commandsNavBack -> scope.launch { commandsNavigator.navigateBack(backBehavior) }
        }
    }

    val navBarBackground = LocalCustomColors.current.navBarBackground
    val ambientColor = MaterialTheme.colorScheme.surfaceContainer

    val onSelectTab: (Route) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        searchState.clear()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(ambientColor)
            .then(
                if (isWideLayout) {
                    Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        val hoverModifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        val itemColors = NavigationItemColors(
            selectedIconColor = MaterialTheme.colorScheme.onSurface,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            selectedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        )
        NavigationSuiteScaffold(
            navigationSuiteType = layoutType,
            containerColor = Color.Transparent,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = navBarBackground,
                navigationRailContainerColor = Color.Transparent,
            ),
            navigationItemVerticalArrangement = Arrangement.Center,
            navigationItems = {
                NavigationSuiteItem(
                    modifier = hoverModifier,
                    navigationSuiteType = layoutType,
                    selected = isOnBasics,
                    onClick = { onSelectTab(Route.Basics) },
                    icon = {
                        Icon(
                            painter = rememberIconPainter(AppIcon.PUZZLE),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("Basics") },
                    colors = itemColors,
                )
                NavigationSuiteItem(
                    modifier = hoverModifier,
                    navigationSuiteType = layoutType,
                    selected = isOnTips,
                    onClick = { onSelectTab(Route.Tips) },
                    icon = {
                        Icon(
                            painter = rememberIconPainter(AppIcon.IDEA),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("Tips") },
                    colors = itemColors,
                )
                NavigationSuiteItem(
                    modifier = hoverModifier,
                    navigationSuiteType = layoutType,
                    selected = isOnCommands,
                    onClick = { onSelectTab(Route.Commands) },
                    icon = {
                        Icon(
                            painter = rememberIconPainter(AppIcon.SEARCH),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text("Commands") },
                    colors = itemColors,
                )
            },
        ) {
            val contentModifier = if (isWideLayout) {
                Modifier
                    .padding(start = 16.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            } else {
                Modifier
            }
            Box(
                modifier = contentModifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                AppNavHost(
                    navController = navController,
                    startDestination = initialRoute,
                ) {
                    composable<Route.Basics> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            BasicsPaneScreen(
                                navigator = basicsNavigator,
                                searchState = searchState,
                                pendingSelection = pendingBasicSelection,
                                onSelectionConsumed = { pendingBasicSelection = null },
                                pendingExpandGroupId = pendingExpandGroupId,
                                onExpandConsumed = { pendingExpandGroupId = null },
                                scope = scope,
                                onNavigate = onNavigate,
                                stack = basicsStack,
                                onPopStack = { basicsStack.removeAt(basicsStack.lastIndex) },
                                lastBasicsGroupId = lastBasicsGroupId,
                            )
                            // Wide layouts render the stack inside the detail pane; mobile overlays the full pane.
                            if (!isWideLayout) {
                                TabStackTop(
                                    stack = basicsStack,
                                    onPop = { basicsStack.removeAt(basicsStack.lastIndex) },
                                    onNavigate = onNavigate,
                                )
                            }
                        }
                    }

                    composable<Route.Commands> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CommandsPaneScreen(
                                navigator = commandsNavigator,
                                searchState = searchState,
                                pendingSelection = pendingCommandSelection,
                                onSelectionConsumed = { pendingCommandSelection = null },
                                scope = scope,
                                onNavigate = onNavigate,
                                stack = commandsStack,
                                onPopStack = { commandsStack.removeAt(commandsStack.lastIndex) },
                            )
                            if (!isWideLayout) {
                                TabStackTop(
                                    stack = commandsStack,
                                    onPop = { commandsStack.removeAt(commandsStack.lastIndex) },
                                    onNavigate = onNavigate,
                                )
                            }
                        }
                    }

                    composable<Route.Tips> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            var showInfo by rememberSaveable { mutableStateOf(false) }
                            Column(modifier = Modifier.fillMaxSize()) {
                                PaneTopBar(
                                    title = "Tips",
                                    actions = {
                                        IconButton(
                                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                                            onClick = { showInfo = true },
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Info,
                                                contentDescription = "Info",
                                            )
                                        }
                                    },
                                )
                                val viewModel: TipsViewModel = koinInject()
                                TipsScreen(viewModel = viewModel, onNavigate = onNavigate)
                            }
                            if (showInfo) {
                                AppInfoDialog(onDismiss = { showInfo = false })
                            }
                            TabStackTop(
                                stack = tipsStack,
                                onPop = { tipsStack.removeAt(tipsStack.lastIndex) },
                                onNavigate = onNavigate,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TabStackTop(
    stack: SnapshotStateList<TabStackEntry>,
    onPop: () -> Unit,
    onNavigate: (NavEvent) -> Unit,
) {
    val top = stack.lastOrNull() ?: return
    TabStackEntryContent(entry = top, onBack = onPop, onNavigate = onNavigate)
}

// Keyed by encoded identity so popping back restores per-entry state (scroll, etc.)
// instead of leaking it into the next entry.
@Composable
internal fun TabStackEntryContent(
    entry: TabStackEntry,
    onBack: () -> Unit,
    onNavigate: (NavEvent) -> Unit,
) {
    val stateHolder = rememberSaveableStateHolder()
    stateHolder.SaveableStateProvider(entry.encode()) {
        when (entry) {
            is TabStackEntry.Command -> CommandDetailPane(
                commandName = entry.name,
                onBack = onBack,
                onNavigate = onNavigate,
            )
            is TabStackEntry.BasicGroup -> BasicGroupDetailPane(
                categoryId = entry.categoryId,
                expandGroupId = entry.expandGroupId,
                onBack = onBack,
                onNavigate = onNavigate,
            )
        }
    }
}

private fun parseDeeplink(url: String?): DeeplinkResult? {
    if (url == null) return null

    return when {
        url.endsWith("/basics.html") || url.endsWith("/basics") ->
            DeeplinkResult(Route.Basics, null)

        url.endsWith("/tips.html") || url.endsWith("/tips") ->
            DeeplinkResult(Route.Tips, null)

        url.contains("/man/") -> {
            val commandName = url.substringAfterLast("/man/").removeSuffix(".html")
            DeeplinkResult(Route.Commands, InitialSelection.Command(commandName))
        }

        url.contains("/basic/") -> {
            val categoryId = url.substringAfterLast("/basic/").removeSuffix(".html")
            DeeplinkResult(
                Route.Basics,
                InitialSelection.Basics(id = categoryId, title = categoryId),
            )
        }

        url.contains("/search/") -> {
            val query = url.substringAfterLast("/search/").removeSuffix(".html")
            DeeplinkResult(Route.Commands, InitialSelection.SearchQuery(query))
        }

        url.endsWith("/") || url.endsWith("/index.html") ->
            DeeplinkResult(Route.Commands, null)

        else -> null
    }
}
