package com.replit.pinduoduo.localizer.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import com.replit.pinduoduo.localizer.android.AndroidLocalization

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val localization = createDemoLocalization(applicationContext)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                DemoShopApp(localization)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DemoShopApp(localization: AndroidLocalization) {
    DemoShopScreen(localization)
}
