package com.example.animetracker.data

import android.content.Context

/**
 * Tiny SharedPreferences-backed store for profile customization (display
 * name, banner image path). This is a single unstructured blob of user
 * prefs rather than a list of records, so it doesn't need Room.
 *
 * The banner is stored as a path to a copy of the image inside the app's
 * private files directory (see [AnimeViewModel.setProfileBanner]) rather
 * than the original content:// URI, since picker URIs aren't guaranteed to
 * remain readable across app restarts.
 */
class ProfilePrefs(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    fun getBannerPath(): String? = prefs.getString(KEY_BANNER_PATH, null)

    fun setBannerPath(path: String?) {
        prefs.edit().putString(KEY_BANNER_PATH, path).apply()
    }

    fun getDisplayName(): String = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply()
    }

    companion object {
        private const val KEY_BANNER_PATH = "banner_path"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
