package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.backend.observer.MessageObserver
import org.webrtc.IceCandidate

interface SignalingClient {
    fun connect(messageObserver: MessageObserver)
    fun register(
        localId: String,
        remoteId: String,
        displayName: String
    )
    fun sendCandidate(candidate: IceCandidate)
    fun sendChatMessage(messageChain: List<MessageOuterClass.MessageUnit>)
}