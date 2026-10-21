package com.ghostmago.app.mesh

import android.content.Context
import android.util.Log
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WhatsApp-style WebRTC voice over local network.
 * Signaling (SDP / ICE / ring) is transported by the mesh layer.
 */
class WebRtcVoiceCall(
    private val context: Context,
    private val onLocalSdp: (type: String, sdp: String) -> Unit,
    private val onIce: (mid: String, index: Int, candidate: String) -> Unit,
    private val onState: (String) -> Unit
) {
    companion object {
        private const val TAG = "WebRtcVoice"
        @Volatile private var factoryReady = false
        private val initLock = Any()

        fun ensureFactory(context: Context): PeerConnectionFactory? {
            synchronized(initLock) {
                return try {
                    if (!factoryReady) {
                        val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                            .setEnableInternalTracer(false)
                            .createInitializationOptions()
                        PeerConnectionFactory.initialize(options)
                        factoryReady = true
                    }
                    val egl = EglBase.create()
                    val adm = JavaAudioDeviceModule.builder(context.applicationContext)
                        .setUseHardwareAcousticEchoCanceler(true)
                        .setUseHardwareNoiseSuppressor(true)
                        .createAudioDeviceModule()
                    PeerConnectionFactory.builder()
                        .setAudioDeviceModule(adm)
                        .setVideoEncoderFactory(DefaultVideoEncoderFactory(egl.eglBaseContext, true, true))
                        .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
                        .createPeerConnectionFactory()
                } catch (e: Exception) {
                    Log.e(TAG, "Factory init failed", e)
                    null
                }
            }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudio: AudioTrack? = null
    private val closed = AtomicBoolean(false)
    private val pendingIce = mutableListOf<IceCandidate>()
    private var remoteSet = false

    fun startAsCaller() {
        executor.execute {
            try {
                if (!setupPeer()) return@execute
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                }
                pc?.createOffer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        if (desc == null) return
                        pc?.setLocalDescription(SimpleSdpObserver(), desc)
                        onLocalSdp(desc.type.canonicalForm(), desc.description)
                        onState("offer_sent")
                    }
                }, constraints)
            } catch (e: Exception) {
                Log.e(TAG, "startAsCaller", e)
                onState("error:${e.message}")
            }
        }
    }

    fun startAsCallee() {
        executor.execute {
            try {
                if (!setupPeer()) return@execute
                onState("ready_callee")
            } catch (e: Exception) {
                Log.e(TAG, "startAsCallee", e)
                onState("error:${e.message}")
            }
        }
    }

    fun handleRemoteSdp(type: String, sdp: String) {
        executor.execute {
            try {
                if (pc == null && !setupPeer()) return@execute
                val sdpType = if (type.equals("offer", true)) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
                val remote = SessionDescription(sdpType, sdp)
                pc?.setRemoteDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        remoteSet = true
                        flushIce()
                        if (sdpType == SessionDescription.Type.OFFER) {
                            val constraints = MediaConstraints().apply {
                                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                            }
                            pc?.createAnswer(object : SimpleSdpObserver() {
                                override fun onCreateSuccess(desc: SessionDescription?) {
                                    if (desc == null) return
                                    pc?.setLocalDescription(SimpleSdpObserver(), desc)
                                    onLocalSdp(desc.type.canonicalForm(), desc.description)
                                    onState("answer_sent")
                                }
                            }, constraints)
                        } else {
                            onState("connected_signal")
                        }
                    }
                }, remote)
            } catch (e: Exception) {
                Log.e(TAG, "handleRemoteSdp", e)
                onState("error:${e.message}")
            }
        }
    }

    fun handleRemoteIce(mid: String, index: Int, candidate: String) {
        executor.execute {
            try {
                val ice = IceCandidate(mid, index, candidate)
                if (remoteSet) {
                    pc?.addIceCandidate(ice)
                } else {
                    pendingIce.add(ice)
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleRemoteIce", e)
            }
        }
    }

    fun setMuted(muted: Boolean) {
        executor.execute {
            localAudio?.setEnabled(!muted)
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        executor.execute {
            try {
                localAudio?.dispose()
                audioSource?.dispose()
                pc?.close()
                pc?.dispose()
            } catch (_: Exception) {
            } finally {
                localAudio = null
                audioSource = null
                pc = null
                onState("closed")
            }
        }
    }

    private fun flushIce() {
        for (c in pendingIce) {
            try { pc?.addIceCandidate(c) } catch (_: Exception) {}
        }
        pendingIce.clear()
    }

    private fun setupPeer(): Boolean {
        if (closed.get()) return false
        factory = ensureFactory(context) ?: return false.also { onState("error:webrtc_init") }
        val iceServers = listOf(
            // Host candidates on same Wi-Fi work without STUN; STUN helps some OEMs
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
        }
        pc = factory!!.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                onState("ice:${state?.name}")
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate == null) return
                onIce(candidate.sdpMid ?: "", candidate.sdpMLineIndex, candidate.sdp)
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                onState("remote_track")
            }
        })
        if (pc == null) {
            onState("error:pc_null")
            return false
        }
        val audioConstraints = MediaConstraints()
        audioSource = factory!!.createAudioSource(audioConstraints)
        localAudio = factory!!.createAudioTrack("AUDIO_LOCAL", audioSource)
        localAudio?.setEnabled(true)
        pc?.addTrack(localAudio, listOf("stream1"))
        onState("pc_ready")
        return true
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) { Log.e(TAG, "sdp create fail: $error") }
        override fun onSetFailure(error: String?) { Log.e(TAG, "sdp set fail: $error") }
    }
}
