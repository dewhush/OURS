package app.ours.push

import app.ours.OursApp
import app.ours.location.LocationTrackingService
import app.ours.notifications.OursNotifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OursMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val app = application as OursApp
        if (!app.prefs.isPaired) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                app.api.registerPushToken(token)
                app.prefs.fcmTokenSent = token
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        val app = application as OursApp
        if (!app.prefs.isPaired) return

        when (type) {
            "wake" -> {
                if (!app.prefs.sharingPaused) {
                    LocationTrackingService.start(this, wakeFix = true)
                }
            }
            "nudge" -> {
                OursNotifier.buzzPhone(this)
                val title = data["title"] ?: "Thinking of you"
                val body = data["body"] ?: "Your partner sent a nudge."
                val id = data["event_id"]?.toIntOrNull() ?: 9001
                OursNotifier.notify(this, OursNotifier.CHANNEL_NUDGE, id, title, body)
            }
            "checkin", "reply", "place", "highlight" -> {
                val title = data["title"] ?: "OURS"
                val body = data["body"] ?: "New update"
                val id = data["event_id"]?.toIntOrNull() ?: System.currentTimeMillis().toInt()
                OursNotifier.notify(this, OursNotifier.CHANNEL_UPDATES, id, title, body)
            }
        }
    }
}
