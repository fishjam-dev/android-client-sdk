package com.fishjam.client

import org.membraneframework.rtc.TrackEncoding
import org.membraneframework.rtc.media.RemoteTrack
import org.membraneframework.rtc.models.EncodingReason
import org.membraneframework.rtc.models.OnEncodingChangedListener
import org.membraneframework.rtc.models.OnVoiceActivityChangedListener
import org.membraneframework.rtc.models.TrackContext
import org.membraneframework.rtc.models.VadStatus
import org.membraneframework.rtc.utils.Metadata

fun interface FishjamOnVoiceActivityChangedListener {
    fun onVoiceActivityChanged(trackContext: FishjamTrackContext)
}

fun interface FishjamOnEncodingChangedListener {
    fun onEncodingChangedListener(trackContext: FishjamTrackContext)
}

class FishjamTrackContext(private val trackContext: TrackContext) {
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

    fun setOnEncodingChangedListener(listener: FishjamOnEncodingChangedListener) {
        val rtcListener =
            OnEncodingChangedListener {
                    trackContext ->
                listener.onEncodingChangedListener(FishjamTrackContext(trackContext))
            }
        trackContext.setOnEncodingChangedListener(rtcListener)
    }

    fun setOnVoiceActivityChangedListener(listener: FishjamOnVoiceActivityChangedListener) {
        val rtcListener =
            OnVoiceActivityChangedListener {
                    trackContext ->
                listener.onVoiceActivityChanged(FishjamTrackContext(trackContext))
            }
        trackContext.setOnVoiceActivityChangedListener(rtcListener)
    }
}
