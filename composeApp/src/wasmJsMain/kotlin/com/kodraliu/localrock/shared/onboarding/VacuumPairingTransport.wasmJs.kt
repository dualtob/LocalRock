package com.kodraliu.localrock.shared.onboarding

/** Why every Onboarding call fails in the Web App. */
internal const val ONBOARDING_NEEDS_APP = "Onboarding needs the LocalRock App on a phone"

/** Onboarding joins the Robot's own Wi-Fi and speaks raw UDP, which a browser cannot do. */
actual val vacuumPairingSupported: Boolean = false

actual class VacuumPairingTransport : AutoCloseable {
    actual suspend fun joinVacuumWifi(ssidPrefix: String, timeoutMs: Long): Unit =
        throw VacuumPairingException(ONBOARDING_NEEDS_APP)

    actual suspend fun joinedSsid(): String? = null

    actual suspend fun sendUdp(host: String, port: Int, data: ByteArray): Unit =
        throw VacuumPairingException(ONBOARDING_NEEDS_APP)

    actual suspend fun receiveUdp(timeoutMs: Long): ByteArray? =
        throw VacuumPairingException(ONBOARDING_NEEDS_APP)

    actual override fun close() = Unit
}

actual fun createVacuumPairingTransport(): VacuumPairingTransport =
    throw VacuumPairingException(ONBOARDING_NEEDS_APP)
