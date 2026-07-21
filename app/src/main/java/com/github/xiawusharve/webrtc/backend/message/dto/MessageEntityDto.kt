package com.github.xiawusharve.webrtc.backend.message.dto

import java.util.Date

data class MessageEntityDto (
    val localId: String? = null,
    val remoteId: String? = null,
    val sdp: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val displayName: String? = null,
    val time: Date? = null,
    val messageChain: List<MessageUnit>,
)