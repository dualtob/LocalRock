package com.kodraliu.localrock

import com.kodraliu.localrock.shared.mqtt.MqttJsTransport
import com.kodraliu.localrock.shared.mqtt.webAppMqttTransport
import com.kodraliu.localrock.shared.notify.backgroundAlertsSupported
import com.kodraliu.localrock.shared.onboarding.vacuumPairingSupported
import com.kodraliu.localrock.shared.platform.donationUrl
import com.kodraliu.localrock.shared.platform.donationsEnabled
import com.kodraliu.localrock.shared.webrtc.liveViewSupported
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

/** What the Web App reports about its platform-specific features. */
class WebPlatformTest {

    @Test
    fun the_platform_is_one_shared_object() {
        assertSame(getPlatform(), getPlatform())
    }

    @Test
    fun there_is_no_donation_link() {
        assertFalse(donationsEnabled)
        assertEquals("", donationUrl)
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
    fun the_mqtt_transport_goes_to_the_page_origin() {
        assertEquals("wss://robots.lan:555/mqtt", (webAppMqttTransport("https://robots.lan:555") as MqttJsTransport).url)
    }
}
