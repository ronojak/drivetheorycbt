package com.drivetheory.cbt.core.gating

import android.content.Context
import com.drivetheory.cbt.core.security.EncryptedPrefs
import kotlin.math.max
import kotlin.math.min

data class GateDecision(val allowed: Boolean, val allowedCount: Int, val reason: String? = null)

class AccessGate(context: Context) {
    private val prefs = EncryptedPrefs.get(context)

    private fun todayKey(): String = "day_" + (System.currentTimeMillis() / (24L * 60 * 60 * 1000))

    /**
     * Free users: allow up to 10 questions per category (vehicle) per day.
     * Premium users: no limit.
     */
    fun canStartSession(isPremium: Boolean, requestedCount: Int, vehicle: String): GateDecision {
        if (isPremium) return GateDecision(true, requestedCount)

        val dayKey = todayKey()
        val key = "${dayKey}_cat_${vehicle.lowercase()}"
        val consumed = prefs.getLong(key, 0L).toInt()
        val dailyLimit = 10
        val remaining = max(0, dailyLimit - consumed)
        if (remaining <= 0) return GateDecision(false, 0, "Daily free limit reached. Upgrade to continue.")

        val allowedCount = min(remaining, requestedCount)
        return GateDecision(allowedCount > 0, allowedCount)
    }

    fun recordUsage(vehicle: String, usedCount: Int) {
        if (usedCount <= 0) return
        val dayKey = todayKey()
        val key = "${dayKey}_cat_${vehicle.lowercase()}"
        val consumed = prefs.getLong(key, 0L)
        prefs.putLong(key, (consumed + usedCount).coerceAtLeast(0))
    }

    fun consumedToday(vehicle: String): Int {
        val dayKey = todayKey()
        val key = "${dayKey}_cat_${vehicle.lowercase()}"
        return prefs.getLong(key, 0L).toInt()
    }

    fun resetTodayAll() {
        val dayKey = todayKey()
        listOf("car", "motorcycle", "lorry", "buscoach").forEach { v ->
            val key = "${dayKey}_cat_${v}"
            prefs.putLong(key, 0L)
        }
    }
}
