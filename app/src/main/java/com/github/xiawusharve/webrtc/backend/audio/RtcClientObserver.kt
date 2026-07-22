package com.github.xiawusharve.webrtc.backend.audio

interface RtcClientObserver {
    fun onInitFail(e: Exception)
    fun onInitSuccess()
}