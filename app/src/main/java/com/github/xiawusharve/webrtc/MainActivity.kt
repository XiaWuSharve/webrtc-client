package com.github.xiawusharve.webrtc

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.xiawusharve.webrtc.backend.Config
import com.github.xiawusharve.webrtc.backend.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.PeerConnectionObserver
import com.github.xiawusharve.webrtc.backend.SignalExchangeObserver
import com.github.xiawusharve.webrtc.backend.SignalingClient
import com.github.xiawusharve.webrtc.ui.theme.WebrtcTheme
import com.permissionx.guolindev.PermissionX
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var signalExchangeObserver: SignalExchangeObserver
    private lateinit var signalingClient: SignalingClient
    private lateinit var myPeerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder
    private val simpleDateFormat = SimpleDateFormat.getDateTimeInstance(
        DateFormat.SHORT, DateFormat.SHORT
    )

    companion object {
        private const val TAG = "MainActivity"
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
        private const val WS_URL = "ws://101.37.76.38:3001"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreviewLayer()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.INTERNET,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
                        Log.i(TAG, "所有权限已授予")
                        Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show()
                        initWebRTC()
                        val notification = Notification(this)
                        notification.createNotificationChannel("0", "电话通道", NotificationManager.IMPORTANCE_HIGH)
                        notification.setContentIntent("hello notification")
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

    @Composable
    fun SaveAndRegisterButton(onSave:() -> Unit, modifier: Modifier = Modifier) {
        Button(
            onClick = { onSave() },
            shape = MaterialTheme.shapes.small,
            modifier = modifier
        ) {
            // TODO 中间态组件
            Text("保存&重新注册")
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

    @Composable
    fun MyTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = modifier,
        )
    }

    @Composable
    fun MessageList(messages: List<String>) {
        Surface() {
            LazyColumn() {
                items(messages) { message -> MessageCard("sharve", Date(), message)}
            }
        }
    }

    @Composable
    fun MessageCard(name: String, time: Date, message: String) {
        Column(modifier = Modifier.padding(all = 8.dp)) {
            Row {
                Text(name, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(simpleDateFormat.format(time), color = MaterialTheme.colorScheme.tertiary)
            }
            Text(message, color = MaterialTheme.colorScheme.secondary)
        }
    }

    enum class Mode { EDIT, PREVIEW }

    // TODO 换接近于label大小的size
    @Composable
    fun ConfigLayer(
        config: Config,
        onLocalIdChange: (String) -> Unit,
        onRemoteIdChange: (String) -> Unit,
        onDisplayNameChange: (String) -> Unit,
        mode: Mode,
        onSave: () -> Unit,
        onEdit: () -> Unit
    ) {
        when(mode) {
            Mode.EDIT -> EditConfig(
                config,
                onLocalIdChange,
                onRemoteIdChange,
                onDisplayNameChange,
                onSave
            )
            Mode.PREVIEW -> PreviewConfig(config, onEdit)
        }
    }

    @Composable
    fun EditConfig(
        config: Config,
        onLocalIdChange: (String) -> Unit,
        onRemoteIdChange: (String) -> Unit,
        onDisplayNameChange: (String) -> Unit,
        onSave: () -> Unit,
    ) {
        // 只在外层添加 padding，内部子组件共用外层修饰符
        Column() {
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                MyTextField(
                    value = config.localId,
                    onValueChange = onLocalIdChange,
                    label = "我的ID - 字母与数字组成",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MyTextField(
                    value = config.remoteId,
                    onValueChange = onRemoteIdChange,
                    label = "对方ID",
                    modifier = Modifier.weight(1f)
                )
            }
            MyTextField(
                value = config.displayName,
                onValueChange = onDisplayNameChange,
                label = "展示名称",
                modifier = Modifier.fillMaxWidth()
            )
            SaveAndRegisterButton(
                onSave = onSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    
    @Composable
    fun PreviewConfig(
        config: Config,
        onEdit: () -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${config.localId}|${config.remoteId}|${config.displayName}",
                color = MaterialTheme.colorScheme.tertiary)
            EditButton(onEdit)
        }
    }

    @Composable
    fun EditButton(onClick: () -> Unit) {
        IconButton(onClick) { Icon(Icons.Filled.Edit, contentDescription = "编辑") }
    }

    @Preview
    @Composable
    fun PreviewLayer() {
        var config by remember {
            mutableStateOf(
                Config(
                    localId = "commie",
                    remoteId = "sharve",
                    displayName = "康米"
                )
            )
        }

        var mode by remember { mutableStateOf(Mode.PREVIEW) }

        WebrtcTheme() {
            //        Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 8.dp)
            ) {
                ConfigLayer(
                    config = config,
                    onLocalIdChange = { newLocalId ->
                        config = config.copy(localId = newLocalId)
                    },
                    onRemoteIdChange = { newRemoteId ->
                        config = config.copy(remoteId = newRemoteId)
                    },
                    onDisplayNameChange = { newDisplayName ->
                        config = config.copy(displayName = newDisplayName)
                    },
                    mode = mode,
                    onSave = { mode = Mode.PREVIEW },
                    onEdit = { mode = Mode.EDIT },
                )

//                SaveAndRegisterButton(
//                    onSave = { register(config.localId) },
//                    modifier = Modifier.fillMaxWidth()
////                    modifier = Modifier.wrapContentSize()
//                )
//                TestCallAutoAnswer(
//                    onClick = { testCall(remoteId) },
//                    modifier = Modifier.wrapContentSize()  // 或指定大小，如 Modifier.size(100.dp)
//                )
//                Spacer(modifier = Modifier.weight(1f))  // 可选：把按钮推到顶部，或者不加也行
                MessageList(
                    listOf("hello world", "hello sharve"),
                )
            }
//        }
        }
    }
}
