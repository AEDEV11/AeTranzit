package com.aedev.aetranzit.ui.onboarding

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aedev.aetranzit.ui.components.AeBrandCompactBar
import com.aedev.aetranzit.ui.components.AePrimaryButton
import com.aedev.aetranzit.ui.theme.AeDimens
import com.aedev.aetranzit.ui.theme.AeTranzitTheme
import com.aedev.aetranzit.ui.theme.StudioStageBottomDark
import com.aedev.aetranzit.ui.theme.StudioStageBottomLight
import com.aedev.aetranzit.ui.theme.StudioStageTopDark
import com.aedev.aetranzit.ui.theme.StudioStageTopLight
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onOnboardingCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    OnboardingContent(
        onSkip = { viewModel.completeOnboarding(onOnboardingCompleted) },
        onGetStarted = { viewModel.completeOnboarding(onOnboardingCompleted) }
    )
}

@Composable
fun OnboardingContent(
    onSkip: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_SLIDES.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_SLIDES.size - 1

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AeDimens.SpaceLg, vertical = AeDimens.SpaceSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AeBrandCompactBar()
                if (!isLastPage) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.sizeIn(
                            minWidth = AeDimens.MinTouchTarget,
                            minHeight = AeDimens.MinTouchTarget
                        )
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(AeDimens.MinTouchTarget))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AeDimens.SpaceLg, vertical = AeDimens.SpaceMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val slide = ONBOARDING_SLIDES[page]
                OnboardingSlideItem(slide = slide)
            }

            Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = AeDimens.SpaceSm)
            ) {
                repeat(ONBOARDING_SLIDES.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) {
                            AeDimens.PagerDotActiveWidth
                        } else {
                            AeDimens.PagerDotSize
                        },
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = AeDimens.SpaceXs)
                            .height(AeDimens.PagerDotSize)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AeDimens.SpaceMd))

            AePrimaryButton(
                text = if (isLastPage) "Get Started" else "Next",
                onClick = {
                    if (isLastPage) {
                        onGetStarted()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(AeDimens.SpaceSm))
        }
    }
}

@Composable
private fun OnboardingSlideItem(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier
) {
    val isDarkSurface = MaterialTheme.colorScheme.background.luminance() < 0.3f || isSystemInDarkTheme()
    val stageBrush = Brush.verticalGradient(
        colors = if (isDarkSurface) {
            listOf(StudioStageTopDark, StudioStageBottomDark)
        } else {
            listOf(StudioStageTopLight, StudioStageBottomLight)
        }
    )

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = AeDimens.SpaceLg),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = AeDimens.ElevatedCardElevation
                ),
                border = BorderStroke(
                    AeDimens.BorderWidth,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.aspectRatio(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(stageBrush)
                        .padding(AeDimens.SpaceLg),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = slide.imageRes),
                        contentDescription = slide.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = AeDimens.SpaceSm)
        )

        Text(
            text = slide.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AeDimens.SpaceSm)
        )
    }
}

@Preview(showBackground = true, name = "Onboarding - Light")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Onboarding - Dark"
)
@Composable
private fun OnboardingContentPreview() {
    AeTranzitTheme {
        OnboardingContent(
            onSkip = {},
            onGetStarted = {}
        )
    }
}
