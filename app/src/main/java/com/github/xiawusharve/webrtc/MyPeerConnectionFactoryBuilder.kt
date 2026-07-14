package com.github.xiawusharve.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.voiceengine.WebRtcAudioEffects

class MyPeerConnectionFactoryBuilder(
    private val context: Context,
) {
    companion object {
        const val TAG = "MyPeerConnectionFactoryBuilder"
        const val SAMPLE_RATE = 8000
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
            // 增加连接检查间隔（默认20ms太频繁），改为 1000ms
            iceCheckIntervalStrongConnectivityMs = 2000
//            // 延长接收超时，减少不必要的重传探测
            iceConnectionReceivingTimeout = 3000
        }
    }

    fun createAudioDeviceModule() {
        // TODO 精细控制
//        AudioRecord
//            .Builder()
//            .setAudioFormat(
//                new AudioFormat.Builder()
//                    .setSampleRate(16000)
//                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
//                    .build()
//            )
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        var speakerDevice: AudioDeviceInfo?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val availableDevice = audioManager.availableCommunicationDevices
            // 3. 从列表中筛选出扬声器 (AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
            // TODO 切换设备
            speakerDevice = availableDevice.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

            // 4. 如果找到了扬声器，就将其设置为通信设备
            speakerDevice?.let {
                audioManager.setCommunicationDevice(it)
            } ?: run {
                Log.e(TAG, "unable to find speaker")
            }
        } else {
            audioManager.isSpeakerphoneOn = true   // 使用扬声器，false 则使用听筒
        }

        this.audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseLowLatency(false)
            .setUseStereoInput(true)
            .setUseStereoOutput(true)
            .setEnableVolumeLogger(true)
            .setSampleRate(SAMPLE_RATE)
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
                // TODO 切换静音
                it.setMicrophoneMute(false)
                it.setSpeakerMute(false)
                it.setPreferredMicrophoneFieldDimension((-1).toFloat())
                it.setNoiseSuppressorEnabled(false)
            }
//        val webRtcAudioEffects = WebRtcAudioEffects.create()
//        webRtcAudioEffects.setNS(false)
//        webRtcAudioEffects.enable()
    }

    fun createPeerConnectionFactory() {
        // 创建 Factory
        this.peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun createAudioTrack(audioTrackId: String) {
        val constraints = MediaConstraints()
        constraints.mandatory.addAll(listOf(
            MediaConstraints.KeyValuePair("sampleRate", SAMPLE_RATE.toString()),
            MediaConstraints.KeyValuePair("echoCancellation", "true"),
            MediaConstraints.KeyValuePair("sampleSize", "16"),
            MediaConstraints.KeyValuePair("autoGainControl", "true"),
            MediaConstraints.KeyValuePair("noiseSuppression", "false"),
            MediaConstraints.KeyValuePair("dtx", "true"),
            MediaConstraints.KeyValuePair("channelCount", "2")
        ))
        this.audioSource = peerConnectionFactory.createAudioSource(constraints)
        this.audioTrack = peerConnectionFactory.createAudioTrack(audioTrackId, audioSource)
        audioTrack.setEnabled(true)
    }
    fun createMyPeerConnection(peerConnectionObserver: PeerConnectionObserver): MyPeerConnection {
        val peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, peerConnectionObserver)
                ?: throw IllegalStateException("无法创建 PeerConnection")
        val myPeerConnection = MyPeerConnection(
            peerConnection = peerConnection,
            audioTrack = audioTrack,
            offerConstraints = MediaConstraints(),
            answerConstraints = MediaConstraints()
        )
        val transceiver = myPeerConnection.addTrack()
        // 1. 获取当前音频方向（kind）支持的所有编码能力
        val capabilities = peerConnectionFactory.getRtpSenderCapabilities(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO
        )
        val allCodecs = capabilities.codecs

        // 2. 过滤出你想要保留的编码列表（即排除 RED 和 ULPFEC）
        val preferredCodecs = allCodecs.filter { codec ->
            // 保留标准的音频编码（如opus, PCMU/PCMA等），同时排除RED和ULPFEC
            codec.name != "red" && codec.name != "ulpfec"
        }

        // 3. 将过滤后的列表设置为优先编码
        transceiver.setCodecPreferences(preferredCodecs)

        // 新增：限制发送码率（建议 16kbps ~ 24kbps，人声通话足够）
        val sender = transceiver.sender
        val parameters = sender.parameters
        parameters.encodings?.forEach { encoding ->
            // 设置最大码率为 20kbps
            encoding.maxBitrateBps = 20000
        }
        sender.parameters = parameters
        return myPeerConnection
    }
}