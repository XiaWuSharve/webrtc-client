package com.github.xiawusharve.webrtc

import java.util.Date

data class MessageEntityPreview(
    val displayName: String,
    val time: Date,
    val messageChain: List<MessageUnitPreview>,
)
