package com.github.xiawusharve.webrtc

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import com.github.xiawusharve.webrtc.ui.theme.WebrtcTheme
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
        private const val TURN_URL = "turn:101.37.76.38:3480"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val WS_URL = "ws://101.37.76.38:3001"
    }

    // 持有 WebRTC 核心对象，方便在 onDestroy 中清理
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var peerConnection: PeerConnection? = null
    private var audioDeviceModule: AudioDeviceModule? = null

    private var myWebSocketClient: MyWebSocketClient? = null
    private var rtcClient: RtcClient? = null


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
                    var remoteSessionId by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = remoteSessionId,
                        onValueChange = { remoteSessionId = it },
                        label = { Text("remote session id") },
                        modifier = Modifier.fillMaxWidth()  // 可选：宽度充满，但不覆盖全局 padding
                    )
                    TestCallAutoAnswer(
                        onClick = { testCall(remoteSessionId) },
                        modifier = Modifier.wrapContentSize()  // 或指定大小，如 Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))  // 可选：把按钮推到顶部，或者不加也行
                }
            }
        }
        // 先请求权限，仅在全部授权后初始化 WebRTC
        requestPermissionsAndInit()
    }

    private fun testCall(remoteSessionId: String) {
        myWebSocketClient?.setRemoteSessionId(remoteSessionId)
        rtcClient?.call()
    }

    private fun requestPermissionsAndInit() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.ACCESS_NETWORK_STATE
            )
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
                    initWebRTC()
                } else {
                    Toast.makeText(
                        this,
                        "以下权限被拒绝: $deniedList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    fun initWebRTC() {
        try {
            // 1. 初始化 PeerConnectionFactory（生产环境关闭 Tracer）
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(false) // 生产环境务必设为 false
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            val audioDeviceModule =
                JavaAudioDeviceModule.builder(this).createAudioDeviceModule()
            // 3. 创建 Factory
            val factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            this.audioDeviceModule = audioDeviceModule
            peerConnectionFactory = factory

            // 4. 创建音频源及轨道
            audioSource = factory.createAudioSource(MediaConstraints())
            audioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

            // 5. 配置 ICE 服务器
            val iceServer = PeerConnection.IceServer.builder(TURN_URL)
                .setUsername(TURN_USERNAME)
                .setPassword(TURN_PASSWORD)
                .createIceServer()
            val rtcConfig = PeerConnection.RTCConfiguration(listOf(iceServer)).apply {
                // 可在此配置其他选项，例如 continualGatheringPolicy 等
            }

            // 6. 创建 PeerConnection（必须传入 Observer）
            val myWebSocketClient = MyWebSocketClient(URI(WS_URL))
            // test
            myWebSocketClient.setRemoteSessionId("fe9cc6c1-f688-299b-9fd5-f6524ae1")
            myWebSocketClient.connect()
            val rtcClient = RtcClient(myWebSocketClient)

            myWebSocketClient.setSdpExchangeObserver(rtcClient)
            val peerConnection = factory.createPeerConnection(rtcConfig, rtcClient)
                ?: throw IllegalStateException("无法创建 PeerConnection")
            rtcClient.setPeerConnection(peerConnection)

            // 7. 添加音频轨道
            peerConnection.addTrack(audioTrack, Collections.singletonList(STREAM_ID))

            // 配置音频路由
            val audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true

            Log.d(TAG, "PeerConnection 创建成功")
            this.myWebSocketClient = myWebSocketClient
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
        peerConnection?.close()
        peerConnection = null

        // 释放音频轨道和源（dispose 后不可再用）
        audioTrack?.dispose()
        audioTrack = null
        audioSource?.dispose()
        audioSource = null

        // 释放 Factory
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        // 释放音频设备模块（如有单独释放方法，可调用）
        audioDeviceModule?.release()

        Log.d(TAG, "WebRTC 资源已释放")
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun TestCallAutoAnswer(onClick:() -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = { onClick() },
        modifier = modifier
    ) {
        Text("测试")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WebrtcTheme {
        TestCallAutoAnswer({
            Log.i("GreetingPreview", "button clicked")
        })
        Greeting("Android")
    }
}