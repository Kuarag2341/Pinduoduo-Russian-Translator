package com.pddtranslator

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import android.os.SystemClock
import java.security.MessageDigest

/**
 * Chinese → Russian translation stays on this device after the language model is downloaded.
 * Only text is handed to ML Kit; screenshots and bitmaps never leave the app.
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
    }
}
