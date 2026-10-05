package io.github.kuscher.booklight.core

/**
 * First run: Booklight is given a key, three things are tried once, one question is answered and
 * two choices are offered, on a stage under the empty field. Everything that is decided about it
 * is decided here: which screen stands at the next opening, what an answer changes, who gets a
 * run at all. The app keeps a [State] in its settings and draws what [screen] says; nothing here
 * knows Android.
 *
 *  - **A run** belongs to a new installation ([Run.NEW]: every step), to one that was there before
 *    and has no key yet ([Run.UPDATE]: the key alone), or was asked for again ([Run.REPLAY]).
 *  - **A step** is over once it was done, skipped or answered ([State.done]); the next one stands.
 *  - **An unfinished run** stands in [MAX_OPENS] openings. After that it waits in the Booklight
 *    window, with what was done kept, until it is asked for ([resume]).
 *  - **"First steps"** asks for it again ([again]): the Booklight window's row, with the opening
 *    piece first, and the command typed in the panel ([AGAIN]), without. Where the steps wait is
 *    said once, in the opening they began to wait in ([later]).
 */
object FirstRun {
    /** Which run an installation is in. Kept in the settings as its number ([id]). */
    enum class Run(val id: Int) {
        NONE(0), NEW(1), UPDATE(2), REPLAY(3);

        companion object {
            /** The run a stored number stands for; a number nobody knows is no run. */
            fun of(id: Int): Run = entries.firstOrNull { it.id == id } ?: NONE
        }
    }

    /** The steps, in their order. [id] is how a step is kept once it is over. */
    enum class Step(val id: String) { KEY("key"), OPEN("open"), SEARCH("search"), SUM("sum"), ASK("ask"), CHOICES("choices") }

    /**
     * What stands under the empty field. K1 asks for the key; K2 is back from the system's dialog
     * without one; K3 is back again; K4: the key works. L2 to L4 are the lessons, Q the question,
     * C the choices.
     */
    enum class Screen(val step: Step) {
        K1(Step.KEY), K2(Step.KEY), K3(Step.KEY), K4(Step.KEY), L2(Step.OPEN), L3(Step.SEARCH), L4(Step.SUM), Q(Step.ASK), C(Step.CHOICES)
    }

    /**
     * What is kept. [done]: the ids of the steps that are over, and two words that are no steps
     * ([SHOWN], [CHANGE]). [opens]: in how many openings this run has stood. [helper]: how often the
     * system's dialog came over the panel for the key. [icon]: the icon's first click has shown the
     * panel. [key]: a key has opened the panel, ever. [suggestions]: search suggestions are on.
     * [mark]: step 1 is over on this installation, however it ended. [sums]: sums are answered
     * ("Show sums" is on); only read here, never changed.
     */
    data class State(
        val run: Run = Run.NONE,
        val done: List<String> = emptyList(),
        val opens: Int = 0,
        val helper: Int = 0,
        val icon: Boolean = false,
        val key: Boolean = false,
        val suggestions: Boolean = false,
        val mark: Boolean = false,
        val sums: Boolean = true,
    )

    /** In [State.done]: the opening (the welcome and the show) has been shown in this run. */
    const val SHOWN = "show"
    /** In [State.done]: a replay asks for the key again although one is known ("Change the key"). */
    const val CHANGE = "change"
    /** An unfinished run stands in this many openings, then waits in the Booklight window. */
    const val MAX_OPENS = 3
    /**
     * The least screen height, in dp, at which first run stands in the panel (its tallest screen is 324 dp): (324 + 56) / 0.8,
     * the 56 dp being what the panel keeps free below it, on a panel whose top edge is at 20 % of the screen (`overlay/Metrics.kt`),
     * so a change there needs a change here.
     */
    const val MIN_SCREEN_DP = 475f

    private fun over(s: State, step: Step) = step.id in s.done

    /**
     * The steps this run has. The run of an installation that was there before is the key alone.
     * The question is a step only while suggestions are off, or once it was answered in this run.
     * The sum's lesson is a step only while sums are answered, or once it is over in this run: an
     * installation that has been lived in may have them switched off when the run is asked for again,
     * and a lesson whose example finds nothing to copy could only be skipped.
     */
    fun steps(s: State): List<Step> = when (s.run) {
        Run.NONE -> emptyList()
        Run.UPDATE -> listOf(Step.KEY)
        Run.NEW, Run.REPLAY -> Step.entries.filter { step ->
            when (step) {
                Step.ASK -> !s.suggestions || over(s, Step.ASK)
                Step.SUM -> s.sums || over(s, Step.SUM)
                else -> true
            }
        }
    }

    /** The step whose turn it is; null: there is no run, or nothing is left of it. */
    fun step(s: State): Step? = steps(s).firstOrNull { !over(s, it) }

    /** The run has stood in [MAX_OPENS] openings and was not finished: it waits in the Booklight window. */
    fun parked(s: State): Boolean = s.run != Run.NONE && s.opens > MAX_OPENS

    /** What stands under the empty field; null: nothing of first run. */
    fun screen(s: State): Screen? {
        if (parked(s)) return null
        return when (step(s) ?: return null) {
            Step.KEY -> when {
                s.key && CHANGE !in s.done -> Screen.K4
                s.helper == 0 -> Screen.K1
                s.helper == 1 -> Screen.K2
                else -> Screen.K3
            }
            Step.OPEN -> Screen.L2
            Step.SEARCH -> Screen.L3
            Step.SUM -> Screen.L4
            Step.ASK -> Screen.Q
            Step.CHOICES -> Screen.C
        }
    }

