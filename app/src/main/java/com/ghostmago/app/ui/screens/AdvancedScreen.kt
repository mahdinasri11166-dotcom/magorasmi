package com.ghostmago.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmago.app.crypto.SafetyNumber
import com.ghostmago.app.crypto.Shamir
import com.ghostmago.app.ui.theme.*
import com.ghostmago.app.util.SecurityUtils
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) } // 0=Shamir 1=Safety 2=Panic 3=Stats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ابزار پیشرفته", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)
            .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            ScrollableTabRow(selectedTabIndex = tab, containerColor = SurfaceDark) {
                listOf("شیمیر", "ایمنی", "وحشت", "آمار").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(t, color = if (tab == i) Primary else TextSecondary) })
                }
            }

            when (tab) {
                0 -> ShamirTab(context)
                1 -> SafetyTab()
                2 -> PanicTab(context)
                3 -> StatsTab(context)
            }
        }
    }
}

@Composable
private fun ShamirTab(context: Context) {
    var secret by remember { mutableStateOf("") }
    var n by remember { mutableStateOf("5") }
    var k by remember { mutableStateOf("3") }
    var sharesText by remember { mutableStateOf("") }
    var recoverInput by remember { mutableStateOf("") }
    var recovered by remember { mutableStateOf("") }

    Text("تقسیم راز (Shamir)", color = TextPrimary, fontWeight = FontWeight.Bold)
    OutlinedTextField(value = secret, onValueChange = { secret = it },
        label = { Text("راز") }, modifier = Modifier.fillMaxWidth(),
        colors = fColors(), shape = RoundedCornerShape(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("n") },
            modifier = Modifier.weight(1f), colors = fColors(), shape = RoundedCornerShape(12.dp))
        OutlinedTextField(value = k, onValueChange = { k = it }, label = { Text("حد نصاب k") },
            modifier = Modifier.weight(1f), colors = fColors(), shape = RoundedCornerShape(12.dp))
    }
    Button(onClick = {
        try {
            val nn = n.toIntOrNull() ?: 5
            val kk = k.toIntOrNull() ?: 3
            val sh = Shamir.split(secret.toByteArray(Charsets.UTF_8), nn, kk)
            sharesText = sh.joinToString("\n") { (i, d) -> Shamir.toShareLine(i, d) }
            recoverInput = sharesText
        } catch (e: Exception) {
            sharesText = "خطا: ${e.message}"
        }
    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Primary),
        shape = RoundedCornerShape(12.dp)) { Text("تقسیم راز") }

    if (sharesText.isNotEmpty()) {
        OutlinedTextField(value = sharesText, onValueChange = {}, readOnly = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), colors = fColors(),
            shape = RoundedCornerShape(12.dp), minLines = 5, maxLines = 12, singleLine = false)
        Button(onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("shares", sharesText))
            Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Secondary),
            shape = RoundedCornerShape(12.dp)) { Text("کپی تکهها") }
    }

    Divider(color = Border)
    Text("بازسازی راز", color = TextPrimary, fontWeight = FontWeight.Bold)
    OutlinedTextField(value = recoverInput, onValueChange = { recoverInput = it },
        label = { Text("تکه ها - هر خط یک سهم") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
        colors = fColors(), shape = RoundedCornerShape(12.dp),
        minLines = 5, maxLines = 12, singleLine = false)
    Button(onClick = {
        try {
            val plain = Shamir.recoverFromText(recoverInput, minShares = 2)
            recovered = String(plain, Charsets.UTF_8)
            if (recovered.isEmpty()) recovered = "(خالی)"
        } catch (e: Exception) {
            recovered = "خطا: ${e.message}"
        }
    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Accent),
        shape = RoundedCornerShape(12.dp)) { Text("بازسازی راز") }
    if (recovered.isNotEmpty()) {
        OutlinedTextField(
            value = recovered,
            onValueChange = {},
            readOnly = true,
            label = { Text("راز بازسازیشده") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            colors = fColors(),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )
    }
}

@Composable
private fun SafetyTab() {
    val myPub = remember { ByteArray(32).also { SecureRandom().nextBytes(it) } }
    val peerPub = remember { ByteArray(32).also { SecureRandom().nextBytes(it) } }
    val sn = remember {
        SafetyNumber.generate("me", myPub, "peer", peerPub)
    }
    val fp = remember { SafetyNumber.fingerprint(myPub) }

    Text("شماره ایمنی (Safety Number)", color = TextPrimary, fontWeight = FontWeight.Bold)
    Text("این عدد را رو در رو با طرف مقابل مقایسه کنید.", color = TextSecondary, fontSize = 13.sp)
    Card(colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(sn, color = Warning, modifier = Modifier.padding(16.dp), fontSize = 14.sp,
            fontWeight = FontWeight.Medium)
    }
    Text("اثر انگشت کلید: $fp", color = TextSecondary, fontSize = 12.sp)
}

@Composable
private fun PanicTab(context: Context) {
    var confirm by remember { mutableStateOf("") }
    Text("وضعیت امنیتی دستگاه", color = TextPrimary, fontWeight = FontWeight.Bold)
    Text(com.ghostmago.app.util.AntiForensics.securityReport(context), color = TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))
    Text("🚨 پاکسازی اضطراری", color = Danger, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text(
        "تمام دادههای ذخیرهشده اپ (پیامها، فایلها، کلیدها، تنظیمات) بهصورت امن پاک میشوند و قابل بازیابی نیستند.",
        color = TextSecondary, fontSize = 13.sp
    )
    OutlinedTextField(value = confirm, onValueChange = { confirm = it },
        label = { Text("برای تأیید بنویسید: WIPE") }, modifier = Modifier.fillMaxWidth(),
        colors = fColors(), shape = RoundedCornerShape(12.dp))
    Button(
        onClick = {
            if (confirm == "WIPE") {
                SecurityUtils.panicWipe(context)
                Toast.makeText(context, "پاکسازی کامل انجام شد", Toast.LENGTH_LONG).show()
                confirm = ""
            } else {
                Toast.makeText(context, "عبارت اشتباه است", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Danger),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("اجرای پاکسازی کامل", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatsTab(context: Context) {
    val keys = listOf(
        "sent" to "پیام مخفی ساخته",
        "received" to "پیام مخفی خوانده",
        "qr_made" to "بارکد ساختهشد",
        "qr_read" to "QR خوانده",
        "image_sent" to "تصویر پنهاننگاری",
        "image_recv" to "استخراج تصویر"
    )
    Text("آمار استفاده", color = TextPrimary, fontWeight = FontWeight.Bold)
    keys.forEach { (k, label) ->
        Card(colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = TextPrimary)
                Text("${SecurityUtils.getStat(context, k)}", color = Primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun fColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary, unfocusedBorderColor = Border,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor = Primary, focusedContainerColor = CardDark, unfocusedContainerColor = CardDark,
    focusedLabelColor = Primary, unfocusedLabelColor = TextSecondary
)
