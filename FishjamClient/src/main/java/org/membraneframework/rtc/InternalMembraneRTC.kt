package org.membraneframework.rtc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.fishjamdev.client.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.membraneframework.rtc.events.OfferData
import org.membraneframework.rtc.media.*
import org.membraneframework.rtc.models.EncodingReason
import org.membraneframework.rtc.models.Endpoint
import org.membraneframework.rtc.models.RTCStats
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.models.TrackData
import org.membraneframework.rtc.models.VadStatus
import org.membraneframework.rtc.utils.ClosableCoroutineScope
import org.membraneframework.rtc.utils.Metadata
import org.membraneframework.rtc.utils.SerializedMediaEvent
import org.membraneframework.rtc.utils.TimberDebugTree
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStreamTrack
import org.webrtc.VideoTrack
import timber.log.Timber
import java.util.*
const val T = "IMRTC"
internal class InternalMembraneRTC(
    createOptions: CreateOptions,
    private val listener: MembraneRTCListener,
    defaultDispatcher: CoroutineDispatcher,
    private val eglBase: EglBase,
    private val context: Context
) : RTCEngineListener, PeerConnectionListener {
    private val rtcEngineCommunication = RTCEngineCommunication(this)
    private val peerConnectionFactoryWrapper =
        PeerConnectionFactoryWrapper(createOptions, RTCModule.audioDeviceModule(context), eglBase, context)
    private val peerConnectionManager = PeerConnectionManager(this, peerConnectionFactoryWrapper)

    private var localEndpoint: Endpoint =
        Endpoint(id = "", type = "webrtc", metadata = mapOf(), tracks = mapOf())

    // mapping from endpoint's id to the endpoint himself
    private val remoteEndpoints = HashMap<String, Endpoint>()

    // mapping from remote track's id to its context
    private val trackContexts = HashMap<String, TrackContext>()

    private val localTracks = mutableListOf<LocalTrack>()
    private val localTracksMutex = Mutex()
//    private val localVideoMutex = Mutex()
//    private val localMicrophoneMutex = Mutex()
//    private val localScreencastMutex = Mutex()
//    private var canUpdateMetadata = false

    private val coroutineScope: CoroutineScope =
        ClosableCoroutineScope(SupervisorJob() + defaultDispatcher)

    init {
        if (BuildConfig.DEBUG) {
            Timber.plant(TimberDebugTree())
        }
    }

    interface Factory {
        fun create(
            createOptions: CreateOptions,
            listener: MembraneRTCListener,
            defaultDispatcher: CoroutineDispatcher
        ): InternalMembraneRTC
    }

//    private fun getMutexForTrack(trackId: String) : Mutex? {
//        var mutex: Mutex? = null
//        localTracks.find { it.id() == trackId }?.let {
//            mutex = when (it) {
//                is LocalAudioTrack -> localMicrophoneMutex
//                is LocalVideoTrack -> localVideoMutex
//                else -> localScreencastMutex
//            }
//        }
//        return mutex
//    }

    fun disconnect() {
        Log.i(T, "disconnect")
        Log.i(T,"")
        coroutineScope.launch {
            rtcEngineCommunication.disconnect()
            localTracksMutex.withLock {
                localTracks.forEach { it.stop() }
            }
            peerConnectionManager.close()
        }
    }

    fun receiveMediaEvent(event: SerializedMediaEvent) {
        Log.i(T, "receiveMediaEvent")
        Log.i(T, event)
        Log.i(T,"")
        rtcEngineCommunication.onEvent(event)
    }

    fun connect(endpointMetadata: Metadata? = mapOf()) {
        Log.i(T, "connect")
        Log.i(T,"")

        coroutineScope.launch {
//            localMicrophoneMutex.lock()
//            localVideoMutex.lock()
//            localScreencastMutex.lock()
            localEndpoint = localEndpoint.copy(metadata = endpointMetadata)
            rtcEngineCommunication.connect(endpointMetadata ?: mapOf())
        }
    }

    fun createLocalVideoTrack(
        videoParameters: VideoParameters,
        metadata: Metadata = mapOf(),
        captureDeviceName: String? = null
    ): LocalVideoTrack {
        Log.i(T, "createLocalVideoTrack")
        Log.i(T,"")
        val videoTrack =
            LocalVideoTrack.create(
                context,
                peerConnectionFactoryWrapper.peerConnectionFactory,
                eglBase,
                videoParameters,
                captureDeviceName
            ).also {
                it.start()
            }

        localTracks.add(videoTrack)
        Log.i(T, "Local tracks")
        Log.i(T, localTracks.toString())
        Log.i(T,"")

        localEndpoint = localEndpoint.withTrack(videoTrack.id(), metadata)

        coroutineScope.launch {
//            localVideoMutex.lock()
            peerConnectionManager.addTrack(videoTrack)
            rtcEngineCommunication.renegotiateTracks()
        }

        return videoTrack
    }

    fun createLocalAudioTrack(metadata: Metadata = mapOf()): LocalAudioTrack {
        Log.i(T, "createLocalAudioTrack")
        Log.i(T,"")

        val audioTrack =
            LocalAudioTrack.create(
                context,
                peerConnectionFactoryWrapper.peerConnectionFactory
            ).also {
                it.start()
            }

        localTracks.add(audioTrack)
        Log.i(T, "local tracks")
        Log.i(T, localTracks.toString())
        Log.i(T,"")

        localEndpoint = localEndpoint.withTrack(audioTrack.id(), metadata)

        coroutineScope.launch {
//            localMicrophoneMutex.lock()
            peerConnectionManager.addTrack(audioTrack)
            rtcEngineCommunication.renegotiateTracks()
        }

        return audioTrack
    }

    fun setTrackBandwidth(
        trackId: String,
        bandwidthLimit: TrackBandwidthLimit.BandwidthLimit
    ) {
        Log.i(T, "setTrackBandwidth")
        Log.i(T, bandwidthLimit.toString())
        Log.i(T,"")

        coroutineScope.launch {
            peerConnectionManager.setTrackBandwidth(trackId, bandwidthLimit)
        }
    }

    fun setEncodingBandwidth(
        trackId: String,
        encoding: String,
        bandwidthLimit: TrackBandwidthLimit.BandwidthLimit
    ) {
        Log.i(T, "setEncodingBandwidth")
        Log.i(T, encoding + " "+ bandwidthLimit.toString())
        Log.i(T,"")

        coroutineScope.launch {
            peerConnectionManager.setEncodingBandwidth(trackId, encoding, bandwidthLimit)
        }
    }

    fun createScreencastTrack(
        mediaProjectionPermission: Intent,
        videoParameters: VideoParameters,
        metadata: Metadata = mapOf(),
        onEnd: (() -> Unit)?
    ): LocalScreencastTrack {
        Log.i(T, "createScreencastTrack")
        Log.i(T,"")

        val screencastTrack =
            LocalScreencastTrack.create(
                context,
                peerConnectionFactoryWrapper.peerConnectionFactory,
                eglBase,
                mediaProjectionPermission,
                videoParameters
            ) { track ->
                if (onEnd != null) {
                    onEnd()
                }
            }

        localTracks.add(screencastTrack)
        Log.i(T, "local tracks")
        Log.i(T, localTracks.toString())
        Log.i(T,"")
        localEndpoint = localEndpoint.withTrack(screencastTrack.id(), metadata)

        coroutineScope.launch {
            screencastTrack.startForegroundService(null, null)
            screencastTrack.start()
        }

        coroutineScope.launch {
//            localScreencastMutex.lock()
            peerConnectionManager.addTrack(screencastTrack)
            rtcEngineCommunication.renegotiateTracks()
        }

        return screencastTrack
    }

    fun removeTrack(trackId: String): Boolean {
        Log.i(T, "removeTrack")
        Log.i(T,"")

        return runBlocking(Dispatchers.Default) {
            localTracksMutex.withLock {
                val track =
                    localTracks.find { it.id() == trackId } ?: run {
                        Timber.e("removeTrack: Can't find track to remove")
                        return@runBlocking false
                    }

                peerConnectionManager.removeTrack(track.id())

                localTracks.remove(track)
                Log.i(T, "local tracks")
                Log.i(T, localTracks.toString())
                Log.i(T,"")
                localEndpoint = localEndpoint.withoutTrack(trackId)
                track.stop()
            }
            rtcEngineCommunication.renegotiateTracks()
            return@runBlocking true
        }
    }

    fun updateEndpointMetadata(endpointMetadata: Metadata) {
        Log.i(T, "updateEndpointMetadata")
        Log.i(T, endpointMetadata.toString())
        Log.i(T,"")

        coroutineScope.launch {
            rtcEngineCommunication.updateEndpointMetadata(endpointMetadata)
            localEndpoint = localEndpoint.copy(metadata = endpointMetadata)
        }
    }

    fun updateTrackMetadata(
        trackId: String,
        trackMetadata: Metadata
    ) {
        Log.i(T, "updateTrackMetadata")
        Log.i(T, trackMetadata.toString())
        Log.i(T,"")

//        val mutex = getMutexForTrack(trackId)
        coroutineScope.launch {
            rtcEngineCommunication.updateTrackMetadata(trackId, trackMetadata)
            localEndpoint = localEndpoint.withTrack(trackId, trackMetadata)
        }
    }



    override fun onConnected(
        endpointID: String,
        otherEndpoints: List<Endpoint>
    ) {
        Log.i(T, "onConnected")
        Log.i(T,"")

        this.localEndpoint = localEndpoint.copy(id = endpointID)
        listener.onConnected(endpointID, otherEndpoints)

        otherEndpoints.forEach {
            this.remoteEndpoints[it.id] = it

            for ((trackId, trackData) in it.tracks) {
                val context =
                    TrackContext(
                        track = null,
                        endpoint = it,
                        trackId = trackId,
                        metadata = trackData.metadata ?: mapOf(),
                        simulcastConfig = trackData.simulcastConfig
                    )

                this.trackContexts[trackId] = context

                this.listener.onTrackAdded(context)
            }
        }
    }

    override fun onSendMediaEvent(event: SerializedMediaEvent) {
        Log.i(T, "onSendMediaEvent")
        Log.i(T, event)
        Log.i(T,"")

        listener.onSendMediaEvent(event)
    }

    override fun onEndpointAdded(endpoint: Endpoint) {
        Log.i(T, "onEndpointAdded")
        Log.i(T,"")

        if (endpoint.id == this.localEndpoint.id) {
            return
        }

        remoteEndpoints[endpoint.id] = endpoint

        listener.onEndpointAdded(endpoint)
    }

    override fun onEndpointRemoved(endpointId: String) {
        Log.i(T, "onEndpointRemoved")

        if (endpointId == localEndpoint.id) {
            listener.onDisconnected()
            return
        }
        val endpoint =
            remoteEndpoints.remove(endpointId) ?: run {
                Timber.e("Failed to process EndpointLeft event: Endpoint not found: $endpointId")
                return
            }

        val trackIds: List<String> = endpoint.tracks.keys.toList()

        trackIds.forEach {
            trackContexts.remove(it)?.let { ctx ->
                listener.onTrackRemoved(ctx)
            }
        }

        listener.onEndpointRemoved(endpoint)
    }

    override fun onEndpointUpdated(
        endpointId: String,
        endpointMetadata: Metadata?
    ) {
        Log.i(T, "onEndpointUpdated")
        Log.i(T,"")

        val endpoint =
            remoteEndpoints.remove(endpointId) ?: run {
                Timber.e("Failed to process EndpointUpdated event: Endpoint not found: $endpointId")
                return
            }

        remoteEndpoints[endpoint.id] = endpoint.copy(metadata = endpointMetadata)
    }

    override fun onOfferData(
        integratedTurnServers: List<OfferData.TurnServer>,
        tracksTypes: Map<String, Int>
    ) {
        Log.i(T, "onOfferData")
        Log.i(T,"")

        coroutineScope.launch {
            try {
                val offer =
                    localTracksMutex.withLock {
                        peerConnectionManager.getSdpOffer(integratedTurnServers, tracksTypes, localTracks)
                    }
                rtcEngineCommunication.sdpOffer(
                    offer.description,
                    localEndpoint.tracks.mapValues { it.value.metadata },
                    offer.midToTrackIdMapping
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to create an sdp offer")
            }
        }
    }

    override fun onSdpAnswer(
        type: String,
        sdp: String,
        midToTrackId: Map<String, String>
    ) {
        Log.i(T, "onSdpAnswer")
        Log.i(T, type + " " + sdp)
        Log.i(T,"")


        coroutineScope.launch {
            peerConnectionManager.onSdpAnswer(sdp, midToTrackId)

            localTracksMutex.withLock {
                // temporary workaround, the backend doesn't add ~ in sdp answer
                localTracks.forEach { localTrack ->
                    if (localTrack.rtcTrack().kind() != "video") return@forEach
                    var config: SimulcastConfig? = null
                    if (localTrack is LocalVideoTrack) {
                        config = localTrack.videoParameters.simulcastConfig
                    } else if (localTrack is LocalScreencastTrack) {
                        config = localTrack.videoParameters.simulcastConfig
                    }
                    listOf(TrackEncoding.L, TrackEncoding.M, TrackEncoding.H).forEach {
                        if (config?.activeEncodings?.contains(it) == false) {
                            peerConnectionManager.setTrackEncoding(localTrack.id(), it, false)
                        }
                    }
                }
//                localVideoMutex.unlock()
//                localMicrophoneMutex.unlock()
//                localScreencastMutex.unlock()
            }
        }
    }

    override fun onRemoteCandidate(
        candidate: String,
        sdpMLineIndex: Int,
        sdpMid: String?
    ) {
        Log.i(T, "onRemoteCandidate")
        Log.i(T,"")

        coroutineScope.launch {
            val iceCandidate =
                IceCandidate(
                    sdpMid ?: "",
                    sdpMLineIndex,
                    candidate
                )

            peerConnectionManager.onRemoteCandidate(iceCandidate)
        }
    }

    override fun onTracksAdded(
        endpointId: String,
        tracks: Map<String, TrackData>
    ) {
        Log.i(T, "onTracksAdded")
        Log.i(T,"")
        Log.i(T, "Is local endpoint: ${localEndpoint.id == endpointId}")

        if (localEndpoint.id == endpointId) return

        val endpoint =
            remoteEndpoints.remove(endpointId) ?: run {
                Timber.e("Failed to process TracksAdded event: Endpoint not found: $endpointId")
                return
            }

        val updatedEndpoint = endpoint.copy(tracks = tracks)

        remoteEndpoints[updatedEndpoint.id] = updatedEndpoint

        for ((trackId, trackData) in updatedEndpoint.tracks) {
            val context =
                TrackContext(
                    track = null,
                    endpoint = endpoint,
                    trackId = trackId,
                    metadata = trackData.metadata ?: mapOf(),
                    simulcastConfig = trackData.simulcastConfig
                )

            this.trackContexts[trackId] = context

            this.listener.onTrackAdded(context)
        }
    }

    override fun onTracksRemoved(
        endpointId: String,
        trackIds: List<String>
    ) {
        Log.i(T, "onTracksRemoved")
        Log.i(T,"")

        val endpoint =
            remoteEndpoints[endpointId] ?: run {
                Timber.e("Failed to process TracksRemoved event: Endpoint not found: $endpointId")
                return
            }

        trackIds.forEach {
            val context = trackContexts.remove(it) ?: return@forEach

            this.listener.onTrackRemoved(context)
        }

        val updatedEndpoint =
            trackIds.fold(endpoint) { acc, trackId ->
                acc.withoutTrack(trackId)
            }

        remoteEndpoints[endpointId] = updatedEndpoint
    }

    override fun onTrackUpdated(
        endpointId: String,
        trackId: String,
        metadata: Metadata?
    ) {
        Log.i(T, "onTrackUpdated")
        Log.i(T,"")

        val endpoint =
            remoteEndpoints[endpointId] ?: run {
                Timber.e("Failed to process TrackUpdated event: Endpoint not found: $endpointId")
                return
            }

        val context =
            trackContexts[trackId] ?: run {
                Timber.e("Failed to process TrackUpdated event: Track context not found: $trackId")
                return
            }

        context.metadata = metadata ?: mapOf()

        val updatedEndpoint =
            endpoint
                .withoutTrack(trackId)
                .withTrack(trackId, metadata)

        remoteEndpoints[endpointId] = updatedEndpoint

        this.listener.onTrackUpdated(context)
    }

    override fun onTrackEncodingChanged(
        endpointId: String,
        trackId: String,
        encoding: String,
        encodingReason: String
    ) {
        Log.i(T, "onTrackEncodingChanged")
        Log.i(T,"")

        val encodingReasonEnum = EncodingReason.fromString(encodingReason)
        if (encodingReasonEnum == null) {
            Timber.e("Invalid encoding reason: $encodingReason")
            return
        }
        val trackContext = trackContexts[trackId]
        if (trackContext == null) {
            Timber.e("Invalid trackId: $trackId")
            return
        }
        val encodingEnum = TrackEncoding.fromString(encoding)
        if (encodingEnum == null) {
            Timber.e("Invalid encoding: $encoding")
            return
        }
        trackContext.setEncoding(encodingEnum, encodingReasonEnum)
    }

    override fun onVadNotification(
        trackId: String,
        status: String
    ) {
        Log.i(T, "onVadNotification")
        Log.i(T,"")

        val trackContext = trackContexts[trackId]
        if (trackContext == null) {
            Timber.e("Invalid track id = $trackId")
            return
        }
        val vadStatus = VadStatus.fromString(status)
        if (vadStatus == null) {
            Timber.e("Invalid vad status = $status")
            return
        }
        trackContext.vadStatus = vadStatus
    }

    override fun onBandwidthEstimation(estimation: Long) {
        Log.i(T, "onBandwidthEstimation")
        Log.i(T,"")

        listener.onBandwidthEstimationChanged(estimation)
    }

    fun setTargetTrackEncoding(
        trackId: String,
        encoding: TrackEncoding
    ) {
        Log.i(T, "setTargetTrackEncoding")
        Log.i(T,"")

        coroutineScope.launch {
            rtcEngineCommunication.setTargetTrackEncoding(trackId, encoding)
        }
    }

    fun enableTrackEncoding(
        trackId: String,
        encoding: TrackEncoding
    ) {
        Log.i(T, "enableTrackEncoding")

        coroutineScope.launch {
            peerConnectionManager.setTrackEncoding(trackId, encoding, true)
        }
    }

    fun disableTrackEncoding(
        trackId: String,
        encoding: TrackEncoding
    ) {
        Log.i(T, "disableTrackEncoding")
        Log.i(T,"")

        coroutineScope.launch {
            peerConnectionManager.setTrackEncoding(trackId, encoding, false)
        }
    }

    override fun onLocalIceCandidate(candidate: IceCandidate) {
        Log.i(T, "onLocalIceCandidate")
        Log.i(T,"")

        coroutineScope.launch {
            rtcEngineCommunication.localCandidate(candidate.sdp, candidate.sdpMLineIndex)
        }
    }

    override fun onAddTrack(
        trackId: String,
        track: MediaStreamTrack
    ) {
        Log.i(T, "onAddTrack")
        Log.i(T,"")

        val trackContext =
            trackContexts[trackId] ?: run {
                Timber.e("onAddTrack: Track context with trackId=$trackId not found")
                return
            }

        when (track) {
            is VideoTrack ->
                trackContext.track = RemoteVideoTrack(track, eglBase)

            is AudioTrack ->
                trackContext.track = RemoteAudioTrack(track)

            else ->
                throw IllegalStateException("invalid type of incoming track")
        }

        listener.onTrackReady(trackContext)
    }

    fun getStats(): Map<String, RTCStats> {
        Log.i(T, "getStats")
        Log.i(T,"")

        return peerConnectionManager.getStats()
    }
}
