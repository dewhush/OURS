package app.ours.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

enum class OursBreakpoint { Compact, Standard, Expanded }

data class OursLayoutSpec(
    val breakpoint: OursBreakpoint,
    val horizontalPadding: Dp,
    val maxContentWidth: Dp,
)

@Composable
fun rememberOursLayoutSpec(): OursLayoutSpec {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 360 -> OursLayoutSpec(
            breakpoint = OursBreakpoint.Compact,
            horizontalPadding = 14.dp,
            maxContentWidth = Dp.Unspecified,
        )
        widthDp < 600 -> OursLayoutSpec(
            breakpoint = OursBreakpoint.Standard,
            horizontalPadding = 16.dp,
            maxContentWidth = Dp.Unspecified,
        )
        else -> OursLayoutSpec(
            breakpoint = OursBreakpoint.Expanded,
            horizontalPadding = 24.dp,
            maxContentWidth = 560.dp,
        )
    }
}

fun Modifier.oursContentWidth(spec: OursLayoutSpec): Modifier =
    if (spec.maxContentWidth != Dp.Unspecified) {
        this.widthIn(max = spec.maxContentWidth).fillMaxWidth()
    } else {
        this.fillMaxWidth()
    }

fun Modifier.oursHeaderInsets(): Modifier = composed {
    windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
        .padding(top = 12.dp)
}

@Composable
fun OursGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = OursTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas),
        content = content,
    )
}

/**
 * Apple HIG translucent material surface (GPU-cached via drawWithCache):
 * clean vertical material fill without artificial diagonal white spotlight fog,
 * subtle ambient shadow clipped outside, 0.75.dp top-edge specular hairline,
 * and a crisp perimeter hairline border.
 */
fun Modifier.oursLiquidGlass(
    radius: Dp,
    colors: app.ours.ui.theme.OursColors,
    elevation: Dp = 4.dp,
    tinted: Boolean = false,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .drawWithCache {
            val elevPx = elevation.toPx()
            val rPx = radius.toPx().coerceAtMost(size.minDimension / 2f)
            val cardPath = Path().apply {
                addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(rPx, rPx)))
            }
            val surfaceBrush = if (tinted) {
                Brush.verticalGradient(
                    colors = listOf(colors.actionSoft, colors.glassGradientEnd),
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(colors.glassGradientStart, colors.glassGradientEnd),
                )
            }
            val rimBrush = Brush.verticalGradient(
                colors = listOf(colors.glassHighlightTopLeft, colors.glassRimBottomRight),
            )
            val strokePx = 0.75.dp.toPx()

            onDrawWithContent {
                if (elevPx > 0f) {
                    clipPath(cardPath, clipOp = ClipOp.Difference) {
                        val steps = 3
                        for (i in 1..steps) {
                            val spread = (elevPx * i) / steps
                            val alpha = (0.04f * (steps - i + 1)) / steps
                            drawRoundRect(
                                color = Color.Black.copy(alpha = alpha),
                                topLeft = Offset(-spread * 0.4f, spread * 0.35f),
                                size = Size(size.width + spread * 0.8f, size.height + spread),
                                cornerRadius = CornerRadius(rPx + spread * 0.4f, rPx + spread * 0.4f),
                            )
                        }
                    }
                }
                drawRoundRect(
                    brush = surfaceBrush,
                    cornerRadius = CornerRadius(rPx, rPx),
                )
                drawContent()
                if (size.width > rPx * 1.2f) {
                    drawLine(
                        color = colors.glassInnerHighlight,
                        start = Offset(rPx * 0.6f, strokePx),
                        end = Offset(size.width - rPx * 0.6f, strokePx),
                        strokeWidth = strokePx,
                    )
                }
                drawRoundRect(
                    brush = rimBrush,
                    cornerRadius = CornerRadius(rPx, rPx),
                    style = Stroke(width = strokePx),
                )
            }
        }
        .clip(shape)
}

@Composable
fun OursGlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = OursRadius.card,
    padding: PaddingValues = PaddingValues(OursSpacing.md),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    OursCard(
        modifier = modifier,
        radius = radius,
        padding = padding,
        frosted = true,
        elevation = elevation,
        onClick = onClick,
        content = content,
    )
}

enum class OursButtonVariant { Primary, Secondary, Ghost, Destructive }

