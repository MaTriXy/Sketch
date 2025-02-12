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

@file:Suppress("RedundantConstructorKeyword")

package com.github.panpf.sketch.fetch

import com.github.panpf.sketch.annotation.WorkerThread
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.request.RequestContext
import com.github.panpf.sketch.request.UriInvalidException
import com.github.panpf.sketch.source.ByteArrayDataSource
import com.github.panpf.sketch.source.DataFrom.MEMORY
import com.github.panpf.sketch.util.Uri
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Create a base64 image uri
 *
 * @return 'data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testNewBase64Uri
 */
fun newBase64Uri(mimeType: String, base64String: String): String =
    "${Base64UriFetcher.SCHEME}:$mimeType;${Base64UriFetcher.BASE64_IDENTIFIER},$base64String"

/**
 * Create a base64 image uri
 *
 * @return 'data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testNewBase64Uri
 */
@OptIn(ExperimentalEncodingApi::class)
fun newBase64Uri(mimeType: String, imageData: ByteArray): String {
    val base64String = Base64.Default.encode(imageData)
    return newBase64Uri(mimeType, base64String)
}

/**
 * Check if the uri is a base64 image uri
 *
 * The following uris are supported
 * * 'data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 * * 'data:img/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testIsBase64Uri
 */
fun isBase64Uri(uri: Uri): Boolean {
    val data = uri.toString()
    return (data.startsWith("${Base64UriFetcher.SCHEME}:image/") || data.startsWith("${Base64UriFetcher.SCHEME}:img/"))
            && data.indexOf(";${Base64UriFetcher.BASE64_IDENTIFIER},") != -1
}

/**
 * Base64 specification
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
enum class Base64Spec {
    Default, Mime, UrlSafe
}

/**
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
const val BASE64_URI_SPEC_KEY = "sketch#base64_uri_spec"

/**
 * Set the specification when decoding base64 uri
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
fun ImageOptions.Builder.base64UriSpec(
    base64Spec: Base64Spec?
): ImageOptions.Builder = apply {
    if (base64Spec != null) {
        setExtra(
            key = BASE64_URI_SPEC_KEY,
            value = base64Spec.name,
            cacheKey = null
        )
    } else {
        removeExtra(BASE64_URI_SPEC_KEY)
    }
}

/**
 * Set the specification when decoding base64 uri
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
fun ImageRequest.Builder.base64UriSpec(
    specification: Base64Spec?
): ImageRequest.Builder = apply {
    if (specification != null) {
        setExtra(
            key = BASE64_URI_SPEC_KEY,
            value = specification.name,
            cacheKey = null
        )
    } else {
        removeExtra(BASE64_URI_SPEC_KEY)
    }
}

/**
 * Get the specification when decoding base64 uri
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
val ImageOptions.base64UriSpec: Base64Spec?
    get() = extras?.value<String>(BASE64_URI_SPEC_KEY)
        ?.let { Base64Spec.valueOf(it) }

/**
 * Get the specification when decoding base64 uri
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest.testBase64UriSpec
 */
val ImageRequest.base64UriSpec: Base64Spec?
    get() = extras?.value<String>(BASE64_URI_SPEC_KEY)
        ?.let { Base64Spec.valueOf(it) }

/**
 * The following uris are supported
 * * 'data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 * * 'data:img/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
 *
 * @see com.github.panpf.sketch.core.common.test.fetch.Base64UriFetcherTest
 */
class Base64UriFetcher constructor(
    val dataEncodedString: String,
    val mimeType: String,
    val spec: Base64Spec? = null,
) : Fetcher {

    companion object {
        const val SCHEME = "data"
        const val BASE64_IDENTIFIER = "base64"
    }

    @WorkerThread
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun fetch(): Result<FetchResult> {
        val base64 = when (spec) {
            Base64Spec.Mime -> Base64.Mime
            Base64Spec.UrlSafe -> Base64.UrlSafe
            else -> Base64.Default
        }
        val bytes = base64.decode(dataEncodedString)
        return Result.success(
            FetchResult(ByteArrayDataSource(data = bytes, dataFrom = MEMORY), mimeType)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Base64UriFetcher
        if (dataEncodedString != other.dataEncodedString) return false
        if (mimeType != other.mimeType) return false
        if (spec != other.spec) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dataEncodedString.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + spec.hashCode()
        return result
    }

    override fun toString(): String {
        return "Base64UriFetcher(data='$dataEncodedString', mimeType='$mimeType', spec=$spec)"
    }

    /**
     * The following uris are supported
     * * 'data:image/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
     * * 'data:img/jpeg;base64,/9j/4QaORX...C8bg/U7T/in//Z'
     */
    class Factory : Fetcher.Factory {

        override fun create(requestContext: RequestContext): Base64UriFetcher? {
            val request = requestContext.request
            val uri = request.uri
            if (!isBase64Uri(uri)) return null
            val uriString = uri.toString()
            val colonSymbolIndex = uriString.indexOf(":").takeIf { it != -1 }
                ?: throw UriInvalidException("Invalid base64 image uri: $uri")
            val semicolonSymbolIndex = uriString.indexOf(";").takeIf { it != -1 }
                ?: throw UriInvalidException("Invalid base64 image uri: $uri")
            val commaSymbolIndex = uriString.indexOf(",").takeIf { it != -1 }
                ?: throw UriInvalidException("Invalid base64 image uri: $uri")
            val mimeType = uriString.substring(colonSymbolIndex + 1, semicolonSymbolIndex)
                .replace("img/", "image/")
            val data = uriString.substring(commaSymbolIndex + 1)
            return Base64UriFetcher(
                dataEncodedString = data,
                mimeType = mimeType,
                spec = request.base64UriSpec
            )
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other != null && this::class == other::class
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String = "Base64UriFetcher"
    }
}