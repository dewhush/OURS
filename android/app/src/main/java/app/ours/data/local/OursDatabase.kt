package app.ours.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import app.ours.data.api.CheckInDto
import app.ours.data.api.LocationPointDto
import app.ours.data.api.OursJson
import app.ours.data.api.PartnerLocationDto
import app.ours.data.api.PlaceDto
import kotlinx.serialization.encodeToString

data class QueuedLocationRow(
    val id: Long,
    val point: LocationPointDto,
)

data class QueuedUploadRow(
    val id: Long,
    val kind: String, // "checkin" or "reply"
    val checkinId: Long?,
    val filePath: String?,
    val noteOrText: String?,
    val lat: Double?,
    val lon: Double?,
    val placeName: String?,
    val attempts: Int,
    val nextRetryAt: Long,
    val createdAt: Long,
)

class OursDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE location_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                accuracy_m REAL NOT NULL,
                speed_mps REAL NOT NULL,
                bearing REAL,
                battery_pct INTEGER NOT NULL,
                charging INTEGER NOT NULL,
                recorded_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE upload_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                kind TEXT NOT NULL,
                checkin_id INTEGER,
                file_path TEXT,
                note_or_text TEXT,
                lat REAL,
                lon REAL,
                place_name TEXT,
                attempts INTEGER NOT NULL DEFAULT 0,
                next_retry_at INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_checkins (
                id INTEGER PRIMARY KEY,
                json_payload TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                highlighted INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_places (
                id INTEGER PRIMARY KEY,
                json_payload TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE cached_partner_location (
                id INTEGER PRIMARY KEY,
                json_payload TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // --- Location queue ---

    fun enqueueLocation(point: LocationPointDto): Long {
        val cv = ContentValues().apply {
            put("lat", point.lat)
            put("lon", point.lon)
            put("accuracy_m", point.accuracyM)
            put("speed_mps", point.speedMps)
            if (point.bearing != null) put("bearing", point.bearing) else putNull("bearing")
            put("battery_pct", point.batteryPct)
            put("charging", if (point.charging) 1 else 0)
            put("recorded_at", point.recordedAt)
        }
        return writableDatabase.insert("location_queue", null, cv)
    }

    fun peekLocations(limit: Int = 100): List<QueuedLocationRow> {
        val result = mutableListOf<QueuedLocationRow>()
        readableDatabase.rawQuery(
            "SELECT id, lat, lon, accuracy_m, speed_mps, bearing, battery_pct, charging, recorded_at " +
                "FROM location_queue ORDER BY recorded_at ASC, id ASC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val bearing = if (c.isNull(5)) null else c.getDouble(5)
                result += QueuedLocationRow(
                    id = c.getLong(0),
                    point = LocationPointDto(
                        lat = c.getDouble(1),
                        lon = c.getDouble(2),
                        accuracyM = c.getDouble(3),
                        speedMps = c.getDouble(4),
                        bearing = bearing,
                        batteryPct = c.getInt(6),
                        charging = c.getInt(7) != 0,
                        recordedAt = c.getLong(8),
                    )
                )
            }
        }
        return result
    }

    fun deleteLocations(ids: List<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        writableDatabase.delete(
            "location_queue",
            "id IN ($placeholders)",
            ids.map { it.toString() }.toTypedArray()
        )
    }

    fun locationQueueCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM location_queue", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    // --- Upload queue (check-ins & replies with retry) ---

    fun enqueueUpload(
        kind: String,
        checkinId: Long? = null,
        filePath: String? = null,
        noteOrText: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        placeName: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        val cv = ContentValues().apply {
            put("kind", kind)
            if (checkinId != null) put("checkin_id", checkinId) else putNull("checkin_id")
            if (filePath != null) put("file_path", filePath) else putNull("file_path")
            if (noteOrText != null) put("note_or_text", noteOrText) else putNull("note_or_text")
            if (lat != null) put("lat", lat) else putNull("lat")
            if (lon != null) put("lon", lon) else putNull("lon")
            if (placeName != null) put("place_name", placeName) else putNull("place_name")
            put("attempts", 0)
            put("next_retry_at", 0L)
            put("created_at", nowMs)
        }
        return writableDatabase.insert("upload_queue", null, cv)
    }

    fun dueUploads(nowMs: Long = System.currentTimeMillis()): List<QueuedUploadRow> {
        val rows = mutableListOf<QueuedUploadRow>()
        readableDatabase.rawQuery(
            "SELECT id, kind, checkin_id, file_path, note_or_text, lat, lon, place_name, attempts, next_retry_at, created_at " +
                "FROM upload_queue WHERE next_retry_at <= ? ORDER BY created_at ASC, id ASC",
            arrayOf(nowMs.toString())
        ).use { c ->
            while (c.moveToNext()) {
                rows += QueuedUploadRow(
                    id = c.getLong(0),
                    kind = c.getString(1),
                    checkinId = if (c.isNull(2)) null else c.getLong(2),
                    filePath = if (c.isNull(3)) null else c.getString(3),
                    noteOrText = if (c.isNull(4)) null else c.getString(4),
                    lat = if (c.isNull(5)) null else c.getDouble(5),
                    lon = if (c.isNull(6)) null else c.getDouble(6),
                    placeName = if (c.isNull(7)) null else c.getString(7),
                    attempts = c.getInt(8),
                    nextRetryAt = c.getLong(9),
                    createdAt = c.getLong(10),
                )
            }
        }
        return rows
    }

    fun pendingUploadCount(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM upload_queue", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun markUploadRetry(id: Long, attempts: Int, nextRetryAt: Long) {
        val cv = ContentValues().apply {
            put("attempts", attempts)
            put("next_retry_at", nextRetryAt)
        }
        writableDatabase.update("upload_queue", cv, "id = ?", arrayOf(id.toString()))
    }

    fun deleteUpload(id: Long) {
        writableDatabase.delete("upload_queue", "id = ?", arrayOf(id.toString()))
    }

    // --- Caches ---

    fun replaceCheckInsCache(items: List<CheckInDto>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("cached_checkins", null, null)
            for (item in items) {
                val cv = ContentValues().apply {
                    put("id", item.id)
                    put("json_payload", OursJson.encodeToString(item))
                    put("created_at", item.createdAt)
                    put("expires_at", item.expiresAt)
                    put("highlighted", if (item.highlighted) 1 else 0)
                }
                db.insert("cached_checkins", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadCheckInsCache(nowMs: Long = System.currentTimeMillis()): List<CheckInDto> {
        val items = mutableListOf<CheckInDto>()
        readableDatabase.rawQuery(
            "SELECT json_payload FROM cached_checkins WHERE expires_at > ? ORDER BY created_at DESC",
            arrayOf(nowMs.toString())
        ).use { c ->
            while (c.moveToNext()) {
                runCatching { OursJson.decodeFromString<CheckInDto>(c.getString(0)) }
                    .onSuccess { items += it }
            }
        }
        return items
    }

    fun replacePlacesCache(places: List<PlaceDto>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("cached_places", null, null)
            for (p in places) {
                val cv = ContentValues().apply {
                    put("id", p.id)
                    put("json_payload", OursJson.encodeToString(p))
                }
                db.insert("cached_places", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadPlacesCache(): List<PlaceDto> {
        val places = mutableListOf<PlaceDto>()
        readableDatabase.rawQuery("SELECT json_payload FROM cached_places", null).use { c ->
            while (c.moveToNext()) {
                runCatching { OursJson.decodeFromString<PlaceDto>(c.getString(0)) }
                    .onSuccess { places += it }
            }
        }
        return places
    }

    fun savePartnerLocationCache(loc: PartnerLocationDto, nowMs: Long = System.currentTimeMillis()) {
        val cv = ContentValues().apply {
            put("id", 1)
            put("json_payload", OursJson.encodeToString(loc))
            put("updated_at", nowMs)
        }
        writableDatabase.insertWithOnConflict("cached_partner_location", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun loadPartnerLocationCache(): PartnerLocationDto? {
        readableDatabase.rawQuery("SELECT json_payload FROM cached_partner_location WHERE id = 1", null).use { c ->
            if (c.moveToFirst()) {
                return runCatching { OursJson.decodeFromString<PartnerLocationDto>(c.getString(0)) }.getOrNull()
            }
        }
        return null
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete("location_queue", null, null)
        db.delete("upload_queue", null, null)
        db.delete("cached_checkins", null, null)
        db.delete("cached_places", null, null)
        db.delete("cached_partner_location", null, null)
    }

    companion object {
        private const val DB_NAME = "ours.db"
        private const val DB_VERSION = 1
    }
}
