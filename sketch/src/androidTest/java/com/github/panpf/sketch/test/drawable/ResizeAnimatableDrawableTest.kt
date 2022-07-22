package com.github.panpf.sketch.test.drawable

import android.R.drawable
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback
import com.github.panpf.sketch.datasource.DataFrom.LOCAL
import com.github.panpf.sketch.decode.ImageInfo
import com.github.panpf.sketch.drawable.SketchAnimatableDrawable
import com.github.panpf.sketch.drawable.internal.ResizeAnimatableDrawable
import com.github.panpf.sketch.fetch.newAssetUri
import com.github.panpf.sketch.resize.Resize
import com.github.panpf.sketch.test.utils.TestAnimatableDrawable1
import com.github.panpf.sketch.test.utils.TestNewMutateDrawable
import com.github.panpf.sketch.test.utils.getTestContext
import com.github.panpf.sketch.util.getDrawableCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResizeAnimatableDrawableTest {

    @Test
    fun test() {
        val imageUri = newAssetUri("sample.jpeg")
        val animDrawable = SketchAnimatableDrawable(
            animatableDrawable = TestAnimatableDrawable1(ColorDrawable(Color.GREEN)),
            imageUri = imageUri,
            requestKey = imageUri,
            requestCacheKey = imageUri,
            imageInfo = ImageInfo(100, 200, "image/jpeg", 0),
            dataFrom = LOCAL,
            transformedList = null,
        )
        ResizeAnimatableDrawable(animDrawable, Resize(100, 500)).apply {
            start()
            stop()
            isRunning

            val callback = object : AnimationCallback() {}
            Assert.assertFalse(unregisterAnimationCallback(callback))
            runBlocking(Dispatchers.Main) {
                registerAnimationCallback(callback)
            }
            Assert.assertTrue(unregisterAnimationCallback(callback))
            clearAnimationCallbacks()
        }
    }

    @Test
    fun testMutate() {
        val context = getTestContext()
        val imageUri = newAssetUri("sample.jpeg")

        ResizeAnimatableDrawable(
            drawable = SketchAnimatableDrawable(
                animatableDrawable = TestAnimatableDrawable1(context.getDrawableCompat(android.R.drawable.bottom_bar)),
                imageUri = imageUri,
                requestKey = imageUri,
                requestCacheKey = imageUri,
                imageInfo = ImageInfo(100, 200, "image/jpeg", 0),
                dataFrom = LOCAL,
                transformedList = null,
            ),
            resize = Resize(500, 300)
        ).apply {
            mutate()
            alpha = 146

            context.getDrawableCompat(android.R.drawable.bottom_bar).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Assert.assertEquals(255, it.alpha)
                }
            }
        }

        ResizeAnimatableDrawable(
            drawable = SketchAnimatableDrawable(
                animatableDrawable = TestAnimatableDrawable1(
                    TestNewMutateDrawable(context.getDrawableCompat(drawable.bottom_bar))
                ),
                imageUri = imageUri,
                requestKey = imageUri,
                requestCacheKey = imageUri,
                imageInfo = ImageInfo(100, 200, "image/jpeg", 0),
                dataFrom = LOCAL,
                transformedList = null,
            ),
            resize = Resize(500, 300)
        ).apply {
            mutate()
            alpha = 146

            context.getDrawableCompat(android.R.drawable.bottom_bar).also {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Assert.assertEquals(255, it.alpha)
                }
            }
        }
    }

    @Test
    fun testToString() {
        val imageUri = newAssetUri("sample.jpeg")
        val animDrawable = SketchAnimatableDrawable(
            animatableDrawable = TestAnimatableDrawable1(ColorDrawable(Color.GREEN)),
            imageUri = imageUri,
            requestKey = imageUri,
            requestCacheKey = imageUri,
            imageInfo = ImageInfo(100, 200, "image/jpeg", 0),
            dataFrom = LOCAL,
            transformedList = null,
        )
        ResizeAnimatableDrawable(animDrawable, Resize(100, 500)).apply {
            Assert.assertEquals("ResizeAnimatableDrawable($animDrawable)", toString())
        }
    }
}