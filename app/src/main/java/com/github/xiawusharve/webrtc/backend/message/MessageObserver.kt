package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.backend.message.dto.response.ChatMessageResponse
import com.github.xiawusharve.webrtc.backend.message.dto.response.ConnectStatus
import org.webrtc.IceCandidate

interface MessageObserver {
    fun onConnected(code: ConnectStatus)
    fun onConnect()
    fun onReceiveMessage(messages: ChatMessageResponse)
    fun onSendMessage()
    fun onReceiveCandidate(iceCandidate: IceCandidate)
}