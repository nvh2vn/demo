package com.example.memgallery.impl

import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.memgallery.DetailActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ✅ BẢN ĐÃ TỐI ƯU (Kỹ thuật 3): sửa cả 2 lỗi leak của bản unoptimized.
 *
 *  Sửa lỗi 1: danh sách dùng WeakReference và được gỡ trong detach() (gọi ở onDestroy).
 *  Sửa lỗi 2: dùng lifecycleScope — coroutine TỰ HỦY khi Activity bị destroy,
 *             nên không còn Runnable nào giữ Activity.
 */
object DetailHelper {

    // ✅ WeakReference không ngăn GC thu hồi Activity; vẫn gỡ chủ động trong detach()
    private val openedActivities = mutableListOf<WeakReference<DetailActivity>>()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun attach(activity: DetailActivity, status: TextView) {
        openedActivities.add(WeakReference(activity))
        status.text = "Mở lúc ${timeFormat.format(Date())}"

        // ✅ Gắn với vòng đời Activity: bị hủy tự động khi onDestroy
        activity.lifecycleScope.launch {
            delay(60_000)
            status.text = "Đã xem ${openedActivities.count { it.get() != null }} ảnh trong phiên này"
        }
    }

    fun detach(activity: DetailActivity) {
        // ✅ Gỡ activity này (và dọn các tham chiếu đã bị GC)
        openedActivities.removeAll { it.get() == null || it.get() === activity }
    }
}
