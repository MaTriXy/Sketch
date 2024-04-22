package com.github.panpf.sketch.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.panpf.sketch.util.DrawableEqualizer

@Composable
fun rememberDrawableStateImage(drawable: DrawableEqualizer): DrawableStateImage =
    remember(drawable) { DrawableStateImage(drawable) }

@Composable
fun rememberDrawableStateImage(@DrawableRes drawableRes: Int): DrawableStateImage =
    remember(drawableRes) { DrawableStateImage(drawableRes) }
