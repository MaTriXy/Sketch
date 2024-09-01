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

package com.github.panpf.sketch.request

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.panpf.sketch.util.screenSize

/**
 * Use window size as resize size
 *
 * @see com.github.panpf.sketch.compose.core.android.test.request.ImageRequestComposeAndroidTest.testSizeWithWindow
 */
@Composable
actual fun ImageRequest.Builder.sizeWithWindow(): ImageRequest.Builder =
    apply {
        val screenSize = LocalContext.current.screenSize()
        size(screenSize)
    }