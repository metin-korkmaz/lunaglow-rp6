package dev.lunaglow.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import dev.lunaglow.capture.CaptureStateStore
import dev.lunaglow.capture.ScreenCaptureService

class MainActivity : ComponentActivity() {
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projectionLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        projectionManager = getSystemService(MediaProjectionManager::class.java)
        projectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                ContextCompat.startForegroundService(
                    this,
                    ScreenCaptureService.startIntent(this, result.resultCode, data),
                )
            }
        }
        notificationLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {
            launchProjectionConsent()
        }

        setContent {
            val preferences = remember { getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE) }
            var onboarded by remember { mutableStateOf(preferences.getBoolean(KEY_ONBOARDED, false)) }
            var diagnosticsVisible by remember { mutableStateOf(false) }

            LunaGlowTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when {
                        !onboarded -> OnboardingScreen {
                            preferences.edit().putBoolean(KEY_ONBOARDED, true).apply()
                            onboarded = true
                        }
                        diagnosticsVisible -> DiagnosticsScreen(
                            onBack = { diagnosticsVisible = false },
                        )
                        else -> MainScreen(
                            captureState = CaptureStateStore.state,
                            onStartCapture = ::beginCapture,
                            onStopCapture = {
                                stopService(Intent(this, ScreenCaptureService::class.java))
                            },
                            onOpenDiagnostics = { diagnosticsVisible = true },
                        )
                    }
                }
            }
        }
    }

    private fun beginCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            launchProjectionConsent()
        } else {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun launchProjectionConsent() {
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private companion object {
        const val PREFERENCES = "lunaglow_preferences"
        const val KEY_ONBOARDED = "onboarded"
    }
}

@androidx.compose.runtime.Composable
private fun LunaGlowTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Color(0xFF5840D6),
        secondary = Color(0xFFB388FF),
        tertiary = Color(0xFFFF6B9D),
        background = Color(0xFF0A0A14),
        surface = Color(0xFF1A1A2E),
        surfaceVariant = Color(0xFF242440),
        onSurfaceVariant = Color(0xFFB0B0C8),
        primaryContainer = Color(0xFF2E2A55),
        onPrimaryContainer = Color(0xFFE0D8FF),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF1A0A0A),
        errorContainer = Color(0xFF3A1A1A),
        onErrorContainer = Color(0xFFFFD6D6),
        outline = Color(0xFF5C5C7A),
        outlineVariant = Color(0xFF3A3A55),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
