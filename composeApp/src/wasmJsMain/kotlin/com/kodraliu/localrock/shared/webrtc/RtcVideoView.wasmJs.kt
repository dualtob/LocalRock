package com.kodraliu.localrock.shared.webrtc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Never shown: [liveViewSupported] is false in the Web App, so no peer is ever created. */
@Composable
actual fun RtcVideoView(peer: RtcPeer, modifier: Modifier) = Unit
