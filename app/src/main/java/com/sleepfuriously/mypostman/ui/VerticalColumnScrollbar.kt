package com.sleepfuriously.mypostman.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a scrollbar for a Column.  The scrollbar adjusts based
 * on scrollState.value and the total content height.
 *
 * This scrollbar does not respond to touches, but at least it shows you where
 * you are in the scroll.  I also suspect that it's not that good in huge long
 * lists either.
 *
 * @param   scrollState     scrollState.value
 * @param   width           Adjust the scrollbar width
 *
 * @param   showScrollBarTrack      Toggle visibility (true = visible)
 * @param   scrollBarCornerRadius   Adjust the sharpness of the bar ends
 * @param   endPadding      Define padding between scollbar and edge of content
 *
 * found:
 *      https://medium.com/@mittalkshitij20/adding-a-custom-scrollbar-to-a-column-in-jetpack-compose-9996c26f498f
 */
@Composable
fun Modifier.verticalColumnScrollbar(
    scrollState: ScrollState,
    width: Dp = 4.dp,
    showScrollBarTrack: Boolean = true,
    scrollBarTrackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    scrollBarColor: Color = MaterialTheme.colorScheme.secondary,
    scrollBarCornerRadius: Float = 4f,
    endPadding: Float = 12f
): Modifier {
    return drawWithContent {
        // Draw the column's content
        drawContent()
        // Dimensions and calculations
        val viewportHeight = this.size.height
        val totalContentHeight = scrollState.maxValue.toFloat() + viewportHeight
        val scrollValue = scrollState.value.toFloat()
        // Compute scrollbar height and position
        val scrollBarHeight =
            (viewportHeight / totalContentHeight) * viewportHeight
        val scrollBarStartOffset =
            (scrollValue / totalContentHeight) * viewportHeight
        // Draw the track (optional)
        if (showScrollBarTrack) {
            drawRoundRect(
                cornerRadius = CornerRadius(scrollBarCornerRadius),
                color = scrollBarTrackColor,
                topLeft = Offset(this.size.width - endPadding, 0f),
                size = Size(width.toPx(), viewportHeight),
            )
        }
        // Draw the scrollbar
        drawRoundRect(
            cornerRadius = CornerRadius(scrollBarCornerRadius),
            color = scrollBarColor,
            topLeft = Offset(this.size.width - endPadding, scrollBarStartOffset),
            size = Size(width.toPx(), scrollBarHeight)
        )
    }
}
