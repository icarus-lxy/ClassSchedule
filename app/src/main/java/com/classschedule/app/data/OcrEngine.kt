package com.classschedule.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** 一行识别结果 + 它在图片里的位置（像素） */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val boxHeight: Int get() = (bottom - top).coerceAtLeast(1)
}

/**
 * 用 ML Kit 的中文文字识别（完全离线、不需要任何权限）把图片转成带坐标的文字行。
 */
object OcrEngine {

    /**
     * 读取相册/文件里的图片；过大的图片按 2 的幂降采样，避免内存爆掉、也加快识别。
     */
    fun loadBitmap(context: Context, uri: Uri, maxSide: Int = 2400): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return null
            var sample = 1
            while (longest / sample > maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** 识别出所有文字行（带坐标），交给解析器还原课表 */
    suspend fun recognizeLines(bitmap: Bitmap): List<OcrLine> {
        val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        try {
            val text: Text = suspendCancellableCoroutine { cont ->
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { error -> cont.resumeWithException(error) }
            }
            val lines = mutableListOf<OcrLine>()
            for (block in text.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    val content = line.text.trim()
                    if (content.isNotEmpty()) {
                        lines.add(OcrLine(content, box.left, box.top, box.right, box.bottom))
                    }
                }
            }
            return lines
        } finally {
            recognizer.close()
        }
    }

    /** 原始识别文字，用于人工排查；withCoords 时带上坐标，方便定位是哪一列/哪一行 */
    fun rawTextOf(lines: List<OcrLine>, withCoords: Boolean): String =
        lines.joinToString("\n") { line ->
            if (withCoords) "[${line.left},${line.top}] ${line.text}" else line.text
        }
}
