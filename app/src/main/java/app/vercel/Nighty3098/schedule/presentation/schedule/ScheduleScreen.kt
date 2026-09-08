package app.vercel.Nighty3098.schedule.presentation.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vercel.Nighty3098.schedule.R
import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.ScheduleChange
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.domain.model.WeekParity
import app.vercel.Nighty3098.schedule.domain.model.describe
import app.vercel.Nighty3098.schedule.ui.theme.accentFor
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

    // Диалог со списком изменений после ручного обновления.
    val lastChanges = state.lastChanges
    if (lastChanges != null) {
        ChangesDialog(
            changes = lastChanges,
            onDismiss = viewModel::dismissChanges,
        )
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
                            text = formatSubtitle(state.selectedDate, state.viewMode),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    // Переключатель «день / неделя».
                    IconButton(
                        onClick = {
                            viewModel.setViewMode(
                                if (state.viewMode == ScheduleViewMode.DAY) {
                                    ScheduleViewMode.WEEK
                                } else {
                                    ScheduleViewMode.DAY
                                },
                            )
                        },
                    ) {
                        Icon(
                            imageVector = if (state.viewMode == ScheduleViewMode.DAY) {
                                Icons.Filled.CalendarViewWeek
                            } else {
                                Icons.Filled.CalendarViewDay
                            },
                            contentDescription = if (state.viewMode == ScheduleViewMode.DAY) {
                                "Неделя"
                            } else {
                                "День"
                            },
                        )
                    }
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
        // FAB «Сегодня»: виден, только когда ушли от текущей даты.
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.selectedDate != LocalDate.now(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.today() },
                    icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                    text = { Text("Сегодня") },
                )
            }
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

            if (state.groupQuery.isBlank()) {
                EmptyHint(
                    text = "Укажи номер группы в Настройках\n(например, 3414)",
                    actionLabel = "Открыть настройки",
                    onAction = onOpenSettings,
                )
                return@Column
            }

            // Свайп вниз обновляет расписание (дублирует кнопку в тулбаре).
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                Crossfade(
                    targetState = state.viewMode,
                    modifier = Modifier.fillMaxSize(),
                    label = "viewMode",
                ) { mode ->
                    when (mode) {
                        ScheduleViewMode.DAY -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                DaySelector(
                                    selectedDate = state.selectedDate,
                                    onSelect = viewModel::selectDate,
                                )
                                // Вертикальный список внутри горизонтального
                                // пейджера: свайп влево/вправо листает дни,
                                // вверх/вниз — пары.
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1,
                                ) { page ->
                                    val date = dates[page]
                                    // remember(date): тот же инстанс потока
                                    // между рекомпозициями, иначе подписка
                                    // перезапускалась бы и страница мигала.
                                    val dayFlow = remember(date) { viewModel.dayFlow(date) }
                                    val day by dayFlow.collectAsState()
                                    DayPage(
                                        date = date,
                                        day = day,
                                        isRefreshing = state.isRefreshing,
                                        themeMode = themeMode,
                                        // Время для подсветки актуально только
                                        // для «сегодня»; остальным дням
                                        // отдаём null — им тиканье не нужно.
                                        now = if (date == LocalDate.now()) now else null,
                                        onToday = onToday,
                                    )
                                }
                            }
                        }
                        ScheduleViewMode.WEEK -> {
                            WeekView(
                                viewModel = viewModel,
                                selectedDate = state.selectedDate,
                                themeMode = themeMode,
                                now = now,
                                onDayClick = { date ->
                                    viewModel.selectDate(date)
                                    viewModel.setViewMode(ScheduleViewMode.DAY)
                                },
                            )
                        }
                    }
                }
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

/**
 * Недельный вид (Пн–Сб): каждый день — карточка в стиле [LessonCard]
 * (скругление 16.dp, полоска акцента, бейдж «Сегодня»).
 * Данные берутся из того же L1-кэша [ScheduleViewModel.dayFlow],
 * новых подписок сверх дневного режима не создаётся.
 * Тап по дню возвращает в дневной режим на выбранной дате
 * (пейджер сам доскроллится через LaunchedEffect).
 */
