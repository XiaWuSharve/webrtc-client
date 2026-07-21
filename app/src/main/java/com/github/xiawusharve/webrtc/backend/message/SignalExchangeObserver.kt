package com.github.xiawusharve.webrtc.backend.message

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalExchangeObserver {
    fun onReceiveCall(sdp: SessionDescription)
    fun onReceiveAnswer(sdp: SessionDescription)
    fun onReceiveCandidate(candidate: IceCandidate)
    fun onConnected(code: Int)
    fun onCall()
    fun onAnswer()
    fun onConnect()
    fun onCandidate(candidate: IceCandidate)
}