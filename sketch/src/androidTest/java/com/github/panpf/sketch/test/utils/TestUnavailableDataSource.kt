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
package com.github.panpf.sketch.test.utils

import androidx.annotation.WorkerThread
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.datasource.UnavailableDataSource
import com.github.panpf.sketch.request.ImageRequest
import java.io.IOException
import java.io.InputStream

class TestUnavailableDataSource(
    override val sketch: Sketch,
    override val request: ImageRequest,
    override val dataFrom: DataFrom,
) : UnavailableDataSource {

    @WorkerThread
    @Throws(IOException::class)
    override fun length(): Long =
        throw UnsupportedOperationException("TestUnavailableDataSource cannot be used")

    @WorkerThread
    @Throws(IOException::class)
    override fun newInputStream(): InputStream =
        throw UnsupportedOperationException("TestUnavailableDataSource cannot be used")

    override fun toString(): String = "TestUnavailableDataSource"
}