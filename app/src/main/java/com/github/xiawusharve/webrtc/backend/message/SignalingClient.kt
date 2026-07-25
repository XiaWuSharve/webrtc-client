package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import org.webrtc.IceCandidate

interface SignalingClient {
    fun connect(messageObserver: MessageObserver)
    fun register(
        localId: String,
        remoteId: String,
        displayName: String
    )
    fun sendCandidate(candidate: IceCandidate)
    fun sendChatMessage(messageChain: List<MessageUnit>)
}