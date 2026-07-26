package com.github.xiawusharve.webrtc.backend.message

abstract class SignalingClientAbstract: SignalingClient {
    abstract var messageObserver: MessageObserver

    override fun connect(messageObserver: MessageObserver) {
        this.messageObserver = messageObserver
    }


}