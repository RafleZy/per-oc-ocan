# OC Editor (MT6789 & GPU) - Android Capacitor & Native Build

Aplikasi editor overclocking untuk prosesor MediaTek Helio G99 (MT6789) dan GPU (Mali-G57 MC2), yang mendukung penyesuaian tabel OPP CPU (A55, D2, A76), frekuensi & voltase bus, OPP GPU (`.ko`), serta pembuatan file zip flashable (AnyKernel/TWRP).

Repository ini telah dikonfigurasi lengkap agar dapat di-build menjadi **APK Android** menggunakan **Capacitor** dengan alur **GitHub Actions Workflow**, serta dapat dijalankan langsung di lingkungan Android.

---

## 📁 Struktur Repository GitHub

```text
.
├── .github/
│   └── workflows/
│       └── build-apk.yml        # CI/CD otomatis untuk build APK di GitHub Actions
├── index.html                   # File HTML editor utama dengan patch WebView/Capacitor
├── www/
│   └── index.html               # Folder bundle web yang disinkronkan ke Android
├── package.json                 # Konfigurasi npm & dependensi Capacitor (Filesystem, Share)
├── capacitor.config.json        # Pengaturan Capacitor (appId: com.akumau.oc, appName: "OC Editor")
├── app/                         # Project source Android & assets
├── settings.gradle.kts          # Konfigurasi Gradle root
├── build.gradle.kts             # Konfigurasi build plugin AGP
└── README.md                    # Dokumentasi lengkap
```

---

## 🚀 Komponen yang Disediakan

### 1. `package.json` & `capacitor.config.json`
- **appId**: `com.akumau.oc`
- **appName**: `OC Editor`
- **webDir**: `www`
- **Plugin Capacitor**:
  - `@capacitor/core` & `@capacitor/android`
  - `@capacitor/filesystem`: Menyimpan file hasil modifikasi (`.zip`, `.img`, `.ko`) ke penyimpanan perangkat.
  - `@capacitor/share`: Menampilkan menu bagikan / simpan langsung bawaan sistem Android.

### 2. GitHub Actions Workflow (`.github/workflows/build-apk.yml`)
Workflow ini otomatis berjalan setiap kali ada `push`, `pull_request`, atau dijalankan manual (`workflow_dispatch`):
1. Melakukan checkout kode sumber dari repo.
2. Menginstall Node.js 20 dan Java JDK 17 (Temurin).
3. Menyiapkan Android SDK.
4. Memindahkan `index.html` ke folder `www/index.html`.
5. Menjalankan `npm install`.
6. Menjalankan `npx cap add android` dan `npx cap sync android`.
7. Melakukan build release/debug APK menggunakan `./gradlew assembleDebug`.
8. Mengunggah file APK yang dihasilkan sebagai **Artifact** di tab GitHub Actions.

### 3. Patch Tombol Download pada `index.html`
Secara default, Android WebView sering memblokir atau mengabaikan tautan `<a download="..." href="blob:...">`. Kami telah menyematkan patch script khusus di `index.html`:
- Mendeteksi apakah aplikasi berjalan di **Capacitor**: Mengubah `Blob` menjadi Base64, menulis file ke `Filesystem`, lalu membuka dialog penyimpanan/berbagi via `Share`.
- Mendeteksi apakah aplikasi berjalan di **Android WebView Bridge** (`AndroidBridge`): Menyimpan ke cache aplikasi dan membuka system share sheet via FileProvider.
- Fallback browser biasa: Tetap menggunakan Object URL standar jika dibuka di browser desktop / Chrome Android.
- Mendukung pemilihan file kernel (`.ko`), image (`.img`), dan zip via file chooser native Android.

---

## 🛠️ Langkah-Langkah Menaruh dan Menjalankan di GitHub

### Langkah 1: Push ke GitHub
Jika belum memiliki repo GitHub:
1. Buat repository baru di GitHub (misal: `oc-editor`).
2. Di komputer lokal Anda, clone atau inisialisasi git:
   ```bash
   git init
   git add .
   git commit -m "feat: inisialisasi struktur repo Capacitor dan GitHub Actions build APK"
   git branch -M main
   git remote add origin https://github.com/<USERNAME>/<NAMA-REPO>.git
   git push -u origin main
   ```

### Langkah 2: Mengunduh APK dari GitHub Actions
1. Buka repository Anda di browser.
2. Klik tab **Actions** di bagian atas repository.
3. Anda akan melihat workflow **"Build Android APK"** sedang berjalan.
4. Tunggu hingga workflow selesai (centang hijau).
5. Klik nama run tersebut, lalu gulir ke bawah ke bagian **Artifacts**.
6. Klik **`OC-Editor-debug-apk`** untuk mengunduh file `.zip` yang berisi `app-debug.apk`.
7. Ekstrak dan instal file APK ke perangkat Android Anda.

---

## 💻 Cara Menjalankan & Test Lokal di PC (Opsional)

Jika ingin build APK secara manual di komputer Anda:
1. Pastikan telah menginstal **Node.js (>= 18)** dan **Android Studio**.
2. Di folder repo, jalankan:
   ```bash
   # Siapkan folder www
   mkdir -p www
   cp index.html www/index.html

   # Install dependensi
   npm install

   # Tambahkan platform Android dan sinkronisasi
   npx cap add android
   npx cap sync android

   # Buka di Android Studio
   npx cap open android
   ```
3. Klik tombol **Run** atau **Build APK** di Android Studio.
