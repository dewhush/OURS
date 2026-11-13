package app.ours.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursAvatar
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursErrorState
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursStatusChip
import app.ours.ui.components.StatusTone
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.Formatters
import coil3.compose.AsyncImage

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Check-ins", style = OursTypography.display, color = colors.ink)
                if (state.pendingUploads > 0) {
                    Text(
                        "${state.pendingUploads} upload(s) queued",
                        style = OursTypography.caption,
                        color = colors.paused,
                    )
                }
            }
            OursButton(
                text = "Check in",
                onClick = onLaunchCameraCheckIn,
                leadingIcon = {
                    Icon(
                        imageVector = OursIcons.Camera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }

        when {
            state.checkInsLoading && state.checkIns.isEmpty() -> {
                OursLoadingState("Loading last 24 hours...")
            }
            state.checkInsError != null && state.checkIns.isEmpty() -> {
                Box(Modifier.padding(OursSpacing.md)) {
                    OursErrorState(state.checkInsError, onRetry = { vm.refreshCheckIns() })
                }
            }
            state.checkIns.isEmpty() -> {
                OursEmptyState(
                    title = "No check-ins in the last 24 hours",
                    detail = "Snap a quick photo to share where you are. Unsaved check-ins disappear after 24 hours.",
                    actionLabel = "Take a photo",
                    onAction = onLaunchCameraCheckIn,
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(OursSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
                ) {
                    items(state.checkIns, key = { it.id }) { checkIn ->
                        CheckInCard(
                            checkIn = checkIn,
                            state = state,
                            vm = vm,
                            onOpenFullPhoto = { onOpenFullPhoto(checkIn) },
                            onOpenReplyPhoto = onOpenFullPhotoId,
                            onLaunchCameraReply = { onLaunchCameraReply(checkIn.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInCard(
    checkIn: CheckInDto,
    state: OursUiState,
    vm: OursViewModel,
    onOpenFullPhoto: () -> Unit,
    onOpenReplyPhoto: (String) -> Unit,
    onLaunchCameraReply: () -> Unit,
) {
    val colors = OursTheme.colors
    val isMine = checkIn.authorId == state.me?.id
    val authorName = if (isMine) state.me?.name ?: "You" else state.partner?.name ?: "Partner"
    var replyText by remember(checkIn.id) { mutableStateOf("") }

    OursCard(
        radius = OursRadius.mediaCard,
        padding = PaddingValues(OursSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
        ) {
            OursAvatar(name = authorName, size = 34.dp, accent = !isMine)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = authorName,
                    style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                )
                val subtitle = buildString {
                    if (!checkIn.placeName.isNullOrBlank()) {
                        append(checkIn.placeName)
                        append("  |  ")
                    }
                    append(Formatters.relativeTime(checkIn.createdAt))
                }
                Text(subtitle, style = OursTypography.caption, color = colors.inkSecondary)
            }
            if (checkIn.highlighted) {
                OursStatusChip(label = "Saved", tone = StatusTone.Live)
            } else {
                Text(
                    text = Formatters.countdown24h(checkIn.expiresAt),
                    style = OursTypography.tabular,
                    color = colors.inkMuted,
                )
            }
        }

        Spacer(Modifier.height(OursSpacing.sm))

        AsyncImage(
            model = vm.photoUrl(checkIn.photoId, thumb = true),
            contentDescription = checkIn.note ?: "Check-in photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(OursRadius.inner))
                .clickable(onClick = onOpenFullPhoto),
        )

        if (!checkIn.note.isNullOrBlank()) {
            Spacer(Modifier.height(OursSpacing.sm))
            Text(text = checkIn.note, style = OursTypography.body, color = colors.ink)
        }

        Spacer(Modifier.height(OursSpacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OursButton(
                text = if (checkIn.highlighted) "Saved in Memories" else "Save highlight",
                variant = if (checkIn.highlighted) OursButtonVariant.Secondary else OursButtonVariant.Primary,
                onClick = { vm.toggleHighlight(checkIn) },
                leadingIcon = {
                    Icon(
                        imageVector = OursIcons.Memories,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            if (isMine) {
                IconButton(onClick = { vm.deleteCheckIn(checkIn.id) }) {
                    Icon(
                        imageVector = OursIcons.Trash,
                        contentDescription = "Delete check-in",
                        tint = colors.error,
                    )
                }
            }
        }

        if (checkIn.replies.isNotEmpty()) {
            Spacer(Modifier.height(OursSpacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(OursRadius.inner))
                    .background(colors.surfaceMuted)
                    .padding(OursSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                for (reply in checkIn.replies) {
                    val replyAuthor = if (reply.authorId == state.me?.id) {
                        state.me?.name ?: "You"
                    } else {
                        state.partner?.name ?: "Partner"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = replyAuthor,
                                style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.ink,
                            )
                            Text(
                                text = Formatters.relativeTime(reply.createdAt),
                                style = OursTypography.tabular,
                                color = colors.inkMuted,
                            )
                        }
                        if (!reply.text.isNullOrBlank()) {
                            Text(reply.text, style = OursTypography.body, color = colors.inkSecondary)
                        }
                        if (!reply.photoId.isNullOrBlank()) {
                            AsyncImage(
                                model = vm.photoUrl(reply.photoId, thumb = true),
                                contentDescription = "Reply photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(OursRadius.chip))
                                    .clickable { onOpenReplyPhoto(reply.photoId) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(OursSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
        ) {
            IconButton(onClick = onLaunchCameraReply) {
                Icon(
                    imageVector = OursIcons.Camera,
                    contentDescription = "Reply with photo",
                    tint = colors.inkSecondary,
                )
            }
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Reply...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = replyText.isNotBlank() && !state.busy,
                onClick = {
                    val textToSend = replyText
                    replyText = ""
                    vm.sendReply(checkIn.id, textToSend, null)
                },
            ) {
                Icon(
                    imageVector = OursIcons.Send,
                    contentDescription = "Send reply",
                    tint = if (replyText.isNotBlank()) colors.action else colors.inkMuted,
                )
            }
        }
    }
}
