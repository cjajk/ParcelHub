package com.cjajk.parcelhub.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * 平台图标（暂时用灰色占位，后面再补Q版萌娘）
 */
object MascotIcons {

    private fun placeholder(): ImageBitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.parseColor("#FF3A3A3C"))
        return bmp.asImageBitmap()
    }

    val cainiao: ImageBitmap by lazy { placeholder() }
    val taobao: ImageBitmap by lazy { placeholder() }
    val pdd: ImageBitmap by lazy { placeholder() }
    val sf: ImageBitmap by lazy { placeholder() }
    val jd: ImageBitmap by lazy { placeholder() }
}