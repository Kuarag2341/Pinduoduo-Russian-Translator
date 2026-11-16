package com.replit.pinduoduo.localizer

import java.util.Locale

enum class LocaleCode(val tag: String) {
    RUSSIAN("ru"),
    CHINESE("zh");

    companion object {
        /**
         * Accepts language tags such as "ru", "ru-RU", "zh-CN", and "zh_CN".
         * Unsupported or malformed values return null instead of silently
         * changing the user's selection.
         */
        fun fromTag(rawTag: String?): LocaleCode? {
            val language = rawTag
                ?.trim()
                ?.replace('_', '-')
                ?.substringBefore('-')
                ?.lowercase(Locale.ROOT)
                ?.takeIf(String::isNotEmpty)
                ?: return null

            return entries.firstOrNull { it.tag == language }
        }
    }
}
