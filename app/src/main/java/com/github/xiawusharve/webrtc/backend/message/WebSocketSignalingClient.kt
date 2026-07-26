package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import com.github.xiawusharve.webrtc.MessageOuterClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import message.MessageOuterClass
import message.MessageOuterClass.Message
import message.candidateMessage
import message.chatMessage
import message.connectMessageRequest
import message.message
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.IceCandidate
import java.net.URI
import java.nio.ByteBuffer
import java.util.Date

open class WebSocketSignalingClient(
    serverUri: URI,
) : WebSocketClient(serverUri), SignalingClient {
    private lateinit var displayName: String
    private lateinit var localId: String
    private lateinit var remoteId: String
    private lateinit var messageObserver: MessageObserver
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        classDiscriminator = "type"
        namingStrategy = JsonNamingStrategy.SnakeCase
        decodeEnumsCaseInsensitive = true
    }
    private val TAG = "SignalingClient"

    override fun connect(messageObserver: MessageObserver) {
        super.connect()
        this.messageObserver = messageObserver
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(TAG, "onOpen handshakedata=$handshakedata")
    }

    override fun onMessage(message: String?) {
    }

    override fun onMessage(bytes: ByteBuffer?) {
        super.onMessage(bytes)
        Log.d(TAG, "received: ${bytes.toString()}")
        val message = Message.parseFrom(bytes)
        when(message.dataCase) {
            Message.DataCase.CHAT_MESSAGE -> {
                messageObserver.onReceiveMessage(message)
            }
            Message.DataCase.CONNECT_MESSAGE_RESPONSE -> {
                if (message.connectMessageResponse.status != MessageOuterClass.ConnectStatus.SUCCESS) {
                    Log.e(TAG, "registering user failed, see server logs")
                }
                messageObserver.onConnected(message.connectMessageResponse.status)
            }
            Message.DataCase.CANDIDATE_MESSAGE -> {
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

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.i(TAG, "onClose code=$code reason=$reason remote=$remote")
    }

    override fun onError(ex: Exception?) {
        Log.i(TAG, "onError ex=$ex")
    }

    // TODO: 添加连接成功回调
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
            createdTime = Date().time
            connectMessageRequest {
                id = localId
                this@connectMessageRequest.displayName = displayName
            }
        }
        send(req.toByteArray())
    }

    override fun sendCandidate(candidate: IceCandidate) {
        val req: MessageOuterClass.Message = message {
            createdTime = Date().time
            candidateMessage {
                remoteId = this.remoteId
                sdpMid = candidate.sdpMid
                sdpMlineIndex = candidate.sdpMLineIndex
                sdp = candidate.sdp
            }
        }
        send(req.toByteArray())
    }

    override fun sendChatMessage(messageChain: List<MessageOuterClass.MessageUnit>) {
        val req: Message = message {
            createdTime = Date().time
            chatMessage = MessageOuterClass.ChatMessage.newBuilder()
                .setRemoteId(remoteId)
                .addAllMessageChain(messageChain)
                .build()
        }
        send(req.toByteArray())
    }

    fun setRemoteId(remoteId: String) {
        this.remoteId = remoteId
    }
}