    /**
     * "2 of 5": where the screen that stands is in its run. Null where no counter stands: on the
     * choices, which are the last screen, and in a run of one step.
     */
    fun count(s: State): Pair<Int, Int>? {
        val on = screen(s) ?: return null
        val counted = steps(s).filter { it != Step.CHOICES }
        if (on == Screen.C || counted.size < 2) return null
        return counted.indexOf(on.step) + 1 to counted.size
    }

    /** Whether first run stands in the panel on a screen this high; on a lower one the Booklight window's Start page has the key's row. */
    fun fits(screenDp: Float): Boolean = screenDp >= MIN_SCREEN_DP

    // ---- answers

    /**
     * What can be answered. [OPEN_HELPER]: "Open Keyboard shortcuts"; it changes nothing that is kept
     * (the panel asks the system for its dialog and holds on). [DONE]: the choices were left, however
     * they were left.
     */
    enum class Answer { OPEN_HELPER, NOT_NOW, GO_ON, CHANGE_KEY, SKIP, AGREE, DONE }

    private fun finish(s: State, step: Step): State = if (over(s, step)) s else s.copy(done = s.done + step.id)

    /** The run is over: nothing of it is kept but that step 1 has ended on this installation. */
    private fun ended(s: State): State = s.copy(run = Run.NONE, done = emptyList(), opens = 0, helper = 0, mark = true)

    /** A run with no step left is over. */
    private fun settle(s: State): State = if (s.run != Run.NONE && step(s) == null) ended(s) else s

    /**
     * [s] after [answer] was given on the screen that stands. An answer that screen does not offer
     * changes nothing. "Not now" on the key ends the run; on the question it is an answer like
     * another. Suggestions are switched on in one place only: [Answer.AGREE] on the question.
     */
    fun answer(s: State, answer: Answer): State {
        val on = screen(s) ?: return s
        return settle(when (on) {
            Screen.K1, Screen.K2, Screen.K3 -> if (answer == Answer.NOT_NOW) ended(s) else s
            Screen.K4 -> when (answer) {
                Answer.GO_ON -> finish(s, Step.KEY).copy(mark = true)
                Answer.CHANGE_KEY -> if (s.run == Run.REPLAY) s.copy(done = s.done + CHANGE, helper = 0) else s
                else -> s
            }
            Screen.L2, Screen.L3, Screen.L4 -> if (answer == Answer.SKIP) finish(s, on.step) else s
            Screen.Q -> when (answer) {
                Answer.AGREE -> finish(s, Step.ASK).copy(suggestions = true)
                Answer.NOT_NOW -> finish(s, Step.ASK)
                else -> s
            }
            Screen.C -> if (answer == Answer.DONE) finish(s, Step.CHOICES) else s
        })
    }

    /**
     * The key opened the panel, or uncovered it under the system's dialog. A key that was known
     * already changes nothing, unless the run was asking for another one. A run that waited for its
     * first key comes back: the first press of the user's own key is what first run is for. (A key
     * that was known is no such news: a replay that asked for another one and then waited goes on waiting.)
     */
    fun keyLanded(s: State): State {
        if (s.key && CHANGE !in s.done) return s
        val t = s.copy(key = true)
        if (s.run == Run.NONE || over(s, Step.KEY)) return t
        return t.copy(done = t.done - CHANGE, helper = 0, opens = if (s.key) s.opens else 0)
    }

    /** The system's dialog came over the panel while it asked for the key. */
    fun helperCame(s: State): State = when (screen(s)) {
        Screen.K1, Screen.K2, Screen.K3 -> s.copy(helper = s.helper + 1)
        else -> s
    }

    // ---- coming back

    /**
     * A screen of this run comes in a new opening: counted once for each panel, at the moment it
     * comes. Past [MAX_OPENS] the run waits ([parked]) and nothing comes.
     */
    fun opened(s: State): State = if (s.run == Run.NONE || step(s) == null || parked(s)) s else s.copy(opens = s.opens + 1)

    /** The run is asked for again where it stopped (the Booklight window's "Go on with the first steps"). */
    fun resume(s: State): State = if (s.run == Run.NONE) s else s.copy(opens = 0)

    /**
     * First run from the start, asked for by the user. No switch is touched, and the key that is
     * known stays known: the run then begins on "Your key works". [overture]: with the opening
     * first (the Booklight window's row); without it where the user is at work in the panel.
     */
    fun replay(s: State, overture: Boolean): State =
        s.copy(run = Run.REPLAY, done = if (overture) emptyList() else listOf(SHOWN), opens = 0, helper = 0)

    /**
     * "First steps" was asked for: an unfinished run goes on where it stopped, a finished one is
     * played again. [overture]: asked for by the Booklight window's row, which plays the whole of it,
     * the opening piece first; false for the command typed in the panel, where the user is at work
     * and the run begins in the panel that is open.
     */
    fun again(s: State, overture: Boolean): State = if (s.run != Run.NONE) resume(s) else replay(s, overture)

    /** What the "First steps" command carries in place of something to run: the panel catches it before anything is run, and asks [againHere]. */
    val AGAIN: Effect = Effect.Internal("first")

    /** How many letters of its name the "First steps" command must be typed by, at the least ([typedFor]). */
    const val TYPED_LETTERS = 3

