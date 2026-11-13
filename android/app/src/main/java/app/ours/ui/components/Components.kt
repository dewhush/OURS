package app.ours.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.composed
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.ours.ui.theme.OursMotion
import app.ours.ui.theme.OursRadius
import app.ours.ui.theme.OursSpacing
import app.ours.ui.theme.OursTheme
import app.ours.ui.theme.OursTypography

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
        animationSpec = OursMotion.enterSpec(150),
        label = "buttonScale",
    )

    val containerColor = when (variant) {
        OursButtonVariant.Primary -> colors.action
        OursButtonVariant.Secondary -> colors.glassSurfaceStrong
        OursButtonVariant.Ghost -> Color.Transparent
        OursButtonVariant.Destructive -> colors.errorSoft
    }
    val contentColor = when (variant) {
        OursButtonVariant.Primary -> colors.onAction
        OursButtonVariant.Secondary -> colors.ink
        OursButtonVariant.Ghost -> colors.inkSecondary
        OursButtonVariant.Destructive -> colors.error
    }
    val border = when (variant) {
        OursButtonVariant.Secondary -> BorderStroke(1.dp, colors.glassBorder)
        else -> null
    }

    val effectiveAlpha = if (enabled) 1f else 0.45f

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = effectiveAlpha
            }
            .clip(RoundedCornerShape(OursRadius.control))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(OursRadius.control),
        color = containerColor,
        contentColor = contentColor,
        border = border,
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
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OursTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) OursMotion.PRESS_SCALE else 1f,
        animationSpec = OursMotion.enterSpec(150),
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
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier.then(clickModifier),
        shape = RoundedCornerShape(radius),
        color = if (frosted) colors.glassSurfaceStrong else colors.surface,
        border = BorderStroke(1.dp, if (frosted) colors.glassBorder else colors.border),
        shadowElevation = if (frosted) 6.dp else 0.dp,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
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
        containerColor = colors.glassSurfaceStrong,
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
                if (ringColor != null) Modifier.border(2.dp, ringColor, CircleShape)
                else Modifier.border(1.dp, colors.border, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = OursTypography.title.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
        )
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
        StatusTone.Live -> Triple(colors.liveSoft, colors.live, colors.live)
        StatusTone.Paused -> Triple(colors.pausedSoft, colors.paused, colors.paused)
        StatusTone.Muted -> Triple(colors.surfaceMuted, colors.inkSecondary, colors.inkMuted)
        StatusTone.Error -> Triple(colors.errorSoft, colors.error, colors.error)
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
fun OursErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OursTheme.colors
    OursCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Could not load",
            style = OursTypography.title,
            color = colors.error,
        )
        Spacer(Modifier.height(OursSpacing.xxs))
        Text(
            text = message,
            style = OursTypography.caption,
            color = colors.inkSecondary,
        )
        Spacer(Modifier.height(OursSpacing.sm))
        OursButton(
            text = "Try again",
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
            initialScale = 0.95f,
            animationSpec = OursMotion.enterSpec(),
        ),
        exit = fadeOut(OursMotion.exitSpec()) + scaleOut(
            targetScale = 0.95f,
            animationSpec = OursMotion.exitSpec(),
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(OursRadius.control),
            color = colors.liveSoft,
            border = BorderStroke(1.dp, colors.live.copy(alpha = 0.35f)),
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
                        .background(colors.live)
                )
                Text(
                    text = message,
                    style = OursTypography.label.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.live,
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
    val offsetY = remember { Animatable(12f) }

    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * OursMotion.STAGGER_MS).toLong()
        if (delayMs > 0) delay(delayMs)
        alpha.animateTo(1f, OursMotion.enterSpec())
    }
    LaunchedEffect(Unit) {
        val delayMs = (index.coerceIn(0, 6) * OursMotion.STAGGER_MS).toLong()
        if (delayMs > 0) delay(delayMs)
        offsetY.animateTo(0f, OursMotion.enterSpec())
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
        animationSpec = OursMotion.enterSpec(150),
        label = "pressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
