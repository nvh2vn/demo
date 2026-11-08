package com.example.memgallery

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.memgallery.bench.AutoScroller
import com.example.memgallery.data.PhotoRepository
import com.example.memgallery.impl.GalleryHost
import com.example.memgallery.metrics.Metrics
import com.example.memgallery.metrics.MetricsOverlay

/**
 * Màn hình chính — DÙNG CHUNG cho 2 bản.
 * Chỉ GalleryHost (trong impl/ của từng flavor) khác nhau.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var overlay: MetricsOverlay
    private lateinit var host: GalleryHost
    private var autoScroller: AutoScroller? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val t0 = SystemClock.elapsedRealtime()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<FrameLayout>(R.id.galleryContainer)
        val photos = PhotoRepository.list(this)

        // Bản unoptimized: dòng này decode TẤT CẢ ảnh trên main thread => chậm/giật.
        host = GalleryHost(this, photos) { name ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra(DetailActivity.EXTRA_NAME, name))
        }
        container.addView(host.view)

        // Startup = từ onCreate đến lúc frame đầu tiên chuẩn bị vẽ (màn hình tương tác được)
        container.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                container.viewTreeObserver.removeOnPreDrawListener(this)
                Metrics.startupMs = SystemClock.elapsedRealtime() - t0
                return true
            }
        })

        overlay = MetricsOverlay(this, findViewById<TextView>(R.id.overlay))

        findViewById<Button>(R.id.btnAutoScroll).setOnClickListener {
            if (autoScroller?.isRunning == true) {
                autoScroller?.stop()
                return@setOnClickListener
            }
            autoScroller = AutoScroller(host.view) { ms ->
                Toast.makeText(this, "Auto-scroll xong: $ms ms", Toast.LENGTH_LONG).show()
            }.also { it.start() }
        }
        findViewById<Button>(R.id.btnReset).setOnClickListener {
            Metrics.reset()
            System.gc() // chỉ để đo: đưa heap về trạng thái "sạch" trước kịch bản mới
        }
        findViewById<Button>(R.id.btnGc).setOnClickListener {
            // Dùng trước khi đọc "DetailAlive": ép GC để biết Activity nào thực sự bị leak
            System.gc()
            Runtime.getRuntime().runFinalization()
            System.gc()
        }
    }

    override fun onStart() {
        super.onStart()
        overlay.start()
    }

    override fun onStop() {
        overlay.stop()
        autoScroller?.stop()
        super.onStop()
    }
}
