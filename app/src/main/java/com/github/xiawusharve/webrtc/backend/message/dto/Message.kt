package com.github.xiawusharve.webrtc.backend.message.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Message

@Serializable
@SerialName("connect")
data class ConnectMessageRequest(
    val data: ConnectDataRequest
): Message()

@Serializable
data class ConnectDataRequest(
    val id: String,
    val displayName: String,
)

@Serializable
@SerialName("candidate")
data class CandidateMessageRequest(
    val data: CandidateDataRequest,
)

@Serializable
data class CandidateDataRequest(
    @SerialName("sessionId") val remoteId: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String,
)
