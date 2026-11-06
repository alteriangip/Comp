package com.artcompanion.data

import com.artcompanion.hid.KeyChord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repo() = ProfileRepository(tmp.root)

    @Test fun seedsThreeBuiltInProfiles() {
        val r = repo()
        assertEquals(listOf(DefaultProfiles.PROCREATE_ID, DefaultProfiles.ARTSTUDIO_ID, DefaultProfiles.CUSTOM_ID), r.profiles.value.map { it.id })
        assertTrue(r.profiles.value.all { it.builtIn })
    }

    @Test fun everyDefaultButtonHasAValidShortcutAndUniqueIds() {
        DefaultProfiles.all().forEach { p ->
            val ids = p.pages.flatMap { it.buttons }.map { it.id }
            p.pages.forEach { pg ->
                val pageIds = pg.buttons.map { it.id }
                assertEquals("duplicate button id in ${p.id}/${pg.id}", pageIds.size, pageIds.toSet().size)
            }
            assertEquals("duplicate page id in ${p.id}", p.pages.size, p.pages.map { it.id }.toSet().size)
            p.pages.flatMap { it.buttons }.forEach { b -> assertNotNull("${p.id}/${b.id}: ${b.chord}", KeyChord.parseOrNull(b.chord)) }
            assertTrue(ids.isNotEmpty())
        }
    }

    @Test fun editsPersistAcrossInstances() {
        val r = repo()
        r.update(DefaultProfiles.PROCREATE_ID) { ProfileEdits.upsertButton(it, "p_main", DeckButton("x", "Test", "Cmd+K")) }
        val again = repo()
        val b = again.get(DefaultProfiles.PROCREATE_ID)!!.pages.first { it.id == "p_main" }.buttons.last()
        assertEquals("x", b.id)
        assertEquals("Cmd+K", b.chord)
    }

    @Test fun editingOneProfileDoesNotChangeAnother() {
        val r = repo()
        val beforeArtstudio = r.get(DefaultProfiles.ARTSTUDIO_ID)
        r.update(DefaultProfiles.PROCREATE_ID) { p ->
            ProfileEdits.upsertButton(p, "p_main", p.pages[0].buttons.first { it.id == "undo" }.copy(label = "UNDO!", chord = "Cmd+U"))
        }
        assertEquals(beforeArtstudio, r.get(DefaultProfiles.ARTSTUDIO_ID))
        assertEquals("Cmd+Z", r.get(DefaultProfiles.ARTSTUDIO_ID)!!.pages[0].buttons.first { it.id == "undo" }.chord)
        assertEquals("Cmd+U", r.get(DefaultProfiles.PROCREATE_ID)!!.pages[0].buttons.first { it.id == "undo" }.chord)
    }

    @Test fun duplicateIsIndependent() {
        val r = repo()
        val copy = r.duplicate(DefaultProfiles.PROCREATE_ID, "Mine")!!
        assertFalse(copy.builtIn)
        assertNotEquals(DefaultProfiles.PROCREATE_ID, copy.id)
        r.update(copy.id) { ProfileEdits.deleteButton(it, "p_main", "undo") }
        assertTrue(r.get(DefaultProfiles.PROCREATE_ID)!!.pages[0].buttons.any { it.id == "undo" })
        assertFalse(r.get(copy.id)!!.pages[0].buttons.any { it.id == "undo" })
    }

    @Test fun builtInsCannotBeDeletedButUserProfilesCan() {
        val r = repo()
        assertFalse(r.delete(DefaultProfiles.PROCREATE_ID))
        val copy = r.duplicate(DefaultProfiles.CUSTOM_ID, "Krita-ish")!!
        assertTrue(r.delete(copy.id))
        assertNull(repo().get(copy.id))
    }

    @Test fun resetRestoresDocumentedDefaults() {
        val r = repo()
        r.update(DefaultProfiles.ARTSTUDIO_ID) { it.copy(name = "Changed", pages = listOf(DeckPage("only", "Only", emptyList()))) }
        r.resetToDefaults(DefaultProfiles.ARTSTUDIO_ID)
        assertEquals(DefaultProfiles.artstudioPro(), r.get(DefaultProfiles.ARTSTUDIO_ID))
    }

    @Test fun pageAndButtonOrdering() {
        var p = DefaultProfiles.custom()
        p = ProfileEdits.addPage(p, "Two")
        assertEquals(2, p.pages.size)
        val second = p.pages[1].id
        p = ProfileEdits.movePage(p, second, -1)
        assertEquals(second, p.pages[0].id)
        p = ProfileEdits.deletePage(p, second)
        assertEquals(1, p.pages.size)
        assertEquals(p, ProfileEdits.deletePage(p, p.pages[0].id)) // last page can't be deleted

        val pg = p.pages[0].id
        p = ProfileEdits.moveButton(p, pg, "redo", -1)
        assertEquals(listOf("redo", "undo"), p.pages[0].buttons.map { it.id })
        p = ProfileEdits.moveButton(p, pg, "redo", -5) // clamped
        assertEquals(listOf("redo", "undo"), p.pages[0].buttons.map { it.id })
    }

    @Test fun corruptFileIsIgnoredAndBuiltInReseeded() {
        tmp.root.resolve("procreate.json").writeText("{ not json")
        val r = repo()
        assertEquals(DefaultProfiles.procreate(), r.get(DefaultProfiles.PROCREATE_ID))
    }
}