@Composable
fun OursButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OursButtonVariant = OursButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = OursTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) OursMotion.PRESS_SCALE else 1f,
        animationSpec = OursMotion.springSnappy(),
        label = "buttonScale",
    )

    val containerColor = when (variant) {
        OursButtonVariant.Primary -> colors.action
        OursButtonVariant.Secondary -> colors.surfaceMuted
        OursButtonVariant.Ghost -> Color.Transparent
        OursButtonVariant.Destructive -> colors.dangerSoft
    }
    val contentColor = when (variant) {
        OursButtonVariant.Primary -> colors.onAction
        OursButtonVariant.Secondary -> colors.ink
        OursButtonVariant.Ghost -> colors.inkSecondary
        OursButtonVariant.Destructive -> colors.danger
    }
    val effectiveAlpha = if (enabled) 1f else 0.42f
    val buttonShape = RoundedCornerShape(OursRadius.control)

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = effectiveAlpha
            }
            .clip(buttonShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = buttonShape,
        color = containerColor,
        contentColor = contentColor,
        border = null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(OursSpacing.xs))
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(OursSpacing.xs))
            }
            Text(
                text = text,
                style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun OursCard(
    modifier: Modifier = Modifier,
    radius: Dp = OursRadius.card,
    padding: PaddingValues = PaddingValues(OursSpacing.md),
    frosted: Boolean = false,
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OursTheme.colors
    val isDark = OursTheme.isDark
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) OursMotion.PRESS_SCALE else 1f,
        animationSpec = OursMotion.springSnappy(),
        label = "cardScale",
    )

    val clickModifier = if (onClick != null) {
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(radius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier
    }

    val shape = RoundedCornerShape(radius)
    Surface(
        modifier = modifier
            .then(
                if (frosted) Modifier.oursLiquidGlass(radius = radius, colors = colors, elevation = elevation)
                else Modifier
            )
            .then(clickModifier),
        shape = shape,
        color = if (frosted) Color.Transparent else colors.surface,
        border = when {
            frosted -> null
            isDark -> BorderStroke(0.5.dp, colors.border)
            else -> BorderStroke(0.75.dp, colors.border)
        },
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/**
 * Authentic Apple iOS segmented control with a physics-driven sliding pill indicator
 * and 44.dp minimum touch height (R-03, R-32).
 */
@Composable
fun <T> OursSegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    minSegmentWidth: Dp = 54.dp,
) {
    val colors = OursTheme.colors
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val count = options.size.coerceAtLeast(1)

    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = 44.dp)
            .widthIn(min = minSegmentWidth * count + 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceMuted)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val segmentWidth = maxWidth / count
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = OursMotion.springSnappy(),
            label = "segmentThumbOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .height(38.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.action),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.onAction else colors.inkSecondary,
                    animationSpec = OursMotion.springSnappy(),
                    label = "segmentTextColor",
                )
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(value) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OursBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OursTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = OursRadius.sheet, topEnd = OursRadius.sheet),
        containerColor = colors.surface,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OursSpacing.md)
                .padding(bottom = OursSpacing.lg),
            content = content,
        )
    }
}

@Composable
fun OursAvatar(
    name: String,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    size: Dp = 40.dp,
    ringColor: Color? = null,
    accent: Boolean = false,
) {
    val colors = OursTheme.colors
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val bg = if (accent) colors.actionSoft else colors.surfaceMuted
    val fg = if (accent) colors.action else colors.ink

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (ringColor != null) Modifier.border(1.5.dp, ringColor, CircleShape)
                else Modifier.border(0.75.dp, colors.border, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

enum class StatusTone { Live, Paused, Muted, Error }

@Composable
fun OursStatusChip(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    val (bg, fg, dot) = when (tone) {
        StatusTone.Live -> Triple(colors.successSoft, colors.success, colors.success)
        StatusTone.Paused -> Triple(colors.pausedSoft, colors.paused, colors.paused)
        StatusTone.Muted -> Triple(colors.surfaceMuted, colors.inkSecondary, colors.inkMuted)
        StatusTone.Error -> Triple(colors.dangerSoft, colors.danger, colors.danger)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OursRadius.chip))
            .background(bg)
            .padding(horizontal = OursSpacing.xs, vertical = OursSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Text(
            text = label,
            style = OursTypography.label,
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
fun OursLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OursSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OursSpacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = colors.action,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = message,
            style = OursTypography.caption,
            color = colors.inkSecondary,
        )
    }
}

@Composable
fun OursEmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    illustrationRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = OursTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(OursSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OursSpacing.xs),
    ) {
        if (illustrationRes != null) {
            Image(
                painter = painterResource(id = illustrationRes),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(OursSpacing.xxs))
        }
        Text(
            text = title,
            style = OursTypography.title,
            color = colors.ink,
        )
        Text(
            text = detail,
            style = OursTypography.caption,
            color = colors.inkSecondary,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(OursSpacing.xs))
            OursButton(
                text = actionLabel,
                onClick = onAction,
                variant = OursButtonVariant.Secondary,
            )
        }
    }
}

