package org.thebetterinternet.aria

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import kotlinx.coroutines.launch

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onBackClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val middleShape = RoundedCornerShape(4.dp)
    val bottomShape = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    val homepage by remember {
        mutableStateOf(prefs.getString("home", "https://google.com") ?: "https://google.com")
    }
    val searchengine by remember {
        mutableStateOf(
            prefs.getString("search", "https://google.com/search?q=%s")
                ?: "https://google.com/search?q=%s"
        )
    }
    var input by remember { mutableStateOf("") }
    var whatToInput by remember { mutableStateOf("") }
    var onInput: () -> Unit = {}
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
                SettingsSection(title = "Browsing") {
                    SettingsItem(
                        icon = Icons.Default.Home,
                        title = "Homepage",
                        subtitle = homepage,
                        onClick = {
                            input = homepage
                            whatToInput = "Homepage"
                            showDialog = true
                            onInput = {
                                prefs.edit() { putString("home", input) }
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.Search,
                        title = "Search Engine",
                        subtitle = searchengine,
                        onClick = {
                            input = searchengine
                            whatToInput = "Search Engine"
                            showDialog = true
                            onInput = {
                                prefs.edit() { putString("search", input) }
                            }
                        },
                        shape = middleShape
                    )
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Downloads",
                        subtitle = "Coming soon!",
                        onClick = onDownloadsClick,
                        shape = bottomShape
                    )
                }
            }

            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About Aria",
                        subtitle = "Version info and about",
                        onClick = onAboutClick
                    )
                    SettingsItem(
                        icon = Icons.Default.Update,
                        title = "Check for Updates",
                        subtitle = "Keep your browser up to date",
                        onClick = onUpdateClick,
                        shape = bottomShape
                    )
                }
            }
        }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        onInput()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Restart Aria to take effect."
                            )
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                    }) {
                        Text("Cancel")
                    }
                },
                title = { Text(whatToInput) },
                text = {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            )
        }
    }
}