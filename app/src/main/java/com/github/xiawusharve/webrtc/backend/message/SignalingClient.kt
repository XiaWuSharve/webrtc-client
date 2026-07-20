package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.lang.Exception
import java.net.URI

open class SignalingClient(
    serverUri: URI,
) : WebSocketClient(serverUri) {
    private lateinit var localId: String
    private lateinit var remoteId: String
    private lateinit var signalingClientObserver: SignalExchangeObserver
    private val TAG = "SignalingClient"

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(TAG, "onOpen handshakedata=$handshakedata")
    }

    override fun onMessage(message: String) {
        Log.d(TAG, "received: $message")
        try {
            val jsonObject = JSONObject(message)
            val type = jsonObject.getString("type")
            when (type) {
                "connect" -> {
                    val status = jsonObject.getInt("data")
                    if (status != 0) {
                        Log.e(TAG, "registering user failed, see server logs")
                    }
                    signalingClientObserver.onConnected(status)
                }
                "call" -> {
                    val data = jsonObject.getJSONObject("data")
                    val remoteSdp = data.getString("sdp")
                    val remoteId = data.getString("remoteId")
                    this.remoteId = remoteId
                    signalingClientObserver.onReceiveCall(
                        SessionDescription(
                            SessionDescription.Type.OFFER, remoteSdp
                        )
                    )
                }
                "answer" -> {
                    val remoteSdp = jsonObject.getString("data")
                    signalingClientObserver.onReceiveAnswer(
                        SessionDescription(
                            SessionDescription.Type.ANSWER,
                            remoteSdp
                        ))
                }
                "candidate" -> {
                    val data = jsonObject.getJSONObject("data")
                    val sdpMid = data.getString("sdpMid")
                    val sdpMLineIndex = data.getInt("sdpMLineIndex")
                    val sdp = data.getString("sdp")
                    signalingClientObserver.onReceiveCandidate(IceCandidate(sdpMid, sdpMLineIndex, sdp))
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "failed to parse message: $e")
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.i(TAG, "onClose code=$code reason=$reason remote=$remote")
    }

    override fun onError(ex: Exception?) {
        Log.i(TAG, "onError ex=$ex")
    }

    fun register(localId: String, signalingClientObserver: SignalExchangeObserver) {
        Log.d(TAG, "registering user")
        this.localId = localId
        signalingClientObserver.setSignalingClient(this)
        this.signalingClientObserver = signalingClientObserver
        val jsonObject = JSONObject()
        jsonObject.put("type", "connect")
        jsonObject.put("data", this.localId)
        send(jsonObject.toString())
    }

    fun call(sdp: SessionDescription) {
        Log.i(TAG, "call")
        val data = JSONObject()
        data.put("sdp", sdp.description)
            .put("remoteId", remoteId)
            .put("localId", localId)
        val jsonObject = JSONObject()
        jsonObject.put("type", "call")
            .put("data", data)
        send(jsonObject.toString())
    }

    fun answer(sdp: SessionDescription) {
        Log.i(TAG, "answer")
        val data = JSONObject()
        data.put("sdp", sdp.description)
            .put("sessionId", remoteId)
        val jsonObject = JSONObject()
        jsonObject.put("type", "answer")
            .put("data", data)
        send(jsonObject.toString())
    }

    fun sendCandidate(candidate: IceCandidate) {
        val data = JSONObject()
        data
            .put("sessionId", remoteId)
            .put("sdp", candidate.sdp)
            .put("sdpMid", candidate.sdpMid)
            .put("sdpMLineIndex", candidate.sdpMLineIndex)
        val jsonObject = JSONObject()
        jsonObject.put("type", "candidate")
            .put("data", data)
        send(jsonObject.toString())
    }

    fun setRemoteId(remoteId: String) {
        this.remoteId = remoteId
    }
}
