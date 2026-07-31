package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.backend.observer.MessageObserver
import java.net.URI

abstract class SignalingClientAbstract(private val serverUri: URI): SignalingClient {
    abstract var messageObserver: MessageObserver

    override fun connect(messageObserver: MessageObserver) {
        this.messageObserver = messageObserver
    }


}