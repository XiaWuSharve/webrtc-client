package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.MessageOuterClass
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalExchangeObserver {
    fun onReceiveCall(sdp: SessionDescription)
    fun onReceiveAnswer(sdp: SessionDescription)
    fun onReceiveEstablish()
    fun onReceiveCandidate(candidate: IceCandidate)
    fun removeCandidate(p0: Array<out IceCandidate?>?)
    fun sendCandidate(candidate: IceCandidate)
    fun call(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>)
    fun answer(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>)
    fun establish(cb: () -> List<MessageOuterClass.MessageUnit>)
    fun sendChatMessage(messageChain: List<MessageOuterClass.MessageUnit>)
    fun register(localId: String, remoteId: String, displayName: String)
}