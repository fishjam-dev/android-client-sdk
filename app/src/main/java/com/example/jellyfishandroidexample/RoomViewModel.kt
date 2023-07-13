package com.example.jellyfishandroidexample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jellyfishdev.jellyfishclient.Config
import com.jellyfishdev.jellyfishclient.JellyfishClient
import com.jellyfishdev.jellyfishclient.JellyfishClientListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.membraneframework.rtc.SimulcastConfig
import org.membraneframework.rtc.media.LocalVideoTrack
import org.membraneframework.rtc.media.RemoteVideoTrack
import org.membraneframework.rtc.media.VideoParameters
import org.membraneframework.rtc.models.Endpoint
import org.membraneframework.rtc.models.TrackContext

class RoomViewModel(application: Application) :
    AndroidViewModel(application),
    JellyfishClientListener {
    private val client = JellyfishClient(getApplication(), this)

    private var localVideoTrack: LocalVideoTrack? = null
    private val mutableParticipants = HashMap<String, Participant>()
    val participants = MutableStateFlow<List<Participant>>(emptyList())
    private val globalToLocalTrackId = HashMap<String, String>()

    private val videoSimulcastConfig = SimulcastConfig(
        enabled = false,
    )

    fun connect(roomToken: String) {
        client.connect(
            Config(
                websocketUrl = BuildConfig.JELLYFISH_SOCKET_URL,
                token = roomToken
            ),
        )
        setupTracks()
    }

    fun disconnect() {
        localVideoTrack?.stop()
        localVideoTrack = null
        globalToLocalTrackId.clear()
        client.cleanUp()
    }

    private fun setupTracks() {
        var videoParameters = VideoParameters.presetHD169
        videoParameters = videoParameters.copy(
            dimensions = videoParameters.dimensions,
            simulcastConfig = videoSimulcastConfig,
        )

        localVideoTrack = client.createVideoTrack(videoParameters, emptyMap())
    }

    override fun onAuthSuccess() {
        client.join()
    }

    override fun onAuthError() {}

    override fun onConnected(endpointID: String, endpointsInRoom: List<Endpoint>) {
        endpointsInRoom.forEach {
            mutableParticipants[it.id] = Participant(
                it.id,
            )
        }
        emitParticipants()
    }

    override fun onConnectError(metadata: Any) {

    }

    override fun onRemoved(reason: String) {
    }

    override fun onEndpointAdded(endpoint: Endpoint) {
        mutableParticipants[endpoint.id] = Participant(
            id = endpoint.id,
        )
        emitParticipants()
    }

    override fun onEndpointRemoved(endpoint: Endpoint) {
        mutableParticipants.remove(endpoint.id)
        emitParticipants()
    }

    override fun onEndpointUpdated(endpoint: Endpoint) {}

    override fun onTrackReady(ctx: TrackContext) {
        viewModelScope.launch {
            val participant = mutableParticipants[ctx.endpoint.id] ?: return@launch

            val (id, newParticipant) = when (ctx.track) {
                is RemoteVideoTrack -> {
                    globalToLocalTrackId[ctx.trackId] = (ctx.track as RemoteVideoTrack).id()

                    val p = participant.copy(videoTrack = ctx.track as RemoteVideoTrack)
                    Pair(ctx.endpoint.id, p)
                }

                else ->
                    throw IllegalArgumentException("invalid type of incoming remote track")
            }

            mutableParticipants[id] = newParticipant

            emitParticipants()
        }
    }

    override fun onTrackRemoved(ctx: TrackContext) {
        viewModelScope.launch {
            val participant = mutableParticipants[ctx.endpoint.id] ?: return@launch

            val localTrackId = globalToLocalTrackId[ctx.trackId]
            val videoTrackId = participant.videoTrack?.id()

            val newParticipant = if (localTrackId == videoTrackId) {
                participant.copy(videoTrack = null)
            } else {
                throw IllegalArgumentException("track has not been found for given endpoint")
            }

            globalToLocalTrackId.remove(ctx.trackId)

            mutableParticipants[ctx.endpoint.id] = newParticipant

            emitParticipants()
        }
    }

    private fun emitParticipants() {
        participants.value =
            mutableParticipants.values.filter { p -> p.videoTrack != null }.toList()
    }
}
