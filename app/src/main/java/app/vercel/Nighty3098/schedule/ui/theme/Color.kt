package app.vercel.Nighty3098.schedule.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ---------- AMOLED-палитра: чистый чёрный + контрастные акценты ----------

val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF000000)
val AmoledSurfaceVariant = Color(0xFF121212)
val AmoledSurfaceContainer = Color(0xFF1A1A1A)
val AmoledOnBackground = Color(0xFFFFFFFF)
val AmoledOnSurface = Color(0xFFFFFFFF)
val AmoledOnSurfaceVariant = Color(0xFFCAC4D0)

/** Контрастный акцент для AMOLED (электрический фиолетово-голубой). */
val AmoledPrimary = Color(0xFFBB86FC)
val AmoledOnPrimary = Color(0xFF000000)
val AmoledPrimaryContainer = Color(0xFF3700B3)
val AmoledOnPrimaryContainer = Color(0xFFFFFFFF)
val AmoledSecondary = Color(0xFF03DAC6)
val AmoledTertiary = Color(0xFFCF6679)

// ---------- Монохромная палитра: чёрный фон + только оттенки серого ----------

val MonoBackground = Color(0xFF000000)
val MonoSurface = Color(0xFF000000)
val MonoSurfaceVariant = Color(0xFF121212)
val MonoSurfaceContainer = Color(0xFF1A1A1A)
val MonoSurfaceContainerHigh = Color(0xFF242424)
val MonoOnBackground = Color(0xFFFFFFFF)
val MonoOnSurface = Color(0xFFFFFFFF)
val MonoOnSurfaceVariant = Color(0xFFB0B0B0)
val MonoOutline = Color(0xFF3A3A3A)

/** Основной акцент монохрома — почти белый. */
val MonoPrimary = Color(0xFFE8E8E8)
val MonoOnPrimary = Color(0xFF000000)
val MonoPrimaryContainer = Color(0xFF2A2A2A)
val MonoOnPrimaryContainer = Color(0xFFFFFFFF)
val MonoSecondary = Color(0xFFA0A0A0)
val MonoTertiary = Color(0xFF7A7A7A)

/** Оттенки серого для типов занятий в монохроме (вместо синий/зелёный/оранжевый). */
val MonoLecture = Color(0xFFF5F5F5)
val MonoPractice = Color(0xFF9E9E9E)
val MonoLab = Color(0xFF616161)
val MonoOther = Color(0xFF424242)

// ---------- Solarized Osaka (тёмный; hex посчитаны из HSL-палитры) ----------

val SolBackground = Color(0xFF00141A) // base04
val SolSurface = Color(0xFF00141A)
val SolSurfaceVariant = Color(0xFF073541) // base02
val SolSurfaceContainer = Color(0xFF002D38) // base03
val SolSurfaceContainerHigh = Color(0xFF073541)
val SolOnBackground = Color(0xFF9FABAD) // base0
val SolOnSurface = Color(0xFF9FABAD)
val SolOnSurfaceVariant = Color(0xFF839495) // fg
val SolOutline = Color(0xFF073541) // border = base02
val SolPrimary = Color(0xFF278BD3) // blue
val SolOnPrimary = Color(0xFF00141A)
val SolPrimaryContainer = Color(0xFF073541)
val SolOnPrimaryContainer = Color(0xFF278BD3)
val SolSecondary = Color(0xFF2AA298) // cyan
val SolTertiary = Color(0xFF6D72C5) // violet
val SolLecture = Color(0xFF278BD3)
val SolPractice = Color(0xFF859900) // green
val SolLab = Color(0xFFFFBF00) // yellow, светлый для тёмного фона
val SolOther = Color(0xFF586E74) // base01

// ---------- Gruvbox (тёмный) ----------

val GrBackground = Color(0xFF282828) // dark0
val GrSurface = Color(0xFF282828)
val GrSurfaceVariant = Color(0xFF3C3836) // dark1
val GrSurfaceContainer = Color(0xFF32302F) // dark0_soft
val GrSurfaceContainerHigh = Color(0xFF504945) // dark2
val GrOnBackground = Color(0xFFEBDBB2) // light1
val GrOnSurface = Color(0xFFEBDBB2)
val GrOnSurfaceVariant = Color(0xFFBDAE93) // light3
val GrOutline = Color(0xFF665C54) // dark3
val GrPrimary = Color(0xFFFE8019) // bright_orange
val GrOnPrimary = Color(0xFF282828)
val GrPrimaryContainer = Color(0xFF3C3836)
val GrOnPrimaryContainer = Color(0xFFFE8019)
val GrSecondary = Color(0xFF8EC07C) // bright_aqua
val GrTertiary = Color(0xFFD3869B) // bright_purple
val GrLecture = Color(0xFF83A598) // bright_blue
val GrPractice = Color(0xFFB8BB26) // bright_green
val GrLab = Color(0xFFFE8019) // bright_orange
val GrOther = Color(0xFF928374) // gray

