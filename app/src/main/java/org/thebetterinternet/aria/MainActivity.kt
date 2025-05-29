package org.thebetterinternet.aria
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse
import org.thebetterinternet.aria.ui.theme.*
import kotlin.math.abs
import kotlin.math.absoluteValue

data class BrowserTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "https://www.google.com",
    val geckoSession: GeckoSession? = null,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val intentUrl = handleIncomingIntent(intent)
        setContent {
            AriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AriaBrowser(initialUrl = intentUrl)
                }
            }
        }
    }
    private fun handleIncomingIntent(intent: Intent): String? {
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.toString()
            }
            else -> null
        }
    }
}

class TabPagerAdapter(
    private var tabs: List<BrowserTab>,
    private val fragmentManager: FragmentManager,
    private val lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return TabFragment.newInstance(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs(newTabs: List<BrowserTab>) {
        tabs = newTabs
        notifyDataSetChanged()
    }

    fun getTab(position: Int): BrowserTab? = tabs.getOrNull(position)
}

class TabFragment : Fragment() {
    private var tabPosition: Int = -1
    private var geckoView: GeckoView? = null

    companion object {
        private const val ARG_TAB_POSITION = "tab_position"
        private var tabsReference: List<BrowserTab> = emptyList()

        fun newInstance(position: Int): TabFragment {
            return TabFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TAB_POSITION, position)
                }
            }
        }

        fun setTabsReference(tabs: List<BrowserTab>) {
            tabsReference = tabs
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tabPosition = arguments?.getInt(ARG_TAB_POSITION) ?: -1
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return GeckoView(requireContext()).also { geckoView ->
            this.geckoView = geckoView
            geckoView.isNestedScrollingEnabled = true
            geckoView.isVerticalScrollBarEnabled = true
            val tab = tabsReference.getOrNull(tabPosition)
            tab?.geckoSession?.let { session ->
                geckoView.setSession(session)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        geckoView = null
    }
}

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
    val homePage = "https://google.com" // placeholder.
    val searchEngine = "https://google.com/search?q=%s" // placeholder.
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
                        startDownload(context, response.uri, extractFilename(response) ?: "download")
                        scope.launch { snackbarHostState.showSnackbar(message = "Downloading file...") }
                    } else {
                        val i = Intent(Intent.ACTION_VIEW)
                        i.data = response.uri.toUri()
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        if (i.resolveActivity(context.packageManager) != null) {
                            context.startActivity(i)
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
                                tab.copy(url = newUrl, title = newUrl.substringAfter("://").substringBefore("/").ifEmpty { "New Tab" })
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
                                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
                onNewTab = {
                    val newSession = GeckoSession().apply { open(geckoRuntime) }
                    tabs = tabs + BrowserTab(geckoSession = newSession)
                    currentTabIndex = tabs.size - 1
                    showBottomSheet = false
                },
                onInvaildUrl = { url ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Invalid link! Searching instead")
                    }
                    showBottomSheet = false
                    currentSession?.loadUri(searchEngine.replaceFirst("%s", url))
                },
                homePage = homePage,
                searchEngine = searchEngine
            )
        }
    }

        androidx.compose.animation.AnimatedVisibility(
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
            onHomepageClick = {},
            onAboutClick = { showAbout = true },
            onSearchClick = {},
            onUpdateClick = {},
            onDownloadsClick = {},
            homePage = homePage
        )
        }
    androidx.compose.animation.AnimatedVisibility(
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TabManager(
    tabs: List<BrowserTab>,
    currentTabIndex: Int,
    geckoRuntime: GeckoRuntime,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        pageCount = { tabs.size },
        initialPage = currentTabIndex
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val tab = tabs[pageIndex]
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            val scale = lerp(0.85f, 0.8f, 0.8f - pageOffset.absoluteValue.coerceAtMost(0.6f))
            val alpha = lerp(0.6f, 1f, 1f - pageOffset.absoluteValue.coerceAtMost(1f))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 64.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        rotationY = pageOffset * 15f
                    }
            ) {
                TabCard(
                    tab = tab,
                    isSelected = pageIndex == currentTabIndex,
                    onTabClick = { onTabSelected(pageIndex) },
                    onCloseClick = { onTabClosed(pageIndex) },
                    isCurrentPage = pageIndex == pagerState.currentPage ||
                            (pageIndex == pagerState.currentPage - 1 && pagerState.currentPageOffsetFraction > 0) ||
                            (pageIndex == pagerState.currentPage + 1 && pagerState.currentPageOffsetFraction < 0),
                    onRefreshClick = { tab.geckoSession?.reload() }
                )
            }
        }
    }
}

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

    Card(
        modifier = Modifier
            .fillMaxSize()
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    FilledIconButton(
        shapes = IconButtonDefaults.shapes(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        interactionSource = interactionSource
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + fraction * (stop - start)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewTabBottomSheet(
    currentUrl: String,
    onNavigate: (String) -> Unit,
    onNewTab: () -> Unit,
    onInvaildUrl: (String) -> Unit,
    onSettings: () -> Unit,
    homePage: String,
    searchEngine: String,
) {
    var urlText by remember { mutableStateOf(currentUrl) }

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
        Row(modifier = Modifier
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
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
                    if (isUrl) { onNavigate(urlText) } else { onInvaildUrl(urlText) }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickLink(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    Button(
        shapes = ButtonDefaults.shapes(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onBackClick: () -> Unit,
    onHomepageClick: () -> Unit,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onAboutClick: () -> Unit,
    homePage: String,
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
                colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        subtitle = homePage,
                        onClick = onHomepageClick
                    )
                    SettingsItem(
                        icon = Icons.Default.Search,
                        title = "Search Engine",
                        subtitle = "Choose your default search provider",
                        onClick = onSearchClick,
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
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp
    )
) {
    val ripple = ripple(
        bounded = true,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    indication = ripple,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            shape = shape,
            color = if (enabled)
                MaterialTheme.colorScheme.surfaceContainer
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.38f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = subtitle,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


private fun startDownload(context: Context, url: String, filename: String) {
    val request = DownloadManager.Request(url.toUri())
        .setTitle(filename)
        .setDescription("Downloading file...")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}
private fun extractFilename(response: WebResponse): String {
    response.headers["content-disposition"]?.let { contentDisposition ->
        val filenameRegex = "filename[*]?\\s*=\\s*[\"']?([^;\"'\\n\\r]+)".toRegex(RegexOption.IGNORE_CASE)
        filenameRegex.find(contentDisposition)?.groupValues?.get(1)?.let { filename ->
            return filename.trim()
        }
    }

    return extractFilenameFromUrl(response.uri)
}

private fun extractFilenameFromUrl(url: String): String {
    return try {
        val uri = url.toUri()
        val path = uri.path ?: ""
        val filename = path.substringAfterLast('/')

        if (filename.isNotEmpty() && filename.contains('.')) {
            filename
        } else {
            "download_${System.currentTimeMillis()}"
        }
    } catch (e: Exception) {
        "download_${System.currentTimeMillis()}"
    }
}

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
                colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                SettingsItem(icon = Icons.Default.Info, title = "Aria", subtitle = "A beautiful Android browser by The Better Internet", onClick = {})
                SettingsItem(icon = Icons.Default.Update, title = "Version", subtitle = "v${getVersionName(LocalContext.current).replace("-nightly", " (Nightly Build)")}", onClick = {}, shape = middleShape)
                SettingsItem(icon = Icons.Default.Numbers, title = "Version Code", subtitle = getVersionCode(LocalContext.current), onClick = {}, shape = middleShape)
                SettingsItem(icon = Icons.Default.Code, title = "Github", subtitle = "https://github.com/TheBetterInternet/Aria", onClick = { val i: Intent = Intent(Intent.ACTION_VIEW); i.setData("https://github.com/TheBetterInternet/Aria".toUri()); context.startActivity(i) }, shape = middleShape)
            }
        }
    }
}

fun getVersionName(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

@Suppress("DEPRECATION")
fun getVersionCode(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        "${packageInfo.versionCode}" ?: "Unknown" // because API 21 cant do packageInfo.longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}
