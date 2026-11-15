package com.pddtranslator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Ink = Color(0xFF202327)
private val Canvas = Color(0xFFF5F2EC)
private val Vermilion = Color(0xFFE8503A)
private val Muted = Color(0xFF797C80)

class MainActivity : ComponentActivity() {
    private var waitingForOverlayPermission = false

    private val overlaySettingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) requestScreenCapture()
            waitingForOverlayPermission = false
        }

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val serviceIntent = Intent(this, ScreenTranslationService::class.java).apply {
                    putExtra(ScreenTranslationService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenTranslationService.EXTRA_RESULT_DATA, result.data)
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
            } else {
                setStatus("Захват экрана отменён")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Vermilion,
                background = Canvas,
                surface = Color.White,
                onBackground = Ink,
                onSurface = Ink,
            )) {
                TranslatorHome(
                    onStart = ::startTranslation,
                    onStop = ::stopTranslation,
                )
            }
        }
    }

    private fun startTranslation() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            overlaySettingsLauncher.launch(intent)
            return
        }
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun stopTranslation() {
        setStatus("Перевод остановлен")
        stopService(Intent(this, ScreenTranslationService::class.java))
    }

    private fun setStatus(message: String) {
        getSharedPreferences(ScreenTranslationService.PREFS, MODE_PRIVATE).edit()
            .putString(ScreenTranslationService.KEY_STATUS, message)
            .apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslatorHome(onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(ScreenTranslationService.PREFS, Context.MODE_PRIVATE) }
    var running by remember { mutableStateOf(prefs.getBoolean(ScreenTranslationService.KEY_RUNNING, false)) }
    var status by remember { mutableStateOf(prefs.getString(ScreenTranslationService.KEY_STATUS, "Готов к работе") ?: "Готов к работе") }
    var textSize by remember { mutableStateOf(prefs.getFloat(ScreenTranslationService.KEY_TEXT_SIZE, 15f)) }
    var selectedColor by remember { mutableIntStateOf(prefs.getInt(ScreenTranslationService.KEY_TEXT_COLOR, android.graphics.Color.WHITE)) }

    LaunchedEffect(Unit) {
        while (true) {
            running = prefs.getBoolean(ScreenTranslationService.KEY_RUNNING, false)
            status = prefs.getString(ScreenTranslationService.KEY_STATUS, "Готов к работе") ?: "Готов к работе"
            delay(700)
        }
    }

    Scaffold(
        containerColor = Canvas,
        floatingActionButton = {
            if (!running) {
                ExtendedFloatingActionButton(
                    onClick = onStart,
                    containerColor = Vermilion,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(Icons.Rounded.ScreenShare, contentDescription = null)
                    Text("Перевести", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Vermilion, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = Color.White)
                }
                Column {
                    Text("Pinduoduo", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Переводчик экрана", fontSize = 13.sp, color = Muted)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Ink),
                shape = RoundedCornerShape(25.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(8.dp).background(if (running) Color(0xFF83D6A1) else Color(0xFFFFB46A), CircleShape))
                        Text(if (running) "ПЕРЕВОД ВКЛЮЧЁН" else "ГОТОВ К ПЕРЕВОДУ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    }
                    Text("Китайский →\nрусский", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(status, fontSize = 13.sp, color = Color(0xFFC9C7C3), lineHeight = 19.sp)
                    if (running) {
                        Button(
                            onClick = onStop,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B3F43), contentColor = Color.White),
                            shape = RoundedCornerShape(15.dp),
                        ) {
                            Icon(Icons.Rounded.StopCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(9.dp))
                            Text("Остановить перевод", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Нажмите плавающую кнопку, чтобы выдать разрешения и начать перевод.", fontSize = 13.sp, color = Color(0xFFCFCECB), lineHeight = 19.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF31876A), modifier = Modifier.size(19.dp))
                        Text("Снимки остаются на устройстве", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Text(
                        "Распознавание и перевод выполняются локально. Интернет может понадобиться только для первой загрузки языковой модели.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Muted,
                    )
                }
            }

            Text("ВНЕШНИЙ ВИД ПЕРЕВОДА", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Размер текста", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("${textSize.toInt()} sp", fontSize = 13.sp, color = Muted)
                    }
                    Slider(
                        value = textSize,
                        onValueChange = { value ->
                            textSize = value
                            prefs.edit().putFloat(ScreenTranslationService.KEY_TEXT_SIZE, value).apply()
                        },
                        valueRange = 11f..24f,
                        steps = 12,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                        Text("Цвет текста", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            android.graphics.Color.WHITE,
                            Color(0xFFFFD36E).toArgb(),
                            Color(0xFFB8F0D2).toArgb(),
                            Color(0xFFFFA9A0).toArgb(),
                            Color(0xFFB9D9FF).toArgb(),
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(29.dp)
                                    .background(Color(color), CircleShape)
                                    .clickable {
                                        selectedColor = color
                                        prefs.edit().putInt(ScreenTranslationService.KEY_TEXT_COLOR, color).apply()
                                    }
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selectedColor == color) {
                                    Box(Modifier.size(9.dp).background(Ink, CircleShape))
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = !running) {
                Text(
                    "Оверлей не принимает касания: кнопки и жесты Pinduoduo остаются доступными.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(end = 70.dp, bottom = 8.dp),
                )
            }
        }
    }
}
