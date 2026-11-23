package com.kodraliu.localrock.ui.devices

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddRobotOfferTest {

    @Test
    fun the_web_app_points_to_the_app_instead_of_offering_onboarding() {
        val offer = addRobotOffer(isWebApp = true, pairingSupported = false)

        assertTrue(offer is AddRobotOffer.NeedsApp)
        assertTrue("LocalRock App" in offer.message, offer.message)
    }

    @Test
    fun the_app_offers_onboarding_where_it_can_pair() {
        assertEquals(AddRobotOffer.Pair, addRobotOffer(isWebApp = false, pairingSupported = true))
        assertEquals(AddRobotOffer.None, addRobotOffer(isWebApp = false, pairingSupported = false))
    }
}
