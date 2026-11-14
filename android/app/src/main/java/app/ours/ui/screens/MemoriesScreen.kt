package app.ours.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ours.R
import app.ours.data.api.CheckInDto
import app.ours.ui.OursUiState
import app.ours.ui.OursViewModel
import app.ours.ui.components.OursButton
import app.ours.ui.components.OursButtonVariant
import app.ours.ui.components.OursCard
import app.ours.ui.components.OursEmptyState
import app.ours.ui.components.OursErrorState
import app.ours.ui.components.OursGlassBackdrop
import app.ours.ui.components.OursIcons
import app.ours.ui.components.OursLoadingState
import app.ours.ui.components.OursMapView
import app.ours.ui.components.OursStaggeredItem
import app.ours.ui.components.oursContentWidth
import app.ours.ui.components.oursHeaderInsets
import app.ours.ui.components.oursLiquidGlass
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
    onQuickCheckIn: () -> Unit = {},
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    val layoutSpec = rememberOursLayoutSpec()
    var showMapMode by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(MemoryFilter.All) }
    val topHeaderInset = WindowInsets.statusBars.union(WindowInsets.displayCutout)
        .asPaddingValues()
        .calculateTopPadding()

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

    OursGlassBackdrop {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (showMapMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                OursMapView(
                    modifier = Modifier.fillMaxSize(),
                    partnerLocation = state.partnerLocation,
                    myLocation = state.myLocation,
                    highlights = filteredMemories,
                    places = state.places,
                    onHighlightClick = onOpenFullPhoto,
                )
                if (state.memories.isEmpty() && !state.memoriesLoading) {
                    OursCard(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(start = OursSpacing.md, end = OursSpacing.md, bottom = 104.dp)
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
            Box(
                modifier = Modifier
                    .oursContentWidth(layoutSpec)
                    .fillMaxSize(),
            ) {
                when {
                    state.memoriesLoading && state.memories.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 88.dp)) {
                            OursLoadingState("Loading saved highlights...")
                        }
                    }
                    state.memoriesError != null && state.memories.isEmpty() -> {
                        Box(Modifier.padding(start = OursSpacing.md, end = OursSpacing.md, top = topHeaderInset + 88.dp)) {
                            OursErrorState(state.memoriesError, onRetry = { vm.refreshMemories() })
                        }
                    }
                    state.memories.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 88.dp)) {
                            OursEmptyState(
                                title = if (strings.langCode == "id") "Belum ada kenangan tersimpan" else "No saved memories yet",
                                detail = "Check-ins expire after 24 hours. Tap Save on any check-in in Chat to keep it here forever.",
                                illustrationRes = R.drawable.ic_ours_memories_empty,
                                actionLabel = if (strings.langCode == "id") "Abadikan momen" else "Capture a moment",
                                onAction = onQuickCheckIn,
                            )
                        }
                    }
                    filteredMemories.isEmpty() -> {
                        Box(Modifier.fillMaxSize().padding(top = topHeaderInset + 88.dp)) {
                            OursEmptyState(
                                title = if (strings.langCode == "id") "Tidak ada hasil" else "No results",
                                detail = if (strings.langCode == "id") "Coba kata kunci atau filter lain." else "Try a different search or filter.",
                                illustrationRes = R.drawable.ic_ours_memories_empty,
                                actionLabel = if (strings.langCode == "id") "Hapus filter" else "Clear filters",
                                onAction = {
                                    searchQuery = ""
                                    filter = MemoryFilter.All
                                },
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = OursSpacing.md,
                                top = topHeaderInset + 88.dp,
                                end = OursSpacing.md,
                                bottom = 148.dp,
                            ),
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
                                    radius = 22.dp,
                                    padding = PaddingValues(12.dp),
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

        // Floating glass top header + collapsible search bar
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
                        .fillMaxWidth()
                        .padding(horizontal = OursSpacing.md, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = strings.memoriesTitle,
                        style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceMuted.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, colors.glassBorder),
                        ) {
                            Row(
                                modifier = Modifier.height(36.dp).padding(3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf(
                                    false to strings.timelineTab,
                                    true to strings.mapTab,
                                ).forEach { (isMap, label) ->
                                    val selected = showMapMode == isMap
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(9.dp))
                                            .clickable { showMapMode = isMap },
                                        shape = RoundedCornerShape(9.dp),
                                        color = if (selected) colors.action else Color.Transparent,
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = label,
                                                style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (selected) colors.onAction else colors.inkSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (showSearch) {
                                        showSearch = false
                                        searchQuery = ""
                                        filter = MemoryFilter.All
                                    } else {
                                        showSearch = true
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (showSearch) OursIcons.Close else OursIcons.Search,
                                contentDescription = if (showSearch) "Close search" else "Search memories",
                                tint = if (showSearch) colors.action else colors.ink,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSearch || searchQuery.isNotEmpty() || filter != MemoryFilter.All) {
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
                )
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .oursLiquidGlass(radius = OursRadius.control, colors = colors, elevation = 3.dp),
            shape = RoundedCornerShape(OursRadius.control),
            color = Color.Transparent,
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
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                            .oursPressScale(clearInteraction)
                            .clickable(
                                interactionSource = clearInteraction,
                                indication = null,
                                onClick = { onQueryChange("") },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = OursIcons.Close,
                            contentDescription = "Clear",
                            tint = colors.inkMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
                        .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                        .oursPressScale(chipInteraction)
                        .oursLiquidGlass(
                            radius = OursRadius.chip,
                            colors = colors,
                            elevation = if (selected) 3.dp else 1.dp,
                            tinted = selected,
                        )
                        .clickable(
                            interactionSource = chipInteraction,
                            indication = null,
                            onClick = { onSelectFilter(value) },
                        ),
                    shape = RoundedCornerShape(OursRadius.chip),
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = OursSpacing.sm, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = OursTypography.label.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                            color = if (selected) colors.action else colors.inkSecondary,
                        )
                    }
                }
            }
        }
    }
}
