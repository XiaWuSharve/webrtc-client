package com.github.xiawusharve.webrtc

import android.util.Log
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class RtcClient(
    private val myWebSocketClient: MyWebSocketClient
): SdpExchangeObserverInterface, PeerConnection.Observer {
    private val TAG = "RtcClient"
    private val candidates: ArrayList<IceCandidate> = ArrayList()
    private var candidateReady = false
    private var peerConnection: PeerConnection? = null
    private val answerObs = object : SdpObserver {
        val TAG = "answerObs"
        override fun onCreateSuccess(sdp: SessionDescription) {
            Log.i(TAG, "onCreateSuccess")
            peerConnection?.setLocalDescription(this)
            myWebSocketClient.answer(sdp)
        }

        override fun onSetSuccess() {
            Log.i(TAG, "set local description success")
            candidateReady = true
            for (c in candidates) {
                peerConnection?.addIceCandidate(c)
            }
        }

        override fun onCreateFailure(error: String?) {
        }

        override fun onSetFailure(error: String?) {
        }

    }
    private val offerObs = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            Log.i(TAG, "onCreateSuccess")
            peerConnection?.setLocalDescription(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                }

                override fun onSetSuccess() {
                    Log.i(TAG, "onSetSuccess")
                }

                override fun onCreateFailure(error: String?) {
                }

                override fun onSetFailure(error: String?) {
                    Log.i(TAG, "onSetFailure")
                }
            }, sdp)
            if (sdp == null) throw NullPointerException("sdp is null")
            myWebSocketClient.call(sdp)
        }

        override fun onSetSuccess() {
            Log.i(TAG, "set remote description success")
            peerConnection?.createAnswer(answerObs, MediaConstraints())
        }

        override fun onCreateFailure(error: String?) {
        }

        override fun onSetFailure(error: String?) {
        }

    }

    fun setPeerConnection(peerConnection: PeerConnection) {
        this.peerConnection = peerConnection
    }

    override fun onCall(sdp: SessionDescription) {
        Log.i(TAG, "onCall")
        peerConnection?.setRemoteDescription(offerObs, sdp)
    }

    override fun onAnswer(sdp: SessionDescription) {
        Log.i(TAG, "onAnswer")
        peerConnection?.setRemoteDescription(answerObs, sdp)
    }

    override fun onCandidate(candidate: IceCandidate) {
        Log.i(TAG, "onCandidate")
        if (candidateReady) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            this.candidates.add(candidate)
        }
    }

    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
    }

    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
    }

    override fun onIceConnectionReceivingChange(receiving: Boolean) {
    }

    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {
    }

    override fun onIceCandidate(candidate: IceCandidate?) {
        Log.i(TAG, "onIceCandidate")
        if (candidate == null) throw NullPointerException("candidate is null");
        myWebSocketClient.sendCandidate(candidate)
    }

    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate?>?) {

    }

    override fun onAddStream(stream: MediaStream?) {
        Log.i(TAG, "onAddStream: 接收到远端流")

        // 如果你将来想单独控制某个音频轨道（例如静音远端），可以获取它
        stream?.audioTracks?.forEach { audioTrack ->
            Log.d(TAG, "远端音频轨道 ID: ${audioTrack.id()}")
            // 注意：这里不需要调用 audioTrack.play() 或类似方法
        }
    }

    override fun onRemoveStream(stream: MediaStream?) {
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
    }

    override fun onRenegotiationNeeded() {
    }

    fun call() {
        Log.i(TAG, "call")
        peerConnection?.createOffer(offerObs, MediaConstraints())
    }
}