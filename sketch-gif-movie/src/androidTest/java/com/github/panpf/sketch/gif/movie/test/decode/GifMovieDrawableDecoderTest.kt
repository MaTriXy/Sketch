package com.github.panpf.sketch.gif.movie.test.decode

import android.os.Build
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.AssetDataSource
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.datasource.DataSource
import com.github.panpf.sketch.decode.GifMovieDrawableDecoder
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.drawable.MovieDrawable
import com.github.panpf.sketch.drawable.SketchAnimatableDrawable
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.animatedTransformation
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.request.repeatCount
import com.github.panpf.sketch.sketch
import com.github.panpf.sketch.transform.PixelOpacity
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileDescriptor
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class GifMovieDrawableDecoderTest {

    @Test
    fun testFactory() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return

        val context = InstrumentationRegistry.getInstrumentation().context
        val sketch = context.sketch

        // normal
        val request = DisplayRequest(context, newAssetUri("sample_anim.gif"))
        val fetchResult =
            FetchResult(AssetDataSource(sketch, request, "sample_anim.gif"), "image/gif")
        Assert.assertNotNull(
            GifMovieDrawableDecoder.Factory().create(sketch, request, RequestContext(), fetchResult)
        )
        val fetchResult0 = FetchResult(AssetDataSource(sketch, request, "sample_anim.gif"), null)
        Assert.assertNotNull(
            GifMovieDrawableDecoder.Factory()
                .create(sketch, request, RequestContext(), fetchResult0)
        )

        // not gif
        val request1 = DisplayRequest(context, newAssetUri("sample.png"))
        val fetchResult1 = FetchResult(AssetDataSource(sketch, request1, "sample.png"), null)
        Assert.assertNull(
            GifMovieDrawableDecoder.Factory()
                .create(sketch, request1, RequestContext(), fetchResult1)
        )

        // disallowAnimatedImage true
        val request2 = DisplayRequest(context, newAssetUri("sample_anim.gif")) {
            disallowAnimatedImage()
        }
        val fetchResult2 = FetchResult(ErrorDataSource(sketch, request2, LOCAL), null)
        Assert.assertNull(
            GifMovieDrawableDecoder.Factory()
                .create(sketch, request2, RequestContext(), fetchResult2)
        )

        // mimeType error
        val request3 = DisplayRequest(context, newAssetUri("sample_anim.gif"))
        val fetchResult3 = FetchResult(
            AssetDataSource(sketch, request3, "sample_anim.gif"),
            "image/jpeg",
        )
        Assert.assertNull(
            GifMovieDrawableDecoder.Factory()
                .create(sketch, request3, RequestContext(), fetchResult3)
        )
    }

    @Test
    fun testDecodeDrawable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return

        val context = InstrumentationRegistry.getInstrumentation().context
        val sketch = context.sketch

        val request = DisplayRequest(context, newAssetUri("sample_anim.gif"))
        val fetchResult = sketch.components.newFetcher(request).let { runBlocking { it.fetch() } }
        GifMovieDrawableDecoder.Factory()
            .create(sketch, request, RequestContext(), fetchResult)!!
            .let { runBlocking { it.decode() } }.apply {
                Assert.assertEquals(ImageInfo(480, 480, "image/gif"), this.imageInfo)
                Assert.assertEquals(480, this.drawable.intrinsicWidth)
                Assert.assertEquals(480, this.drawable.intrinsicHeight)
                Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, this.exifOrientation)
                Assert.assertEquals(LOCAL, this.dataFrom)
                Assert.assertNull(this.transformedList)
                val movieDrawable =
                    (this.drawable as SketchAnimatableDrawable).wrappedDrawable as MovieDrawable
                Assert.assertEquals(-1, movieDrawable.getRepeatCount())
                Assert.assertNull(movieDrawable.getAnimatedTransformation())
            }

        val request1 = DisplayRequest(context, newAssetUri("sample_anim.gif")) {
            repeatCount(3)
            animatedTransformation { PixelOpacity.TRANSLUCENT }
            resize(300, 300)
        }
        val fetchResult1 = sketch.components.newFetcher(request1).let { runBlocking { it.fetch() } }
        GifMovieDrawableDecoder.Factory()
            .create(sketch, request1, RequestContext(), fetchResult1)!!
            .let { runBlocking { it.decode() } }.apply {
                Assert.assertEquals(ImageInfo(480, 480, "image/gif"), this.imageInfo)
                Assert.assertEquals(480, this.drawable.intrinsicWidth)
                Assert.assertEquals(480, this.drawable.intrinsicHeight)
                Assert.assertEquals(ExifInterface.ORIENTATION_UNDEFINED, this.exifOrientation)
                Assert.assertEquals(LOCAL, this.dataFrom)
                Assert.assertNull(this.transformedList)
                val movieDrawable =
                    (this.drawable as SketchAnimatableDrawable).wrappedDrawable as MovieDrawable
                Assert.assertEquals(3, movieDrawable.getRepeatCount())
                Assert.assertNotNull(movieDrawable.getAnimatedTransformation())
            }
    }

    private class ErrorDataSource(
        override val sketch: Sketch,
        override val request: ImageRequest,
        override val dataFrom: DataFrom
    ) : DataSource {
        override fun length(): Long = throw UnsupportedOperationException("Unsupported length()")

        override fun newFileDescriptor(): FileDescriptor =
            throw UnsupportedOperationException("Unsupported newFileDescriptor()")

        override fun newInputStream(): InputStream =
            throw UnsupportedOperationException("Unsupported newInputStream()")
    }
}