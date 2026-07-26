package com.github.xiawusharve.webrtc.backend.message

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.ByteBuffer

data class Frame(
    val createdTime: Long,
    val payload: ByteArray,
) {
    companion object {
        fun parseFrom(buffer: ByteBuf): Frame {
            val createdTime = buffer.readLong()
            val len = buffer.readInt()
            val payload = ByteArray(len)
            buffer.readBytes(payload)
            return Frame(createdTime, payload)
        }
    }
    fun toByteBuf(): ByteBuf {
        val buffer = Unpooled.buffer(12 + payload.size)
            .writeLong(createdTime)
            .writeInt(payload.size)
            .writeBytes(payload)
        return buffer
    }
}
