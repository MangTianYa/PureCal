package com.dotcalendar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * 用点阵网格绘制图标，呼应 Nothing OS 的 dot-matrix 语言。
 * pattern 中 '#' 为一个圆点，其余字符为空白。
 */
@Composable
fun DotMatrixIcon(
    pattern: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = LocalContentColor.current,
    dotScale: Float = 0.30f,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.size(size).then(semanticsModifier)) {
        val rows = pattern.size
        val columns = pattern.maxOfOrNull { it.length } ?: return@Canvas
        val cell = min(this.size.width / columns, this.size.height / rows)
        val offsetX = (this.size.width - cell * columns) / 2f
        val offsetY = (this.size.height - cell * rows) / 2f
        val radius = cell * dotScale

        pattern.forEachIndexed { y, row ->
            row.forEachIndexed { x, char ->
                if (char == '#') {
                    drawCircle(
                        color = tint,
                        radius = radius,
                        center = Offset(
                            x = offsetX + cell * (x + 0.5f),
                            y = offsetY + cell * (y + 0.5f),
                        ),
                    )
                }
            }
        }
    }
}

object DotIcons {
    val Calendar = listOf(
        "..#.#..",
        "..###..",
        "#######",
        "#.....#",
        "#.###.#",
        "#.....#",
        "#######",
    )

    val Spark = listOf(
        "#..#..#",
        ".#.#.#.",
        "..###..",
        "#######",
        "..###..",
        ".#.#.#.",
        "#..#..#",
    )

    val Gear = listOf(
        "..#.#..",
        ".#####.",
        "##...##",
        "#.....#",
        "##...##",
        ".#####.",
        "..#.#..",
    )

    val Today = listOf(
        "..###..",
        ".#...#.",
        ".#.#.#.",
        "#..#..#",
        ".#.#.#.",
        ".#...#.",
        "..###..",
    )
}
