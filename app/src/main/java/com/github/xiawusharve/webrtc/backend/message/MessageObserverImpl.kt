package com.github.xiawusharve.webrtc.backend.message

import android.content.Context
import android.widget.Toast
import com.github.xiawusharve.webrtc.MessagePreview
import com.github.xiawusharve.webrtc.backend.message.dto.MessageUnitType
import com.github.xiawusharve.webrtc.backend.message.dto.response.ChatMessageResponse
import com.github.xiawusharve.webrtc.backend.message.dto.response.ConnectStatus
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.Date

class MessageObserverImpl(
    private val context: Context,
    private var messageListStatus: List<MessagePreview>,
    private val signalExchangeObserver: SignalExchangeObserver
): MessageObserver {
    override fun onConnected(code: ConnectStatus) {
        when (code) {
            ConnectStatus.SUCCESS -> {
                Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
            }
            ConnectStatus.ALREADY_EXIST -> {
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

    override fun onReceiveMessage(messages: ChatMessageResponse) {
        for (m in messages.data.messageChain) {
            when(m.type) {
                MessageUnitType.CALL -> {
                    signalExchangeObserver.onReceiveCall(
                        SessionDescription(
                            SessionDescription.Type.OFFER, m.message
                        )
                    )
                }

                MessageUnitType.ANSWER -> {
                    signalExchangeObserver.onReceiveAnswer(
                        SessionDescription(
                            SessionDescription.Type.ANSWER,
                            m.message
                        ))
                }

                MessageUnitType.ESTABLISH -> {
                    signalExchangeObserver.onReceiveEstablish()
                }

                MessageUnitType.TEXT -> {

                }
            }
        }
        messageListStatus = messageListStatus + MessagePreview(
            messages.data.displayName,
            Date(messages.createdTime),
            messages.data.messageChain
        )
    }

    override fun onSendMessage() {
        TODO("Not yet implemented")
    }

    override fun onReceiveCandidate(iceCandidate: IceCandidate) {
        signalExchangeObserver.onReceiveCandidate(iceCandidate)
    }
}