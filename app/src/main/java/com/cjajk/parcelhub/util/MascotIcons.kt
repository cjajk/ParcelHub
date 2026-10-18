package com.cjajk.parcelhub.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

/**
 * 平台 Q 版萌娘图标（96x96 webp），base64 内嵌，解码失败显示灰色占位
 */
object MascotIcons {

    private val CAINIAO_B64 = "UklGRkIFAABXRUJQVlA4IDYFAAAQFgCdASpgAGAAPrVKoEunJCMhq5nbIOAWiWoAyjRykayj9L47Vv30QbbnzH9D7/D775vQ0/HbElODnVxerjbtJCMkB8/c7SwB7gypgErDHts2Fo1lWaZrPKv8XfhGxv99PigoboXas+2H1h7W3W0TVghI9dxRB4qh32YOtnA4FMKZQHC/dmgkLiUMWNCKkkhUibc2g8PNIdFOsY7IzVfmW6LutcgQg/eek30D3S+E931z3bDil3myiAD+8KXX/RruH6OVof+hfWj5Csr2pjgNeX5OacBRkVtilq/qQ+OBzrAk+xvbyae7HZNF2b0iEnVNn0K14NxrAFZDhG4rr0UjqX6PNlW33FFhfKBZUvK2gKS7G5zswbkbZnVymkr5uTIdOMPxRVsWXEkWT3iO8ldA3xP0IDQLcJDs/6mC3aitE7XmUz/3z4XnTMx+xbw5WUDsI5CMsaXwNRbHxX+vYh4jkWif8GcLmwBiPTIC4T23HHKtyUrS30XvqXWmHEm+GZmdLEjaKPP1jOGBB7W5y0wyJv6o0C06OQuKmSbhxIp+vh/JeU6g9kRmw99HmPdqdV9vXlSbN68elepY6EqYeOfmvX9cmj4VzB37dnRktzV26+9maa8poYPz4anzy6r+DyEtuQPbZxgTiViQNhEz9TNkmZPJm6xf8Fp7muUyEzaZ9Gp+WJMWT6BEJ1juoXOk70tXioNqZQpwabCKgp5qy4Y5Bc6+M5TtASHcBx3krWg1F+/Py11OMtp+N1vH1LPbwIY1GNj+89W0pWmet+rt3jGpaFj5cX+lMavx4HfNfUJtsa0RctdBFHSgkFnp/Vu6KZMEFAIyhda/OAq4sU2n5BQ71asELuPjt8/QbNDESlFN7RQwHUmXfXdzkC0ay+5VDfQ31huJ0JehFq17K0PMdeuHjlQZARPf1hG6zCeKjIM/1SnS3XvtVsUPolgJMcmmztORKODoUpdyKuejyTmn+4OHQ0PqkT7cFYz49oODvLN3KHXK7uTWb11OpQk2wiND+lTIphn04g0QpDE6HqVz9iNuuSV+tbMtIOQ6IYyS67PGwSNhVrpQDQMae351jKTYmq3CDTBQ9aZU9NynWtVsOKWqLTbL8xqo6r5J+zsu4TfI8K7ZYtxzFIw6Ym8pxSgpn/euepRorXgenwgy0UG5AjAqGExFSNzh7VEUtHDS1BBrNRQg7n8BQg96FT0K5BAKYfsh6QzmCfvwinBM/lkNOtb3mQJxyDAm282N4LkNtu+edAjVpYbQpy0JOg7M3JpEjndNyjDvlC1hPseWCk+Gu5tushoht6TV6etIX0f04bPwiQVJyZHs7deHXMeTejgHJo92l7fBrTewyJaW1K16czn7Bv5hflQ1bTgtvErzfdWjY31P1PgyhqBHVlY6SRpLEgka4UtpUPB3gQb7WDk8rH6qBwcx0+iSo71aFbwK/CVItgHFTYQdr6fj4r4SRgbB3E3FfVzaOW4y+/3/2v1JzpwFLjWoftKSwuIgxNMGZ191uM0YM4qf4URIC543dmXWfiJQEkNRt0l+2iJfEC3/TxpQ8tv+lW9+iskuda7Wz8h0WYt2tnPLlTLOMxttd/SmtuyBOAdfNdHEA/L9k2gIKM/sE3Spz4vskHQ0ba0hTXY4sPStff6KGHRURiEoLgbjtMUxhPrOfVbr4vK4s4VHJYezje/BSoIt6LQ1rBepZTPtp7V11pUWb5lEgUAh1874qqogPEVf6SEL26eZpZ188ZU5h5mwBuGurs1e2ibHN94APHWFzmAAAA=="

    private fun decode(b64: String): ImageBitmap {
        return try {
            val bytes = Base64.decode(b64, Base64.DEFAULT or Base64.NO_WRAP)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp?.asImageBitmap() ?: placeholder()
        } catch (e: Exception) {
            placeholder()
        }
    }

    private fun placeholder(): ImageBitmap {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.parseColor("#FF3A3A3C"))
        return bmp.asImageBitmap()
    }

    val cainiao: ImageBitmap by lazy { decode(CAINIAO_B64) }
    val taobao: ImageBitmap by lazy { decode(TAOBAO_B64) }
    val pdd: ImageBitmap by lazy { decode(PDD_B64) }
    val sf: ImageBitmap by lazy { decode(SF_B64) }
    val jd: ImageBitmap by lazy { decode(JD_B64) }
}