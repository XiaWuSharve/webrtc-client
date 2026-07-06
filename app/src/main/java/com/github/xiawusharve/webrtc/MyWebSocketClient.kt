package com.github.xiawusharve.webrtc

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.lang.Exception
import java.net.URI

open class MyWebSocketClient(
    serverUri: URI,
) : WebSocketClient(serverUri) {
    private var remoteSessionId: String? = null
    private var sdpExchangeObserverInterface: SdpExchangeObserverInterface? = null
    private val TAG = "MyWebSocketClient"
    private var mySessionId: String? = null

    fun setRemoteSessionId(sessionId: String) {
        this.remoteSessionId = sessionId
    }

    fun setSdpExchangeObserver(sdpExchangeObserverInterface: SdpExchangeObserverInterface) {
        this.sdpExchangeObserverInterface = sdpExchangeObserverInterface
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.i(TAG, "onOpen handshakedata=$handshakedata")
        requestId()
    }

    override fun onMessage(message: String) {
        Log.i(TAG, "onMessage message=$message")
        val sdpExchangeObserverInterface = sdpExchangeObserverInterface
        if (sdpExchangeObserverInterface == null) {
            Log.i(TAG, "sdpExchangeObserverInterface not set")
            return
        }
        try {
            val jsonObject = JSONObject(message)
            val type = jsonObject.getString("type")
            if (type == "connect") {
                mySessionId = jsonObject.getString("data")
            } else if (type == "call") {
                val remoteSdp = jsonObject.getString("data")
                sdpExchangeObserverInterface.onCall(SessionDescription(
                    SessionDescription.Type.OFFER, remoteSdp))
            } else if (type == "answer") {
                val remoteSdp = jsonObject.getString("data")
                sdpExchangeObserverInterface.onAnswer(
                    SessionDescription(
                        SessionDescription.Type.ANSWER,
                        remoteSdp),

                    )
            } else if (type == "candidate") {
                val data = jsonObject.getJSONObject("data")
                val sdpMid = data.getString("sdpMid")
                val sdpMLineIndex = data.getInt("sdpMLineIndex")
                val sdp = data.getString("sdp")
                sdpExchangeObserverInterface.onCandidate(IceCandidate(sdpMid, sdpMLineIndex, sdp))
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

    fun requestId() {
        Log.i(TAG, "requestId")
        val jsonObject = JSONObject()
        jsonObject.put("type", "connect")
        Log.i(TAG, "sending connection request")
        send(jsonObject.toString())
    }

    fun call(sdp: SessionDescription) {
        Log.i(TAG, "call")
        val data = JSONObject()
        data.put("sdp", sdp.description)
            .put("sessionId", remoteSessionId)
        val jsonObject = JSONObject()
        jsonObject.put("type", "call")
            .put("data", data)
        send(jsonObject.toString())
    }

    fun answer(sdp: SessionDescription) {
        Log.i(TAG, "answer")
        val data = JSONObject()
        data.put("sdp", sdp.description)
            .put("sessionId", remoteSessionId)
        val jsonObject = JSONObject()
        jsonObject.put("type", "answer")
            .put("data", data)
        send(jsonObject.toString())
    }

    fun sendCandidate(candidate: IceCandidate) {
        val data = JSONObject()
        data
            .put("sessionId", remoteSessionId)
            .put("sdp", candidate.sdp)
            .put("sdpMid", candidate.sdpMid)
            .put("sdpMLineIndex", candidate.sdpMLineIndex)
        val jsonObject = JSONObject()
        jsonObject.put("type", "candidate")
            .put("data", data)
        send(jsonObject.toString())
    }
}
