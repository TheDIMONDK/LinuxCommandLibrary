package com.linuxcommandlibrary.app.nav

import com.linuxcommandlibrary.app.ui.composables.AppIcon

internal data class TopLevelDestination(
    val key: RouteKey,
    val label: String,
    val icon: AppIcon,
)

internal val TopLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(RouteKey.Basics, "Basics", AppIcon.PUZZLE),
    TopLevelDestination(RouteKey.Tips, "Tips", AppIcon.IDEA),
    TopLevelDestination(RouteKey.Commands, "Commands", AppIcon.SEARCH),
)
