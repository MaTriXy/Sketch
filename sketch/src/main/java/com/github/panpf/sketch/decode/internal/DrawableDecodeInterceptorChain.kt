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
package com.github.panpf.sketch.decode.internal

import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.decode.DrawableDecodeInterceptor
import com.github.panpf.sketch.decode.DrawableDecodeResult
import com.github.panpf.sketch.fetch.FetchResult
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.internal.RequestContext
import com.github.panpf.sketch.util.requiredWorkThread

internal class DrawableDecodeInterceptorChain constructor(
    override val sketch: Sketch,
    override val request: ImageRequest,
    override val requestContext: RequestContext,
    override val fetchResult: FetchResult?,
    private val interceptors: List<DrawableDecodeInterceptor>,
    private val index: Int,
) : DrawableDecodeInterceptor.Chain {

    @WorkerThread
    override suspend fun proceed(): Result<DrawableDecodeResult> {
        requiredWorkThread()
        val interceptor = interceptors[index]
        val next = DrawableDecodeInterceptorChain(
            sketch, request, requestContext, fetchResult, interceptors, index + 1
        )
        return interceptor.intercept(next)
    }
}
