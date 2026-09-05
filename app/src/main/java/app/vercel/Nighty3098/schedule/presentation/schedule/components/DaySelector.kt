package app.vercel.Nighty3098.schedule.presentation.schedule.components

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
    val locale = Locale.forLanguageTag("ru")
    val dates = rememberDates(today)

    // Держим выбранную дату в видимой зоне: и при первом показе,
    // и при смене даты извне (кнопка «Сегодня», свайп виджета и т.п.).
    LaunchedEffect(selectedDate) {
        val idx = dates.indexOf(selectedDate).takeIf { it >= 0 }
            ?: dates.indexOf(today).takeIf { it >= 0 } ?: 0
        state.animateScrollToItem((idx - 2).coerceAtLeast(0))
    }

    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(dates, key = { it.toEpochDay() }) { date ->
            val selected = date == selectedDate
            val isToday = date == today
            Card(
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onSelect(date) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
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
                        text = date.dayOfWeek
                            .getDisplayName(TextStyle.SHORT, locale)
                            .replaceFirstChar { it.uppercaseChar() },
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = if (isToday) "●" else date.month
                            .getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDates(today: LocalDate): List<LocalDate> {
    return ( -7..21 ).map { today.plusDays(it.toLong()) }
}
