package com.github.xiawusharve.webrtc.backend

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver

class PeerConnectionObserver: PeerConnection.Observer {
    private lateinit var signalExchangeObserver: SignalExchangeObserver
    companion object {
        const val TAG = "PeerConnectionObserver"
    }

    fun setSignalExchangeObserver(signalExchangeObserver: SignalExchangeObserver) {
        this.signalExchangeObserver = signalExchangeObserver
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Log.v(TAG, "onSignalingChange: ${newState?.name}")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Log.v(TAG, "onIceConnectionChange: ${newState?.name}")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.v(TAG, "onIceConnectionReceivingChange: $receiving")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Log.v(TAG, "onIceGatheringChange: ${newState?.name}")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "sending candidate")
        if (candidate != null) signalExchangeObserver.onCandidate(candidate)
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        super.onIceCandidateError(event)
        Log.e(TAG, "onIceCandidateError: ${event?.errorText}")
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate?>?) {
//        peerConnection.removeIceCandidates(candidates)
    }

    override fun onRemoveStream(stream: MediaStream?) {
        Log.v(TAG, "onRemoveStream")
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
        Log.v(TAG, "onDataChannel")
    }

    override fun onRenegotiationNeeded() {
        Log.v(TAG, "onRenegotiationNeeded")
    }

    // 如果你的信令仍使用 Plan B，可保留 onAddStream，但现代应用通常使用 Unified Plan，以 onTrack 为主
    override fun onAddStream(stream: MediaStream?) {
        Log.i(TAG, "onAddStream")
        stream?.audioTracks?.forEach { audioTrack ->
            Log.d(TAG, "远端音频轨道 ID: ${audioTrack.id()}")
            // 确保轨道可用（默认已启用，可省略）
            audioTrack.setEnabled(true)
        }
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        super.onTrack(transceiver)
        Log.i(TAG, "接收到远端轨道")

        val track = transceiver?.receiver?.track() ?: return
        Log.d(TAG, "轨道类型: ${track.kind()}, ID: ${track.id()}")

        if (track.kind() == "audio") {
            // 确保远端音频轨道被启用，WebRTC 会自动将音频路由到扬声器/听筒
            track.setEnabled(true)
            // 如需默认使用扬声器，可以在这里切换（需要 Context 获取 AudioManager）
//             enableSpeakerphone(context)
        }
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream?>?) {
        super.onAddTrack(receiver, mediaStreams)
        Log.i(TAG, "onAddTrack, 流数量: ${mediaStreams?.size}")
        mediaStreams?.forEach { stream ->
            stream?.audioTracks?.forEach { track ->
                Log.d(TAG, "onAddTrack 音频轨道 ID: ${track.id()}")
                track.setEnabled(true)
            }
        }
    }
}