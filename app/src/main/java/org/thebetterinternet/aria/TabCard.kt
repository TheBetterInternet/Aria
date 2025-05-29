package org.thebetterinternet.aria

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabCard(
    tab: BrowserTab,
    isSelected: Boolean,
    onTabClick: () -> Unit,
    onCloseClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isCurrentPage: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300)
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(10.dp))
        Card(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabClick()
                }
                .shadow(elevation = elevation, shape = RoundedCornerShape(16.dp)),
            border = BorderStroke(2.dp, borderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tab.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onRefreshClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Tab",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        FilledTonalIconButton(
                            onClick = onCloseClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close Tab",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        tab.geckoSession?.let { session ->
                            AndroidView(
                                factory = { context ->
                                    GeckoView(context).apply {
                                        setSession(session)
                                        isFocusable = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { },
                                            onDragEnd = { },
                                            onDrag = { change, dragAmount ->
                                                if (abs(dragAmount.x) < abs(dragAmount.y)) {
                                                    change.consume()
                                                }
                                            }
                                        )
                                    },
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = tab.isLoading,
                            enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                            exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }
}