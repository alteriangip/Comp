package com.artcompanion.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import com.artcompanion.ArtCompanionApp
import com.artcompanion.data.DefaultProfiles
import com.artcompanion.data.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real app shell on the JVM (Robolectric native graphics) to check layouts and
 * editing flows. Bluetooth is not started here, so the deck shows its "not connected" state.
 * Screenshots go to build/ui-screens/ for visual review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: ArtCompanionApp
    private lateinit var vm: AppViewModel

    @Before fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.profiles.resetAll()
        app.settings.reset()
        vm = AppViewModel(app)
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        val dir = File("build/ui-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setApp() = compose.setContent { AppRoot(vm, {}, {}, {}) }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun deckPortrait() {
        setApp()
        compose.onNodeWithText("Procreate").assertIsDisplayed()
        compose.onNodeWithText("Undo").assertIsDisplayed()
        compose.onNodeWithText("Starting…").assertIsDisplayed() // Bluetooth is not started in tests
        shot("deck_procreate_portrait")
    }

    @Test @Config(qualifiers = "w852dp-h393dp-land-xhdpi")
    fun deckLandscapeArtstudio() {
        vm.selectProfile(DefaultProfiles.ARTSTUDIO_ID)
        setApp()
        compose.onNodeWithText("Artstudio Pro").assertIsDisplayed()
        compose.onNodeWithText("Eyedropper").assertIsDisplayed()
        shot("deck_artstudio_landscape")
        compose.onNodeWithText("View").performClick()
        compose.onNodeWithText("Zoom in").assertIsDisplayed()
        shot("deck_artstudio_view_landscape")
    }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun tapWhileDisconnectedExplainsInsteadOfPretending() {
        setApp()
        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Not connected to an iPad. Open Connection to pair.").assertIsDisplayed()
    }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun editButtonPersistsOnlyInThatProfile() {
        setApp()
        compose.onNodeWithContentDescription("Edit deck").performClick()
        shot("deck_edit_mode")
        compose.onNodeWithText("Undo").performClick()
        compose.onNodeWithText("Edit button").assertIsDisplayed()
        compose.onNodeWithText("Label").performTextReplacement("Back")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Back").assertIsDisplayed()

        assertEquals("Back", app.profiles.get(DefaultProfiles.PROCREATE_ID)!!.pages[0].buttons.first { it.id == "undo" }.label)
        assertEquals("Undo", app.profiles.get(DefaultProfiles.ARTSTUDIO_ID)!!.pages[0].buttons.first { it.id == "undo" }.label)
    }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun addButtonWithCustomChord() {
        vm.selectProfile(DefaultProfiles.CUSTOM_ID)
        setApp()
        compose.onNodeWithContentDescription("Edit deck").performClick()
        compose.onNodeWithContentDescription("Add button").performClick()
        compose.onNodeWithText("Label").performTextReplacement("Save")
        compose.onNodeWithText("⌘ Cmd").performClick()
        compose.onNodeWithText("Key").performTextReplacement("s")
        compose.onNodeWithText("Sends ⌘S", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("Save")[0].performClick() // top-bar Save action
        val b = app.profiles.get(DefaultProfiles.CUSTOM_ID)!!.pages[0].buttons.last()
        assertEquals("Save", b.label)
        assertEquals("Cmd+S", b.chord)
    }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun connectionAndSettingsScreens() {
        setApp()
        compose.onNodeWithText("Connection").performClick()
        compose.onNodeWithText("Troubleshooting").assertIsDisplayed()
        shot("connection_portrait")
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Appearance").assertIsDisplayed()
        shot("settings_portrait")
    }

    @Test @Config(qualifiers = "w393dp-h852dp-port-xhdpi")
    fun lightTheme() {
        vm.updateSettings { it.copy(theme = ThemeMode.LIGHT) }
        setApp()
        shot("deck_light_portrait")
    }
}
