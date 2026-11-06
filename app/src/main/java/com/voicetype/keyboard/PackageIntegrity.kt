package com.voicetype.keyboard

import java.security.MessageDigest

/** Validates downloaded, data-only packages before the app installs them. */
object PackageIntegrity {
    const val MAX_PACKAGE_BYTES: Int = 2 * 1024 * 1024
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    fun isValidSha256(value: String): Boolean = sha256Pattern.matches(value)

    /** Returns false for malformed, missing, or mismatched checksums. */
    fun verify(bytes: ByteArray, expectedSha256: String): Boolean {
        if (bytes.isEmpty() || bytes.size > MAX_PACKAGE_BYTES) return false
        if (!isValidSha256(expectedSha256)) return false
        return sha256(bytes).equals(expectedSha256, ignoreCase = true)
    }

    /** Package IDs are data identifiers, never file paths or executable names. */
    fun isValidPackageId(value: String): Boolean =
        value.length in 3..80 && Regex("^[a-z0-9]+(?:[.-][a-z0-9]+)*$").matches(value)
}
