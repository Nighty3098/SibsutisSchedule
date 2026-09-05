package app.vercel.Nighty3098.schedule

import app.vercel.Nighty3098.schedule.domain.repository.ScheduleRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Границы 3-часового TTL кэша расписания. */
class CacheTtlTest {

    private val now = 1_700_000_000_000L
    private val hour = 60 * 60 * 1000L

    @Test
    fun freshCacheSkipsNetwork() {
        assertTrue(ScheduleRepository.isCacheFresh(now - hour, now)) // 1 ч назад
        assertTrue(ScheduleRepository.isCacheFresh(now - 2 * hour, now)) // 2 ч назад
    }

    @Test
    fun staleCacheGoesToNetwork() {
        assertFalse(ScheduleRepository.isCacheFresh(now - 4 * hour, now)) // 4 ч назад
        // Ровно 3 часа — уже протух (строгое <).
        assertFalse(ScheduleRepository.isCacheFresh(now - 3 * hour, now))
    }

    @Test
    fun noMetaMeansStale() {
        assertFalse(ScheduleRepository.isCacheFresh(null, now))
        assertFalse(ScheduleRepository.isCacheFresh(0, now))
    }
}