@Composable
fun OursNudgeWaveOverlay(
    nudgeKey: Int,
    modifier: Modifier = Modifier,
) {
    if (nudgeKey <= 0) return
    val colors = OursTheme.colors
    val ring1 = remember(nudgeKey) { Animatable(0f) }
    val ring2 = remember(nudgeKey) { Animatable(0f) }
    val travel = remember(nudgeKey) { Animatable(0f) }

    LaunchedEffect(nudgeKey) {
        ring1.animateTo(1f, OursMotion.enterSpec(900))
    }
    LaunchedEffect(nudgeKey) {
        delay(160)
        ring2.animateTo(1f, OursMotion.enterSpec(900))
    }
    LaunchedEffect(nudgeKey) {
        travel.animateTo(1f, OursMotion.enterSpec(850))
    }

    val active = ring1.value < 1f || ring2.value < 1f || travel.value < 1f
    if (!active) return

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val origin = Offset(size.width * 0.5f, size.height * 0.62f)
            val target = Offset(size.width * 0.5f, size.height * 0.34f)
            val maxR = size.minDimension * 0.36f

            if (ring1.value in 0.01f..0.99f) {
                drawCircle(
                    color = colors.action.copy(alpha = (1f - ring1.value) * 0.55f),
                    radius = maxR * ring1.value,
                    center = origin,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            if (ring2.value in 0.01f..0.99f) {
                drawCircle(
                    color = colors.action.copy(alpha = (1f - ring2.value) * 0.45f),
                    radius = maxR * ring2.value,
                    center = origin,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            if (travel.value in 0.01f..0.99f) {
                val arcPath = Path().apply {
                    moveTo(origin.x, origin.y)
                    quadraticTo(
                        size.width * 0.68f,
                        size.height * 0.48f,
                        target.x,
                        target.y,
                    )
                }
                val pm = PathMeasure()
                pm.setPath(arcPath, false)
                val seg = Path()
                val startD = (travel.value - 0.25f).coerceAtLeast(0f) * pm.length
                val endD = travel.value * pm.length
                pm.getSegment(startD, endD, seg, true)
                drawPath(
                    path = seg,
                    color = colors.action.copy(alpha = (1f - travel.value) * 0.85f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                val tip = pm.getPosition(endD)
                drawCircle(
                    color = colors.action.copy(alpha = 1f - travel.value * 0.6f),
                    radius = 6.dp.toPx(),
                    center = tip,
                )
            }
        }
    }
}

@Composable
fun OursErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    val strings = OursTheme.strings
    OursCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.couldNotLoadTitle,
            style = OursTypography.title,
            color = colors.danger,
        )
        Spacer(Modifier.height(OursSpacing.xxs))
        Text(
            text = message,
            style = OursTypography.caption,
            color = colors.inkSecondary,
        )
        Spacer(Modifier.height(OursSpacing.sm))
        OursButton(
            text = strings.tryAgainCta,
            onClick = onRetry,
            variant = OursButtonVariant.Secondary,
        )
    }
}

@Composable
fun OursSendConfirmation(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(OursMotion.enterSpec()) + scaleIn(
            initialScale = 0.92f,
            animationSpec = OursMotion.springSnappy(),
        ),
        exit = fadeOut(OursMotion.exitSpec()) + scaleOut(
            targetScale = 0.92f,
            animationSpec = OursMotion.exitSpec(),
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(OursRadius.control),
            color = colors.successSoft,
            border = BorderStroke(1.dp, colors.success.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = OursSpacing.md, vertical = OursSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OursSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.success)
                )
                Text(
                    text = message,
                    style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.success,
                )
            }
        }
    }
}

@Composable
fun OursStaggeredItem(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(14f) }

    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * OursMotion.STAGGER_MS).toLong()
        if (delayMs > 0) delay(delayMs)
        alpha.animateTo(1f, OursMotion.enterSpec())
    }
    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * OursMotion.STAGGER_MS).toLong()
        if (delayMs > 0) delay(delayMs)
        offsetY.animateTo(0f, OursMotion.springFluid())
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = offsetY.value * density
        },
    ) {
        content()
    }
}

fun Modifier.oursPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) OursMotion.PRESS_SCALE else 1f,
        animationSpec = OursMotion.springSnappy(),
        label = "pressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun OursInterlockedRingsMark(
    modifier: Modifier = Modifier,
    leftRingColor: Color = OursTheme.colors.ink,
    rightRingColor: Color = OursTheme.colors.action,
    backgroundColor: Color = OursTheme.colors.canvas,
    progress: Float = 1f,
    convergeProgress: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension / 100f
        val r = 18f * s
        val stroke = 6.5f * s
        val cutStroke = 10.5f * s
        val sweep = (360f * progress.coerceIn(0f, 1f))
        val c = convergeProgress.coerceIn(0f, 1f)
        val sep = 5f * (1f - c)

        val leftCenter = Offset((39f - sep) * s, 50f * s)
        val rightCenter = Offset((61f + sep) * s, 50f * s)
        val leftTopLeft = Offset(leftCenter.x - r, leftCenter.y - r)
        val rightTopLeft = Offset(rightCenter.x - r, rightCenter.y - r)
        val arcSize = Size(r * 2f, r * 2f)

        drawArc(
            color = leftRingColor,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = leftTopLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = rightRingColor,
            startAngle = 90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = rightTopLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )

        if (progress > 0.7f) {
            drawArc(
                color = backgroundColor,
                startAngle = -78f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = leftTopLeft,
                size = arcSize,
                style = Stroke(width = cutStroke, cap = StrokeCap.Butt),
            )
            drawArc(
                color = leftRingColor,
                startAngle = -78f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = leftTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            drawArc(
                color = backgroundColor,
                startAngle = 102f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = rightTopLeft,
                size = arcSize,
                style = Stroke(width = stroke + 4f * s, cap = StrokeCap.Butt),
            )
            drawArc(
                color = rightRingColor,
                startAngle = 102f,
                sweepAngle = 52f,
                useCenter = false,
                topLeft = rightTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
