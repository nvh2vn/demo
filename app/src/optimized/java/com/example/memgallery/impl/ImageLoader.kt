package com.example.memgallery.impl

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.LruCache
import android.widget.ImageView
import com.example.memgallery.R
import com.example.memgallery.data.PhotoRepository
import com.example.memgallery.metrics.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ✅ BẢN ĐÃ TỐI ƯU — áp dụng 3 kỹ thuật trong file này:
 *
 *  Kỹ thuật 1 (Bitmap downsampling): chỉ decode ở kích thước cần hiển thị (inSampleSize).
 *  Kỹ thuật 2 (LruCache): giữ lại ảnh đã decode, cuộn lại không decode lại.
 *  Kỹ thuật 4 (một phần): decode ở luồng nền (IO), chỉ gán ảnh trên main thread,
 *                         và HỦY việc tải khi view bị tái sử dụng.
 *
 * Chữ ký hàm giống hệt bản unoptimized.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object ImageLoader {

    /** Đặt true để thử RGB_565 cho thumbnail (2 byte/pixel thay vì 4) và so sánh số đo. */
    private const val THUMB_USE_RGB565 = false

    private val placeholder = ColorDrawable(Color.LTGRAY)

    // Scope sống suốt tiến trình; mỗi ImageView giữ Job riêng để hủy được.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Giới hạn số luồng decode song song để không làm đỉnh bộ nhớ tăng đột biến.
    private val decodeDispatcher = Dispatchers.IO.limitedParallelism(3)

    // ✅ KỸ THUẬT 2: LruCache — dung lượng = 1/8 bộ nhớ app được cấp (memoryClass).
    @Volatile private var cache: LruCache<String, Bitmap>? = null

    private fun cache(context: Context): LruCache<String, Bitmap> =
        cache ?: synchronized(this) {
            cache ?: run {
                val memoryClassMb = (context.applicationContext
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
                val cacheBytes = memoryClassMb * 1024 * 1024 / 8
                object : LruCache<String, Bitmap>(cacheBytes) {
                    // Đo theo BYTE thật của bitmap, không phải theo số phần tử
                    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
                }.also { cache = it }
            }
        }

    fun load(context: Context, name: String, target: ImageView, reqW: Int, reqH: Int, cover: Boolean) {
        cancel(target) // view có thể đang tải ảnh khác (do được tái sử dụng)

        val key = "$name@${reqW}x$reqH@$cover"
        val c = cache(context)

        c.get(key)?.let {
            Metrics.cacheHit.incrementAndGet()
            target.setImageBitmap(it)
            return
        }
        Metrics.cacheMiss.incrementAndGet()
        target.setImageDrawable(placeholder)

        val appContext = context.applicationContext // KHÔNG giữ Activity trong coroutine
        val job = scope.launch {
            val bitmap = try {
                withContext(decodeDispatcher) {
                    val t0 = System.nanoTime()
                    val b = decodeSampled(appContext, name, reqW, reqH, cover)
                    Metrics.recordDecode(System.nanoTime() - t0)
                    b
                }
            } catch (e: OutOfMemoryError) {
                Metrics.oomCount.incrementAndGet()
                c.evictAll() // giải phóng cache rồi bỏ qua ảnh này
                null
            }
            if (bitmap != null) {
                c.put(key, bitmap)
                target.setImageBitmap(bitmap)
            }
        }
        target.setTag(R.id.loader_job, job)
    }

    /** Hủy việc tải đang chạy của view này (gọi khi view được tái sử dụng / Activity đóng). */
    fun cancel(target: ImageView) {
        (target.getTag(R.id.loader_job) as? Job)?.cancel()
        target.setTag(R.id.loader_job, null)
    }

    // ------------------------------------------------------------------
    // ✅ KỸ THUẬT 1: Bitmap downsampling
    // Theo hướng dẫn chính thức "Loading Large Bitmaps Efficiently" của Android.
    // ------------------------------------------------------------------

    private fun decodeSampled(context: Context, name: String, reqW: Int, reqH: Int, cover: Boolean): Bitmap? {
        // Bước 1: chỉ đọc KÍCH THƯỚC (inJustDecodeBounds = true => không cấp phát pixel)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        PhotoRepository.open(context, name).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Bước 2: tính inSampleSize (lũy thừa của 2) rồi decode thật
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
            inPreferredConfig =
                if (cover && THUMB_USE_RGB565) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
        }
        val decoded = PhotoRepository.open(context, name).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null

        // Bước 3: inSampleSize chỉ giảm theo bậc 2^n nên ảnh còn có thể lớn hơn cần thiết
        // => thu về đúng kích thước đích để cache chứa được nhiều ảnh hơn.
        val scale = if (cover) max(reqW.toFloat() / decoded.width, reqH.toFloat() / decoded.height)
                    else min(reqW.toFloat() / decoded.width, reqH.toFloat() / decoded.height)
        if (scale >= 0.67f) return decoded // sát kích thước đích rồi, không cần thu thêm
        val w = max(1, (decoded.width * scale).roundToInt())
        val h = max(1, (decoded.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(decoded, w, h, true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /**
     * inSampleSize = N nghĩa là decode 1 pixel cho mỗi N x N pixel gốc
     * => bộ nhớ giảm N² lần. Luôn là lũy thừa của 2 vì decoder xử lý hiệu quả nhất ở các bậc đó.
     */
    private fun calculateInSampleSize(srcW: Int, srcH: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (srcH > reqH || srcW > reqW) {
            val halfH = srcH / 2
            val halfW = srcW / 2
            // Tăng gấp đôi chừng nào ảnh sau khi giảm vẫn >= kích thước yêu cầu
            while (halfH / inSampleSize >= reqH && halfW / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
