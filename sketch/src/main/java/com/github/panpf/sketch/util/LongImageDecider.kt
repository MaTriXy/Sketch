package com.github.panpf.sketch.util

class LongImageDecider(
    val minDifferenceOfAspectRatio: Float = DEFAULT_MIN_DIFFERENCE_OF_ASPECT_RATIO
) {

    companion object {
        const val DEFAULT_MIN_DIFFERENCE_OF_ASPECT_RATIO: Float = 3f
    }

    fun isLongImage(
        imageWidth: Int, imageHeight: Int, resizeWidth: Int, resizeHeight: Int
    ): Boolean {
        val imageAspectRatio = imageWidth.toFloat().div(imageHeight).format(1)
        val resizeAspectRatio = resizeWidth.toFloat().div(resizeHeight).format(1)
        val maxAspectRatio = resizeAspectRatio.coerceAtLeast(imageAspectRatio)
        val minAspectRatio = resizeAspectRatio.coerceAtMost(imageAspectRatio)
        return maxAspectRatio >= (minAspectRatio * minDifferenceOfAspectRatio)
    }

    override fun toString(): String =
        "LongImageDecider($minDifferenceOfAspectRatio)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongImageDecider

        if (minDifferenceOfAspectRatio != other.minDifferenceOfAspectRatio) return false

        return true
    }

    override fun hashCode(): Int {
        return minDifferenceOfAspectRatio.hashCode()
    }
}