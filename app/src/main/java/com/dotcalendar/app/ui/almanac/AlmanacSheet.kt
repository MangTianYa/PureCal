package com.dotcalendar.app.ui.almanac

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotcalendar.app.almanac.Almanac
import com.dotcalendar.app.almanac.AlmanacPosition
import com.dotcalendar.app.ui.components.DotPill
import com.dotcalendar.app.ui.components.OutlinePill
import com.dotcalendar.app.ui.theme.JiRedContainerDark
import com.dotcalendar.app.ui.theme.JiRedContainerLight
import com.dotcalendar.app.ui.theme.JiRedDark
import com.dotcalendar.app.ui.theme.JiRedLight
import com.dotcalendar.app.ui.theme.YiGreenContainerDark
import com.dotcalendar.app.ui.theme.YiGreenContainerLight
import com.dotcalendar.app.ui.theme.YiGreenDark
import com.dotcalendar.app.ui.theme.YiGreenLight

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlmanacSheet(
    almanac: Almanac,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colors.outlineVariant)
        },
    ) {
        AlmanacContent(
            almanac = almanac,
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlmanacContent(
    almanac: Almanac,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val yiContainer = if (isDark) YiGreenContainerDark else YiGreenContainerLight
    val yiContent = if (isDark) YiGreenDark else YiGreenLight
    val jiContainer = if (isDark) JiRedContainerDark else JiRedContainerLight
    val jiContent = if (isDark) JiRedDark else JiRedLight

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 40.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "今日黄历",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = colors.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = almanac.monthDay,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${almanac.solarText} · ${almanac.weekday}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = "${almanac.ganZhiYear} ${almanac.ganZhiMonth} ${almanac.ganZhiDay} · ${almanac.zodiac}年 · ${almanac.constellation}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
            )

            val tags = buildList {
                if (almanac.jieQi.isNotBlank()) add("节气 · ${almanac.jieQi}")
                addAll(almanac.festivals)
            }.distinct()

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag -> OutlinePill(text = tag) }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        item {
            SectionPillHeader(title = "宜", badgeContainer = yiContainer, badgeContent = yiContent)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = almanac.yi.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))

            SectionPillHeader(title = "忌", badgeContainer = jiContainer, badgeContent = jiContent)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = almanac.ji.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text(
                text = "日课",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            val info = listOf(
                "值神" to "${almanac.tianShen} · ${almanac.tianShenType} · ${almanac.tianShenLuck}",
                "建除" to almanac.zhiXing,
                "星宿" to "${almanac.xiu} · ${almanac.xiuLuck}",
                "六曜" to almanac.liuYao,
                "月相" to almanac.yueXiang,
                "纳音" to almanac.naYin,
                "冲煞" to "冲${almanac.chong} · 煞${almanac.sha}",
                "胎神" to almanac.taiShen,
                "日禄" to almanac.dayLu,
                "物候" to almanac.wuHou,
            )

            info.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { (label, value) ->
                        InfoCell(
                            label = label,
                            value = value,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FullInfoRow(label = "彭祖百忌", value = almanac.pengZu)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text(
                text = "吉神宜趋",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = almanac.jiShen.ifEmpty { listOf("无") }.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "凶煞宜忌",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = almanac.xiongSha.ifEmpty { listOf("无") }.joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Text(
                text = "神位方位",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            almanac.positions.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { position ->
                        PositionCell(
                            position = position,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "黄历内容属中国传统民俗文化，仅供参考。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SectionPillHeader(
    title: String,
    badgeContainer: Color,
    badgeContent: Color,
) {
    DotPill(
        text = title,
        containerColor = badgeContainer,
        contentColor = badgeContent,
    )
}

@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 6.dp, horizontal = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FullInfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PositionCell(
    position: AlmanacPosition,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        Text(
            text = position.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = position.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
