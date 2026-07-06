package com.github.xiawusharve.webrtc

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class RtcClient(
    private val signalingClient: SignalingClient
): SdpExchangeObserverInterface, PeerConnection.Observer {
    private val TAG = "RtcClient"
    private val candidates: ArrayList<IceCandidate> = ArrayList()
    private var candidateReady = false
    private lateinit var peerConnection: PeerConnection
    private val answerObs = object : SdpObserver {
        val TAG = "answerObs"
        override fun onCreateSuccess(sdp: SessionDescription) {
            Log.d(TAG, "created sdp")
            Log.d(TAG, "setting local sdp")
            peerConnection.setLocalDescription(this)
            Log.d(TAG, "answering")
            signalingClient.answer(sdp)
        }

        override fun onSetSuccess() {
            Log.d(TAG, "set remote sdp success")
            Log.d(TAG, "adding cached candidates")
            candidateReady = true
            for (c in candidates) {
                peerConnection.addIceCandidate(c)
            }
        }

        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "create sdp failed: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.e(TAG, "set remote sdp failed: $error")
        }

    }
    private val offerObs = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            Log.d(TAG, "created offer")
            Log.d(TAG, "setting local sdp")
            peerConnection.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                }

                override fun onSetSuccess() {
                    Log.d(TAG, "set local sdp done")
                }

                override fun onCreateFailure(error: String?) {
                }

                override fun onSetFailure(error: String?) {
                    Log.e(TAG, "set local sdp failed: $error")
                }
            }, sdp)
            if (sdp == null) throw NullPointerException("sdp is null")
            signalingClient.call(sdp)
        }

        override fun onSetSuccess() {
            Log.d(TAG, "set remote sdp success")
            peerConnection.createAnswer(answerObs, MediaConstraints())
        }

        override fun onCreateFailure(error: String?) {
            Log.e(TAG, "create sdp failed: $error")
        }

        override fun onSetFailure(error: String?) {
            Log.e(TAG, "set sdp failed: $error")
        }
    }

    fun setPeerConnection(peerConnection: PeerConnection) {
        this.peerConnection = peerConnection
    }

    override fun onCall(sdp: SessionDescription) {
        Log.d(TAG, "received call")
        peerConnection.setRemoteDescription(offerObs, sdp)
    }

    override fun onAnswer(sdp: SessionDescription) {
        Log.d(TAG, "received answer")
        peerConnection.setRemoteDescription(answerObs, sdp)
    }

    override fun onCandidate(candidate: IceCandidate) {
        Log.d(TAG, "received candidate, adding to peer connection")
        if (candidateReady) {
            peerConnection.addIceCandidate(candidate)
        } else {
            this.candidates.add(candidate)
        }
    }

    override fun onConnect() {
        Log.d(TAG, "connection established")
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
        Log.v(TAG, "onSignalingChange")
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
        Log.v(TAG, "onIceConnectionChange")
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
        Log.v(TAG, "onIceConnectionReceivingChange")
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
        Log.v(TAG, "onIceGatheringChange")
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.d(TAG, "sending candidate")
        if (candidate == null) throw NullPointerException("candidate is null");
        signalingClient.sendCandidate(candidate)
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate?>?) {
        Log.v(TAG, "onIceCandidatesRemoved")
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

    fun call() {
        Log.d(TAG, "creating offer")
        peerConnection.createOffer(offerObs, MediaConstraints())
    }

    fun connect() {
        Log.d(TAG, "====START COMMUNICATION====")
        signalingClient.register()
    }
}