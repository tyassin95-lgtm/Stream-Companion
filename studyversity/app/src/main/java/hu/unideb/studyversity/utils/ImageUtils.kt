package hu.unideb.studyversity.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {
    /** Reads the picked image, scales it down to at most [maxSide] px and returns it as JPEG (profile pictures stay small). */
    fun readScaledJpeg(context: Context, uri: Uri, maxSide: Int = 800, quality: Int = 85): ByteArray? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxSide || bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        bitmap?.let {
            val scale = maxSide.toFloat() / maxOf(it.width, it.height)
            val out = if (scale < 1f) Bitmap.createScaledBitmap(it, (it.width * scale).toInt(), (it.height * scale).toInt(), true) else it
            ByteArrayOutputStream().also { s -> out.compress(Bitmap.CompressFormat.JPEG, quality, s) }.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
