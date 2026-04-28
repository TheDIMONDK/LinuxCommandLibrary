package com.linuxcommandlibrary.app

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
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
import com.linuxcommandlibrary.app.ui.screens.basics.BasicsPaneScreen
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
}

private data class DeeplinkResult(val route: Route, val selection: InitialSelection?)

@Composable
fun App(initialDeeplink: String? = null) {
    val reviewHandler: ReviewHandler = koinInject()
    val commandsRepository: CommandsRepository = koinInject()
    LaunchedEffect(Unit) {
        reviewHandler.incrementAppStartCount()
        reviewHandler.requestReviewIfNeeded()
        withContext(Dispatchers.Default) {
            commandsRepository.getCommands()
        }
    }

    LinuxTheme {
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

    val searchState = rememberSearchState()
    val openAppAction = rememberOpenAppAction()
    val scope = rememberCoroutineScope()

    val commandsNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val basicsNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    var pendingCommandSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBasicSelection by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingExpandGroupId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        when (val selection = deeplinkResult.selection) {
            is InitialSelection.Command -> pendingCommandSelection = selection.name
            is InitialSelection.Basics -> pendingBasicSelection = selection.id
            null -> {}
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination
    val isOnTips = currentRoute?.hasRoute<Route.Tips>() == true
    val isOnCommands = currentRoute?.hasRoute<Route.Commands>() == true
    val isOnBasics = currentRoute?.hasRoute<Route.Basics>() == true

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    val isWideLayout = layoutType != NavigationSuiteType.NavigationBar

    val onNavigate: (NavEvent) -> Unit = { event ->
        when (event) {
            is NavEvent.ToCommand -> {
                // The pane screen's LaunchedEffect picks this up after mount and calls
                // navigator.navigateTo. Going through state instead of inlining the
                // suspend call avoids cross-recomposition timing issues when the tab
                // switch happens before the suspending navigator update propagates.
                pendingCommandSelection = event.commandName
                if (currentRoute?.hasRoute<Route.Commands>() != true) {
                    navController.navigate(Route.Commands) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            is NavEvent.ToBasicGroups -> {
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

            is NavEvent.OpenAction -> openAppAction(event.action)
        }
    }

    // PopUntilContentChange so chained see-also detail screens pop one at a time;
    // the default PopUntilScaffoldValueChange treats Detail("ls") and Detail("rm")
    // as the same scaffold value and pops both together.
    val backBehavior = BackNavigationBehavior.PopUntilContentChange
    BackHandler(
        enabled = (isOnCommands && commandsNavigator.canNavigateBack(backBehavior)) ||
            (isOnBasics && basicsNavigator.canNavigateBack(backBehavior)),
    ) {
        scope.launch {
            when {
                isOnCommands -> commandsNavigator.navigateBack(backBehavior)
                isOnBasics -> basicsNavigator.navigateBack(backBehavior)
            }
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
                        BasicsPaneScreen(
                            navigator = basicsNavigator,
                            searchState = searchState,
                            pendingSelection = pendingBasicSelection,
                            onSelectionConsumed = { pendingBasicSelection = null },
                            pendingExpandGroupId = pendingExpandGroupId,
                            onExpandConsumed = { pendingExpandGroupId = null },
                            scope = scope,
                            onNavigate = onNavigate,
                        )
                    }

                    composable<Route.Commands> {
                        CommandsPaneScreen(
                            navigator = commandsNavigator,
                            searchState = searchState,
                            pendingSelection = pendingCommandSelection,
                            onSelectionConsumed = { pendingCommandSelection = null },
                            scope = scope,
                            onNavigate = onNavigate,
                        )
                    }

                    composable<Route.Tips> {
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
                    }
                }
            }
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

        url.endsWith("/") || url.endsWith("/index.html") ->
            DeeplinkResult(Route.Commands, null)

        else -> null
    }
}
