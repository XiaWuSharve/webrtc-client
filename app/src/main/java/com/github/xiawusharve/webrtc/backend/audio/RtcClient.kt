package com.github.xiawusharve.webrtc.backend.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.xiawusharve.webrtc.backend.message.SignalExchangeObserverImpl
import com.github.xiawusharve.webrtc.backend.message.WebSocketSignalingClient

class RtcClient(
    private val WS_URL: String,
    private val context: Context,
    val TURN_URL: String,
    val TURN_USERNAME: String,
    val TURN_PASSWORD: String,
): BroadcastReceiver() {

    private lateinit var signalingClient: WebSocketSignalingClient
    private lateinit var signalExchangeObserver: SignalExchangeObserverImpl
    private lateinit var myPeerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder


    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RtcClient", "trigger onReceive")
    }
}