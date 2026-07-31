package com.github.xiawusharve.webrtc.backend.message

import com.github.xiawusharve.webrtc.MessageOuterClass
import com.github.xiawusharve.webrtc.messageUnit

class MessageChain(
    private val messageChain: List<MessageOuterClass.MessageUnit>
) {
    data class MessageChainIndexed(val messageChain: MessageChain, val index: Int)
    companion object {
        fun parseFrom(s: String): MessageChainIndexed {
            // abc/calldef/answerghi/establishjkl -> abc /call def /answer ghi /establish jkl
            // TODO 检查空字符串格式问题
            var result =
                s.split(Regex("(?=/call|/answer|/establish)|(?<=/call|/answer|/establish)"))
            val strPredicate = { s: String -> s == "/call" || s == "/answer" || s == "/establish" }
            val p = result.indexOfFirst(strPredicate)
            if (p != -1) {
                result =
                    result.filterIndexed { index, string -> index <= p || !strPredicate(string) }
            }
            val messageChain = result.map { s ->
                messageUnit {
                    type = when (s) {
                        "/call" -> MessageOuterClass.MessageUnitType.CALL
                        "/answer" -> MessageOuterClass.MessageUnitType.ANSWER
                        "/establish" -> MessageOuterClass.MessageUnitType.ESTABLISH
                        else -> MessageOuterClass.MessageUnitType.TEXT
                    }
                    message = s
                }
            }
            return MessageChainIndexed(MessageChain(messageChain), p)
        }
    }

    override fun toString(): String {
        return messageChain.joinToString("") { it.message }
    }

    fun list(): List<MessageOuterClass.MessageUnit> {
        return this.messageChain
    }
}