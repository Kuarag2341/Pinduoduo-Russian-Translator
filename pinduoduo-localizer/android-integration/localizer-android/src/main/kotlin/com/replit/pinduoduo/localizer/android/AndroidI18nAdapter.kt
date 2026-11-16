package com.replit.pinduoduo.localizer.android

import android.content.Context

/**
 * Adapter for an authorized host application's Android I18n call sites.
 * The host must supply its original Chinese text or a host-owned string resource.
 */
class AndroidI18nAdapter(
    private val localization: AndroidLocalization
) {
    fun getString(resourceKey: String, originalText: String): String =
        localization.translate(resourceKey, originalText)

    fun getString(
        context: Context,
        resourceKey: String,
        originalStringResourceId: Int
    ): String {
        val originalText = try {
            context.getString(originalStringResourceId)
        } catch (_: android.content.res.Resources.NotFoundException) {
            ""
        }
        return localization.translate(resourceKey, originalText)
    }
}
