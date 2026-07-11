package com.github.xiawusharve.webrtc

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.net.URI

class MainActivity : AppCompatActivity() {

    private lateinit var signalExchangeObserver: SignalExchangeObserver
    private lateinit var signalingClient: SignalingClient
    private lateinit var myPeerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder

    companion object {
        private const val TAG = "MainActivity"
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
        private const val WS_URL = "ws://101.37.76.38:3001"
    }

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
                    MessageList(listOf("hello world", "hello sharve"))

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

    fun initWebRTC() {
        Log.i(TAG, "初始化WebRTC")
        try {
            // connect
            val signalingClient = SignalingClient(URI(WS_URL))
            signalingClient.connect()
            // webrtc
            val myPeerConnectionFactoryBuilder = MyPeerConnectionFactoryBuilder(this)
            myPeerConnectionFactoryBuilder.createRTCConfiguration(
                URL = TURN_URL,
                username = TURN_USERNAME,
                password = TURN_PASSWORD
            )
            myPeerConnectionFactoryBuilder.initializeFactory(true)
            myPeerConnectionFactoryBuilder.createAudioDeviceModule()
            myPeerConnectionFactoryBuilder.createPeerConnectionFactory()
            myPeerConnectionFactoryBuilder.createAudioTrack(AUDIO_TRACK_ID)
            Log.i(TAG, "WebRTC 初始化成功")
            val signalExchangeObserver = SignalExchangeObserver(
                myPeerConnectionFactoryBuilder,
                PeerConnectionObserver(),
            )
            this.myPeerConnectionFactoryBuilder = myPeerConnectionFactoryBuilder
            this.signalingClient = signalingClient
            this.signalExchangeObserver = signalExchangeObserver
        } catch (e: Exception) {
            Log.e(TAG, "WebRTC 初始化失败", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            // 清理已分配的资源
//            releaseResources()
        }
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

    private fun register(localId: String) {
        Log.i(TAG, "注册用户中")

        signalingClient.register(
            localId = localId,
            signalingClientObserver = signalExchangeObserver
        ) // TODO: 添加连接成功回调
    }

    private fun testCall(remoteId: String) {
        Log.d(TAG, "testing communication...")
        // TODO 交给rtc client设置
        signalingClient.setRemoteId(remoteId)
        signalExchangeObserver.onCall()
    }

//    override fun onDestroy() {
//        releaseResources()
//        super.onDestroy()
//    }
//
//    private fun releaseResources() {
//        // 关闭 PeerConnection
//        peerConnection.close()
//
//        // 释放音频轨道和源（dispose 后不可再用）
//        audioTrack.dispose()
//        audioSource.dispose()
//
//        // 释放 Factory
//        myPeerConnectionFactoryBuilder.dispose()
//
//        // 释放音频设备模块（如有单独释放方法，可调用）
//        audioDeviceModule.release()
//
//        Log.d(TAG, "WebRTC 资源已释放")
//    }

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

@Composable
fun MessageList(messages: List<String>) {
    LazyColumn {
        items(messages) { message -> Message(message)}
    }
}

@Composable
fun Message(message: String) {
    Row() {
        Text("sharve")
        Text(": ")
        Text(message)
    }
}