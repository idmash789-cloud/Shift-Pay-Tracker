package com.shiftpay.tracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * User preferences live in DataStore (Preferences), not in Room.
 * Only shift/job/pay data lives in the Room database.
 */
data class AppSettings(
    val mileageRate: Double = 0.70,
    val defaultJobId: Long? = null,
    val currency: String = "USD"
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val MILEAGE_RATE = doublePreferencesKey("mileage_rate")
        val DEFAULT_JOB_ID = longPreferencesKey("default_job_id")
        val CURRENCY = stringPreferencesKey("currency")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            mileageRate = prefs[Keys.MILEAGE_RATE] ?: 0.70,
            defaultJobId = prefs[Keys.DEFAULT_JOB_ID],
            currency = prefs[Keys.CURRENCY] ?: "USD"
        )
    }

    suspend fun getSettings(): AppSettings = settings.first()

    suspend fun saveSettings(value: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MILEAGE_RATE] = value.mileageRate
            if (value.defaultJobId != null) prefs[Keys.DEFAULT_JOB_ID] = value.defaultJobId
            else prefs.remove(Keys.DEFAULT_JOB_ID)
            prefs[Keys.CURRENCY] = value.currency
        }
    }
}
