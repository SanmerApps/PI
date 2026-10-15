package dev.sanmer.pi.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.ui.component.Dot
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    viewModel: HomeViewModel
) {
    val pagerState = rememberPagerState { 2 }

    Scaffold(
        modifier = Modifier.imePadding(),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = CircleShape
                        )
                        .padding(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) {
                        Dot(
                            modifier = Modifier.size(8.dp),
                            color = with(MaterialTheme.colorScheme) {
                                if (pagerState.currentPage == it) onSecondaryContainer else secondaryContainer
                            }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        HorizontalPager(
            state = pagerState
        ) {
            when (it) {
                0 -> HomeContent(
                    viewModel = viewModel,
                    contentPadding = contentPadding
                )

                1 -> PackagesContent(
                    viewModel = koinViewModel(),
                    contentPadding = contentPadding
                )
            }
        }
    }
}