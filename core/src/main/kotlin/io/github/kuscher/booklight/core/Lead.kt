package io.github.kuscher.booklight.core

/**
 * Who leads when words are typed after an app's name, without a chip: "netflix stock price" and
 * "netflix severance" cannot be told apart, so the rule goes by what the user does, for each app.
 * The web leads until the app's own row ("Search Netflix for …") has been picked once; from then
 * on the app leads for words after its name; the web's row picked twice running, with the app's
 * row beside it, gives the lead back to the web.
 *
 * One number for an app holds it: 0 while the web leads, else how many picks of the web's row,
 * one after the other, still take the lead back. It is kept with what Booklight learns ([History]).
 */
object Lead {
    /** How many picks of the web's row running give the lead back to the web. */
    const val BACK = 2

    /** The app leads. */
    fun app(state: Int): Boolean = state > 0

    /** The app's row was picked: the app leads, whatever was picked before. */
    fun pickedApp(): Int = BACK

    /** The web's row was picked while the app's row stood in the list. */
    fun pickedWeb(state: Int): Int = (state - 1).coerceAtLeast(0)
}
