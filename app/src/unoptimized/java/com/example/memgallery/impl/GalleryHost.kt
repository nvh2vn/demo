package com.example.memgallery.impl

import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.memgallery.metrics.Metrics

/**
 * ❌ BẢN CHƯA TỐI ƯU: ScrollView + GridLayout.
 *
 * Vấn đề cố ý giữ lại:
 *  - Tạo ImageView cho TẤT CẢ ảnh ngay lúc mở app (không tái sử dụng view).
 *  - Decode TẤT CẢ ảnh (full-size) trên main thread ngay trong constructor.
 *  => RAM tăng vọt, thời gian khởi động rất dài, có thể ANR / OOM.
 */
class GalleryHost(
    activity: AppCompatActivity,
    photos: List<String>,
    onPhotoClick: (String) -> Unit,
) {
    private val scroll = ScrollView(activity)
    val view: View get() = scroll

    init {
        val columns = 3
        val cell = activity.resources.displayMetrics.widthPixels / columns

        val grid = GridLayout(activity).apply { columnCount = columns }
        scroll.addView(grid, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        for (name in photos) {
            // ❌ VẤN ĐỀ: mỗi ảnh một view mới, tạo hết ngay từ đầu
            val iv = ImageView(activity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = null
                setOnClickListener { onPhotoClick(name) }
            }
            Metrics.viewsCreated.incrementAndGet()
            grid.addView(iv, GridLayout.LayoutParams().apply { width = cell; height = cell })

            // ❌ VẤN ĐỀ: decode ảnh gốc ngay trên main thread
            ImageLoader.load(activity, name, iv, cell, cell, cover = true)
        }
    }
}
