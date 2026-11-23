package com.kodraliu.localrock.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Web App's deployment names its Server in `config.json`, next to `index.html`. */
class WebAppConfigTest {

    @Test
    fun a_valid_file_names_the_server() {
        assertEquals(
            WebAppServer.Set("https://api-roborock.dinglreiter.eu:555"),
            webAppServer("""{"serverUrl": "https://api-roborock.dinglreiter.eu:555"}"""),
        )
    }

    @Test
    fun a_missing_file_means_no_server_set() {
        assertEquals(WebAppServer.NotSet, webAppServer(null))
    }

    @Test
    fun invalid_json_means_no_server_set() {
        assertEquals(WebAppServer.NotSet, webAppServer("<!doctype html><title>Not found</title>"))
    }

    @Test
    fun a_file_without_the_key_means_no_server_set() {
        assertEquals(WebAppServer.NotSet, webAppServer("""{"server": "https://robots.lan:555"}"""))
    }

    @Test
    fun a_blank_address_means_no_server_set() {
        assertEquals(WebAppServer.NotSet, webAppServer("""{"serverUrl": "  "}"""))
    }
}
