package com.github.xiawusharve.webrtc.backend.message

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.annotation.RequiresPermission
import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.MessagePreview
import com.github.xiawusharve.webrtc.MyNotification
import com.github.xiawusharve.webrtc.backend.MyViewModel
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.Date

class MessageObserverImpl(
//    private val context: Context,
    private val viewModel: MyViewModel,
    private val signalExchangeObserver: SignalExchangeObserver,
    private val context: Context
): MessageObserver {
    override fun onConnected(code: MessageOuterClass.ConnectStatus) {
        viewModel.register()
//        when (code) {
//            MessageOuterClass.ConnectStatus.SUCCESS -> {
//                Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
//            }
//            MessageOuterClass.ConnectStatus.ALREADY_EXIST -> {
//                Toast.makeText(context, "id已存在", Toast.LENGTH_LONG).show()
//            }
//            else -> {
//                Toast.makeText(context, "注册时发生错误：$code", Toast.LENGTH_LONG).show()
//            }
//        }
    }

    override fun onConnect() {
        TODO("Not yet implemented")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceiveMessage(messages: MessageOuterClass.Message) {
        for (m in messages.chatMessage.messageChainList) {
            when(m.type) {
                MessageOuterClass.MessageUnitType.CALL -> {
                    signalExchangeObserver.onReceiveCall(
                        SessionDescription(
                            SessionDescription.Type.OFFER, m.message
                        )
                    )
                }

                MessageOuterClass.MessageUnitType.ANSWER -> {
                    signalExchangeObserver.onReceiveAnswer(
                        SessionDescription(
                            SessionDescription.Type.ANSWER,
                            m.message
                        ))
                }

                MessageOuterClass.MessageUnitType.ESTABLISH -> {
                    signalExchangeObserver.onReceiveEstablish()
                }

                MessageOuterClass.MessageUnitType.TEXT -> {
                }

                else -> {}
            }
        }
        val messageChain = MessageChain(messages.chatMessage.messageChainList)
        viewModel.addMessage(MessagePreview(
            messages.chatMessage.displayName,
            Date(messages.createdTime),
            messageChain
        ))
        val myNotification = MyNotification(context)
        myNotification.createNotificationChannel("voice_call_urgent", "电话通道", NotificationManager.IMPORTANCE_HIGH)
        val notification =
            myNotification.createNotification(messages.chatMessage.displayName, messageChain.toString())
        myNotification.notify(notification)
    }

    override fun onSendMessage() {
        TODO("Not yet implemented")
    }

    override fun onReceiveCandidate(iceCandidate: IceCandidate) {
        signalExchangeObserver.onReceiveCandidate(iceCandidate)
    }
}