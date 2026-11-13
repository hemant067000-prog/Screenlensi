package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenCaptureStateHolder {
    private val _isMediaProjectionActive = MutableStateFlow(false)
    val isMediaProjectionActive: StateFlow<Boolean> = _isMediaProjectionActive.asStateFlow()

    private val _latestCapturedBitmap = MutableStateFlow<Bitmap?>(null)
    val latestCapturedBitmap: StateFlow<Bitmap?> = _latestCapturedBitmap.asStateFlow()

    private val _capturedFrameCount = MutableStateFlow(0)
    val capturedFrameCount: StateFlow<Int> = _capturedFrameCount.asStateFlow()

    fun setProjectionActive(active: Boolean) {
        _isMediaProjectionActive.value = active
        if (!active) {
            _latestCapturedBitmap.value = null
        }
    }

    fun onNewScreenFrame(bitmap: Bitmap) {
        _latestCapturedBitmap.value = bitmap
        _capturedFrameCount.value += 1
    }
}

class ScreenCaptureTranslatorService : Service() {

    companion object {
        const val CHANNEL_ID = "screenlens_live_translator_channel"
        const val NOTIFICATION_ID = 4801

        const val ACTION_START_CAPTURE = "com.example.action.START_CAPTURE"
        const val ACTION_UPDATE_TRANSLATION = "com.example.action.UPDATE_TRANSLATION"
        const val ACTION_STOP_CAPTURE = "com.example.action.STOP_CAPTURE"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_TRANSLATED_TEXT = "extra_translated_text"
        const val EXTRA_SOURCE_TEXT = "extra_source_text"
        const val EXTRA_LANG_CODE = "extra_lang_code"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var lastFrameTimestampMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                val initialNotification = buildTranslatorNotification(
                    langCode = "AUTO",
                    translatedText = "Live Screen Translator active — translating on-screen text in background",
                    sourceText = "Switch to any app; floating PiP & notification will display replaced English text."
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            initialNotification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, initialNotification)
                    }
                } catch (_: Throwable) {
                }

                if (resultCode != 0 && resultData != null) {
                    startMediaProjectionCapture(resultCode, resultData)
                }
            }

            ACTION_UPDATE_TRANSLATION -> {
                val translated = intent.getStringExtra(EXTRA_TRANSLATED_TEXT) ?: return START_STICKY
                val source = intent.getStringExtra(EXTRA_SOURCE_TEXT) ?: ""
                val lang = intent.getStringExtra(EXTRA_LANG_CODE) ?: "AUTO"
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(
                    NOTIFICATION_ID,
                    buildTranslatorNotification(
                        langCode = lang,
                        translatedText = translated,
                        sourceText = source
                    )
                )
            }

            ACTION_STOP_CAPTURE -> {
                stopCaptureInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startMediaProjectionCapture(resultCode: Int, resultData: Intent) {
        stopCaptureInternal()
        try {
            val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: return
            val projection = mpManager.getMediaProjection(resultCode, resultData) ?: return
            mediaProjection = projection

            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCaptureInternal()
                }
            }, null)

            val thread = HandlerThread("ScreenLensCaptureThread").apply { start() }
            captureThread = thread
            val handler = Handler(thread.looper)
            captureHandler = handler

            val width = 640
            val height = 360
            val densityDpi = resources.displayMetrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            reader.setOnImageAvailableListener({ ir ->
                val image = try {
                    ir.acquireLatestImage()
                } catch (_: Throwable) {
                    null
                } ?: return@setOnImageAvailableListener

                try {
                    val now = System.currentTimeMillis()
                    // Throttle bitmap extraction to ~5 fps to keep CPU usage ultra-low in background
                    if (now - lastFrameTimestampMs >= 200L) {
                        lastFrameTimestampMs = now
                        val plane = image.planes.firstOrNull()
                        if (plane != null) {
                            val buffer = plane.buffer
                            val pixelStride = plane.pixelStride
                            val rowStride = plane.rowStride
                            val rowPadding = rowStride - pixelStride * width
                            val paddedWidth = width + rowPadding / pixelStride

                            val rawBitmap = Bitmap.createBitmap(
                                paddedWidth.coerceAtLeast(width),
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            buffer.rewind()
                            rawBitmap.copyPixelsFromBuffer(buffer)
                            val cropped = if (paddedWidth > width) {
                                Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                            } else {
                                rawBitmap
                            }
                            ScreenCaptureStateHolder.onNewScreenFrame(cropped)
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    image.close()
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "ScreenLensVirtualDisplay",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )

            ScreenCaptureStateHolder.setProjectionActive(true)
        } catch (_: Throwable) {
            ScreenCaptureStateHolder.setProjectionActive(false)
        }
    }

    private fun stopCaptureInternal() {
        ScreenCaptureStateHolder.setProjectionActive(false)
        try {
            virtualDisplay?.release()
        } catch (_: Throwable) {
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (_: Throwable) {
        }
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (_: Throwable) {
        }
        mediaProjection = null

        try {
            captureThread?.quitSafely()
        } catch (_: Throwable) {
        }
        captureThread = null
        captureHandler = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScreenLens Off-Screen Live Translation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live translated English text while ScreenLens runs in the background"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildTranslatorNotification(
        langCode: String,
        translatedText: String,
        sourceText: String
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenCaptureTranslatorService::class.java).apply {
            action = ACTION_STOP_CAPTURE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("[$langCode -> EN] $translatedText")
            .setContentText(sourceText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("ScreenLens • [$langCode -> English]")
                    .bigText("$translatedText\nOriginal: $sourceText")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Screen Capture",
                pendingStop
            )
            .build()
    }

    override fun onDestroy() {
        stopCaptureInternal()
        super.onDestroy()
    }
}
