package app.vercel.Nighty3098.schedule.data.repository

import android.util.Log
import app.vercel.Nighty3098.schedule.data.datastore.SettingsDataStore
import app.vercel.Nighty3098.schedule.data.local.LessonEntity
import app.vercel.Nighty3098.schedule.data.local.ScheduleDao
import app.vercel.Nighty3098.schedule.data.local.ScheduleMetaEntity
import app.vercel.Nighty3098.schedule.data.network.AuthManager
import app.vercel.Nighty3098.schedule.data.network.GroupResolver
import app.vercel.Nighty3098.schedule.data.network.ScheduleFetcher
import app.vercel.Nighty3098.schedule.data.parser.ScheduleParser
import app.vercel.Nighty3098.schedule.domain.model.DaySchedule
import app.vercel.Nighty3098.schedule.domain.model.Lesson
import app.vercel.Nighty3098.schedule.domain.model.LessonSnapshot
import app.vercel.Nighty3098.schedule.domain.model.LessonType
import app.vercel.Nighty3098.schedule.domain.model.RefreshOutcome
import app.vercel.Nighty3098.schedule.domain.model.WeekParity
import app.vercel.Nighty3098.schedule.domain.model.diffLessonSnapshots
import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Репозиторий: сеть → Room → UI, с офлайн-фолбэком.
 *
 * refresh():
 * 1. resolve группы (цифры = готовый ID, иначе AJAX-поиск),
 * 2. GET HTML (при AuthRequired — login по сохранённым credentials + 1 retry),
 * 3. parse days[1..14] через Jsoup,
 * 4. replace кэша группы в Room.
 *
 * observeDay(): читает только Room (работает без интернета) и проецирует
 * двухнедельный цикл на конкретную дату через [WeekParity].
 */
class ScheduleRepositoryImpl(
    private val dao: ScheduleDao,
    private val fetcher: ScheduleFetcher,
    private val resolver: GroupResolver,
    private val auth: AuthManager,
    private val settings: SettingsDataStore,
) : ScheduleRepository {

    override fun observeDay(date: LocalDate, groupQuery: String): Flow<DaySchedule> {
        val week = WeekParity.of(date)
        val day = WeekParity.dayIndex(date)
        return dao.observeDay(groupQuery.trim(), week, day).map { entities ->
            DaySchedule(
                date = date,
                weekIndex = week,
                lessons = entities.map { it.toDomain() },
            )
        }
    }

    override suspend fun refresh(groupQuery: String, force: Boolean): Result<RefreshOutcome> =
        withContext(Dispatchers.IO) {
            val group = groupQuery.trim()
            if (group.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Пустой номер группы"))
            }
            runCatching {
                // TTL-кэш: свежие данные отдаём из Room без единого запроса.
                val updatedAt = dao.getMeta(group)?.updatedAtMillis
                if (!force && ScheduleRepository.isCacheFresh(updatedAt)) {
                    val ageMin = (System.currentTimeMillis() - (updatedAt ?: 0)) / 60_000
                    Log.d(TAG, "Кэш группы $group свежий (возраст $ageMin мин) — сеть пропускаем")
                    return@runCatching RefreshOutcome(dao.countLessons(group))
                }
                if (force) {
                    Log.d(TAG, "Принудительное обновление группы $group — идём в сеть")
                }
                val match = resolver.resolveId(group)
                val html = try {
                    fetcher.fetchHtml(match.id)
                } catch (e: ScheduleFetcher.AuthRequiredException) {
                    // Пробуем войти и повторить ровно один раз.
                    val login = settings.getLoginOnce()
                    val password = settings.getPassword()
                        ?: throw Exception("Сайт требует вход. Укажи логин и пароль в Настройках.")
                    if (login.isBlank()) {
                        throw Exception("Сайт требует вход. Укажи логин и пароль в Настройках.")
                    }
                    val ok = try {
                        auth.login(login, password)
                    } catch (ae: Exception) {
                        throw Exception("Не удалось войти: ${ae.message}")
                    }
                    if (!ok) throw Exception("Неверный логин или пароль.")
                    fetcher.fetchHtml(match.id)
                }
                val parsed = ScheduleParser.parse(group, html)
                // Снимок старого кэша ДО замены — для диффа изменений.
                val oldSnapshots = dao.getGroupLessons(group).map { it.toSnapshot() }
                val newSnapshots = parsed.lessons.map { it.toSnapshot() }
                val changes = diffLessonSnapshots(oldSnapshots, newSnapshots)
                if (changes.isNotEmpty()) {
                    Log.d(TAG, "Обновление группы $group: изменений ${changes.size}")
                }
                val entities = parsed.lessons.map { it.toEntity(group) }
                dao.replaceGroup(group, entities)
                dao.upsertMeta(
                    ScheduleMetaEntity(
                        groupQuery = group,
                        title = parsed.title.ifEmpty { match.text },
                        updatedAtMillis = System.currentTimeMillis(),
                        lessonCount = entities.size,
                    ),
                )
                RefreshOutcome(entities.size, changes)
            }
        }

    override suspend fun lastUpdated(groupQuery: String): Long? =
        withContext(Dispatchers.IO) {
            dao.getMeta(groupQuery.trim())?.updatedAtMillis
        }

    override suspend fun getDayLessons(date: LocalDate, groupQuery: String): List<Lesson> =
        withContext(Dispatchers.IO) {
            val week = WeekParity.of(date)
            val day = WeekParity.dayIndex(date)
            dao.getDay(groupQuery.trim(), week, day).map { it.toDomain() }
        }

    // ---------- mapping ----------

    private fun LessonEntity.toDomain(): Lesson = Lesson(
        id = id,
        groupQuery = groupQuery,
        weekIndex = weekIndex,
        dayIndex = dayIndex,
        number = number,
        timeFrom = timeFrom,
        timeTo = timeTo,
        subject = subject,
        typeRaw = typeRaw,
        type = LessonType.fromRaw(typeRaw),
        teachers = teachers.split('\n').map { it.trim() }.filter { it.isNotEmpty() },
        room = room,
        subgroup = subgroup,
        groups = groups.split('\n').map { it.trim() }.filter { it.isNotEmpty() },
    )

    private fun LessonEntity.toSnapshot(): LessonSnapshot = LessonSnapshot(
        weekIndex = weekIndex,
        dayIndex = dayIndex,
        number = number,
        timeFrom = timeFrom,
        timeTo = timeTo,
        subject = subject,
        typeRaw = typeRaw,
        teachers = teachers.split('\n').map { it.trim() }.filter { it.isNotEmpty() },
        room = room,
        subgroup = subgroup,
    )

    private fun ScheduleParser.ParsedLesson.toSnapshot(): LessonSnapshot = LessonSnapshot(
        weekIndex = weekIndex,
        dayIndex = dayIndex,
        number = number,
        timeFrom = timeFrom,
        timeTo = timeTo,
        subject = subject,
        typeRaw = typeRaw,
        teachers = teachers,
        room = room,
        subgroup = subgroup,
    )

    private fun ScheduleParser.ParsedLesson.toEntity(group: String): LessonEntity =
        LessonEntity(
            groupQuery = group,
            weekIndex = weekIndex,
            dayIndex = dayIndex,
            number = number,
            timeFrom = timeFrom,
            timeTo = timeTo,
            subject = subject,
            typeRaw = typeRaw,
            teachers = teachers.joinToString("\n"),
            room = room,
            subgroup = subgroup,
            groups = groups.joinToString("\n"),
        )

    companion object {
        private const val TAG = "ScheduleRepo"
    }
}