    /**
     * Whether the "First steps" command is offered for what was [typed]: only where it was typed
     * for. [name]: its name ("First steps"); [words]: the other words it is found by (tour, welcome,
     * intro), both in the user's language. Typed for it: [TYPED_LETTERS] letters or more that begin
     * its name ("fir", "first s"), or one of its other words in full, in any case. Nothing less: by
     * the matching every other row is found by, "f", "fi" and "w" would find it too (the start of a
     * name, the start of a hidden word), and someone who never asked for first run would meet its
     * row for a letter or two, as row one where no app begins with those letters. Such a row is no
     * part of an ordinary day.
     */
    fun typedFor(typed: String, name: String, words: List<String>): Boolean {
        val t = Matcher.fold(typed)
        if (t.isEmpty()) return false
        return (t.length >= TYPED_LETTERS && Matcher.fold(name).startsWith(t)) || words.any { Matcher.fold(it) == t }
    }

    /**
     * "First steps" was asked for by its command, in a panel that is open: [again] without the
     * opening piece, and the run stands in that very panel, which is therefore the first of its
     * [MAX_OPENS] openings (no new panel is made that could count itself).
     */
    fun againHere(s: State): State = opened(again(s, overture = false))

    /**
     * What the Booklight window's row for the first steps offers. [goesOn]: a run is unfinished, and
     * the row goes on with it where it stopped; else it plays the whole of it again. [count]: where
     * an unfinished run stopped, as its counter says it ("3 of 5"), also while it waits; null where
     * no counter stands.
     */
    data class Offer(val goesOn: Boolean, val count: Pair<Int, Int>?)

    fun offer(s: State): Offer = if (s.run == Run.NONE) Offer(goesOn = false, count = null) else Offer(goesOn = true, count = count(resume(s)))

    /**
     * The steps have begun to wait in the Booklight window in the change from [before] to [after],
     * and the bare field says so for the rest of that opening: "Not now" was answered on the key's
     * step, which ends the run; or an unfinished run has stood in its [MAX_OPENS] openings and this
     * one is the first in which nothing stands. Once: a run that waits says nothing at the openings
     * after that, and a run that was finished has nothing waiting.
     */
    fun later(before: State, after: State): Boolean {
        val asked = screen(before).let { it == Screen.K1 || it == Screen.K2 || it == Screen.K3 }
        return (asked && after.run == Run.NONE) || (before.run != Run.NONE && !parked(before) && parked(after))
    }

    /**
     * For the debug hooks: [s] moved to where [wanted] stands, without touching whether a key is
     * known or whether suggestions are on. Null where that cannot be: "Your key works" without a
     * key, the question with suggestions on.
     */
    fun at(s: State, wanted: Screen): State? {
        val run = if (s.run == Run.NEW || s.run == Run.REPLAY) s.run else Run.REPLAY
        val before = Step.entries.takeWhile { it != wanted.step }.map { it.id }
        val base = s.copy(run = run, done = before + SHOWN, opens = 0, helper = 0)
        val to = when (wanted) {
            Screen.K1, Screen.K2, Screen.K3 -> base.copy(done = if (s.key) base.done + CHANGE else base.done, helper = wanted.ordinal)
            else -> base
        }
        return to.takeIf { screen(it) == wanted }
    }

    // ---- an installation that was there before

    /**
     * In the settings' list of tips that have had their turn: step 1 is over on this installation.
     * It is kept there because every build with tips keeps that list whole and reads it only for
     * the tips it knows, while a build from before first run drops first run's own fields when it
     * writes the settings.
     */
    const val MARK = "first:key"

    /**
     * The state of an installation that was there before this build, from what its settings hold
     * ([old]: whether a key is known, the mark, the switch): no run with a key or with the mark;
     * else the key's step, once. Nothing else of [old] is kept, so it gives the same answer every
     * time the same settings are read, and again after an older build has written them.
     */
    fun forUpdate(old: State): State =
        State(run = if (old.key || old.mark) Run.NONE else Run.UPDATE, key = old.key, suggestions = old.suggestions, mark = old.mark)

    /** "Show the tips again" empties the list of tips that have had their turn: the mark stays in it. */
    fun keepMark(tipsSeen: List<String>): List<String> = tipsSeen.filter { it == MARK }

    // ---- the lessons

    /**
     * What Enter does while a lesson stands. Nothing opens in first run: an app's Open and a search
     * inside an app are practice (the footer says what Enter would have done), a sum is copied for
     * real and the panel stays. Everything else is as every day.
     */
    enum class Enter { AS_ALWAYS, PRACTICE_OPEN, PRACTICE_SEARCH, COPY_STAYS }

    /**
     * What Enter does on an action with [effect] now. [line]: the line of the list of everything
     * the row belongs to (the app's `Guide.used`: `appsearch`, `sums`, `settings`…), or null.
     * [searchable]: an app of this device can be searched; where none can, a settings page found
     * under the Settings keyword stands in for lesson 3. A lesson's thing counts whichever lesson
     * stands. Making an app the chip (Search on its row) is no practice: it is done for real.
     */
    /**
     * The lines of the list of everything that a lesson knows (the app's `Guide.used` says them by these names: were one
     * renamed there alone, a lesson's Enter would run as every day). `settings` is the Settings scope's own key.
     */
    const val LINE_SEARCH = "appsearch"
    const val LINE_SUMS = "sums"
    const val LINE_SETTINGS = "settings"

    fun enters(s: State, line: String?, effect: Effect, searchable: Boolean): Enter {
        val on = screen(s)
        if (on != Screen.L2 && on != Screen.L3 && on != Screen.L4) return Enter.AS_ALWAYS
        return when {
            effect is Effect.LaunchApp -> Enter.PRACTICE_OPEN
            line == LINE_SEARCH && effect is Effect.Open -> Enter.PRACTICE_SEARCH
            !searchable && line == LINE_SETTINGS && effect is Effect.OpenSettings -> Enter.PRACTICE_SEARCH
            line == LINE_SUMS && effect is Effect.CopyText -> Enter.COPY_STAYS
            else -> Enter.AS_ALWAYS
        }
    }

