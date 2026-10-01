package io.github.kuscher.booklight.entry

import android.app.PendingIntent
import android.service.quicksettings.TileService

/** A Quick Settings tile that opens the panel. It has no state of its own. */
class PanelTile : TileService() {
    override fun onClick() {
        startActivityAndCollapse(PendingIntent.getActivity(this, 1, SearchWidget.panel(this), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
    }
}
