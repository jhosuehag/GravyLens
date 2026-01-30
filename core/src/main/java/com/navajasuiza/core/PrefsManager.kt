package com.navajasuiza.core

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "navaja_suiza_prefs"
        private const val KEY_HANDLE_X = "handle_x"
        private const val KEY_HANDLE_Y = "handle_y"
        private const val KEY_IS_HIDDEN = "is_hidden"
    }

    var handleXPortrait: Int
        get() = prefs.getInt("handle_x_port", 0)
        set(value) = prefs.edit().putInt("handle_x_port", value).apply()

    var handleYPortrait: Int
        get() = prefs.getInt("handle_y_port", 200)
        set(value) = prefs.edit().putInt("handle_y_port", value).apply()

    var handleXLandscape: Int
        get() = prefs.getInt("handle_x_land", 0)
        set(value) = prefs.edit().putInt("handle_x_land", value).apply()

    var handleYLandscape: Int
        get() = prefs.getInt("handle_y_land", 100)
        set(value) = prefs.edit().putInt("handle_y_land", value).apply()

    var isHidden: Boolean
        get() = prefs.getBoolean(KEY_IS_HIDDEN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_HIDDEN, value).apply()

    var isHistoryEnabled: Boolean
        get() = prefs.getBoolean("is_history_enabled", true)
        set(value) = prefs.edit().putBoolean("is_history_enabled", value).apply()
}
