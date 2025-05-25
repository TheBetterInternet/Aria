package org.thebetterinternet.aria
import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.Log
//import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.thebetterinternet.aria.ui.theme.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.window.OnBackInvokedDispatcher
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PageSize
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil3.Uri
import coil3.compose.AsyncImage
import java.net.URL
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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
        super.onCreate(savedInstanceState)
        setContent {
            AriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AriaBrowser()
                }
            }
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
fun AriaBrowser() {
    var tabs by remember { mutableStateOf(listOf<BrowserTab>()) }
    var currentTabIndex by remember { mutableStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTabManager by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return
    val geckoRuntime = remember { GeckoRuntime.create(context) }
    geckoRuntime.settings.setExtensionsWebAPIEnabled(true)
    geckoRuntime.settings.setExtensionsProcessEnabled(true)
    geckoRuntime.settings.setAboutConfigEnabled(true)
    geckoRuntime.settings.setRemoteDebuggingEnabled(true)
    geckoRuntime.webExtensionController.enableExtensionProcessSpawning()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val swipeRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            val initialSession = GeckoSession().apply { open(geckoRuntime) }
            tabs = listOf(BrowserTab(geckoSession = initialSession))
        }
    }

    val currentTab = if (tabs.isNotEmpty() && currentTabIndex < tabs.size) tabs[currentTabIndex] else null
    val currentSession = currentTab?.geckoSession

    LaunchedEffect(currentSession) {
        currentSession?.let { session ->
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
                            isRefreshing = true,
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
            onSettingsClick = { },
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
                onNewTab = {
                    val newSession = GeckoSession().apply { open(geckoRuntime) }
                    tabs = tabs + BrowserTab(geckoSession = newSession)
                    currentTabIndex = tabs.size - 1
                    showBottomSheet = false
                }
            )
        }
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
            .background(MaterialTheme.colorScheme.background)
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
                AnimatedTabCard(
                    tab = tab,
                    isSelected = pageIndex == currentTabIndex,
                    onTabClick = { onTabSelected(pageIndex) },
                    onCloseClick = { onTabClosed(pageIndex) },
                    isCurrentPage = pageIndex == pagerState.currentPage ||
                            (pageIndex == pagerState.currentPage - 1 && pagerState.currentPageOffsetFraction > 0) ||
                            (pageIndex == pagerState.currentPage + 1 && pagerState.currentPageOffsetFraction < 0)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedTabCard(
    tab: BrowserTab,
    isSelected: Boolean,
    onTabClick: () -> Unit,
    onCloseClick: () -> Unit,
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tab.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    AnimatedIconButton(
                        onClick = onCloseClick,
                        icon = Icons.Default.Close,
                        contentDescription = "Close Tab"
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    if (isCurrentPage) {
                        tab.geckoSession?.let { session ->
                            AndroidView(
                                factory = { context ->
                                    GeckoView(context).apply {
                                        setSession(session)
                                        isClickable = false
                                        isFocusable = false
                                        isActivated = false
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
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = "Website",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                     androidx.compose.animation.AnimatedVisibility(
                        visible = tab.isLoading,
                        enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                        exit = scaleOut(animationSpec = tween(200)) + fadeOut()
                    ) {
                        ContainedLoadingIndicator(
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(animationSpec = tween(200)) + fadeIn(),
                exit = scaleOut(animationSpec = tween(200)) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(6.dp)
                        )
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 12.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val haptic = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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

@Composable
fun NewTabBottomSheet(
    currentUrl: String,
    onNavigate: (String) -> Unit,
    onNewTab: () -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        TextField(
            value = urlText,
            onValueChange = { urlText = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            placeholder = { Text("Search or enter URL") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                AnimatedIconButton(
                    onClick = { onNavigate(urlText) },
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onNavigate(urlText) })
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimatedQuickLink(
                icon = Icons.Default.Home,
                label = "Home",
                onClick = { onNavigate("https://www.google.com") }
            )

            AnimatedQuickLink(
                icon = Icons.Default.Favorite,
                label = "Favorites",
                onClick = { }
            )

            AnimatedQuickLink(
                icon = Icons.Default.History,
                label = "History",
                onClick = { }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AnimatedQuickLink(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val doubleClickThreshold = 500L // (ms) btw
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = onTabsClick) {
                    Icon(Icons.Default.Tab, contentDescription = "Tabs")
                }
                if (tabCount > 1) {
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = 8.dp, y = (-8).dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
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
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                if (!isSearch) Icon(Icons.Default.Add, contentDescription = "New Tab")
                if (isSearch) Icon(Icons.Default.Search, contentDescription = "Search")
            }

            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}
