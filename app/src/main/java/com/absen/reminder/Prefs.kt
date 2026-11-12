package com.absen.reminder

import android.content.Context

const val TYPE_MASUK = 0
const val TYPE_PULANG = 1
const val ABSEN_URL = "https://star-asn.kemenimipas.go.id/authentication/login"

data class Sched(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,       // Calendar.SUNDAY(1)..SATURDAY(7)
    val ringtone: String?     // null = dering alarm bawaan
)

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("absen", Context.MODE_PRIVATE)

    fun load(c: Context, t: Int): Sched {
        val p = sp(c)
        return Sched(
            p.getBoolean("en$t", false),
            p.getInt("h$t", if (t == TYPE_MASUK) 7 else 16),
            p.getInt("m$t", 0),
            p.getString("d$t", "2,3,4,5,6")!!.split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
            p.getString("r$t", null)
        )
    }

    fun save(c: Context, t: Int, s: Sched) {
        sp(c).edit()
            .putBoolean("en$t", s.enabled).putInt("h$t", s.hour).putInt("m$t", s.minute)
            .putString("d$t", s.days.joinToString(",")).putString("r$t", s.ringtone)
            .apply()
    }
}
