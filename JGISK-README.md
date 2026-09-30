# JGisk (fork KernelSU) - target: GKI android15-6.6 (kernel 6.6.x)

## Build (GitHub Actions)
1. Buat repo GitHub **PRIVATE**, upload isi folder ini (termasuk folder `.github`).
2. Tab Actions > "Build JGisk" > Run workflow (~30-45 menit, jalan di cloud, HP tidak terbebani).
3. Unduh artifact `JGisk-manager-apk` (APK).

## Pasang (mode LKM, tanpa build kernel penuh)
1. Backup `init_boot`/`boot` stok. Bootloader harus sudah unlock.
2. Install APK JGisk, buka, pilih Install > "Patch init_boot/boot" (LKM) dan flash hasilnya via fastboot.
   Kernel harus tepat KMI android15-6.6 (punyamu: 6.6.118-android15-8).

## Catatan
- Package: com.jg.jgisk. Signature di-lock ke `jgisk.jks` (kunci di kernel Kbuild).
- Ganti password keystore: buat jks baru, lalu update KSU_EXPECTED_SIZE/HASH di kernel/Kbuild.
- Lisensi GPLv2: hasil fork wajib tetap open source.
