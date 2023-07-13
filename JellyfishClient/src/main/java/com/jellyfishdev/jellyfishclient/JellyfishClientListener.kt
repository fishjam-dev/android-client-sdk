package com.jellyfishdev.jellyfishclient

import okhttp3.Response
import org.membraneframework.rtc.models.Endpoint
import org.membraneframework.rtc.models.TrackContext
import timber.log.Timber

interface JellyfishClientListener {
    /**
     * Emitted when the websocket connection is closed
     */
    fun onSocketClose(code: Int, reason: String) {
        Timber.i("Socket was closed with code $code reason: $reason")
    }

    /**
     * Emitted when occurs an error in the websocket connection
     */
    fun onSocketError(t: Throwable, response: Response?) {
        Timber.w("Socket error:", t, response)
    }

    /**
     * Emitted when the websocket connection is opened
     */
    fun onSocketOpen() {
        Timber.i("Socket opened")
    }

    /**
     * Emitted when authentication is successful
     */
    fun onAuthSuccess()

    /**
     * Emitted when authentication fails
     */
    fun onAuthError()

    /**
     * Emitted when local user is connected to jellyfish.
     */
    fun onConnected(endpointID: String, otherEndpoints: List<Endpoint>)

    /**
     * Emitted when there was an error while connecting to the jellyfish=.
     */
    fun onConnectError(metadata: Any)

    /**
     * Emitted when the connection is closed
     */
    fun onDisconnected() {
        Timber.i("Client disconnected")
    }

    /**
     * Called each time new endpoint joins the room.
     */
    fun onEndpointAdded(endpoint: Endpoint)

    /**
     * Called each time endpoint leaves the room.
     */
    fun onEndpointRemoved(endpoint: Endpoint)

    /**
     * Called each time endpoint has its metadata updated.
     */
    fun onEndpointUpdated(endpoint: Endpoint)

    /**
     * Called when data in a new track arrives.
     *
     * This callback is always called after {@link JellyfishClientListener.onTrackAdded}.
     * It informs user that data related to the given track arrives and can be played or displayed.
     */
    fun onTrackReady(ctx: TrackContext)

    /**
     * Called each time the endpoint which was already in the room, adds new track. Fields track and stream will be set to null.
     * These fields will be set to non-null value in {@link JellyfishClientListener.onTrackReady}
     */
    fun onTrackAdded(ctx: TrackContext) {
        Timber.i("Track ${ctx.trackId} added")
    }

    /**
     * Called when some track will no longer be sent.
     *
     * It will also be called before {@link JellyfishClientListener.onEndpointRemoved} for each track of this endpoint.
     */
    fun onTrackRemoved(ctx: TrackContext)

    /**
     * Called each time endpoint has its track metadata updated.
     */
    fun onTrackUpdated(ctx: TrackContext) {
        Timber.i("Track ${ctx.trackId} updated")
    }

    /**
     * Called every time the server estimates client's bandwidth.
     *
     * @param estimation - client's available incoming bitrate estimated
     * by the server. It's measured in bits per second.
     */
    fun onBandwidthEstimationChanged(estimation: Long) {
        Timber.i("Bandwidth estimation changed: $estimation")
    }
}
