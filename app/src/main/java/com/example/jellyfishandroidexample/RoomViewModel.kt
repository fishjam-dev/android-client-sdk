package com.example.jellyfishandroidexample

import android.app.Application
import android.util.Log
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
                token = "SFMyNTY.g2gDdAAAAAJkAAdwZWVyX2lkbQAAACRjODU4NzkwMS1jNzg0LTRmZWMtYjg4MC03ZjU3YjEwZjM4MjVkAAdyb29tX2lkbQAAACRmM2U2ZDI1OS03NWIyLTRkODEtOTQ1MS1iNTUyNjkyYTYyMDBuBgCdakpKiQFiAAFRgA.C4mIsGZ41oUOxq1UXFVXjRqvGIrVk42Ij2khN3XZ8NQ",
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
        Log.e("KAROL", "xXXXD")
        client.join()
    }

    override fun onAuthError() {
        Log.e("KAROL", "xD")
    }

    override fun onConnected(peerID: String, peersInRoom: List<Endpoint>) {
        Log.e("KAROL", "connected")
        peersInRoom.forEach {
            mutableParticipants[it.id] = Participant(
                it.id,
            )
        }
        emitParticipants()
    }

    override fun onRemoved(reason: String) {
    }

    override fun onEndpointAdded(endpoint: Endpoint) {
        Log.e("KAROL", "added")
        mutableParticipants[endpoint.id] = Participant(
            id = endpoint.id,
        )
        emitParticipants()
    }

    override fun onEndpointRemoved(endpoint: Endpoint) {
        Log.e("KAROL", "removed")

        mutableParticipants.remove(endpoint.id)
        emitParticipants()
    }

    override fun onEndpointUpdated(endpoint: Endpoint) {
        Log.e("KAROL", "updated")

    }

    override fun onTrackReady(ctx: TrackContext) {
        Log.e("KAROL", "track ready")

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
        Log.e("KAROL", "track removed")

        viewModelScope.launch {
            val participant = mutableParticipants[ctx.endpoint.id] ?: return@launch

            val localTrackId = globalToLocalTrackId[ctx.trackId]
            val videoTrackId = participant.videoTrack?.id()

            val newParticipant = if (localTrackId == videoTrackId) {
                participant.copy(videoTrack = null)
            } else {
                throw IllegalArgumentException("track has not been found for given peer")
            }

            globalToLocalTrackId.remove(ctx.trackId)

            mutableParticipants[ctx.endpoint.id] = newParticipant

            emitParticipants()
        }
    }

    private fun emitParticipants() {
        Log.e("KAROL", "emit")

        participants.value =
            mutableParticipants.values.filter { p -> p.videoTrack != null }.toList()
    }
}
