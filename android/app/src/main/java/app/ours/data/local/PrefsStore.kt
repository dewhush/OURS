package app.ours.data.local

import android.content.Context
import android.content.SharedPreferences
import app.ours.BuildConfig
import app.ours.data.api.TokensDto
import app.ours.data.api.UserDto

class PrefsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("ours_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, null)
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.DEFAULT_SERVER_URL.takeIf { it.isNotBlank() }
            ?: "http://192.168.100.92:8080"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim().trimEnd('/')).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, if (value == "id") "id" else "en").apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) {
            val normalized = when (value) {
                "light", "dark" -> value
                else -> "system"
            }
            prefs.edit().putString(KEY_THEME_MODE, normalized).apply()
        }

    var accessToken: String?
        get() = TokenCipher.decrypt(prefs.getString(KEY_ACCESS_TOKEN, null))
        private set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, TokenCipher.encrypt(value)).apply()

    var refreshToken: String?
        get() = TokenCipher.decrypt(prefs.getString(KEY_REFRESH_TOKEN, null))
        private set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, TokenCipher.encrypt(value)).apply()

    var accessExpiresAt: Long
        get() = prefs.getLong(KEY_ACCESS_EXPIRES_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_ACCESS_EXPIRES_AT, value).apply()

    var refreshExpiresAt: Long
        get() = prefs.getLong(KEY_REFRESH_EXPIRES_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_REFRESH_EXPIRES_AT, value).apply()

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var partnerId: Long?
        get() = if (prefs.contains(KEY_PARTNER_ID)) prefs.getLong(KEY_PARTNER_ID, -1L).takeIf { it > 0 } else null
        set(value) {
            if (value == null) prefs.edit().remove(KEY_PARTNER_ID).apply()
            else prefs.edit().putLong(KEY_PARTNER_ID, value).apply()
        }

    var partnerName: String?
        get() = prefs.getString(KEY_PARTNER_NAME, null)
        set(value) = prefs.edit().putString(KEY_PARTNER_NAME, value).apply()

    var partnerEmail: String?
        get() = prefs.getString(KEY_PARTNER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_PARTNER_EMAIL, value).apply()

    var sharingPaused: Boolean
        get() = prefs.getBoolean(KEY_SHARING_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHARING_PAUSED, value).apply()

    var partnerPaused: Boolean
        get() = prefs.getBoolean(KEY_PARTNER_PAUSED, false)
        set(value) = prefs.edit().putBoolean(KEY_PARTNER_PAUSED, value).apply()

    var eventCursor: Long
        get() = prefs.getLong(KEY_EVENT_CURSOR, 0L)
        set(value) = prefs.edit().putLong(KEY_EVENT_CURSOR, value).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var inviteCode: String?
        get() = prefs.getString(KEY_INVITE_CODE, null)
        set(value) = prefs.edit().putString(KEY_INVITE_CODE, value).apply()

    var fcmTokenSent: String?
        get() = prefs.getString(KEY_FCM_TOKEN_SENT, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN_SENT, value).apply()

    val isPaired: Boolean
        get() = !accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank() && userId > 0

    var crashDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_CRASH_DETECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_CRASH_DETECTION, value).apply()

    var emergencyContactName: String
        get() = prefs.getString(KEY_EMERGENCY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_NAME, value.trim()).apply()

    var emergencyContactPhone: String
        get() = prefs.getString(KEY_EMERGENCY_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMERGENCY_PHONE, value.trim()).apply()

    fun saveTokens(tokens: TokensDto) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, TokenCipher.encrypt(tokens.accessToken))
            .putLong(KEY_ACCESS_EXPIRES_AT, tokens.accessExpiresAt)
            .putString(KEY_REFRESH_TOKEN, TokenCipher.encrypt(tokens.refreshToken))
            .putLong(KEY_REFRESH_EXPIRES_AT, tokens.refreshExpiresAt)
            .apply()
    }

    fun saveSelf(user: UserDto) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_EMAIL, user.email)
            .apply()
    }

    fun advanceCursor(eventId: Long) {
        if (eventId > 0 && eventId > eventCursor) {
            eventCursor = eventId
        }
    }

    fun clearSession() {
        val savedServerUrl = serverUrl
        val savedLang = language
        val savedTheme = themeMode
        prefs.edit().clear().apply()
        serverUrl = savedServerUrl
        language = savedLang
        themeMode = savedTheme
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ACCESS_EXPIRES_AT = "access_expires_at"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_REFRESH_EXPIRES_AT = "refresh_expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_PARTNER_ID = "partner_id"
        private const val KEY_PARTNER_NAME = "partner_name"
        private const val KEY_PARTNER_EMAIL = "partner_email"
        private const val KEY_SHARING_PAUSED = "sharing_paused"
        private const val KEY_PARTNER_PAUSED = "partner_paused"
        private const val KEY_EVENT_CURSOR = "event_cursor"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_INVITE_CODE = "invite_code"
        private const val KEY_FCM_TOKEN_SENT = "fcm_token_sent"
        private const val KEY_CRASH_DETECTION = "crash_detection_enabled"
        private const val KEY_EMERGENCY_NAME = "emergency_contact_name"
        private const val KEY_EMERGENCY_PHONE = "emergency_contact_phone"
    }
}
