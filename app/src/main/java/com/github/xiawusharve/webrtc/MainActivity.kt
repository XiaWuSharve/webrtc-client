package com.github.xiawusharve.webrtc

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.xiawusharve.webrtc.MyViewModel.Mode
import com.github.xiawusharve.webrtc.backend.message.MessageChain
import com.github.xiawusharve.webrtc.ui.theme.WebrtcTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var myToast: MyToast
    private val simpleDateFormat = SimpleDateFormat.getDateTimeInstance(
        DateFormat.SHORT, DateFormat.SHORT
    )

    // TODO config file
    companion object {
        private const val TAG = "MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.myToast = MyToast(this)
        enableEdgeToEdge()
        setContent {
            UILayer()
        }
        requestPermissions(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
        )
        testNotification()
    }

    private fun testNotification() {
        startForegroundService(this)
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
                items(message.messageChain.list()) { unit ->
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Composable
    fun UILayer() {
        val viewModel by viewModels<MyViewModel>()
        val editMessage by viewModel.editMessage.collectAsStateWithLifecycle()
        val config by viewModel.config.collectAsStateWithLifecycle()
        val messageList by viewModel.messages.collectAsStateWithLifecycle()
        val mode by viewModel.mode.collectAsStateWithLifecycle()
        WebrtcTheme() {
            Scaffold(
                bottomBar = {
                    SendLayer(
                        onClick = {
                            val messageChainIndexed = MessageChain.parseFrom(editMessage)
                            viewModel.send(messageChainIndexed)
                            viewModel.addMessage(MessagePreview(
                                displayName = config.displayName,
                                time = Date(),
                                messageChain = messageChainIndexed.messageChain
                            ))
                            viewModel.clearEditMessage() },
                        value = editMessage,
                        onValueChange = viewModel::updateEditMessage
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
                        onLocalIdChange = viewModel::updateLocalId,
                        onRemoteIdChange = viewModel::updateRemoteId,
                        onDisplayNameChange = viewModel::updateDisplayName,
                        mode = mode,
                        onSave = {
                            viewModel.register()
                            viewModel.switchMode(Mode.PREVIEW)
                        },
                        onEdit = { viewModel.switchMode(Mode.EDIT) },
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
