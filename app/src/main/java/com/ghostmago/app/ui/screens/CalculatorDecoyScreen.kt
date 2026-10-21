package com.ghostmago.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmago.app.data.AppState

/**
 * Natural calculator. Display always updates.
 * Unlock vault: type PIN then press = (default 0000).
 */
@Composable
fun CalculatorDecoyScreen(onUnlocked: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var acc by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf<Char?>(null) }
    var typingNew by remember { mutableStateOf(true) }
    var pinBuf by remember { mutableStateOf("") }

    fun fmt(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "Error"
        val asLong = n.toLong()
        return if (n == asLong.toDouble()) asLong.toString()
        else {
            val s = String.format("%.10f", n).trimEnd('0').trimEnd('.')
            if (s.length > 14) String.format("%.6g", n) else s
        }
    }

    fun apply(a: Double, b: Double, o: Char): Double = when (o) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) Double.NaN else a / b
        else -> b
    }

    fun onDigit(d: String) {
        pinBuf = (pinBuf + d).takeLast(16)
        if (typingNew || display == "0" || display == "Error") {
            display = d
            typingNew = false
        } else if (display.length < 16) {
            display += d
        }
    }

    fun onDot() {
        if (typingNew || display == "Error") {
            display = "0."
            typingNew = false
            pinBuf = ""
            return
        }
        if (!display.contains('.')) display += "."
    }

    fun onOp(next: Char) {
        try {
            val v = display.toDouble()
            if (acc != null && op != null && !typingNew) {
                val r = apply(acc!!, v, op!!)
                display = fmt(r)
                acc = r
            } else {
                acc = v
            }
            op = next
            typingNew = true
            pinBuf = ""
        } catch (_: Exception) {
            display = "Error"
            acc = null
            op = null
            typingNew = true
            pinBuf = ""
        }
    }

    fun onEquals() {
        val pin = AppState.vaultPin.ifBlank { "0000" }
        if (pinBuf == pin || display == pin) {
            pinBuf = ""
            onUnlocked()
            return
        }
        try {
            val v = display.toDouble()
            if (acc != null && op != null) {
                display = fmt(apply(acc!!, v, op!!))
            }
            acc = null
            op = null
            typingNew = true
            pinBuf = ""
        } catch (_: Exception) {
            display = "Error"
            acc = null
            op = null
            typingNew = true
            pinBuf = ""
        }
    }

    fun onClear() {
        display = "0"
        acc = null
        op = null
        typingNew = true
        pinBuf = ""
    }

    fun onSign() {
        try {
            if (display != "0" && display != "Error") {
                display = fmt(-display.toDouble())
            }
        } catch (_: Exception) {
        }
        pinBuf = ""
    }

    fun onPercent() {
        try {
            display = fmt(display.toDouble() / 100.0)
            typingNew = true
        } catch (_: Exception) {
        }
        pinBuf = ""
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = display,
                color = Color.White,
                fontSize = if (display.length > 10) 40.sp else 56.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }

        val rows = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    val weight = if (key == "0") 2f else 1f
                    val bg = when (key) {
                        in listOf("÷", "×", "−", "+", "=") -> Color(0xFFFF9F0A)
                        in listOf("C", "±", "%") -> Color(0xFFA5A5A5)
                        else -> Color(0xFF333333)
                    }
                    val fg = if (key in listOf("C", "±", "%")) Color.Black else Color.White
                    Button(
                        onClick = {
                            when (key) {
                                "C" -> onClear()
                                "±" -> onSign()
                                "%" -> onPercent()
                                "÷" -> onOp('/')
                                "×" -> onOp('*')
                                "−" -> onOp('-')
                                "+" -> onOp('+')
                                "=" -> onEquals()
                                "." -> onDot()
                                else -> onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(weight)
                            .aspectRatio(if (key == "0") 2.15f else 1f),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = fg),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(key, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
