package com.example.memgallery.data

import android.content.Context
import com.example.memgallery.BuildConfig
import java.io.InputStream

/**
 * Nguồn dữ liệu ảnh DÙNG CHUNG cho cả 2 bản.
 * Ảnh nằm cố định trong assets/photos => cùng bộ dữ liệu, đo đạc lặp lại được.
 */
object PhotoRepository {
    private const val DIR = "photos"

    fun list(context: Context): List<String> =
        (context.assets.list(DIR) ?: emptyArray())
            .filter { it.endsWith(".jpg", ignoreCase = true) }
            .sorted()
            .take(BuildConfig.PHOTO_COUNT)

    fun open(context: Context, name: String): InputStream =
        context.assets.open("$DIR/$name")
}
