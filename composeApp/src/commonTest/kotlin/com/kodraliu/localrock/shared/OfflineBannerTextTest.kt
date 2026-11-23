package com.kodraliu.localrock.shared

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Web App is served by the Server and reaches MQTT through it, so a link that stays down means
 * the Server can't be reached, and the banner says so instead of a generic "offline".
 */
class OfflineBannerTextTest {

    @Test
    fun webAppSaysTheServerCannotBeReached() {
        assertEquals("Can't reach the Server — reconnecting…", mqttOfflineBannerText(isWebApp = true))
    }

    @Test
    fun appKeepsItsOfflineBanner() {
        assertEquals("Offline — reconnecting…", mqttOfflineBannerText(isWebApp = false))
    }
}
