package com.example.memgallery.impl

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.example.memgallery.DetailActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ❌ BẢN CHƯA TỐI ƯU — chứa 2 lỗi MEMORY LEAK kinh điển:
 *
 *  Lỗi 1: biến "static" (object) giữ tham chiếu tới Activity và không bao giờ xóa.
 *  Lỗi 2: Handler + lambda ẩn danh giữ TextView (=> giữ Activity) tới 60 giây sau khi đóng.
 *
 * Kịch bản kiểm tra: mở ảnh -> Back, lặp lại 10 lần => LeakCanary báo leak,
 * "DetailAlive" trên khung số liệu tăng dần dù đã bấm GC.
 */
object DetailHelper {

    // ❌ LEAK #1: object là singleton sống suốt vòng đời tiến trình (GC root).
    //    Activity nằm trong list này => không bao giờ được GC.
    private val openedActivities = mutableListOf<DetailActivity>()

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun attach(activity: DetailActivity, status: TextView) {
        openedActivities.add(activity)
        status.text = "Mở lúc ${timeFormat.format(Date())}"

        // ❌ LEAK #2: lambda bắt (capture) `status` -> TextView giữ Context = Activity.
        //    MessageQueue giữ lambda này 60 giây, kể cả khi Activity đã đóng.
        handler.postDelayed({
            status.text = "Đã xem ${openedActivities.size} ảnh trong phiên này"
        }, 60_000)
    }

    fun detach(activity: DetailActivity) {
        // ❌ VẤN ĐỀ: quên gỡ activity khỏi list và quên hủy callback của Handler
    }
}
