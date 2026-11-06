package com.artcompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artcompanion.ArtCompanionApp
import com.artcompanion.data.AppSettings
import com.artcompanion.data.BackupCodec
import com.artcompanion.data.DeckButton
import com.artcompanion.data.Profile
import com.artcompanion.data.ProfileEdits
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ArtCompanionApp
    val hid = app.hid
    val dispatcher = app.dispatcher
    private val profileRepo = app.profiles
    private val settingsRepo = app.settings

    val settings: StateFlow<AppSettings> = settingsRepo.settings
    val profiles: StateFlow<List<Profile>> = profileRepo.profiles

    val activeProfile: StateFlow<Profile?> = combine(profiles, settings) { ps, s ->
        ps.firstOrNull { it.id == s.activeProfileId } ?: ps.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateSettings(t: (AppSettings) -> AppSettings) = settingsRepo.update(t)
    fun selectProfile(id: String) = settingsRepo.update { it.copy(activeProfileId = id) }

    // ---- deck editing (always applied to one profile by id)
    fun upsertButton(profileId: String, pageId: String, b: DeckButton) =
        profileRepo.update(profileId) { ProfileEdits.upsertButton(it, pageId, b) }
    fun deleteButton(profileId: String, pageId: String, buttonId: String) =
        profileRepo.update(profileId) { ProfileEdits.deleteButton(it, pageId, buttonId) }
    fun moveButton(profileId: String, pageId: String, buttonId: String, delta: Int) =
        profileRepo.update(profileId) { ProfileEdits.moveButton(it, pageId, buttonId, delta) }
    fun addPage(profileId: String, name: String) = profileRepo.update(profileId) { ProfileEdits.addPage(it, name) }
    fun renamePage(profileId: String, pageId: String, name: String) =
        profileRepo.update(profileId) { ProfileEdits.renamePage(it, pageId, name) }
    fun deletePage(profileId: String, pageId: String) = profileRepo.update(profileId) { ProfileEdits.deletePage(it, pageId) }
    fun movePage(profileId: String, pageId: String, delta: Int) =
        profileRepo.update(profileId) { ProfileEdits.movePage(it, pageId, delta) }

    // ---- profile management
    fun duplicateProfile(id: String, name: String) = profileRepo.duplicate(id, name)?.also { selectProfile(it.id) }
    fun renameProfile(id: String, name: String) = profileRepo.update(id) { it.copy(name = name) }
    fun deleteProfile(id: String) {
        if (profileRepo.delete(id) && settings.value.activeProfileId == id) {
            profiles.value.firstOrNull()?.let { selectProfile(it.id) }
        }
    }
    fun resetProfile(id: String) = profileRepo.resetToDefaults(id)

    // ---- backup
    fun exportBackup(): String = BackupCodec.encode(settings.value, profiles.value)

    /** Returns null on success, or an error message. */
    fun importBackup(text: String): String? = runCatching {
        val b = BackupCodec.decode(text)
        profileRepo.replaceAll(b.profiles)
        settingsRepo.update { b.settings }
        if (profiles.value.none { it.id == b.settings.activeProfileId }) profiles.value.firstOrNull()?.let { selectProfile(it.id) }
    }.exceptionOrNull()?.let { it.message ?: "Import failed" }

    fun resetEverything() {
        profileRepo.resetAll()
        settingsRepo.reset()
    }
}
