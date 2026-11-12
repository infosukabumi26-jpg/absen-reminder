package com.absen.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val t = i.getIntExtra("type", TYPE_MASUK)
        Scheduler.schedule(c, t) // jadwalkan kemunculan berikutnya
        ContextCompat.startForegroundService(c, Intent(c, AlarmService::class.java).putExtra("type", t))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = Scheduler.scheduleAll(c)
}
