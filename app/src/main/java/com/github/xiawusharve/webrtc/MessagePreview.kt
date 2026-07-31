package com.github.xiawusharve.webrtc

import com.github.xiawusharve.webrtc.backend.message.MessageChain
import java.util.Date

data class MessagePreview(
    val displayName: String,
    val time: Date,
    val messageChain: MessageChain,
) {
    companion object {
        fun parseFrom(s: String): MessagePreview {
            val strings = s.split(Regex("(?<!\\\\)(?:\\\\\\\\)*:")).map { rawS ->
                rawS.replace("\\\\", "\u0000")   // \\ -> 临时占位符
                    .replace("\\:", ":")         // \: -> :
                    .replace("\u0000", "\\")
            }
            val time: Date = try {
                Date(strings[1].toLong())
            } catch (e: NumberFormatException) {
                Date()
            }
            return MessagePreview(
                strings[0],
                time,
                MessageChain.parseFrom(strings[2]).messageChain
            )
        }
    }
    override fun toString(): String {
        val displayName = displayName
            .replace("\\", "\\\\")
            .replace(":", "\\:")
        val time = time.time.toString()
        val messageChain = messageChain.toString()
            .replace("\\", "\\\\")
            .replace(":", "\\:")
        return "$displayName:$time:$messageChain"
    }
}