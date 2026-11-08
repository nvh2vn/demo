package com.example.memgallery

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.memgallery.impl.DetailHelper
import com.example.memgallery.impl.ImageLoader
import com.example.memgallery.metrics.Metrics
import com.example.memgallery.metrics.MetricsOverlay

/** Xem 1 ảnh toàn màn hình — DÙNG CHUNG cho 2 bản. */
class DetailActivity : AppCompatActivity() {

    private lateinit var overlay: MetricsOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        Metrics.trackDetail(this) // chỉ để ĐO số Activity còn sống (WeakReference)

        val name = intent.getStringExtra(EXTRA_NAME) ?: return
        val image = findViewById<ImageView>(R.id.detailImage)
        val status = findViewById<TextView>(R.id.detailStatus)

        val dm = resources.displayMetrics
        ImageLoader.load(this, name, image, dm.widthPixels, dm.heightPixels, cover = false)

        // Chỗ khác biệt giữa 2 bản: bản unoptimized rò rỉ Activity ở đây
        DetailHelper.attach(this, status)

        overlay = MetricsOverlay(this, findViewById(R.id.overlay))
    }

    override fun onStart() {
        super.onStart()
        overlay.start()
    }

    override fun onStop() {
        overlay.stop()
        super.onStop()
    }

    override fun onDestroy() {
        DetailHelper.detach(this)
        ImageLoader.cancel(findViewById(R.id.detailImage))
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NAME = "photo_name"
    }
}
