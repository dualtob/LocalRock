package com.kodraliu.localrock.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The Web App's deployment names its Server in `config.json`, next to `index.html`. */
class WebAppServerTest {

    @Test
    fun a_valid_file_names_the_server() {
        assertEquals(
            "https://api-roborock.example.com:555",
            webAppServer("""{"serverUrl": "https://api-roborock.example.com:555"}"""),
        )
    }

    @Test
    fun a_missing_file_means_no_server_set() {
        assertNull(webAppServer(null))
    }

    @Test
    fun invalid_json_means_no_server_set() {
        assertNull(webAppServer("<!doctype html><title>Not found</title>"))
    }

    @Test
    fun a_file_without_the_key_means_no_server_set() {
        assertNull(webAppServer("""{"server": "https://robots.lan:555"}"""))
    }

    @Test
    fun a_blank_address_means_no_server_set() {
        assertNull(webAppServer("""{"serverUrl": "  "}"""))
    }
}
