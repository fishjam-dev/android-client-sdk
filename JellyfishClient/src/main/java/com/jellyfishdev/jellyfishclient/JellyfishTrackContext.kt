package com.jellyfishdev.jellyfishclient

import org.membraneframework.rtc.TrackEncoding
import org.membraneframework.rtc.media.RemoteTrack
import org.membraneframework.rtc.models.EncodingReason
import org.membraneframework.rtc.models.OnEncodingChangedListener
import org.membraneframework.rtc.models.OnVoiceActivityChangedListener
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.models.VadStatus
import org.membraneframework.rtc.utils.Metadata

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

    fun setOnEncodingChangedListener(listener: OnEncodingChangedListener) {
        trackContext.setOnEncodingChangedListener(listener)
    }

    fun setOnVoiceActivityChangedListener(listener: OnVoiceActivityChangedListener) {
        trackContext.setOnVoiceActivityChangedListener(listener)
    }
}