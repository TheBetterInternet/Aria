package org.thebetterinternet.aria

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AriaBrowser(initialUrl: String?) {
    var tabs by remember { mutableStateOf(listOf<BrowserTab>()) }
    var currentTabIndex by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTabManager by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    val geckoRuntime = remember { App.getRuntime(context) }
    val prefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    val homePage by remember {
        mutableStateOf(prefs.getString("home", "https://google.com") ?: "https://google.com")
    }
    val searchEngine by remember {
        mutableStateOf(
            prefs.getString("search", "https://google.com/search?q=%s")
                ?: "https://google.com/search?q=%s"
        )
    }
    geckoRuntime.settings.setExtensionsWebAPIEnabled(true)
    geckoRuntime.settings.setExtensionsProcessEnabled(true)
    geckoRuntime.settings.setAboutConfigEnabled(true)
    geckoRuntime.settings.setRemoteDebuggingEnabled(true)
    geckoRuntime.webExtensionController.enableExtensionProcessSpawning()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val swipeRefreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val url = initialUrl ?: homePage
            val initialSession = GeckoSession().apply { open(geckoRuntime) }
            tabs = listOf(BrowserTab(geckoSession = initialSession, url = url))
        }
    }

    val currentTab = if (tabs.isNotEmpty() && currentTabIndex < tabs.size) tabs[currentTabIndex] else null
    val currentSession = currentTab?.geckoSession
    LaunchedEffect(currentSession) {
        currentSession?.let { session ->
            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onCloseRequest(session: GeckoSession) {
                    if (tabs.size > 1) {
                        val newTabs = tabs.toMutableList()
                        newTabs[currentTabIndex].geckoSession?.close()
                        newTabs.removeAt(currentTabIndex)
                        tabs = newTabs
                        if (currentTabIndex > 0) {
                            currentTabIndex--
                        }
                    }
                }

                override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                    if (!response.requestExternalApp) {
                        startDownload(
                            context,
                            response.uri,
                            extractFilename(response) ?: "download"
                        )
                        scope.launch { snackbarHostState.showSnackbar(message = "Downloading file...") }
                    } else {
                        val i = Intent(Intent.ACTION_VIEW, response.uri.toUri()).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(i)
                        } catch (e: ActivityNotFoundException) {
                            // do nothing.
                        }
                    }
                }
            }
            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    url?.let { newUrl ->
                        tabs = tabs.mapIndexed { index, tab ->
                            if (index == currentTabIndex) {
                                tab.copy(
                                    url = newUrl,
                                    title = newUrl.substringAfter("://").substringBefore("/")
                                        .ifEmpty { "New Tab" })
                            } else tab
                        }
                    }
                }

                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    tabs = tabs.mapIndexed { index, tab ->
                        if (index == currentTabIndex) {
                            tab.copy(canGoBack = canGoBack)
                        } else tab
                    }
                }

                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    tabs = tabs.mapIndexed { index, tab ->
                        if (index == currentTabIndex) {
                            tab.copy(canGoForward = canGoForward)
                        } else tab
                    }
                }

            }

            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onPageStart(session: GeckoSession, url: String) {
                    tabs = tabs.mapIndexed { index, tab ->
                        if (index == currentTabIndex) tab.copy(isLoading = true) else tab
                    }
                }

                override fun onPageStop(session: GeckoSession, success: Boolean) {
                    tabs = tabs.mapIndexed { index, tab ->
                        if (index == currentTabIndex) tab.copy(isLoading = false) else tab
                    }
                }
            }

            session.loadUri(currentTab.url)
        }
    }

    BackHandler(enabled = currentTab?.canGoBack == true) {
        currentSession?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showTabManager,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = EaseInOutCubic)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = EaseInOutCubic)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                TabManager(
                    tabs = tabs,
                    currentTabIndex = currentTabIndex,
                    geckoRuntime = geckoRuntime,
                    onTabSelected = { index ->
                        currentTabIndex = index
                        showTabManager = false
                    },
                    onTabClosed = { index ->
                        if (tabs.size > 1) {
                            val newTabs = tabs.toMutableList()
                            newTabs[index].geckoSession?.close()
                            newTabs.removeAt(index)
                            tabs = newTabs
                            if (currentTabIndex >= index && currentTabIndex > 0) {
                                currentTabIndex--
                            }
                        }
                    },
                    onNewTab = {
                        val newSession = GeckoSession().apply { open(geckoRuntime) }
                        tabs = tabs + BrowserTab(geckoSession = newSession)
                        currentTabIndex = tabs.size - 1
                        showTabManager = false
                    },
                    onDismiss = { showTabManager = false }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !showTabManager,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = EaseInOutCubic)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300, easing = EaseInOutCubic)
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var viewPager: ViewPager2? by remember { mutableStateOf(null) }
                    AndroidView(
                        factory = { ctx ->
                            ViewPager2(ctx).apply {
                                viewPager = this
                                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                                isUserInputEnabled = false
                                offscreenPageLimit = 1
                                registerOnPageChangeCallback(object :
                                    ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        super.onPageSelected(position)
                                        currentTabIndex = position
                                    }
                                })
                            }
                        },
                        update = { vp ->
                            if (tabs.isNotEmpty()) {
                                TabFragment.setTabsReference(tabs)

                                val currentAdapter = vp.adapter as? TabPagerAdapter
                                if (currentAdapter == null) {
                                    val adapter = TabPagerAdapter(
                                        tabs = tabs,
                                        fragmentManager = activity.supportFragmentManager,
                                        lifecycle = activity.lifecycle
                                    )
                                    vp.adapter = adapter
                                } else {
                                    currentAdapter.updateTabs(tabs)
                                }

                                if (currentTabIndex != vp.currentItem && currentTabIndex < tabs.size) {
                                    vp.setCurrentItem(currentTabIndex, false)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTab?.isLoading == true,
                        enter = scaleIn(
                            animationSpec = tween(200),
                            transformOrigin = TransformOrigin.Center
                        ) + fadeIn(animationSpec = tween(200)),
                        exit = scaleOut(
                            animationSpec = tween(200),
                            transformOrigin = TransformOrigin.Center
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        PullToRefreshBox(
                            state = swipeRefreshState,
                            onRefresh = { currentSession?.reload() },
                            isRefreshing = currentTab!!.isLoading,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ContainedLoadingIndicator()
                                }
                            }
                        ) {
                            Spacer(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)
        BrowserBottomBar(
            tabCount = tabs.size,
            onTabsClick = { showTabManager = !showTabManager },
            onNewTabClick = {
                if (!showTabManager) {
                    showBottomSheet = true
                } else {
                    val newSession = GeckoSession().apply { open(geckoRuntime) }
                    tabs = tabs + BrowserTab(geckoSession = newSession)
                    currentTabIndex = tabs.size - 1
                    showTabManager = false
                }
            },
            onNewTabDoubleClick = {
                if (showTabManager) {
                    showBottomSheet = true
                } else {
                    val newSession = GeckoSession().apply { open(geckoRuntime) }
                    tabs = tabs + BrowserTab(geckoSession = newSession)
                    currentTabIndex = tabs.size - 1
                    showTabManager = false
                }
            },
            onSettingsClick = { showSettings = true },
            isSearch = !showTabManager,
            modifier = Modifier.navigationBarsPadding()
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NewTabBottomSheet(
                currentUrl = currentTab?.url ?: "",
                onNavigate = { url ->
                    currentSession?.loadUri(url)
                    showBottomSheet = false
                },
                onSettings = {
                    showBottomSheet = false
                    showSettings = true
                },
                onInvaildUrl = { url ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Invalid link! Searching instead")
                    }
                    showBottomSheet = false
                    currentSession?.loadUri(searchEngine.replaceFirst("%s", url))
                },
                homePage = homePage
            )
        }
    }

    AnimatedVisibility(
        visible = showSettings,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = EaseInOutCubic)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300, easing = EaseInOutCubic)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        SettingsPage(
            onBackClick = { showSettings = false },
            onAboutClick = { showAbout = true },
            onUpdateClick = {},
            onDownloadsClick = {},
        )
    }
    AnimatedVisibility(
        visible = showAbout,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = EaseInOutCubic)
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300, easing = EaseInOutCubic)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        AboutPage(onBackClick = { showAbout = false }, context = context)
    }
}