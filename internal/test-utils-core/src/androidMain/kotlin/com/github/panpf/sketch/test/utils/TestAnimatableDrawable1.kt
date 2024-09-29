/*
 * Copyright (C) 2024 panpf <panpfpanpf@outlook.com>
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

package com.github.panpf.sketch.test.utils

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.appcompat.graphics.drawable.DrawableWrapperCompat
import com.github.panpf.sketch.drawable.SketchDrawable
import com.github.panpf.sketch.util.toLogString

class TestAnimatableDrawable1(drawable: Drawable) : DrawableWrapperCompat(drawable), Animatable,
    SketchDrawable {
    private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun mutate(): TestAnimatableDrawable1 {
        val mutateDrawable = drawable?.mutate()
        return if (mutateDrawable != null && mutateDrawable !== drawable) {
            TestAnimatableDrawable1(drawable = mutateDrawable)
        } else {
            this
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TestAnimatableDrawable1
        if (drawable != other.drawable) return false
        return true
    }

    override fun hashCode(): Int {
        return drawable.hashCode()
    }

    override fun toString(): String {
        return "TestAnimatableDrawable1(drawable=${drawable?.toLogString()})"
    }
}