package com.kodraliu.localrock.shared

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Web App reaches MQTT through its Server's `/mqtt`, so a link that stays down means
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
