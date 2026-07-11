package com.github.xiawusharve.webrtc

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

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

    override fun onAddStream(stream: MediaStream?) {
        Log.i(TAG, "接收到远端流")

        // 如果你将来想单独控制某个音频轨道（例如静音远端），可以获取它
        stream?.audioTracks?.forEach { audioTrack ->
            Log.d(TAG, "远端音频轨道 ID: ${audioTrack.id()}")
            // 注意：这里不需要调用 audioTrack.play() 或类似方法
        }
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
}