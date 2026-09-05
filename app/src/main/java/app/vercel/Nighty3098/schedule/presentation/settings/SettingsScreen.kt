package app.vercel.Nighty3098.schedule.presentation.settings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.vercel.Nighty3098.schedule.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val savedGroup by viewModel.groupQuery.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val savedLogin by viewModel.login.collectAsState()

    var groupDraft by remember(savedGroup) { mutableStateOf(savedGroup) }
    var loginDraft by remember(savedLogin) { mutableStateOf(savedLogin) }
    var passwordDraft by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
