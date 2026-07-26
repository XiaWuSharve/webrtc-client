package com.github.xiawusharve.webrtc.backend.message

import android.content.Context
import android.widget.Toast
import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.MessagePreview
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.Date

class MessageObserverImpl(
    private val context: Context,
    private var messageListStatus: MutableList<MessagePreview>,
    private val signalExchangeObserver: SignalExchangeObserver
): MessageObserver {
    override fun onConnected(code: MessageOuterClass.ConnectStatus) {
        when (code) {
            MessageOuterClass.ConnectStatus.SUCCESS -> {
                Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
            }
            MessageOuterClass.ConnectStatus.ALREADY_EXIST -> {
                Toast.makeText(context, "id已存在", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(context, "注册时发生错误：$code", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onConnect() {
        TODO("Not yet implemented")
    }

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
        messageListStatus.add(MessagePreview(
            messages.chatMessage.displayName,
            Date(messages.createdTime),
            messages.chatMessage.messageChainList
        ))
    }

    override fun onSendMessage() {
        TODO("Not yet implemented")
    }

    override fun onReceiveCandidate(iceCandidate: IceCandidate) {
        signalExchangeObserver.onReceiveCandidate(iceCandidate)
    }
}