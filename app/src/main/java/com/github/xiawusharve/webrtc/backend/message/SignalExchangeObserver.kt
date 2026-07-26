package com.github.xiawusharve.webrtc.backend.message

import message.MessageOuterClass
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalExchangeObserver {
    fun onReceiveCall(sdp: SessionDescription)
    fun onReceiveAnswer(sdp: SessionDescription)
    fun onReceiveEstablish()
    fun onReceiveCandidate(candidate: IceCandidate)
    fun onSendCandidate(candidate: IceCandidate)
    fun onRemoveCandidate(p0: Array<out IceCandidate?>?)
    fun onCall(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>)
    fun onAnswer(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>)
    fun onEstablish(cb: () -> List<MessageOuterClass.MessageUnit>)
}