package app.ours.data.api

import android.content.Context
import app.ours.data.local.PrefsStore
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String,
) : IOException(message)

class OursApiClient(
    private val prefs: PrefsStore,
    private val onSessionExpired: () -> Unit = {},
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val jpegMediaType = "image/jpeg".toMediaType()
    private val refreshMutex = Mutex()

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun createImageLoader(context: Context): ImageLoader {
        val authedClient = okHttp.newBuilder()
            .addInterceptor { chain ->
                val token = prefs.accessToken
                val req = if (!token.isNullOrBlank()) {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(req)
            }
            .build()
        return ImageLoader.Builder(context.applicationContext)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { authedClient }))
            }
            .build()
    }

    fun photoUrl(photoId: String, thumb: Boolean): String {
        val base = prefs.serverUrl.trimEnd('/')
        val size = if (thumb) "thumb" else "full"
        return "$base/api/photos/$photoId?size=$size"
    }

    suspend fun health(serverUrl: String = prefs.serverUrl): HealthResponse {
        val base = serverUrl.trim().trimEnd('/')
        val req = Request.Builder().url("$base/health").get().build()
        return executeUnauthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun createPair(
        serverUrl: String,
        setupKey: String,
        name: String,
    ): PairCreateResponse {
        val base = serverUrl.trim().trimEnd('/')
        val payload = OursJson.encodeToString(PairCreateRequest(name.trim()))
        val req = Request.Builder()
            .url("$base/api/pair/create")
            .header("X-Setup-Key", setupKey.trim())
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeUnauthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun getInvite(): InviteResponse {
        val req = Request.Builder()
            .url("${base()}/api/pair/invite")
            .get()
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun joinPair(
        serverUrl: String,
        code: String,
        name: String,
    ): PairJoinResponse {
        val base = serverUrl.trim().trimEnd('/')
        val payload = OursJson.encodeToString(PairJoinRequest(code.trim(), name.trim()))
        val req = Request.Builder()
            .url("$base/api/pair/join")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeUnauthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun me(): MeResponse {
        val req = Request.Builder().url("${base()}/api/me").get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun registerPushToken(token: String) {
        val payload = OursJson.encodeToString(PushTokenRequest(token))
        val req = Request.Builder()
            .url("${base()}/api/push-token")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        executeAuthedUnit(req)
    }

    suspend fun uploadLocations(
        points: List<LocationPointDto>,
        afterEventId: Long?,
    ): LocationsUploadResponse {
        val query = if (afterEventId != null) "?after_event_id=$afterEventId" else ""
        val payload = OursJson.encodeToString(points)
        val req = Request.Builder()
            .url("${base()}/api/locations$query")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun partnerLocation(): PartnerLocationDto {
        val req = Request.Builder().url("${base()}/api/partner/location").get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun partnerHistory(sinceMs: Long, untilMs: Long? = null): HistoryResponse {
        val url = buildString {
            append("${base()}/api/partner/history?since=$sinceMs")
            if (untilMs != null) append("&until=$untilMs")
        }
        val req = Request.Builder().url(url).get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun setSharingPaused(paused: Boolean): SharingResponse {
        val payload = OursJson.encodeToString(SharingRequest(paused))
        val req = Request.Builder()
            .url("${base()}/api/sharing")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun getPlaces(): PlacesResponse {
        val req = Request.Builder().url("${base()}/api/places").get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun createPlace(name: String, lat: Double, lon: Double, radiusM: Double): PlaceDto {
        val payload = OursJson.encodeToString(CreatePlaceRequest(name.trim(), lat, lon, radiusM))
        val req = Request.Builder()
            .url("${base()}/api/places")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun deletePlace(id: Long) {
        val req = Request.Builder().url("${base()}/api/places/$id").delete().build()
        executeAuthedUnit(req)
    }

    suspend fun getCheckIns(): CheckInsResponse {
        val req = Request.Builder().url("${base()}/api/checkins").get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun createCheckIn(
        photoFile: File,
        note: String?,
        lat: Double?,
        lon: Double?,
        placeName: String?,
    ): CheckInDto {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", photoFile.name, photoFile.asRequestBody(jpegMediaType))
            .apply {
                if (!note.isNullOrBlank()) addFormDataPart("note", note.trim())
                if (lat != null && lon != null) {
                    addFormDataPart("lat", lat.toString())
                    addFormDataPart("lon", lon.toString())
                }
                if (!placeName.isNullOrBlank()) addFormDataPart("place_name", placeName.trim())
            }
            .build()
        val req = Request.Builder()
            .url("${base()}/api/checkins")
            .post(multipart)
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun deleteCheckIn(id: Long) {
        val req = Request.Builder().url("${base()}/api/checkins/$id").delete().build()
        executeAuthedUnit(req)
    }

    suspend fun setHighlight(id: Long, highlighted: Boolean): CheckInDto {
        val builder = Request.Builder().url("${base()}/api/checkins/$id/highlight")
        val req = if (highlighted) {
            builder.post(ByteArray(0).toRequestBody(null)).build()
        } else {
            builder.delete().build()
        }
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun createReply(
        checkInId: Long,
        text: String?,
        photoFile: File?,
    ): ReplyDto {
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                if (!text.isNullOrBlank()) addFormDataPart("text", text.trim())
                if (photoFile != null) {
                    addFormDataPart("photo", photoFile.name, photoFile.asRequestBody(jpegMediaType))
                }
            }
            .build()
        val req = Request.Builder()
            .url("${base()}/api/checkins/$checkInId/replies")
            .post(multipart)
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun getMemories(beforeMs: Long? = null, limit: Int = 50): MemoriesResponse {
        val url = buildString {
            append("${base()}/api/memories?limit=$limit")
            if (beforeMs != null) append("&before=$beforeMs")
        }
        val req = Request.Builder().url(url).get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun getMessages(beforeMs: Long? = null, limit: Int = 50): MessagesResponse {
        val url = buildString {
            append("${base()}/api/messages?limit=$limit")
            if (beforeMs != null) append("&before=$beforeMs")
        }
        val req = Request.Builder().url(url).get().build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun sendMessage(
        text: String?,
        photoFile: File? = null,
        replyToMessageId: Long? = null,
        replyToCheckInId: Long? = null,
    ): ChatMessageDto {
        val req = if (photoFile == null) {
            val payload = OursJson.encodeToString(
                CreateMessageRequest(
                    text = text.orEmpty().trim(),
                    replyToMessageId = replyToMessageId,
                    replyToCheckInId = replyToCheckInId,
                )
            )
            Request.Builder()
                .url("${base()}/api/messages")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
        } else {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("photo", photoFile.name, photoFile.asRequestBody(jpegMediaType))
                .apply {
                    if (!text.isNullOrBlank()) addFormDataPart("text", text.trim())
                    if (replyToMessageId != null) addFormDataPart("reply_to_message_id", replyToMessageId.toString())
                    if (replyToCheckInId != null) addFormDataPart("reply_to_checkin_id", replyToCheckInId.toString())
                }
                .build()
            Request.Builder()
                .url("${base()}/api/messages")
                .post(multipart)
                .build()
        }
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    suspend fun nudge() {
        val req = Request.Builder()
            .url("${base()}/api/nudge")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        executeAuthedUnit(req)
    }

    suspend fun getEvents(afterId: Long): EventsResponse {
        val req = Request.Builder()
            .url("${base()}/api/events?after=$afterId")
            .get()
            .build()
        return executeAuthed(req) { body -> OursJson.decodeFromString(body) }
    }

    fun openWebSocket(listener: WebSocketListener): WebSocket? {
        val token = prefs.accessToken ?: return null
        val httpUrl = "${base()}/api/ws".toHttpUrlOrNull() ?: return null
        val wsUrl = httpUrl.newBuilder()
            .addQueryParameter("token", token)
            .build()
        val req = Request.Builder().url(wsUrl).build()
        return okHttp.newWebSocket(req, listener)
    }

    private fun base(): String {
        val url = prefs.serverUrl.trim().trimEnd('/')
        if (url.isBlank()) throw ApiException(0, "no_server", "Server URL is not set")
        return url
    }

    private suspend fun <T> executeUnauthed(request: Request, decode: (String) -> T): T =
        withContext(Dispatchers.IO) {
            okHttp.newCall(request).execute().use { resp ->
                val body = resp.body.string()
                if (!resp.isSuccessful) throw parseError(resp.code, body)
                decode(body)
            }
        }

    private suspend fun <T> executeAuthed(request: Request, decode: (String) -> T): T =
        withContext(Dispatchers.IO) {
            val tokenUsed = prefs.accessToken ?: throw ApiException(401, "unauthorized", "Not signed in")
            val firstReq = request.newBuilder().header("Authorization", "Bearer $tokenUsed").build()
            okHttp.newCall(firstReq).execute().use { resp ->
                if (resp.code != 401) {
                    val body = resp.body.string()
                    if (!resp.isSuccessful) throw parseError(resp.code, body)
                    return@withContext decode(body)
                }
            }
            val newToken = refreshAccessToken(tokenUsed)
            val retryReq = request.newBuilder().header("Authorization", "Bearer $newToken").build()
            okHttp.newCall(retryReq).execute().use { resp ->
                val body = resp.body.string()
                if (!resp.isSuccessful) throw parseError(resp.code, body)
                decode(body)
            }
        }

    private suspend fun executeAuthedUnit(request: Request) {
        executeAuthed(request) { Unit }
    }

    private suspend fun refreshAccessToken(failedToken: String): String = refreshMutex.withLock {
        val current = prefs.accessToken
        if (current != null && current != failedToken) {
            return@withLock current
        }
        val refresh = prefs.refreshToken
            ?: run {
                onSessionExpired()
                throw ApiException(401, "invalid_refresh_token", "Session expired")
            }
        val payload = OursJson.encodeToString(RefreshRequest(refresh))
        val req = Request.Builder()
            .url("${base()}/api/auth/refresh")
            .post(payload.toRequestBody(jsonMediaType))
            .build()
        okHttp.newCall(req).execute().use { resp ->
            val body = resp.body.string()
            if (!resp.isSuccessful) {
                val err = parseError(resp.code, body)
                if (err.errorCode == "invalid_refresh_token" || resp.code == 401) {
                    onSessionExpired()
                }
                throw err
            }
            val tokens = OursJson.decodeFromString<TokensDto>(body)
            prefs.saveTokens(tokens)
            return@withLock tokens.accessToken
        }
    }

    private fun parseError(code: Int, body: String): ApiException {
        val parsed = runCatching { OursJson.decodeFromString<ApiErrorBody>(body) }.getOrNull()
        return ApiException(
            statusCode = code,
            errorCode = parsed?.error ?: "http_$code",
            message = parsed?.message ?: "Request failed ($code)",
        )
    }
}
