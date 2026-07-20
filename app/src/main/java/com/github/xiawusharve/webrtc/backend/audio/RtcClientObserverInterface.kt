package com.github.xiawusharve.webrtc.backend.audio

interface RtcClientObserverInterface {
    fun onInitFail(e: Exception)
    fun onInitSuccess()
    fun onReceiveMessage()
}