package com.example.memgallery.bench

import android.util.Log
import android.view.Choreographer
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Cuộn tự động với TỐC ĐỘ CỐ ĐỊNH (px/giây, tính theo thời gian thật giữa 2 frame)
 * để mỗi lần đo đều chạy cùng một kịch bản => kết quả lặp lại được.
 *
 * Một "vòng" = cuộn từ đầu xuống cuối rồi cuộn ngược về đầu.
 * Dùng được cho cả RecyclerView (bản optimized) lẫn ScrollView (bản unoptimized):
 * chỉ cần View có scrollBy() và canScrollVertically().
 */
class AutoScroller(
    private val target: View,
    private val rounds: Int = 3,
    private val pxPerSecond: Float = 2500f,
    private val onFinished: (elapsedMs: Long) -> Unit,
) {
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var direction = 1       // 1 = xuống, -1 = lên
    private var completedRounds = 0
    private var lastFrameNanos = 0L
    private var startNanos = 0L

    val isRunning: Boolean get() = running

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val dtSec = if (lastFrameNanos == 0L) 0f else (frameTimeNanos - lastFrameNanos) / 1e9f
            lastFrameNanos = frameTimeNanos

            val dy = (pxPerSecond * dtSec * direction).toInt()
            if (dy != 0) target.scrollBy(0, dy)

            if (!target.canScrollVertically(direction)) {
                if (direction == 1) {
                    direction = -1               // tới đáy -> quay lại
                } else {
                    completedRounds++            // về tới đỉnh -> xong 1 vòng
                    direction = 1
                    if (completedRounds >= rounds) {
                        finish(frameTimeNanos)
                        return
                    }
                }
            }
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        direction = 1
        completedRounds = 0
        lastFrameNanos = 0L
        startNanos = System.nanoTime()
        // RecyclerView không hỗ trợ scrollTo() tuyệt đối nên dùng scrollToPosition
        if (target is RecyclerView) target.scrollToPosition(0) else target.scrollTo(0, 0)
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(callback)
    }

    private fun finish(@Suppress("UNUSED_PARAMETER") frameTimeNanos: Long) {
        running = false
        val ms = (System.nanoTime() - startNanos) / 1_000_000
        Log.i("MEMGALLERY", "AUTOSCROLL done: $rounds rounds in $ms ms")
        onFinished(ms)
    }
}
