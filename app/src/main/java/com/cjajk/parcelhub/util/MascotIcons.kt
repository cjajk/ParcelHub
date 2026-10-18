package com.cjajk.parcelhub.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.cjajk.parcelhub.R

/**
 * 平台Q版萌娘图标
 */
object MascotIcons {

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun loadDrawable(@DrawableRes resId: Int): ImageBitmap {
        return try {
            ContextCompat.getDrawable(appContext, resId)
                ?.toBitmap()
                ?.asImageBitmap()
                ?: placeholder()
        } catch (_: Exception) {
            placeholder()
        }
    }

    private fun placeholder(): ImageBitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.parseColor("#FF3A3A3C"))
        return bmp.asImageBitmap()
    }

    val cainiao: ImageBitmap by lazy { loadDrawable(R.drawable.ic_platform_cainiao) }
    val taobao: ImageBitmap by lazy { loadDrawable(R.drawable.ic_platform_taobao) }
    val pdd: ImageBitmap by lazy { loadDrawable(R.drawable.ic_platform_pdd) }
    val sf: ImageBitmap by lazy { loadDrawable(R.drawable.ic_platform_sf) }
    val jd: ImageBitmap by lazy { loadDrawable(R.drawable.ic_platform_jd) }
}