package com.github.xiawusharve.webrtc.backend

import androidx.lifecycle.ViewModel
import com.github.xiawusharve.webrtc.Config
import com.github.xiawusharve.webrtc.MessagePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MyViewModel : ViewModel() {
    enum class Mode { EDIT, PREVIEW }
    // 消息列表
    private val _messages = MutableStateFlow<List<MessagePreview>>(emptyList())
    val messages: StateFlow<List<MessagePreview>> = _messages.asStateFlow()

    // 配置信息（改成保存纯字符串）
    private val _config = MutableStateFlow(
        Config(
            localId = "",
            remoteId = "",
            displayName = ""
        )
    )
    val config: StateFlow<Config> = _config.asStateFlow()

    // 当前模式
    private val _mode = MutableStateFlow(Mode.PREVIEW)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    // 编辑消息文本
    private val _editMessage = MutableStateFlow("")
    val editMessage: StateFlow<String> = _editMessage.asStateFlow()

    // 更新方法示例
    fun updateLocalId(newId: String) {
        _config.update { it.copy(localId = newId) }
    }
    fun updateRemoteId(newId: String) {
        _config.update { it.copy(remoteId = newId) }
    }
    fun updateDisplayName(displayName: String) {
        _config.update { it.copy(displayName = displayName) }
    }

    fun updateEditMessage(text: String) {
        _editMessage.value = text
    }

    fun switchMode(newMode: Mode) {
        _mode.value = newMode
    }
}