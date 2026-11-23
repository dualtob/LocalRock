package com.kodraliu.localrock

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Fetching `config.json` in a real browser, where a malformed request fails before it is sent. */
class WebAppConfigFetchTest {

    @Test
    fun the_config_file_is_read_from_its_address() = runTest {
        val config = """{"serverUrl": "https://robots.lan:555"}"""

        assertEquals(config, fetchConfigJson("data:application/json," + config))
    }
}
