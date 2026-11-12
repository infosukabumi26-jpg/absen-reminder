package com.absen.reminder

import android.Manifest
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var pendingType = TYPE_MASUK
    private val labels = mutableMapOf<Int, TextView>()

    private val ringtonePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33)
            r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        else @Suppress("DEPRECATION") r.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) setRingtone(pendingType, uri)
    }
    private val songPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            setRingtone(pendingType, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 80) }
        root.addView(TextView(this).apply {
            text = "Absen Reminder"; textSize = 26f; setTypeface(null, Typeface.BOLD)
        })
        root.addView(Button(this).apply {
            text = "Periksa Izin (Notifikasi & Layar Penuh)"
            setOnClickListener { requestPermissions() }
        })
        section(root, TYPE_MASUK, "Absen Masuk")
        section(root, TYPE_PULANG, "Absen Pulang")
        setContentView(ScrollView(this).apply { addView(root) })
        requestPermissions()
        Scheduler.scheduleAll(this)
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent())
                startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName")))
        }
    }

    private fun update(t: Int, f: (Sched) -> Sched) {
        Prefs.save(this, t, f(Prefs.load(this, t)))
        Scheduler.schedule(this, t)
    }

    private fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)

    private fun titleOf(uri: Uri): String {
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
            }
        } catch (_: Exception) {}
        return try { RingtoneManager.getRingtone(this, uri).getTitle(this) } catch (_: Exception) { uri.lastPathSegment ?: "Dipilih" }
    }

    private fun setRingtone(t: Int, uri: Uri) {
        update(t) { it.copy(ringtone = uri.toString()) }
        labels[t]?.text = "Nada: " + titleOf(uri)
    }

    private fun section(root: LinearLayout, t: Int, title: String) {
        val s = Prefs.load(this, t)
        root.addView(TextView(this).apply {
            text = title; textSize = 20f; setTypeface(null, Typeface.BOLD); setPadding(0, 48, 0, 8)
        })
        root.addView(SwitchCompat(this).apply {
            text = "Aktifkan alarm"; isChecked = s.enabled
            setOnCheckedChangeListener { _, c -> update(t) { it.copy(enabled = c) } }
        })
        root.addView(Button(this).apply {
            text = "Jam: " + fmt(s.hour, s.minute)
            setOnClickListener {
                val cur = Prefs.load(this@MainActivity, t)
                TimePickerDialog(this@MainActivity, { _, h, m ->
                    update(t) { it.copy(hour = h, minute = m) }
                    text = "Jam: " + fmt(h, m)
                }, cur.hour, cur.minute, true).show()
            }
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val names = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
        names.forEachIndexed { idx, n ->
            val day = idx + Calendar.SUNDAY
            row.addView(CheckBox(this).apply {
                text = n; textSize = 11f; isChecked = day in s.days
                setOnCheckedChangeListener { _, c ->
                    update(t) { it.copy(days = if (c) it.days + day else it.days - day) }
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        root.addView(row)
        val lbl = TextView(this).apply {
            text = "Nada: " + (s.ringtone?.let { titleOf(Uri.parse(it)) } ?: "Dering alarm bawaan")
        }
        labels[t] = lbl
        root.addView(lbl)
        root.addView(Button(this).apply {
            text = "Pilih Dering Sistem"
            setOnClickListener {
                pendingType = t
                ringtonePicker.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                    .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL))
            }
        })
        root.addView(Button(this).apply {
            text = "Pilih Lagu dari Penyimpanan"
            setOnClickListener { pendingType = t; songPicker.launch(arrayOf("audio/*")) }
        })
        root.addView(Button(this).apply {
            text = "Tes Alarm Sekarang"
            setOnClickListener {
                ContextCompat.startForegroundService(this@MainActivity,
                    Intent(this@MainActivity, AlarmService::class.java).putExtra("type", t))
            }
        })
    }
}
