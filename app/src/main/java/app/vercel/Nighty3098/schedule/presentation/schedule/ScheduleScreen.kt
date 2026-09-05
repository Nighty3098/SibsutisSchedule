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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

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

    // Живая подсветка «текущая/следующая пара»: раз в 20 с перечитываем
    // время, чтобы бейдж сам переключался на стыке пар без ручного
    // обновления. Идёт только для страницы «сегодня».
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(HIGHLIGHT_TICK_MS)
            now = LocalTime.now()
        }
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
    // Один стабильный колбэк для всех страниц: не мешает пропуску
    // рекомпозиции DayPage при тиканье часов.
    val onToday = remember { { viewModel.today() } }

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
                    // Время для подсветки актуально только для «сегодня»;
                    // остальным дням отдаём null — им тиканье не нужно.
                    now = if (date == LocalDate.now()) now else null,
                    onToday = onToday,
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
    now: LocalTime?,
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

        // Индексы текущей/следующей пары считаем ОДИН раз на эмиссию данных
        // и по id, а не lessons.indexOf() внутри каждого item (это было O(n²)
        // и пересчитывалось на каждую рекомпозицию).
        val ids = remember(lessons, now) { currentAndNextIds(lessons, date, now) }
        val currentId = ids.first
        val nextId = ids.second
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(lessons, key = { it.id }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    isCurrent = lesson.id == currentId,
                    isNext = lesson.id == nextId,
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

/** Период опроса времени для подсветки текущей пары. */
private const val HIGHLIGHT_TICK_MS = 20_000L

/** Форматтер локален не создаётся на каждую рекомпозицию (дорогая операция). */
private val SubtitleFormatter =
    DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))

private fun formatSubtitle(date: LocalDate): String =
    date.format(SubtitleFormatter)
        .replaceFirstChar { it.uppercaseChar() }

/**
 * id текущей и следующей пары (только если выбран сегодня).
 * Текущая: start <= now < end. Следующая: первая с end > now.
 * Возвращаем id (у каждого урока уникальный), а не индекс, чтобы
 * сравнение в items шло за O(1).
 */
private fun currentAndNextIds(
    lessons: List<Lesson>,
    date: LocalDate,
    now: LocalTime?,
): Pair<Long?, Long?> {
    if (date != LocalDate.now() || lessons.isEmpty()) return null to null
    val t = now ?: LocalTime.now()
    var current = -1
    var next = -1
    lessons.forEachIndexed { i, l ->
        val s = l.startTime ?: return@forEachIndexed
        val e = l.endTime ?: return@forEachIndexed
        if (!t.isBefore(s) && t.isBefore(e) && current == -1) current = i
    }
    next = lessons.indexOfFirst { (it.endTime ?: return@indexOfFirst false).isAfter(t) }
        .let { if (it == current) -1 else it }
    // Если есть текущая, следующей считаем ближайшую после неё.
    if (current != -1 && next != -1 && next < current) next = -1
    return lessons.getOrNull(current)?.id to lessons.getOrNull(next)?.id
}
