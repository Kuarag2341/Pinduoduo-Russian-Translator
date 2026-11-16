package com.replit.pinduoduo.localizer

enum class DictionarySection(val fileName: String) {
    HOME("home"),
    SEARCH("search"),
    CATEGORIES("categories"),
    PRODUCTS("products"),
    ATTRIBUTES("attributes"),
    CART("cart"),
    FAVORITES("favorites"),
    PROFILE("profile"),
    ORDERS("orders"),
    DELIVERY("delivery"),
    SYSTEM("system");

    companion object {
        fun fromKey(key: String): DictionarySection? {
            val prefix = key.substringBefore('.', missingDelimiterValue = "")
            return entries.firstOrNull { it.fileName == prefix }
        }
    }
}
