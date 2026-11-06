package com.artcompanion.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Stores each profile as its own JSON file in [dir] (`<id>.json`). Writes are atomic
 * (temp file + rename), so a crash mid-save cannot corrupt a profile.
 *
 * All mutations go through [update], which copies the profile. Profiles are immutable data
 * classes, so no state is shared between them.
 */
class ProfileRepository(private val dir: File) {

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val lock = Any()

    init {
        dir.mkdirs()
        load()
    }

    private fun load() = synchronized(lock) {
        val loaded = dir.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { f ->
            runCatching { json.decodeFromString(Profile.serializer(), f.readText()) }.getOrNull()
        }.associateBy { it.id }.toMutableMap()
        // Seed any missing built-in profile (first launch, or the user deleted the file).
        DefaultProfiles.all().forEach { def ->
            if (def.id !in loaded) { loaded[def.id] = def; write(def) }
        }
        _profiles.value = sorted(loaded.values)
    }

    private fun sorted(ps: Collection<Profile>): List<Profile> {
        val order = listOf(DefaultProfiles.PROCREATE_ID, DefaultProfiles.ARTSTUDIO_ID, DefaultProfiles.CUSTOM_ID)
        return ps.sortedWith(compareBy<Profile>({ order.indexOf(it.id).let { i -> if (i < 0) 99 else i } }, { it.name.lowercase() }))
    }

    fun get(id: String): Profile? = _profiles.value.firstOrNull { it.id == id }

    fun update(id: String, transform: (Profile) -> Profile): Profile? = synchronized(lock) {
        val cur = get(id) ?: return null
        val next = transform(cur).copy(id = cur.id, builtIn = cur.builtIn)
        write(next)
        _profiles.value = sorted(_profiles.value.map { if (it.id == id) next else it })
        next
    }

    /** Creates a new user profile as an independent deep copy of [sourceId]. */
    fun duplicate(sourceId: String, newName: String): Profile? = synchronized(lock) {
        val src = get(sourceId) ?: return null
        val copy = src.copy(id = "user_" + UUID.randomUUID().toString().take(8), name = newName, builtIn = false)
        write(copy)
        _profiles.value = sorted(_profiles.value + copy)
        copy
    }

    /** Deletes a user profile. Built-in profiles can only be reset, not deleted. */
    fun delete(id: String): Boolean = synchronized(lock) {
        val p = get(id) ?: return false
        if (p.builtIn) return false
        File(dir, "$id.json").delete()
        _profiles.value = _profiles.value.filterNot { it.id == id }
        true
    }

    /** Restores a built-in profile to its documented defaults; user profiles reset to their target's defaults. */
    fun resetToDefaults(id: String): Profile? = update(id) { cur ->
        val def = DefaultProfiles.builtInById(id) ?: DefaultProfiles.forTarget(cur.target)
        def.copy(name = if (cur.builtIn) def.name else cur.name)
    }

    fun resetAll() = synchronized(lock) {
        dir.listFiles().orEmpty().forEach { it.delete() }
        load()
    }

    /** Replaces every profile (used by backup restore). Built-ins missing from [all] are re-seeded. */
    fun replaceAll(all: List<Profile>) = synchronized(lock) {
        dir.listFiles().orEmpty().forEach { it.delete() }
        all.forEach { write(it) }
        load()
    }

    private fun write(p: Profile) {
        val tmp = File(dir, "${p.id}.json.tmp")
        tmp.writeText(json.encodeToString(Profile.serializer(), p))
        val target = File(dir, "${p.id}.json")
        if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
        fun newId(): String = UUID.randomUUID().toString().take(8)
    }
}

/** Pure helpers for editing a profile's pages and buttons. */
object ProfileEdits {
    fun upsertButton(p: Profile, pageId: String, button: DeckButton): Profile = p.copy(pages = p.pages.map { page ->
        if (page.id != pageId) page
        else if (page.buttons.any { it.id == button.id }) page.copy(buttons = page.buttons.map { if (it.id == button.id) button else it })
        else page.copy(buttons = page.buttons + button)
    })

    fun deleteButton(p: Profile, pageId: String, buttonId: String): Profile = p.copy(pages = p.pages.map { page ->
        if (page.id != pageId) page else page.copy(buttons = page.buttons.filterNot { it.id == buttonId })
    })

    /** Moves a button by [delta] positions within its page (clamped). */
    fun moveButton(p: Profile, pageId: String, buttonId: String, delta: Int): Profile = p.copy(pages = p.pages.map { page ->
        if (page.id != pageId) return@map page
        val list = page.buttons.toMutableList()
        val i = list.indexOfFirst { it.id == buttonId }
        if (i < 0) return@map page
        val j = (i + delta).coerceIn(0, list.lastIndex)
        list.add(j, list.removeAt(i))
        page.copy(buttons = list)
    })

    fun addPage(p: Profile, name: String): Profile = p.copy(pages = p.pages + DeckPage("pg_" + ProfileRepository.newId(), name, emptyList()))

    fun renamePage(p: Profile, pageId: String, name: String): Profile =
        p.copy(pages = p.pages.map { if (it.id == pageId) it.copy(name = name) else it })

    fun deletePage(p: Profile, pageId: String): Profile =
        if (p.pages.size <= 1) p else p.copy(pages = p.pages.filterNot { it.id == pageId })

    fun movePage(p: Profile, pageId: String, delta: Int): Profile {
        val list = p.pages.toMutableList()
        val i = list.indexOfFirst { it.id == pageId }
        if (i < 0) return p
        list.add((i + delta).coerceIn(0, list.lastIndex), list.removeAt(i))
        return p.copy(pages = list)
    }
}
