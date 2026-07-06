package com.github.xiawusharve.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SdpExchangeObserverInterface {
//    fun onConnect(sessionId: String);

    fun onCall(sdp: SessionDescription);

    fun onAnswer(sdp: SessionDescription);

    fun onCandidate(candidate: IceCandidate);

    fun onConnect();
}