package com.github.panpf.sketch.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.Keep
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.BitmapPool
import com.github.panpf.sketch.decode.Transformed
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.util.JsonSerializable
import com.github.panpf.sketch.util.JsonSerializer
import org.json.JSONObject

class RotateTransformation(val degrees: Int) : Transformation {

    override val key: String = "RotateTransformation($degrees)"

    override suspend fun transform(
        sketch: Sketch,
        request: ImageRequest,
        input: Bitmap
    ): TransformResult? {
        if (degrees % 360 == 0) return null
        return TransformResult(
            rotate(input, degrees, sketch.bitmapPool),
            RotateTransformed(degrees)
        )
    }

    companion object {
        fun rotate(bitmap: Bitmap, degrees: Int, bitmapPool: BitmapPool): Bitmap {
            val matrix = Matrix()
            matrix.setRotate(degrees.toFloat())

            val newRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            matrix.mapRect(newRect)
            val newWidth = newRect.width().toInt()
            val newHeight = newRect.height().toInt()

            // If the Angle is not divisible by 90°, the new image will be oblique, so support transparency so that the oblique part is not black
            var config = bitmap.config ?: Bitmap.Config.ARGB_8888
            if (degrees % 90 != 0 && config != Bitmap.Config.ARGB_8888) {
                config = Bitmap.Config.ARGB_8888
            }
            val result = bitmapPool.getOrCreate(newWidth, newHeight, config)
            matrix.postTranslate(-newRect.left, -newRect.top)
            val canvas = Canvas(result)
            val paint = Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, matrix, paint)
            return result
        }
    }
}

class RotateTransformed(val degrees: Int) : Transformed {

    override val key: String by lazy { toString() }
    override val cacheResultToDisk: Boolean = true

    override fun toString(): String = "RotateTransformed($degrees)"

    override fun <T : JsonSerializable, T1 : JsonSerializer<T>> getSerializerClass(): Class<T1> {
        @Suppress("UNCHECKED_CAST")
        return Serializer::class.java as Class<T1>
    }

    @Keep
    class Serializer : JsonSerializer<RotateTransformed> {
        override fun toJson(t: RotateTransformed): JSONObject =
            JSONObject().apply {
                t.apply {
                    put("degrees", degrees)
                }
            }

        override fun fromJson(jsonObject: JSONObject): RotateTransformed =
            RotateTransformed(
                jsonObject.getInt("degrees")
            )
    }
}

fun List<Transformed>.getRotateTransformed(): RotateTransformed? =
    find { it is RotateTransformed } as RotateTransformed?