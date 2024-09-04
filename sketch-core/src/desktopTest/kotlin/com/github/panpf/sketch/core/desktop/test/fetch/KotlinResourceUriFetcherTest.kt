package com.github.panpf.sketch.core.desktop.test.fetch

import com.github.panpf.sketch.fetch.KotlinResourceUriFetcher
import com.github.panpf.sketch.fetch.isKotlinResourceUri
import com.github.panpf.sketch.fetch.newKotlinResourceUri
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.test.singleton.getTestContextAndSketch
import com.github.panpf.sketch.test.utils.toRequestContext
import com.github.panpf.sketch.util.Size
import com.github.panpf.sketch.util.toUri
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinResourceUriFetcherTest {

    @Test
    fun testNewKotlinResourceUri() {
        assertEquals(
            expected = "file:///kotlin_resource/sample.jpeg",
            actual = newKotlinResourceUri("sample.jpeg")
        )
        assertEquals(
            expected = "file:///kotlin_resource/images/sample.jpeg",
            actual = newKotlinResourceUri("images/sample.jpeg")
        )
        assertEquals(
            expected = "file:///kotlin_resource/sample.jpeg?from=home",
            actual = newKotlinResourceUri("sample.jpeg?from=home")
        )
    }

    @Test
    fun testIsKotlinResourceUri() {
        assertEquals(
            expected = true,
            actual = isKotlinResourceUri("file:///kotlin_resource/sample.jpeg".toUri())
        )
        assertEquals(
            expected = true,
            actual = isKotlinResourceUri("file:///kotlin_resource/images/sample.jpeg".toUri())
        )
        assertEquals(
            expected = true,
            actual = isKotlinResourceUri("file:///kotlin_resource/sample.jpeg?from=home".toUri())
        )

        assertEquals(
            expected = false,
            actual = isKotlinResourceUri("file1:///kotlin_resource/sample.jpeg".toUri())
        )
        assertEquals(
            expected = false,
            actual = isKotlinResourceUri("file:///kotlin_resource1/sample.jpeg".toUri())
        )
        assertEquals(
            expected = false,
            actual = isKotlinResourceUri("file:///sample.jpeg".toUri())
        )
    }

    @Test
    fun testFactoryCreate() {
        val factory = KotlinResourceUriFetcher.Factory()
        val (context, sketch) = getTestContextAndSketch()

        factory.create(
            ImageRequest(context, "file:///kotlin_resource/sample.jpeg")
                .toRequestContext(sketch, Size.Empty)
        )!!
            .apply {
                assertEquals("sample.jpeg", resourcePath)
            }
        factory.create(
            ImageRequest(context, "file:///kotlin_resource/images/sample.jpeg")
                .toRequestContext(sketch, Size.Empty)
        )!!
            .apply {
                assertEquals("images/sample.jpeg", resourcePath)
            }
        factory.create(
            ImageRequest(context, "file:///kotlin_resource/sample.jpeg?from=home")
                .toRequestContext(sketch, Size.Empty)
        )!!
            .apply {
                assertEquals("sample.jpeg", resourcePath)
            }

        assertEquals(
            expected = null,
            actual = factory.create(
                ImageRequest(context, "file1:///kotlin_resource/sample.jpeg")
                    .toRequestContext(sketch, Size.Empty)
            )
        )
        assertEquals(
            expected = null,
            actual = factory.create(
                ImageRequest(context, "file:///kotlin_resource1/sample.jpeg")
                    .toRequestContext(sketch, Size.Empty)
            )
        )
        assertEquals(
            expected = null,
            actual = factory.create(
                ImageRequest(context, "file:///sample.jpeg")
                    .toRequestContext(sketch, Size.Empty)
            )
        )
    }

    @Test
    fun testFactoryEqualsAndHashCode() {
        val factory1 = KotlinResourceUriFetcher.Factory()
        val factory2 = KotlinResourceUriFetcher.Factory()
        assertEquals(expected = factory1, actual = factory2)
        assertEquals(expected = factory1.hashCode(), actual = factory2.hashCode())
    }

    @Test
    fun testFactoryToString() {
        assertEquals(
            expected = "KotlinResourceUriFetcher",
            actual = KotlinResourceUriFetcher.Factory().toString()
        )
    }

    // TODO test
}