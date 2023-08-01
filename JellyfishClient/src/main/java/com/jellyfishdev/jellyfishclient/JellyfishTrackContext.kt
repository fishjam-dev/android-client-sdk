package com.jellyfishdev.jellyfishclient

import org.membraneframework.rtc.TrackEncoding
import org.membraneframework.rtc.media.RemoteTrack
import org.membraneframework.rtc.models.EncodingReason
import org.membraneframework.rtc.models.OnEncodingChangedListener
import org.membraneframework.rtc.models.OnVoiceActivityChangedListener
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.models.VadStatus
import org.membraneframework.rtc.utils.Metadata

fun interface JellyfishOnVoiceActivityChangedListener {
    fun onVoiceActivityChanged(trackContext: JellyfishTrackContext)
}

fun interface JellyfishOnEncodingChangedListener {
    fun onEncodingChangedListener(trackContext: JellyfishTrackContext)
}

class JellyfishTrackContext(private val trackContext: TrackContext) {
    val track: RemoteTrack?
        get() {
            return trackContext.track
        }

    val trackId: String
        get() {
            return trackContext.trackId
        }

    val peer: Peer
        get() {
            return trackContext.endpoint
        }

    val metadata: Metadata
        get() {
            return trackContext.metadata
        }

    val vadStatus: VadStatus
        get() {
            return trackContext.vadStatus
        }

    val encoding: TrackEncoding?
        get() {
            return trackContext.encoding
        }

    val encodingReason: EncodingReason?
        get() {
            return trackContext.encodingReason
        }

    fun setOnEncodingChangedListener(listener: JellyfishOnEncodingChangedListener) {
        val rtcListener = OnEncodingChangedListener { trackContext -> listener.onEncodingChangedListener(JellyfishTrackContext(trackContext)) }
        trackContext.setOnEncodingChangedListener(rtcListener)
    }

    fun setOnVoiceActivityChangedListener(listener: JellyfishOnVoiceActivityChangedListener) {
        val rtcListener = OnVoiceActivityChangedListener { trackContext -> listener.onVoiceActivityChanged(JellyfishTrackContext(trackContext)) }
        trackContext.setOnVoiceActivityChangedListener(rtcListener)
    }
}
