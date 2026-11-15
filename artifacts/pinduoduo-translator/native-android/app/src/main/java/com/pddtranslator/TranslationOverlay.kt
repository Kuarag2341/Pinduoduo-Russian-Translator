package com.pddtranslator

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import kotlin.math.roundToInt

data class TranslatedRegion(
    val text: String,
    val bounds: Rect,
    val patchColor: Int? = null,
)

/**
 * Screen text is a touch-through window. It never receives input, so taps and gestures
 * continue to reach the original app below it.
 */
class TranslationOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val visibleViews = mutableListOf<TextView>()

    fun show(regions: List<TranslatedRegion>) {
        clear()
        val settings = context.getSharedPreferences(ScreenTranslationService.PREFS, Context.MODE_PRIVATE)
        val textSize = settings.getFloat(ScreenTranslationService.KEY_TEXT_SIZE, 15f)
        val textColor = settings.getInt(ScreenTranslationService.KEY_TEXT_COLOR, Color.WHITE)

        regions.forEach { region ->
            if (region.text.isBlank() || region.bounds.width() <= 0 || region.bounds.height() <= 0) return@forEach
            val label = TextView(context).apply {
                text = region.text
                val background = region.patchColor
                val selectedTextColor = if (background != null &&
                    kotlin.math.abs(Color.luminance(background) - Color.luminance(textColor)) < MIN_TEXT_CONTRAST
                ) {
                    if (Color.luminance(background) > 0.5f) Color.BLACK else Color.WHITE
                } else {
                    textColor
                }
                setTextColor(selectedTextColor)
                this.textSize = textSize
                gravity = Gravity.CENTER
                maxLines = MAX_TRANSLATED_LINES
                setHorizontallyScrolling(false)
                includeFontPadding = false
                setPadding(dp(1), 0, dp(1), 0)
                setBackgroundColor(background ?: Color.argb(218, 26, 29, 32))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    breakStrategy = android.text.Layout.BREAK_STRATEGY_SIMPLE
                }
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this,
                    MIN_TEXT_SIZE_SP,
                    textSize.roundToInt().coerceAtLeast(MIN_TEXT_SIZE_SP + 1),
                    AUTO_SIZE_STEP_SP,
                    android.util.TypedValue.COMPLEX_UNIT_SP,
                )
            }
            val params = WindowManager.LayoutParams(
                region.bounds.width(),
                region.bounds.height(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = region.bounds.left
                y = region.bounds.top
            }
            try {
                windowManager.addView(label, params)
                visibleViews.add(label)
            } catch (_: WindowManager.BadTokenException) {
                // The user may have revoked overlay access while the service was running.
            } catch (_: IllegalStateException) {
                // A display may be changing size during device rotation.
            }
        }
    }

    fun clear() {
        visibleViews.forEach { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // Already detached.
            }
        }
        visibleViews.clear()
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val MIN_TEXT_SIZE_SP = 7
        const val AUTO_SIZE_STEP_SP = 1
        const val MAX_TRANSLATED_LINES = 3
        const val MIN_TEXT_CONTRAST = 0.42f
    }
}
