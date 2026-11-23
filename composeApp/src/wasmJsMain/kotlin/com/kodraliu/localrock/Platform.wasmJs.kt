package com.kodraliu.localrock

class WebPlatform : Platform {
    override val name: String = "Web"
    override val isWebApp: Boolean = true
}

actual fun getPlatform(): Platform = WebPlatform()
