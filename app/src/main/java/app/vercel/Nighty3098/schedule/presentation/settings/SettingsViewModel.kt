package app.vercel.Nighty3098.schedule.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    val groupQuery: StateFlow<String> = settings.groupQuery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val login: StateFlow<String> = settings.login
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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val settings: SettingsRepository,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settings) as T
    }
}
