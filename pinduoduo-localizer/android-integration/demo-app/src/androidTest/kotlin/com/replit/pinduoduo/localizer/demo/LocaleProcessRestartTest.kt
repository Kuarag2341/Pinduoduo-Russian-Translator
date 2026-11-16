package com.replit.pinduoduo.localizer.demo

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.replit.pinduoduo.localizer.android.AndroidLocalization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run this test in separate instrumentation invocations with phase=write and
 * phase=verify, force-stopping the demo app between invocations.
 */
@RunWith(AndroidJUnit4::class)
class LocaleProcessRestartTest {
    @Test
    fun russianSelectionSurvivesAppProcessRestart() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences(
            "pinduoduo_localizer_locale",
            Context.MODE_PRIVATE,
        )

        when (InstrumentationRegistry.getArguments().getString("phase")) {
            "write" -> {
                preferences.edit().clear().commit()
                val localization = AndroidLocalization(context)

                assertEquals("zh", localization.currentLanguage())
                assertTrue(localization.selectLanguage("ru-RU"))
                assertEquals("ru", localization.currentLanguage())
                assertEquals("ru", preferences.getString("selected_language", null))
            }

            "verify" -> {
                val localization = AndroidLocalization(context)

                assertEquals("ru", localization.currentLanguage())
                assertEquals("Главная", localization.translate("home.tab.home", "首页"))
            }

            else -> error("Set instrumentation argument phase to write or verify")
        }
    }
}
