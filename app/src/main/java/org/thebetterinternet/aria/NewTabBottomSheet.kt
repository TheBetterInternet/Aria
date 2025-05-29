package org.thebetterinternet.aria

import android.util.Patterns
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewTabBottomSheet(
    currentUrl: String,
    onNavigate: (String) -> Unit,
    onInvaildUrl: (String) -> Unit,
    onSettings: () -> Unit,
    homePage: String,
) {
    var urlText by remember {
        if (currentUrl != homePage && currentUrl != "${homePage}/") {
            mutableStateOf(currentUrl)
        } else {
            mutableStateOf("")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(5.dp))
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        "Search or enter URL",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val isUrl = Patterns.WEB_URL.matcher(urlText).matches()
                    if (isUrl) {
                        onNavigate(urlText)
                    } else {
                        onInvaildUrl(urlText)
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
            Spacer(Modifier.size(5.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            QuickLink(
                icon = Icons.Default.Home,
                onClick = { onNavigate(homePage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            QuickLink(
                icon = Icons.Default.Favorite,
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            QuickLink(
                icon = Icons.Default.History,
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            QuickLink(
                icon = Icons.Default.Settings,
                onClick = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}