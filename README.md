# MemGallery – demo tối ưu bộ nhớ trên Android

Một project, hai bản cài song song trên cùng điện thoại:

| Bản | applicationId | Tên app | Icon |
|---|---|---|---|
| unoptimized | `com.example.memgallery.unopt` | Gallery UNOPT | đỏ |
| optimized | `com.example.memgallery.opt` | Gallery OPT | xanh |

Giao diện và dữ liệu (40 ảnh 4000×3000 trong `assets/photos`) giống hệt nhau; chỉ khác 3 file trong `impl/`.

## Build & cài lên điện thoại

Bật USB debugging, cắm cáp, rồi (ở thư mục `MemGallery`):

```powershell
.\gradlew installUnoptimizedDebug installOptimizedDebug
```
Hoặc trong Android Studio: **Build Variants** → chọn `unoptimizedDebug` / `optimizedDebug` → Run.

Đo bản release (tuỳ chọn, gần thực tế hơn): `.\gradlew installUnoptimizedRelease installOptimizedRelease`.

## Chỗ khác nhau giữa 2 bản (để chụp vào báo cáo)

| Kỹ thuật | `unoptimized/.../impl/` | `optimized/.../impl/` |
|---|---|---|
| 1. Bitmap downsampling | `ImageLoader.kt` – decode nguyên 4000×3000 | `ImageLoader.kt` – `inJustDecodeBounds` + `calculateInSampleSize` |
| 2. LruCache | không có cache | `ImageLoader.kt` – `LruCache` (1/8 `memoryClass`) |
| 3. Chống leak | `DetailHelper.kt` – static list + Handler | `DetailHelper.kt` – WeakReference + `lifecycleScope` |
| 4. Tái sử dụng view | `GalleryHost.kt` – ScrollView, 40 view | `GalleryHost.kt` – RecyclerView + ViewHolder |

Tìm trong code: `❌` (vấn đề) và `✅` (tối ưu).

## Khung số liệu trên màn hình

```
Java 12/256MB (peak 20) | Native 30MB (peak 55) | PSS 120MB (peak 150)
Decode N | Hit H Miss M (%) | avg X ms
Views V | DetailAlive A | OOM O | GC g | Jank j% (janky/total) | Startup ms
```
- **Native**: từ Android 8 pixel của Bitmap nằm ở native heap, không phải Java heap.
- **DetailAlive**: số `DetailActivity` chưa bị GC (đo bằng WeakReference). **Bấm nút GC trước khi đọc.**
- **OOM**: số lần bắt được `OutOfMemoryError` khi decode (app không chết để còn đo tiếp).
- **Jank**: % frame vẽ lâu hơn ngân sách 1 frame (FrameMetrics).
- **Startup**: từ `onCreate` đến frame đầu tiên của MainActivity.
- **Peak**: đỉnh từ lần bấm **Reset** gần nhất.

Nút: **Auto-scroll** (cuộn 3 vòng, tốc độ cố định 2500 px/s, hiện thời gian xong), **Reset** (xóa bộ đếm + GC), **GC**.

Log CSV: `adb logcat -s MEMGALLERY` (cột: `time,javaMB,nativeMB,pssMB,decode,hit,miss,avgMs,views,aliveDetail,oom,gc`).

## Kịch bản đo (Phase 6 – bạn tự thực hiện)

Chuẩn bị: đóng app nền, pin > 50%, ghi lại tên máy / RAM / bản Android. Mỗi kịch bản chạy **3 lần**, lấy trung bình. Trước mỗi lần: force-stop app, mở lại.

| # | Kịch bản | Cách làm |
|---|---|---|
| S1 | Mở app | Mở app, đợi 5 giây, đọc Startup, Java/Native/PSS |
| S2 | Cuộn | Bấm **Reset** → **Auto-scroll** → khi xong đọc Peak, Decode, Hit rate, avg ms, GC, Jank |
| S3 | Leak | Mở ảnh → Back, lặp 10 lần → bấm **GC** → đọc DetailAlive; xem thông báo LeakCanary |
| S4 | Xoay màn hình | Trong ảnh, xoay 10 lần → Back → **GC** → đọc DetailAlive |
| S5 | Heap dump | Sau S2: Android Studio Profiler → Memory → Capture heap dump → đếm `Bitmap` |
| S6 | Hệ thống | xem lệnh bên dưới |
| S7 | Độ mượt | xem lệnh bên dưới (tuỳ chọn) |

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# S6: thống kê bộ nhớ hệ thống (chạy sau khi đã thao tác xong trong app)
& $adb shell dumpsys meminfo com.example.memgallery.unopt
& $adb shell dumpsys meminfo com.example.memgallery.opt

# S7: jank do hệ thống đo (reset trước, cuộn, rồi đọc)
& $adb shell dumpsys gfxinfo com.example.memgallery.unopt reset
# ... bấm Auto-scroll ...
& $adb shell dumpsys gfxinfo com.example.memgallery.unopt

# Mở/đóng nhanh app để đo lại từ đầu
& $adb shell am force-stop com.example.memgallery.unopt
& $adb shell monkey -p com.example.memgallery.unopt 1
```

## Nếu bản unoptimized chết ngay khi mở

40 ảnh × ~46 MB ≈ 1.8 GB. Nếu máy kill app trước khi đo được:
đổi `PHOTO_COUNT` trong `app/build.gradle.kts` (vd `"20"`), **dùng cùng một giá trị cho cả 2 bản** và ghi lại trong báo cáo. Chụp Logcat lúc crash làm bằng chứng.

## Cấu hình

Kotlin · Views (XML, không Compose) · AGP 9.2.1 · Gradle 9.4.1 · minSdk 26 · LeakCanary (chỉ debug) · không dùng thư viện load ảnh (Glide/Coil) để tự cài đặt được các kỹ thuật.
