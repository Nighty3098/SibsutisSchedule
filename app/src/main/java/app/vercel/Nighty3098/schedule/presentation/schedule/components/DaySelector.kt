package app.vercel.Nighty3098.schedule.presentation.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Горизонтальный скролл дней для выбора даты (стиль Google Calendar).
 * Окно: -7 … +21 день от сегодня. Сегодня помечен точкой.
 */
@Composable
fun DaySelector(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    state: LazyListState = rememberLazyListState(),
) {
    val chips = rememberDayChips(today)

    // Держим выбранную дату в видимой зоне: и при первом показе,
    // и при смене даты извне (кнопка «Сегодня», свайп виджета и т.п.).
    LaunchedEffect(selectedDate) {
        val idx = chips.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 }
            ?: chips.indexOfFirst { it.date == today }.takeIf { it >= 0 } ?: 0
        state.animateScrollToItem((idx - 2).coerceAtLeast(0))
    }

    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(chips, key = { it.date.toEpochDay() }) { chip ->
            val selected = chip.date == selectedDate
            // Плавная смена заливки выбранного дня вместо резкого скачка.
            val bg by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                animationSpec = tween(220),
                label = "dayChipBg",
            )
            val content by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                animationSpec = tween(220),
                label = "dayChipFg",
            )
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onSelect(chip.date) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bg,
                    contentColor = content,
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (selected) 4.dp else 0.dp,
                ),
            ) {
                Column(
                    // fillMaxWidth обязателен: иначе колонка ужмётся под текст
                    // и CenterHorizontally/textAlign.Center не дадут эффекта.
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = chip.weekday,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = chip.dayOfMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = chip.leading,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Подпись дня с заранее готовыми строками — не пересобирается на рекомпозицию. */
private data class DayChip(
    val date: LocalDate,
    val weekday: String,
    val dayOfMonth: String,
    /** «●» для сегодня, иначе краткий месяц. */
    val leading: String,
)

@Composable
private fun rememberDayChips(today: LocalDate): List<DayChip> {
    val locale = Locale.forLanguageTag("ru")
    return remember(today) {
        (-7..21).map { off ->
            val d = today.plusDays(off.toLong())
            DayChip(
                date = d,
                weekday = d.dayOfWeek
                    .getDisplayName(TextStyle.SHORT, locale)
                    .replaceFirstChar { it.uppercaseChar() },
                dayOfMonth = d.dayOfMonth.toString(),
                leading = if (d == today) {
                    "●"
                } else {
                    d.month.getDisplayName(TextStyle.SHORT, locale)
                },
            )
        }
    }
}