    /** [s] after Enter did [enter]: the lesson it belongs to is over. Where no lesson stands, nothing is marked. */
    fun ran(s: State, enter: Enter): State {
        val on = screen(s)
        if (on != Screen.L2 && on != Screen.L3 && on != Screen.L4) return s
        return settle(when (enter) {
            Enter.PRACTICE_OPEN -> finish(s, Step.OPEN)
            Enter.PRACTICE_SEARCH -> finish(s, Step.SEARCH)
            Enter.COPY_STAYS -> finish(s, Step.SUM)
            Enter.AS_ALWAYS -> s
        })
    }

    /** The lesson that stands (the second, third or fourth step's screen); null: none does. */
    fun lesson(s: State): Screen? = screen(s)?.takeIf { it == Screen.L2 || it == Screen.L3 || it == Screen.L4 }

    /**
     * What the footer's left end says while a lesson's list is typed: the next key, and what it does
     * today. [OPENS]: Enter opens what is selected, for practice. [TO_SEARCH]: Tab moves to Search.
     * [INTO_APP]: Enter makes the app the chip. [SEARCHES]: Enter searches inside the app, for
     * practice. [COPIES]: Enter copies the answer, and the panel stays. [TITLE]: none of these: the
     * lesson's own title stands there.
     */
    enum class Coach { OPENS, TO_SEARCH, INTO_APP, SEARCHES, COPIES, TITLE }

    /**
     * The coach line for the lesson that stands and the row that is selected; null where no lesson
     * stands. [enter]: what Enter would do on that row now ([enters]). [effect]: the armed action's;
     * null with nothing selected. [search]: the row also offers Search (lesson 3's Tab has somewhere
     * to go). It never promises what Enter will not do: a row that is not the lesson's own gets the
     * lesson's title.
     */
    fun coach(s: State, enter: Enter, effect: Effect?, search: Boolean): Coach? = when (lesson(s)) {
        Screen.L2 -> if (enter == Enter.PRACTICE_OPEN) Coach.OPENS else Coach.TITLE
        Screen.L3 -> when {
            // (Where no app can be searched a settings page stands in, and Enter opens it.)
            enter == Enter.PRACTICE_SEARCH -> if (effect is Effect.OpenSettings) Coach.OPENS else Coach.SEARCHES
            effect is Effect.EnterScope && effect.act == Act.SEARCH -> Coach.INTO_APP
            enter == Enter.PRACTICE_OPEN && search -> Coach.TO_SEARCH
            else -> Coach.TITLE
        }
        Screen.L4 -> if (enter == Enter.COPY_STAYS) Coach.COPIES else Coach.TITLE
        else -> null
    }

    /** How many letters of an app's name a recipe shows as what to type: three at least, four at most (the recipe has room for no more). */
    const val LEAST_LETTERS = 3
    const val MOST_LETTERS = 4
    /** The longest word to look for that a recipe shows; a longer one is cut. */
    const val RECIPE_WORD = 8

    /**
     * The first [n] characters of [text] as a reader counts them, never half of one ([ends]). (An
     * app's name may begin with an emoji; cut by units, its first letters ended in half of one.)
     */
    fun head(text: String, n: Int): String = if (n <= 0) "" else ends(text).let { text.substring(0, it.getOrElse(n - 1) { text.length }) }

    /**
     * Where each character of [text] ends, as a reader counts them: one place for each, in the
     * text's own units, the last of them the text's length. A character that takes two of the units
     * (an emoji, a letter outside the common ones) is one, and what belongs to a character stays
     * with it: a mark over a letter, a skin tone, a variation selector, whatever a joiner joins to
     * it, the second half of a flag. A recipe's letters are cut by this ([head]), and written by
     * it, a character at a time.
     */
    fun ends(text: String): List<Int> {
        val at = ArrayList<Int>()
        var end = 0
        var begun = false     // a character has begun: the next one that begins ends it
        var half = false      // the character before was the first half of a flag
        while (end < text.length) {
            val cp = text.codePointAt(end)
            val flag = cp in 0x1F1E6..0x1F1FF
            val joined = end > 0 && text.codePointBefore(end) == JOINER
            val belongs = joined || cp == JOINER || cp in 0xFE00..0xFE0F || cp in 0x1F3FB..0x1F3FF || (flag && half) ||
                Character.getType(cp).let { it == Character.NON_SPACING_MARK.toInt() || it == Character.ENCLOSING_MARK.toInt() || it == Character.COMBINING_SPACING_MARK.toInt() }
            if (!belongs) {
                if (begun) at += end
                begun = true
                half = flag
            } else if (flag) half = false
            end += Character.charCount(cp)
        }
        if (text.isNotEmpty()) at += text.length
        return at
    }

    /** What joins two characters into one (a family, a flag with a sign on it). */
    private const val JOINER = 0x200D

    /**
     * The starts of an app's [name] that a recipe may show, the shortest first: of its first word (a
     * space would end the letters), in small letters. A name of three letters or fewer is typed whole.
     * None where the name begins with something a keyboard cannot type (an emoji, a sign): a recipe
     * shows letters to type, so such an app is not the example, and Settings stands in for it.
     */
    fun starts(name: String): List<String> {
        val word = name.trim().lowercase().substringBefore(' ')
        return when {
            word.isEmpty() -> emptyList()
            !Character.isLetterOrDigit(word.codePointAt(0)) -> emptyList()
            head(word, LEAST_LETTERS) == word -> listOf(word)
            else -> (LEAST_LETTERS..MOST_LETTERS).map { head(word, it) }.distinct()
        }
    }

