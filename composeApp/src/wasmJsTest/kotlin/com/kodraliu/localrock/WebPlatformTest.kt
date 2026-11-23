package com.kodraliu.localrock

import com.kodraliu.localrock.shared.mqtt.defaultMqttTransport
import com.kodraliu.localrock.shared.notify.backgroundAlertsSupported
import com.kodraliu.localrock.shared.onboarding.vacuumPairingSupported
import com.kodraliu.localrock.shared.webrtc.liveViewSupported
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** What the Web App reports about its platform-specific features. */
class WebPlatformTest {

    @Test
    fun the_platform_says_it_is_the_web_app() {
        assertTrue(getPlatform().isWebApp)
    }

    @Test
    fun onboarding_is_unsupported() {
        assertFalse(vacuumPairingSupported)
    }

    @Test
    fun background_alerts_are_unsupported() {
        assertFalse(backgroundAlertsSupported)
    }

    @Test
    fun the_camera_live_view_is_unsupported() {
        assertFalse(liveViewSupported)
    }

    @Test
    fun there_is_an_mqtt_transport() {
        assertNotNull(defaultMqttTransport())
    }
}
