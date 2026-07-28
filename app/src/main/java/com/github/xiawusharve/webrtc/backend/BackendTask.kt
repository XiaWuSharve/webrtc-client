package com.github.xiawusharve.webrtc.backend

import android.Manifest
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import com.github.xiawusharve.webrtc.MainActivity.Companion.TAG
import com.github.xiawusharve.webrtc.MessagePreview
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.message.KCPSignalingClient
import com.github.xiawusharve.webrtc.backend.message.MessageObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalExchangeObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalingClient
import com.github.xiawusharve.webrtc.backend.message.WebSocketSignalingClient
import com.permissionx.guolindev.PermissionX
import kcp.ChannelConfig
import kcp.KcpConfig
import java.net.ProtocolException
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat

class BackendTask: AsyncTask {
    private lateinit var signalExchangeObserver: SignalExchangeObserverImpl
    private val messageList = mutableStateListOf<MessagePreview>()
    private lateinit var signalingClient: SignalingClient
    private val simpleDateFormat = SimpleDateFormat.getDateTimeInstance(
        DateFormat.SHORT, DateFormat.SHORT
    )

    // TODO config file

    companion object {
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val WS_URL = "ws://101.37.76.38:3001/ws"
        private const val iceCheckIntervalStrongConnectivityMs = 2000
        private const val iceConnectionReceivingTimeout = 3000
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
    }

    override fun run() {
        requestPermissions()
        initCommunicationComponents()
    }

    private fun initCommunicationComponents(protocol: String = "kcp") {
        Log.i(TAG, "初始化通讯组件")
        // connect
        var signalingClient: SignalingClient
        when(protocol) {
            "kcp" -> {
                val kcpConfig = KcpConfig()
                kcpConfig.nodelay(true, 40, 2, true)
                kcpConfig.setSndwnd(1024)
                kcpConfig.setRcvwnd(1024)
                kcpConfig.setMtu(1400)
                kcpConfig.isAckNoDelay = false
                kcpConfig.setAckMaskSize(0)

                val channelConfig = ChannelConfig(kcpConfig)

//                channelConfig.setFecAdapt(FecAdapt(10, 3))


                //channelConfig.setTimeoutMillis(10000);

                //禁用参数
                channelConfig.isCrc32Check = false
                signalingClient = KCPSignalingClient(URI(WS_URL), channelConfig)
            }
            "websocket" ->  signalingClient = WebSocketSignalingClient(URI(WS_URL))
            else -> throw ProtocolException("unknown protocol $protocol")
        }

        // webrtc
        val myPeerConnectionFactoryBuilder = MyPeerConnectionFactoryBuilder(this)
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
        val signalExchangeObserver =
            SignalExchangeObserverImpl(myPeerConnectionFactoryBuilder, signalingClient)
        signalingClient.connect(MessageObserverImpl(this, messageList, signalExchangeObserver))
        Log.i(TAG, "初始化通讯组件成功")
        this.signalExchangeObserver = signalExchangeObserver
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun requestPermissions() {
        Log.i(TAG, "正在获取权限")
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
            )
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Log.i(TAG, "所有权限已授予")
                    Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "acquire permission failed: $deniedList")
                    Toast.makeText(
                        this,
                        "以下权限被拒绝: $deniedList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

}