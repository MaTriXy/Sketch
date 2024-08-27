package com.github.panpf.sketch.sample.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.panpf.sketch.LocalPlatformContext
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.fetch.FileUriFetcher
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.sample.EventBus
import com.github.panpf.sketch.sample.util.sha256String
import com.github.panpf.sketch.util.MimeTypeMap
import com.github.panpf.zoomimage.SketchZoomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.File

@Composable
actual fun PhotoViewerBottomBarWrapper(
    imageUri: String,
    modifier: Modifier,
    zoomState: SketchZoomState,
    buttonBackgroundColor: Color?,
    buttonContentColor: Color?,
    onInfoClick: (() -> Unit)?,
) {
    val context = LocalPlatformContext.current
    val coroutineScope = rememberCoroutineScope()
    PhotoViewerBottomBar(
        modifier = modifier,
        zoomState = zoomState,
        buttonBackgroundColor = buttonBackgroundColor,
        buttonContentColor = buttonContentColor,
        onInfoClick = onInfoClick,
        onShareClick = {
            coroutineScope.launch {
                sharePhoto(context, imageUri)
            }
        },
        onSaveClick = {
            coroutineScope.launch {
                savePhoto(context, imageUri)
            }
        },
    )
}

private suspend fun savePhoto(context: PlatformContext, imageUri: String) {
    val sketch: Sketch = SingletonSketch.get(context)
    val fetcher = withContext(Dispatchers.IO) {
        sketch.components.newFetcherOrThrow(ImageRequest(sketch.context, imageUri))
    }
    if (fetcher is FileUriFetcher) {
        return EventBus.toastFlow.emit("Local files do not need to be saved")
    }

    val fetchResult = withContext(Dispatchers.IO) {
        fetcher.fetch()
    }.let {
        it.getOrNull()
            ?: return EventBus.toastFlow.emit("Failed to save picture: ${it.exceptionOrNull()!!.message}")
    }
    val userHomeDir = File(System.getProperty("user.home"))
    val userPicturesDir = File(userHomeDir, "Pictures")
    val outDir = File(userPicturesDir, "sketch4").apply { mkdirs() }
    val fileExtension = MimeTypeMap.getExtensionFromUrl(imageUri)
        ?: MimeTypeMap.getExtensionFromMimeType(fetchResult.mimeType ?: "")
        ?: "jpeg"
    val imageFile = File(outDir, "${imageUri.sha256String()}.$fileExtension")
    val result = withContext(Dispatchers.IO) {
        runCatching {
            fetchResult.dataSource.openSource().buffer().use { input ->
                imageFile.outputStream().sink().buffer().use { output ->
                    output.writeAll(input)
                }
            }
        }
    }
    return if (result.isSuccess) {
        EventBus.toastFlow.emit("Saved to the '${imageFile.parentFile?.path}' directory")
    } else {
        val exception = result.exceptionOrNull()
        EventBus.toastFlow.emit("Failed to save picture: ${exception?.message}")
    }
}

@Suppress("UNUSED_PARAMETER")
private suspend fun sharePhoto(context: PlatformContext, imageUri: String) {
    EventBus.toastFlow.emit("Desktop platform does not support sharing photo")
}