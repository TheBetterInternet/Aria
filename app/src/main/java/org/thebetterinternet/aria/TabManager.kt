package org.thebetterinternet.aria

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.mozilla.geckoview.GeckoRuntime
import kotlin.math.absoluteValue

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
            pageSize = PageSize.Fixed(350.dp),
            reverseLayout = true,
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { pageIndex ->
            val tab = tabs[pageIndex]
            val pageOffset =
                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
            val scale = lerp(0.85f, 0.8f, 0.8f - pageOffset.absoluteValue.coerceAtMost(0.6f))
            val alpha = lerp(0.6f, 1f, 1f - pageOffset.absoluteValue.coerceAtMost(1f))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 64.dp)
                    .graphicsLayer {
                        this.alpha = alpha
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