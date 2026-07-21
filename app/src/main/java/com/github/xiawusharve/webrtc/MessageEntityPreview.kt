package com.github.xiawusharve.webrtc

import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import java.util.Date

data class MessageEntityPreview(
    val displayName: String,
    val time: Date,
    val messageChain: List<MessageUnit>,
)
