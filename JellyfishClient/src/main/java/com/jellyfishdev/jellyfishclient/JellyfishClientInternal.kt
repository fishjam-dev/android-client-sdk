package com.jellyfishdev.jellyfishclient

import android.content.Context
import jellyfish.PeerNotifications.PeerMessage
import jellyfish.PeerNotifications.PeerMessage.MediaEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.membraneframework.rtc.BuildConfig
import org.membraneframework.rtc.MembraneRTC
import org.membraneframework.rtc.MembraneRTCListener
import org.membraneframework.rtc.models.Peer
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.utils.SerializedMediaEvent
import org.membraneframework.rtc.utils.TimberDebugTree
import timber.log.Timber

internal class JellyfishClientInternal(
    appContext: Context,
    private val listener: JellyfishClientListener,
) :
    MembraneRTCListener {
    private var webSocket: WebSocket? = null
    val webrtcClient = MembraneRTC.create(appContext, this)

    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    fun connect(config: Config) {
        val request = Request.Builder().url(config.websocketUrl).build()
        val webSocket = OkHttpClient().newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onSocketClose(code, reason)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    try {
                        val peerMessage = PeerMessage.parseFrom(bytes.toByteArray())
                        if (peerMessage.hasAuthenticated()) {
                            listener.onAuthSuccess()
                        } else if (peerMessage.hasMediaEvent()) {
                            receiveEvent(peerMessage.mediaEvent.data)
                        } else {
                            Timber.w("Received unexpected websocket message: $peerMessage")
                        }
                    } catch (e: Exception) {
                        Timber.e("Received invalid websocket message", e)
                    }
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    listener.onSocketOpen()
                    val authRequest = PeerMessage
                        .newBuilder()
                        .setAuthRequest(PeerMessage.AuthRequest.newBuilder().setToken(config.token))
                        .build()
                    sendEvent(authRequest)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    listener.onSocketError(t, response)
                }
            },
        )

        this.webSocket = webSocket
    }

    fun leave() {
        webrtcClient.disconnect()
    }

    fun cleanUp() {
        webrtcClient.disconnect()
        webSocket?.close(1000, null)
        webSocket = null
        listener.onDisconnected()
    }

    private fun sendEvent(peerMessage: PeerMessage) {
        webSocket?.send(peerMessage.toByteArray().toByteString())
    }

    private fun receiveEvent(event: SerializedMediaEvent) {
        webrtcClient.receiveMediaEvent(event)
    }

    override fun onJoinError(metadata: Any) {
        listener.onJoinError(metadata)
    }

    override fun onJoinSuccess(peerID: String, peersInRoom: List<Peer>) {
        listener.onJoinSuccess(peerID, peersInRoom)
    }

    override fun onPeerJoined(peer: Peer) {
        listener.onPeerJoined(peer)
    }

    override fun onPeerLeft(peer: Peer) {
        listener.onPeerLeft(peer)
    }

    override fun onPeerUpdated(peer: Peer) {
        listener.onPeerUpdated(peer)
    }

    override fun onSendMediaEvent(event: SerializedMediaEvent) {
        val mediaEvent = PeerMessage
            .newBuilder()
            .setMediaEvent(MediaEvent.newBuilder().setData(event))
            .build()
        sendEvent(mediaEvent)
    }

    override fun onTrackAdded(ctx: TrackContext) {
        listener.onTrackAdded(ctx)
    }

    override fun onTrackReady(ctx: TrackContext) {
        listener.onTrackReady(ctx)
    }

    override fun onTrackRemoved(ctx: TrackContext) {
        listener.onTrackRemoved(ctx)
    }

    override fun onTrackUpdated(ctx: TrackContext) {
        listener.onTrackUpdated(ctx)
    }

    override fun onRemoved(reason: String) {
        listener.onRemoved(reason)
    }

    override fun onBandwidthEstimationChanged(estimation: Long) {
        listener.onBandwidthEstimationChanged(estimation)
    }
}
