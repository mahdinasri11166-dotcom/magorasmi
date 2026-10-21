package com.ghostmago.app

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ghostmago.app.data.AppState
import com.ghostmago.app.ui.NavGraph
import com.ghostmago.app.ui.theme.BgDark
import com.ghostmago.app.ui.theme.GhostMagoTheme
import com.ghostmago.app.util.LocaleHelper

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applyToApp(this)
        AppState.load(applicationContext)
        try {
            com.ghostmago.app.util.ApkCleanup.runOnce(applicationContext)
        } catch (_: Exception) {
        }
        try {
            if (AppState.useHardwareKeystore) {
                com.ghostmago.app.crypto.HardwareKeystore.ensureKey(true)
            }
            com.ghostmago.app.data.DeadManSwitch.checkIn(applicationContext)
        } catch (_: Exception) {
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        // Force RTL for window decor
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL

        setContent {
            // Compose RTL: text aligns right, rows reverse correctly, Persian joins via system font
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GhostMagoTheme(darkTheme = AppState.darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = BgDark) {
                        NavGraph()
                    }
                }
            }
        }
    }
}
