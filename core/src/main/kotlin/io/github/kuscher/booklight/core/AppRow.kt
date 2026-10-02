package io.github.kuscher.booklight.core

/**
 * An app's row, one structure for every app: Open · Search · Play · Window · the arrow.
 *
 * An app shows only what it has, the order never changes, and nothing is dimmed to hold a place:
 * a calculator is Open · Window · arrow, an app that can be searched has Search second, a music
 * app that plays what is named has Play after it. Window and the arrow are stops that open a list
 * under the row: where the app's window goes (New window, then the places), and the rest (App
 * info, the app's pages in Settings, Don't suggest, Uninstall). Two lists, never one inside the other.
 *
 * This only orders and marks what the app list made: which apps can be searched or play is the
 * device's to say.
 */
object AppRow {
    const val OPEN = "open"
    /** The stop that opens the lines about the app's window. (`window` is the id New window has had since 1.0.) */
    const val WINDOW = "places"
    const val NEW_WINDOW = "window"
    const val INFO = "info"
    const val UNINSTALL = "uninstall"

    /** What stands on the row as an icon, in the row's order. */
    val ICONS = listOf(OPEN, Act.SEARCH.id, Act.PLAY.id, WINDOW)
    /** The places an app's window can be asked to open in, in the order they are listed. */
    val PLACES = listOf("full", "left", "right", "p3l", "p3m", "p3r", "p23l", "p23r", "ptl", "ptr", "pbl", "pbr", "pc")
    /** The lines behind Window. */
    val WINDOW_LINES = listOf(NEW_WINDOW) + PLACES
    /** The app's own pages in Settings. */
    val PAGES = listOf("notifications", "language", "defaults", "battery")
    /** The lines behind the arrow. ("Don't suggest" is put in by [Zero.offer], before what removes.) */
    val ARROW_LINES = listOf(INFO) + PAGES + UNINSTALL

    private val order = ICONS + WINDOW_LINES + ARROW_LINES

    /**
     * Where the action [id] stands on an app's row: null for an icon on the row, else the list it is
     * a line of. An id the row does not know is a line behind the arrow.
     */
    fun behind(id: String): Behind? = when (id) {
        in ICONS -> null
        in WINDOW_LINES -> Behind.WINDOW
        else -> Behind.ARROW
    }

    /**
     * [actions] as an app's row has them: in the row's order, each marked with where it stands.
     * What the app does not have is simply not among them (no Search, no Play, no New window for an
     * app of another profile, no Uninstall for one that came with the device). Window with no line
     * behind it is left out. An action the row does not know is a line behind the arrow, after the
     * ones it knows; what removes something is last whatever it is.
     */
    fun arrange(actions: List<Action>): List<Action> {
        val known = actions.filter { it.id in order }.sortedBy { order.indexOf(it.id) }
        val lines = known.any { behind(it.id) == Behind.WINDOW }
        val all = known.filter { it.id != WINDOW || lines } + actions.filter { it.id !in order }
        return (all.filter { !it.danger } + all.filter { it.danger }).map { a ->
            val b = behind(a.id)
            a.copy(more = b != null, behind = b ?: Behind.ARROW)
        }
    }
}

/** The action armed on an app's chip: one chip for an app, whichever of its two actions the text is for. */
object Chips {
    /** The action a chip is entered with: the one [asked] for if the app has it, else the first it has. Null: it has neither. */
    fun enter(acts: List<Act>, asked: Act?): Act? = asked?.takeIf { it in acts } ?: acts.firstOrNull()

    /** The action Tab changes to from [act]: the other of the two. Null for an app that has only one. */
    fun other(acts: List<Act>, act: Act?): Act? = if (acts.size < 2) null else acts.firstOrNull { it != act }
}

/**
 * Where Enter was pressed to make an app the chip: the letters as they were typed ([text]: "spo",
 * not the app's whole name), the row that was selected ([row], its id) and the action that was
 * armed on it ([action], its id). [usual]: it was one of the rows under the empty field. Backspace
 * on the chip's empty field is one step back to exactly there.
 */
data class Origin(val text: String, val row: String, val action: String, val usual: Boolean = false) {
    /**
     * In [rows], the list made again for [text]: which row is selected and which of its actions is
     * armed, as their places. [stops]: what the arming can rest on, on a row. The row gone: row one
     * with its own default (among the usual rows none: at rest nothing is selected there). The
     * action gone, or not one the arming can rest on: the row's own default.
     */
    fun place(rows: List<Result>, stops: (Result) -> List<Int> = { it.actions.indices.toList() }): Pair<Int, Int> {
        val i = rows.indexOfFirst { it.id == row }
        if (i < 0) return (if (usual || rows.isEmpty()) -1 else 0) to (if (usual) 0 else rows.firstOrNull()?.armed ?: 0)
        val r = rows[i]
        return i to (r.actions.indexOfFirst { it.id == action }.takeIf { it in stops(r) } ?: r.armed)
    }
}
