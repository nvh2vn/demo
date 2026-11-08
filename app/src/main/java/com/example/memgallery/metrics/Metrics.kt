package com.example.memgallery.metrics

import android.app.Activity
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Bộ đếm số liệu dùng chung cho 2 bản. Cả hai bản đều gọi cùng các hàm này,
 * nên số liệu so sánh được với nhau.
 */
object Metrics {
    val decodeCount = AtomicLong()
    val cacheHit = AtomicLong()
    val cacheMiss = AtomicLong()
    val totalDecodeNanos = AtomicLong()
    val viewsCreated = AtomicLong()
    val oomCount = AtomicLong()

    // Số frame và số frame "giật" (vượt ngân sách thời gian 1 frame)
    val frames = AtomicLong()
    val jankyFrames = AtomicLong()

    // Thời gian từ onCreate đến khi gallery vẽ xong frame đầu tiên (ms)
    @Volatile var startupMs: Long = -1

    // Đỉnh bộ nhớ trong 1 "cửa sổ đo" (reset bằng nút Reset)
    @Volatile var peakJavaMb = 0L
    @Volatile var peakNativeMb = 0L
    @Volatile var peakPssMb = 0L

    fun recordDecode(nanos: Long) {
        decodeCount.incrementAndGet()
        totalDecodeNanos.addAndGet(nanos)
    }

    fun avgDecodeMs(): Double {
        val n = decodeCount.get()
        return if (n == 0L) 0.0 else totalDecodeNanos.get() / 1e6 / n
    }

    fun cacheHitRatePercent(): Double {
        val total = cacheHit.get() + cacheMiss.get()
        return if (total == 0L) 0.0 else cacheHit.get() * 100.0 / total
    }

    fun jankPercent(): Double {
        val f = frames.get()
        return if (f == 0L) 0.0 else jankyFrames.get() * 100.0 / f
    }

    fun reset() {
        decodeCount.set(0); cacheHit.set(0); cacheMiss.set(0)
        totalDecodeNanos.set(0); viewsCreated.set(0); oomCount.set(0)
        frames.set(0); jankyFrames.set(0)
        peakJavaMb = 0; peakNativeMb = 0; peakPssMb = 0
        // Không reset startupMs và danh sách DetailActivity (đó là trạng thái, không phải bộ đếm)
    }

    // ---- ĐO LEAK: theo dõi DetailActivity bằng WeakReference ----
    // (Đây là code ĐO, KHÔNG gây leak: WeakReference không giữ Activity sống.)
    private val detailRefs = CopyOnWriteArrayList<WeakReference<Activity>>()

    fun trackDetail(activity: Activity) {
        detailRefs.add(WeakReference(activity))
    }

    /** Số DetailActivity chưa bị GC. Bấm nút GC trước khi đọc để kết quả chính xác. */
    fun aliveDetail(): Int {
        detailRefs.removeAll { it.get() == null }
        return detailRefs.size
    }
}
