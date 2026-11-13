package app.ours.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ours.OursApp
import app.ours.data.api.ApiException
import app.ours.data.api.ChatMessageDto
import app.ours.data.api.CheckInDto
import app.ours.data.api.EventChatMessageData
import app.ours.data.api.EventCheckInData
import app.ours.data.api.EventDto
import app.ours.data.api.EventHighlightData
import app.ours.data.api.EventReplyData
import app.ours.data.api.EventSharingData
import app.ours.data.api.OursJson
import app.ours.data.api.PartnerLocationDto
import app.ours.data.api.PlaceDto
import app.ours.data.api.UserDto
import app.ours.location.LocationTrackingService
import app.ours.location.WatchdogScheduler
import app.ours.notifications.OursNotifier
import app.ours.push.FirebaseBootstrap
import app.ours.util.PhotoCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

enum class MainTab { Map, Memories, Chat, Settings }

data class OursUiState(
    val isPaired: Boolean = false,
    val onboardingDone: Boolean = false,
    val serverUrl: String = "",
    val inviteCode: String? = null,
    val me: UserDto? = null,
    val partner: UserDto? = null,
    val sharingPaused: Boolean = false,
    val partnerPaused: Boolean = false,
    val partnerLocation: PartnerLocationDto? = null,
    val checkIns: List<CheckInDto> = emptyList(),
    val checkInsLoading: Boolean = false,
    val checkInsError: String? = null,
    val chatMessages: List<ChatMessageDto> = emptyList(),
    val messagesNextBefore: Long? = null,
    val messagesLoading: Boolean = false,
    val memories: List<CheckInDto> = emptyList(),
    val memoriesNextBefore: Long? = null,
    val memoriesLoading: Boolean = false,
    val memoriesError: String? = null,
    val places: List<PlaceDto> = emptyList(),
    val pendingUploads: Int = 0,
    val wsConnected: Boolean = false,
    val activeTab: MainTab = MainTab.Map,
    val busy: Boolean = false,
    val confirmationMessage: String? = null,
    val errorMessage: String? = null,
)

class OursViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as OursApp
    private val prefs = app.prefs
    private val db = app.db
    private val api = app.api

    private val _state = MutableStateFlow(
        OursUiState(
            isPaired = prefs.isPaired,
            onboardingDone = prefs.onboardingDone,
            serverUrl = prefs.serverUrl,
            inviteCode = prefs.inviteCode,
            me = if (prefs.userId > 0 && prefs.userName != null) {
                UserDto(prefs.userId, prefs.userName!!, 0L)
            } else null,
            partner = if (prefs.partnerId != null && prefs.partnerName != null) {
                UserDto(prefs.partnerId!!, prefs.partnerName!!, 0L)
            } else null,
            sharingPaused = prefs.sharingPaused,
            partnerPaused = prefs.partnerPaused,
            partnerLocation = db.loadPartnerLocationCache(),
            checkIns = db.loadCheckInsCache(),
            places = db.loadPlacesCache(),
            pendingUploads = db.pendingUploadCount(),
        )
    )
    val state: StateFlow<OursUiState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private var wsReconnectJob: Job? = null
    private var confirmationJob: Job? = null
    private var isForeground = false

    init {
        viewModelScope.launch {
            app.sessionExpiredEvents.collect {
                disconnectWebSocket()
                LocationTrackingService.stop(app)
                _state.update {
                    OursUiState(
                        isPaired = false,
                        onboardingDone = false,
                        serverUrl = prefs.serverUrl,
                        errorMessage = "Your session expired. Please pair again.",
                    )
                }
            }
        }
        if (prefs.isPaired) {
            refreshAll()
        }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(activeTab = tab) }
        when (tab) {
            MainTab.Map -> refreshPartnerLocation()
            MainTab.Memories -> if (_state.value.memories.isEmpty()) refreshMemories()
            MainTab.Chat -> {
                refreshCheckIns()
                refreshMessages()
            }
            MainTab.Settings -> refreshMeAndPlaces()
        }
    }

    fun onForegroundChanged(foreground: Boolean) {
        isForeground = foreground
        if (foreground && prefs.isPaired && prefs.onboardingDone) {
            connectWebSocket()
            viewModelScope.launch {
                app.uploadManager.flushDueUploads()
                _state.update { it.copy(pendingUploads = db.pendingUploadCount()) }
            }
        } else if (!foreground) {
            disconnectWebSocket()
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun showConfirmation(msg: String) {
        confirmationJob?.cancel()
        _state.update { it.copy(confirmationMessage = msg) }
        confirmationJob = viewModelScope.launch {
            delay(2400)
            _state.update { it.copy(confirmationMessage = null) }
        }
    }

    // --- Onboarding & Pairing ---

    suspend fun testServerHealth(url: String): Result<String> = runCatching {
        val resp = api.health(url)
        prefs.serverUrl = url
        _state.update { it.copy(serverUrl = prefs.serverUrl) }
        "Connected (v${resp.version})"
    }

    fun createPair(serverUrl: String, setupKey: String, name: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, errorMessage = null) }
            runCatching {
                prefs.serverUrl = serverUrl
                api.createPair(serverUrl, setupKey, name)
            }.onSuccess { resp ->
                prefs.saveSelf(resp.user)
                prefs.saveTokens(resp.tokens)
                prefs.inviteCode = resp.inviteCode
                _state.update {
                    it.copy(
                        busy = false,
                        isPaired = true,
                        serverUrl = prefs.serverUrl,
                        me = resp.user,
                        inviteCode = resp.inviteCode,
                    )
                }
                FirebaseBootstrap.syncTokenIfNeeded(app)
                onSuccess(resp.inviteCode)
            }.onFailure { err ->
                _state.update { it.copy(busy = false, errorMessage = err.message ?: "Failed to create pair") }
            }
        }
    }

    fun joinPair(serverUrl: String, code: String, name: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, errorMessage = null) }
            runCatching {
                prefs.serverUrl = serverUrl
                api.joinPair(serverUrl, code, name)
            }.onSuccess { resp ->
                prefs.saveSelf(resp.user)
                prefs.saveTokens(resp.tokens)
                prefs.inviteCode = null
                _state.update {
                    it.copy(
                        busy = false,
                        isPaired = true,
                        serverUrl = prefs.serverUrl,
                        me = resp.user,
                        inviteCode = null,
                    )
                }
                FirebaseBootstrap.syncTokenIfNeeded(app)
                refreshAll()
                onSuccess()
            }.onFailure { err ->
                _state.update { it.copy(busy = false, errorMessage = err.message ?: "Failed to join pair") }
            }
        }
    }

    fun refreshInviteCode() {
        viewModelScope.launch {
            runCatching { api.getInvite() }
                .onSuccess { resp ->
                    prefs.inviteCode = resp.inviteCode
                    _state.update { it.copy(inviteCode = resp.inviteCode) }
                }
                .onFailure { err ->
                    if (err is ApiException && err.errorCode == "already_paired") {
                        prefs.inviteCode = null
                        _state.update { it.copy(inviteCode = null) }
                        refreshMeAndPlaces()
                    }
                }
        }
    }

    fun finishOnboarding() {
        prefs.onboardingDone = true
        _state.update { it.copy(onboardingDone = true) }
        if (prefs.isPaired && !prefs.sharingPaused) {
            LocationTrackingService.start(app)
            WatchdogScheduler.schedule(app)
        }
        if (isForeground) connectWebSocket()
        refreshAll()
    }

    // --- Data refresh ---

    fun refreshAll() {
        if (!prefs.isPaired) return
        refreshMeAndPlaces()
        refreshPartnerLocation()
        refreshCheckIns()
        refreshMessages()
        refreshMemories()
        FirebaseBootstrap.syncTokenIfNeeded(app)
    }

    fun refreshMeAndPlaces() {
        viewModelScope.launch {
            runCatching { api.me() }.onSuccess { meResp ->
                prefs.saveSelf(meResp.me)
                prefs.partnerId = meResp.partner?.id
                prefs.partnerName = meResp.partner?.name
                prefs.sharingPaused = meResp.sharingPaused
                prefs.partnerPaused = meResp.partnerPaused
                if (meResp.partner != null) prefs.inviteCode = null
                _state.update {
                    it.copy(
                        me = meResp.me,
                        partner = meResp.partner,
                        sharingPaused = meResp.sharingPaused,
                        partnerPaused = meResp.partnerPaused,
                        inviteCode = if (meResp.partner != null) null else it.inviteCode,
                    )
                }
            }
            runCatching { api.getPlaces() }.onSuccess { placesResp ->
                db.replacePlacesCache(placesResp.places)
                _state.update { it.copy(places = placesResp.places) }
            }
        }
    }

    fun refreshPartnerLocation() {
        viewModelScope.launch {
            runCatching { api.partnerLocation() }.onSuccess { loc ->
                db.savePartnerLocationCache(loc)
                prefs.partnerPaused = loc.paused
                _state.update { it.copy(partnerLocation = loc, partnerPaused = loc.paused) }
            }
        }
    }

    fun refreshCheckIns() {
        viewModelScope.launch {
            _state.update { it.copy(checkInsLoading = true, checkInsError = null) }
            runCatching { api.getCheckIns() }
                .onSuccess { resp ->
                    db.replaceCheckInsCache(resp.checkins)
                    _state.update {
                        it.copy(
                            checkIns = resp.checkins,
                            checkInsLoading = false,
                            checkInsError = null,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            checkInsLoading = false,
                            checkInsError = err.message ?: "Unable to load check-ins",
                        )
                    }
                }
        }
    }

    fun refreshMessages(loadMore: Boolean = false) {
        viewModelScope.launch {
            val before = if (loadMore) _state.value.messagesNextBefore else null
            if (loadMore && before == null) return@launch
            _state.update { it.copy(messagesLoading = true) }
            runCatching { api.getMessages(beforeMs = before, limit = 50) }
                .onSuccess { resp ->
                    _state.update { cur ->
                        val merged = if (loadMore) cur.chatMessages + resp.items else resp.items
                        cur.copy(
                            chatMessages = merged.distinctBy { it.id },
                            messagesNextBefore = resp.nextBefore,
                            messagesLoading = false,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(messagesLoading = false) }
                }
        }
    }

    fun sendChatMessage(
        text: String?,
        photoUri: Uri? = null,
        replyToMessageId: Long? = null,
        replyToCheckInId: Long? = null,
    ) {
        if (text.isNullOrBlank() && photoUri == null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, errorMessage = null) }
            val compressed = if (photoUri != null) {
                withContext(Dispatchers.IO) { PhotoCompressor.compressToUploadFile(app, photoUri) }
            } else null

            runCatching {
                api.sendMessage(
                    text = text,
                    photoFile = compressed,
                    replyToMessageId = replyToMessageId,
                    replyToCheckInId = replyToCheckInId,
                )
            }.onSuccess { msg ->
                compressed?.delete()
                _state.update { cur ->
                    val next = (listOf(msg) + cur.chatMessages).distinctBy { it.id }
                    cur.copy(busy = false, chatMessages = next)
                }
            }.onFailure { err ->
                compressed?.delete()
                _state.update { it.copy(busy = false, errorMessage = err.message ?: "Message failed") }
            }
        }
    }

    fun refreshMemories(loadMore: Boolean = false) {
        viewModelScope.launch {
            val before = if (loadMore) _state.value.memoriesNextBefore else null
            if (loadMore && before == null) return@launch
            _state.update { it.copy(memoriesLoading = true, memoriesError = null) }
            runCatching { api.getMemories(beforeMs = before, limit = 50) }
                .onSuccess { resp ->
                    _state.update { cur ->
                        val merged = if (loadMore) cur.memories + resp.items else resp.items
                        cur.copy(
                            memories = merged.distinctBy { it.id },
                            memoriesNextBefore = resp.nextBefore,
                            memoriesLoading = false,
                            memoriesError = null,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            memoriesLoading = false,
                            memoriesError = err.message ?: "Unable to load memories",
                        )
                    }
                }
        }
    }

    // --- Actions ---

    fun sendCheckIn(photoUri: Uri, note: String?, placeName: String?) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, errorMessage = null) }
            val compressed = withContext(Dispatchers.IO) {
                PhotoCompressor.compressToUploadFile(app, photoUri)
            }
            if (compressed == null) {
                _state.update { it.copy(busy = false, errorMessage = "Could not read photo") }
                return@launch
            }
            val lastLoc = withContext(Dispatchers.IO) { db.peekLocations(1).lastOrNull()?.point }
            val result = app.uploadManager.enqueueAndUploadCheckIn(
                photoFile = compressed,
                note = note?.takeIf { it.isNotBlank() },
                lat = lastLoc?.lat,
                lon = lastLoc?.lon,
                placeName = placeName?.takeIf { it.isNotBlank() },
            )
            _state.update { it.copy(busy = false, pendingUploads = db.pendingUploadCount()) }
            result.onSuccess { created ->
                if (created != null) {
                    _state.update { cur ->
                        val updated = listOf(created) + cur.checkIns.filterNot { it.id == created.id }
                        db.replaceCheckInsCache(updated)
                        cur.copy(checkIns = updated)
                    }
                    showConfirmation("Check-in sent")
                } else {
                    showConfirmation("Queued — will upload when online")
                }
            }.onFailure { err ->
                _state.update { it.copy(errorMessage = err.message ?: "Check-in failed") }
            }
        }
    }

    fun sendReply(checkInId: Long, text: String?, photoUri: Uri?) {
        if (text.isNullOrBlank() && photoUri == null) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, errorMessage = null) }
            val compressed = if (photoUri != null) {
                withContext(Dispatchers.IO) { PhotoCompressor.compressToUploadFile(app, photoUri) }
            } else null

            val result = app.uploadManager.enqueueAndUploadReply(checkInId, text, compressed)
            _state.update { it.copy(busy = false, pendingUploads = db.pendingUploadCount()) }
            result.onSuccess { reply ->
                if (reply != null) {
                    _state.update { cur ->
                        val updated = cur.checkIns.map { c ->
                            if (c.id == checkInId) c.copy(replies = c.replies + reply) else c
                        }
                        db.replaceCheckInsCache(updated)
                        cur.copy(checkIns = updated)
                    }
                    showConfirmation("Reply sent")
                } else {
                    showConfirmation("Reply queued")
                }
            }.onFailure { err ->
                _state.update { it.copy(errorMessage = err.message ?: "Reply failed") }
            }
        }
    }

    fun toggleHighlight(checkIn: CheckInDto) {
        viewModelScope.launch {
            val target = !checkIn.highlighted
            runCatching { api.setHighlight(checkIn.id, target) }
                .onSuccess { updated ->
                    _state.update { cur ->
                        val nextCheckIns = cur.checkIns.map { if (it.id == updated.id) updated else it }
                        db.replaceCheckInsCache(nextCheckIns)
                        val nextMemories = if (updated.highlighted) {
                            listOf(updated) + cur.memories.filterNot { it.id == updated.id }
                        } else {
                            cur.memories.filterNot { it.id == updated.id }
                        }
                        cur.copy(checkIns = nextCheckIns, memories = nextMemories)
                    }
                    showConfirmation(if (target) "Saved to Memories" else "Removed from Memories")
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not update highlight") }
                }
        }
    }

    fun deleteCheckIn(checkInId: Long) {
        viewModelScope.launch {
            runCatching { api.deleteCheckIn(checkInId) }
                .onSuccess {
                    _state.update { cur ->
                        val next = cur.checkIns.filterNot { it.id == checkInId }
                        db.replaceCheckInsCache(next)
                        cur.copy(
                            checkIns = next,
                            memories = cur.memories.filterNot { it.id == checkInId },
                        )
                    }
                    showConfirmation("Check-in deleted")
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not delete check-in") }
                }
        }
    }

    fun sendNudge() {
        viewModelScope.launch {
            runCatching { api.nudge() }
                .onSuccess { showConfirmation("Nudge sent") }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not send nudge") }
                }
        }
    }

    fun toggleSharingPaused(paused: Boolean) {
        viewModelScope.launch {
            runCatching { api.setSharingPaused(paused) }
                .onSuccess { resp ->
                    prefs.sharingPaused = resp.paused
                    _state.update { it.copy(sharingPaused = resp.paused) }
                    if (resp.paused) {
                        LocationTrackingService.stop(app)
                        WatchdogScheduler.cancel(app)
                        showConfirmation("Sharing paused")
                    } else {
                        LocationTrackingService.start(app)
                        WatchdogScheduler.schedule(app)
                        showConfirmation("Sharing resumed")
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not update sharing") }
                }
        }
    }

    fun addPlace(name: String, lat: Double, lon: Double, radiusM: Double) {
        viewModelScope.launch {
            runCatching { api.createPlace(name, lat, lon, radiusM) }
                .onSuccess { place ->
                    _state.update { cur ->
                        val next = cur.places + place
                        db.replacePlacesCache(next)
                        cur.copy(places = next)
                    }
                    showConfirmation("Place saved")
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not save place") }
                }
        }
    }

    fun deletePlace(id: Long) {
        viewModelScope.launch {
            runCatching { api.deletePlace(id) }
                .onSuccess {
                    _state.update { cur ->
                        val next = cur.places.filterNot { it.id == id }
                        db.replacePlacesCache(next)
                        cur.copy(places = next)
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(errorMessage = err.message ?: "Could not delete place") }
                }
        }
    }

    fun updateServerUrl(url: String) {
        prefs.serverUrl = url
        _state.update { it.copy(serverUrl = prefs.serverUrl) }
        disconnectWebSocket()
        if (isForeground && prefs.isPaired) connectWebSocket()
        showConfirmation("Server URL updated")
    }

    fun signOut() {
        disconnectWebSocket()
        LocationTrackingService.stop(app)
        WatchdogScheduler.cancel(app)
        prefs.clearSession()
        db.clearAll()
        _state.value = OursUiState(
            isPaired = false,
            onboardingDone = false,
            serverUrl = prefs.serverUrl,
        )
    }

    fun photoUrl(photoId: String, thumb: Boolean): String = api.photoUrl(photoId, thumb)

    // --- WebSocket & Events ---

    private fun connectWebSocket() {
        if (webSocket != null || !prefs.isPaired) return
        webSocket = api.openWebSocket(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.update { it.copy(wsConnected = true) }
                catchUpEvents()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("\"pong\"")) return
                runCatching { OursJson.decodeFromString<EventDto>(text) }
                    .onSuccess { event -> handleIncomingEvent(event, showNotification = false) }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@OursViewModel.webSocket = null
                _state.update { it.copy(wsConnected = false) }
                scheduleWsReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@OursViewModel.webSocket = null
                _state.update { it.copy(wsConnected = false) }
                scheduleWsReconnect()
            }
        })
    }

    private fun disconnectWebSocket() {
        wsReconnectJob?.cancel()
        webSocket?.close(1000, "background")
        webSocket = null
        _state.update { it.copy(wsConnected = false) }
    }

    private fun scheduleWsReconnect() {
        if (!isForeground || !prefs.isPaired) return
        wsReconnectJob?.cancel()
        wsReconnectJob = viewModelScope.launch {
            delay(5_000)
            if (isForeground && prefs.isPaired && webSocket == null) {
                connectWebSocket()
            }
        }
    }

    private fun catchUpEvents() {
        viewModelScope.launch {
            var cursor = prefs.eventCursor
            while (true) {
                val resp = runCatching { api.getEvents(cursor) }.getOrNull() ?: break
                for (event in resp.events) {
                    handleIncomingEvent(event, showNotification = false)
                    cursor = maxOf(cursor, event.id)
                }
                if (resp.events.size < 100) break
            }
        }
    }

    private fun handleIncomingEvent(event: EventDto, showNotification: Boolean) {
        if (event.id > 0) {
            prefs.advanceCursor(event.id)
        }
        when (event.type) {
            "location" -> {
                runCatching { OursJson.decodeFromJsonElement<PartnerLocationDto>(event.data) }
                    .onSuccess { loc ->
                        db.savePartnerLocationCache(loc)
                        prefs.partnerPaused = loc.paused
                        _state.update { it.copy(partnerLocation = loc, partnerPaused = loc.paused) }
                    }
            }
            "checkin" -> {
                runCatching { OursJson.decodeFromJsonElement<EventCheckInData>(event.data) }
                    .onSuccess { data ->
                        _state.update { cur ->
                            val next = listOf(data.checkin) + cur.checkIns.filterNot { it.id == data.checkin.id }
                            db.replaceCheckInsCache(next)
                            cur.copy(checkIns = next)
                        }
                    }
            }
            "reply" -> {
                runCatching { OursJson.decodeFromJsonElement<EventReplyData>(event.data) }
                    .onSuccess { data ->
                        _state.update { cur ->
                            val next = cur.checkIns.map { c ->
                                if (c.id == data.reply.checkinId && c.replies.none { it.id == data.reply.id }) {
                                    c.copy(replies = c.replies + data.reply)
                                } else c
                            }
                            db.replaceCheckInsCache(next)
                            cur.copy(checkIns = next)
                        }
                    }
            }
            "chat_message" -> {
                runCatching { OursJson.decodeFromJsonElement<EventChatMessageData>(event.data) }
                    .onSuccess { data ->
                        _state.update { cur ->
                            val next = (listOf(data.message) + cur.chatMessages).distinctBy { it.id }
                            cur.copy(chatMessages = next)
                        }
                    }
            }
            "highlight" -> {
                runCatching { OursJson.decodeFromJsonElement<EventHighlightData>(event.data) }
                    .onSuccess { data ->
                        _state.update { cur ->
                            val next = cur.checkIns.map { c ->
                                if (c.id == data.checkinId) c.copy(highlighted = data.highlighted) else c
                            }
                            db.replaceCheckInsCache(next)
                            cur.copy(checkIns = next)
                        }
                        refreshMemories()
                    }
            }
            "sharing" -> {
                runCatching { OursJson.decodeFromJsonElement<EventSharingData>(event.data) }
                    .onSuccess { data ->
                        prefs.partnerPaused = data.paused
                        _state.update { cur ->
                            cur.copy(
                                partnerPaused = data.paused,
                                partnerLocation = if (data.paused) {
                                    cur.partnerLocation?.copy(
                                        paused = true,
                                        lat = null,
                                        lon = null,
                                        speedMps = null,
                                        batteryPct = null,
                                    )
                                } else cur.partnerLocation
                            )
                        }
                    }
            }
            "nudge" -> {
                OursNotifier.buzzPhone(app)
                showConfirmation("${prefs.partnerName ?: "Partner"} is thinking of you")
            }
        }
        if (showNotification && event.id > 0) {
            OursNotifier.handleEvent(app, event, prefs.partnerName)
        }
    }

    override fun onCleared() {
        disconnectWebSocket()
        super.onCleared()
    }
}
