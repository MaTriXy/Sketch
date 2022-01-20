package com.github.panpf.sketch.request

import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.internal.ImageOptions
import com.github.panpf.sketch.request.internal.ImageRequest

interface DownloadOptions : ImageOptions {

    val httpHeaders: Map<String, String>?   //todo 搞一个专门的 header，因为需要 addHeader 和 setHeader 两种
    val networkContentDiskCachePolicy: CachePolicy?

    override fun isEmpty(): Boolean =
        super.isEmpty() && httpHeaders == null && networkContentDiskCachePolicy == null

    fun newDownloadOptions(
        configBlock: (Builder.() -> Unit)? = null
    ): DownloadOptions = Builder(this).apply {
        configBlock?.invoke(this)
    }.build()

    fun newDownloadOptionsBuilder(
        configBlock: (Builder.() -> Unit)? = null
    ): Builder = Builder(this).apply {
        configBlock?.invoke(this)
    }

    companion object {
        fun new(
            configBlock: (Builder.() -> Unit)? = null
        ): DownloadOptions = Builder().apply {
            configBlock?.invoke(this)
        }.build()

        fun newBuilder(
            configBlock: (Builder.() -> Unit)? = null
        ): Builder = Builder().apply {
            configBlock?.invoke(this)
        }
    }

    open class Builder {

        private var depth: RequestDepth? = null
        private var parametersBuilder: Parameters.Builder? = null

        private var httpHeaders: MutableMap<String, String>? = null
        private var networkContentDiskCachePolicy: CachePolicy? = null

        constructor()

        internal constructor(options: DownloadOptions) {
            this.depth = options.depth
            this.parametersBuilder = options.parameters?.newBuilder()

            this.httpHeaders = options.httpHeaders?.toMutableMap()
            this.networkContentDiskCachePolicy = options.networkContentDiskCachePolicy
        }

        fun depth(depth: RequestDepth?): Builder = apply {
            this.depth = depth
        }

        fun depthFrom(from: String?): Builder = apply {
            if (from != null) {
                setParameter(ImageRequest.REQUEST_DEPTH_FROM, from, null)
            } else {
                removeParameter(ImageRequest.REQUEST_DEPTH_FROM)
            }
        }

        fun parameters(parameters: Parameters?): Builder = apply {
            this.parametersBuilder = parameters?.newBuilder()
        }

        /**
         * Set a parameter for this request.
         *
         * @see Parameters.Builder.set
         */
        @JvmOverloads
        fun setParameter(key: String, value: Any?, cacheKey: String? = value?.toString()): Builder =
            apply {
                this.parametersBuilder = (this.parametersBuilder ?: Parameters.Builder()).apply {
                    set(key, value, cacheKey)
                }
            }

        /**
         * Remove a parameter from this request.
         *
         * @see Parameters.Builder.remove
         */
        fun removeParameter(key: String): Builder = apply {
            this.parametersBuilder?.remove(key)
        }

        fun httpHeaders(httpHeaders: Map<String, String>?): Builder = apply {
            this.httpHeaders = httpHeaders?.toMutableMap()
        }

        /**
         * Add a header for any network operations performed by this request.
         */
        fun addHttpHeader(name: String, value: String): Builder = apply {
            this.httpHeaders = (this.httpHeaders ?: HashMap()).apply {
                put(name, value)
            }
        }

        /**
         * Set a header for any network operations performed by this request.
         */
        fun setHttpHeader(name: String, value: String): Builder = apply {
            this.httpHeaders = (this.httpHeaders ?: HashMap()).apply {
                set(name, value)
            }
        }

        /**
         * Remove all network headers with the key [name].
         */
        fun removeHttpHeader(name: String): Builder = apply {
            this.httpHeaders?.remove(name)
        }

        fun networkContentDiskCachePolicy(networkContentDiskCachePolicy: CachePolicy?): Builder =
            apply {
                this.networkContentDiskCachePolicy = networkContentDiskCachePolicy
            }

        fun build(): DownloadOptions = DownloadOptionsImpl(
            depth = depth,
            parameters = parametersBuilder?.build(),
            httpHeaders = httpHeaders?.toMap(),
            networkContentDiskCachePolicy = networkContentDiskCachePolicy,
        )
    }

    private class DownloadOptionsImpl(
        override val depth: RequestDepth?,
        override val parameters: Parameters?,
        override val httpHeaders: Map<String, String>?,
        override val networkContentDiskCachePolicy: CachePolicy?,
    ) : DownloadOptions
}