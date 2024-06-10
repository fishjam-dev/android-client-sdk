package org.membraneframework.rtc

import android.util.Log
import com.google.gson.reflect.TypeToken
import org.membraneframework.rtc.events.*
import org.membraneframework.rtc.models.Endpoint
import org.membraneframework.rtc.utils.Metadata
import org.membraneframework.rtc.utils.SerializedMediaEvent
import timber.log.Timber
import kotlin.math.roundToLong
const val TA = "RTCEC"
internal class RTCEngineCommunication(
    private val engineListener: RTCEngineListener
) {
    interface RTCEngineCommunicationFactory {
        fun create(listener: RTCEngineListener): RTCEngineCommunication
    }

    fun connect(endpointMetadata: Metadata) {
        Log.i(TA, "connect")
        Log.i(TA, endpointMetadata.toString())
        Log.i(TA,"")
        sendEvent(Connect(endpointMetadata))
    }

    fun updateEndpointMetadata(endpointMetadata: Metadata) {
        Log.i(TA, "updateEndpointMetadata")
        Log.i(TA, endpointMetadata.toString())
        Log.i(TA,"")
        sendEvent(UpdateEndpointMetadata(endpointMetadata))
    }

    fun updateTrackMetadata(
        trackId: String,
        trackMetadata: Metadata
    ) {
        Log.i(TA, "updateTrackMetadata")
        Log.i(TA, trackId + " "+ trackMetadata.toString())
        Log.i(TA,"")
        sendEvent(UpdateTrackMetadata(trackId, trackMetadata))
    }

    fun setTargetTrackEncoding(
        trackId: String,
        encoding: TrackEncoding
    ) {
        Log.i(TA, "setTargetTrackEncoding")
        Log.i(TA, trackId + " " + encoding.toString())
        Log.i(TA,"")
        sendEvent(
            SelectEncoding(
                trackId,
                encoding.rid
            )
        )
    }

    fun renegotiateTracks() {
        Log.i(TA, "renegotiateTracks")
        Log.i(TA,"")
        sendEvent(RenegotiateTracks())
    }

    fun localCandidate(
        sdp: String,
        sdpMLineIndex: Int
    ) {
        Log.i(TA, "localCandidate")
        Log.i(TA, sdp + " " + sdpMLineIndex)
        Log.i(TA,"")
        sendEvent(
            LocalCandidate(
                sdp,
                sdpMLineIndex
            )
        )
    }

    fun sdpOffer(
        sdp: String,
        trackIdToTrackMetadata: Map<String, Metadata?>,
        midToTrackId: Map<String, String>
    ) {
        Log.i(TA, "sdpOffer")
        Log.i(TA, sdp)
        Log.i(TA, trackIdToTrackMetadata.toString())
        Log.i(TA, midToTrackId.toString())
        Log.i(TA,"")
        sendEvent(
            SdpOffer(
                sdp,
                trackIdToTrackMetadata,
                midToTrackId
            )
        )
    }

    fun disconnect() {
        Log.i(TA, "disconnect")
        Log.i(TA,"")
        sendEvent(Disconnect())
    }

    private fun sendEvent(event: SendableEvent) {
        Log.i(TA, "sendEvent")
        Log.i(TA, event.toString())
        Log.i(TA,"")
        val serializedMediaEvent = gson.toJson(event.serializeToMap())
        engineListener.onSendMediaEvent(serializedMediaEvent)
    }

    private fun decodeEvent(event: SerializedMediaEvent): ReceivableEvent? {

        val type = object : TypeToken<Map<String, Any?>>() {}.type

        val rawMessage: Map<String, Any?> = gson.fromJson(event, type)

        ReceivableEvent.decode(rawMessage)?.let {
            Log.i(TA, "decodeEvent")
            Log.i(TA, it.toString())
            Log.i(TA,"")
            return it
        } ?: run {
            Timber.d("Failed to decode event $rawMessage")
            return null
        }
    }

    fun onEvent(serializedEvent: SerializedMediaEvent) {
        when (val event = decodeEvent(serializedEvent)) {
            is Connected -> engineListener.onConnected(event.data.id, event.data.otherEndpoints)
            is OfferData -> engineListener.onOfferData(event.data.integratedTurnServers, event.data.tracksTypes)
            is EndpointRemoved -> engineListener.onEndpointRemoved(event.data.id)
            is EndpointAdded ->
                engineListener.onEndpointAdded(
                    Endpoint(event.data.id, event.data.type, event.data.metadata, mapOf())
                )
            is EndpointUpdated -> engineListener.onEndpointUpdated(event.data.id, event.data.metadata)
            is RemoteCandidate ->
                engineListener.onRemoteCandidate(
                    event.data.candidate,
                    event.data.sdpMLineIndex,
                    event.data.sdpMid
                )
            is SdpAnswer -> engineListener.onSdpAnswer(event.data.type, event.data.sdp, event.data.midToTrackId)
            is TrackUpdated ->
                engineListener.onTrackUpdated(
                    event.data.endpointId,
                    event.data.trackId,
                    event.data.metadata
                )
            is TracksAdded ->
                engineListener.onTracksAdded(
                    event.data.endpointId,
                    event.data.tracks
                )
            is TracksRemoved -> engineListener.onTracksRemoved(event.data.endpointId, event.data.trackIds)
            is EncodingSwitched ->
                engineListener.onTrackEncodingChanged(
                    event.data.endpointId,
                    event.data.trackId,
                    event.data.encoding,
                    event.data.reason
                )
            is VadNotification -> engineListener.onVadNotification(event.data.trackId, event.data.status)
            is BandwidthEstimation -> engineListener.onBandwidthEstimation(event.data.estimation.roundToLong())
            else -> Timber.e("Failed to process unknown event: $event")
        }
    }
}
