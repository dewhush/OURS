package app.ours.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ours.R
import app.ours.data.api.ChatMessageDto
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursGlassBackdrop
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursLiquidGlass
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.theme.OursMotion
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
    val partnerName = state.partner?.name ?: strings.partnerFallback
    val myId = state.me?.id ?: -1L
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    var draft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<ReplyTarget?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ChatFilter.All) }
    val topHeaderInset = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateTopPadding()

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
        state.partner == null -> strings.waitingForPartner
        state.partnerPaused -> strings.sharingPaused
        isLive -> strings.liveStatus
        loc?.lastSeenAt != null -> "${strings.seenPrefix} ${Formatters.relativeTime(loc.lastSeenAt, strings.langCode, nowMs)}"
        else -> strings.offlineStatus
    }
    val statusTone = when {
        state.partnerPaused -> StatusTone.Paused
        isLive -> StatusTone.Live
        else -> StatusTone.Muted
    }
    val searchBarOpen = showSearch || searchQuery.isNotEmpty() || filter != ChatFilter.All

    OursGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .oursContentWidth(layoutSpec)
                    .fillMaxSize(),
            ) {
                when {
                    (state.checkInsLoading || state.messagesLoading) && threadItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 96.dp)) {
                            OursLoadingState(strings.loadingConversation)
                        }
                    }
                    !state.errorMessage.isNullOrBlank() && threadItems.isEmpty() -> {
                        Box(Modifier.padding(start = OursSpacing.md, end = OursSpacing.md, top = topHeaderInset + 96.dp)) {
                            app.ours.ui.components.OursErrorState(
                                message = state.errorMessage!!,
                                onRetry = { vm.refreshAll() },
                            )
                        }
                    }
                    threadItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 96.dp)) {
                            OursEmptyState(
                                title = "${strings.sayHelloTitle} $partnerName",
                                detail = strings.sayHelloDetail,
                                illustrationRes = R.drawable.ic_ours_chat_empty,
                                actionLabel = strings.takePhoto,
                                onAction = onLaunchCameraCheckIn,
                            )
                        }
                    }
                    filteredThreadItems.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 96.dp)) {
                            OursEmptyState(
                                title = strings.noResultsTitle,
                                detail = strings.noResultsDetail,
                                illustrationRes = R.drawable.ic_ours_chat_empty,
                                actionLabel = strings.clearFiltersCta,
                                onAction = {
                                    searchQuery = ""
                                    filter = ChatFilter.All
                                },
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = OursSpacing.md,
                                top = topHeaderInset + if (searchBarOpen) 196.dp else 108.dp,
                                end = OursSpacing.md,
                                bottom = 184.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            if (state.messagesNextBefore != null) {
                                item(key = "load_older") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        OursButton(
                                            text = strings.loadOlderMessages,
                                            variant = OursButtonVariant.Ghost,
                                            loading = state.messagesLoading,
                                            onClick = { vm.refreshMessages(loadMore = true) },
                                        )
                                    }
                                }
                            }

                            itemsIndexed(filteredThreadItems, key = { _, it -> it.key }) { idx, item ->
                                val prevItem = filteredThreadItems.getOrNull(idx - 1)
                                val nextItem = filteredThreadItems.getOrNull(idx + 1)
                                val showDateDivider = prevItem == null ||
                                    Formatters.dayKey(item.createdAt) != Formatters.dayKey(prevItem.createdAt)
                                if (showDateDivider) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = OursSpacing.sm, bottom = OursSpacing.xs),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = colors.surfaceMuted,
                                        ) {
                                            Text(
                                                text = Formatters.dayLabel(item.createdAt, strings.langCode),
                                                style = OursTypography.caption,
                                                color = colors.inkSecondary,
                                                modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                                when (item) {
                                    is ChatThreadItem.MessageItem -> {
                                        val msg = item.msg
                                        val isMine = msg.authorId == myId
                                        val prevSameSender = !showDateDivider &&
                                            prevItem is ChatThreadItem.MessageItem &&
                                            prevItem.msg.authorId == msg.authorId
                                        val nextSameDay = nextItem != null &&
                                            Formatters.dayKey(item.createdAt) == Formatters.dayKey(nextItem.createdAt)
                                        val nextSameSender = nextSameDay &&
                                            nextItem is ChatThreadItem.MessageItem &&
                                            nextItem.msg.authorId == msg.authorId
                                        val quoted = resolveQuotePreview(
                                            replyToMessageId = msg.replyToMessageId,
                                            replyToCheckInId = msg.replyToCheckInId,
                                            messages = state.chatMessages,
                                            checkIns = state.checkIns,
                                        )
                                        if (!prevSameSender && !showDateDivider) {
                                            Spacer(Modifier.height(6.dp))
                                        }
                                        MessageBubble(
                                            msg = msg,
                                            isMine = isMine,
                                            isFirstInGroup = !prevSameSender,
                                            isLastInGroup = !nextSameSender,
                                            quotedPreview = quoted,
                                            langCode = strings.langCode,
                                            photoUrl = { pid -> vm.photoUrl(pid, thumb = true) },
                                            onOpenPhoto = onOpenFullPhotoId,
                                            onQuote = {
                                                replyTarget = ReplyTarget(
                                                    messageId = msg.id,
                                                    senderName = if (isMine) (state.me?.name ?: strings.youFallback) else partnerName,
                                                    preview = msg.text ?: "Photo",
                                                )
                                            },
                                            onCopy = { txt ->
                                                clipboard.setText(AnnotatedString(txt))
                                                vm.notifyCopied(strings.copiedTextToast)
                                            },
                                        )
                                    }

                                    is ChatThreadItem.CheckInItem -> {
                                        val c = item.checkIn
                                        val isMine = c.authorId == myId
                                        Spacer(Modifier.height(6.dp))
                                        CheckInBubble(
                                            checkIn = c,
                                            isMine = isMine,
                                            myId = myId,
                                            partnerName = partnerName,
                                            photoUrl = vm.photoUrl(c.photoId, thumb = true),
                                            replyPhotoUrl = { pid -> vm.photoUrl(pid, thumb = true) },
                                            onOpenPhoto = { onOpenFullPhoto(c) },
                                            onOpenReplyPhoto = onOpenFullPhotoId,
                                            onToggleHighlight = { vm.toggleHighlight(c) },
                                            onReplyCamera = { onLaunchCameraReply(c.id) },
                                            onQuote = {
                                                replyTarget = ReplyTarget(
                                                    checkInId = c.id,
                                                    senderName = if (isMine) (state.me?.name ?: strings.youFallback) else partnerName,
                                                    preview = c.note ?: c.placeName ?: strings.checkIn,
                                                )
                                            },
                                            onDelete = if (isMine) ({ vm.deleteCheckIn(c.id) }) else null,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topHeaderInset + if (searchBarOpen) 196.dp else 108.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to colors.canvas,
                            0.78f to colors.canvas,
                            0.92f to colors.canvas.copy(alpha = 0.85f),
                            1.0f to Color.Transparent,
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .oursContentWidth(layoutSpec)
                    .oursHeaderInsets()
                    .padding(horizontal = layoutSpec.horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                Surface(
                    shape = RoundedCornerShape(OursRadius.card),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .oursLiquidGlass(radius = OursRadius.card, colors = colors, elevation = 6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = OursSpacing.md, vertical = 10.dp)
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
                                text = "${state.pendingUploads} ${strings.queuedStatus}",
                                style = OursTypography.tabular,
                                color = colors.paused,
                            )
                        }
                        IconButton(
                            onClick = {
                                if (showSearch) {
                                    showSearch = false
                                    searchQuery = ""
                                    filter = ChatFilter.All
                                } else {
                                    showSearch = true
                                }
                            },
                            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                        ) {
                            Icon(
                                imageVector = if (showSearch) OursIcons.Close else OursIcons.Search,
                                contentDescription = strings.chatSearchPlaceholder,
                                tint = if (showSearch) colors.action else colors.ink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = searchBarOpen,
                    enter = fadeIn(OursMotion.enterSpec()) + expandVertically(OursMotion.springFluid()),
                    exit = fadeOut(OursMotion.exitSpec()) + shrinkVertically(OursMotion.exitSpec()),
                ) {
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
                    )
                }
            }

            val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.18f to colors.canvas.copy(alpha = 0.94f),
                            0.32f to colors.canvas,
                            1.0f to colors.canvas,
                        ),
                    )
                    .oursContentWidth(layoutSpec)
                    .then(if (imeOpen) Modifier else Modifier.navigationBarsPadding())
                    .padding(
                        start = OursSpacing.md,
                        end = OursSpacing.md,
                        top = 18.dp,
                        bottom = if (imeOpen) OursSpacing.xs else 84.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .oursLiquidGlass(radius = 16.dp, colors = colors, elevation = 3.dp)
                            .padding(horizontal = OursSpacing.sm, vertical = OursSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${strings.replyingToPrefix} ${replyTarget!!.senderName}",
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
                            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                        ) {
                            Icon(
                                imageVector = OursIcons.Close,
                                contentDescription = strings.cancelLabel,
                                tint = colors.inkMuted,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .oursLiquidGlass(radius = 999.dp, colors = colors, elevation = 6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                    ) {
                        IconButton(
                            onClick = onLaunchCameraCheckIn,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.surfaceMuted),
                        ) {
                            Icon(
                                imageVector = OursIcons.Camera,
                                contentDescription = strings.takePhoto,
                                tint = colors.inkSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = OursSpacing.xs, vertical = 8.dp),
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

                        val canSend = draft.isNotBlank() && !state.busy
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (canSend) colors.action else colors.surfaceMuted)
                                .clickable(enabled = canSend, role = Role.Button) {
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
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = OursIcons.Send,
                                contentDescription = strings.messagePlaceholder,
                                tint = if (canSend) colors.onAction else colors.inkMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    msg: ChatMessageDto,
    isMine: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    quotedPreview: String?,
    langCode: String,
    photoUrl: (String) -> String,
    onOpenPhoto: (String) -> Unit,
    onQuote: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val colors = OursTheme.colors
    val bubbleBg = if (isMine) colors.action else colors.surfaceMuted
    val textColor = if (isMine) colors.onAction else colors.ink
    val metaColor = if (isMine) colors.onAction.copy(alpha = 0.74f) else colors.inkMuted
    val shape = RoundedCornerShape(
        topStart = if (!isMine && !isFirstInGroup) 8.dp else 20.dp,
        topEnd = if (isMine && !isFirstInGroup) 8.dp else 20.dp,
        bottomStart = if (!isMine && isLastInGroup) 5.dp else if (!isMine) 8.dp else 20.dp,
        bottomEnd = if (isMine && isLastInGroup) 5.dp else if (isMine) 8.dp else 20.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = shape,
            color = bubbleBg,
            shadowElevation = 0.dp,
            modifier = Modifier
                .widthIn(max = 288.dp)
                .clip(shape)
                .combinedClickable(
                    onClick = onQuote,
                    onLongClick = {
                        if (!msg.text.isNullOrBlank()) {
                            onCopy(msg.text)
                        }
                    },
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                if (!quotedPreview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isMine) Color.White.copy(alpha = 0.16f)
                                else colors.surface.copy(alpha = 0.75f)
                            )
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = quotedPreview,
                            style = OursTypography.caption,
                            color = if (isMine) colors.onAction.copy(alpha = 0.90f) else colors.inkSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!msg.photoId.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl(msg.photoId),
                        contentDescription = msg.text ?: "Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenPhoto(msg.photoId) },
                    )
                    if (!msg.text.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                    }
                }
                if (!msg.text.isNullOrBlank()) {
                    Text(
                        text = msg.text,
                        style = OursTypography.body,
                        color = textColor,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Formatters.relativeTime(msg.createdAt, langCode = langCode),
                    style = OursTypography.tabular.copy(fontSize = OursTypography.label.fontSize),
                    color = metaColor,
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
    myId: Long,
    partnerName: String,
    photoUrl: String,
    replyPhotoUrl: (String) -> String,
    onOpenPhoto: () -> Unit,
    onOpenReplyPhoto: (String) -> Unit,
    onToggleHighlight: () -> Unit,
    onReplyCamera: () -> Unit,
    onQuote: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings

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
            color = colors.surface,
            border = BorderStroke(0.75.dp, colors.border),
            modifier = Modifier
                .widthIn(max = 284.dp)
                .clickable(onClick = onQuote),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Box {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = checkIn.note ?: strings.checkIn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(OursRadius.inner))
                            .clickable(onClick = onOpenPhoto),
                    )
                    if (!checkIn.highlighted) {
                        Surface(
                            shape = RoundedCornerShape(OursRadius.chip),
                            color = colors.glassSurfaceStrong,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                        ) {
                            Text(
                                text = Formatters.countdown24h(
                                    expiresAtMs = checkIn.expiresAt,
                                    langCode = strings.langCode,
                                ),
                                style = OursTypography.tabular,
                                color = colors.ink,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }

                if (!checkIn.placeName.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = checkIn.placeName,
                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
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

                if (checkIn.replies.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceMuted)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        checkIn.replies.forEach { reply ->
                            val replyAuthor = if (reply.authorId == myId) strings.youFallback else partnerName
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!reply.photoId.isNullOrBlank()) {
                                    AsyncImage(
                                        model = replyPhotoUrl(reply.photoId),
                                        contentDescription = reply.text ?: strings.replyLabel,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onOpenReplyPhoto(reply.photoId) },
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = replyAuthor,
                                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.inkSecondary,
                                    )
                                    if (!reply.text.isNullOrBlank()) {
                                        Text(
                                            text = reply.text,
                                            style = OursTypography.caption,
                                            color = colors.ink,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val saveInteraction = remember { MutableInteractionSource() }
                        Surface(
                            shape = RoundedCornerShape(OursRadius.chip),
                            color = if (checkIn.highlighted) colors.actionSoft else colors.surfaceMuted,
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .oursPressScale(saveInteraction)
                                .clip(RoundedCornerShape(OursRadius.chip))
                                .semantics { selected = checkIn.highlighted }
                                .clickable(
                                    interactionSource = saveInteraction,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onToggleHighlight,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = OursIcons.Memories,
                                    contentDescription = strings.saveLabel,
                                    tint = if (checkIn.highlighted) colors.action else colors.inkSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = if (checkIn.highlighted) strings.savedLabel else strings.saveLabel,
                                    style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (checkIn.highlighted) colors.action else colors.inkSecondary,
                                )
                            }
                        }

                        val replyInteraction = remember { MutableInteractionSource() }
                        Surface(
                            shape = RoundedCornerShape(OursRadius.chip),
                            color = colors.surfaceMuted,
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .oursPressScale(replyInteraction)
                                .clip(RoundedCornerShape(OursRadius.chip))
                                .clickable(
                                    interactionSource = replyInteraction,
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onReplyCamera,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = OursIcons.Camera,
                                    contentDescription = strings.replyLabel,
                                    tint = colors.inkSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = strings.replyLabel,
                                    style = OursTypography.label,
                                    color = colors.inkSecondary,
                                )
                            }
                        }

                        if (onDelete != null) {
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                    .clip(RoundedCornerShape(OursRadius.chip))
                                    .clickable(role = Role.Button, onClick = onDelete),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = OursIcons.Trash,
                                    contentDescription = strings.deleteCheckInLabel,
                                    tint = colors.inkMuted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = Formatters.relativeTime(checkIn.createdAt, langCode = strings.langCode),
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
