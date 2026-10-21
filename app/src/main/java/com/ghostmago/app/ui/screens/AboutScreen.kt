package com.ghostmago.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghostmago.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("راهنما", color = TextPrimary) },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("شبکه ارتباطیکات (Mishkat)", color = Primary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("سازنده: Mr Elf", color = Accent, fontWeight = FontWeight.SemiBold)

            GuideCard(
                "نام برنامه",
                "نام برنامه برگرفته از آیه ۳۵ سوره نور: «كَمِشْكَاةٍ فِيهَا مِصْبَاحٌ». شبکه ارتباطیکات یعنی چراغدان یا محفظهای که نور در آن پنهان و محفوظ است. دقیقاً ترکیب «نور» + «پنهان/محفوظ»."
            )
            GuideCard(
                "آیکون و مخفیسازی",
                "در منوی گوشی آیکون «ماشین حساب» دیده میشود.\n" +
                    "مخفی کردن آیکون: از شمارهگیر *#*#1234#*#* یا از تنظیمات → مخفی الآن.\n" +
                    "نمایش دوباره و باز کردن: *#*#4321#*#* یا تنظیمات → نمایش آیکون.\n" +
                    "اگر بعد از مخفیسازی هنوز میانبر مانده، یکبار صفحه را قفل کنید یا لانچر را تازه کنید. روی بعضی گوشیها کد مخفی سیستم محدود است — از دکمه تنظیمات استفاده کنید."
            )
            GuideCard("شروع", "پین ماشینحساب سپس رمز. پیشفرض پین 0000 و رمز mr elf.")
            GuideCard("شبکه ارتباطی", "فرمانده بارکد دعوت میسازد. هر دو گوشی یک وایفای. بعد از اسکن اتصال خودکار؛ فرمانده تأیید میکند.")
            GuideCard("پنهاننگاری", "متن، عکس، صوت، پیامک و فایل در تصویر.")
            GuideCard("فشار", "کلمه امن یا رمز اضطراری فقط صندوق جعلی را باز میکند.")
            GuideCard("امنیت", "قفل خودکار، نسخه شبکه ارتباطی، FLAG_SECURE، پاکسازی اضطراری.")
            Text("اولویت آفلاین · بدون وابستگی ابر · رمزنگاری محلی", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GuideCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
