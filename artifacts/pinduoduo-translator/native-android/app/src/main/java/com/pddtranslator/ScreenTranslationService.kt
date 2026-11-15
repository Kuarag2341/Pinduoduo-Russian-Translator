package com.pddtranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition
import java.util.concurrent.Executor
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

class ScreenTranslationService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ocrExecutor = Executor { command -> worker?.post(command) }
    private var recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    private var repository: TranslationRepository? = null
    private var overlay: TranslationOverlay? = null
    private val processingFrame = AtomicBoolean(false)
    @Volatile private var lastFrameAt = 0L
    @Volatile private var frameNumber = 0L

    override fun onCreate() {
        super.onCreate()
        repository = TranslationRepository(this)
        overlay = TranslationOverlay(this)
        workerThread = HandlerThread("screen-ocr").also { it.start() }
        worker = Handler(workerThread!!.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            saveStatus("Перевод остановлен")
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultCode == 0 || resultData == null) {
            saveStatus("Разрешение на захват экрана не получено")
            stopSelf()
            return START_NOT_STICKY
        }
        beginProjection(resultCode, resultData)
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notification = buildNotification("Подготовка захвата экрана…")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            projection = manager.getMediaProjection(resultCode, data)?.also { mediaProjection ->
                mediaProjection.registerCallback(projectionCallback, worker!!)
                startVirtualDisplay(mediaProjection)
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, true).apply()
                saveStatus("Распознавание экрана включено")
                updateNotification("Перевод экрана активен")
            } ?: run {
                saveStatus("Android не выдал доступ к экрану")
                stopSelf()
            }
        } catch (error: SecurityException) {
            saveStatus("Нет разрешения Android на захват экрана")
            stopSelf()
        } catch (error: IllegalStateException) {
            saveStatus("Не удалось запустить захват экрана")
            stopSelf()
        }
    }

    private fun startVirtualDisplay(mediaProjection: MediaProjection) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        imageReader?.setOnImageAvailableListener({ reader -> processLatestImage(reader) }, worker)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "PinduoduoTranslation",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            worker,
        ) ?: throw IllegalStateException("Android не создал виртуальный экран")
    }

    private fun processLatestImage(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (_: IllegalStateException) {
            null
        } ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastFrameAt < MIN_FRAME_INTERVAL_MS || !processingFrame.compareAndSet(false, true)) {
            image.close()
            return
        }
        lastFrameAt = now
        val sequence = ++frameNumber
        val bitmap = try {
            image.toBitmap()
        } catch (_: RuntimeException) {
            image.close()
            processingFrame.set(false)
            saveStatus("Не удалось прочитать кадр экрана")
            return
        }
        image.close()

        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener(ocrExecutor) { recognized ->
                translateRecognizedText(recognized, sequence)
            }
            .addOnFailureListener(ocrExecutor) {
                if (sequence == frameNumber) saveStatus("Ошибка распознавания текста")
                processingFrame.set(false)
            }
            .addOnCompleteListener(ocrExecutor) {
                bitmap.recycle()
            }
    }

    private fun translateRecognizedText(recognized: Text, sequence: Long) {
        val lines = recognized.textBlocks.flatMap { block -> block.lines }
            .filter { line -> line.text.any { it.code in 0x3400..0x9FFF } }
        if (lines.isEmpty()) {
            if (sequence == frameNumber) {
                mainHandler.post { overlay?.clear() }
                saveStatus("Китайский текст на экране не найден")
            }
            processingFrame.set(false)
            return
        }

        val metrics = resources.displayMetrics
        val screenBounds = android.graphics.Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
        val safeRegions = lines.mapNotNull { line ->
            val bounds = line.boundingBox ?: return@mapNotNull null
            if (!screenBounds.intersects(bounds.left, bounds.top, bounds.right, bounds.bottom)) return@mapNotNull null
            TranslatedRegion(line.text, android.graphics.Rect(bounds))
        }
        if (safeRegions.isEmpty()) {
            if (sequence == frameNumber) {
                mainHandler.post { overlay?.clear() }
                saveStatus("Китайский текст не помещается на экране")
            }
            processingFrame.set(false)
            return
        }
        if (sequence == frameNumber) mainHandler.post { overlay?.show(emptyList()) }

        val remaining = AtomicInteger(safeRegions.size)
        val translated = ConcurrentLinkedQueue<TranslatedRegion>()
        safeRegions.forEach { region ->
            repository?.translate(region.text) { result ->
                result.onSuccess { russian ->
                    translated.add(TranslatedRegion(russian, region.bounds))
                    mainHandler.post {
                        if (sequence == frameNumber) overlay?.show(translated.toList())
                    }
                }.onFailure { error ->
                    if (sequence == frameNumber) {
                        saveStatus(error.message ?: "Не удалось перевести текст")
                        updateNotification("Проверьте подключение для загрузки модели")
                    }
                }
                if (remaining.decrementAndGet() <= 0) {
                    if (sequence == frameNumber && translated.isNotEmpty()) {
                        val count = translated.size
                        saveStatus("Переведено строк: $count")
                        updateNotification("Переведено строк: $count")
                    }
                    processingFrame.set(false)
                }
            } ?: run {
                if (remaining.decrementAndGet() <= 0) processingFrame.set(false)
            }
        }
    }

    private fun Image.toBitmap(): Bitmap {
        val plane = planes.first()
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPixels = rowStride / pixelStride
        val padded = Bitmap.createBitmap(rowPixels, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }

    private fun saveStatus(message: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_STATUS, message).apply()
    }

    private fun buildNotification(message: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_translate)
            .setContentTitle("Pinduoduo Переводчик")
            .setContentText(message)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Перевод экрана",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Показывает состояние перевода экрана" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopSelf()
        }
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, false)
            .apply()
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        overlay?.clear()
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        recognizer.close()
        repository?.close()
        workerThread?.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFS = "translator_settings"
        const val KEY_RUNNING = "running"
        const val KEY_STATUS = "status"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_TEXT_COLOR = "text_color"
        const val EXTRA_RESULT_CODE = "projection_result_code"
        const val EXTRA_RESULT_DATA = "projection_result_data"
        const val ACTION_STOP = "com.pddtranslator.STOP"
        private const val CHANNEL_ID = "screen_translation"
        private const val NOTIFICATION_ID = 28
        private const val MIN_FRAME_INTERVAL_MS = 950L
    }
}
