package com.github.xiawusharve.webrtc.backend.message.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageUnitType {
    @SerialName("text") TEXT,
    @SerialName("call") CALL,
    @SerialName("answer") ANSWER,
    @SerialName("establish") ESTABLISH,
}

@Serializable
data class MessageUnit(
    val type: MessageUnitType,
    val message: String? = null,
)
