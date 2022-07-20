package com.github.panpf.sketch.test.decode.internal

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Bitmap.Config.RGB_565
import android.graphics.ColorSpace
import android.graphics.ColorSpace.Named.ADOBE_RGB
import android.graphics.ColorSpace.Named.SRGB
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.datasource.DataSource
import com.github.panpf.sketch.datasource.FileDataSource
import com.github.panpf.sketch.decode.internal.BitmapDecodeException
import com.github.panpf.sketch.decode.internal.DefaultBitmapDecoder
import com.github.panpf.sketch.decode.internal.exifOrientationName
import com.github.panpf.sketch.decode.internal.getExifOrientationTransformed
import com.github.panpf.sketch.decode.internal.getInSampledTransformed
import com.github.panpf.sketch.decode.internal.getResizeTransformed
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.resize.Precision.EXACTLY
import com.github.panpf.sketch.resize.Precision.LESS_PIXELS
import com.github.panpf.sketch.resize.Precision.SAME_ASPECT_RATIO
import com.github.panpf.sketch.resize.Scale.CENTER_CROP
import com.github.panpf.sketch.resize.Scale.END_CROP
import com.github.panpf.sketch.resize.Scale.FILL
import com.github.panpf.sketch.resize.Scale.START_CROP
import com.github.panpf.sketch.test.utils.ExifOrientationTestFileHelper
import com.github.panpf.sketch.test.utils.TestAssets
import com.github.panpf.sketch.test.utils.corners
import com.github.panpf.sketch.test.utils.getTestContextAndNewSketch
import com.github.panpf.sketch.util.format
import com.github.panpf.sketch.util.toShortInfoString
import com.github.panpf.tools4j.test.ktx.assertNoThrow
import com.github.panpf.tools4j.test.ktx.assertThrow
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class DefaultBitmapDecoderTest {

    @Test
    fun testDefault() {
        val (context, sketch) = getTestContextAndNewSketch()

        LoadRequest(context, newAssetUri("sample.jpeg")).run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1291x1936,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
        }

        LoadRequest(context, newAssetUri("sample.webp")).run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.webp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1080x1344,ARGB_8888)", bitmap.toShortInfoString())
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Assert.assertEquals(
                    "ImageInfo(1080x1344,'image/webp',UNDEFINED)",
                    imageInfo.toShortString()
                )
            } else {
                Assert.assertEquals("ImageInfo(1080x1344,'',UNDEFINED)", imageInfo.toShortString())
            }
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
        }

        // exif
        ExifOrientationTestFileHelper(context, "exif_origin_clock_hor.jpeg").files().forEach {
            LoadRequest(context, it.file.path).run {
                DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, it.file))
                    .let { runBlocking { it.decode() } }
            }.apply {
                Assert.assertEquals("Bitmap(1500x750,ARGB_8888)", bitmap.toShortInfoString())
                Assert.assertEquals(
                    "ImageInfo(1500x750,'image/jpeg',${exifOrientationName(it.exifOrientation)})",
                    imageInfo.toShortString()
                )
                Assert.assertEquals(LOCAL, dataFrom)
                Assert.assertNotNull(transformedList?.getExifOrientationTransformed())
            }
        }
    }

    @Test
    fun testBitmapConfig() {
        val (context, sketch) = getTestContextAndNewSketch()

        LoadRequest(context, newAssetUri("sample.jpeg")) {
            bitmapConfig(RGB_565)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1291x1936,RGB_565)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
        }

        LoadRequest(context, newAssetUri("sample.webp")) {
            bitmapConfig(RGB_565)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.webp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1080x1344,RGB_565)", bitmap.toShortInfoString())
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Assert.assertEquals(
                    "ImageInfo(1080x1344,'image/webp',UNDEFINED)",
                    imageInfo.toShortString()
                )
            } else {
                Assert.assertEquals("ImageInfo(1080x1344,'')", imageInfo.toShortString())
            }
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
        }
    }

    @Test
    fun testColorSpace() {
        if (VERSION.SDK_INT < VERSION_CODES.O) return

        val (context, sketch) = getTestContextAndNewSketch()

        LoadRequest(context, newAssetUri("sample.jpeg")).run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1291x1936,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
            Assert.assertEquals(ColorSpace.get(SRGB), bitmap.colorSpace)
        }

        LoadRequest(context, newAssetUri("sample.webp")).run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.webp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1080x1344,ARGB_8888)", bitmap.toShortInfoString())
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Assert.assertEquals(
                    "ImageInfo(1080x1344,'image/webp',UNDEFINED)",
                    imageInfo.toShortString()
                )
            } else {
                Assert.assertEquals("ImageInfo(1080x1344,'')", imageInfo.toShortString())
            }
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
            Assert.assertEquals(ColorSpace.get(SRGB), bitmap.colorSpace)
        }

        LoadRequest(context, newAssetUri("sample.jpeg")) {
            colorSpace(ColorSpace.get(ADOBE_RGB))
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1291x1936,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
            Assert.assertEquals(ColorSpace.get(ADOBE_RGB), bitmap.colorSpace)
        }

        LoadRequest(context, newAssetUri("sample.webp")) {
            colorSpace(ColorSpace.get(ADOBE_RGB))
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.webp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertEquals("Bitmap(1080x1344,ARGB_8888)", bitmap.toShortInfoString())
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Assert.assertEquals(
                    "ImageInfo(1080x1344,'image/webp',UNDEFINED)",
                    imageInfo.toShortString()
                )
            } else {
                Assert.assertEquals("ImageInfo(1080x1344,'')", imageInfo.toShortString())
            }
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNull(transformedList)
            Assert.assertEquals(ColorSpace.get(ADOBE_RGB), bitmap.colorSpace)
        }
    }

    @Test
    fun testResize() {
        val (context, sketch) = getTestContextAndNewSketch()

        // precision = LESS_PIXELS
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(800, 800, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 800 * 800 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(646x968,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 500, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(323x484,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }

        // precision = SAME_ASPECT_RATIO
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                500f.div(300).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(323x194,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(322x193,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(300, 500, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                300f.div(500).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(291x484,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(290x484,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // precision = EXACTLY
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals("Bitmap(500x300,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(300, 500, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals("Bitmap(300x500,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',NORMAL)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // scale
        val startCropBitmap = LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, SAME_ASPECT_RATIO, START_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val centerCropBitmap = LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, SAME_ASPECT_RATIO, CENTER_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val endCropBitmap = LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, SAME_ASPECT_RATIO, END_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val fillBitmap = LoadRequest(context, newAssetUri("sample.jpeg")) {
            resize(500, 300, SAME_ASPECT_RATIO, FILL)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.jpeg"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        Assert.assertTrue(startCropBitmap.width * startCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(centerCropBitmap.width * centerCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(endCropBitmap.width * endCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(fillBitmap.width * fillBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            centerCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(endCropBitmap.corners().toString(), fillBitmap.corners().toString())
    }

    @Test
    fun testResizeNoRegion() {
        val (context, sketch) = getTestContextAndNewSketch()

        // precision = LESS_PIXELS
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 500, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(350x506,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(200, 200, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 200 * 200 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(87x126,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNull(transformedList?.getResizeTransformed())
        }

        // precision = SAME_ASPECT_RATIO
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                500f.div(300).format(1)
            )
            Assert.assertEquals("Bitmap(350x210,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(300, 500, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                300f.div(500).format(1)
            )
            Assert.assertEquals("Bitmap(152x253,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }

        // precision = EXACTLY
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals("Bitmap(500x300,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(300, 500, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals("Bitmap(300x500,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(700x1012,'image/bmp',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // scale
        val startCropBitmap = LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, SAME_ASPECT_RATIO, START_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val centerCropBitmap = LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, SAME_ASPECT_RATIO, CENTER_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val endCropBitmap = LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, SAME_ASPECT_RATIO, END_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val fillBitmap = LoadRequest(context, newAssetUri("sample.bmp")) {
            resize(500, 300, SAME_ASPECT_RATIO, FILL)
        }.run {
            DefaultBitmapDecoder(sketch, this, AssetDataSource(sketch, this, "sample.bmp"))
                .let { runBlocking { it.decode() } }
        }.bitmap
        Assert.assertTrue(startCropBitmap.width * startCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(centerCropBitmap.width * centerCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(endCropBitmap.width * endCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(fillBitmap.width * fillBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            centerCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(endCropBitmap.corners().toString(), fillBitmap.corners().toString())
    }

    @Test
    fun testResizeExif() {
        val (context, sketch) = getTestContextAndNewSketch()

        val testFile = ExifOrientationTestFileHelper(context, "sample.jpeg").files()
            .find { it.exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE }!!

        // precision = LESS_PIXELS
        LoadRequest(context, testFile.file.path) {
            resize(800, 800, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 800 * 800 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(646x968,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(500, 500, LESS_PIXELS)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(323x484,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }

        // precision = SAME_ASPECT_RATIO
        LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                500f.div(300).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(323x194,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(322x193,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(300, 500, SAME_ASPECT_RATIO)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                300f.div(500).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(291x484,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(290x484,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // precision = EXACTLY
        LoadRequest(context, testFile.file.path) {
            resize(500, 300, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals("Bitmap(500x300,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(300, 500, EXACTLY)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals("Bitmap(300x500,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1291x1936,'image/jpeg',TRANSPOSE)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // scale
        val startCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, START_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val centerCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, CENTER_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val endCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, END_CROP)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val fillBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, FILL)
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        Assert.assertTrue(startCropBitmap.width * startCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(centerCropBitmap.width * centerCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(endCropBitmap.width * endCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(fillBitmap.width * fillBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            centerCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(endCropBitmap.corners().toString(), fillBitmap.corners().toString())
    }

    @Test
    fun testResizeExifIgnore() {
        val (context, sketch) = getTestContextAndNewSketch()

        val testFile = ExifOrientationTestFileHelper(context, "sample.jpeg").files()
            .find { it.exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE }!!

        // precision = LESS_PIXELS
        LoadRequest(context, testFile.file.path) {
            resize(800, 800, LESS_PIXELS)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 800 * 800 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(968x646,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(500, 500, LESS_PIXELS)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                imageInfo.width.toFloat().div(imageInfo.height).format(1)
            )
            Assert.assertEquals("Bitmap(484x323,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
        }

        // precision = SAME_ASPECT_RATIO
        LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                500f.div(300).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(484x291,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(484x290,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(300, 500, SAME_ASPECT_RATIO)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals(
                bitmap.width.toFloat().div(bitmap.height).format(1),
                300f.div(500).format(1)
            )
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                Assert.assertEquals("Bitmap(194x323,ARGB_8888)", bitmap.toShortInfoString())
            } else {
                Assert.assertEquals("Bitmap(193x322,ARGB_8888)", bitmap.toShortInfoString())
            }
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // precision = EXACTLY
        LoadRequest(context, testFile.file.path) {
            resize(500, 300, EXACTLY)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 500 * 300 * 1.1f
            )
            Assert.assertEquals("Bitmap(500x300,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }
        LoadRequest(context, testFile.file.path) {
            resize(300, 500, EXACTLY)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.apply {
            Assert.assertTrue(
                "${bitmap.width}x${bitmap.height}",
                bitmap.width * bitmap.height <= 300 * 500 * 1.1f
            )
            Assert.assertEquals("Bitmap(300x500,ARGB_8888)", bitmap.toShortInfoString())
            Assert.assertEquals(
                "ImageInfo(1936x1291,'image/jpeg',UNDEFINED)",
                imageInfo.toShortString()
            )
            Assert.assertEquals(LOCAL, dataFrom)
            Assert.assertNotNull(transformedList?.getInSampledTransformed())
            Assert.assertNotNull(transformedList?.getResizeTransformed())
        }

        // scale
        val startCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, START_CROP)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val centerCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, CENTER_CROP)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val endCropBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, END_CROP)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        val fillBitmap = LoadRequest(context, testFile.file.path) {
            resize(500, 300, SAME_ASPECT_RATIO, FILL)
            ignoreExifOrientation()
        }.run {
            DefaultBitmapDecoder(sketch, this, FileDataSource(sketch, this, testFile.file))
                .let { runBlocking { it.decode() } }
        }.bitmap
        Assert.assertTrue(startCropBitmap.width * startCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(centerCropBitmap.width * centerCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(endCropBitmap.width * endCropBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertTrue(fillBitmap.width * fillBitmap.height <= 500 * 300 * 1.1f)
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            centerCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            startCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            endCropBitmap.corners().toString()
        )
        Assert.assertNotEquals(
            centerCropBitmap.corners().toString(),
            fillBitmap.corners().toString()
        )
        Assert.assertNotEquals(endCropBitmap.corners().toString(), fillBitmap.corners().toString())
    }

    @Test
    fun testFactoryEqualsAndHashCode() {
        val element1 = DefaultBitmapDecoder.Factory()
        val element11 = DefaultBitmapDecoder.Factory()
        val element2 = DefaultBitmapDecoder.Factory()

        Assert.assertNotSame(element1, element11)
        Assert.assertNotSame(element1, element2)
        Assert.assertNotSame(element2, element11)

        Assert.assertEquals(element1, element1)
        Assert.assertEquals(element1, element11)
        Assert.assertEquals(element1, element2)
        Assert.assertEquals(element2, element11)
        Assert.assertNotEquals(element1, null)
        Assert.assertNotEquals(element1, Any())

        Assert.assertEquals(element1.hashCode(), element1.hashCode())
        Assert.assertEquals(element1.hashCode(), element11.hashCode())
        Assert.assertEquals(element1.hashCode(), element2.hashCode())
        Assert.assertEquals(element2.hashCode(), element11.hashCode())
    }

    @Test
    fun testError() {
        val (context, sketch) = getTestContextAndNewSketch()

        /* full */
        assertThrow(BitmapDecodeException::class) {
            val request = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500, LESS_PIXELS)
            }
            val dataSource = runBlocking {
                sketch.components.newFetcher(request).fetch().dataSource
            }
            sketch.bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
            DefaultBitmapDecoder(sketch, request, FullTestDataSource(dataSource))
                .let { runBlocking { it.decode() } }
        }

        assertNoThrow {
            val request = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500, LESS_PIXELS)
            }
            val dataSource = runBlocking {
                sketch.components.newFetcher(request).fetch().dataSource
            }
            sketch.bitmapPool.put(Bitmap.createBitmap(323, 484, ARGB_8888))
            DefaultBitmapDecoder(
                sketch,
                request,
                FullTestDataSource(dataSource, enabledCount = true)
            ).let { runBlocking { it.decode() } }
        }

        /* region */
        assertThrow(BitmapDecodeException::class) {
            val request1 = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500)
            }
            val dataSource1 = runBlocking {
                sketch.components.newFetcher(request1).fetch().dataSource
            }
            DefaultBitmapDecoder(sketch, request1, RegionTestDataSource(dataSource1, true))
                .let { runBlocking { it.decode() } }
        }

        assertThrow(BitmapDecodeException::class) {
            val request1 = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500)
            }
            val dataSource1 = runBlocking {
                sketch.components.newFetcher(request1).fetch().dataSource
            }
            DefaultBitmapDecoder(sketch, request1, RegionTestDataSource(dataSource1, false))
                .let { runBlocking { it.decode() } }
        }

        assertNoThrow {
            val request1 = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500)
            }
            val dataSource1 = runBlocking {
                sketch.components.newFetcher(request1).fetch().dataSource
            }
            DefaultBitmapDecoder(
                sketch,
                request1,
                RegionTestDataSource(dataSource1, false, enabledCount = true)
            ).let { runBlocking { it.decode() } }
        }

        assertThrow(BitmapDecodeException::class) {
            val request1 = LoadRequest(context, TestAssets.SAMPLE_JPEG_URI) {
                resize(500, 500)
            }
            val dataSource1 = runBlocking {
                sketch.components.newFetcher(request1).fetch().dataSource
            }
            DefaultBitmapDecoder(sketch, request1, RegionTestDataSource(dataSource1, null))
                .let { runBlocking { it.decode() } }
        }
    }

    class FullTestDataSource(
        private val fileDataSource: DataSource,
        private val enabledCount: Boolean = false,
    ) : DataSource by fileDataSource {

        private var count = 0

        override fun newInputStream(): InputStream {
            val stackStringList = Exception().stackTraceToString().split("\n")
            if (stackStringList.find { it.contains(".realDecodeFull(") } != null) {
                count++
                if (!enabledCount || count == 1) {
                    throw IllegalArgumentException("Problem decoding into existing bitmap")
                }
            }
            return fileDataSource.newInputStream()
        }
    }

    class RegionTestDataSource(
        private val fileDataSource: DataSource,
        private val srcError: Boolean? = false,
        private val enabledCount: Boolean = false,
    ) : DataSource by fileDataSource {

        private var count = 0

        override fun newInputStream(): InputStream {
            val stackStringList = Exception().stackTraceToString().split("\n")
            if (stackStringList.find { it.contains(".realDecodeRegion(") } != null) {
                when (srcError) {
                    true -> {
                        throw IllegalArgumentException("rectangle is outside the image srcRect")
                    }
                    false -> {
                        count++
                        if (!enabledCount || count == 1) {
                            throw IllegalArgumentException("Problem decoding into existing bitmap")
                        }
                    }
                    else -> {
                        throw Exception()
                    }
                }
            }
            return fileDataSource.newInputStream()
        }
    }
}