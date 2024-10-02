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
import com.github.panpf.sketch.state.rememberPainterStateImage
import org.jetbrains.compose.resources.DrawableResource

/**
 * Set Drawable placeholder image when loading
 *
 * @see com.github.panpf.sketch.compose.resources.common.test.request.ImageOptionsComposeResourcesTest.testPlaceholder
 */
@Composable
fun ImageOptions.Builder.placeholder(resource: DrawableResource): ImageOptions.Builder =
    placeholder(rememberPainterStateImage(resource))

/**
 * Set Drawable placeholder image when uri is invalid
 *
 * @see com.github.panpf.sketch.compose.resources.common.test.request.ImageOptionsComposeResourcesTest.testFallback
 */
@Composable
fun ImageOptions.Builder.fallback(resource: DrawableResource): ImageOptions.Builder =
    fallback(rememberPainterStateImage(resource))

/**
 * Set Color image to display when loading fails.
 *
 * You can also set image of different error types via the trailing lambda function
 *
 * @see com.github.panpf.sketch.compose.resources.common.test.request.ImageOptionsComposeResourcesTest.testError
 */
@Composable
fun ImageOptions.Builder.error(resource: DrawableResource): ImageOptions.Builder =
    error(rememberPainterStateImage(resource))