package com.pddtranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
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
    private var captureWidth = 0
    private var captureHeight = 0
    private var captureDensityDpi = 0
    private var displayListenerRegistered = false
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
        captureWidth = width
        captureHeight = height
        captureDensityDpi = densityDpi
        imageReader = createImageReader(width, height)
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
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, worker)
        displayListenerRegistered = true
    }

    private fun createImageReader(width: Int, height: Int): ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3).also { reader ->
            reader.setOnImageAvailableListener({ availableReader -> processLatestImage(availableReader) }, worker)
        }

    private fun refreshCaptureSize() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        if (width <= 0 || height <= 0 ||
            (width == captureWidth && height == captureHeight && densityDpi == captureDensityDpi)
        ) return

        val display = virtualDisplay ?: return
        val replacementReader = try {
            createImageReader(width, height)
        } catch (_: RuntimeException) {
            saveStatus("Не удалось обновить размер захвата экрана")
            return
        }
        val previousReader = imageReader
        try {
            display.resize(width, height, densityDpi)
            display.setSurface(replacementReader.surface)
            imageReader = replacementReader
            captureWidth = width
            captureHeight = height
            captureDensityDpi = densityDpi
            frameNumber += 1
            mainHandler.post { overlay?.clear() }
            previousReader?.setOnImageAvailableListener(null, null)
            previousReader?.close()
        } catch (_: RuntimeException) {
            replacementReader.setOnImageAvailableListener(null, null)
            replacementReader.close()
            saveStatus("Не удалось обновить размер захвата экрана")
        }
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
                translateRecognizedText(recognized, sequence, bitmap)
            }
            .addOnFailureListener(ocrExecutor) {
                if (sequence == frameNumber) saveStatus("Ошибка распознавания текста")
                processingFrame.set(false)
            }
            .addOnCompleteListener(ocrExecutor) {
                bitmap.recycle()
            }
    }

    private fun translateRecognizedText(recognized: Text, sequence: Long, bitmap: Bitmap) {
        val lines = recognized.textBlocks.flatMap { block -> block.lines }
            .filter(::isSafeChineseTextLine)
        if (lines.isEmpty()) {
            if (sequence == frameNumber) {
                mainHandler.post { overlay?.clear() }
                saveStatus("Китайский текст на экране не найден")
            }
            processingFrame.set(false)
            return
        }

        val metrics = resources.displayMetrics
        val candidates = lines.mapNotNull { line ->
            val bounds = line.boundingBox ?: return@mapNotNull null
            if (!isReliableTextBounds(bounds, bitmap.width, bitmap.height)) return@mapNotNull null
            val screenBounds = android.graphics.Rect(
                (bounds.left * metrics.widthPixels.toFloat() / bitmap.width).toInt(),
                (bounds.top * metrics.heightPixels.toFloat() / bitmap.height).toInt(),
                (bounds.right * metrics.widthPixels.toFloat() / bitmap.width).toInt(),
                (bounds.bottom * metrics.heightPixels.toFloat() / bitmap.height).toInt(),
            )
            if (screenBounds.width() <= 0 || screenBounds.height() <= 0) return@mapNotNull null
            TranslatedRegion(
                line.text,
                screenBounds,
                estimateFlatBackgroundColor(bitmap, bounds),
            )
        }
        // Refuse ambiguous overlapping OCR boxes so one patch cannot erase a neighboring label.
        val safeRegions = candidates.filterIndexed { index, region ->
            candidates.indices.none { otherIndex ->
                otherIndex != index && android.graphics.Rect.intersects(
                    region.bounds,
                    candidates[otherIndex].bounds,
                )
            }
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
                    translated.add(TranslatedRegion(russian, region.bounds, region.patchColor))
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

    private fun isReliableTextBounds(bounds: android.graphics.Rect, width: Int, height: Int): Boolean {
        if (bounds.left < 0 || bounds.top < 0 || bounds.right > width || bounds.bottom > height) return false
        val boxWidth = bounds.width()
        val boxHeight = bounds.height()
        return boxWidth >= MIN_TEXT_BOUNDS_PX &&
            boxHeight >= MIN_TEXT_BOUNDS_PX &&
            boxWidth <= width * MAX_TEXT_WIDTH_FRACTION &&
            boxHeight <= height * MAX_TEXT_HEIGHT_FRACTION &&
            boxWidth.toFloat() / boxHeight <= MAX_TEXT_ASPECT_RATIO
    }

    private fun isSafeChineseTextLine(line: Text.Line): Boolean =
        line.confidence >= MIN_OCR_CONFIDENCE &&
            line.text.any { it.code in 0x3400..0x9FFF } &&
            line.text.none { it.isDigit() || it in CURRENCY_MARKERS }

    /**
     * Estimate a solid patch only when both the outside edge and most of the OCR box
     * agree on one color. This rejects textured photo regions; ambiguous cases keep the
     * previous semi-opaque label style rather than painting over uncertain content.
     */
    private fun estimateFlatBackgroundColor(bitmap: Bitmap, bounds: android.graphics.Rect): Int? {
        val ringWidth = maxOf(MIN_PATCH_SAMPLE_PX, minOf(bounds.width(), bounds.height()) / PATCH_RING_DIVISOR)
        val stride = maxOf(1, minOf(bounds.width(), bounds.height()) / PATCH_SAMPLE_STRIDE_DIVISOR)
        val samples = ArrayList<Int>()
        fun addSample(x: Int, y: Int) {
            if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                samples.add(bitmap.getPixel(x, y))
            }
        }

        var x = bounds.left
        while (x < bounds.right) {
            for (offset in 1..ringWidth) {
                addSample(x, bounds.top - offset)
                addSample(x, bounds.bottom - 1 + offset)
            }
            x += stride
        }
        var y = bounds.top
        while (y < bounds.bottom) {
            for (offset in 1..ringWidth) {
                addSample(bounds.left - offset, y)
                addSample(bounds.right - 1 + offset, y)
            }
            y += stride
        }

        if (samples.size < MIN_PATCH_SAMPLES) return null
        val dominantBucket = samples.groupingBy(::colorBucket).eachCount().maxByOrNull { it.value } ?: return null
        if (dominantBucket.value.toFloat() / samples.size < MIN_PATCH_DOMINANT_FRACTION) return null
        if (backgroundFractionInside(bitmap, bounds, dominantBucket.key, stride) <
            MIN_PATCH_INTERIOR_BACKGROUND_FRACTION
        ) return null
        val dominantColors = samples.filter { colorBucket(it) == dominantBucket.key }
        val red = dominantColors.map { Color.red(it) }.average().toInt()
        val green = dominantColors.map { Color.green(it) }.average().toInt()
        val blue = dominantColors.map { Color.blue(it) }.average().toInt()
        val meanDeviation = dominantColors.map { color ->
            kotlin.math.abs(Color.red(color) - red) +
                kotlin.math.abs(Color.green(color) - green) +
                kotlin.math.abs(Color.blue(color) - blue)
        }.average()
        if (meanDeviation > MAX_PATCH_COLOR_DEVIATION) return null
        return Color.rgb(red, green, blue)
    }

    private fun backgroundFractionInside(
        bitmap: Bitmap,
        bounds: android.graphics.Rect,
        expectedBucket: Int,
        stride: Int,
    ): Float {
        var matches = 0
        var count = 0
        var y = bounds.top
        while (y < bounds.bottom) {
            var x = bounds.left
            while (x < bounds.right) {
                if (colorBucket(bitmap.getPixel(x, y)) == expectedBucket) matches += 1
                count += 1
                x += stride
            }
            y += stride
        }
        return if (count == 0) 0f else matches.toFloat() / count
    }

    private fun colorBucket(color: Int): Int =
        ((Color.red(color) shr COLOR_BUCKET_SHIFT) shl 8) or
            ((Color.green(color) shr COLOR_BUCKET_SHIFT) shl 4) or
            (Color.blue(color) shr COLOR_BUCKET_SHIFT)

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

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == DEFAULT_DISPLAY_ID) worker?.post { refreshCaptureSize() }
        }
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, false)
            .apply()
        imageReader?.setOnImageAvailableListener(null, null)
        if (displayListenerRegistered) {
            (getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .unregisterDisplayListener(displayListener)
            displayListenerRegistered = false
        }
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
        private const val DEFAULT_DISPLAY_ID = 0
        private const val MIN_OCR_CONFIDENCE = 0.55f
        private val CURRENCY_MARKERS = setOf('¥', '￥', '$', '€', '£')
        private const val MIN_TEXT_BOUNDS_PX = 10
        private const val MAX_TEXT_WIDTH_FRACTION = 0.92f
        private const val MAX_TEXT_HEIGHT_FRACTION = 0.15f
        private const val MAX_TEXT_ASPECT_RATIO = 35f
        private const val MIN_PATCH_SAMPLE_PX = 2
        private const val PATCH_RING_DIVISOR = 5
        private const val PATCH_SAMPLE_STRIDE_DIVISOR = 24
        private const val MIN_PATCH_SAMPLES = 24
        private const val MIN_PATCH_DOMINANT_FRACTION = 0.68f
        private const val MIN_PATCH_INTERIOR_BACKGROUND_FRACTION = 0.55f
        private const val COLOR_BUCKET_SHIFT = 4
        private const val MAX_PATCH_COLOR_DEVIATION = 22.0
    }
}
