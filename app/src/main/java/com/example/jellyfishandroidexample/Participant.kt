package com.example.jellyfishandroidexample

import com.jellyfishdev.jellyfishclient.webrtc.VideoTrack

data class Participant(
    val id: String,
    val videoTrack: VideoTrack? = null,
)
