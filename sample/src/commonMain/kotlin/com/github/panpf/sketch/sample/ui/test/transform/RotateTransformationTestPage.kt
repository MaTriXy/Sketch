package com.github.panpf.sketch.sample.ui.test.transform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.cache.CachePolicy.DISABLED
import com.github.panpf.sketch.decode.BitmapConfig
import com.github.panpf.sketch.images.ResourceImages
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.sample.ui.components.MyAsyncImage
import com.github.panpf.sketch.sample.ui.setting.platformBitmapConfigs
import com.github.panpf.sketch.transform.RotateTransformation
import kotlinx.collections.immutable.toImmutableList

@Composable
fun RotateTransformationTestPage() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        val rotateDegreesState = remember { mutableStateOf(45) }

        val bitmapConfigValues =
            remember { listOf("Default").plus(platformBitmapConfigs()).toImmutableList() }
        val bitmapConfigState = remember { mutableStateOf("Default") }
        val bitmapConfig = remember(bitmapConfigState.value) {
            bitmapConfigState.value.takeIf { it != "Default" }?.let { BitmapConfig(it) }
        }

        MyAsyncImage(
            request = ComposableImageRequest(ResourceImages.jpeg.uri) {
                memoryCachePolicy(DISABLED)
                resultCachePolicy(DISABLED)
                addTransformations(RotateTransformation(rotateDegreesState.value))
                bitmapConfig(bitmapConfig)
            },
            contentDescription = "image",
            modifier = Modifier.fillMaxWidth().weight(1f)
        )

        Spacer(Modifier.size(16.dp))

        sliderListItem(
            title = "Rotate Angle",
            state = rotateDegreesState,
            minValue = 0,
            maxValue = 360
        )

        singleChoiceListItem(
            title = "Bitmap Config",
            values = bitmapConfigValues,
            state = bitmapConfigState
        )
    }
}