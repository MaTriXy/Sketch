package com.github.panpf.sketch.test.utils

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import com.github.panpf.sketch.painter.AnimatablePainter
import com.github.panpf.sketch.painter.PainterWrapper

fun Painter.size(size: Size): SizePainter {
    return if (this is AnimatablePainter) {
        SizeAnimatablePainter(this, size)
    } else {
        SizePainter(this, size)
    }
}

open class SizePainter(painter: Painter, val size: Size) : PainterWrapper(painter) {

    override val intrinsicSize: Size = size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SizePainter
        if (painter != other.painter) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        // Because size are value classes, they will be replaced by long.
        // Long will lose precision when converting hashCode, causing the hashCode generated by different srcOffset and srcSize to be the same.
        result = 31 * result + size.toString().hashCode()
        return result
    }

    override fun toString(): String {
        return "SizePainter(painter=$painter, size=$size)"
    }
}

class SizeAnimatablePainter(painter: Painter, size: Size) : SizePainter(painter, size),
    AnimatablePainter {

    private var running = false

    override val intrinsicSize: Size = size

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SizeAnimatablePainter
        if (painter != other.painter) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = painter.hashCode()
        // Because size are value classes, they will be replaced by long.
        // Long will lose precision when converting hashCode, causing the hashCode generated by different srcOffset and srcSize to be the same.
        result = 31 * result + size.toString().hashCode()
        return result
    }

    override fun toString(): String {
        return "SizeAnimatablePainter(painter=$painter, size=$size)"
    }
}