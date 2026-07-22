package com.github.xiawusharve.webrtc.backend.message.dto.request

import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class MessageRequest {
    abstract val createdTime: Long
}

@Serializable
@SerialName("candidate")
data class CandidateMessageRequest(
    val data: CandidateData, override val createdTime: Long,
): MessageRequest()

@Serializable
data class CandidateData(
    val remoteId: String,
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String,
)

@Serializable
@SerialName("chat")
data class ChatMessageRequest(
    override val createdTime: Long,
    val data: ChatDataRequest,
): MessageRequest()

@Serializable
data class ChatDataRequest(
    val remoteId: String,
    val messageChain: List<MessageUnit>,
)


@Serializable
@SerialName("connect")
data class ConnectMessageRequest(
    val data: ConnectDataRequest, override val createdTime: Long
): MessageRequest()

@Serializable
data class ConnectDataRequest(
    val id: String,
    val displayName: String,
)
