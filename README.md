# Goodeva BLE Scanner & Tracker

Aplikasi Android (Kotlin + Jetpack Compose) untuk memindai perangkat Bluetooth Low Energy (BLE) di sekitar, melacak satu perangkat lewat visual radar berdasarkan RSSI, dan menyimpan riwayat perangkat yang pernah terdeteksi.

## Fitur

- **Scanner:** tombol Start/Stop manual, daftar perangkat real-time (nama, MAC address, RSSI mentah, kategori sinyal, estimasi jarak), urut otomatis dari sinyal terkuat, pencarian berdasarkan nama atau MAC, dan filter RSSI minimum. Perangkat yang tidak mengirim sinyal lebih dari 10 detik ditandai redup dengan keterangan "Tidak terlihat".
- **Radar:** pelacakan satu perangkat dengan RSSI yang dihaluskan, visual radar yang berubah warna sesuai kategori sinyal, deteksi "Sinyal hilang", dan informasi stabilitas sinyal.
- **History:** riwayat perangkat yang tersimpan di database lokal (Room) beserta waktu terakhir terdeteksi, tetap ada setelah aplikasi ditutup.

## Persyaratan

- Android Studio versi stabil terbaru (project memakai Android Gradle Plugin 9.3.3)
- HP Android **12 (API 31) atau lebih baru** dengan Bluetooth LE
- **Perangkat fisik.** Emulator Android tidak bisa memindai BLE.

## Setup dan cara menjalankan

1. Clone repository:
```bash
   git clone https://github.com/Abrrbam/ble-scanner.git
```
2. Buka folder project di Android Studio, lalu tunggu Gradle sync selesai.
3. Aktifkan **Developer options** dan **USB debugging** di HP, lalu sambungkan lewat USB.
4. Pilih HP di daftar device, lalu tekan **Run**.
5. Saat pertama dibuka, berikan izin **Perangkat di sekitar (Nearby devices)** dan pastikan Bluetooth menyala.

**Build APK:** Build → Generate App Bundles or APKs → Generate APKs. Hasilnya ada di `app/build/outputs/apk/debug/app-debug.apk`. APK siap pakai tersedia di halaman Releases repository ini. APK tersebut adalah build debug yang ditandatangani dengan debug key bawaan Android Studio, cukup untuk pengujian.

## Arsitektur

Pola **MVVM** dengan tiga lapisan dan **Dependency Injection (Hilt)**.

```
ui/        Composable + ViewModel (StateFlow)         ← apa yang dilihat user
domain/    Model, interface (BleScanner, DeviceRepository), logika murni (RssiTracker)
data/      Implementasi: BLE (AndroidBleScanner), Room (DAO, Entity), repository
di/        Modul Hilt yang menghubungkan interface ke implementasi
```

Alur data:

- `AndroidBleScanner` membungkus callback `BluetoothLeScanner` menjadi `Flow` (`callbackFlow`). Scan mulai saat Flow di-collect dan berhenti saat dibatalkan.
- `ScannerViewModel` mengumpulkan hasil scan, menggabungkannya dengan filter, mengurutkan, lalu mengirim `ScannerUiState` ke UI. Perangkat juga disimpan ke Room lewat `DeviceRepository`.
- `RadarViewModel` memfilter hasil scan untuk satu alamat, menghaluskan RSSI dengan `RssiTracker`, dan menghasilkan `RadarUiState`.
- `HistoryViewModel` mengamati Room lewat `Flow`, jadi layar History otomatis update.

ViewModel hanya bergantung pada interface di `domain/`, sehingga implementasi BLE atau database dapat diganti tanpa mengubah UI dan logika mudah dites.

## Library dan alasan

