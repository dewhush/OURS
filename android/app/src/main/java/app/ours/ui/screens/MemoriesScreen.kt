package app.ours.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursErrorState
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursMapView
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.Formatters
import coil3.compose.AsyncImage

@Composable
fun MemoriesScreen(
    state: OursUiState,
    vm: OursViewModel,
    onOpenFullPhoto: (CheckInDto) -> Unit,
) {
    val colors = OursTheme.colors
    var showMapMode by remember { mutableStateOf(false) }

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
            Text("Memories", style = OursTypography.display, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                OursButton(
                    text = "Timeline",
                    variant = if (!showMapMode) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                    onClick = { showMapMode = false },
                )
                OursButton(
                    text = "Map",
                    variant = if (showMapMode) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                    onClick = { showMapMode = true },
                )
            }
        }

        if (showMapMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                OursMapView(
                    modifier = Modifier.fillMaxSize(),
                    highlights = state.memories,
                    places = state.places,
                    onHighlightClick = onOpenFullPhoto,
                )
                if (state.memories.isEmpty() && !state.memoriesLoading) {
                    OursCard(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(OursSpacing.md)
                            .fillMaxWidth(),
                    ) {
                        Text("No pinned memories yet", style = OursTypography.title, color = colors.ink)
                        Text(
                            "Save a check-in as a highlight in Chat before its 24 hours expire.",
                            style = OursTypography.caption,
                            color = colors.inkSecondary,
                        )
                    }
                }
            }
        } else {
            when {
                state.memoriesLoading && state.memories.isEmpty() -> {
                    OursLoadingState("Loading saved highlights...")
                }
                state.memoriesError != null && state.memories.isEmpty() -> {
                    Box(Modifier.padding(OursSpacing.md)) {
                        OursErrorState(state.memoriesError, onRetry = { vm.refreshMemories() })
                    }
                }
                state.memories.isEmpty() -> {
                    OursEmptyState(
                        title = "No saved memories yet",
                        detail = "Check-ins expire after 24 hours. Tap Save on any check-in in Chat to keep it here forever.",
                        actionLabel = "Refresh",
                        onAction = { vm.refreshMemories() },
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(OursSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(OursSpacing.md),
                    ) {
                        items(state.memories, key = { it.id }) { item ->
                            val authorName = if (item.authorId == state.me?.id) {
                                state.me.name
                            } else {
                                state.partner?.name ?: "Partner"
                            }
                            OursCard(
                                radius = OursRadius.mediaCard,
                                padding = PaddingValues(OursSpacing.sm),
                                onClick = { onOpenFullPhoto(item) },
                            ) {
                                AsyncImage(
                                    model = vm.photoUrl(item.photoId, thumb = true),
                                    contentDescription = item.note ?: "Saved memory photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(OursRadius.inner)),
                                )
                                Spacer(Modifier.height(OursSpacing.sm))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = item.placeName ?: authorName,
                                        style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.ink,
                                    )
                                    Text(
                                        text = Formatters.dateShort(item.createdAt),
                                        style = OursTypography.tabular,
                                        color = colors.inkMuted,
                                    )
                                }
                                if (!item.note.isNullOrBlank()) {
                                    Spacer(Modifier.height(OursSpacing.xxs))
                                    Text(
                                        text = item.note,
                                        style = OursTypography.body,
                                        color = colors.inkSecondary,
                                    )
                                }
                                if (item.replies.isNotEmpty()) {
                                    Spacer(Modifier.height(OursSpacing.xxs))
                                    Text(
                                        text = "${item.replies.size} ${if (item.replies.size == 1) "reply" else "replies"}",
                                        style = OursTypography.caption,
                                        color = colors.inkMuted,
                                    )
                                }
                            }
                        }

                        if (state.memoriesNextBefore != null) {
                            item {
                                OursButton(
                                    text = "Load older memories",
                                    variant = OursButtonVariant.Secondary,
                                    loading = state.memoriesLoading,
                                    onClick = { vm.refreshMemories(loadMore = true) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
