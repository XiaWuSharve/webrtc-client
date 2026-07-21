package com.github.xiawusharve.webrtc.backend.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.xiawusharve.webrtc.backend.message.SignalExchangeObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalingClient
import java.net.URI

class RtcClient(
    private val WS_URL: String,

    private val context: Context,
    val TURN_URL: String,
    val TURN_USERNAME: String,
    val TURN_PASSWORD: String,
): BroadcastReceiver() {

    private lateinit var signalingClient: SignalingClient
    private lateinit var signalExchangeObserver: SignalExchangeObserverImpl
    private lateinit var myPeerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder

    companion object {
        const val TAG = "RtcClient"
        private const val iceCheckIntervalStrongConnectivityMs = 2000
        private const val iceConnectionReceivingTimeout = 3000
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("RtcClient", "trigger onReceive")
    }

    // TODO: 添加连接成功回调
    fun register(localId: String) {
        signalingClient.register(localId, signalExchangeObserver)
    }

    fun init(observer: RtcClientObserverInterface) {
        Log.i(TAG, "初始化WebRTC")
        try {
            // connect
            val signalingClient = SignalingClient(URI(WS_URL))
            signalingClient.connect()
            // webrtc
            val myPeerConnectionFactoryBuilder = MyPeerConnectionFactoryBuilder(context)
            myPeerConnectionFactoryBuilder.createRTCConfiguration(
                URL = TURN_URL,
                username = TURN_USERNAME,
                password = TURN_PASSWORD,
                iceCheckIntervalStrongConnectivityMs,
                iceConnectionReceivingTimeout
            )
            myPeerConnectionFactoryBuilder.initializeFactory(true)
            myPeerConnectionFactoryBuilder.createAudioDeviceModule()
            myPeerConnectionFactoryBuilder.createPeerConnectionFactory()
            myPeerConnectionFactoryBuilder.createAudioTrack(AUDIO_TRACK_ID)
            Log.i(TAG, "WebRTC 初始化成功")
            val signalExchangeObserver = SignalExchangeObserverImpl(
                myPeerConnectionFactoryBuilder,
                PeerConnectionObserver(),
            )
            this.myPeerConnectionFactoryBuilder = myPeerConnectionFactoryBuilder
            this.signalingClient = signalingClient
            this.signalExchangeObserver = signalExchangeObserver
            observer.onInitSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "WebRTC 初始化失败", e)
            observer.onInitFail(e)
        }
    }

    fun call(remoteId: String) {
        signalingClient.setRemoteId(remoteId)
        signalExchangeObserver.onCall()
    }

    private fun close() {
        TODO()
    }
}