// ---------- Tokyo Night ----------

val TnBackground = Color(0xFF1A1B26)
val TnSurface = Color(0xFF1A1B26)
val TnSurfaceVariant = Color(0xFF24283B) // storm
val TnSurfaceContainer = Color(0xFF1F2335)
val TnSurfaceContainerHigh = Color(0xFF292E42)
val TnOnBackground = Color(0xFFC0CAF5)
val TnOnSurface = Color(0xFFC0CAF5)
val TnOnSurfaceVariant = Color(0xFFA9B1D6) // fg_dark
val TnOutline = Color(0xFF3B4261)
val TnHighest = Color(0xFF3B4157) // High +12% к fg: Card красится в Highest
val TnPrimary = Color(0xFF7AA2F7) // blue
val TnOnPrimary = Color(0xFF1A1B26)
val TnPrimaryContainer = Color(0xFF24283B)
val TnOnPrimaryContainer = Color(0xFF7AA2F7)
val TnSecondary = Color(0xFFBB9AF7) // magenta
val TnTertiary = Color(0xFF7DCFFF) // cyan
val TnLecture = Color(0xFF7AA2F7)
val TnPractice = Color(0xFF9ECE6A) // green
val TnLab = Color(0xFFFF9E64) // orange
val TnOther = Color(0xFF565F89) // comment

// ---------- Catppuccin Mocha ----------

val CpBackground = Color(0xFF1E1E2E) // base
val CpSurface = Color(0xFF1E1E2E)
val CpSurfaceVariant = Color(0xFF313244) // surface0
val CpSurfaceContainer = Color(0xFF181825) // mantle
val CpSurfaceContainerHigh = Color(0xFF313244)
val CpOnBackground = Color(0xFFCDD6F4) // text
val CpOnSurface = Color(0xFFCDD6F4)
val CpOnSurfaceVariant = Color(0xFFA6ADC8) // subtext0
val CpOutline = Color(0xFF45475A) // surface1
val CpHighest = Color(0xFF444659) // High +12% к fg: Card красится в Highest
val CpPrimary = Color(0xFFCBA6F7) // mauve
val CpOnPrimary = Color(0xFF11111B) // crust
val CpPrimaryContainer = Color(0xFF313244)
val CpOnPrimaryContainer = Color(0xFFCBA6F7)
val CpSecondary = Color(0xFFF5C2E7) // pink
val CpTertiary = Color(0xFF89DCEB) // sky
val CpLecture = Color(0xFF89B4FA) // blue
val CpPractice = Color(0xFFA6E3A1) // green
val CpLab = Color(0xFFFAB387) // peach
val CpOther = Color(0xFF9399B2) // overlay2

// ---------- Дополнительные роли M3 ----------
// Без них компоненты (снекбар, трек прогресса, верхние контейнеры)
// падают в базовые серые значения darkColorScheme по умолчанию.
val AmoledBright = Color(0xFF2A2A2A)
val AmoledHighest = Color(0xFF1F1F1F) // чуть светлее High: обычный Card красится в Highest
val AmoledInverseSurface = Color(0xFFE8E8E8)
val AmoledInverseOnSurface = Color(0xFF000000)
val AmoledInversePrimary = Color(0xFF000000)
val AmoledOutlineVariant = Color(0xFF333333)

val MonoBright = Color(0xFF2E2E2E)
val MonoHighest = Color(0xFF1F1F1F) // чуть светлее High: обычный Card красится в Highest
val MonoInverseSurface = Color(0xFFE8E8E8)
val MonoInverseOnSurface = Color(0xFF000000)
val MonoInversePrimary = Color(0xFF000000)
val MonoOutlineVariant = Color(0xFF242424)

val SolHighest = Color(0xFF19434E) // High +12% к fg: Card красится в Highest

val GrBright = Color(0xFFA89984) // light4
val GrHighest = Color(0xFF635B52) // High +12% к fg: Card красится в Highest
val GrOutlineVariant = Color(0xFF504945) // dark2

val CpBright = Color(0xFF9399B2) // overlay2
val CpDim = Color(0xFF11111B) // crust

// ---------- Цвета типов занятий (бейдж/полоска LessonCard) ----------

val LectureBlue = Color(0xFF2196F3)
val PracticeGreen = Color(0xFF4CAF50)
val LabOrange = Color(0xFFFF9800)
val OtherGrey = Color(0xFF9E9E9E)
