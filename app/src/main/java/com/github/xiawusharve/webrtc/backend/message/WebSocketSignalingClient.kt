package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnit
import com.github.xiawusharve.webrtc.backend.message.dto.request.CandidateData
import com.github.xiawusharve.webrtc.backend.message.dto.request.CandidateMessageRequest
import com.github.xiawusharve.webrtc.backend.message.dto.request.ChatDataRequest
import com.github.xiawusharve.webrtc.backend.message.dto.request.ChatMessageRequest
import com.github.xiawusharve.webrtc.backend.message.dto.request.ConnectDataRequest
import com.github.xiawusharve.webrtc.backend.message.dto.request.ConnectMessageRequest
import com.github.xiawusharve.webrtc.backend.message.dto.request.MessageRequest
import com.github.xiawusharve.webrtc.backend.message.dto.response.CandidateMessageResponse
import com.github.xiawusharve.webrtc.backend.message.dto.response.ChatMessageResponse
import com.github.xiawusharve.webrtc.backend.message.dto.response.ConnectMessageResponse
import com.github.xiawusharve.webrtc.backend.message.dto.response.ConnectStatus
import com.github.xiawusharve.webrtc.backend.message.dto.response.MessageResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.IceCandidate
import java.net.URI
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

    override fun onMessage(message: String) {
        Log.d(TAG, "received: $message")
        when(val messageRequestObj = json.decodeFromString<MessageResponse>(message)) {
            is ChatMessageResponse -> {
                messageObserver.onReceiveMessage(messageRequestObj)
            }
            is ConnectMessageResponse -> {
                if (messageRequestObj.status != ConnectStatus.SUCCESS) {
                    Log.e(TAG, "registering user failed, see server logs")
                }
                messageObserver.onConnected(messageRequestObj.status)
            }
            is CandidateMessageResponse -> {
                messageObserver.onReceiveCandidate(IceCandidate(
                    messageRequestObj.data.sdpMid,
                    messageRequestObj.data.sdpMLineIndex,
                    messageRequestObj.data.sdp))
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
        val req: MessageRequest = ConnectMessageRequest(
            ConnectDataRequest(localId, displayName),
            Date().time
        )
        send(json.encodeToString(req))
    }

    override fun sendCandidate(candidate: IceCandidate) {
        val req: MessageRequest = CandidateMessageRequest(
            data = CandidateData(this.remoteId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp),
            createdTime = Date().time
        )
        send(json.encodeToString(req))
    }

    override fun sendChatMessage(messageChain: List<MessageUnit>) {
        val req: MessageRequest = ChatMessageRequest(
            Date().time,
            ChatDataRequest(remoteId, messageChain),
        )
        send(json.encodeToString(req))
    }

    fun setRemoteId(remoteId: String) {
        this.remoteId = remoteId
    }
}
