package com.kodraliu.localrock.shared.onboarding

private const val ONBOARDING_NEEDS_APP = "Onboarding needs the LocalRock App on a phone"

/** Only Onboarding uses RSA, and the Web App does no Onboarding. */
actual class RsaKeyPair private constructor() {
    actual val publicKeyPem: String
        get() = throw RsaException(ONBOARDING_NEEDS_APP)

    actual fun decryptPkcs1V15Blocks(ciphertext: ByteArray): ByteArray =
        throw RsaException(ONBOARDING_NEEDS_APP)
}

actual fun generateRsa1024KeyPair(): RsaKeyPair = throw RsaException(ONBOARDING_NEEDS_APP)

actual fun importRsa1024KeyPair(privateKeyPkcs8Pem: String): RsaKeyPair =
    throw RsaException(ONBOARDING_NEEDS_APP)
