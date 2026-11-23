package com.kodraliu.localrock

interface Platform {
    val name: String

    /** True in the Web App, which uses the page's own origin as the Server and has no Server setup. */
    val isWebApp: Boolean get() = false
}

expect fun getPlatform(): Platform