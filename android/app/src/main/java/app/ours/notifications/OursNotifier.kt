package app.ours.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import app.ours.MainActivity
import app.ours.R
import app.ours.data.api.EventChatMessageData
import app.ours.data.api.EventDto
import app.ours.data.api.EventHighlightData
import app.ours.data.api.EventPlaceData
import app.ours.data.api.EventSharingData
import app.ours.data.api.OursJson
import kotlinx.serialization.json.decodeFromJsonElement

object OursNotifier {
    const val CHANNEL_LOCATION = "ours_location_service"
    const val CHANNEL_UPDATES = "ours_updates"
    const val CHANNEL_NUDGE = "ours_nudge"
    const val FOREGROUND_NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val locationChannel = NotificationChannel(
            CHANNEL_LOCATION,
            "Live location sharing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps your live location updated for your partner."
            setShowBadge(false)
        }
        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            "Check-ins, replies & places",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Partner check-ins, replies, highlights, and place alerts."
        }
        val nudgeChannel = NotificationChannel(
            CHANNEL_NUDGE,
            "Thinking of you",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Haptic buzzes from your partner."
            enableVibration(true)
        }
        nm.createNotificationChannel(locationChannel)
        nm.createNotificationChannel(updatesChannel)
        nm.createNotificationChannel(nudgeChannel)
    }

    fun buildForegroundNotification(context: Context, paused: Boolean): Notification {
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (paused) "Location sharing is paused" else "Sharing live location with your partner"
        return NotificationCompat.Builder(context, CHANNEL_LOCATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("OURS")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .setSilent(true)
            .build()
    }

    fun handleEvent(context: Context, event: EventDto, partnerName: String?) {
        val partner = partnerName ?: "Partner"
        when (event.type) {
            "checkin" -> notify(
                context,
                CHANNEL_UPDATES,
                event.id.toInt(),
                "New check-in from $partner",
                "Tap to see what they shared."
            )
            "reply" -> notify(
                context,
                CHANNEL_UPDATES,
                event.id.toInt(),
                "$partner replied",
                "Tap to read their reply."
            )
            "chat_message" -> {
                val data = runCatching { OursJson.decodeFromJsonElement<EventChatMessageData>(event.data) }.getOrNull()
                val text = data?.message?.text ?: "Sent a photo"
                notifyMessaging(
                    context = context,
                    id = event.id.toInt(),
                    senderName = partner,
                    text = text,
                )
            }
            "highlight" -> {
                val data = runCatching { OursJson.decodeFromJsonElement<EventHighlightData>(event.data) }.getOrNull()
                if (data?.highlighted == true) {
                    notify(
                        context,
                        CHANNEL_UPDATES,
                        event.id.toInt(),
                        "$partner saved a highlight",
                        "Added to your shared memories."
                    )
                }
            }
            "place" -> {
                val data = runCatching { OursJson.decodeFromJsonElement<EventPlaceData>(event.data) }.getOrNull()
                if (data != null) {
                    val verb = if (data.transition == "enter") "arrived at" else "left"
                    notify(
                        context,
                        CHANNEL_UPDATES,
                        event.id.toInt(),
                        "$partner $verb ${data.placeName}",
                        "Place alert"
                    )
                }
            }
            "sharing" -> {
                val data = runCatching { OursJson.decodeFromJsonElement<EventSharingData>(event.data) }.getOrNull()
                if (data != null) {
                    val msg = if (data.paused) "$partner paused location sharing" else "$partner resumed location sharing"
                    notify(context, CHANNEL_UPDATES, event.id.toInt(), "OURS", msg)
                }
            }
            "nudge" -> {
                buzzPhone(context)
                notify(
                    context,
                    CHANNEL_NUDGE,
                    event.id.toInt(),
                    "Thinking of you",
                    "$partner sent a nudge."
                )
            }
        }
    }

    fun notifyMessaging(context: Context, id: Int, senderName: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val sender = Person.Builder().setName(senderName).build()
        val style = NotificationCompat.MessagingStyle(sender)
            .addMessage(text, System.currentTimeMillis(), sender)

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(0xFFD95D39.toInt())
            .setSubText("OURS")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(pi)
        applyShortCriticalTextIfSupported(builder, senderName)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun notify(context: Context, channelId: String, id: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(0xFFD95D39.toInt())
            .setSubText("OURS")
            .setCategory(
                if (channelId == CHANNEL_NUDGE) NotificationCompat.CATEGORY_SOCIAL
                else NotificationCompat.CATEGORY_STATUS
            )
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
        applyShortCriticalTextIfSupported(builder, title.take(16))
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun applyShortCriticalTextIfSupported(
        builder: NotificationCompat.Builder,
        shortText: String,
    ) {
        if (Build.VERSION.SDK_INT >= 36) {
            runCatching {
                val method = builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                method.invoke(builder, shortText)
            }
        }
    }

    fun buzzPhone(context: Context) {
        val pattern = longArrayOf(0, 120, 80, 180)
        if (Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vib?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}
