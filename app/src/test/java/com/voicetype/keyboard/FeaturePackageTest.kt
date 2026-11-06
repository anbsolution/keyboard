package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturePackageTest {
    @Test fun packageIntegrityRejectsPathTraversal() {
        assertTrue(!PackageIntegrity.isValidPackageId("../theme"))
    }

    @Test fun catalogThemePayloadIsJsonAndHasValidChecksum() {
        val payload = """{"type":"theme","name":"Test","background":"#102B50","keyBackground":"#FFFFFF","foreground":"#173B70"}"""
        val bytes = payload.toByteArray(Charsets.UTF_8)
        assertTrue(PackageIntegrity.verify(bytes, PackageIntegrity.sha256(bytes)))
    }

    @Test fun catalogIdsAreDistinct() {
        val ids = listOf("theme.royal-blue", "theme.royal-gold", "theme.mint-light", "theme.graphite", "theme.violet")
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue(PackageIntegrity.isValidPackageId(it)) }
    }
}
