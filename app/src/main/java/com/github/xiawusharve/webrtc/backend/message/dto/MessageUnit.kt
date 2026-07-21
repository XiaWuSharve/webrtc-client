package com.github.xiawusharve.webrtc.backend.message.dto

data class MessageUnit(
    val type: MessageType,
    val text: String? = null,
)