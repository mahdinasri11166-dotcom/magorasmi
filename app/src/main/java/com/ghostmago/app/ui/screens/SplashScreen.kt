package com.ghostmago.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmago.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    // Splash is now skipped when login is used; kept for compatibility
    LaunchedEffect(Unit) {
        delay(800)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👻", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("مشکات", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("سازنده: Mr Elf", color = Accent, fontSize = 14.sp)
        }
    }
}
