package com.artcompanion.data

import kotlinx.serialization.Serializable

/** A full configuration backup: settings plus every profile. Palettes join this in Phase 4. */
@Serializable
data class Backup(
    val format: String = FORMAT,
    val version: Int = 1,
    val createdAtMs: Long,
    val settings: AppSettings,
    val profiles: List<Profile>,
) {
    companion object { const val FORMAT = "artcompanion-backup" }
}

object BackupCodec {
    fun encode(settings: AppSettings, profiles: List<Profile>, now: Long = System.currentTimeMillis()): String =
        ProfileRepository.json.encodeToString(Backup.serializer(), Backup(createdAtMs = now, settings = settings, profiles = profiles))

    /** Parses and validates a backup. Throws IllegalArgumentException with a readable message. */
    fun decode(text: String): Backup {
        val b = runCatching { ProfileRepository.json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("Not an ArtCompanion backup file") }
        require(b.format == Backup.FORMAT) { "Not an ArtCompanion backup file" }
        require(b.version <= 1) { "Backup was made by a newer version of ArtCompanion" }
        require(b.profiles.map { it.id }.toSet().size == b.profiles.size) { "Backup contains duplicate profiles" }
        val bad = b.profiles.flatMap { p -> p.pages.flatMap { it.buttons } }
            .firstOrNull { com.artcompanion.hid.KeyChord.parseOrNull(it.chord) == null }
        require(bad == null) { "Button \"${bad?.label}\" has an invalid shortcut \"${bad?.chord}\"" }
        return b
    }
}
