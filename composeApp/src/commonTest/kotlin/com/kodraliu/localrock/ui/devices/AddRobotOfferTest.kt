package com.kodraliu.localrock.ui.devices

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddRobotOfferTest {

    @Test
    fun the_web_app_points_to_the_app_instead_of_offering_onboarding() {
        val offer = addRobotOffer(isWebApp = true, onboardingSupported = false)

        assertTrue(offer is AddRobotOffer.NeedsApp)
        assertTrue("LocalRock App" in offer.message, offer.message)
    }

    @Test
    fun the_app_offers_onboarding_where_the_platform_supports_it() {
        assertEquals(AddRobotOffer.Onboard, addRobotOffer(isWebApp = false, onboardingSupported = true))
        assertEquals(AddRobotOffer.None, addRobotOffer(isWebApp = false, onboardingSupported = false))
    }
}
