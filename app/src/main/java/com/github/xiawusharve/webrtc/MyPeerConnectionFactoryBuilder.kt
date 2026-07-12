package com.github.xiawusharve.webrtc

import android.content.Context
import android.media.AudioManager
import android.os.Build
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import android.util.Log
import org.webrtc.AudioSource

class MyPeerConnectionFactoryBuilder(
    private val context: Context,
) {
    companion object {
        const val TAG = "MyPeerConnectionFactoryBuilder"
    }

    private lateinit var audioSource: AudioSource
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var rtcConfiguration: PeerConnection.RTCConfiguration
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioDeviceModule: AudioDeviceModule

    // 初始化 PeerConnectionFactory（生产环境关闭 Tracer）
    fun initializeFactory(enableInternalTracer: Boolean) {
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(enableInternalTracer) // 生产环境务必设为 false
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)
    }

    fun createRTCConfiguration(URL: String, username: String, password: String) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder(URL)
                .setUsername(username)
                .setPassword(password)
                .createIceServer())
        this.rtcConfiguration = PeerConnection.RTCConfiguration(iceServers).apply {
//            iceTransportsType = PeerConnection.IceTransportsType.RELAY
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
    }

    fun createAudioDeviceModule() {
        this.audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(error: String?) {
                    Log.e(TAG, "AudioRecord 初始化错误: $error")
                }

                override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                    error: String?
                ) {
                    Log.e(TAG, "AudioRecord 启动错误: errorCode=${errorCode?.name}, error=$error")
                }

                override fun onWebRtcAudioRecordError(error: String) {
                    Log.e(TAG, "AudioRecord 发生错误: $error")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(error: String?) {
                    Log.e(TAG, "AudioTrack 初始化错误: $error")
                }

                override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                    error: String?
                ) {
                    Log.e(TAG, "AudioTrack 启动错误: errorCode=${errorCode?.name}, error=$error")
                }

                override fun onWebRtcAudioTrackError(error: String) {
                    Log.e(TAG, "AudioTrack 发生错误: $error")
                }
            })
            .setAudioRecordStateCallback(object : JavaAudioDeviceModule.AudioRecordStateCallback {
                override fun onWebRtcAudioRecordStart() {
                    Log.d(TAG, "AudioRecord 开始录制")
                }

                override fun onWebRtcAudioRecordStop() {
                    Log.d(TAG, "AudioRecord 停止录制")
                }
            })
            .setAudioTrackStateCallback(object : JavaAudioDeviceModule.AudioTrackStateCallback {
                override fun onWebRtcAudioTrackStart() {
                    Log.d(TAG, "AudioTrack 开始播放")
                }

                override fun onWebRtcAudioTrackStop() {
                    Log.d(TAG, "AudioTrack 停止播放")
                }
            })
            .createAudioDeviceModule()
            .also {
                it.setMicrophoneMute(false)
                it.setSpeakerMute(false)
            }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true   // 使用扬声器，false 则使用听筒
    }

    fun createPeerConnectionFactory() {
        // 创建 Factory
        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun createAudioTrack(audioTrackId: String) {
        this.audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        this.audioTrack = peerConnectionFactory.createAudioTrack(audioTrackId, audioSource)
        audioTrack.setEnabled(true)
    }
    fun createMyPeerConnection(peerConnectionObserver: PeerConnectionObserver): MyPeerConnection {
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, peerConnectionObserver)
                ?: throw IllegalStateException("无法创建 PeerConnection")
        return MyPeerConnection(
            peerConnection = peerConnection,
            audioTrack = audioTrack,
            offerConstraints = MediaConstraints(),
            answerConstraints = MediaConstraints()
        )
    }
}