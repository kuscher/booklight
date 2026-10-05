package io.github.kuscher.booklight.core

/**
 * First run's stage is not there in one frame: a screen is set down part by part, comes back as rows
 * do, or takes another screen's place where that stood. When each of its parts comes is said here, as
 * data (docs/design/first-run/motion.md §3: T1, T5, T8, and §2.2 for the landing of the opening
 * piece). The app draws the stage by these marks on one clock, and counts by them too: a screen takes
 * a key only once the answer Enter would run has begun to show ([Marks.lead], `FirstRun.inView`).
 * The times themselves are the app's, as all motion's are (`overlay/Motion.kt`): it hands them in as
 * a [Pace].
 */
object SetDown {
    /** How a screen comes onto the glass. */
    enum class Comes {
        /** It stands whole at once: the field was emptied again, a panel was turned round, the system's animations are off. */
        WHOLE,
        /** Set down part by part at the gate of a run's first opening: the seat, then the caps a beat apart or a recipe written a letter at a time, the caption, the answers (T1, T5). */
        SET_DOWN,
        /** At the gate of a later opening: its parts arrive as rows do, and nothing is written again. */
        BACK,
        /** Where another screen stood: the seat stays and its words roll; what changes in the band is written anew; what stays does not move (T5 in the same panel; the key's screens). */
        TURN,
        /** After a list that gave way (a lesson's Enter, the sum copied, "First steps" asked for): set down once its rows have begun to fade (T8). */
        AFTER_LIST,
        /** At the landing of the opening piece, behind the highlight that travels into its armed answer (§2.2, B5). */
        LANDS,
    }

    /**
     * The times, in ms. [part]: between the seat's parts, and between parts that come as rows do.
     * [beat]: between two pieces of a band. [letter]: between two letters of a typed part. [band]: how
     * long after the gate the band of a screen set down at an opening begins, at the least. [turn]:
     * how long after its words began to roll a screen's new band is written. [afterList]: how long
     * after a list gave way its stage begins. [press]: how long what gives way takes to fade.
     * [askText], [askNote], [askAnswers]: the question's parts, counted from its seat. [landsDisc] to
     * [landsBand]: the parts at a landing, counted from its beginning. [rest]: from a stage's last
     * part beginning to its being at rest.
     *
     * A pace is checked where it is made, for what [marks] counts on: no time is negative; the
     * question's parts come in reading order, and its text not before the screen it belongs to
     * (where it takes a lesson's place its seat stands and its text comes at [turn], at a landing
     * at [landsBand]: each less [askText] is where its count begins); and at a landing the armed
     * answer's words come before the band. A pace that breaks one of these would give a part a mark
     * before its screen came, which is drawn, and counted, as a part that is there.
     */
    data class Pace(
        val part: Int, val beat: Int, val letter: Int, val band: Int, val turn: Int, val afterList: Int, val press: Int,
        val askText: Int, val askNote: Int, val askAnswers: Int,
        val landsDisc: Int, val landsWords: Int, val landsCounter: Int, val landsArmed: Int, val landsBand: Int, val rest: Int,
    ) {
        init {
            require(listOf(part, beat, letter, band, turn, afterList, press, askText, askNote, askAnswers, landsDisc, landsWords, landsCounter, landsArmed, landsBand, rest).all { it >= 0 }) { "a pace has no negative time" }
            require(askText <= askNote && askNote <= askAnswers) { "the question's parts come in reading order: its text, its note, its answers" }
            require(turn >= askText && landsBand >= askText) { "the question's text does not come before its screen" }
            require(landsArmed <= landsBand) { "at a landing the armed answer's words come before the band" }
        }
    }

    /** The mark of a part that does not come: it is there. */
    const val THERE = -100_000

    /**
     * When each part comes, in ms after the screen came (for a screen the panel opens on: after the
     * gate). [disc], [words], [counter]: the seat. [pieces]: the band's pieces from left to right
     * (the question's: its text, its note); [written]: a typed part's letters follow its piece one
     * after the other ([letter]), else they stand with it. [caption]: the line under the band.
     * [answers]: the answers with their `tab` cap; [armed]: the words of the answer Enter would run,
     * which at a landing come first, inside the highlight. [rises]: the answers rise as they come;
     * not at a landing, where the highlight is on its way to where they stand. [end]: everything is
     * at rest.
     */
    data class Marks(
        val disc: Int, val words: Int, val counter: Int, val pieces: List<Int>, val written: Boolean,
        val caption: Int, val answers: Int, val armed: Int, val rises: Boolean, val end: Int,
    ) {
        /** How long after the screen came the answer Enter would run begins to show: from then the screen is in view. */
        val lead: Int get() = maxOf(0, armed)
        /**
         * How long after the screen came every one of its answers has begun to show. Later than [lead] at a landing
         * alone, where the armed answer's words come first: a key that can reach another answer counts from here.
         */
        val all: Int get() = maxOf(lead, answers)
    }

    /**
     * From which of a screen's marks a key or a click counts, in ms after the screen came: it is taken once the answer
     * it would run has been in view since then for `FirstRun.SEEN_MS`. [enter]: it is Enter, which runs the armed
     * answer and no other; it counts from when that answer began to show ([Marks.lead]). Tab, the pointer and a click
     * reach any answer, and count from when every answer has begun to show ([Marks.all]); so does Enter once the
     * arming has been [moved] from the answer that was armed when the screen came, since the answer it runs then is
     * one of the others. (With one mark for all of them, Tab and then Enter at a landing ran "Not now" 50 ms after it
     * began to show.)
     */
    fun since(m: Marks, enter: Boolean, moved: Boolean): Int = if (enter && !moved) m.lead else m.all

