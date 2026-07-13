package dev.lunaglow.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.lunaglow.R
import dev.lunaglow.color.ColorExtractor
import dev.lunaglow.color.ColorSmoother
import dev.lunaglow.coordinator.AmbientLedCoordinator
import dev.lunaglow.led.LedDriverRegistry
import dev.lunaglow.ui.MainActivity

class ScreenCaptureService : Service() {
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    private val extractor = ColorExtractor()
    private val smoother = ColorSmoother()
    private val throttle = FrameThrottle(FRAMES_PER_SECOND)
    private var ledCoordinator: AmbientLedCoordinator? = null

    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var processedFrames = 0L
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        workerThread = HandlerThread("LunaGlowCapture").apply { start() }
        workerHandler = Handler(workerThread.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopCapture(stopProjection = true)
            ACTION_START -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCapture(stopProjection = true)
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCapture(intent: Intent) {
        if (projection != null) return
        CaptureStateStore.update(CaptureState.Starting)
        ledCoordinator = AmbientLedCoordinator(LedDriverRegistry.current())
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        if (resultCode == Int.MIN_VALUE || resultData == null) {
            fail("Screen capture consent data is missing.")
            return
        }

        try {
            val manager = getSystemService(MediaProjectionManager::class.java)
            val activeProjection = manager.getMediaProjection(resultCode, resultData)
            val callback = object : MediaProjection.Callback() {
                override fun onStop() {
                    workerHandler.post { stopCapture(stopProjection = false) }
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    workerHandler.post { resizeCaptureSurface(width, height) }
                }
            }
            activeProjection.registerCallback(callback, workerHandler)
            projection = activeProjection
            projectionCallback = callback

            val dimensions = targetDimensions(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
            )
            val reader = createReader(dimensions.first, dimensions.second)
            imageReader = reader
            virtualDisplay = activeProjection.createVirtualDisplay(
                "LunaGlowCapture",
                dimensions.first,
                dimensions.second,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                workerHandler,
            )
        } catch (error: Exception) {
            fail(error.message ?: "Unable to start screen capture.")
        }
    }

    private fun createReader(width: Int, height: Int): ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, MAX_IMAGES).also { reader ->
            reader.setOnImageAvailableListener({ availableReader ->
                val image = availableReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    if (!throttle.shouldProcess(System.nanoTime())) return@setOnImageAvailableListener
                    val plane = image.planes.firstOrNull() ?: return@setOnImageAvailableListener
                    val colors = smoother.smooth(
                        extractor.extract(
                            buffer = plane.buffer,
                            width = image.width,
                            height = image.height,
                            rowStride = plane.rowStride,
                            pixelStride = plane.pixelStride,
                        ),
                    )
                    processedFrames += 1
                    val coordinator = ledCoordinator
                    coordinator?.offer(colors, System.nanoTime())
                    CaptureStateStore.update(
                        CaptureState.Capturing(colors, processedFrames, coordinator?.failureMessage),
                    )
                } catch (error: Exception) {
                    CaptureStateStore.update(
                        CaptureState.Error(error.message ?: "Unable to process captured frame."),
                    )
                } finally {
                    image.close()
                }
            }, workerHandler)
        }

    private fun resizeCaptureSurface(contentWidth: Int, contentHeight: Int) {
        val display = virtualDisplay ?: return
        if (stopping || contentWidth <= 0 || contentHeight <= 0) return
        val dimensions = targetDimensions(contentWidth, contentHeight)
        val replacement = createReader(dimensions.first, dimensions.second)
        val previous = imageReader
        imageReader = replacement
        display.resize(dimensions.first, dimensions.second, resources.displayMetrics.densityDpi)
        display.surface = replacement.surface
        previous?.setOnImageAvailableListener(null, null)
        previous?.close()
    }

    private fun stopCapture(stopProjection: Boolean, errorMessage: String? = null) {
        if (stopping) return
        stopping = true
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        imageReader?.close()
        virtualDisplay = null
        imageReader = null

        val activeProjection = projection
        val callback = projectionCallback
        if (activeProjection != null && callback != null) activeProjection.unregisterCallback(callback)
        projection = null
        projectionCallback = null
        if (stopProjection) activeProjection?.stop()

        throttle.reset()
        smoother.reset()
        processedFrames = 0L
        ledCoordinator?.stop()
        ledCoordinator = null
        CaptureStateStore.update(CaptureStopState.resolve(CaptureStateStore.state.value, errorMessage))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        stopping = false
    }

    private fun fail(message: String) {
        stopCapture(stopProjection = true, errorMessage = message)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.capture_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.capture_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.action_stop_capture), stopIntent)
            .build()
    }

    private fun targetDimensions(width: Int, height: Int): Pair<Int, Int> =
        if (width >= height) CAPTURE_LONG_EDGE to CAPTURE_SHORT_EDGE
        else CAPTURE_SHORT_EDGE to CAPTURE_LONG_EDGE

    companion object {
        const val ACTION_START = "dev.lunaglow.capture.START"
        const val ACTION_STOP = "dev.lunaglow.capture.STOP"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        private const val NOTIFICATION_CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1001
        private const val CAPTURE_LONG_EDGE = 160
        private const val CAPTURE_SHORT_EDGE = 90
        private const val FRAMES_PER_SECOND = 15
        private const val MAX_IMAGES = 2

        fun startIntent(context: Context, resultCode: Int, resultData: Intent): Intent =
            Intent(context, ScreenCaptureService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, resultData)

        fun stopIntent(context: Context): Intent =
            Intent(context, ScreenCaptureService::class.java).setAction(ACTION_STOP)
    }
}
