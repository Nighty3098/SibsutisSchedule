package app.vercel.Nighty3098.schedule.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.vercel.Nighty3098.schedule.data.calendar.CalendarSyncManager
import app.vercel.Nighty3098.schedule.data.calendar.DeviceCalendar
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import app.vercel.Nighty3098.schedule.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val groupQuery: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val login: String = "",
    val saved: Boolean = false,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val calendarSync: CalendarSyncManager,
) : ViewModel() {

    val groupQuery: StateFlow<String> = settings.groupQuery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val login: StateFlow<String> = settings.login
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val calendarSyncEnabled: StateFlow<Boolean> = settings.calendarSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val calendarId: StateFlow<Long?> = settings.calendarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val calendarName: StateFlow<String> = settings.calendarName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun saveGroup(value: String) {
        viewModelScope.launch { settings.setGroupQuery(value) }
    }

    fun saveTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun saveCredentials(login: String, password: String) {
        viewModelScope.launch { settings.setCredentials(login, password) }
    }

    // ---------- Календарь ----------

    fun hasCalendarPermission(): Boolean = calendarSync.hasPermission()

    suspend fun loadDeviceCalendars(): List<DeviceCalendar> =
        calendarSync.availableCalendars()

    fun setCalendarSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setCalendarSync(enabled) }
    }

    fun setCalendarAccount(id: Long?, name: String) {
        viewModelScope.launch { settings.setCalendarAccount(id, name) }
    }

    /** Полная пересинхронизация (вызывать при наличии разрешений). */
    suspend fun syncCalendarNow(): Result<Int> = calendarSync.syncNow()

    /** Удалить наши события из календаря (при выключении/смене календаря). */
    suspend fun clearCalendarEvents() {
        calendarSync.clearAppEvents()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val settings: SettingsRepository,
        private val calendarSync: CalendarSyncManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settings, calendarSync) as T
    }
}
