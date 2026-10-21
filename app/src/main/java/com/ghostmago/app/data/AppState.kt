package com.ghostmago.app.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ghostmago.app.util.SecurityUtils
import java.io.File
import java.security.MessageDigest

enum class MeshRole(val value: String, val fa: String, val rank: Int, val symbol: String) {
    SUPREME("supreme", "فرمانده کل", 100, "★★★"),
    COMMANDER("commander", "فرمانده", 80, "★★"),
    LEADER("leader", "سرگروه", 40, "◆"),
    SOLDIER("soldier", "نیرو", 20, "●"),
    RECRUIT("recruit", "تازهکار", 10, "○");

    fun canCommand() = rank >= COMMANDER.rank
    fun canLead() = rank >= LEADER.rank
    /** Membership approve/reject: commander ranks only. */
    fun canApprove() = rank >= COMMANDER.rank
    fun canInvite() = rank >= LEADER.rank
    fun isReadOnly() = this == RECRUIT

    /** Role gate passwords — never show these in UI help text. */
    fun requiresRolePassword(): Boolean = this == SUPREME || this == COMMANDER
    fun checkRolePassword(input: String): Boolean = when (this) {
        SUPREME -> input.trim() == "yaali"
        COMMANDER -> input.trim() == "malk"
        else -> true
    }
    companion object {
        fun from(s: String) = entries.find { it.value == s } ?: RECRUIT
    }
}

object AppState {
    var isLoggedIn by mutableStateOf(false)
    var isDuressMode by mutableStateOf(false)
    var isHoneyMode by mutableStateOf(false)
    var nickname by mutableStateOf("کاربر")
    var meshRole by mutableStateOf(MeshRole.SOLDIER)
    var groupId by mutableStateOf("")
    var darkTheme by mutableStateOf(true)
    var accentColor by mutableStateOf(0)
    var bgImagePath by mutableStateOf<String?>(null)
    var appPasswordHash by mutableStateOf("")
    var duressPasswordHash by mutableStateOf("")
    var vaultPin by mutableStateOf("0000") // numeric calculator unlock, changeable
    var safeWord by mutableStateOf("آزادی") // typed in chat/settings triggers honey
    var radioSilence by mutableStateOf(false)
    var ephemeralDefaultSec by mutableStateOf(0)
    var randomSendDelay by mutableStateOf(false)
    var forceReadOnly by mutableStateOf(false)
    var inviteTtlSec by mutableStateOf(3600)
    var coverTraffic by mutableStateOf(false)
    var coverIntervalSec by mutableStateOf(15)
    var requireSafetyNumber by mutableStateOf(true)
    var sealedSender by mutableStateOf(false)
    var opProfileId by mutableStateOf("urban")
    var useHardwareKeystore by mutableStateOf(true)
    var onboardingDone by mutableStateOf(false)
    var devMode by mutableStateOf(false)
    var strictAttestation by mutableStateOf(false)
    var sessionTimeoutMin by mutableStateOf(15)
    var lastActiveAt by mutableStateOf(0L)
    var autoLockEnabled by mutableStateOf(true)
    var meshPort by mutableStateOf(5599)
    var roomsJson by mutableStateOf("[]")
    var pendingMeshPassword by mutableStateOf("")
    var pendingMeshAutoJoin by mutableStateOf(false)
    /** Last successful mesh credentials — reconnect without retyping password. */
    var lastGroupPassword by mutableStateOf("")
    var lastGroupId by mutableStateOf("default")
    var lastMeshNickname by mutableStateOf("")
    var relayEnabled by mutableStateOf(false)
    var relayHost by mutableStateOf("")
    var useBluetoothMesh by mutableStateOf(false)
    var iconHidden by mutableStateOf(false)
    var hideIconCode by mutableStateOf("1234")
    var showIconCode by mutableStateOf("4321")
    private var loginFailCount = 0
    private var loginLockUntil = 0L // override: soldier receiver-only
    var failedLoginAttempts by mutableStateOf(0)
    var lockUntil by mutableStateOf(0L)
    var calculatorUnlocked by mutableStateOf(false)

