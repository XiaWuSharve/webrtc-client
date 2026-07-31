package com.github.xiawusharve.webrtc.backend

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.xiawusharve.webrtc.Config
import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.MessagePreview
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.message.KCPSignalingClient
import com.github.xiawusharve.webrtc.backend.message.MessageChain
import com.github.xiawusharve.webrtc.backend.message.MessageObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalExchangeObserverImpl
import com.github.xiawusharve.webrtc.backend.message.SignalingClient
import com.github.xiawusharve.webrtc.backend.message.WebSocketSignalingClient
import kcp.ChannelConfig
import kcp.KcpConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.ProtocolException
import java.net.URI

// TODO 封装模块解耦
class MyViewModel(private val application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager(application)
    private val _messages: MutableStateFlow<List<MessagePreview>> = MutableStateFlow(emptyList())
    val messages: StateFlow<List<MessagePreview>> = _messages.asStateFlow()
    private val _config: MutableStateFlow<Config> = MutableStateFlow(Config("", "", ""))
    val config: StateFlow<Config> = _config.asStateFlow()
    private val _editMessage = MutableStateFlow("")
    val editMessage: StateFlow<String> = _editMessage.asStateFlow()
    // 当前模式
    private val _mode = MutableStateFlow(Mode.PREVIEW)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    init {
        viewModelScope.launch {
            val config = preferenceManager.loadConfig()
            val messages = preferenceManager.loadMessageList()
            updateConfig { config }
            setMessages(messages)
            initCommunicationComponents()
            if (config.localId == "" || config.remoteId == "" || config.displayName == "") {
                switchMode(Mode.EDIT)
            } else {
                // connect回调里自动注册
//                signalExchangeObserver.register()
            }
        }
    }
    companion object {
        private const val TAG = "MyViewModel"
        private const val TURN_URL = "turn:101.37.76.38:3480?transport=udp"
        private const val TURN_USERNAME = "sharve"
        private const val TURN_PASSWORD = "sharve"
        private const val WS_URL = "ws://101.37.76.38:3001/ws"
        private const val iceCheckIntervalStrongConnectivityMs = 2000
        private const val iceConnectionReceivingTimeout = 3000
        private const val AUDIO_TRACK_ID = "demoAudioTrackId"
    }
    enum class Mode { EDIT, PREVIEW }

    private lateinit var signalExchangeObserver: SignalExchangeObserverImpl
    // 更新方法示例
    fun updateConfig(content: (Config) -> Config) {
        _config.update(content)
        viewModelScope.launch { preferenceManager.saveConfig(_config.value) }
    }
    fun updateLocalId(newId: String) {
        updateConfig{ it.copy(localId = newId) }
    }
    fun updateRemoteId(newId: String) {
        updateConfig { it.copy(remoteId = newId) }
    }
    fun updateDisplayName(displayName: String) {
        updateConfig { it.copy(displayName = displayName) }
    }

    fun clearEditMessage() {
        _editMessage.value = ""
    }

    fun updateEditMessage(text: String) {
        _editMessage.value = text

    }

    fun switchMode(newMode: Mode) {
        _mode.value = newMode
    }

    fun addMessage(message: MessagePreview) {
        _messages.update { it + message }
        viewModelScope.launch { preferenceManager.saveMessageList(_messages.value) }
    }

    fun setMessages(messages: List<MessagePreview>) {
        _messages.update { messages }
    }

    fun initCommunicationComponents(protocol: String = "kcp") {
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
        val myPeerConnectionFactoryBuilder = MyPeerConnectionFactoryBuilder(application)
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
        // connect回调里自动注册
        signalingClient.connect(MessageObserverImpl( this::addMessage, signalExchangeObserver, application))
        Log.i(TAG, "初始化通讯组件成功")
        this.signalExchangeObserver = signalExchangeObserver
    }

    fun register() {
        Log.i(TAG, "注册用户中")
        signalExchangeObserver.register(config.value.localId, config.value.remoteId, config.value.displayName) // TODO: 添加连接成功回调
    }

    fun send(messageChainIndexed: MessageChain.MessageChainIndexed): Boolean {
        val messageChain = messageChainIndexed.messageChain
        Log.i(TAG, "sending $messageChain")
        val p = messageChainIndexed.index
        if (p == -1) signalExchangeObserver.sendChatMessage(messageChain.list())
        else {
            val asyncFunc = { sdp: String ->
                messageChain.list().mapIndexed { index, unit ->
                    if (index == p) unit.toBuilder().setMessage(sdp).build() else unit
                }
            }
            when(messageChain.list()[p].type) {
                MessageOuterClass.MessageUnitType.CALL -> signalExchangeObserver.call(asyncFunc)
                MessageOuterClass.MessageUnitType.ANSWER -> signalExchangeObserver.answer(asyncFunc)
                MessageOuterClass.MessageUnitType.ESTABLISH -> signalExchangeObserver.establish { messageChain.list() }
                else -> {}
            }
        }
        // TODO 肯定不能永远true
        return true
    }

}