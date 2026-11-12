CARA BUILD APK
1. Pasang Android Studio (Koala atau lebih baru).
2. File > Open > pilih folder AbsenReminder. Tunggu Gradle sync.
3. Build > Build Bundle(s)/APK(s) > Build APK(s).
4. APK ada di app/build/outputs/apk/debug/app-debug.apk, kirim ke HP lalu install.
Atau lewat terminal: ./gradlew assembleDebug (jalankan "gradle wrapper" dulu bila belum ada gradlew).

SETELAH INSTALL
- Buka aplikasi, izinkan Notifikasi dan "Notifikasi layar penuh".
- Atur jam, hari, dan nada untuk Absen Masuk dan Absen Pulang, lalu aktifkan.
- Matikan optimasi baterai untuk aplikasi ini (Pengaturan > Aplikasi > Baterai > Tanpa batasan).
- Tekan "Tes Alarm Sekarang" untuk mencoba.