| Teknologi | Alasan |
|---|---|
| Kotlin + Jetpack Compose | Bahasa dan UI toolkit resmi Android modern, kode lebih ringkas daripada XML |
| ViewModel + StateFlow | Standar MVVM, state bertahan saat rotasi layar |
| Hilt (2.60.1) + KSP | DI resmi Google, boilerplate minim, kompatibel dengan AGP 9 |
| Room (2.8.5) | Database lokal resmi, mendukung Flow dan coroutine. Dipilih versi 2.x yang stabil dan matang, bukan Room 3 |
| Navigation Compose | Navigasi antar layar, argumen route untuk layar Radar |
| Coroutines + Flow | Menangani aliran data BLE yang asinkron dan berkelanjutan |
| BLE API bawaan Android | Tanpa library BLE pihak ketiga: lebih ringan dan kontrol penuh atas siklus hidup scan |

## Keputusan teknis dan asumsi

- **minSdk 31.** Cukup satu izin runtime, `BLUETOOTH_SCAN`, dengan flag `neverForLocation` sehingga tidak perlu izin lokasi dan tidak ada cabang kode untuk Android lama.
- **Nama perangkat** diambil dari `scanRecord.deviceName` agar tidak membutuhkan izin `BLUETOOTH_CONNECT`.
- **Nyalakan Bluetooth** dengan mengarahkan user ke pengaturan Bluetooth, bukan dialog `ACTION_REQUEST_ENABLE` (yang juga butuh `BLUETOOTH_CONNECT`).
- **Estimasi jarak** memakai tabel kategori dari spesifikasi tugas (batas dibaca "≥"), bukan rumus path-loss, karena tabel itu yang diminta dan rumus butuh kalibrasi tiap perangkat.
- **Daftar Scanner** diperbarui maksimal 1x per detik agar urutan tidak melompat dan item bisa ditekan dengan nyaman. Search dan filter bereaksi langsung.
- **Smoothing RSSI di Radar:** exponential moving average (α = 0,25). Nilai mentah tetap ditampilkan.
- **Sinyal hilang:** RSSI halus < -90 dBm atau tidak ada paket selama 8 detik.
- **Stabilitas:** simpangan baku RSSI dalam 10 detik terakhir (< 3 dB stabil, < 6 dB cukup stabil, lainnya tidak stabil).
- **Riwayat otomatis:** perangkat disimpan ke Room tiap 5 detik selama scan dan sekali saat scan berhenti. Satu baris per MAC address, nama lama tidak tertimpa nilai kosong.

## Error handling dan lifecycle

- **Izin ditolak:** layar menjelaskan kebutuhan izin dan menyediakan tombol minta izin. Jika ditolak permanen, tombol berubah menjadi pintasan ke pengaturan aplikasi. Status izin dicek ulang setiap aplikasi kembali ke foreground.
- **Bluetooth mati:** dipantau lewat `BroadcastReceiver` yang dibungkus Flow. Scan dihentikan otomatis, layar menampilkan petunjuk. Di layar Radar, pelacakan lanjut otomatis begitu Bluetooth menyala lagi.
- **Bluetooth tidak tersedia / scan gagal:** pesan error ditampilkan tanpa crash.
- **Throttling scan:** Android membatasi 5 kali start scan per 30 detik dan membatasinya secara diam-diam. Pembatas dibuat di `ScannerViewModel` dan menampilkan pesan berapa detik harus menunggu.
- **Background dan pindah layar:** scan di Scanner dihentikan saat layar tidak terlihat (`ON_STOP`), baik karena aplikasi ke background maupun karena pengguna membuka layar lain (misalnya Radar), kecuali pada perubahan konfigurasi. Setelah kembali, pengguna menekan Start lagi. Scan di Radar berhenti sekitar 5 detik setelah layar tidak terlihat.
- **Rotasi layar:** scan dipegang ViewModel sehingga tidak putus. `StateFlow` memakai `WhileSubscribed(5000)`.
- **Kegagalan penyimpanan** riwayat ditangkap supaya tidak menghentikan scan.

## Testing

Unit test (JUnit) untuk logika murni:

- `SignalCategoryTest`: batas-batas kategori sesuai tabel
- `RssiTrackerTest`: smoothing, jendela waktu, simpangan baku
- `DeviceFilterTest`: pengurutan, pencarian nama/MAC, filter RSSI

```bash
./gradlew testDebugUnitTest
```

**Uji manual di perangkat fisik** (Xiaomi 2201117TY, Android 13):

