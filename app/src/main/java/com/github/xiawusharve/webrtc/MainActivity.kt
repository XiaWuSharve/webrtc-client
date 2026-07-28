package com.github.xiawusharve.webrtc

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.xiawusharve.webrtc.backend.MyViewModel
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.message.KCPSignalingClient
import com.github.xiawusharve.webrtc.backend.message.MessageObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalExchangeObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalingClient
import com.github.xiawusharve.webrtc.backend.message.WebSocketSignalingClient
import com.github.xiawusharve.webrtc.ui.theme.WebrtcTheme
import com.permissionx.guolindev.PermissionX
import kcp.ChannelConfig
import kcp.KcpConfig
import java.net.ProtocolException
import java.net.URI
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity() {

    private lateinit var signalExchangeObserver: SignalExchangeObserverImpl
    private val simpleDateFormat = SimpleDateFormat.getDateTimeInstance(
        DateFormat.SHORT, DateFormat.SHORT
    )

    // TODO config file
    companion object {
        private const val TAG = "MainActivity"
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val WS_URL = "ws://192.168.239.36:3001/ws"
        private const val iceCheckIntervalStrongConnectivityMs = 2000
        private const val iceConnectionReceivingTimeout = 3000
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PreviewLayer()
        }
        requestPermissions()
        initCommunicationComponents()
        testNotification()
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

    private fun testNotification() {
        val myNotification = MyNotification(this)
        myNotification.createNotificationChannel("0", "电话通道", NotificationManager.IMPORTANCE_HIGH)
        val notification =
            myNotification.createNotification("hello notification", "收到一条消息")
        myNotification.nofity(notification)
        startForegroundService(this)
    }

    private fun register(localId: String, remoteId: String, displayName: String) {
        Log.i(TAG, "注册用户中")
        signalExchangeObserver.register(localId, remoteId, displayName) // TODO: 添加连接成功回调
    }

    data class MessageChainIndexed(val messageChain: List<MessageOuterClass.MessageUnit>, val index: Int)

    fun text2MessageChain(editMessage: String): MessageChainIndexed {
        // abc/calldef/answerghi/establishjkl -> abc /call def /answer ghi /establish jkl
        // TODO 检查空字符串格式问题
        var result = editMessage.split(Regex("(?=/call|/answer|/establish)|(?<=/call|/answer|/establish)"))
        val strPredicate = { s: String -> s == "/call" || s == "/answer" || s == "/establish" }
        val p = result.indexOfFirst(strPredicate)
        if (p != -1) {
            result = result.filterIndexed { index, string -> index <= p || !strPredicate(string) }
        }
        val messageChain = result.map { s -> messageUnit {
            type = when(s) {
                "/call" -> MessageOuterClass.MessageUnitType.CALL
                "/answer" -> MessageOuterClass.MessageUnitType.ANSWER
                "/establish" -> MessageOuterClass.MessageUnitType.ESTABLISH
                else -> MessageOuterClass.MessageUnitType.TEXT
            }
            message = s
        }}
        return MessageChainIndexed(messageChain, p)
    }

    private fun send(messageChainIndexed: MessageChainIndexed): Boolean {
        val messageChain = messageChainIndexed.messageChain
        Log.i(TAG, "sending $messageChain")
        val p = messageChainIndexed.index
        if (p == -1) signalExchangeObserver.sendChatMessage(messageChain)
        else {
            val asyncFunc = { sdp: String ->
                messageChain.mapIndexed { index, unit ->
                    if (index == p) unit.toBuilder().setMessage(sdp).build() else unit
                }
            }
            when(messageChain[p].type) {
                MessageOuterClass.MessageUnitType.CALL -> signalExchangeObserver.call(asyncFunc)
                MessageOuterClass.MessageUnitType.ANSWER -> signalExchangeObserver.answer(asyncFunc)
                MessageOuterClass.MessageUnitType.ESTABLISH -> signalExchangeObserver.establish { messageChain }
                else -> {}
            }
        }
        // TODO 肯定不能永远true
        return true
    }

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
    fun MyTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            modifier = modifier,
        )
    }

    @Composable
    fun MessageList(messages: List<MessagePreview>) {
        Surface() {
            LazyColumn() {
                items(messages) { message -> MessageCard(message)}
            }
        }
    }

    @Composable
    fun MessageCard(message: MessagePreview) {
        Column(modifier = Modifier.padding(all = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.displayName, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(simpleDateFormat.format(message.time), color = MaterialTheme.colorScheme.tertiary)
            }
            val interactionSource = remember { MutableInteractionSource() }
            LazyRow(verticalAlignment = Alignment.CenterVertically) {
                items(message.messageChain) { unit ->
                    when(unit.type) {
                        MessageOuterClass.MessageUnitType.TEXT -> unit.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                        MessageOuterClass.MessageUnitType.CALL -> Text("/call",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(interactionSource, indication = ripple()){})
                        MessageOuterClass.MessageUnitType.ANSWER -> Text("/answer",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(interactionSource, indication = ripple()){})
                        MessageOuterClass.MessageUnitType.ESTABLISH -> Text("/establish", color = MaterialTheme.colorScheme.primary)
                        else -> {}
                    }
                }
            }
        }
    }

    // TODO 换接近于label大小的size
    @Composable
    fun ConfigLayer(
        viewModel: MyViewModel
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
                onSave
            )
            Mode.PREVIEW -> PreviewConfig(config, onEdit)
        }
    }

    @Composable
    fun EditConfig(
        viewModel: MyViewModel,
        onSave: () -> Unit,
    ) {
        val config by viewModel.config.collectAsStateWithLifecycle()
        // 只在外层添加 padding，内部子组件共用外层修饰符
        Column() {
            Row(horizontalArrangement = Arrangement.SpaceAround) {
                MyTextField(
                    value = config.localId,
                    onValueChange = viewModel::updateLocalId,
                    label = "我的ID - 字母与数字组成",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MyTextField(
                    value = config.remoteId,
                    onValueChange = viewModel::updateRemoteId,
                    label = "对方ID",
                    modifier = Modifier.weight(1f)
                )
            }
            MyTextField(
                value = config.displayName,
                onValueChange = viewModel::updateDisplayName,
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Preview
    @Composable
    fun PreviewLayer() {

        val config by MyViewModel().config.collectAsStateWithLifecycle()
        WebrtcTheme() {
            Scaffold(
                bottomBar = {
                    SendLayer(

                        onClick = {
                            val messageChainIndexed = text2MessageChain(editMessage)
                            send(messageChainIndexed)
                            messageList.add(MessagePreview(
                                displayName = config.displayName,
                                time = Date(),
                                messageChain = messageChainIndexed.messageChain
                            ))
                            editMessage = "" },
                        value = editMessage,
                        onValueChange = { v -> editMessage = v}
                    , modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 8.dp))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 8.dp)
                    .imePadding()
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(innerPadding)
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
                        onSave = {
                            register(localId = config.localId, remoteId = config.remoteId, displayName = config.displayName)
                            mode = Mode.PREVIEW
                        },
                        onEdit = { mode = Mode.EDIT },
                    )
                    MessageList(messageList)
                }
            }
        }
    }

    @Composable
    fun SendLayer(onClick: () -> Unit, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                maxLines = 3,
                modifier = Modifier.weight(1f)
            )
            FilledIconButton(onClick) { Icon(Icons.Filled.Send, contentDescription = "发送") }
        }
    }
}
