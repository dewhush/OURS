package app.ours

import android.app.Application
import app.ours.data.api.OursApiClient
import app.ours.data.local.OursDatabase
import app.ours.data.local.PrefsStore
import app.ours.data.local.UploadManager
import app.ours.notifications.OursNotifier
import app.ours.push.FirebaseBootstrap
import coil3.ImageLoader
import coil3.SingletonImageLoader
import kotlinx.coroutines.flow.MutableSharedFlow
import org.maplibre.android.MapLibre

class OursApp : Application(), SingletonImageLoader.Factory {

    lateinit var prefs: PrefsStore
        private set
    lateinit var db: OursDatabase
        private set
    lateinit var api: OursApiClient
        private set
    lateinit var uploadManager: UploadManager
        private set

    val sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        OursNotifier.ensureChannels(this)
        FirebaseBootstrap.initIfConfigured(this)

        prefs = PrefsStore(this)
        db = OursDatabase(this)
        api = OursApiClient(
            prefs = prefs,
            onSessionExpired = {
                prefs.clearSession()
                db.clearAll()
                sessionExpiredEvents.tryEmit(Unit)
            },
        )
        uploadManager = UploadManager(db, api, prefs)
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return api.createImageLoader(context)
    }
}