- Diuji: permintaan dan penolakan izin (termasuk penolakan permanen), Bluetooth dimatikan saat scan, rotasi layar, aplikasi ke background, dan riwayat tetap ada setelah aplikasi ditutup.
- Belum diuji pada rilis ini: pencabutan izin lewat pengaturan saat aplikasi berjalan, dan Bluetooth dimatikan saat berada di layar Radar.

**Hasil uji dengan perangkat nyata (earbuds TWS):** layar Radar menerima paket dari perangkat target dan menampilkan kategori serta warna sesuai nilai sinyal. RSSI mentah berfluktuasi sekitar ±7 dBm (contoh pengamatan: -69 sampai -82 dBm), sedangkan RSSI halus (EMA) bergerak lebih stabil.

## Known issues

- **MAC address acak.** Banyak perangkat memakai alamat BLE acak yang berganti berkala (dan bisa berbeda dari alamat Bluetooth Classic yang terlihat di pengaturan perangkat), sehingga perangkat yang sama dapat tercatat sebagai beberapa entri di riwayat.
- **Hanya perangkat yang sedang beriklan (advertising).** Perangkat yang sudah tersambung ke perangkat lain atau yang hanya mendukung Bluetooth Classic bisa tidak terdeteksi. Pada pengujian dengan earbuds TWS, alamat BLE yang muncul berbeda dari alamat Bluetooth Classic di pengaturan perangkat, dan beberapa alamat baru muncul setiap kali case dibuka. Perangkat yang berhenti beriklan ditandai redup di daftar Scanner, dan memilihnya di Radar akan berakhir pada status "Sinyal hilang".
- **Jarak hanya estimasi.** RSSI dipengaruhi hambatan (dinding, tubuh), orientasi antena, dan berbeda antar perangkat. Arah perangkat tidak dapat diketahui dari BLE, jadi posisi titik di radar hanya menunjukkan jarak.
- **Perangkat yang jarang beriklan** (lebih dari 8 detik antar paket) dapat dianggap hilang oleh Radar. Batasnya dapat diubah lewat konstanta `LOST_TIMEOUT_MS`.
- **Flag `neverForLocation`** dapat membuat sistem menyaring sebagian perangkat tertentu, misalnya beacon.
- **Pembatas scan** hanya menghitung start dari layar Scanner. Scan di layar Radar tidak ikut dihitung, padahal Android menghitung keduanya.
- **Scan Scanner berhenti saat membuka Radar** sehingga Start perlu ditekan lagi setelah kembali. Ini disengaja untuk menghemat baterai dan menghindari throttling.
- **Database versi 1** tanpa skrip migrasi.
- Hanya diuji di satu perangkat (Android 13). Tampilan pada ukuran layar sangat kecil atau tablet belum dioptimalkan.

## Kendala selama pengerjaan

- **AGP 9 memakai Kotlin bawaan**, sehingga plugin `kotlin-android` tidak dipakai dan `kapt` tidak kompatibel. Hilt dan Room memakai **KSP**.
- **Import `hiltViewModel`** pindah ke `androidx.hilt.lifecycle.viewmodel.compose` pada androidx.hilt 1.4.0.
- **Rotasi layar memicu `ON_STOP`**, yang sempat menghentikan scan. Diperbaiki dengan memeriksa `isChangingConfigurations`.
- **Throttling scan Android tidak memberi error**, jadi pembatas dibuat sendiri.
- **Daftar yang melompat** saat diurutkan tiap paket masuk, sehingga item sulit ditekan. Diatasi dengan menerbitkan daftar maksimal 1x per detik.
- **RSSI berisik**, diatasi dengan smoothing EMA di Radar.
- **Radar tidak pernah menemukan target** pada pengujian pertama dengan perangkat nyata. Penyebabnya spasi tersembunyi di awal argumen route sehingga alamat target tidak pernah sama dengan alamat dari hasil scan. Log menunjukkan paket target sebenarnya diterima, tetapi dibuang oleh filter. Diperbaiki dengan menghapus spasi di sumbernya dan menambah `trim()` serta perbandingan tanpa peka huruf besar/kecil sebagai pengaman.