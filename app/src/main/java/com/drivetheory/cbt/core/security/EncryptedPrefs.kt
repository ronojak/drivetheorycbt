package com.drivetheory.cbt.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefs private constructor(context: Context) {
    private val prefs: SharedPreferences = try {
        val appCtx = context.applicationContext
        val masterKey = MasterKey.Builder(appCtx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appCtx,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Throwable) {
        // Fallback to normal SharedPreferences to avoid runtime crashes on devices without crypto providers or keystore issues
        context.applicationContext.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun getBoolean(key: String, def: Boolean = false): Boolean = prefs.getBoolean(key, def)
    fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun getString(key: String, def: String? = null): String? = prefs.getString(key, def)
    fun putString(key: String, value: String?) { prefs.edit().putString(key, value).apply() }
    fun getLong(key: String, def: Long = 0L): Long = prefs.getLong(key, def)
    fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }

    companion object {
        @Volatile private var instance: EncryptedPrefs? = null
        fun get(context: Context): EncryptedPrefs = instance ?: synchronized(this) {
            instance ?: EncryptedPrefs(context).also { instance = it }
        }
    }
}
