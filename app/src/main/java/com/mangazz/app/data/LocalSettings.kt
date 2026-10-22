package com.mangazz.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore(name = "zz_settings")
class LocalSettings(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val readingKey = stringPreferencesKey("reading")
    val theme = context.preferences.data.map { it[themeKey] ?: "blue" }
    val reading = context.preferences.data.map { it[readingKey] ?: "عمودي" }
    suspend fun setTheme(value: String) { context.preferences.edit { it[themeKey] = value } }
    suspend fun setReading(value: String) { context.preferences.edit { it[readingKey] = value } }
}
