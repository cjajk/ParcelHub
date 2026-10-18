package com.cjajk.parcelhub.util

import android.content.Context

/** 快递100密钥本地存储：由用户在 App 内自行填写，不再硬编码 */
object ExpressKeyStore {
    private const val SP = "parcel_hub"
    private const val KEY_CUSTOMER = "kd100_customer"
    private const val KEY_KEY = "kd100_key"

    fun getCustomer(context: Context): String =
        context.getSharedPreferences(SP, Context.MODE_PRIVATE).getString(KEY_CUSTOMER, "") ?: ""

    fun getKey(context: Context): String =
        context.getSharedPreferences(SP, Context.MODE_PRIVATE).getString(KEY_KEY, "") ?: ""

    fun save(context: Context, customer: String, key: String) {
        context.getSharedPreferences(SP, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOMER, customer.trim())
            .putString(KEY_KEY, key.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean =
        getCustomer(context).isNotBlank() && getKey(context).isNotBlank()
}
