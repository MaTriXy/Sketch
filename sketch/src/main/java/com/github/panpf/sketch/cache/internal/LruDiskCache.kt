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
package com.github.panpf.sketch.cache.internal

import android.content.Context
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.cache.DiskCache.Editor
import com.github.panpf.sketch.cache.DiskCache.Snapshot
import com.github.panpf.sketch.util.DiskLruCache
import com.github.panpf.sketch.util.Logger
import com.github.panpf.sketch.util.fileNameCompatibilityMultiProcess
import com.github.panpf.sketch.util.format
import com.github.panpf.sketch.util.formatFileSize
import com.github.panpf.sketch.util.intMerged
import com.github.panpf.sketch.util.md5
import kotlinx.coroutines.sync.Mutex
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A disk cache that manages the cache according to a least-used rule
 */
class LruDiskCache private constructor(
    context: Context,
    override val maxSize: Long,
    directory: File,
    /* Version, used to delete the old cache, update this value when you want to actively delete the old cache */
    val appVersion: Int,
    /* Controlled by sketch, sketch fixes the wrong disk cache generated by the old version and will modify this version to update the cache */
    val internalVersion: Int,
) : DiskCache {

    companion object {
        private const val MODULE = "LruDiskCache"

        @JvmStatic
        private val editLockLock = Any()
    }

    private var _cache: DiskLruCache? = null
    private val keyMapperCache = KeyMapperCache { md5(it) }
    private val editLockMap: MutableMap<String, Mutex> = WeakHashMap()
    private val getCount = AtomicInteger()
    private val hitCount = AtomicInteger()

    override var logger: Logger? = null
    override val size: Long
        get() = _cache?.size() ?: 0
    override val directory: File by lazy {
        fileNameCompatibilityMultiProcess(context, directory)
    }

    private fun cache(): DiskLruCache = synchronized(this) {
        _cache ?: openDiskLruCache().apply {
            this@LruDiskCache._cache = this
        }
    }

    private fun openDiskLruCache(): DiskLruCache {
        directory.apply {
            if (this.exists()) {
                val journalFile = File(this, DiskLruCache.JOURNAL_FILE)
                if (!journalFile.exists()) {
                    this.deleteRecursively()
                    this.mkdirs()
                }
            } else {
                this.mkdirs()
            }
        }
        val unionVersion = intMerged(appVersion, internalVersion)
        return DiskLruCache.open(directory, unionVersion, 1, maxSize)
    }

    override fun edit(key: String): Editor? {
        val cache = cache()
        val encodedKey = keyMapperCache.mapKey(key)
        var diskEditor: DiskLruCache.Editor? = null
        try {
            diskEditor = cache.edit(encodedKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return diskEditor?.let {
            MyEditor(
                key = key,
                cache = cache,
                editor = it,
                logger = logger
            )
        }
    }

    override fun remove(key: String): Boolean {
        val cache = cache()
        val encodedKey = keyMapperCache.mapKey(key)
        return try {
            cache.remove(encodedKey)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun get(key: String): Snapshot? {
        val cache = cache()
        val encodedKey = keyMapperCache.mapKey(key)
        var snapshot: DiskLruCache.Snapshot? = null
        try {
            snapshot = cache.get(encodedKey)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return (snapshot?.let { MySnapshot(key, encodedKey, cache, it, logger) }).apply {
            val getCount1 = getCount.addAndGet(1)
            val hitCount1 = if (this != null) {
                hitCount.addAndGet(1)
            } else {
                hitCount.get()
            }
            if (getCount1 == Int.MAX_VALUE || hitCount1 == Int.MAX_VALUE) {
                getCount.set(0)
                hitCount.set(0)
            }
            logger?.d(MODULE) {
                val hitRatio = (hitCount1.toFloat() / getCount1).format(2)
                val state = if (this != null) "hit" else "miss"
                "get. $state. hitRate ${hitRatio}. $key"
            }
        }
    }

    override fun exist(key: String): Boolean {
        val cache = cache()
        val encodedKey = keyMapperCache.mapKey(key)
        return try {
            cache.exist(encodedKey)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun clear() {
        val oldSize = size
        val cache = this._cache
        if (cache != null) {
            try {
                cache.delete()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            this._cache = null
        } else {
            DiskLruCache.Util.deleteContents(directory)
        }
        logger?.w(MODULE, "clear. cleared ${oldSize.formatFileSize()}")
    }

    override fun editLock(key: String): Mutex = synchronized(editLockLock) {
        val encodedKey = keyMapperCache.mapKey(key)
        editLockMap[encodedKey] ?: Mutex().apply {
            this@LruDiskCache.editLockMap[encodedKey] = this
        }
    }

    /**
     * It can still be used after closing, and will reopen a new DiskLruCache
     */
    override fun close() {
        try {
            _cache?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _cache = null
    }

    override fun toString(): String =
        "$MODULE(maxSize=${maxSize.formatFileSize()},appVersion=${appVersion},internalVersion=${internalVersion},directory='${directory.path}')"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LruDiskCache) return false

        if (maxSize != other.maxSize) return false
        if (directory != other.directory) return false
        if (appVersion != other.appVersion) return false
        if (internalVersion != other.internalVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxSize.hashCode()
        result = 31 * result + directory.hashCode()
        result = 31 * result + appVersion
        result = 31 * result + internalVersion
        return result
    }

    class MySnapshot constructor(
        override val key: String,
        private val encodedKey: String,
        private val cache: DiskLruCache,
        private val snapshot: DiskLruCache.Snapshot,
        private val logger: Logger?,
    ) : Snapshot {

        override val file: File = snapshot.getFile(0)

        @Throws(IOException::class)
        override fun newInputStream(): InputStream = snapshot.getInputStream(0)

        override fun edit(): Editor? = snapshot.edit()?.let {
            MyEditor(key, cache, it, logger)
        }

        override fun remove(): Boolean =
            try {
                cache.remove(encodedKey)
                logger?.d(MODULE) {
                    "delete. size ${cache.size().formatFileSize()}. $key"
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
    }

    class MyEditor(
        private val key: String,
        private val cache: DiskLruCache,
        private val editor: DiskLruCache.Editor,
        private val logger: Logger?,
    ) : Editor {

        @Throws(IOException::class)
        override fun newOutputStream(): OutputStream {
            return editor.newOutputStream(0)
        }

        @Throws(IOException::class)
        override fun commit() {
            editor.commit()
            logger?.d(MODULE) {
                "commit. size ${cache.size().formatFileSize()}. $key"
            }
        }

        override fun abort() {
            try {
                editor.abort()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            logger?.d(MODULE) {
                "abort. size ${cache.size().formatFileSize()}. $key"
            }
        }
    }

    class ForDownloadBuilder(private val context: Context) {

        private var maxSize: Long? = null
        private var directory: File? = null
        private var appVersion: Int? = null

        fun maxSize(maxSize: Long?): ForDownloadBuilder = apply {
            this.maxSize = maxSize
        }

        fun directory(directory: File): ForDownloadBuilder = apply {
            this.directory = directory
        }

        fun appVersion(appVersion: Int?): ForDownloadBuilder = apply {
            this.appVersion = appVersion
        }

        fun build(): LruDiskCache {
            val directory = directory ?: File(
                context.externalCacheDir ?: context.cacheDir,
                DiskCache.DEFAULT_DIR_NAME + File.separator + "download"
            )

            val maxSize = maxSize
            require(maxSize == null || maxSize > 0) { "maxSize must be greater than 0" }
            val finalMaxSize = maxSize ?: (300 * 1024 * 1024)

            require(appVersion == null || appVersion in 1.rangeTo(Short.MAX_VALUE)) {
                "The value range for 'version' is 1 to ${Short.MAX_VALUE}"
            }
            val appVersion = appVersion ?: 1

            return LruDiskCache(
                context = context,
                maxSize = finalMaxSize,
                directory = directory,
                appVersion = appVersion,
                internalVersion = 1 // Range from 1 to Short.MAX_VALUE
            )
        }
    }

    class ForResultBuilder(private val context: Context) {

        private var maxSize: Long? = null
        private var directory: File? = null
        private var appVersion: Int? = null

        fun maxSize(maxSize: Long?): ForResultBuilder = apply {
            this.maxSize = maxSize
        }

        fun directory(directory: File): ForResultBuilder = apply {
            this.directory = directory
        }

        fun appVersion(appVersion: Int?): ForResultBuilder = apply {
            this.appVersion = appVersion
        }

        fun build(): LruDiskCache {
            val directory = directory ?: File(
                context.externalCacheDir ?: context.cacheDir,
                DiskCache.DEFAULT_DIR_NAME + File.separator + "result"
            )

            val maxSize = maxSize
            require(maxSize == null || maxSize > 0) { "maxSize must be greater than 0" }
            val finalMaxSize = maxSize ?: (200 * 1024 * 1024)

            require(appVersion == null || appVersion in 1.rangeTo(Short.MAX_VALUE)) {
                "The value range for 'version' is 1 to ${Short.MAX_VALUE}"
            }
            val appVersion = appVersion ?: 1

            return LruDiskCache(
                context = context,
                maxSize = finalMaxSize,
                directory = directory,
                appVersion = appVersion,
                internalVersion = 2 // Range from 1 to Short.MAX_VALUE
            )
        }
    }
}