    /** Nothing comes: everything is there. */
    val WHOLE = Marks(THERE, THERE, THERE, emptyList(), written = false, caption = THERE, answers = THERE, armed = THERE, rises = false, end = 0)

    /** When piece [k] of the band begins; a band asked for a piece it has not got answers with its last. */
    fun piece(m: Marks, k: Int): Int = if (m.pieces.isEmpty()) THERE else m.pieces[k.coerceIn(0, m.pieces.lastIndex)]

    /** When letter [i] of the typed part that is piece [k] comes. */
    fun letter(m: Marks, pace: Pace, k: Int, i: Int): Int {
        val at = piece(m, k)
        return if (at == THERE || !m.written) at else at + pace.letter * maxOf(0, i)
    }

    /**
     * The marks of a screen that comes as [comes] says. [letters]: the band's pieces from left to
     * right: for a typed part of a recipe how many letters it has, 0 for a cap or a sign. [ask]: the
     * question, whose band is its text and its note. [caption]: a line stands under the band.
     * [bandAfter]: how long after the gate the band of a screen set down at an opening begins (the
     * glass is at rest by then). [bandChanges], [captionChanges], [answersChange]: for a screen that
     * takes another's place, whether its band, its caption and its answers are other than the ones
     * that stood: what says the same does not move.
     */
    fun marks(
        comes: Comes, pace: Pace, letters: List<Int>, ask: Boolean, caption: Boolean, bandAfter: Int = pace.band,
        bandChanges: Boolean = true, captionChanges: Boolean = true, answersChange: Boolean = true,
    ): Marks {
        if (comes == Comes.WHOLE) return WHOLE
        val p = pace.part
        val base = if (comes == Comes.AFTER_LIST) pace.afterList else 0
        // The seat's three parts. Where a screen takes another's place they are there already, and their words roll.
        val seat = when (comes) {
            Comes.TURN -> listOf(THERE, THERE, THERE)
            Comes.LANDS -> listOf(pace.landsDisc, pace.landsWords, pace.landsCounter)
            else -> listOf(base, base + p, base + 2 * p)
        }
        if (ask) {
            // The question: its text, its note and its answers in reading order, also where it comes back.
            val from = when (comes) { Comes.TURN -> pace.turn - pace.askText; Comes.LANDS -> pace.landsBand - pace.askText; else -> base }
            val answers = from + pace.askAnswers
            return Marks(seat[0], seat[1], seat[2], listOf(from + pace.askText, from + pace.askNote), written = false, caption = THERE, answers = answers, armed = answers, rises = comes != Comes.LANDS, end = answers + pace.rest)
        }
        // The band. A screen come back to has it whole, as a row; one that keeps the band of the screen before it does not move it.
        val whole = comes == Comes.BACK
        val from = when (comes) {
            Comes.SET_DOWN -> bandAfter
            Comes.BACK -> 3 * p
            Comes.TURN -> if (bandChanges) pace.turn else THERE
            Comes.LANDS -> pace.landsBand
            else -> base + 3 * p
        }
        val pieces = ArrayList<Int>(letters.size)
        var last = from
        for (k in letters.indices) {
            val at = if (whole || from == THERE || k == 0) from else last + pace.beat
            pieces += at
            last = if (whole || from == THERE) from else at + pace.letter * maxOf(0, letters[k] - 1)
        }
        // What follows the band: the caption a beat after its last piece, the answers one part after that.
        val after = when {
            whole -> from + p
            from == THERE -> pace.press
            letters.isEmpty() -> from
            else -> last + pace.beat
        }
        val captionAt = if (!caption || (comes == Comes.TURN && !captionChanges)) THERE else after
        val rest = if (caption) after + p else after
        val answers = if (comes == Comes.TURN && !answersChange) THERE else rest
        // (At a landing the armed answer's words fade in inside the highlight that has come for them, before the band.)
        val armed = if (comes == Comes.LANDS) pace.landsArmed else answers
        return Marks(seat[0], seat[1], seat[2], pieces, written = !whole, caption = captionAt, answers = answers, armed = armed, rises = comes != Comes.LANDS, end = maxOf(0, last, captionAt, answers, armed, seat[2]) + pace.rest)
    }

    /**
     * Whether two screens show the same thing in their band, so that it stays where it is when the
     * one takes the other's place: the key's screens after the first all show "your key". Every other
     * screen has a band of its own.
     */
    fun sameBand(a: FirstRun.Screen?, b: FirstRun.Screen): Boolean {
        val yours = setOf(FirstRun.Screen.K2, FirstRun.Screen.K3, FirstRun.Screen.K4)
        return a == b || (a in yours && b in yours)
    }

    /**
     * Whether the caption under two screens' bands says the same, so that it stays where it is when
     * the one takes the other's place: lessons 2 and 3 share theirs ("Practice: nothing opens…"), and
     * two of the key's screens do where the suggested key leaves them the same words. [suggested]:
     * the key that is suggested, which the key's captions are worded for.
     */
    fun sameCaption(a: FirstRun.Screen?, b: FirstRun.Screen, suggested: FirstRun.Key): Boolean {
        fun words(on: FirstRun.Screen?): String = when (on) {
            null, FirstRun.Screen.Q, FirstRun.Screen.C -> "none"
            FirstRun.Screen.L2, FirstRun.Screen.L3 -> "practice"
            FirstRun.Screen.L4 -> "stays"
            else -> FirstRun.caption(on, suggested).name.lowercase()
        }
        return words(a) == words(b)
    }
}
