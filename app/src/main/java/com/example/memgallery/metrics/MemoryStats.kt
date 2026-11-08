package com.example.memgallery.metrics

import android.os.Debug

/** Đọc số liệu bộ nhớ của tiến trình hiện tại. Đơn vị: MB. */
object MemoryStats {
    private const val MB = 1024L * 1024L

    /** Java heap đang dùng (Dalvik/ART heap). */
    fun javaHeapUsedMb(): Long {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / MB
    }

    /** Giới hạn Java heap của app. */
    fun javaHeapMaxMb(): Long = Runtime.getRuntime().maxMemory() / MB

    /**
     * Native heap. Từ Android 8 (API 26) pixel của Bitmap nằm ở đây,
     * không nằm trong Java heap => Bitmap lớn làm native heap tăng mạnh.
     */
    fun nativeHeapMb(): Long = Debug.getNativeHeapAllocatedSize() / MB

    /** Tổng PSS (proportional set size): mức RAM thực sự app chiếm dụng. */
    fun totalPssMb(): Long {
        val mi = Debug.MemoryInfo()
        Debug.getMemoryInfo(mi)
        return mi.totalPss / 1024L // totalPss tính bằng KB
    }

    /** Số lần GC của ART từ khi tiến trình khởi động. */
    fun gcCount(): Long =
        Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: -1L
}
