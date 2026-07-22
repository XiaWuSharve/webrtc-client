package com.github.xiawusharve.webrtc.backend.message.dto.response

import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class MessageResponse {
    abstract val createdTime: Long
}

@Serializable
@SerialName("candidate")
data class CandidateMessageResponse(
    val data: CandidateData, override val createdTime: Long,
): MessageResponse()

@Serializable
data class CandidateData(
    @SerialName("sessionId") val remoteId: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String,
)


@Serializable
@SerialName("chat")
data class ChatMessageResponse(
    override val createdTime: Long,
    val data: ChatDataResponse,
): MessageResponse()

@Serializable
data class ChatDataResponse(
    val remoteId: String,
    val displayName: String,
    val messageChain: List<MessageUnit>,
)


@Serializable
@SerialName("connect")
data class ConnectMessageResponse(
    @SerialName("data") val status: ConnectStatus, override val createdTime: Long
): MessageResponse()

enum class ConnectStatus {
    SUCCESS    ,
    FAIL        ,
    ALREADY_EXIST,
}