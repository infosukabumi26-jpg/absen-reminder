package com.absen.reminder

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {
    private var type = TYPE_MASUK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)

        // Tombol Back dinonaktifkan: hanya bisa keluar lewat tombol konfirmasi
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* diabaikan */ }
        })
        build(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); build(intent) }

    private fun build(i: Intent) {
        type = i.getIntExtra("type", TYPE_MASUK)
        val judul = if (type == TYPE_MASUK) "WAKTUNYA\nABSEN MASUK!" else "WAKTUNYA\nABSEN PULANG!"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(if (type == TYPE_MASUK) Color.parseColor("#B71C1C") else Color.parseColor("#0D47A1"))
            setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = judul; textSize = 36f; setTextColor(Color.WHITE)
            gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "\nJangan lupa absen! Tekan tombol di bawah untuk membuka halaman absen."
            textSize = 18f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "ABSEN SEKARANG"; textSize = 22f
            setOnClickListener { konfirmasi() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 220).apply { topMargin = 96 })
        setContentView(root)
    }

    private fun konfirmasi() {
        startService(Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_STOP))
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ABSEN_URL)))
        finish()
    }

    // Jika pengguna menekan Home saat alarm masih berbunyi, tampilkan lagi
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AlarmService.active) {
            try {
                startActivity(Intent(this, AlarmActivity::class.java).putExtra("type", type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
            } catch (_: Exception) {}
        }
    }
}
