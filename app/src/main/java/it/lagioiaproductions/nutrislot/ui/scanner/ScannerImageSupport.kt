package it.lagioiaproductions.nutrislot.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

internal class ScannerImageLoader(
    private val context: Context
) {

    fun decode(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to decode scanner image", error)
            null
        }
    }

    private companion object {
        const val TAG = "ScannerImageLoader"
    }
}

internal object ScannerImageEncoder {

    private const val MAX_UPLOAD_DIMENSION = 1600

    fun prepareForUpload(bitmap: Bitmap): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= MAX_UPLOAD_DIMENSION) return bitmap

        val scale = MAX_UPLOAD_DIMENSION.toFloat() / largestSide.toFloat()
        val newWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return bitmap.scale(newWidth, newHeight)
    }

    fun toJpeg(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            output.toByteArray()
        }
    }
}