    /**
     * The letters to show for the app called [name]: its shortest start that puts its row first
     * ([leads]: typed, these letters have that app's row as row one). Null: none does, and this app
     * cannot be the example: Enter is pressed on row one.
     */
    inline fun letters(name: String, leads: (String) -> Boolean): String? = starts(name).firstOrNull(leads)

    /**
     * What the recipes of lessons 2 and 3 show, on this device. [open]: the letters lesson 2 types;
     * empty where there is no app to name. [search]: what lesson 3 types before Tab. [enter]: Enter
     * follows that Tab: on an app's row Tab moves to Search and Enter makes the app the chip; false
     * where the Settings keyword stands in, which Tab alone makes the chip. [word]: what is then
     * looked for.
     */
    data class Example(val open: String, val search: String, val enter: Boolean, val word: String)

    /**
     * The example for this device. [app]: the letters of an app that can be searched ([letters]);
     * lesson 3 then builds on lesson 2's app and looks for [word]. Null where no app here can be
     * searched, or none leads its list: lesson 2 then names the Settings app ([settings]: its
     * letters, null where it has none), and lesson 3 goes by the Settings [keyword] to the page [page].
     */
    fun example(app: String?, word: String, settings: String?, keyword: String, page: String): Example =
        if (app != null) Example(app, app, enter = true, word = word) else Example(settings.orEmpty(), keyword, enter = false, word = page)

    /** One piece of a lesson's recipe: letters to type, shown as they will stand in the field, or a key to press. */
    sealed interface Part {
        data class Typed(val text: String) : Part
        enum class Key : Part { TAB, ENTER }
    }

    /**
     * The recipe of the lesson that stands, left to right: what to type and which keys to press.
     * [sum]: what lesson 4 types. Empty where no lesson stands. Letters are cut to what the band
     * has room for, and where there is nothing to type that piece is left out.
     */
    fun recipe(s: State, example: Example, sum: String): List<Part> {
        fun typed(text: String, most: Int): Part? = head(text, most).takeIf { it.isNotEmpty() }?.let { Part.Typed(it) }
        return when (lesson(s)) {
            Screen.L2 -> listOfNotNull(typed(example.open, MOST_LETTERS), Part.Key.ENTER)
            Screen.L3 -> listOfNotNull(typed(example.search, MOST_LETTERS), Part.Key.TAB, Part.Key.ENTER.takeIf { example.enter }, typed(example.word, RECIPE_WORD), Part.Key.ENTER)
            Screen.L4 -> listOf(Part.Typed(sum), Part.Key.ENTER)
            else -> emptyList()
        }
    }

    // ---- the start

    /** How an opening begins. [WELCOME]: with the opening piece: the welcome, then the show in which Booklight uses itself, then the key's screen. */
    enum class Overture { NONE, WELCOME }

    /**
     * The least screen height, in dp, for the welcome: its glass is 468 dp high, so (468 + 56) / 0.8, with the same 56 dp kept free
     * below and the same top edge at 20 % of the screen (`overlay/Metrics.kt`) as [MIN_SCREEN_DP], and a change there needs a change here.
     */
    const val WELCOME_SCREEN_DP = 655f

