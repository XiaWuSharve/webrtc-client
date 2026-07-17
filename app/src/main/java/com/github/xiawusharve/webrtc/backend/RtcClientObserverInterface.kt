package com.github.xiawusharve.webrtc.backend

interface RtcClientObserverInterface {
    fun onInitFail(e: Exception)
    fun onInitSuccess()
}