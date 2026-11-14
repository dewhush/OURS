package app.ours.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursErrorState
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursMapView
import app.ours.ui.components.OursStaggeredItem
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursPressScale
import app.ours.ui.components.rememberOursLayoutSpec
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import app.ours.util.Formatters
import coil3.compose.AsyncImage

private enum class MemoryFilter { All, Photos, Places }

@Composable
fun MemoriesScreen(
    state: OursUiState,
    vm: OursViewModel,
    onOpenFullPhoto: (CheckInDto) -> Unit,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val layoutSpec = rememberOursLayoutSpec()
    var showMapMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MemoryFilter.All) }

    val filteredMemories = remember(state.memories, searchQuery, filter) {
        val q = searchQuery.trim().lowercase()
        state.memories.filter { item ->
            val matchesFilter = when (filter) {
                MemoryFilter.All -> true
                MemoryFilter.Photos -> item.photoId.isNotBlank()
                MemoryFilter.Places -> !item.placeName.isNullOrBlank()
            }
            val matchesQuery = q.isEmpty() ||
                (item.placeName?.lowercase()?.contains(q) == true) ||
                (item.note?.lowercase()?.contains(q) == true)
            matchesFilter && matchesQuery
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .oursHeaderInsets(),
        contentAlignment = Alignment.TopCenter,
    ) {
    Column(
        modifier = Modifier
            .oursContentWidth(layoutSpec)
            .fillMaxHeight(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layoutSpec.horizontalPadding, vertical = OursSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(strings.memoriesTitle, style = OursTypography.display, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs)) {
                OursButton(
                    text = strings.timelineTab,
                    variant = if (!showMapMode) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                    onClick = { showMapMode = false },
                )
                OursButton(
                    text = strings.mapTab,
                    variant = if (showMapMode) OursButtonVariant.Primary else OursButtonVariant.Secondary,
                    onClick = { showMapMode = true },
                )
            }
        }

        OursSearchFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            placeholder = strings.searchPlaceholder,
            filters = listOf(
                MemoryFilter.All to strings.filterAll,
                MemoryFilter.Photos to strings.filterPhotos,
                MemoryFilter.Places to strings.filterPlaces,
            ),
            selectedFilter = filter,
            onSelectFilter = { filter = it },
            modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = OursSpacing.xxs),
        )

        if (showMapMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                OursMapView(
                    modifier = Modifier.fillMaxSize(),
                    highlights = filteredMemories,
                    places = state.places,
                    onHighlightClick = onOpenFullPhoto,
                )
                if (state.memories.isEmpty() && !state.memoriesLoading) {
                    OursCard(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(OursSpacing.md)
                            .fillMaxWidth(),
                        frosted = true,
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
                        itemsIndexed(filteredMemories, key = { _, it -> it.id }) { idx, item ->
                            val authorName = if (item.authorId == state.me?.id) {
                                state.me.name
                            } else {
                                state.partner?.name ?: "Partner"
                            }
                            OursStaggeredItem(index = idx) {
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> OursSearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    filters: List<Pair<T, String>>,
    selectedFilter: T,
    onSelectFilter: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OursRadius.card),
            color = colors.glassSurfaceStrong,
            border = BorderStroke(1.dp, colors.glassBorder),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OursSpacing.sm, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = OursTypography.body,
                            color = colors.inkMuted,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = OursTypography.body.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.action),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    val clearInteraction = remember { MutableInteractionSource() }
                    Icon(
                        imageVector = OursIcons.Close,
                        contentDescription = "Clear",
                        tint = colors.inkMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .oursPressScale(clearInteraction)
                            .clickable(
                                interactionSource = clearInteraction,
                                indication = null,
                                onClick = { onQueryChange("") },
                            ),
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
        ) {
            filters.forEach { (value, label) ->
                val selected = value == selectedFilter
                val chipInteraction = remember { MutableInteractionSource() }
                Surface(
                    modifier = Modifier
                        .oursPressScale(chipInteraction)
                        .clip(RoundedCornerShape(OursRadius.chip))
                        .clickable(
                            interactionSource = chipInteraction,
                            indication = null,
                            onClick = { onSelectFilter(value) },
                        ),
                    shape = RoundedCornerShape(OursRadius.chip),
                    color = if (selected) colors.actionSoft else colors.surface,
                    border = BorderStroke(1.dp, if (selected) colors.action else colors.border),
                ) {
                    Text(
                        text = label,
                        style = OursTypography.label.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) colors.action else colors.inkSecondary,
                        modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
