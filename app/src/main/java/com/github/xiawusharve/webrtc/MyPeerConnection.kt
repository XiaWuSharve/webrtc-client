package com.github.xiawusharve.webrtc

import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.util.Collections

class MyPeerConnection(
    private val peerConnection: PeerConnection,
    private val audioTrack: AudioTrack,
    private val offerConstraints: MediaConstraints,
    private val answerConstraints: MediaConstraints
) {
    fun addTrack(): RtpTransceiver {
        return peerConnection.addTransceiver(audioTrack)
    }

    fun setLocalSdp(localSdpObserver: SdpObserver, sdp: SessionDescription) {
        peerConnection.setLocalDescription(localSdpObserver, sdp)
    }

    fun createOffer(offerObserver: SdpObserver) {
        peerConnection.createOffer(offerObserver, offerConstraints)
    }

    fun createAnswer(answerObserver: SdpObserver) {
        peerConnection.createAnswer(answerObserver, answerConstraints)
    }

    fun setRemoteSdp(remoteSdpObserver: SdpObserver, sdp: SessionDescription) {
        peerConnection.setRemoteDescription(remoteSdpObserver, sdp)
    }

    fun addCandidate(candidate: IceCandidate) {
        peerConnection.addIceCandidate(candidate)
    }
}