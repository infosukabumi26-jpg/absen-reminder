package com.absen.reminder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {
    companion object {
        @Volatile var active = false
        const val ACTION_STOP = "com.absen.reminder.STOP"
        const val CH = "absen_alarm"
        const val NID = 1001
    }

    private var player: MediaPlayer? = null
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAlarm(); return START_NOT_STICKY }
        val type = intent?.getIntExtra("type", TYPE_MASUK) ?: TYPE_MASUK
        val title = if (type == TYPE_MASUK) "WAKTUNYA ABSEN MASUK!" else "WAKTUNYA ABSEN PULANG!"

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH, "Alarm Absen", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        val ai = Intent(this, AlarmActivity::class.java).putExtra("type", type)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(this, 200 + type, ai,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Ketuk untuk membuka halaman absen")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        ServiceCompat.startForeground(this, NID, n,
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)

        active = true
        startSound(type)
        try { startActivity(ai) } catch (_: Exception) { /* full-screen intent notifikasi jadi cadangan */ }
        return START_NOT_STICKY
    }

    private fun startSound(type: Int) {
        player?.release()
        val custom = Prefs.load(this, type).ringtone?.let { Uri.parse(it) }
        val default = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = try { create(custom ?: default) } catch (_: Exception) { create(default) }
        player?.start()
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
            .vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    private fun create(uri: Uri) = MediaPlayer().apply {
        setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        setDataSource(this@AlarmService, uri)
        isLooping = true
        prepare()
    }

    private fun stopAlarm() {
        active = false
        player?.release(); player = null
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { player?.release(); player = null; active = false; super.onDestroy() }
}
