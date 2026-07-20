package com.github.xiawusharve.webrtc

import com.github.xiawusharve.webrtc.backend.message.MessageType

data class MessageUnitPreview(
    val type: MessageType,
    val text: String? = null,
)
