package com.example.memgallery.impl

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.memgallery.metrics.Metrics

/**
 * ✅ BẢN ĐÃ TỐI ƯU (Kỹ thuật 4): RecyclerView + ViewHolder.
 *
 *  - Chỉ tạo view cho số ô đang hiển thị (+ vài ô dự phòng), rồi TÁI SỬ DỤNG khi cuộn.
 *  - Ảnh được nạp bất đồng bộ + có cache (xem ImageLoader).
 *  - Khi view bị thu hồi (onViewRecycled) thì HỦY việc tải ảnh cũ để không gán nhầm ảnh.
 */
class GalleryHost(
    private val activity: AppCompatActivity,
    private val photos: List<String>,
    private val onPhotoClick: (String) -> Unit,
) {
    private val columns = 3
    private val cell = activity.resources.displayMetrics.widthPixels / columns

    private val recycler = RecyclerView(activity).apply {
        layoutManager = GridLayoutManager(activity, columns)
        setHasFixedSize(true) // kích thước RecyclerView không đổi theo nội dung
        adapter = PhotoAdapter()
    }
    val view: View get() = recycler

    private inner class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.Holder>() {

        inner class Holder(val image: ImageView) : RecyclerView.ViewHolder(image)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            // ✅ Chỉ chạy khi cần view MỚI (không phải mỗi lần cuộn)
            Metrics.viewsCreated.incrementAndGet()
            val iv = ImageView(activity).apply {
                layoutParams = RecyclerView.LayoutParams(cell, cell)
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = null
            }
            return Holder(iv)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val name = photos[position]
            holder.image.setOnClickListener { onPhotoClick(name) }
            ImageLoader.load(activity, name, holder.image, cell, cell, cover = true)
        }

        override fun onViewRecycled(holder: Holder) {
            // ✅ Hủy tải ảnh cũ + bỏ tham chiếu bitmap khi view rời màn hình
            ImageLoader.cancel(holder.image)
            holder.image.setImageDrawable(null)
        }

        override fun getItemCount(): Int = photos.size
    }
}
