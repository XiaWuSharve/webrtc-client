package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.candidateMessage
import com.github.xiawusharve.webrtc.connectMessageRequest
import com.github.xiawusharve.webrtc.message
import io.netty.buffer.ByteBuf
import kcp.ChannelConfig
import kcp.KcpClient
import kcp.KcpListener
import kcp.Ukcp
import org.webrtc.IceCandidate
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Date

open class KCPSignalingClient(
    private val serverUri: URI,
    channelConfig: ChannelConfig
) : KcpClient(channelConfig), KcpListener, SignalingClient {
    private lateinit var writer: Ukcp
    private lateinit var displayName: String
    private lateinit var localId: String
    private lateinit var remoteId: String
    private lateinit var messageObserver: MessageObserver
    private val TAG = "KCPSignalingClient"

    override fun connect(messageObserver: MessageObserver) {
        writer = super.connect(InetSocketAddress(serverUri.host, serverUri.port), this)
        this.messageObserver = messageObserver
    }

    override fun register(
        localId: String,
        remoteId: String,
        displayName: String
    ) {
        Log.d(TAG, "registering user")
        this.localId = localId
        this.remoteId = remoteId
        this.displayName = displayName
        val req: MessageOuterClass.Message = message {
            connectMessageRequest = connectMessageRequest {
                id = localId
                this@connectMessageRequest.displayName = displayName
            }
        }
        send(req.toByteArray())
    }

    fun send(data: ByteArray) {
        val frame = Frame(
            createdTime = Date().time,
            payload = data
        )
        val buf = frame.toByteBuf()
        Log.d(TAG, "sending in kcp: ${buf.toString(StandardCharsets.UTF_8)}")
        if (!writer.write(buf)) {
            Log.e(TAG, "缓冲区满了？")
        }
        buf.release()
    }

    // 工厂模式对共同字段统一处理
    override fun sendCandidate(candidate: IceCandidate) {
        val req: MessageOuterClass.Message = message {
            candidateMessage = candidateMessage {
                remoteId = this.remoteId
                sdpMid = candidate.sdpMid
                sdpMlineIndex = candidate.sdpMLineIndex
                sdp = candidate.sdp
            }
        }
        send(req.toByteArray())
    }

    override fun sendChatMessage(messageChain: List<MessageOuterClass.MessageUnit>) {
        val req: MessageOuterClass.Message = message {
            chatMessage = MessageOuterClass.ChatMessage.newBuilder()
                .setRemoteId(remoteId)
                .addAllMessageChain(messageChain)
                .build()
        }
        send(req.toByteArray())
    }

    override fun onConnected(ukcp: Ukcp?) {
        Log.i(TAG, "onConnected")
    }

    override fun handleReceive(byteBuf: ByteBuf?, ukcp: Ukcp?) {
        Log.d(TAG, "handleReceive")
        if (byteBuf == null) return
        Log.d(TAG, "received: ${byteBuf.toString(StandardCharsets.UTF_8)}")
        val frame = Frame.parseFrom(byteBuf)
        val message = MessageOuterClass.Message.parseFrom(frame.payload)
        when(message.dataCase) {
            MessageOuterClass.Message.DataCase.CHAT_MESSAGE -> {
                messageObserver.onReceiveMessage(message)
            }
            MessageOuterClass.Message.DataCase.CONNECT_MESSAGE_RESPONSE -> {
                if (message.connectMessageResponse.status != MessageOuterClass.ConnectStatus.SUCCESS) {
                    Log.e(TAG, "registering user failed, see server logs")
                }
                messageObserver.onConnected(message.connectMessageResponse.status)
            }
            MessageOuterClass.Message.DataCase.CANDIDATE_MESSAGE -> {
                messageObserver.onReceiveCandidate(IceCandidate(
                    message.candidateMessage.sdpMid,
                    message.candidateMessage.sdpMlineIndex,
                    message.candidateMessage.sdp))
            }
            else -> {
                Log.e(TAG, "unknown message type ${message.dataCase.name}")
            }
        }
    }

    override fun handleException(ex: Throwable?, ukcp: Ukcp?) {
        Log.e(TAG, "handleException: $ex")
    }

    override fun handleClose(ukcp: Ukcp?) {
        Log.d(TAG, "handleClose")
    }

}
