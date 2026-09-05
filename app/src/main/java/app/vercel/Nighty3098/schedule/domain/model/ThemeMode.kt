package app.vercel.Nighty3098.schedule.domain.model

/** Режим темы приложения. */
enum class ThemeMode(val title: String) {
    /** Material You: системные dynamicColorScheme. */
    SYSTEM("Material You"),

    /** AMOLED: чёрный фон #000000, контрастные акценты, без dynamic colors. */
    AMOLED("AMOLED"),

    /** Монохром: чёрный фон #000000, только оттенки серого, без цветных акцентов. */
    MONOCHROME("Монохром"),

    /** Solarized Osaka (тёмный): бирюзово-чёрный фон, приглушённые акценты. */
    SOLARIZED_OSAKA("Solarized Osaka"),

    /** Gruvbox (тёмный): тёплая ретро-палитра. */
    GRUVBOX("Gruvbox"),

    /** Tokyo Night: ночной синий неон. */
    TOKYO_NIGHT("Tokyo Night"),

    /** Catppuccin Mocha: пастель на тёмном. */
    CATPPUCCIN("Catppuccin Mocha");

    companion object {
        fun fromNameOrDefault(name: String?): ThemeMode =
            try {
                if (name == null) SYSTEM else valueOf(name)
            } catch (_: IllegalArgumentException) {
                SYSTEM
            }
    }
}
