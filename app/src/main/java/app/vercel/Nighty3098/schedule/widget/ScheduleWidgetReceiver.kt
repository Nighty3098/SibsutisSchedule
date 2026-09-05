package app.vercel.Nighty3098.schedule.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Receiver виджета. Объявлен в AndroidManifest + xml/schedule_widget_info.xml. */
class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ScheduleWidget()
}
