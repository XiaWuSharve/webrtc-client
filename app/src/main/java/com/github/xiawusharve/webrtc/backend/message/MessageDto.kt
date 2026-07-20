package com.github.xiawusharve.webrtc.backend.message

data class MessageDto(
    val type: MessageType,
    val text: String,
    val localId: String,
    val remoteId: String,
    val sdp: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
)
