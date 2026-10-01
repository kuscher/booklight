package io.github.kuscher.booklight.entry

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.overlay.OverlayActivity

/** A search pill for the home screen or the desktop: a click opens the panel. It shows nothing that changes. */
class SearchWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val open = PendingIntent.getActivity(context, 0, panel(context), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val views = RemoteViews(context.packageName, R.layout.widget_search).apply { setOnClickPendingIntent(R.id.widget_search, open) }
        for (id in ids) manager.updateAppWidget(id, views)
    }

    companion object {
        /** Opens the panel, marked as asked for by name, so it is the panel however it was started. */
        fun panel(context: Context): Intent = Intent(context, OverlayActivity::class.java).setAction(OverlayActivity.ACTION_PANEL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
