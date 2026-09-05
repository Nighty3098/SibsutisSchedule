package app.vercel.Nighty3098.schedule.presentation.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.vercel.Nighty3098.schedule.data.calendar.DeviceCalendar
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val savedGroup by viewModel.groupQuery.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val savedLogin by viewModel.login.collectAsState()
    val calendarEnabled by viewModel.calendarSyncEnabled.collectAsState()
    val calendarId by viewModel.calendarId.collectAsState()
    val calendarName by viewModel.calendarName.collectAsState()

    var groupDraft by remember(savedGroup) { mutableStateOf(savedGroup) }
    var loginDraft by remember(savedLogin) { mutableStateOf(savedLogin) }
    var passwordDraft by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ---- Календарь: локальное состояние ----
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var deviceCalendars by remember { mutableStateOf(emptyList<DeviceCalendar>()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }
    var syncBusy by remember { mutableStateOf(false) }

    /** Полный цикл включения: флаг → календари → календарь по умолчанию → запись. */
    fun enableCalendarFlow() {
        scope.launch {
            syncBusy = true
            syncMessage = ""
            viewModel.setCalendarSyncEnabled(true)
            val cals = viewModel.loadDeviceCalendars()
            deviceCalendars = cals
            if (cals.isNotEmpty() && calendarId == null) {
                val pick = cals.first()
                viewModel.setCalendarAccount(pick.id, pick.name)
            }
            viewModel.syncCalendarNow()
                .onSuccess { syncMessage = "Записано событий: $it" }
                .onFailure { syncMessage = "Ошибка синхронизации: ${it.message}" }
            syncBusy = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        android.util.Log.d("CalendarSync", "permission result: $grants")
        if (grants.values.all { it }) {
            permissionDenied = false
            enableCalendarFlow()
        } else {
            permissionDenied = true
        }
    }

    fun onCalendarToggle(on: Boolean) {
        android.util.Log.d("CalendarSync", "toggle tap: on=$on")
        if (!on) {
            scope.launch {
                viewModel.setCalendarSyncEnabled(false)
                viewModel.clearCalendarEvents()
                syncMessage = "Синхронизация выключена, события удалены"
            }
            return
        }
        permissionDenied = false
        if (viewModel.hasCalendarPermission()) {
            enableCalendarFlow()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
    }

    // Подгружаем список календарей, когда синхронизация включена
    // (например, после перезапуска экрана).
    LaunchedEffect(calendarEnabled) {
        if (calendarEnabled && viewModel.hasCalendarPermission()) {
            deviceCalendars = viewModel.loadDeviceCalendars()
        }
    }

    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Edge-to-edge: клавиатура приходит инсетом, а не ресайзом —
                // без imePadding() скролл уходил бы под клавиатуру.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Группа ----
            Card(colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Группа",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupDraft,
                        onValueChange = { groupDraft = it },
                        label = { Text("Группа") },
                        placeholder = { Text("3414") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.saveGroup(groupDraft)
                                focusManager.clearFocus()
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveGroup(groupDraft)
                            focusManager.clearFocus()
                        },
                        enabled = groupDraft.trim().isNotEmpty() &&
                            groupDraft.trim() != savedGroup.trim(),
                    ) {
                        Text("Сохранить группу")
                    }
                }
            }

            // ---- Тема: выпадающий селектор (тем уже 7, радиокнопки не масштабируются) ----
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Тема", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    ThemeDropdown(
                        current = theme,
                        onSelect = { viewModel.saveTheme(it) },
                    )
                }
            }

            // ---- Календарь: запись пар в календарь устройства (Google — через системную синхронизацию) ----
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Календарь",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Пары на 4 недели вперёд",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = calendarEnabled,
                            onCheckedChange = ::onCalendarToggle,
                            enabled = !syncBusy,
                        )
                    }
                    if (permissionDenied && !calendarEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Без доступа к календарю запись пар невозможна. " +
                                "Разреши доступ в настройках системы.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                ctx.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    ).apply {
                                        data = Uri.fromParts("package", ctx.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            },
                        ) {
                            Text("Открыть настройки")
                        }
                    }
                    if (calendarEnabled) {
                        Spacer(Modifier.height(8.dp))
                        if (deviceCalendars.isEmpty()) {
                            Text(
                                "Календари не найдены — проверь разрешения и аккаунты.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            CalendarDropdown(
                                calendars = deviceCalendars,
                                selectedId = calendarId,
                                selectedName = calendarName,
                                onSelect = { cal ->
                                    scope.launch {
                                        syncBusy = true
                                        // Сначала чистим СТАРЫЙ календарь, потом пишем в новый.
                                        viewModel.clearCalendarEvents()
                                        viewModel.setCalendarAccount(cal.id, cal.name)
                                        viewModel.syncCalendarNow()
                                            .onSuccess {
                                                syncMessage = "Записано событий: $it"
                                            }
                                            .onFailure {
                                                syncMessage = "Ошибка: ${it.message}"
                                            }
                                        syncBusy = false
                                    }
                                },
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncBusy = true
                                        viewModel.syncCalendarNow()
                                            .onSuccess {
                                                syncMessage = "Записано событий: $it"
                                            }
                                            .onFailure {
                                                syncMessage = "Ошибка: ${it.message}"
                                            }
                                        syncBusy = false
                                    }
                                },
                                enabled = !syncBusy,
                            ) {
                                Text("Синхронизировать сейчас")
                            }
                        }
                        if (syncMessage.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                syncMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---- Вход (обязателен: сайт отдаёт расписание только после авторизации) ----
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Вход в СибГУТИ", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = loginDraft,
                        onValueChange = { loginDraft = it },
                        label = { Text("Логин") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocus.requestFocus() },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordDraft,
                        onValueChange = { passwordDraft = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Скрыть пароль"
                                    } else {
                                        "Показать пароль"
                                    },
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.saveCredentials(loginDraft, passwordDraft)
                                passwordDraft = ""
                                focusManager.clearFocus()
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocus),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.saveCredentials(loginDraft, passwordDraft)
                            passwordDraft = ""
                            focusManager.clearFocus()
                        },
                        enabled = loginDraft.isNotBlank() && passwordDraft.isNotEmpty(),
                    ) {
                        Text("Сохранить вход")
                    }
                }
            }
        }
    }
}

/** Выпадающий селектор календаря устройства для записи пар. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDropdown(
    calendars: List<DeviceCalendar>,
    selectedId: Long?,
    selectedName: String,
    onSelect: (DeviceCalendar) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = calendars.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.name ?: selectedName.ifEmpty { "Выбери календарь" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Календарь") },
            supportingText = selected?.let { sel -> { Text(sel.account) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            calendars.forEach { cal ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(cal.name)
                            Text(
                                cal.account,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onSelect(cal)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
/** Выпадающий селектор темы с названием и описанием каждого варианта. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDropdown(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = current.title,
            onValueChange = {},
            readOnly = true,
            label = { Text("Оформление") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.title) },
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
