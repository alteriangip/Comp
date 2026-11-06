package com.artcompanion.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test fun roundTrip() {
        val settings = AppSettings(theme = ThemeMode.LIGHT, haptics = false, buttonScale = ButtonScale.LARGE, activeProfileId = DefaultProfiles.ARTSTUDIO_ID)
        val text = BackupCodec.encode(settings, DefaultProfiles.all(), now = 42)
        val b = BackupCodec.decode(text)
        assertEquals(settings, b.settings)
        assertEquals(DefaultProfiles.all(), b.profiles)
        assertEquals(42L, b.createdAtMs)
    }

    @Test fun rejectsForeignJson() {
        assertFails("Not an ArtCompanion backup file") { BackupCodec.decode("""{"hello":"world"}""") }
        assertFails("Not an ArtCompanion backup file") { BackupCodec.decode("garbage") }
    }

    @Test fun rejectsInvalidShortcut() {
        val bad = DefaultProfiles.custom().let { p ->
            p.copy(pages = listOf(p.pages[0].copy(buttons = listOf(DeckButton("b", "Bad", "Cmd+Banana")))))
        }
        assertFails("invalid shortcut") { BackupCodec.decode(BackupCodec.encode(AppSettings(), listOf(bad))) }
    }

    @Test fun rejectsNewerVersion() {
        val text = BackupCodec.encode(AppSettings(), DefaultProfiles.all()).replace("\"version\":1", "\"version\":99")
        assertFails("newer version") { BackupCodec.decode(text) }
    }

    @Test fun toleratesUnknownFields() {
        val text = BackupCodec.encode(AppSettings(), DefaultProfiles.all()).replaceFirst("{", "{\"futureField\":true,")
        BackupCodec.decode(text)
    }

    private fun assertFails(fragment: String, block: () -> Unit) {
        val e = runCatching(block).exceptionOrNull()
        assertTrue("expected failure containing \"$fragment\", got $e", e is IllegalArgumentException && e.message!!.contains(fragment))
    }
}
