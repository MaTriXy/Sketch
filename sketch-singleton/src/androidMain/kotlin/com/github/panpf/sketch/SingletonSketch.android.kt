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

package com.github.panpf.sketch

import android.content.Context
import android.view.View

/**
 * Get the factory of [Sketch] from Application, only supports Android platform
 *
 * @see com.github.panpf.sketch.singleton.android.test.SingletonSketchAndroidTest.testApplicationSketchFactory
 */
internal actual fun PlatformContext.applicationSketchFactory(): SingletonSketch.Factory? {
    return applicationContext as? SingletonSketch.Factory
}

/**
 * Get the singleton [Sketch] from Context.
 *
 * @see com.github.panpf.sketch.singleton.android.test.SingletonSketchAndroidTest.testContextSketch
 */
actual val Context.sketch: Sketch
    get() = SingletonSketch.get(this)

/**
 * Get the singleton [Sketch] from View.
 *
 * @see com.github.panpf.sketch.singleton.android.test.SingletonSketchAndroidTest.testViewSketch
 */
val View.sketch: Sketch
    get() = SingletonSketch.get(context)
