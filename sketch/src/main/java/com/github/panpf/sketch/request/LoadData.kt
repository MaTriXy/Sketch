package com.github.panpf.sketch.request

import android.graphics.Bitmap
import com.github.panpf.sketch.datasource.DataFrom
import com.github.panpf.sketch.decode.BitmapDecodeResult
import com.github.panpf.sketch.decode.ExifOrientation
import com.github.panpf.sketch.decode.ImageInfo

data class LoadData constructor(
    val bitmap: Bitmap,
    val imageInfo: ImageInfo,
    @ExifOrientation val imageExifOrientation: Int,
    val dataFrom: DataFrom,
    val transformedList: List<String>?,
) : ImageData

fun BitmapDecodeResult.toLoadData(): LoadData =
    LoadData(bitmap, imageInfo, imageExifOrientation, dataFrom, transformedList)