package app.ours.push

import android.content.Context
import app.ours.BuildConfig
import app.ours.OursApp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FirebaseBootstrap {
    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            BuildConfig.FIREBASE_SENDER_ID.isNotBlank()

    fun initIfConfigured(context: Context) {
        if (!isConfigured()) return
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
        }
    }

    fun syncTokenIfNeeded(app: OursApp) {
        if (!isConfigured() || !app.prefs.isPaired) return
        initIfConfigured(app)
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank() && token != app.prefs.fcmTokenSent) {
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            app.api.registerPushToken(token)
                            app.prefs.fcmTokenSent = token
                        }
                    }
                }
            }
        }
    }
}
