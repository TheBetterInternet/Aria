package org.thebetterinternet.aria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BrowserBottomBar(
    tabCount: Int,
    isSearch: Boolean,
    onTabsClick: () -> Unit,
    onNewTabClick: () -> Unit,
    onNewTabDoubleClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastClickTime by remember { mutableStateOf(0L) }
    val doubleClickThreshold = 1000L // (ms) btw
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                FilledTonalIconButton(
                    onClick = onTabsClick,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Tab, contentDescription = "Tabs")
                }
                if (tabCount > 1) {
                    Surface(
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 10.dp, y = (-10).dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (tabCount > 99) ":D" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime < doubleClickThreshold) {
                        onNewTabDoubleClick()
                        lastClickTime = 0L
                    } else {
                        lastClickTime = currentTime
                        onNewTabClick()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    if (!isSearch) Icons.Default.Add else Icons.Default.Search,
                    contentDescription = if (!isSearch) "New Tab" else "Search",
                    modifier = Modifier.size(28.dp),
                )
            }

            FilledTonalIconButton(
                onClick = onSettingsClick,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}