package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.MessageOuterClass
import org.webrtc.IceCandidate

interface MessageObserver {
    fun onConnected(code: MessageOuterClass.ConnectStatus)
    fun onConnect()
    fun onReceiveMessage(messages: MessageOuterClass.Message)
    fun onSendMessage()
    fun onReceiveCandidate(iceCandidate: IceCandidate)
}