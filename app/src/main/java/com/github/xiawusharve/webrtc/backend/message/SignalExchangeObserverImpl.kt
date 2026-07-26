package com.github.xiawusharve.webrtc.backend.message

import android.util.Log
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnection
import com.github.xiawusharve.webrtc.backend.audio.MyPeerConnectionFactoryBuilder
import com.github.xiawusharve.webrtc.backend.audio.PeerConnectionObserverImpl
import com.github.xiawusharve.webrtc.backend.audio.builder
import message.MessageOuterClass
import org.webrtc.IceCandidate
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class SignalExchangeObserverImpl(
    private val peerConnectionFactoryBuilder: MyPeerConnectionFactoryBuilder,
    private val signalingClient: WebSocketSignalingClient
): SignalExchangeObserver {
    companion object {
        private const val TAG = "SignalExchangeObserver"
    }
    private val candidates: ArrayList<IceCandidate> = ArrayList()
    private var candidateReady = false
    private lateinit var peerConnection: MyPeerConnection

    fun setPeerConnection(peerConnection: MyPeerConnection) {
        this.peerConnection = peerConnection
    }

    override fun onReceiveCall(sdp: SessionDescription) {
        Log.d(TAG, "received call")
        this.peerConnection = peerConnectionFactoryBuilder.createMyPeerConnection(
            PeerConnectionObserverImpl(this)
        )
//        peerConnection.addTrack()

        peerConnection.setRemoteSdp(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                TODO("Not yet implemented")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "setRemoteSdp success")
            }

            override fun onCreateFailure(p0: String?) {
                TODO("Not yet implemented")
            }

            override fun onSetFailure(p0: String?) {
                Log.e(TAG, "onSetFailure: $p0")
            }
        }, sdp)
    }

    override fun onReceiveAnswer(sdp: SessionDescription) {
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

    override fun onReceiveCandidate(candidate: IceCandidate) {
        Log.d(TAG, "received candidate, adding to peer connection")
        if (candidateReady) {
            peerConnection.addCandidate(candidate)
        } else {
            this.candidates.add(candidate)
        }
    }

    override fun onSendCandidate(candidate: IceCandidate) {
        signalingClient.sendCandidate(candidate)
    }

    override fun onRemoveCandidate(p0: Array<out IceCandidate?>?) {
        peerConnection.removeCandidate(p0)
    }

    override fun onReceiveEstablish() {
        Log.d(TAG, "onReceiveEstablish")
    }

    override fun onCall(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>) {
        Log.d(TAG, "onCall")
        this.peerConnection = peerConnectionFactoryBuilder.createMyPeerConnection(
            PeerConnectionObserverImpl(this)
        )
        // TODO sdp builder and 立体声 采样率 VGA等
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    val s = builder(p0).enableOpusDtx().setMaxAverageBitRate(12000).build()
                    peerConnection.setLocalSdp(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {}
                        override fun onSetSuccess() {
                            signalingClient.sendChatMessage(cb(s.description))
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "onSetFailure: $error")
                        }
                    },s)
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

    override fun onAnswer(cb: (sdp: String) -> List<MessageOuterClass.MessageUnit>) {
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
                if (p0 != null) {
                    val s = builder(p0).enableOpusDtx().setMaxAverageBitRate(12000).build()
                    peerConnection.setLocalSdp(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {}
                        override fun onSetSuccess() {
                            signalingClient.sendChatMessage(cb(s.description))
                            candidateReady = true
                            for (c in candidates) {
                                Log.d(TAG, "cached candidate: ${c.sdp}")
                                peerConnection.addCandidate(c)
                            }
                        }
                        override fun onCreateFailure(error: String?) {}
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "onSetFailure: $error")
                        }
                    },s)
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

    override fun onEstablish(cb: () -> List<MessageOuterClass.MessageUnit>) {
        signalingClient.sendChatMessage(cb())
    }
}