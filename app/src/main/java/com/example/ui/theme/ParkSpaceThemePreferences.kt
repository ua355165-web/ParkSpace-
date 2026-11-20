package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists explicit user Light/Dark theme selection across cold app restarts.
 * A null value indicates following the Android system appearance.
 */
interface ThemePreferenceStore {
  val darkThemeOverride: StateFlow<Boolean?>

  fun setDarkTheme(isDark: Boolean)

  fun toggleTheme(currentEffectiveDark: Boolean)

  fun clearOverride()
}

class SharedPrefsThemePreferenceStore(context: Context) : ThemePreferenceStore {
  private val prefs =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _darkThemeOverride = MutableStateFlow(readInitialOverride())
  override val darkThemeOverride: StateFlow<Boolean?> = _darkThemeOverride.asStateFlow()

  private fun readInitialOverride(): Boolean? =
    if (prefs.contains(KEY_DARK_THEME_OVERRIDE)) {
      prefs.getBoolean(KEY_DARK_THEME_OVERRIDE, false)
    } else {
      null
    }

  override fun setDarkTheme(isDark: Boolean) {
    prefs.edit().putBoolean(KEY_DARK_THEME_OVERRIDE, isDark).apply()
    _darkThemeOverride.value = isDark
  }

  override fun toggleTheme(currentEffectiveDark: Boolean) {
    setDarkTheme(!currentEffectiveDark)
  }

  override fun clearOverride() {
    prefs.edit().remove(KEY_DARK_THEME_OVERRIDE).apply()
    _darkThemeOverride.value = null
  }

  companion object {
    const val PREFS_NAME = "parkspace_theme_prefs"
    private const val KEY_DARK_THEME_OVERRIDE = "dark_theme_override"
  }
}
