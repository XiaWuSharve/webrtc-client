package com.github.xiawusharve.webrtc.backend.message

import io.netty.buffer.ByteBuf
import kcp.ChannelConfig
import kcp.KcpClient
import kcp.KcpListener
import kcp.Ukcp
import message.MessageOuterClass
import org.webrtc.IceCandidate
import java.net.InetSocketAddress
import java.net.URI

open class KCPSignalingClient(
    private val serverUri: URI,
    channelConfig: ChannelConfig
) : KcpClient(channelConfig), KcpListener, SignalingClient {

    private lateinit var messageObserver: MessageObserver

    override fun connect(messageObserver: MessageObserver) {
        super.connect(InetSocketAddress(serverUri.host, serverUri.port), this)
        this.messageObserver = messageObserver
    }

    override fun register(
        localId: String,
        remoteId: String,
        displayName: String
    ) {
        TODO("Not yet implemented")
    }

    override fun sendCandidate(candidate: IceCandidate) {
        TODO("Not yet implemented")
    }

    override fun sendChatMessage(messageChain: List<MessageOuterClass.MessageUnit>) {
        TODO("Not yet implemented")
    }

    override fun onConnected(ukcp: Ukcp?) {
        TODO("Not yet implemented")
    }

    override fun handleReceive(byteBuf: ByteBuf?, ukcp: Ukcp?) {
        TODO("Not yet implemented")
    }

    override fun handleException(ex: Throwable?, ukcp: Ukcp?) {
        TODO("Not yet implemented")
    }

    override fun handleClose(ukcp: Ukcp?) {
        TODO("Not yet implemented")
    }

}
