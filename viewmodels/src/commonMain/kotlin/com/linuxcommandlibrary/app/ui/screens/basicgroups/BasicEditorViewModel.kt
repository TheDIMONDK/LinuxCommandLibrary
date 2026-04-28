package com.linuxcommandlibrary.app.ui.screens.basicgroups

import com.linuxcommandlibrary.app.data.BasicsRepository
import com.linuxcommandlibrary.shared.BasicGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BasicEditorViewModel(
    categoryId: String,
    basicsRepository: BasicsRepository,
    scope: CoroutineScope,
) {
    val showTitles: Boolean = categoryId != "terminalgames"

    private val _groups = MutableStateFlow<List<BasicGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadJob = scope.launch(Dispatchers.Default) {
            val basicInfo = basicsRepository.getBasicInfo(categoryId)
            if (basicInfo != null) {
                _groups.value = basicInfo.groups
            }
        }
    }

    fun cancel() {
        loadJob?.cancel()
        loadJob = null
    }
}