    fun load(context: Context) {
        val p = SecurityUtils.encryptedPrefs(context)
        nickname = p.getString("nickname", "کاربر") ?: "کاربر"
        meshRole = MeshRole.from(p.getString("mesh_role", "soldier") ?: "soldier")
        groupId = p.getString("group_id", "") ?: ""
        darkTheme = p.getBoolean("dark_theme", true)
        accentColor = p.getInt("accent", 0)
        bgImagePath = p.getString("bg_image", null)
        appPasswordHash = p.getString("app_pw_hash", "") ?: ""
        duressPasswordHash = p.getString("duress_pw_hash", "") ?: ""
        relayEnabled = p.getBoolean("relay_enabled", false)
        relayHost = p.getString("relay_host", "") ?: ""
        useBluetoothMesh = p.getBoolean("bt_mesh", false)
        iconHidden = p.getBoolean("icon_hidden", false)
        hideIconCode = p.getString("hide_icon_code", "1234") ?: "1234"
        showIconCode = p.getString("show_icon_code", "4321") ?: "4321"
        vaultPin = p.getString("vault_pin", "0000") ?: "0000"
        safeWord = p.getString("safe_word", "آزادی") ?: "آزادی"
        radioSilence = p.getBoolean("radio_silence", false)
        ephemeralDefaultSec = p.getInt("ephemeral_sec", 0)
        randomSendDelay = p.getBoolean("random_delay", false)
        forceReadOnly = p.getBoolean("force_readonly", false)
        inviteTtlSec = p.getInt("invite_ttl", 3600)
        coverTraffic = p.getBoolean("cover_traffic", false)
        coverIntervalSec = p.getInt("cover_interval", 15)
        requireSafetyNumber = p.getBoolean("require_safety", true)
        sealedSender = p.getBoolean("sealed_sender", false)
        opProfileId = p.getString("op_profile", "urban") ?: "urban"
        useHardwareKeystore = p.getBoolean("hw_keystore", true)
        onboardingDone = p.getBoolean("onboarding_done", false)
        devMode = p.getBoolean("dev_mode", false)
        strictAttestation = p.getBoolean("strict_attest", false)
        sessionTimeoutMin = p.getInt("session_timeout_min", 15)
        autoLockEnabled = p.getBoolean("auto_lock", true)
        meshPort = p.getInt("mesh_port", 5599)
        lastGroupPassword = p.getString("last_group_pw", "") ?: ""
        lastGroupId = p.getString("last_group_id", "default") ?: "default"
        lastMeshNickname = p.getString("last_mesh_nick", "") ?: ""
        roomsJson = p.getString("rooms_json", "[]") ?: "[]"
        failedLoginAttempts = p.getInt("fail_attempts", 0)
        lockUntil = p.getLong("lock_until", 0L)
        if (appPasswordHash.isEmpty()) {
            appPasswordHash = hashPassword("mr elf")
            p.edit().putString("app_pw_hash", appPasswordHash).apply()
        }
    }

    fun save(context: Context) {
        SecurityUtils.encryptedPrefs(context).edit()
            .putString("nickname", nickname)
            .putString("mesh_role", meshRole.value)
            .putString("group_id", groupId)
            .putBoolean("dark_theme", darkTheme)
            .putInt("accent", accentColor)
            .putString("bg_image", bgImagePath)
            .putString("app_pw_hash", appPasswordHash)
            .putString("duress_pw_hash", duressPasswordHash)
            .putBoolean("relay_enabled", relayEnabled)
            .putString("relay_host", relayHost)
            .putBoolean("bt_mesh", useBluetoothMesh)
            .putBoolean("icon_hidden", iconHidden)
            .putString("hide_icon_code", hideIconCode)
            .putString("show_icon_code", showIconCode)
            .putString("vault_pin", vaultPin)
            .putString("safe_word", safeWord)
            .putBoolean("radio_silence", radioSilence)
            .putInt("ephemeral_sec", ephemeralDefaultSec)
            .putBoolean("random_delay", randomSendDelay)
            .putBoolean("force_readonly", forceReadOnly)
            .putInt("invite_ttl", inviteTtlSec)
            .putBoolean("cover_traffic", coverTraffic)
            .putInt("cover_interval", coverIntervalSec)
            .putBoolean("require_safety", requireSafetyNumber)
            .putBoolean("sealed_sender", sealedSender)
            .putString("op_profile", opProfileId)
            .putBoolean("hw_keystore", useHardwareKeystore)
            .putBoolean("onboarding_done", onboardingDone)
            .putBoolean("dev_mode", devMode)
            .putBoolean("strict_attest", strictAttestation)
            .putInt("session_timeout_min", sessionTimeoutMin)
            .putBoolean("auto_lock", autoLockEnabled)
            .putInt("mesh_port", meshPort)
            .putString("last_group_pw", lastGroupPassword)
            .putString("last_group_id", lastGroupId)
            .putString("last_mesh_nick", lastMeshNickname)
            .putString("rooms_json", roomsJson)
            .putInt("fail_attempts", failedLoginAttempts)
            .putLong("lock_until", lockUntil)
            .apply()
    }

