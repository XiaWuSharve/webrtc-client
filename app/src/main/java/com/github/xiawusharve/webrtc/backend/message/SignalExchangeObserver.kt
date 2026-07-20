package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnection
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.audio.PeerConnectionObserver
import com.github.xiawusharve.webrtc.backend.audio.builder
import org.webrtc.IceCandidate
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class SignalExchangeObserver(
    private val peerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder,
    private val peerConnectionObserver: PeerConnectionObserver,
) {
    init {
        peerConnectionObserver.setSignalExchangeObserver(this)
    }
    companion object {
        private const val TAG = "SignalExchangeObserver"
    }
    private val candidates: ArrayList<IceCandidate> = ArrayList()
    private var candidateReady = false
    private lateinit var peerConnection: MyPeerConnection
    private lateinit var signalingClient: SignalingClient

    fun setPeerConnection(peerConnection: MyPeerConnection) {
        this.peerConnection = peerConnection
    }
    fun setSignalingClient(signalingClient: SignalingClient) {
        this.signalingClient = signalingClient
    }

    fun onReceiveCall(sdp: SessionDescription) {
        Log.d(TAG, "received call")
        peerConnection.setRemoteSdp(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                TODO("Not yet implemented")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "setRemoteSdp success")
                onAnswer()
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }
        }, sdp)
    }

    fun onReceiveAnswer(sdp: SessionDescription) {
        Log.d(TAG, "received answer")
        peerConnection.setRemoteSdp(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                TODO("Not yet implemented")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "set remote sdp success")
                Log.d(TAG, "adding cached candidates")
                candidateReady = true
                for (c in candidates) {
                    Log.d(TAG, "cached candidate: ${c.sdp}")
                    peerConnection.addCandidate(c)
                }
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }
        } , sdp)
    }

    fun onReceiveCandidate(candidate: IceCandidate) {
        Log.d(TAG, "received candidate, adding to peer connection")
        if (candidateReady) {
            peerConnection.addCandidate(candidate)
        } else {
            this.candidates.add(candidate)
        }
    }

    fun onConnected(code: Int) {
        Log.d(TAG, "connection established: $code")
        this.peerConnection = peerConnectionFactoryBuilder.createMyPeerConnection(peerConnectionObserver)
//        peerConnection.addTrack()
    }

    fun onCall() {
        Log.d(TAG, "onCall")
        // TODO sdp builder and 立体声 采样率 VGA等
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    peerConnection.setLocalSdp(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {
                            TODO("Not yet implemented")
                        }

                        override fun onSetSuccess() {
                            signalingClient.call(p0)
                        }

                        override fun onCreateFailure(error: String?) {
                            TODO("Not yet implemented")
                        }

                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "onSetFailure: $error")
                        }
                    },
                        builder(
                            p0
                        ).enableOpusDtx().setMaxAverageBitRate(12000).build()
                    )
                }
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailure")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun onAnswer() {
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    peerConnection.setLocalSdp(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {
                            TODO("Not yet implemented")
                        }

                        override fun onSetSuccess() {
                            signalingClient.answer(p0)
                            candidateReady = true
                            for (c in candidates) {
                                Log.d(TAG, "cached candidate: ${c.sdp}")
                                peerConnection.addCandidate(c)
                            }
                        }

                        override fun onCreateFailure(error: String?) {
                            TODO("Not yet implemented")
                        }

                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "onSetFailure: $error")
                        }
                    },
                        builder(
                            p0
                        ).enableOpusDtx().setMaxAverageBitRate(12000).build()
                    )
                }
            }

            override fun onSetSuccess() {
                TODO("Not yet implemented")
            }

            override fun onCreateFailure(p0: String?) {
                Log.e(TAG, "onCreateFailure")
            }

            override fun onSetFailure(p0: String?) {
                TODO("Not yet implemented")
            }
        })
    }

    fun onConnect() {
        TODO("Not yet implemented")
    }

    fun onCandidate(candidate: IceCandidate) {
        signalingClient.sendCandidate(candidate)
    }
}