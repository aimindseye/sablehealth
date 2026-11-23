package io.github.aimindseye.sablehealth

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Local user-declared Google Health sync, never a verified Fitbit/Health Connect connection. */
internal class CompanionSyncPreferenceState(
    private val preferences: SharedPreferences,
) {
    var confirmed by mutableStateOf(preferences.getBoolean("fitbit.sync.user_confirmed", false))
        private set

    fun chooseConfirmation(value: Boolean) {
        confirmed = value
        preferences.edit().putBoolean("fitbit.sync.user_confirmed", value).apply()
    }
}
