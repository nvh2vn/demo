package com.example.memgallery.metrics

import android.app.Activity
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import android.widget.TextView
import java.util.Locale

/**
 * Khung số liệu nổi trên màn hình + ghi Logcat (tag MEMGALLERY, dạng CSV).
 *
 * - Cập nhật khung mỗi 500 ms, ghi Logcat mỗi 1 giây.
 * - Đo jank bằng FrameMetrics (frame nào vẽ lâu hơn ngân sách 1 frame là "janky").
 * - PHẢI gọi stop() trong onDestroy để không giữ Activity.
 *
 * Lọc Logcat:  adb logcat -s MEMGALLERY
 * Cột CSV:     time,javaMB,nativeMB,pssMB,decode,hit,miss,avgMs,views,aliveDetail,oom,gc
 */
class MetricsOverlay(
    private val activity: Activity,
    private val textView: TextView,
) {
    private val ui = Handler(Looper.getMainLooper())
    private var frameThread: HandlerThread? = null
    private var tick = 0
    private val startTime = System.currentTimeMillis()

    // Ngân sách thời gian cho 1 frame (ms), tính theo tần số quét thực của màn hình
    @Suppress("DEPRECATION")
    private val frameBudgetNs: Long =
        (1_000_000_000.0 / activity.windowManager.defaultDisplay.refreshRate.coerceAtLeast(30f)).toLong()

    private val frameListener = Window.OnFrameMetricsAvailableListener { _, fm, _ ->
        // Bỏ qua frame vẽ đầu tiên (đã có chỉ số startup riêng)
        if (fm.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1L) return@OnFrameMetricsAvailableListener
        Metrics.frames.incrementAndGet()
        // Cho phép sai số 10% để không đếm nhầm frame sát ngưỡng
        if (fm.getMetric(FrameMetrics.TOTAL_DURATION) > frameBudgetNs * 1.1) {
            Metrics.jankyFrames.incrementAndGet()
        }
    }

    private val sampler = object : Runnable {
        override fun run() {
            sample()
            tick++
            ui.postDelayed(this, 500)
        }
    }

    fun start() {
        val t = HandlerThread("frame-metrics").also { it.start() }
        frameThread = t
        activity.window.addOnFrameMetricsAvailableListener(frameListener, Handler(t.looper))
        if (activity is com.example.memgallery.MainActivity) {
            Log.i(TAG, "time,javaMB,nativeMB,pssMB,decode,hit,miss,avgMs,views,aliveDetail,oom,gc")
        }
        ui.post(sampler)
    }

    fun stop() {
        ui.removeCallbacks(sampler)
        runCatching { activity.window.removeOnFrameMetricsAvailableListener(frameListener) }
        frameThread?.quitSafely()
        frameThread = null
    }

    private fun sample() {
        val java = MemoryStats.javaHeapUsedMb()
        val native = MemoryStats.nativeHeapMb()
        val pss = MemoryStats.totalPssMb()
        if (java > Metrics.peakJavaMb) Metrics.peakJavaMb = java
        if (native > Metrics.peakNativeMb) Metrics.peakNativeMb = native
        if (pss > Metrics.peakPssMb) Metrics.peakPssMb = pss

        val decode = Metrics.decodeCount.get()
        val hit = Metrics.cacheHit.get()
        val miss = Metrics.cacheMiss.get()
        val avg = Metrics.avgDecodeMs()
        val views = Metrics.viewsCreated.get()
        val alive = Metrics.aliveDetail()
        val oom = Metrics.oomCount.get()
        val gc = MemoryStats.gcCount()

        val loc = Locale.US
        textView.text = buildString {
            appendLine(String.format(loc, "Java %d/%dMB (peak %d) | Native %dMB (peak %d) | PSS %dMB (peak %d)",
                java, MemoryStats.javaHeapMaxMb(), Metrics.peakJavaMb, native, Metrics.peakNativeMb, pss, Metrics.peakPssMb))
            appendLine(String.format(loc, "Decode %d | Hit %d Miss %d (%.0f%%) | avg %.1f ms",
                decode, hit, miss, Metrics.cacheHitRatePercent(), avg))
            append(String.format(loc, "Views %d | DetailAlive %d | OOM %d | GC %d | Jank %.1f%% (%d/%d) | Startup %s",
                views, alive, oom, gc, Metrics.jankPercent(), Metrics.jankyFrames.get(), Metrics.frames.get(),
                if (Metrics.startupMs >= 0) "${Metrics.startupMs}ms" else "-"))
        }

        // Ghi Logcat mỗi 1 giây (mỗi 2 lần lấy mẫu)
        if (tick % 2 == 0) {
            Log.i(TAG, String.format(loc, "%d,%d,%d,%d,%d,%d,%d,%.1f,%d,%d,%d,%d",
                (System.currentTimeMillis() - startTime) / 1000, java, native, pss,
                decode, hit, miss, avg, views, alive, oom, gc))
        }
    }

    companion object {
        const val TAG = "MEMGALLERY"
    }
}
