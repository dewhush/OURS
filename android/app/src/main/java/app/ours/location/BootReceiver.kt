package app.ours.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.ours.data.local.PrefsStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val prefs = PrefsStore(context)
            if (prefs.isPaired && !prefs.sharingPaused) {
                LocationTrackingService.start(context)
                WatchdogScheduler.schedule(context)
            }
        }
    }
}
