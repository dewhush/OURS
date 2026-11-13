package app.ours.data.local

import app.ours.data.api.CheckInDto
import app.ours.data.api.OursApiClient
import app.ours.data.api.ReplyDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class UploadManager(
    private val db: OursDatabase,
    private val api: OursApiClient,
    private val prefs: PrefsStore,
) {
    private val mutex = Mutex()

    suspend fun enqueueAndUploadCheckIn(
        photoFile: File,
        note: String?,
        lat: Double?,
        lon: Double?,
        placeName: String?,
    ): Result<CheckInDto?> = withContext(Dispatchers.IO) {
        val rowId = db.enqueueUpload(
            kind = KIND_CHECKIN,
            filePath = photoFile.absolutePath,
            noteOrText = note,
            lat = lat,
            lon = lon,
            placeName = placeName,
        )
        runCatching {
            val created = api.createCheckIn(photoFile, note, lat, lon, placeName)
            db.deleteUpload(rowId)
            photoFile.delete()
            created
        }.recoverCatching { err ->
            val nextAttempts = 1
            if (UploadRetryPolicy.shouldGiveUp(nextAttempts, err)) {
                db.deleteUpload(rowId)
                photoFile.delete()
                throw err
            } else {
                db.markUploadRetry(
                    rowId,
                    nextAttempts,
                    System.currentTimeMillis() + UploadRetryPolicy.nextDelayMs(nextAttempts),
                )
                null // Queued for retry
            }
        }
    }

    suspend fun enqueueAndUploadReply(
        checkInId: Long,
        text: String?,
        photoFile: File?,
    ): Result<ReplyDto?> = withContext(Dispatchers.IO) {
        val rowId = db.enqueueUpload(
            kind = KIND_REPLY,
            checkinId = checkInId,
            filePath = photoFile?.absolutePath,
            noteOrText = text,
        )
        runCatching {
            val created = api.createReply(checkInId, text, photoFile)
            db.deleteUpload(rowId)
            photoFile?.delete()
            created
        }.recoverCatching { err ->
            val nextAttempts = 1
            if (UploadRetryPolicy.shouldGiveUp(nextAttempts, err)) {
                db.deleteUpload(rowId)
                photoFile?.delete()
                throw err
            } else {
                db.markUploadRetry(
                    rowId,
                    nextAttempts,
                    System.currentTimeMillis() + UploadRetryPolicy.nextDelayMs(nextAttempts),
                )
                null
            }
        }
    }

    suspend fun flushDueUploads(): Int = mutex.withLock {
        if (!prefs.isPaired) return@withLock 0
        var uploaded = 0
        val due = withContext(Dispatchers.IO) { db.dueUploads() }
        for (row in due) {
            val file = row.filePath?.let { File(it) }
            val result = runCatching {
                when (row.kind) {
                    KIND_CHECKIN -> {
                        if (file == null || !file.exists()) {
                            db.deleteUpload(row.id)
                            return@runCatching false
                        }
                        api.createCheckIn(file, row.noteOrText, row.lat, row.lon, row.placeName)
                        true
                    }
                    KIND_REPLY -> {
                        val cid = row.checkinId ?: return@runCatching false
                        api.createReply(cid, row.noteOrText, file?.takeIf { it.exists() })
                        true
                    }
                    else -> false
                }
            }
            result.onSuccess { ok ->
                db.deleteUpload(row.id)
                file?.delete()
                if (ok) uploaded++
            }.onFailure { err ->
                val attempts = row.attempts + 1
                if (UploadRetryPolicy.shouldGiveUp(attempts, err)) {
                    db.deleteUpload(row.id)
                    file?.delete()
                } else {
                    db.markUploadRetry(
                        row.id,
                        attempts,
                        System.currentTimeMillis() + UploadRetryPolicy.nextDelayMs(attempts),
                    )
                }
            }
        }
        uploaded
    }

    companion object {
        const val KIND_CHECKIN = "checkin"
        const val KIND_REPLY = "reply"
    }
}
