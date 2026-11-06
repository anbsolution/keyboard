package com.voicetype.keyboard

import org.junit.Assert.*
import org.junit.Test

class PackageIntegrityTest {
    @Test fun sha256MatchesKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", PackageIntegrity.sha256("abc".toByteArray()))
    }
    @Test fun rejectsTamperedAndOversizedPackages() {
        val bytes = "safe".toByteArray()
        assertTrue(PackageIntegrity.verify(bytes, PackageIntegrity.sha256(bytes)))
        assertFalse(PackageIntegrity.verify("changed".toByteArray(), PackageIntegrity.sha256(bytes)))
        assertFalse(PackageIntegrity.verify(ByteArray(PackageIntegrity.MAX_PACKAGE_BYTES + 1), "0".repeat(64)))
    }
    @Test fun packageIdsCannotEscapePackageDirectory() {
        assertFalse(PackageIntegrity.isValidPackageId("../theme"))
        assertFalse(PackageIntegrity.isValidPackageId("theme/../../x"))
        assertTrue(PackageIntegrity.isValidPackageId("theme.royal-blue"))
    }
}
