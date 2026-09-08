package app.vercel.Nighty3098.schedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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

/** Кривые Material emphasized motion для навигационных переходов. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = appContainer()
        setContent {
            val themeMode by container.settings.themeMode.collectAsState(
                initial = app.vercel.Nighty3098.schedule.domain.model.ThemeMode.SYSTEM,
            )
            val scope = rememberCoroutineScope()
            // Разрешение на уведомления (Android 13+): нужно фоновому
            // воркеру для «Расписание изменилось». Без него — молча
            // пропускаем, приложение работает как раньше.
            val context = LocalContext.current
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            // Перерисовка виджетов после обновления данных.
            // За темой следит контейнер (observeThemeForWidgets).
            val updateWidgets = {
                scope.launch { ScheduleWidget().updateAll(applicationContext) }
            }
            ScheduleTheme(themeMode = themeMode) {
                val nav = rememberNavController()
                // Переходы как системные экраны новых Android: новый экран
                // въезжает с края на всю ширину, старый остаётся и гаснет
                // (параллакс), фейд нового идёт с задержкой 90мс —
                // фирменный стаггер Material motion. На navigation 2.8+ эти
                // же транзишены анимируют predictive back-жест.
                NavHost(
                    navController = nav,
                    startDestination = "schedule",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = EmphasizedDecelerate),
                        ) + fadeIn(tween(210, delayMillis = 90))
                    },
                    exitTransition = {
                        fadeOut(tween(350, easing = EmphasizedAccelerate))
                    },
                    popEnterTransition = {
                        // Без задержки фейда и с быстрым проявлением: экран
                        // под уходящими настройками должен стать непрозрачным
                        // как можно раньше, иначе мигает белый фон окна.
                        slideInHorizontally(
                            initialOffsetX = { -it / 5 },
                            animationSpec = tween(350, easing = EmphasizedDecelerate),
                        ) + fadeIn(tween(200))
                    },
                    popExitTransition = {
                        // Без затухания: уходящие настройки остаются
                        // непрозрачными весь слайд — окну нечего просветить.
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = EmphasizedAccelerate),
                        )
                    },
                ) {
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
