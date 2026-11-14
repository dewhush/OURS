package app.ours.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ours.data.api.ChatMessageDto
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.Formatters
import coil3.compose.AsyncImage

private sealed interface ChatThreadItem {
    val key: String
    val createdAt: Long

    data class MessageItem(val msg: ChatMessageDto) : ChatThreadItem {
        override val key: String = "m_${msg.id}"
        override val createdAt: Long = msg.createdAt
    }

    data class CheckInItem(val checkIn: CheckInDto) : ChatThreadItem {
        override val key: String = "c_${checkIn.id}"
        override val createdAt: Long = checkIn.createdAt
    }
}

private enum class ChatFilter { All, Photos, Saved }

private data class ReplyTarget(
    val messageId: Long? = null,
    val checkInId: Long? = null,
    val senderName: String,
    val preview: String,
)

@Composable
fun ChatScreen(
    state: OursUiState,
    vm: OursViewModel,
    onLaunchCameraCheckIn: () -> Unit,
    onLaunchCameraReply: (Long) -> Unit,
    onOpenFullPhoto: (CheckInDto) -> Unit,
    onOpenFullPhotoId: (String) -> Unit,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val layoutSpec = rememberOursLayoutSpec()
    val partnerName = state.partner?.name ?: "Partner"
    val myId = state.me?.id ?: -1L
    val listState = rememberLazyListState()

    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ReplyTarget?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ChatFilter.All) }

    val threadItems = remember(state.chatMessages, state.checkIns) {
        (state.chatMessages.map { ChatThreadItem.MessageItem(it) } +
            state.checkIns.map { ChatThreadItem.CheckInItem(it) })
            .sortedBy { it.createdAt }
    }

    val filteredThreadItems = remember(threadItems, searchQuery, filter) {
        val q = searchQuery.trim().lowercase()
        threadItems.filter { item ->
            val matchesFilter = when (filter) {
                ChatFilter.All -> true
                ChatFilter.Photos -> when (item) {
                    is ChatThreadItem.MessageItem -> !item.msg.photoId.isNullOrBlank()
                    is ChatThreadItem.CheckInItem -> item.checkIn.photoId.isNotBlank()
                }
                ChatFilter.Saved -> when (item) {
                    is ChatThreadItem.MessageItem -> false
                    is ChatThreadItem.CheckInItem -> item.checkIn.highlighted
                }
            }
            val matchesQuery = q.isEmpty() || when (item) {
                is ChatThreadItem.MessageItem -> item.msg.text?.lowercase()?.contains(q) == true
                is ChatThreadItem.CheckInItem ->
                    (item.checkIn.note?.lowercase()?.contains(q) == true) ||
                        (item.checkIn.placeName?.lowercase()?.contains(q) == true)
            }
            matchesFilter && matchesQuery
        }
    }

    LaunchedEffect(filteredThreadItems.size) {
        if (filteredThreadItems.isNotEmpty()) {
            listState.animateScrollToItem(filteredThreadItems.lastIndex)
        }
    }

    val loc = state.partnerLocation
    val nowMs = System.currentTimeMillis()
    val isLive = !state.partnerPaused && loc?.recordedAt?.let { nowMs - it <= 15 * 60_000L } == true
    val statusText = when {
        state.partner == null -> "Waiting for partner"
        state.partnerPaused -> "Paused"
        isLive -> "Live"
        loc?.lastSeenAt != null -> "Seen ${Formatters.relativeTime(loc.lastSeenAt, nowMs)}"
        else -> "Offline"
    }
    val statusTone = when {
        state.partnerPaused -> StatusTone.Paused
        isLive -> StatusTone.Live
        else -> StatusTone.Muted
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .oursContentWidth(layoutSpec)
            .fillMaxHeight(),
    ) {
        // Floating frosted top header
        Surface(
            shape = RoundedCornerShape(OursRadius.card),
            color = colors.glassSurfaceStrong,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .oursHeaderInsets()
                .padding(horizontal = layoutSpec.horizontalPadding),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.sm),
            ) {
                OursAvatar(
                    name = partnerName,
                    photoUrl = state.partnerAvatarUrl,
                    size = 40.dp,
                    ringColor = if (isLive) colors.success else null,
                    accent = true,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partnerName,
                        style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    OursStatusChip(label = statusText, tone = statusTone)
                }
                if (state.pendingUploads > 0) {
                    Text(
                        text = "${state.pendingUploads} queued",
                        style = OursTypography.tabular,
                        color = colors.paused,
                    )
                }
            }
        }

        OursSearchFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = strings.chatSearchPlaceholder,
            filters = listOf(
                ChatFilter.All to strings.filterAll,
                ChatFilter.Photos to strings.filterPhotos,
                ChatFilter.Saved to strings.filterSaved,
            ),
            selectedFilter = filter,
            onSelectFilter = { filter = it },
            modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = OursSpacing.xs),
        )

        // Chronological messenger thread
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                (state.checkInsLoading || state.messagesLoading) && threadItems.isEmpty() -> {
                    OursLoadingState("Loading conversation...")
                }
                threadItems.isEmpty() -> {
                    val emptyGreeting = if (strings.langCode == "id") {
                        "Sapa $partnerName"
                    } else {
                        "Say hello to $partnerName"
                    }
                    OursEmptyState(
                        title = emptyGreeting,
                        detail = strings.sayHelloDetail,
                        actionLabel = strings.takePhoto,
                        onAction = onLaunchCameraCheckIn,
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = OursSpacing.md,
                            vertical = OursSpacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                    ) {
                        if (state.messagesNextBefore != null) {
                            item(key = "load_older") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    OursButton(
                                        text = "Load older messages",
                                        variant = OursButtonVariant.Ghost,
                                        loading = state.messagesLoading,
                                        onClick = { vm.refreshMessages(loadMore = true) },
                                    )
                                }
                            }
                        }

                        items(filteredThreadItems, key = { it.key }) { item ->
                            when (item) {
                                is ChatThreadItem.MessageItem -> {
                                    val msg = item.msg
                                    val isMine = msg.authorId == myId
                                    val quoted = resolveQuotePreview(
                                        replyToMessageId = msg.replyToMessageId,
                                        replyToCheckInId = msg.replyToCheckInId,
                                        messages = state.chatMessages,
                                        checkIns = state.checkIns,
                                    )
                                    MessageBubble(
                                        msg = msg,
                                        isMine = isMine,
                                        quotedPreview = quoted,
                                        photoUrl = { pid -> vm.photoUrl(pid, thumb = true) },
                                        onOpenPhoto = onOpenFullPhotoId,
                                        onQuote = {
                                            replyTarget = ReplyTarget(
                                                messageId = msg.id,
                                                senderName = if (isMine) (state.me?.name ?: "You") else partnerName,
                                                preview = msg.text ?: "Photo",
                                            )
                                        },
                                    )
                                }

                                is ChatThreadItem.CheckInItem -> {
                                    val c = item.checkIn
                                    val isMine = c.authorId == myId
                                    CheckInBubble(
                                        checkIn = c,
                                        isMine = isMine,
                                        photoUrl = vm.photoUrl(c.photoId, thumb = true),
                                        onOpenPhoto = { onOpenFullPhoto(c) },
                                        onToggleHighlight = { vm.toggleHighlight(c) },
                                        onQuote = {
                                            replyTarget = ReplyTarget(
                                                checkInId = c.id,
                                                senderName = if (isMine) (state.me?.name ?: "You") else partnerName,
                                                preview = c.note ?: c.placeName ?: "Photo check-in",
                                            )
                                        },
                                        onDelete = if (isMine) ({ vm.deleteCheckIn(c.id) }) else null,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating frosted bottom composer
        Surface(
            shape = RoundedCornerShape(OursRadius.card),
            color = colors.glassSurfaceStrong,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.xs),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OursSpacing.sm, vertical = OursSpacing.xs),
            ) {
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = OursSpacing.xs)
                            .clip(RoundedCornerShape(OursRadius.chip))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = OursSpacing.sm, vertical = OursSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${replyTarget!!.senderName}",
                                style = OursTypography.label,
                                color = colors.action,
                            )
                            Text(
                                text = replyTarget!!.preview,
                                style = OursTypography.caption,
                                color = colors.inkSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { replyTarget = null },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = OursIcons.Close,
                                contentDescription = "Cancel reply",
                                tint = colors.inkMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                ) {
                    IconButton(onClick = onLaunchCameraCheckIn) {
                        Icon(
                            imageVector = OursIcons.Camera,
                            contentDescription = "Camera check-in",
                            tint = colors.inkSecondary,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(OursRadius.card),
                        color = colors.surfaceMuted,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = strings.messagePlaceholder,
                                    style = OursTypography.body,
                                    color = colors.inkMuted,
                                )
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                textStyle = OursTypography.body.copy(color = colors.ink),
                                cursorBrush = SolidColor(colors.action),
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    val canSend = draft.isNotBlank() && !state.busy
                    IconButton(
                        enabled = canSend,
                        onClick = {
                            val textToSend = draft.trim()
                            val target = replyTarget
                            draft = ""
                            replyTarget = null
                            vm.sendChatMessage(
                                text = textToSend,
                                replyToMessageId = target?.messageId,
                                replyToCheckInId = target?.checkInId,
                            )
                        },
                    ) {
                        Icon(
                            imageVector = OursIcons.Send,
                            contentDescription = "Send",
                            tint = if (canSend) colors.action else colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessageDto,
    isMine: Boolean,
    quotedPreview: String?,
    photoUrl: (String) -> String,
    onOpenPhoto: (String) -> Unit,
    onQuote: () -> Unit,
) {
    val colors = OursTheme.colors
    val bubbleBg = if (isMine) colors.glassActionTint else colors.glassSurface
    val bubbleBorder = if (isMine) colors.action.copy(alpha = 0.25f) else colors.glassBorder

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = OursRadius.card,
                topEnd = OursRadius.card,
                bottomStart = if (isMine) OursRadius.card else 6.dp,
                bottomEnd = if (isMine) 6.dp else OursRadius.card,
            ),
            color = bubbleBg,
            border = BorderStroke(1.dp, bubbleBorder),
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clickable(onClick = onQuote),
        ) {
            Column(modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = OursSpacing.xs)) {
                if (!quotedPreview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = OursSpacing.xxs)
                            .clip(RoundedCornerShape(OursRadius.chip))
                            .background(colors.surfaceMuted.copy(alpha = 0.7f))
                            .padding(horizontal = OursSpacing.xs, vertical = 4.dp),
                    ) {
                        Text(
                            text = quotedPreview,
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!msg.photoId.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl(msg.photoId),
                        contentDescription = msg.text ?: "Photo message",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(OursRadius.inner))
                            .clickable { onOpenPhoto(msg.photoId) },
                    )
                    if (!msg.text.isNullOrBlank()) {
                        Spacer(Modifier.height(OursSpacing.xxs))
                    }
                }
                if (!msg.text.isNullOrBlank()) {
                    Text(
                        text = msg.text,
                        style = OursTypography.body,
                        color = colors.ink,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.relativeTime(msg.createdAt),
                    style = OursTypography.tabular.copy(fontSize = OursTypography.label.fontSize),
                    color = colors.inkMuted,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun CheckInBubble(
    checkIn: CheckInDto,
    isMine: Boolean,
    photoUrl: String,
    onOpenPhoto: () -> Unit,
    onToggleHighlight: () -> Unit,
    onQuote: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = OursTheme.colors
    val bubbleBg = if (isMine) colors.glassActionTint else colors.glassSurface
    val bubbleBorder = if (isMine) colors.action.copy(alpha = 0.25f) else colors.glassBorder

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = OursRadius.mediaCard,
                topEnd = OursRadius.mediaCard,
                bottomStart = if (isMine) OursRadius.mediaCard else 6.dp,
                bottomEnd = if (isMine) 6.dp else OursRadius.mediaCard,
            ),
            color = bubbleBg,
            border = BorderStroke(1.dp, bubbleBorder),
            modifier = Modifier
                .widthIn(max = 270.dp)
                .clickable(onClick = onQuote),
        ) {
            Column(modifier = Modifier.padding(OursSpacing.xs)) {
                Box {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = checkIn.note ?: "Check-in photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(OursRadius.inner))
                            .clickable(onClick = onOpenPhoto),
                    )
                    // Subtle 24h countdown pill or Saved badge on photo corner
                    Surface(
                        shape = RoundedCornerShape(OursRadius.chip),
                        color = colors.glassSurfaceStrong,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        Text(
                            text = if (checkIn.highlighted) "Saved" else Formatters.countdown24h(checkIn.expiresAt),
                            style = OursTypography.tabular,
                            color = if (checkIn.highlighted) colors.live else colors.ink,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                if (!checkIn.placeName.isNullOrBlank()) {
                    Spacer(Modifier.height(OursSpacing.xxs))
                    Text(
                        text = checkIn.placeName,
                        style = OursTypography.label,
                        color = colors.action,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                if (!checkIn.note.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = checkIn.note,
                        style = OursTypography.body,
                        color = colors.ink,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                Spacer(Modifier.height(OursSpacing.xxs))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                    ) {
                        val saveInteraction = remember { MutableInteractionSource() }
                        Surface(
                            shape = RoundedCornerShape(OursRadius.chip),
                            color = if (checkIn.highlighted) colors.actionSoft else colors.surfaceMuted,
                            modifier = Modifier
                                .oursPressScale(saveInteraction)
                                .clip(RoundedCornerShape(OursRadius.chip))
                                .clickable(interactionSource = saveInteraction, indication = null, onClick = onToggleHighlight),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = OursIcons.Memories,
                                    contentDescription = "Save highlight",
                                    tint = if (checkIn.highlighted) colors.action else colors.inkSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (checkIn.highlighted) "Saved" else "Save",
                                    style = OursTypography.label,
                                    color = if (checkIn.highlighted) colors.action else colors.inkSecondary,
                                )
                            }
                        }

                        if (onDelete != null) {
                            Icon(
                                imageVector = OursIcons.Trash,
                                contentDescription = "Delete check-in",
                                tint = colors.inkMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onDelete),
                            )
                        }
                    }

                    Text(
                        text = Formatters.relativeTime(checkIn.createdAt),
                        style = OursTypography.tabular,
                        color = colors.inkMuted,
                    )
                }
            }
        }
    }
}

private fun resolveQuotePreview(
    replyToMessageId: Long?,
    replyToCheckInId: Long?,
    messages: List<ChatMessageDto>,
    checkIns: List<CheckInDto>,
): String? {
    if (replyToMessageId != null) {
        val m = messages.firstOrNull { it.id == replyToMessageId }
        return m?.text ?: if (m?.photoId != null) "Photo" else "Message"
    }
    if (replyToCheckInId != null) {
        val c = checkIns.firstOrNull { it.id == replyToCheckInId }
        return c?.note ?: c?.placeName ?: "Photo check-in"
    }
    return null
}
