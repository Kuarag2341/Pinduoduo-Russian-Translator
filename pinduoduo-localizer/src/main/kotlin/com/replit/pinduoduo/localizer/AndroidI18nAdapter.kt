package com.replit.pinduoduo.localizer

/**
 * Platform-neutral seam for a future Android I18n integration.
 * resourceName should be a stable resource key, not a version-specific numeric ID.
 */
class AndroidI18nAdapter(
    private val localization: LocalizationModule
) {
    fun getString(resourceName: String, originalText: String): String =
        localization.translate("native.$resourceName", originalText)
}