    /**
     * Whether this opening begins with the opening piece: once, on the very first start of a new
     * installation, and again where a replay asks for it. [motion]: the system's animations are on.
     * [reader]: a screen reader is on. [plain]: the start carries no text from another app. Asking
     * uses nothing up: [shown] does.
     */
    fun overture(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Overture = when {
        // (A screen too low for the welcome's glass has no piece at all: the show is not played without its welcome.)
        !motion || reader || !plain || screenDp < WELCOME_SCREEN_DP -> Overture.NONE
        s.run != Run.NEW && s.run != Run.REPLAY -> Overture.NONE
        SHOWN in s.done || parked(s) || step(s) != Step.KEY -> Overture.NONE
        else -> Overture.WELCOME
    }

    /**
     * The opening piece is this opening's and is not played, because the system's animations are off
     * or a screen reader is on: the key's screen stands at once, and the welcome's title greets as the
     * field's placeholder (and is said to a screen reader before the screen's own words). Like
     * [overture] it uses nothing up.
     */
    fun greets(s: State, motion: Boolean, reader: Boolean, plain: Boolean, screenDp: Float): Boolean =
        (!motion || reader) && overture(s, motion = true, reader = false, plain = plain, screenDp = screenDp) != Overture.NONE

    /** The opening piece has ended, or has played for two seconds: it does not come again in this run. */
    fun shown(s: State): State = if (SHOWN in s.done) s else s.copy(done = s.done + SHOWN)

    /** The very first opening of a new installation, the one its opening piece begins, runs at the Slow speed whatever is set. */
    fun slow(s: State, overture: Overture): Boolean = s.run == Run.NEW && overture != Overture.NONE

    // ---- the opening piece, while it plays

    /**
     * Where the opening piece is. [WELCOME]: the welcome stands. [SHOW]: Booklight types into its own
     * field. [LANDING]: the key's screen is being set down, and has not been in view for long enough
     * to take a key.
     */
    enum class Playing { WELCOME, SHOW, LANDING }

    /**
     * A key or a click while the piece plays, as the panel sees it. [TYPES]: a key that puts a
     * character into the field. [ENTER]: Enter. [CUE]: a click on the welcome's handle, which carries
     * the cue. [OTHER]: Esc, Tab, an arrow, Backspace, any other key, and a click elsewhere on the
     * glass. [MODIFIER]: Shift, Ctrl, Alt or the Action key alone.
     */
    enum class Press { TYPES, ENTER, CUE, OTHER, MODIFIER }

    /**
     * What a press makes of the piece. [NOTHING]: it goes on. [BEGIN]: the welcome hands over to the
     * show. [LAND]: the piece ends and the key's screen is set down. [TYPE]: the piece ends in that
     * frame and the character is typed, as on any day.
     */
    enum class Ends { NOTHING, BEGIN, LAND, TYPE }

    /**
     * What the show has to show on this device. [WAITS]: its rows are still being worked out.
     * [READY]: they are there. [NONE]: this device has nothing to show (no app whose name begins with
     * a letter): there is no show.
     */
    enum class Cast { WAITS, READY, NONE }

    /**
     * What [press] does while the piece is [playing]. A typed character always wins. The cue (Enter
     * in the welcome, a click on its handle) is the one thing in the piece that is the user's to run:
     * it begins the show; where the device has nothing to show ([cast]), it sets the key's screen
     * down instead, and never does nothing. Every other press sets the key's screen down, once: while
     * that happens, nothing more does. Whatever the answer, a press that is not [Ends.TYPE] is used
     * up here and reaches nothing else: the key's screen comes with an answer armed, and no Enter
     * pressed during the piece may be that answer's.
     *
     * While the rows are still being worked out ([Cast.WAITS]) the cue answers [Ends.BEGIN] too, and
     * nothing here keeps it: the app does. It notes that the cue was given, begins no show yet, and
     * asks [handOver] from then on, which says when the show can begin, or that the key's screen is
     * set down instead (the rows turned out to be none, or the welcome has waited its second).
     */
    fun pressed(playing: Playing, press: Press, cast: Cast = Cast.READY): Ends = when {
        press == Press.MODIFIER -> Ends.NOTHING
        press == Press.TYPES -> Ends.TYPE
        playing == Playing.LANDING -> Ends.NOTHING
        playing == Playing.WELCOME && (press == Press.ENTER || press == Press.CUE) -> if (cast == Cast.NONE) Ends.LAND else Ends.BEGIN
        else -> Ends.LAND
    }

    /**
     * The welcome has stood and Booklight presses the handle itself, or the cue was given: what
     * happens now. With a show it begins. With none the key's screen is set down in that moment.
     * While this device's rows are still being worked out the welcome stands on; once it has
     * [waited] its second for them, the key's screen is set down without a show. A cue given
     * meanwhile is the app's to keep ([pressed]): it asks here again, frame by frame, until the
     * answer is no longer [Ends.NOTHING].
     */
    fun handOver(cast: Cast, waited: Boolean): Ends = when (cast) {
        Cast.READY -> Ends.BEGIN
        Cast.NONE -> Ends.LAND
        Cast.WAITS -> if (waited) Ends.LAND else Ends.NOTHING
    }

    /** The key suggested beside Action. */
    enum class Key { QUICK_INSERT, M }

    /** A keyboard that is attached. [external]: not the device's own. [quickInsert]: it has the Quick Insert key. */
    data class Keyboard(val id: Int, val external: Boolean, val quickInsert: Boolean)

    /**
     * Whether the keyboard in front has the Quick Insert key: the one the last key came from
     * ([last], its id); before any key, the device's own; with neither, the first there is. No
     * keyboard known: no.
     */
    fun hasQuickInsert(keyboards: List<Keyboard>, last: Int?): Boolean =
        (keyboards.firstOrNull { it.id == last } ?: keyboards.firstOrNull { !it.external } ?: keyboards.firstOrNull())?.quickInsert ?: false

    /** Action + Quick Insert where the keyboard has that key; Action + M where it has not. */
    fun suggest(quickInsert: Boolean): Key = if (quickInsert) Key.QUICK_INSERT else Key.M

    /** The other keys to try when the suggested ones did not work: Action + M after Quick Insert; after M, nothing more. */
    fun other(suggested: Key): Key? = if (suggested == Key.QUICK_INSERT) Key.M else null

    // ---- the gate

    /** What stands in the panel on a screen [screenDp] high: [screen], or nothing where first run does not fit. */
    fun stage(s: State, screenDp: Float): Screen? = if (fits(screenDp)) screen(s) else null

    /**
     * A panel is made. An unfinished run stands in [MAX_OPENS] openings, so this one is counted here,
     * before anything asks what stands or how the opening begins: asked first, a fourth opening would
     * still begin with the opening piece and end on an empty field. The order for every panel is
     * [keyLanded] (where the key made it), then this, then [overture] and [stage]. On a screen too
     * low for first run nothing is counted. [plain]: the panel is an ordinary one: it carries no text
     * from another app and no example is about to be typed into it. In any other panel no screen can
     * stand, so it is not one of the run's openings.
     */
    fun opening(s: State, screenDp: Float, plain: Boolean): State = if (plain && fits(screenDp)) opened(s) else s

    // ---- the stage, as it stands

    /**
     * The answers the screen that stands offers, in their order. A lesson offers Skip; the question,
     * Not now and then Agree: Tab arms the first, and Tab then Enter are the keys that skipped each
     * lesson before it, so the answer they reach must be the one that agrees to nothing. None on the
     * choices, which are a list of their own, and none where nothing stands.
     */
    fun answers(s: State): List<Answer> = when (screen(s)) {
        Screen.K1, Screen.K2, Screen.K3 -> listOf(Answer.OPEN_HELPER, Answer.NOT_NOW)
        Screen.K4 -> if (s.run == Run.REPLAY) listOf(Answer.GO_ON, Answer.CHANGE_KEY) else listOf(Answer.GO_ON)
        Screen.L2, Screen.L3, Screen.L4 -> listOf(Answer.SKIP)
        Screen.Q -> listOf(Answer.NOT_NOW, Answer.AGREE)
        Screen.C, null -> emptyList()
    }

    /**
     * Which of them Enter runs when the screen comes: on the key's screens the first. On "Now press
     * your keys" none: the thing to press there is the key. In a lesson none: the thing to do there
     * is to type, and Enter on the empty field does nothing, as every day. On the question none: an
     * answer there is consent, and Enter gives none until Tab or the pointer has chosen one. -1: none.
     */
    fun armed(s: State): Int = when (screen(s)) {
        Screen.K1, Screen.K3, Screen.K4 -> 0
        else -> -1
    }

    /**
     * Tab on the stage, with [armed] armed: the next answer, with [back] the one before ([tab]).
     * [arrived]: the glass has opened and the stage can be seen. Until then a key is pressed blind,
     * and changes nothing.
     */
    fun tabbed(s: State, armed: Int, back: Boolean, arrived: Boolean): Int = if (arrived) tab(armed, answers(s).size, back) else armed

    /**
     * Enter on the stage: the answer that is armed. Null, and Enter does nothing, where none is
     * armed and before the stage has [arrived]: Tab and Enter pressed while the panel was still
     * opening must not answer a question nobody has read.
     */
    fun entered(s: State, armed: Int, arrived: Boolean): Answer? = if (arrived) answers(s).getOrNull(armed) else null

    /**
     * Tab among [count] answers of which [armed] is armed (-1: none): the next one, with [back] the
     * one before, round and round. The first press arms the first, whichever way it goes.
     */
    fun tab(armed: Int, count: Int, back: Boolean): Int = when {
        count <= 0 -> -1
        armed !in 0 until count -> 0
        else -> (armed + (if (back) count - 1 else 1)) % count
    }

    // ---- in view

    /** How long a screen of first run must have been in view before it takes a key or a click. */
    const val SEEN_MS = 350L

    /**
     * Whether the screen that stands takes a key pressed at [press]. All of [press], [opened] and
     * [came] are ms of one clock that only goes forward (the app's is the time since the device
     * started, which a key event carries too); [press] is the key's own time, when it was pressed
     * and not when it was handled. [opened]: when the glass had opened far enough for the stage to
     * be seen. [came]: when the screen came: for one the panel opens on, the panel's making, which
     * is before [opened]; for one that comes later, after it. The count begins at the later of the
     * two, never before. [lead]: how long after that beginning the answer the key would run begins
     * to show, where the screen's parts are set down one after the other (`SetDown.since`); 0, or
     * less, where it stands whole at once. A screen is in view once that answer is, and takes the
     * key [SEEN_MS] after that: the Enter that skipped a lesson, pressed again at once, must not
     * answer what took the lesson's place, and nothing is answered that has not been set down yet.
     * [uncovered]: for a screen that came while the system's Keyboard shortcuts dialog was over the
     * panel (the key's screens alone: "Now press your keys", "Your key works" where the key landed
     * under it), when the panel was uncovered again; 0 for a screen that came in view. Under the
     * dialog nobody saw the screen, however long it stood there: the answer is in view from the
     * later of the two, its own beginning to show and the dialog's going, and the key counts
     * [SEEN_MS] after that. It can only make a key count later.
     * Every doubt counts against the key: a press made before the screen came is not in view, and a
     * moment that is not known (a screen still covered) must be given as now, never as 0, which
     * would be long ago.
     */
    fun inView(press: Long, opened: Long, came: Long, lead: Long, uncovered: Long = 0L): Boolean =
        press - maxOf(maxOf(opened, came) + maxOf(lead, 0L), uncovered) >= SEEN_MS

    /**
     * What a screen says to a screen reader: its [parts] (where it is in the run, its title, its
     * line, its keys, how to act), one sentence after another. A part that ends in a mark of its own
     * (the question's title, "Didn't work?") keeps it, and gets no full stop after it; a part that
     * says nothing is left out.
     */
    fun sentence(parts: List<String>): String =
        parts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") { if (it.last() in ENDS) it else "$it." }

    /** What ends a sentence. */
    private const val ENDS = ".?!"

    /**
     * The screen that stands is one come back to: this run has stood in an opening before this one.
     * Its parts then arrive as rows do, and nothing of it is written again; in a run's first opening
     * a screen is set down part by part. Its limit: it is counted for the run, not for each screen,
     * and an opening is counted when its panel is made ([opening]), not when its screen was drawn.
     * So a screen comes as one come back to that nobody has seen, where the run went on to it in
     * the last opening and was left at once, or where a panel was closed before its glass had
     * opened. That costs such a screen its set-down and nothing else: when it takes a key is
     * [inView]'s to say, whichever way it comes.
     */
    fun comesBack(s: State): Boolean = screen(s) != null && s.opens > 1

    /** What stands under the caps: where the suggested key is and that any keys will do; "your keys"; the other keys to try; nothing. */
    enum class Caption { WHERE_AND_ANY, ANY, YOURS, TRY_OTHER, NONE }

    /** The caption of the screen that stands, for the key that is [suggested]. Only Quick Insert needs saying where it is; only after it is there another key to try. */
    fun caption(s: State, suggested: Key): Caption = caption(screen(s), suggested)

    /** The caption of the screen [on], whichever state it stands in. */
    fun caption(on: Screen?, suggested: Key): Caption = when (on) {
        Screen.K1 -> if (suggested == Key.QUICK_INSERT) Caption.WHERE_AND_ANY else Caption.ANY
        Screen.K2 -> Caption.YOURS
        Screen.K3 -> if (other(suggested) != null) Caption.TRY_OTHER else Caption.ANY
        else -> Caption.NONE
    }

    /** The system's own words for its Keyboard shortcuts dialog as they were read from it; null: that one could not be read. */
    data class Read(val customize: String? = null, val set: String? = null, val quick: String? = null)

    /** The words Booklight quotes: the dialog's Customize button, its Set shortcut button, and the Quick Insert key's name. */
    data class Words(val customize: String, val set: String, val quick: String)

    /** A word of the system's longer than this is not quoted: it is no button's name. */
    const val MOST_WORD = 40

    private fun usable(read: String?, ours: String): String = read?.trim()?.takeIf { it.isNotEmpty() && it.length <= MOST_WORD && '\n' !in it } ?: ours

    /** The words to quote: each of the system's own where it was read and can be a button's name, else [ours]. */
    fun words(read: Read?, ours: Words): Words = Words(usable(read?.customize, ours.customize), usable(read?.set, ours.set), usable(read?.quick, ours.quick))

    // ---- the hold, and the key landing

    /**
     * The panel kept alive behind the system's Keyboard shortcuts dialog. [ASKED]: the dialog was
     * asked for and has not come. [UNDER]: it is over the panel, which has lost the focus to it.
     */
    enum class Hold { NONE, ASKED, UNDER }

    /**
     * What the panel is told. [ASK]: Enter on "Open Keyboard shortcuts". [FOCUS_LOST], [FOCUS_BACK]:
     * its window's focus. [KEY]: a start by the user's key. [GONE]: another window has had the front
     * for longer than the key's own start takes it. [NO_DIALOG]: a second after asking.
     */
    enum class Signal { ASK, FOCUS_LOST, FOCUS_BACK, KEY, GONE, NO_DIALOG }

    /** The hold after a signal. [close]: the panel goes, as it does on any lost focus. [came]: the dialog has come over the panel, once more. */
    data class Held(val hold: Hold, val close: Boolean = false, val came: Boolean = false)

    /**
     * What a signal makes of the hold. Under the dialog the panel is told one thing, that it lost the
     * focus; so the first lost focus after asking is the dialog, and the focus coming back is the
     * dialog gone (closed by hand, or by the key landing: its start comes first, the focus after).
     * Without a hold a lost focus closes the panel, as every day.
     */
    fun hold(h: Hold, signal: Signal): Held = when (h) {
        Hold.NONE -> when (signal) {
            Signal.ASK -> Held(Hold.ASKED)
            Signal.FOCUS_LOST -> Held(Hold.NONE, close = true)
            else -> Held(Hold.NONE)
        }
        Hold.ASKED -> when (signal) {
            Signal.FOCUS_LOST -> Held(Hold.UNDER, came = true)
            Signal.NO_DIALOG -> Held(Hold.NONE)
            Signal.GONE -> Held(Hold.NONE, close = true)
            else -> Held(Hold.ASKED)
        }
        Hold.UNDER -> when (signal) {
            Signal.FOCUS_BACK -> Held(Hold.NONE)
            Signal.GONE -> Held(Hold.NONE, close = true)
            else -> Held(Hold.UNDER)
        }
    }

    /**
     * A start of the panel as the system describes it. [launcher]: the launcher activity was asked
     * for (what the icon, a keyboard shortcut and "Open" in the store all do). [assist]: the
     * assistant key. [referrer]: who the system says sent it, as a package's name; [SYSTEM] for the
     * system itself. [bounds]: it says where on screen an icon was. [extras]: it carries anything.
     * [home]: the home app's package.
     */
    data class Start(val launcher: Boolean, val assist: Boolean = false, val referrer: String? = null, val bounds: Boolean = false, val extras: Boolean = false, val home: String? = null)

    /** Who the system itself is, as a referrer. */
    const val SYSTEM = "android"

    /** A click on the app's icon: the home app sent it, or it says where the icon was. */
    fun fromIcon(s: Start): Boolean = s.launcher && (s.bounds || (s.home != null && s.referrer == s.home))

    /**
     * The user's key: the assistant key, or a start of the launcher activity by the system itself
     * (or by nobody who is named) that says nothing about an icon and carries nothing. That is what
     * a keyboard shortcut's start was seen to be; "Open" in the store, in an installer or in
     * Settings names its sender, and so does another app.
     */
    fun byKey(s: Start): Boolean = when {
        s.assist -> true
        !s.launcher || fromIcon(s) || s.extras -> false
        else -> s.referrer == null || s.referrer == SYSTEM
    }

    /**
     * Whether a click on the icon shows the panel instead of the Booklight window: once, for a new
     * installation that still asks for its key, and only where the key's step will stand in the
     * panel this click makes. So it is asked of the state as this [opening] leaves it: a click that
     * is the fourth opening would show an empty field. (A click on the icon carries nothing: its
     * panel is a plain one.) [iconClicked] uses it up.
     */
    fun iconShowsPanel(s: State, screenDp: Float): Boolean =
        !s.icon && s.run == Run.NEW &&
            stage(opening(s, screenDp, plain = true), screenDp).let { it == Screen.K1 || it == Screen.K2 || it == Screen.K3 }

    fun iconClicked(s: State): State = s.copy(icon = true)
}
