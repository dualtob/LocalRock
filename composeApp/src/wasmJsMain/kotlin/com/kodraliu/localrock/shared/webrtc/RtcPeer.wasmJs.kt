package com.kodraliu.localrock.shared.webrtc

/** The camera live view is not available in the Web App yet. */
actual val liveViewSupported: Boolean = false

actual fun createRtcPeer(iceServers: List<RtcIceServer>): RtcPeer =
    throw UnsupportedOperationException("The camera live view is not available in the Web App")
