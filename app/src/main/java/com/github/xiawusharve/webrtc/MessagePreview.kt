package com.github.xiawusharve.webrtc

import java.util.Date

data class MessagePreview(
    val displayName: String,
    val time: Date,
    val messageChain: List<MessageOuterClass.MessageUnit>,
)
