package com.example.memgallery.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import com.example.memgallery.data.PhotoRepository
import com.example.memgallery.metrics.Metrics

/**
 * ❌ BẢN CHƯA TỐI ƯU — cố tình viết "ngây thơ" để làm mốc so sánh.
 *
 * Có 3 vấn đề được cố ý giữ lại:
 *  1. Decode ảnh GỐC 4000x3000 (≈ 46 MB trong RAM) dù ô hiển thị chỉ ~360 px.
 *  2. Decode ngay trên MAIN THREAD => UI đứng hình.
 *  3. KHÔNG có cache => cuộn lại / mở lại là decode lại từ đầu.
 *
 * Chữ ký hàm giống hệt bản optimized để code dùng chung không cần biết đang ở bản nào.
 */
object ImageLoader {

    @Suppress("UNUSED_PARAMETER")
    fun load(context: Context, name: String, target: ImageView, reqW: Int, reqH: Int, cover: Boolean) {
        val t0 = System.nanoTime()
        Metrics.cacheMiss.incrementAndGet() // không có cache => lần nào cũng là "miss"
        try {
            // ❌ VẤN ĐỀ: decode nguyên kích thước, không dùng inSampleSize, chạy trên main thread
            val bitmap: Bitmap? = PhotoRepository.open(context, name).use { BitmapFactory.decodeStream(it) }
            target.setImageBitmap(bitmap)
        } catch (e: OutOfMemoryError) {
            // Ghi nhận OOM để đưa vào bảng so sánh (thay vì để app chết khi đang đo)
            Metrics.oomCount.incrementAndGet()
            target.setImageDrawable(ColorDrawable(Color.DKGRAY))
        }
        Metrics.recordDecode(System.nanoTime() - t0)
    }

    /** Bản này không có tác vụ nền để hủy. */
    @Suppress("UNUSED_PARAMETER")
    fun cancel(target: ImageView) = Unit
}
