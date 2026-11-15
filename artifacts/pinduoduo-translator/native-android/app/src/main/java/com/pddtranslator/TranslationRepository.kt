package com.pddtranslator

import android.content.Context
import android.os.SystemClock
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.security.MessageDigest

/**
 * Chinese → Russian text is translated on-device after ML Kit downloads its language model.
 * Only individual page text nodes reach this repository; full pages and product images do not.
 */
class TranslationRepository(context: Context) {
    private val preferences = context.getSharedPreferences("translation_cache", Context.MODE_PRIVATE)
    private val translator: Translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CHINESE)
            .setTargetLanguage(TranslateLanguage.RUSSIAN)
            .build(),
    )
    @Volatile private var modelReady = false
    @Volatile private var modelDownloadInProgress = false
    @Volatile private var retryModelDownloadAfter = 0L
    private val pendingRequests = mutableListOf<Pair<String, (Result<String>) -> Unit>>()

    fun translate(text: String, onResult: (Result<String>) -> Unit) {
        val source = text.trim()
        if (source.isEmpty()) {
            onResult(Result.failure(IllegalArgumentException("Пустой текст")))
            return
        }
        val cached = preferences.getString(cacheKey(source), null)
        if (cached != null) {
            onResult(Result.success(cached))
            return
        }
        if (SystemClock.elapsedRealtime() < retryModelDownloadAfter) {
            onResult(Result.failure(IllegalStateException(
                "Нет доступной языковой модели. Проверьте интернет и попробуйте позже.",
            )))
            return
        }

        val translateNow = synchronized(this) {
            if (modelReady) {
                true
            } else {
                pendingRequests.add(source to onResult)
                if (modelDownloadInProgress) return
                modelDownloadInProgress = true
                false
            }
        }
        if (translateNow) {
            translateWithModel(source, onResult)
            return
        }

        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                val queued = synchronized(this) {
                    modelReady = true
                    modelDownloadInProgress = false
                    retryModelDownloadAfter = 0L
                    pendingRequests.toList().also { pendingRequests.clear() }
                }
                queued.forEach { (pendingText, callback) -> translateWithModel(pendingText, callback) }
            }
            .addOnFailureListener { error ->
                val queued = synchronized(this) {
                    modelDownloadInProgress = false
                    retryModelDownloadAfter = SystemClock.elapsedRealtime() + RETRY_DELAY_MS
                    pendingRequests.toList().also { pendingRequests.clear() }
                }
                val failure = Result.failure<String>(IllegalStateException(
                    "Не удалось загрузить модель перевода. Проверьте интернет и повторите попытку.",
                    error,
                ))
                queued.forEach { (_, callback) -> callback(failure) }
            }
    }

    /**
     * Translate readable text while preserving prices, numbers, currencies, and common
     * specification values verbatim. Only Chinese text outside those tokens is translated.
     */
    fun translatePreservingValues(text: String, onResult: (Result<String>) -> Unit) {
        val parts = mutableListOf<TranslationPart>()
        var cursor = 0
        VALUE_TOKEN.findAll(text).forEach { match ->
            if (match.range.first > cursor) {
                parts += TranslationPart(text.substring(cursor, match.range.first), isValue = false)
            }
            parts += TranslationPart(match.value, isValue = true)
            cursor = match.range.last + 1
        }
        if (cursor < text.length) parts += TranslationPart(text.substring(cursor), isValue = false)
        if (parts.isEmpty()) {
            onResult(Result.success(text))
            return
        }

        val translatedParts = parts.map { it.text }.toMutableList()
        fun translatePart(index: Int) {
            if (index >= parts.size) {
                onResult(Result.success(translatedParts.joinToString(separator = "")))
                return
            }
            val part = parts[index]
            if (part.isValue || part.text.none { it.code in CHINESE_RANGE }) {
                translatePart(index + 1)
                return
            }
            val firstContent = part.text.indexOfFirst { !it.isWhitespace() }
            val lastContent = part.text.indexOfLast { !it.isWhitespace() }
            if (firstContent < 0 || lastContent < firstContent) {
                translatePart(index + 1)
                return
            }
            val leadingWhitespace = part.text.substring(0, firstContent)
            val content = part.text.substring(firstContent, lastContent + 1)
            val trailingWhitespace = part.text.substring(lastContent + 1)
            translate(content) { result ->
                result.onSuccess { translatedParts[index] = leadingWhitespace + it + trailingWhitespace }
                    .onFailure { onResult(Result.failure(it)) }
                if (result.isSuccess) translatePart(index + 1)
            }
        }
        translatePart(0)
    }

    private fun translateWithModel(text: String, onResult: (Result<String>) -> Unit) {
        translator.translate(text)
            .addOnSuccessListener { translated ->
                remember(text, translated)
                onResult(Result.success(translated))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(IllegalStateException("Не удалось перевести текст.", error)))
            }
    }

    private fun remember(source: String, translated: String) {
        val all = preferences.all
        if (all.size >= MAX_CACHE_ENTRIES) {
            val editor = preferences.edit()
            all.keys.take(all.size - MAX_CACHE_ENTRIES + 1).forEach(editor::remove)
            editor.apply()
        }
        preferences.edit().putString(cacheKey(source), translated).apply()
    }

    private fun cacheKey(text: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return "zh_ru_$hash"
    }

    fun close() = translator.close()

    private companion object {
        const val MAX_CACHE_ENTRIES = 1000
        const val RETRY_DELAY_MS = 30_000L
        val CHINESE_RANGE = 0x3400..0x9FFF
        val VALUE_TOKEN = Regex(
            """(?:\p{Sc}\s*)?\d[\d.,]*(?:\s*(?:元|人民币|块|件|个|ml|g|kg|cm|mm|m|码|号|寸|GB|TB|小时|天|月|年|折|券|套|包|片|瓶|盒|条|只|厘米|公斤|升|度))?|(?:\p{Sc}\s*)?[零〇一二两三四五六七八九十百千万亿点]+(?:\s*(?:元|人民币|块|角|分|折|件|个|码|号|寸|套|包|片|瓶|盒|条|只))""",
            RegexOption.IGNORE_CASE,
        )
    }

    private data class TranslationPart(val text: String, val isValue: Boolean)
}
