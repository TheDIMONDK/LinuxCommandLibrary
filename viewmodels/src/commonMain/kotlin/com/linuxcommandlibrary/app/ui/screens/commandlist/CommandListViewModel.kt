package com.linuxcommandlibrary.app.ui.screens.commandlist

import com.linuxcommandlibrary.app.data.CommandInfo
import com.linuxcommandlibrary.app.data.CommandsRepository
import com.linuxcommandlibrary.app.data.DataManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommandListViewModel(
    private val dataManager: DataManager,
    private val commandsRepository: CommandsRepository,
    private val scope: CoroutineScope,
) {
    private val _commands = MutableStateFlow<List<CommandInfo>>(emptyList())
    val commands = _commands.asStateFlow()

    /**
     * Re-exposes DataManager's live bookmark set. Updates automatically when
     * the user adds or removes a bookmark anywhere in the app.
     */
    val bookmarkedNames = dataManager.bookmarkNames

    private var loadJob: Job? = null
    private var bookmarkJob: Job? = null

    init {
        loadJob = scope.launch(Dispatchers.Default) {
            _commands.value = commandsRepository.getCommands()
        }
        // Re-sort the list whenever bookmarks change so bookmarked items float to the top.
        bookmarkJob = scope.launch(Dispatchers.Default) {
            dataManager.bookmarkNames.collect { bookmarks ->
                _commands.value = _commands.value.sortedBy { it.name !in bookmarks }
            }
        }
    }

    fun cancel() {
        loadJob?.cancel()
        bookmarkJob?.cancel()
        loadJob = null
        bookmarkJob = null
    }
}
