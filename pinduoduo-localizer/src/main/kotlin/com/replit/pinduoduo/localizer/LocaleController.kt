package com.replit.pinduoduo.localizer

import java.util.concurrent.atomic.AtomicReference

class LocaleController(initialTag: String = LocaleCode.CHINESE.tag) {
    private val selected = AtomicReference(
        LocaleCode.fromTag(initialTag) ?: LocaleCode.CHINESE
    )

    fun current(): LocaleCode = selected.get()

    /**
     * Changes the locale only when the tag is supported.
     * Unknown tags leave the current selection unchanged.
     */
    fun select(tag: String): Boolean {
        val parsed = LocaleCode.fromTag(tag) ?: return false
        selected.set(parsed)
        return true
    }
}
