package com.github.panpf.sketch.fetch

import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.datasource.AssetsDataSource
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.internal.ImageRequest

/**
 * Support 'asset://test.png' uri
 */
class AssetUriFetcher(
    val sketch: Sketch,
    val request: LoadRequest,
    val assetFileName: String
) : Fetcher {

    companion object {
        const val SCHEME = "asset"

        @JvmStatic
        fun makeUri(assetFilePath: String): String = "$SCHEME://$assetFilePath"
    }

    override suspend fun fetch(): FetchResult =
        FetchResult(AssetsDataSource(sketch.appContext, assetFileName))

    class Factory : Fetcher.Factory {
        override fun create(sketch: Sketch, request: ImageRequest): AssetUriFetcher? =
            if (request is LoadRequest && request.uri.scheme == SCHEME) {
                val assetFileName = request.uriString.substring(("$SCHEME://").length)
                AssetUriFetcher(sketch, request, assetFileName)
            } else {
                null
            }
    }
}