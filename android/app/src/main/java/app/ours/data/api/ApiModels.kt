package app.ours.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

val OursJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = true
}

@Serializable
data class ApiErrorBody(
    val error: String,
    val message: String,
)

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class TokensDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("access_expires_at") val accessExpiresAt: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_at") val refreshExpiresAt: Long,
)

@Serializable
data class PairCreateRequest(
    val name: String,
)

@Serializable
data class PairCreateResponse(
    val user: UserDto,
    val tokens: TokensDto,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("invite_expires_at") val inviteExpiresAt: Long,
)

@Serializable
data class InviteResponse(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("invite_expires_at") val inviteExpiresAt: Long,
)

@Serializable
data class PairJoinRequest(
    val code: String,
    val name: String,
)

@Serializable
data class PairJoinResponse(
    val user: UserDto,
    val tokens: TokensDto,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class MeResponse(
    val me: UserDto,
    val partner: UserDto? = null,
    @SerialName("sharing_paused") val sharingPaused: Boolean,
    @SerialName("partner_paused") val partnerPaused: Boolean,
    @SerialName("server_time") val serverTime: Long,
)

@Serializable
data class PushTokenRequest(
    val token: String,
)

@Serializable
data class LocationPointDto(
    val lat: Double,
    val lon: Double,
    @SerialName("accuracy_m") val accuracyM: Double,
    @SerialName("speed_mps") val speedMps: Double,
    val bearing: Double? = null,
    @SerialName("battery_pct") val batteryPct: Int,
    val charging: Boolean,
    @SerialName("recorded_at") val recordedAt: Long,
)

@Serializable
data class PartnerLocationDto(
    @SerialName("user_id") val userId: Long,
    val paused: Boolean,
    @SerialName("last_seen_at") val lastSeenAt: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("accuracy_m") val accuracyM: Double? = null,
    @SerialName("speed_mps") val speedMps: Double? = null,
    val bearing: Double? = null,
    @SerialName("battery_pct") val batteryPct: Int? = null,
    val charging: Boolean? = null,
    @SerialName("recorded_at") val recordedAt: Long? = null,
)

@Serializable
data class LocationsUploadResponse(
    val accepted: Int,
    val paused: Boolean,
    val events: List<EventDto>,
)

@Serializable
data class HistoryResponse(
    val points: List<LocationPointDto>,
)

@Serializable
data class SharingRequest(
    val paused: Boolean,
)

@Serializable
data class SharingResponse(
    val paused: Boolean,
)

@Serializable
data class PlaceDto(
    val id: Long,
    val name: String,
    val lat: Double,
    val lon: Double,
    @SerialName("radius_m") val radiusM: Double,
    @SerialName("created_by") val createdBy: Long,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class PlacesResponse(
    val places: List<PlaceDto>,
)

@Serializable
data class CreatePlaceRequest(
    val name: String,
    val lat: Double,
    val lon: Double,
    @SerialName("radius_m") val radiusM: Double,
)

@Serializable
data class ReplyDto(
    val id: Long,
    @SerialName("checkin_id") val checkinId: Long,
    @SerialName("author_id") val authorId: Long,
    val text: String? = null,
    @SerialName("photo_id") val photoId: String? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class ChatMessageDto(
    val id: Long,
    @SerialName("author_id") val authorId: Long,
    val text: String? = null,
    @SerialName("photo_id") val photoId: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null,
    @SerialName("reply_to_checkin_id") val replyToCheckInId: Long? = null,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class CreateMessageRequest(
    val text: String,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null,
    @SerialName("reply_to_checkin_id") val replyToCheckInId: Long? = null,
)

@Serializable
data class MessagesResponse(
    val items: List<ChatMessageDto>,
    @SerialName("next_before") val nextBefore: Long? = null,
)

@Serializable
data class EventChatMessageData(
    val message: ChatMessageDto,
)

@Serializable
data class CheckInDto(
    val id: Long,
    @SerialName("author_id") val authorId: Long,
    @SerialName("photo_id") val photoId: String,
    val note: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("expires_at") val expiresAt: Long,
    val highlighted: Boolean,
    @SerialName("highlighted_by") val highlightedBy: Long? = null,
    @SerialName("highlighted_at") val highlightedAt: Long? = null,
    val replies: List<ReplyDto> = emptyList(),
)

@Serializable
data class CheckInsResponse(
    val checkins: List<CheckInDto>,
)

@Serializable
data class MemoriesResponse(
    val items: List<CheckInDto>,
    @SerialName("next_before") val nextBefore: Long? = null,
)

@Serializable
data class EventDto(
    val id: Long,
    val type: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("actor_id") val actorId: Long,
    val data: JsonObject,
)

@Serializable
data class EventsResponse(
    val events: List<EventDto>,
)

@Serializable
data class EventCheckInData(
    val checkin: CheckInDto,
)

@Serializable
data class EventReplyData(
    val reply: ReplyDto,
)

@Serializable
data class EventHighlightData(
    @SerialName("checkin_id") val checkinId: Long,
    val highlighted: Boolean,
)

@Serializable
data class EventSharingData(
    val paused: Boolean,
)

@Serializable
data class EventPlaceData(
    @SerialName("place_id") val placeId: Long,
    @SerialName("place_name") val placeName: String,
    val transition: String,
    val lat: Double,
    val lon: Double,
)
