package org.thebetterinternet.aria

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onBackClick: () -> Unit,
    context: Context
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val middleShape = RoundedCornerShape(4.dp)
    val bottomShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Aria",
                    subtitle = "A beautiful Android browser by The Better Internet",
                    onClick = {})
                SettingsItem(
                    icon = Icons.Default.Update,
                    title = "Version",
                    subtitle = "v${
                        getVersionName(LocalContext.current).replace(
                            "-nightly",
                            " (Nightly Build)"
                        )
                    }",
                    onClick = {},
                    shape = middleShape
                )
                SettingsItem(
                    icon = Icons.Default.Numbers,
                    title = "Version Code",
                    subtitle = getVersionCode(LocalContext.current),
                    onClick = {},
                    shape = middleShape
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Source Code",
                    subtitle = "Access Aria's source code to modify, learn or do anything with it",
                    onClick = {
                        val i: Intent =
                            Intent(Intent.ACTION_VIEW); i.setData("https://github.com/TheBetterInternet/Aria".toUri()); context.startActivity(
                        i
                    )
                    },
                    shape = middleShape
                )
                SettingsItem(
                    icon = Icons.Default.CarCrash,
                    title = "Initiate crash",
                    subtitle = "Crash Aria for testing purposes",
                    onClick = { throw Exception("Crash Test") },
                    shape = bottomShape
                )
            }
        }
    }
}