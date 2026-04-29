package com.linuxcommandlibrary.app.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.linuxcommandlibrary.app.NavEvent
import com.linuxcommandlibrary.app.ui.screens.search.SearchScreen
import com.linuxcommandlibrary.app.ui.screens.search.SearchViewModel
import org.koin.compose.koinInject

@Composable
fun SearchOverlayBox(
    searchState: SearchState,
    onNavigate: (NavEvent) -> Unit,
    selectedCommandName: String? = null,
    selectedBasicGroupId: Long? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = searchState.searchText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300, delayMillis = 300)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                val searchViewModel: SearchViewModel = koinInject()
                SearchScreen(
                    searchText = searchState.searchText,
                    viewModel = searchViewModel,
                    onNavigate = onNavigate,
                    selectedCommandName = selectedCommandName,
                    selectedBasicGroupId = selectedBasicGroupId,
                )
            }
        }
    }
}
