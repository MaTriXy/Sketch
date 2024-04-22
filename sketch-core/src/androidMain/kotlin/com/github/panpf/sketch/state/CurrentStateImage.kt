/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.state

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.github.panpf.sketch.Image
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.util.DrawableEqualizer

fun CurrentStateImage(defaultImage: StateImage? = null): CurrentStateImage =
    CurrentStateImageImpl(defaultImage)

fun CurrentStateImage(defaultDrawable: DrawableEqualizer): CurrentStateImage =
    CurrentStateImageImpl(DrawableStateImage(defaultDrawable))

fun CurrentStateImage(@DrawableRes defaultDrawableRes: Int): CurrentStateImage =
    CurrentStateImageImpl(DrawableStateImage(defaultDrawableRes))

/**
 * Use current [Drawable] as the state [Drawable]
 */
interface CurrentStateImage : StateImage {
    val defaultImage: StateImage?
}

private class CurrentStateImageImpl(
    override val defaultImage: StateImage? = null
) : CurrentStateImage {

    override fun getImage(
        sketch: Sketch,
        request: ImageRequest,
        throwable: Throwable?
    ): Image? = request.target?.currentImage ?: defaultImage?.getImage(sketch, request, throwable)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrentStateImage) return false
        if (defaultImage != other.defaultImage) return false
        return true
    }

    override fun hashCode(): Int {
        return defaultImage?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "CurrentStateImage($defaultImage)"
    }
}