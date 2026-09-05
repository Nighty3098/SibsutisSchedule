package app.vercel.Nighty3098.schedule.presentation.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vercel.Nighty3098.schedule.R
import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.presentation.schedule.components.DaySelector
import app.vercel.Nighty3098.schedule.presentation.schedule.components.LessonCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    themeMode: ThemeMode,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    // Свайп дней: ±год от сегодня, стартовая страница — сегодня.
    val dates = viewModel.pagerDates
    val pagerState = rememberPagerState(
        initialPage = viewModel.initialPageIndex,
        pageCount = { dates.size },
    )
    // Свайп -> выбранная дата.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.selectDate(dates[page])
        }
    }
    // Календарь/кнопки -> свайп к дате (свою же страницу пропускаем).
    LaunchedEffect(state.selectedDate) {
        val target = dates.indexOf(state.selectedDate)
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.groupQuery.isBlank()) {
                                "Расписание"
                            } else {
                                "Группа ${state.groupQuery}"
                            },
                        )
                        Text(
                            text = formatSubtitle(state.selectedDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !state.isRefreshing,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                // Резервируем те же 4.dp, чтобы контент не прыгал
                // при появлении/исчезновении индикатора.
                Spacer(modifier = Modifier.height(4.dp))
            }
            DaySelector(
                selectedDate = state.selectedDate,
                onSelect = viewModel::selectDate,
            )

            if (state.groupQuery.isBlank()) {
                EmptyHint(
                    text = "Укажи номер группы в Настройках\n(например, 3414)",
                    actionLabel = "Открыть настройки",
                    onAction = onOpenSettings,
                )
                return@Column
            }

            // Вертикальный список внутри горизонтального пейджера:
            // свайп влево/вправо листает дни, вверх/вниз — пары.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val date = dates[page]
                // remember(date): тот же инстанс потока между рекомпозициями,
                // иначе подписка перезапускалась бы и страница мигала.
                val dayFlow = remember(date) { viewModel.dayFlow(date) }
                val day by dayFlow.collectAsState()
                DayPage(
                    date = date,
                    day = day,
                    isRefreshing = state.isRefreshing,
                    themeMode = themeMode,
                    onToday = viewModel::today,
                )
            }
        }
    }
}

/** Одна страница дня: маскот в пустой день либо таймлайн пар. */
@Composable
private fun DayPage(
    date: LocalDate,
    day: DaySchedule?,
    isRefreshing: Boolean,
    themeMode: ThemeMode,
    onToday: () -> Unit,
) {
    val lessons = day?.lessons.orEmpty()
    Box(modifier = Modifier.fillMaxSize()) {
        if (lessons.isEmpty() && !isRefreshing) {
            // Маскот — только когда пар нет.
            Image(
                painter = painterResource(R.drawable.girl),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.52f),
            )
            EmptyHint(
                text = "Пар нет 🎉\nОтдыхай!",
                actionLabel = "Сегодня",
                onAction = onToday,
            )
            return@Box
        }
        if (lessons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        val (currentIdx, nextIdx) = currentAndNextIndexes(lessons, date)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(lessons, key = { it.id }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    isCurrent = lessons.indexOf(lesson) == currentIdx,
                    isNext = lessons.indexOf(lesson) == nextIdx,
                    themeMode = themeMode,
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EmptyHint(text: String, actionLabel: String, onAction: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun formatSubtitle(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru")))
        .replaceFirstChar { it.uppercaseChar() }

/**
 * Индексы текущей и следующей пары (только если выбран сегодня).
 * Текущая: start <= now < end. Следующая: первая с end > now.
 */
private fun currentAndNextIndexes(
    lessons: List<Lesson>,
    date: LocalDate,
    now: LocalTime = LocalTime.now(),
): Pair<Int, Int> {
    if (date != LocalDate.now() || lessons.isEmpty()) return -1 to -1
    var current = -1
    var next = -1
    lessons.forEachIndexed { i, l ->
        val s = l.startTime ?: return@forEachIndexed
        val e = l.endTime ?: return@forEachIndexed
        if (!now.isBefore(s) && now.isBefore(e) && current == -1) current = i
    }
    next = lessons.indexOfFirst { (it.endTime ?: return@indexOfFirst false).isAfter(now) }
        .let { if (it == current) -1 else it }
    // Если есть текущая, следующей считаем ближайшую после неё.
    if (current != -1 && next != -1 && next < current) next = -1
    return current to next
}
