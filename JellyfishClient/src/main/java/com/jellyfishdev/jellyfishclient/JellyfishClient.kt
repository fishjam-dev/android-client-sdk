package com.jellyfishdev.jellyfishclient

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.membraneframework.rtc.BuildConfig
import org.membraneframework.rtc.MembraneRTC
import org.membraneframework.rtc.MembraneRTCListener
import org.membraneframework.rtc.TrackEncoding
import org.membraneframework.rtc.media.LocalAudioTrack
import org.membraneframework.rtc.media.LocalScreencastTrack
import org.membraneframework.rtc.media.LocalVideoTrack
import org.membraneframework.rtc.media.TrackBandwidthLimit
import org.membraneframework.rtc.media.VideoParameters
import org.membraneframework.rtc.models.Peer
import org.membraneframework.rtc.models.RTCStats
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.utils.Metadata
import org.membraneframework.rtc.utils.SerializedMediaEvent
import org.membraneframework.rtc.utils.TimberDebugTree
import org.webrtc.Logging
import timber.log.Timber

data class Config (
    val websocketUrl: String,
    val token: String,
)

class JellyfishClient(appContext: Context, val listener: JellyfishClientListener) : MembraneRTCListener {
    private var webSocket: WebSocket? = null
    private val webrtcClient = MembraneRTC.create(appContext, this)
    private val gson = Gson()

    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    /**
     * Connects to the server using the WebSocket connection
     *
     * @param config - Configuration object for the client
     */
    fun connect(config: Config) {
        val request = Request.Builder().url(config.websocketUrl).build()
        val webSocket = OkHttpClient().newWebSocket(request, object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onSocketClose(code, reason)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                when(val event = ReceivableEvent.decode(text)) {
                    is AuthenticatedEvent -> {
                        listener.onAuthSuccess()
                    }
                    is UnauthenticatedEvent -> {
                        listener.onAuthError()
                    }
                    is ReceivableMediaEvent -> {
                        receiveEvent(event)
                    }
                    else -> {}
                }
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onSocketOpen()
                sendEvent(AuthRequest(config.token))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onSocketError(t, response)
            }
        })

        this.webSocket = webSocket
    }

    /**
     * Leaves the room. This function should be called when user leaves the room in a clean way e.g. by clicking a
     * dedicated, custom button `disconnect`. As a result there will be generated one more media event that should be sent
     * to the RTC Engine. Thanks to it each other peer will be notified that peer left in {@link MessageEvents.onPeerLeft},
     */
    fun leave() {
        webrtcClient.disconnect()
    }

    /**
     * Disconnect from the room, and close the websocket connection. Tries to leave the room gracefully, but if it fails,
     * it will close the websocket anyway.
     */
    fun cleanUp() {
        webrtcClient.disconnect()
        webSocket?.close(1000, null)
        webSocket = null
        listener.onDisconnected()
    }

    private fun sendEvent(event: SendableEvent) {
        webSocket?.send(gson.toJson(event))
    }

    private fun receiveEvent(event: ReceivableMediaEvent) {
        webrtcClient.receiveMediaEvent(event.data)
    }

    /**
     * Tries to join the room. If user is accepted then {@link JellyfishClient.onJoinSuccess} will be called.
     * In other case {@link JellyfishClient.onJoinError} is invoked.
     * <p>
     * @param peerMetadata - Any information that other peers will receive in onPeerJoined
     * after accepting this peer
     */
    fun join(peerMetadata: Metadata = emptyMap()) {
        webrtcClient.join(peerMetadata)
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
        sendEvent(SendableMediaEvent(event))
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

    /**
     * Creates a video track utilizing device's camera.
     * <p>
     * The client assumes that the user has already granted camera permissions.
     *
     * @param videoParameters a set of target parameters such as camera resolution, frame rate or simulcast configuration
     * @param metadata the metadata that will be sent to the <strong>Membrane RTC Engine</strong> for media negotiation
     * @param captureDeviceName the name of the device to start video capture with, you can get device name by using
     * `LocalVideoTrack.getCaptureDevices` method
     * @return an instance of the video track
     */
    fun createVideoTrack(
        videoParameters: VideoParameters,
        metadata: Metadata,
        captureDeviceName: String? = null
    ): LocalVideoTrack {
        return webrtcClient.createVideoTrack(videoParameters, metadata, captureDeviceName)
    }

    /**
     * Creates an audio track utilizing device's microphone.
     * <p>
     * The client assumes that the user has already granted microphone recording permissions.
     *
     * @param metadata the metadata that will be sent to the <strong>Membrane RTC Engine</strong> for media negotiation
     * @return an instance of the audio track
     */
    fun createAudioTrack(metadata: Metadata): LocalAudioTrack {
        return webrtcClient.createAudioTrack(metadata)
    }

    /**
     * Creates a screen track recording the entire device's screen.
     * <p>
     * The method requires a media projection permission to be able to start the recording. The client assumes that the intent is valid.
     *
     * @param mediaProjectionPermission a valid media projection permission intent that can be used to starting a screen capture
     * @param videoParameters a set of target parameters of the screen capture such as resolution, frame rate or simulcast configuration
     * @param metadata the metadata that will be sent to the <strong>Membrane RTC Engine</strong> for media negotiation
     * @param onEnd callback that will be invoked once the screen capture ends
     * @return an instance of the screencast track
     */
    fun createScreencastTrack(
        mediaProjectionPermission: Intent,
        videoParameters: VideoParameters,
        metadata: Metadata,
        onEnd: (() -> Unit)? = null
    ): LocalScreencastTrack? {
        return webrtcClient.createScreencastTrack(mediaProjectionPermission, videoParameters, metadata, onEnd)
    }

    /**
     * Removes an instance of local track from the client.
     *
     * @param trackId an id of a valid local track that has been created using the current client
     * @return a boolean whether the track has been successfully removed or not
     */
    fun removeTrack(trackId: String): Boolean {
        return webrtcClient.removeTrack(trackId)
    }

    /**
     * Sets track encoding that server should send to the client library.
     *
     * The encoding will be sent whenever it is available.
     * If chosen encoding is temporarily unavailable, some other encoding
     * will be sent until chosen encoding becomes active again.
     *
     * @param trackId an id of a remote track
     * @param encoding an encoding to receive
     */
    fun setTargetTrackEncoding(trackId: String, encoding: TrackEncoding) {
        webrtcClient.setTargetTrackEncoding(trackId, encoding)
    }

    /**
     * Enables track encoding so that it will be sent to the server.
     *
     * @param trackId an id of a local track
     * @param encoding an encoding that will be enabled
     */
    fun enableTrackEncoding(trackId: String, encoding: TrackEncoding) {
        webrtcClient.enableTrackEncoding(trackId, encoding)
    }

    /**
     * Disables track encoding so that it will be no longer sent to the server.
     *
     * @param trackId and id of a local track
     * @param encoding an encoding that will be disabled
     */
    fun disableTrackEncoding(trackId: String, encoding: TrackEncoding) {
        webrtcClient.disableTrackEncoding(trackId, encoding)
    }

    /**
     * Updates the metadata for the current peer.
     * @param peerMetadata Data about this peer that other peers will receive upon joining.
     *
     * If the metadata is different from what is already tracked in the room, the optional
     * callback `onPeerUpdated` will be triggered for other peers in the room.
     */
    fun updatePeerMetadata(peerMetadata: Metadata) {
        webrtcClient.updatePeerMetadata(peerMetadata)
    }

    /**
     * Updates the metadata for a specific track.
     * @param trackId local track id of audio or video track.
     * @param trackMetadata Data about this track that other peers will receive upon joining.
     *
     * If the metadata is different from what is already tracked in the room, the optional
     * callback `onTrackUpdated` will be triggered for other peers in the room.
     */
    fun updateTrackMetadata(trackId: String, trackMetadata: Metadata) {
        webrtcClient.updateTrackMetadata(trackId, trackMetadata)
    }

    /**
     * Updates maximum bandwidth for the track identified by trackId.
     * This value directly translates to quality of the stream and, in case of video, to the amount of RTP packets being sent.
     * In case trackId points at the simulcast track bandwidth is split between all of the variant streams proportionally to their resolution.
     * @param trackId track id of a video track
     * @param bandwidthLimit bandwidth in kbps
     */
    fun setTrackBandwidth(trackId: String, bandwidthLimit: TrackBandwidthLimit.BandwidthLimit) {
        webrtcClient.setTrackBandwidth(trackId, bandwidthLimit)
    }

    /**
     * Updates maximum bandwidth for the given simulcast encoding of the given track.
     * @param trackId track id of a video track
     * @param encoding rid of the encoding
     * @param bandwidthLimit bandwidth in kbps
     */
    fun setEncodingBandwidth(
        trackId: String,
        encoding: String,
        bandwidthLimit: TrackBandwidthLimit.BandwidthLimit
    ) {
        webrtcClient.setEncodingBandwidth(trackId, encoding, bandwidthLimit)
    }

    /**
     * Changes severity level of debug logs
     * @param severity enum value representing the logging severity
     */
    fun changeWebRTCLoggingSeverity(severity: Logging.Severity) {
        webrtcClient.changeWebRTCLoggingSeverity(severity)
    }

    /**
     * Returns current connection stats
     * @return a map containing statistics
     */
    fun getStats(): Map<String, RTCStats> {
        return webrtcClient.getStats()
    }
}