    fun hashPassword(pw: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(("GhostMago|v1|" + pw).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    fun tryLogin(context: Context, pw: String): String {
        if (System.currentTimeMillis() < loginLockUntil) return "locked"
        val h = hashPassword(pw)
        // Duress password → fake honey UI
        if (duressPasswordHash.isNotEmpty() && h == duressPasswordHash) {
            loginFailCount = 0
            isLoggedIn = true
            isDuressMode = true
            isHoneyMode = true
            touchSession()
            save(context)
            try { SecurityUtils.audit(context, "DURESS_LOGIN") } catch (_: Exception) {}
            return "ok"
        }
        if (h == appPasswordHash) {
            loginFailCount = 0
            isLoggedIn = true
            isDuressMode = false
            isHoneyMode = false
            touchSession()
            save(context)
            return "ok"
        }
        // Safe word as alternate duress
        if (safeWord.isNotBlank() && pw.trim() == safeWord) {
            loginFailCount = 0
            isLoggedIn = true
            isDuressMode = true
            isHoneyMode = true
            touchSession()
            save(context)
            return "ok"
        }
        loginFailCount++
        if (loginFailCount >= 5) {
            loginLockUntil = System.currentTimeMillis() + 60_000
            loginFailCount = 0
        }
        return "wrong"
    }

    fun setPassword(context: Context, newPw: String) {
        appPasswordHash = hashPassword(newPw)
        save(context)
        SecurityUtils.audit(context, "PASSWORD_CHANGED")
    }

    fun setDuressPassword(context: Context, pw: String) {
        duressPasswordHash = if (pw.isBlank()) "" else hashPassword(pw)
        save(context)
    }

    fun setVaultPin(context: Context, pin: String) {
        if (pin.all { it.isDigit() } && pin.length in 4..12) {
            vaultPin = pin
            save(context)
            SecurityUtils.audit(context, "VAULT_PIN_CHANGED")
        }
    }

    fun setSafeWord(context: Context, word: String) {
        safeWord = word.trim()
        save(context)
    }

    fun canSendGroup(): Boolean {
        if (forceReadOnly) return false
        if (isHoneyMode) return false
        return !meshRole.isReadOnly() || meshRole.canLead()
    }

    fun setBackground(context: Context, uri: Uri?) {
        if (uri == null) {
            bgImagePath = null; save(context); return
        }
        try {
            val dir = File(context.filesDir, "bg").apply { mkdirs() }
            val dest = File(dir, "custom_bg.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            bgImagePath = dest.absolutePath
            save(context)
        } catch (_: Exception) {}
    }

    /** Fake messages shown in honey/duress mode */
    fun honeyMessages(): List<Pair<String, String>> = listOf(
        "علی" to "سلام، فردا جلسه ساعت ۱۰",
        "مریم" to "فایل گزارش را فرستادم",
        "حسین" to "یادتان نرود قبض را بپردازید",
        "سیستم" to "پشتیبانگیری انجام شد",
        "رضا" to "کد تخفیف: SALE20"
    )

    fun touchSession() {
        lastActiveAt = System.currentTimeMillis()
    }

    fun shouldAutoLock(): Boolean {
        if (!autoLockEnabled || !isLoggedIn) return false
        if (lastActiveAt == 0L) return false
        val limit = sessionTimeoutMin.coerceIn(1, 120) * 60_000L
        return System.currentTimeMillis() - lastActiveAt > limit
    }

    fun lockSession() {
        isLoggedIn = false
        calculatorUnlocked = false
        lastActiveAt = 0L
    }
}
