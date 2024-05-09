package com.example.jellyfishandroidexample

import org.membraneframework.rtc.media.VideoTrack

data class Participant(
    val id: String,
    val videoTrack: VideoTrack? = null
)
