package app.vercel.Nighty3098.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.vercel.Nighty3098.schedule.presentation.schedule.ScheduleScreen
import app.vercel.Nighty3098.schedule.presentation.schedule.ScheduleViewModel
import app.vercel.Nighty3098.schedule.presentation.settings.SettingsScreen
import app.vercel.Nighty3098.schedule.presentation.settings.SettingsViewModel
import app.vercel.Nighty3098.schedule.ui.theme.ScheduleTheme
import app.vercel.Nighty3098.schedule.widget.ScheduleWidget
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = appContainer()
        setContent {
            val themeMode by container.settings.themeMode.collectAsState(
                initial = app.vercel.Nighty3098.schedule.domain.model.ThemeMode.SYSTEM,
            )
            val scope = rememberCoroutineScope()
            // Перерисовка виджетов после обновления данных.
            // За темой следит контейнер (observeThemeForWidgets).
            val updateWidgets = {
                scope.launch { ScheduleWidget().updateAll(applicationContext) }
            }
            ScheduleTheme(themeMode = themeMode) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "schedule") {
                    composable("schedule") {
                        val vm: ScheduleViewModel = viewModel(
                            factory = ScheduleViewModel.Factory(
                                container.scheduleRepository,
                                container.settings,
                                // После успешного обновления данных виджет
                                // показывает уже новый кэш, а не вчерашний.
                                onLessonsChanged = {
                                    updateWidgets()
                                    // Календарь пересинхронизируется,
                                    // только если включён в настройках.
                                    scope.launch {
                                        runCatching {
                                            container.calendarSync.syncIfEnabled()
                                        }
                                    }
                                },
                            ),
                        )
                        ScheduleScreen(
                            viewModel = vm,
                            themeMode = themeMode,
                            onOpenSettings = { nav.navigate("settings") },
                        )
                    }
                    composable("settings") {
                        val vm: SettingsViewModel = viewModel(
                            factory = SettingsViewModel.Factory(
                                container.settings,
                                container.calendarSync,
                            ),
                        )
                        SettingsScreen(viewModel = vm, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
