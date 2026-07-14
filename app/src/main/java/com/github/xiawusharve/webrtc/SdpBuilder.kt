package com.github.xiawusharve.webrtc

import android.util.Log
import org.webrtc.SessionDescription

fun builder(sessionDescription: SessionDescription): SdpBuilder {
    return SdpBuilder(sessionDescription)
}

class SdpBuilder(
    private val sessionDescription: SessionDescription
) {
    companion object {
        const val TAG = "SdpBuilder"
    }
    private var description = sessionDescription.description

    fun setMaxAverageBitRate(bitRate: Int): SdpBuilder {
        // 1. find Opus's negotiated payload type (it is dynamic, never assume 111)
        val rtpmap = Regex("a=rtpmap:(\\d+) opus/(\\d+)", RegexOption.IGNORE_CASE).find(description)
            ?: return this // no Opus offered; leave SDP untouched

        val pt = rtpmap.groupValues[1]

        // 2. locate that payload type's existing fmtp line
        val fmtpRe = Regex("a=fmtp:$pt (.*)")
        val fmtp = fmtpRe.find(description)

        if (fmtp == null) {
            // no fmtp line yet: add one immediately after the rtpmap, preserving m-line order
            description = description.replace(rtpmap.value, "${rtpmap.value}\r\na=fmtp:$pt maxaveragebitrate=$bitRate")
            return this
        }

        val maxaveragebitrateRe = Regex("maxaveragebitrate=(\\d+)")
        val maxaveragebitrate = maxaveragebitrateRe.find(fmtp.groupValues[1])
        if (maxaveragebitrate == null) {
            // 3. append the param to the existing list, leaving order and BUNDLE intact
            description = description.replace(fmtpRe, "a=fmtp:$pt ${fmtp.groupValues[1]};maxaveragebitrate=$bitRate")
        } else {
            description = description.replace(maxaveragebitrateRe, "maxaveragebitrate=$bitRate")
        }

        return this
    }
    fun enableOpusDtx(): SdpBuilder {
        // 1. find Opus's negotiated payload type (it is dynamic, never assume 111)
        val rtpmap = Regex("a=rtpmap:(\\d+) opus/(\\d+)", RegexOption.IGNORE_CASE).find(description)
            ?: return this // no Opus offered; leave SDP untouched

        val pt = rtpmap.groupValues[1]

        // 2. locate that payload type's existing fmtp line
        val fmtpRe = Regex("a=fmtp:$pt (.*)")
        val fmtp = fmtpRe.find(description)

        if (fmtp == null) {
            // no fmtp line yet: add one immediately after the rtpmap, preserving m-line order
            description = description.replace(rtpmap.value, "${rtpmap.value}\r\na=fmtp:$pt usedtx=1")
            return this
        }

        if (fmtp.groupValues[1].contains("usedtx=")) return this // already set; idempotent

        // 3. append the param to the existing list, leaving order and BUNDLE intact
        description = description.replace(fmtpRe, "a=fmtp:$pt ${fmtp.groupValues[1]};usedtx=1")
        return this
    }

    fun build(): SessionDescription {
        Log.d(TAG, description.toString())
        return SessionDescription(sessionDescription.type, description.toString())
    }
}