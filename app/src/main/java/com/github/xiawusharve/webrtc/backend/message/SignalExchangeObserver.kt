package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalExchangeObserver {
    fun onReceiveCall(sdp: SessionDescription)
    fun onReceiveAnswer(sdp: SessionDescription)
    fun onReceiveEstablish()
    fun onReceiveCandidate(candidate: IceCandidate)
    fun onSendCandidate(candidate: IceCandidate)
    fun onRemoveCandidate(p0: Array<out IceCandidate?>?)
    fun onCall(messageChain: List<MessageUnit>, p: MessageUnit)
    fun onAnswer(messageChain: List<MessageUnit>, p: MessageUnit)
    fun onEstablish(messageChain: List<MessageUnit>, p: MessageUnit)
}