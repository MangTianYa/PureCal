package com.dotcalendar.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotcalendar.app.ui.almanac.AlmanacSheet
import com.dotcalendar.app.ui.components.BrandRow
import com.dotcalendar.app.util.chineseMonths
import com.dotcalendar.app.util.monthTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.absoluteValue

private const val START_YEAR = 2020
private const val YEAR_COUNT = 21
private const val TOTAL_MONTHS = 12 * YEAR_COUNT

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val almanac by viewModel.almanac.collectAsStateWithLifecycle()
    val selectedHoliday by viewModel.selectedHoliday.collectAsStateWithLifecycle()
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val holidayDataVersion by viewModel.holidayDataVersion.collectAsStateWithLifecycle()

    val today by produceState(initialValue = LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(60_000)
        }
    }

    val scope = rememberCoroutineScope()
    val initialPage = remember { visibleMonth.toPageIndex().coerceIn(0, TOTAL_MONTHS - 1) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { TOTAL_MONTHS },
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val month = page.toYearMonth()
            viewModel.onMonthChanged(month)

            // 仅在翻页结束后迁移选中日期，避免滑动过程中摘要卡反复跳变。
            val selected = viewModel.selectedDate.value
            if (YearMonth.from(selected) != month) {
                val day = selected.dayOfMonth.coerceAtMost(month.lengthOfMonth())
                viewModel.selectDate(month.atDay(day))
            }
        }
    }

    var showAlmanac by rememberSaveable { mutableStateOf(false) }
    val pageMonth = pagerState.currentPage.toYearMonth()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
    ) {
        BrandRow(trailing = "日历")

        MonthHero(
            month = pageMonth,
            zodiacProvider = { viewModel.zodiacOf(it) },
            canGoPrevious = pagerState.currentPage > 0,
            canGoNext = pagerState.currentPage < TOTAL_MONTHS - 1,
            onPreviousClick = {
                if (pagerState.currentPage > 0) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage - 1,
                            animationSpec = tween(
                                durationMillis = 320,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
            },
            onNextClick = {
                if (pagerState.currentPage < TOTAL_MONTHS - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = tween(
                                durationMillis = 320,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
            },
            onTodayClick = {
                scope.launch {
                    val targetPage = YearMonth.from(today).toPageIndex().coerceIn(0, TOTAL_MONTHS - 1)
                    pagerState.animateScrollToPage(
                        page = targetPage,
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                    viewModel.selectDate(today)
                }
            },
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
            pageSpacing = 0.dp,
        ) { page ->
            val month = remember(page) { page.toYearMonth() }
            val days = remember(
                month,
                selectedDate,
                today,
                settings.showLunar,
                settings.weekStartsMonday,
                holidayDataVersion,
            ) {
                viewModel.calendarDays(
                    month = month,
                    selectedDate = selectedDate,
                    today = today,
                    showLunar = settings.showLunar,
                    weekStartsMonday = settings.weekStartsMonday,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 在 Draw 阶段延迟读取动画偏移量，彻底避免滚动过程中的高频 Composition 重组
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val pageAlpha = (1f - pageOffset * 0.45f).coerceIn(0.5f, 1f)
                        val pageScale = (1f - pageOffset * 0.04f).coerceIn(0.94f, 1f)
                        alpha = pageAlpha
                        scaleX = pageScale
                        scaleY = pageScale
                    },
            ) {
                MonthGrid(
                    days = days,
                    weekStartsMonday = settings.weekStartsMonday,
                    onDateClick = { date ->
                        viewModel.selectDate(date)
                        val targetMonth = YearMonth.from(date)
                        if (targetMonth != month) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = targetMonth.toPageIndex().coerceIn(0, TOTAL_MONTHS - 1),
                                    animationSpec = tween(
                                        durationMillis = 320,
                                        easing = FastOutSlowInEasing,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }

        DaySummaryCard(
            date = selectedDate,
            almanac = almanac,
            holiday = selectedHoliday,
            onClick = { showAlmanac = true },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp),
        )
    }

    if (showAlmanac) {
        AlmanacSheet(
            almanac = almanac,
            onDismiss = { showAlmanac = false },
        )
    }
}

@Composable
internal fun MonthHero(
    month: YearMonth,
    zodiac: String = "",
    zodiacProvider: ((YearMonth) -> String)? = null,
    canGoPrevious: Boolean = true,
    canGoNext: Boolean = true,
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onTodayClick: () -> Unit = {},
) {
    val resolvedZodiacProvider: (YearMonth) -> String = zodiacProvider ?: { zodiac }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                val isForward = targetState > initialState
                if (isForward) {
                    (slideInVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> height / 2 },
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 240),
                    )).togetherWith(
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> -height / 2 },
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180),
                        ),
                    )
                } else {
                    (slideInVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> -height / 2 },
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 240),
                    )).togetherWith(
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> height / 2 },
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180),
                        ),
                    )
                }.using(SizeTransform(clip = true))
            },
            label = "monthTitleAnimation",
            modifier = Modifier.weight(1f),
        ) { currentMonth ->
            Column {
                Text(
                    text = monthTitle(currentMonth.year, currentMonth.monthValue),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${chineseMonths[currentMonth.monthValue - 1]} · ${resolvedZodiacProvider(currentMonth)}年",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onPreviousClick,
                enabled = canGoPrevious,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "上一月",
                    tint = if (canGoPrevious) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                )
            }

            Surface(
                modifier = Modifier.clickable(onClick = onTodayClick),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "今天",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            IconButton(
                onClick = onNextClick,
                enabled = canGoNext,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "下一月",
                    tint = if (canGoNext) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
                )
            }
        }
    }
}

private fun YearMonth.toPageIndex(): Int =
    ((year - START_YEAR) * 12 + (monthValue - 1)).coerceIn(0, TOTAL_MONTHS - 1)

private fun Int.toYearMonth(): YearMonth =
    YearMonth.of(START_YEAR + this / 12, this % 12 + 1)
