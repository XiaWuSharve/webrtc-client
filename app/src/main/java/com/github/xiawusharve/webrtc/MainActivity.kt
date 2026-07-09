package com.github.xiawusharve.webrtc

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.permissionx.guolindev.PermissionX
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.net.URI
import java.util.Collections

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
        private const val STREAM_ID = "demoStreamId"
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val WS_URL = "ws://101.37.76.38:3001"
    }

    // 持有 WebRTC 核心对象，方便在 onDestroy 中清理
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private lateinit var audioTrack: AudioTrack
    private lateinit var audioSource: AudioSource
    private lateinit var peerConnection: PeerConnection
    private lateinit var audioDeviceModule: AudioDeviceModule

    private lateinit var signalingClient: SignalingClient
    private lateinit var rtcClient: RtcClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)  // ✅ 整个 Column 都避开系统栏
                ) {
                    var localId by remember { mutableStateOf("") }
                    LocalIdTextField(localId, {value -> localId = value})

                    var remoteId by remember { mutableStateOf("") }
                    RemoteIdTextField(remoteId, { value -> remoteId = value })

                    RegisterButton(
                        onClick = { register(localId) },
                        modifier = Modifier.wrapContentSize()
                    )
                    TestCallAutoAnswer(
                        onClick = { testCall(remoteId) },
                        modifier = Modifier.wrapContentSize()  // 或指定大小，如 Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))  // 可选：把按钮推到顶部，或者不加也行
                }
            }
        }
        // 先请求权限，仅在全部授权后初始化 WebRTC
        requestPermissionsAndInit()
    }

    private fun testCall(remoteId: String) {
        Log.d(TAG, "testing communication...")
        signalingClient.setRemoteId(remoteId)
        rtcClient.call()
    }

    private fun register(localId: String) {
        Log.i(TAG, "注册用户中")
        signalingClient.setLocalId(localId)
        signalingClient.register() // TODO: 添加连接成功回调
    }

    private fun requestPermissionsAndInit() {
        Log.i(TAG, "正在获取权限")
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Log.i(TAG, "所有权限已授予")
                    Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
                    initWebRTC()
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

    fun initWebRTC() {
        Log.i(TAG, "初始化WebRTC")
        try {
            // 1. 初始化 PeerConnectionFactory（生产环境关闭 Tracer）
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true) // 生产环境务必设为 false
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            val audioDeviceModule =
                JavaAudioDeviceModule.builder(this).createAudioDeviceModule()
            // 3. 创建 Factory
            val factory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()
            this.audioDeviceModule = audioDeviceModule
            peerConnectionFactory = factory

            // 4. 创建音频源及轨道
            audioSource = factory.createAudioSource(MediaConstraints())
            audioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

            // 5. 配置 ICE 服务器
            val iceServers = listOf(
//                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
                PeerConnection.IceServer.builder(TURN_URL)
                    .setUsername(TURN_USERNAME)
                    .setPassword(TURN_PASSWORD)
                    .createIceServer(),
//                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            )
            val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                // 可在此配置其他选项，例如 continualGatheringPolicy 等
                iceTransportsType = PeerConnection.IceTransportsType.RELAY
            }

            // 6. 创建 PeerConnection（必须传入 Observer）
            val signalingClient = SignalingClient(URI(WS_URL))
            signalingClient.connect()
            val rtcClient = RtcClient(signalingClient)

            signalingClient.setSdpExchangeObserver(rtcClient)
            val peerConnection = factory.createPeerConnection(rtcConfig, rtcClient)
                ?: throw IllegalStateException("无法创建 PeerConnection")
            rtcClient.setPeerConnection(peerConnection)

            // 7. 添加音频轨道
            peerConnection.addTrack(audioTrack, Collections.singletonList(STREAM_ID))

            Log.i(TAG, "WebRTC 初始化成功")
            this.signalingClient = signalingClient
            this.rtcClient = rtcClient
            this.peerConnection = peerConnection
        } catch (e: Exception) {
            Log.e(TAG, "WebRTC 初始化失败", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            // 清理已分配的资源
            releaseResources()
        }
    }

    override fun onDestroy() {
        releaseResources()
        super.onDestroy()
    }

    private fun releaseResources() {
        // 关闭 PeerConnection
        peerConnection.close()

        // 释放音频轨道和源（dispose 后不可再用）
        audioTrack.dispose()
        audioSource.dispose()

        // 释放 Factory
        peerConnectionFactory.dispose()

        // 释放音频设备模块（如有单独释放方法，可调用）
        audioDeviceModule.release()

        Log.d(TAG, "WebRTC 资源已释放")
    }

}

@Composable
fun RegisterButton(onClick:() -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = { onClick() },
        modifier = modifier
    ) {
        Text("注册")
    }
}

@Composable
fun TestCallAutoAnswer(onClick:() -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = { onClick() },
        modifier = modifier
    ) {
        Text("拨号等待回答并发送candidates建立通讯")
    }
}

// 定义 TextField 组件，同时接收值和回调
@Composable
fun LocalIdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("我的ID - 字母与数字组成") },
        modifier = modifier
    )
}

// 定义 TextField 组件，同时接收值和回调
@Composable
fun RemoteIdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("对方ID") },
        modifier = modifier
    )
}