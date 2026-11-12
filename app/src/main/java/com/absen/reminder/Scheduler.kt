package com.absen.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object Scheduler {
    private fun pi(c: Context, t: Int) = PendingIntent.getBroadcast(
        c, 100 + t, Intent(c, AlarmReceiver::class.java).putExtra("type", t),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun schedule(c: Context, t: Int) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(c, t))
        val s = Prefs.load(c, t)
        if (!s.enabled || s.days.isEmpty()) return
        val next = nextTrigger(s) ?: return
        val show = PendingIntent.getActivity(
            c, 300 + t, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(next, show), pi(c, t))
    }

    fun scheduleAll(c: Context) {
        schedule(c, TYPE_MASUK); schedule(c, TYPE_PULANG)
    }

    private fun nextTrigger(s: Sched): Long? {
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, s.hour); set(Calendar.MINUTE, s.minute)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis > now && cal.get(Calendar.DAY_OF_WEEK) in s.days) return cal.timeInMillis
        }
        return null
    }
}