@Composable
private fun WeekView(
    viewModel: ScheduleViewModel,
    selectedDate: LocalDate,
    themeMode: ThemeMode,
    now: LocalTime?,
    onDayClick: (LocalDate) -> Unit,
) {
    val weekDates = remember(selectedDate) { viewModel.weekDates(selectedDate) }
    val monday = weekDates.first()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Неделя • ${WeekParity.evenOddName(monday).lowercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        // Один lazy-item на день: подписка и рекомпозиция изолированы
        // внутри секции, весь список не перерисовывается.
        weekDates.forEach { date ->
            item(key = date.toEpochDay()) {
                WeekDaySection(
                    viewModel = viewModel,
                    date = date,
                    isToday = date == LocalDate.now(),
                    isSelected = date == selectedDate,
                    themeMode = themeMode,
                    // Тиканье часов нужно только сегодняшней секции —
                    // остальные получают null и пропускают рекомпозицию.
                    now = if (date == LocalDate.now()) now else null,
                    onDayClick = onDayClick,
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/** Карточка одного дня недели: шапка + компактные строки пар. */
@Composable
private fun WeekDaySection(
    viewModel: ScheduleViewModel,
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    themeMode: ThemeMode,
    now: LocalTime?,
    onDayClick: (LocalDate) -> Unit,
) {
    val dayFlow = remember(date) { viewModel.dayFlow(date) }
    val day by dayFlow.collectAsState()
    val lessons = day?.lessons.orEmpty()
    // Подсветка текущей/следующей — как в дневном режиме.
    val ids = remember(lessons, now) { currentAndNextIds(lessons, date, now) }

    Card(
        onClick = { onDayClick(date) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Шапка дня: дата, счётчик пар, бейдж «Сегодня».
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSubtitle(date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (lessons.isNotEmpty()) {
                    Text(
                        text = pairsCount(lessons.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                if (isToday) {
                    TodayPill()
                }
            }

            if (lessons.isEmpty()) {
                Text(
                    text = "Пар нет 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Spacer(Modifier.height(4.dp))
                lessons.forEachIndexed { index, lesson ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 21.dp),
                        )
                    }
                    WeekLessonRow(
                        lesson = lesson,
                        isCurrent = lesson.id == ids.first,
                        isNext = lesson.id == ids.second,
                        themeMode = themeMode,
                        onClick = { onDayClick(date) },
                    )
                }
            }
        }
    }
}

/**
 * Компактная строка пары: полоска типа, время, предмет и детали —
 * уменьшенная версия [LessonCard] с той же подсветкой.
 */
@Composable
private fun WeekLessonRow(
    lesson: Lesson,
    isCurrent: Boolean,
    isNext: Boolean,
    themeMode: ThemeMode,
    onClick: () -> Unit,
) {
    val accent = lesson.type.accentFor(themeMode)
    val highlighted = isCurrent || isNext
    val time = listOf(lesson.timeFrom, lesson.timeTo)
        .filter { it.isNotBlank() }
        .joinToString("–")
        .ifEmpty { "—" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (highlighted) accent.copy(alpha = 0.10f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) {
                accent
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(90.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lesson.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                lesson.room.takeIf { it.isNotBlank() }?.let { "ауд. $it" },
                lesson.subgroup.takeIf { it.isNotBlank() },
                lesson.teachers.firstOrNull(),
            ).joinToString(" • ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                Text(
                    text = "● Сейчас идёт",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            } else if (isNext) {
                Text(
                    text = "Следующая",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

/** Пилюля «Сегодня» в шапке карточки дня — как выбранный чип DaySelector. */
@Composable
private fun TodayPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Сегодня",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private fun pairsCount(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "$n пара"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "$n пары"
    else -> "$n пар"
}

/** Диалог со списком изменений после ручного обновления. */
@Composable
private fun ChangesDialog(
    changes: List<ScheduleChange>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (changes.size == 1) {
                    "Расписание изменилось"
                } else {
                    "Расписание изменилось: ${changes.size}"
                },
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(changes, key = { it.describe() }) { change ->
                    Text(
                        text = change.describe(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        },
    )
}

/** Период опроса времени для подсветки текущей пары. */
private const val HIGHLIGHT_TICK_MS = 20_000L

/** Форматтер локален не создаётся на каждую рекомпозицию (дорогая операция). */
private val SubtitleFormatter =
    DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))

private val MonthFormatter =
    DateTimeFormatter.ofPattern("LLLL", Locale.forLanguageTag("ru"))

private fun formatSubtitle(date: LocalDate): String =
    date.format(SubtitleFormatter)
        .replaceFirstChar { it.uppercaseChar() }

/**
 * Подзаголовок тулбара: в дневном режиме — дата + чётность недели,
 * в недельном — диапазон Пн–Сб + чётность.
 */
private fun formatSubtitle(date: LocalDate, mode: ScheduleViewMode): String =
    when (mode) {
        ScheduleViewMode.DAY ->
            "${formatSubtitle(date)} • ${WeekParity.evenOddName(date)} неделя"
        ScheduleViewMode.WEEK -> formatWeekRange(weekMondayOf(date))
    }

private fun formatWeekRange(monday: LocalDate): String {
    val saturday = monday.plusDays(5)
    val range = if (monday.month == saturday.month) {
        "${monday.dayOfMonth}–${saturday.dayOfMonth} ${saturday.format(MonthFormatter)}"
    } else {
        "${monday.dayOfMonth} ${monday.format(MonthFormatter)} – " +
            "${saturday.dayOfMonth} ${saturday.format(MonthFormatter)}"
    }
    return "$range • ${WeekParity.evenOddName(monday)} неделя"
}

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
