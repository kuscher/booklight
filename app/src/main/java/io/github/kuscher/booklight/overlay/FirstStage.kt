package io.github.kuscher.booklight.overlay

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.kuscher.booklight.R
import io.github.kuscher.booklight.core.Body
import io.github.kuscher.booklight.core.FirstRun
import io.github.kuscher.booklight.core.SetDown
import io.github.kuscher.booklight.data.firstRun
import io.github.kuscher.booklight.device.SystemWords
import io.github.kuscher.booklight.ui.Fonts
import io.github.kuscher.booklight.ui.Symbols
import kotlinx.coroutines.delay

/** A large cap's label, and a typed part of a recipe: the field's own type. */
private val CAP = TextStyle(fontFamily = Fonts.text, fontSize = 24.sp, fontWeight = FontWeight(500))
/** The line under the caps: the footer's type. */
private val CAPTION = TextStyle(fontFamily = Fonts.text, fontSize = 13.sp, fontWeight = FontWeight(500), letterSpacing = 0.1.sp)
private val CAP_SHAPE = RoundedCornerShape(16.dp)
/** Inside each end of a large cap. */
private val CAP_PAD = 20.dp
/** "Your key": two rows' heights wide, whatever was suggested. */
private val YOUR_KEY = 112.dp
/** The band under the seat: the caps on its centre line, the caption on the line a footer's words stand on. */
private val BAND = 112.dp
private val CAPTION_BAND = 28.dp
/** The question's disclosure: a row's small type on lines of 20, in strong ink. */
private val ASK = SMALL.copy(lineHeight = 20.sp)
/** The room the disclosure has: four lines. Where it needs more, the stage is that much taller ([AskRoom]). */
private val ASK_TEXT = 80.dp
/** The two answers under it, 32 dp each and 4 apart; the note beside them ends well before them. */
private val ASK_ANSWERS = 68.dp
private val ASK_NOTE = 442.dp
/** Where a stage's words begin, and where its answers end: the titles' edge and the panel's right margin. */
private val LEFT = 72.dp
private val RIGHT = 20.dp

/** What a screen says: in its seat, under its band, and to a screen reader. */
private class Said(
    val symbol: String, val title: String, val line: String, val caption: String,
    /** For a screen reader: the keys or the recipe as words, null for none; and how to act. */
    val keys: String?, val act: String,
)

/** How far a part of the stage rises into its place, as a row does. */
private val RISE = 12.dp

/** Where the stage's clock stands once everything is at rest: with the system's animations off it is there from the first frame. */
private const val AT_REST = 1_000_000f

/**
 * The stage's clock, in ms. It starts when the screen that stands comes into view (`OverlayModel.viewFrom`: when it
 * came, and not before the gate), runs as slowly as a debug build asks, and stops once the screen's marks have ended
 * and its last spring has let go (`Motion.SETTLES_MS` after them): it is then set at rest, so that every part stands
 * exactly in its place, as it does on a screen that comes whole.
 * Every part of the stage is drawn as a function of this clock and of its own mark (`OverlayModel.marks`, core
 * `SetDown`): nothing is kept from frame to frame, a part that has not come has no say, and a debug hook can stand the
 * clock still (`OverlayModel.stageAt`). With the system's animations off it is at rest in the first frame. It is read
 * where things are drawn, never where they are composed: a frame of it composes nothing.
 */
@Composable
private fun stageClock(model: OverlayModel): () -> Float {
    val motion = LocalMotion.current
    val came = model.cameAt
    val arrived = model.arrived
    val stands = model.stageAt
    fun at(now: Long): Float = when {
        stands != null -> stands
        !motion.on -> AT_REST
        !arrived -> 0f
        else -> (now - model.viewFrom) / motion.slow
    }
    val clock = remember(came, arrived, stands, motion.on) { mutableFloatStateOf(at(SystemClock.uptimeMillis())) }
    LaunchedEffect(clock) {
        if (stands != null || !motion.on || !arrived) return@LaunchedEffect
        // (A letter typed while it runs takes the stage away: what has not come by then never comes. The marks are asked
        // in every frame: a recipe that is worked out a moment late makes them longer, on the clock that runs. A screen
        // that stands whole has nothing to come, and no frame is spent on it.)
        while (model.stage != null && model.marks.end.let { it > 0 && clock.floatValue < it + Motion.SETTLES_MS }) clock.floatValue = at(withFrameNanos { it / 1_000_000 })
        if (model.stage != null) clock.floatValue = AT_REST
    }
    return remember(clock) { { clock.floatValue } }
}

/** How far something has come that begins at [at] ms of the stage's clock and takes [ms]: 0 to 1, on the standard curve. A part whose mark is `SetDown.THERE` is there. */
private fun come(t: Float, at: Int, ms: Int): Float = if (at == SetDown.THERE) 1f else FastOutSlowInEasing.transform(((t - at) / ms).coerceIn(0f, 1f))

/** How far below its place something still is that rises there from [at] on, [rise] px on `place`. */
private fun below(t: Float, at: Int, rise: Float): Float = if (at == SetDown.THERE) 0f else (1f - PLACE.at(t - at)) * rise

/** A part that rises into its place as a row does: 12 dp up on `place`, fading in over 140 ms (motion.md §3, "rises"). Drawn so; its place in the layout never changes. */
private fun Modifier.rises(now: () -> Float, at: Int, rise: Float): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.RISE_MS)
    translationY = below(t, at, rise)
}

/** A part that fades in where it stands, over [ms]. */
private fun Modifier.fadesIn(now: () -> Float, at: Int, ms: Int = Motion.RISE_MS): Modifier = graphicsLayer { alpha = come(now(), at, ms) }

/**
 * A cap that comes up: it fades in over 110 ms and grows from 0.96 of its size on `pop`, about its centre. The "up" half
 * of a key that is pressed: every cap on the stage has moved the way the user's key will (motion.md §4). [down]: how
 * far it is held down just now, 0 to 1 (a little under 0 as it springs back up): "your key", as the user's key lands.
 */
private fun Modifier.comesUp(now: () -> Float, at: Int, down: () -> Float = { 0f }): Modifier = graphicsLayer {
    val t = now()
    alpha = come(t, at, Motion.CAP_MS)
    val size = (if (at == SetDown.THERE) 1f else Motion.CAP_SMALL + (1f - Motion.CAP_SMALL) * POP.at(t - at)) - (1f - Motion.CAP_SMALL) * down()
    scaleX = size; scaleY = size
}

/**
 * What one content of a transition is drawn by: the stage's clock and marks for as long as it is the content that
 * stands, and those of its last moment once it gives way to another. So what goes fades as it stood: a part of it that
 * had not come never comes, and nothing of it follows the clock of the screen that takes its place.
 */
private class Own(val now: () -> Float, val marks: SetDown.Marks)

@Composable
private fun own(now: () -> Float, marks: SetDown.Marks, stands: Boolean): Own {
    val last = remember { floatArrayOf(0f) }
    val kept = remember { arrayOf(marks) }
    val live = rememberUpdatedState(stands)
    val clock = rememberUpdatedState(now)
    if (stands) kept[0] = marks
    val mine = remember { { if (live.value) clock.value().also { last[0] = it } else last[0] } }
    return Own(mine, kept[0])
}

/**
 * [value], a moment late where it changes: so that words which change together roll one after the other (the title,
 * then its line, then the counter: motion.md T5). With no wait, and with the system's animations off, it is simply [value].
 */
@Composable
private fun <T> late(value: T, ms: Int): T {
    val motion = LocalMotion.current
    var shown by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (shown != value) { if (ms > 0) delay(motion.held(ms.toLong())); shown = value } }
    return if (ms <= 0 || !motion.on) value else shown
}

/**
 * [value] for as long as what draws it [stands], and after that the value of its last standing moment: what gives way
 * is drawn as it stood (the arming of answers that fade, the counter of a stage that goes), as [own] has it for the
 * clock and the marks.
 */
@Composable
private fun <T> kept(value: T, stands: Boolean): T {
    val last = remember { arrayOfNulls<Any?>(1).also { it[0] = value } }
    if (stands) last[0] = value
    @Suppress("UNCHECKED_CAST")
    return last[0] as T
}

/**
 * How far each answer shows as pressed, for `OptionStrip` to draw: the answer that was just given ([press]: given on
 * these very answers; null for none) has its slot inked in the frame of the press, in full, and let go over
 * `Motion.RELEASE_MS`. There is no way in: the answer is run in the call that presses it, so on most screens the
 * answers are on their way out by the next frame, and a press that eased in would never be seen (an answer is held up
 * for nothing, not even for this). It is the ink of the answers it was given on: those that give way carry it while
 * they fade, at the strength they stood at (an answer is only ever given once it has been in view, whole), and the
 * answers that take their place never show it. Nothing with the system's animations off.
 *
 * The ink is reckoned where it is drawn, from when the press was given: the frame that first draws after the press
 * draws it whole. (Set by an effect it would come a frame later, an effect beginning only once its frame is drawn.)
 */
@Composable
private fun pressedInk(press: OverlayModel.Pressed?): (Int) -> Float {
    val motion = LocalMotion.current
    // (A press given before these answers were first drawn is not theirs to show: a stage that is back after something was
    // typed over it does not show an old press again.)
    val before = remember { press }
    val mine = press?.takeIf { it !== before && motion.on }
    // The frames' own time while the ink lets go, by the clock the press was timed by; read where the ink is drawn.
    val frame = remember { mutableLongStateOf(0L) }
    LaunchedEffect(mine) {
        if (mine == null) return@LaunchedEffect
        val span = motion.held(Motion.RELEASE_MS.toLong())
        while (frame.longValue - mine.at < span) frame.longValue = withFrameNanos { it / 1_000_000 }
    }
    return remember(mine) {
        { k ->
            if (mine == null || k != mine.slot) 0f
            else 1f - FastOutSlowInEasing.transform(((frame.longValue - mine.at).coerceAtLeast(0L) / (Motion.RELEASE_MS * motion.slow)).coerceIn(0f, 1f))
        }
    }
}

/** What stands under the seat, with everything it draws: where the question takes a lesson's place, the lesson's band still draws what it showed while it fades. */
private data class Under(
    val on: FirstRun.Screen, val parts: List<FirstRun.Part>, val caption: String, val text: String, val note: String,
    val labels: List<String>, val answers: List<FirstRun.Answer>,
) {
    val ask: Boolean get() = on.step == FirstRun.Step.ASK
}

/** The answers, as one thing that gives way to other answers. */
private data class Answers(val labels: List<String>, val answers: List<FirstRun.Answer>)

/** What a band shows: the suggested keys as two large caps, "your key", or a lesson's recipe. */
private sealed interface Picture {
    data class Caps(val action: String, val name: String) : Picture
    data object Yours : Picture
    data class Recipe(val parts: List<FirstRun.Part>) : Picture
}

/**
 * First run's stage under the empty field (docs/design/first-run/design.md §4 to §7): a seat like a row's (a mark, a
 * title and a line, the counter where a row's kind stands), and under it a band. For the key's step the band holds the
 * keys as large caps; for a lesson, its recipe: what to type and which keys to press; in both, one line of caption and
 * the answers at the right end. The question's band is its disclosure, whole, with a note and two answers under it. One
 * skeleton for every screen: the words and the band change where they stand. It is not a row: nothing of it can be
 * selected, and the field above it is live.
 *
 * It is not there in one frame (motion.md §3): a screen is set down part by part, comes back as rows do, or takes
 * another's place where that stood. When each part comes is the model's to say (`OverlayModel.marks`, core `SetDown`,
 * with `Motion.kt`'s times); here every part is drawn by its mark on one clock ([stageClock]), as a layer over a layout
 * that never changes: sizes are known before anything moves, and the place of the answers is the same in every frame.
 */
@Composable
fun FirstStage(model: OverlayModel, on: FirstRun.Screen, onAnswer: (FirstRun.Answer) -> Unit) {
    val motion = LocalMotion.current
    val state = model.settings.firstRun()
    val answers = FirstRun.answers(state)
    val labels = answers.map {
        stringResource(when (it) {
            FirstRun.Answer.OPEN_HELPER -> R.string.card_shortcut_action
            FirstRun.Answer.GO_ON -> R.string.first_next
            FirstRun.Answer.CHANGE_KEY -> R.string.first_key_change
            FirstRun.Answer.SKIP -> R.string.first_skip
            FirstRun.Answer.AGREE -> R.string.first_agree
            else -> R.string.card_not_now
        })
    }
    val count = FirstRun.count(state)
    val counter = count?.let { stringResource(R.string.first_step, it.first, it.second) }
    // A lesson's recipe is this device's: until its example is worked out, lessons 2 and 3 show none.
    val parts = model.recipe(state)
    val said = when (on.step) {
        FirstRun.Step.KEY -> keySaid(model, on, state)
        FirstRun.Step.ASK -> askSaid()
        else -> lessonSaid(on, parts)
    }
    val text = if (on == FirstRun.Screen.Q) stringResource(R.string.first_suggest_text, model.settings.engine().name) else ""
    val note = if (on == FirstRun.Screen.Q) stringResource(R.string.first_suggest_note) else ""

    // A screen reader is told each screen once, when it comes: where it is in the run, what it says, the keys, and how to act.
    // (Once for each coming: the model keeps what was said, so a stage that is only back after something was typed over
    // it and deleted is not said again, and neither is the greeting.)
    // (One sentence after another, core `FirstRun.sentence`: a part that ends in a mark of its own, as the question's title
    // does, gets no full stop after it.)
    val told = listOfNotNull(counter, said.title, said.line, text, note, said.keys, said.caption, said.act)
    // Where first run's opening piece is this opening's and is not played, its title is said once, before the first screen's own words.
    val hello = stringResource(R.string.first_hello_title)
    val cameAt = model.cameAt
    LaunchedEffect(on, cameAt) {
        if (model.stage == on && model.says(on, recipe = said.keys != null)) model.tell(FirstRun.sentence(if (model.greeting()) listOf(hello) + told else told))
    }
    // A lesson's recipe is this device's, and can be worked out a moment after the lesson was said: it is said then, by itself.
    LaunchedEffect(on, said.keys) { said.keys?.let { if (model.stage == on && model.saysRecipe(on)) model.tell(it) } }

    val armed = model.stageArmed
    val density = LocalDensity.current
    // The clock and the marks this stage is drawn by. Once it is no longer the stage that stands (a letter was typed over
    // it; the choices took the question's place) and is only fading where it stood, they are those of its last moment.
    val standing = model.stage != null
    val whole = own(stageClock(model), model.marks, stands = standing)
    val now = whole.now
    val marks = whole.marks
    // The screen took another's place where that stood: its seat stays, and its words roll.
    val turn = model.comes == SetDown.Comes.TURN
    val rise = with(density) { RISE.toPx() }
    // The answers, at the band's right end. Where a screen has other answers than the one before it, the old ones fade
    // where they stand and the new ones come by their mark; answers that stay (a lesson's Skip) do not move.
    val strip: @Composable (Under, Own) -> Unit = { under, outer ->
        AnimatedContent(Answers(under.labels, under.answers),
            // (Their room is the new answers' own from the first frame, and nothing is cut by it: answers that fade stand over
            // the band's free end meanwhile, and nothing beside them moves a second time once they have gone.)
            transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(SizeTransform(clip = false) { _, _ -> snap() }) },
            contentAlignment = Alignment.CenterEnd, label = "answers") { a ->
            val stands = standing && transition.targetState == EnterExitState.Visible
            val o = own(outer.now, outer.marks, stands)
            val m = o.marks
            // Answers that give way keep the arming of their last moment: their highlight fades where it stood, and does not
            // set off for the answer the next screen arms.
            val chosen = kept(armed, stands)
            // (The answer that was just given shows as pressed, on the answers it was given on: also while they fade for it.)
            val pressed = pressedInk(model.stagePress?.takeIf { it.answers == a.answers })
            // (They rise as they come, the armed one's highlight with them. Not at the landing of the opening piece: there
            // the highlight is on its way to where the armed answer stands, and nothing of them moves.)
            Box(Modifier.graphicsLayer { if (m.rises) translationY = below(o.now(), m.answers, rise) }) {
                OptionStrip(a.labels, chosen.coerceIn(0, (a.labels.size - 1).coerceAtLeast(0)), onChoose = { model.stageArm(it) },
                    // (A click counts from the moment the keys do: "Agree" is consent, by whichever way it is given. And only on
                    // answers that stand: ones that are fading, under a typed letter or for the next screen's, take none.)
                    onRun = { if (stands && model.stage == under.on && model.seen) { model.stageArm(it); a.answers.getOrNull(it)?.let(onAnswer) } },
                    // First run's show hands its one highlight over to the armed answer (`Glide`): while it is on its way the answers
                    // say where the armed one stands (slots of 32 dp, 4 apart), and are not lit themselves.
                    modifier = Modifier.onGloballyPositioned { c ->
                        if (model.gliding) with(density) {
                            val at = c.positionInRoot()
                            val top = at.y + chosen.coerceAtLeast(0) * 36.dp.toPx()
                            model.answerAt = floatArrayOf(at.x, top, at.x + c.size.width, top + 32.dp.toPx())
                        }
                    },
                    vertical = true, lit = chosen >= 0 && !model.gliding,
                    shown = { k -> come(o.now(), if (k == chosen) m.armed else m.answers, if (m.rises) Motion.RISE_MS else Motion.CAP_MS) },
                    pressed = pressed)
            }
        }
    }
    Column(Modifier.fillMaxWidth().height(Metrics.stage(on, model.askMore)).padding(vertical = Metrics.pad)) {
        // (A stage that goes keeps the counter it had: the screen that takes its place may have none.)
        Seat(said.symbol, said.title, said.line, kept(counter, standing), now, marks, turn, rise)
        // Under the seat: the question's disclosure and its answers, or a band. Where the one takes the other's place (Skip
        // on the sum's lesson), what stood fades where it stands and what comes is set down by its marks.
        AnimatedContent(Under(on, parts, said.caption, text, note, labels, answers), contentKey = { it.ask },
            transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) }, contentAlignment = Alignment.TopStart, label = "under") { under ->
            val o = own(now, marks, transition.targetState == EnterExitState.Visible)
            if (under.ask) AskBand(under, model.askMore, armed, o, rise) { strip(under, it) }
            else Band(model, under, armed, o) { strip(under, it) }
        }
    }
}

/** The key's step: four screens. Only here are the system's own words and the suggested key read. */
@Composable
private fun keySaid(model: OverlayModel, on: FirstRun.Screen, state: FirstRun.State): Said {
    val context = LocalContext.current
    val words = SystemWords.words(context)
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val title = stringResource(when (on) {
        FirstRun.Screen.K2 -> R.string.first_key_press
        FirstRun.Screen.K3 -> R.string.first_key_retry
        FirstRun.Screen.K4 -> R.string.key_works
        else -> R.string.card_shortcut_title
    })
    val line = when (on) {
        FirstRun.Screen.K2 -> stringResource(R.string.first_key_press_text)
        FirstRun.Screen.K3 -> stringResource(R.string.first_key_retry_text, words.customize)
        FirstRun.Screen.K4 -> stringResource(R.string.first_key_works_text)
        else -> stringResource(R.string.first_key_text)
    }
    val caption = when (FirstRun.caption(state, key)) {
        FirstRun.Caption.WHERE_AND_ANY -> stringResource(R.string.first_key_where, name) + " · " + stringResource(R.string.first_key_any)
        FirstRun.Caption.ANY -> stringResource(R.string.first_key_any)
        FirstRun.Caption.YOURS -> stringResource(R.string.first_key_yours)
        FirstRun.Caption.TRY_OTHER -> stringResource(R.string.first_key_try, stringResource(R.string.first_key_letter))
        FirstRun.Caption.NONE -> ""
    }
    val keys = stringResource(R.string.a11y_first_keys, stringResource(R.string.key_action), name)
    val act = stringResource(when (on) {
        FirstRun.Screen.K2 -> R.string.a11y_first_tab_opens
        // (Asked for again, "Your key works" has a second answer.)
        FirstRun.Screen.K4 -> if (FirstRun.answers(state).size > 1) R.string.a11y_first_goes_on_change else R.string.a11y_first_goes_on
        else -> R.string.a11y_first_opens
    })
    return Said("key", title, line, caption, keys.takeIf { on == FirstRun.Screen.K1 }, act)
}

/** A lesson: its mark is what it teaches, its caption says what other days are like. [parts]: its recipe, said to a screen reader a piece at a time. */
@Composable
private fun lessonSaid(on: FirstRun.Screen, parts: List<FirstRun.Part>): Said {
    val tab = stringResource(R.string.first_key_tab)
    val enter = stringResource(R.string.first_key_enter)
    // (An app's first letters are spelled, so that a reader does not make a word of them; a sum is said as it is; keys go by their names.)
    val recipe = parts.joinToString(", ") {
        when (it) {
            is FirstRun.Part.Typed -> if (it.text.all(Char::isLetter)) it.text.toList().joinToString(" ") else it.text
            FirstRun.Part.Key.TAB -> tab
            FirstRun.Part.Key.ENTER -> enter
        }
    }
    val keys = if (parts.isEmpty()) null else stringResource(R.string.a11y_first_example, recipe)
    val act = stringResource(R.string.a11y_first_skips)
    return when (on) {
        FirstRun.Screen.L3 -> Said("search", stringResource(R.string.first_search_title), stringResource(R.string.first_search_text), stringResource(R.string.first_practice), keys, act)
        FirstRun.Screen.L4 -> Said("calc", stringResource(R.string.first_sum_title), stringResource(R.string.first_sum_text), stringResource(R.string.first_enter_stays), keys, act)
        else -> Said("open", stringResource(R.string.first_open_title), stringResource(R.string.first_open_text), stringResource(R.string.first_practice), keys, act)
    }
}

/** The question: its title alone in the seat. Its text and its note are the band's. */
@Composable
private fun askSaid(): Said = Said("globe", stringResource(R.string.first_suggest_title), "", "", null, stringResource(R.string.a11y_first_asks))

/**
 * The seat: where a row would be. A disc with the step's mark, the title and one line under it (the question has the
 * title alone), and the counter where a row's kind stands. Where a screen is set down its three parts rise one after
 * the other ([marks]); where it takes another's place ([turn]) they stay, the mark's symbol gives way to the new one,
 * and the words roll as a counter's do: the title, the line 22 ms later, the counter's 44.
 */
@Composable
private fun Seat(symbol: String, title: String, line: String, counter: String?, now: () -> Float, marks: SetDown.Marks, turn: Boolean, rise: Float) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val dark = LocalDark.current
    val second = scheme.onSurface.copy(alpha = SECOND)
    val lineLate = late(line, if (turn) Motion.PART_MS else 0)
    // (A screen without a line under its title, the question, has none from its first frame: its title stands alone on the seat's centre line.)
    val lineNow = if (line.isEmpty()) "" else lineLate
    val counterNow = late(counter, if (turn) 2 * Motion.PART_MS else 0) ?: counter
    Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(Metrics.row), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.rises(now, marks.disc, rise).size(36.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = if (dark) 0.12f else 0.08f)), contentAlignment = Alignment.Center) {
            AnimatedContent(symbol, transitionSpec = { ((scaleIn(motion.pop(), 0.6f) + fadeIn(motion.fade(110))) togetherWith (scaleOut(motion.fade(Motion.PRESS_MS), 0.6f) + fadeOut(motion.fade(Motion.PRESS_MS)))).using(null) }, contentAlignment = Alignment.Center, label = "mark") {
                Icon(Symbols.of(it), null, Modifier.size(20.dp), tint = second)
            }
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp).rises(now, marks.words, rise)) {
            AnimatedContent(title, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "title") {
                Text(it, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
            }
            if (lineNow.isNotEmpty()) AnimatedContent(lineNow, transitionSpec = { motion.roll() }, contentAlignment = Alignment.TopStart, label = "line") {
                Text(it, color = second, style = SMALL, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadeEnd())
            }
        }
        if (counterNow != null) AnimatedContent(counterNow, Modifier.rises(now, marks.counter, rise), transitionSpec = { motion.roll() }, contentAlignment = Alignment.CenterEnd, label = "counter") {
            Text(it, color = second, style = SMALL, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * The band under the seat, for the key's step and for a lesson: what to press, from the titles' edge on the band's
 * centre line ([Keys]); the caption under it, on the line a footer's words stand on; the answers at the right end, on
 * the same centre line. Each comes by its mark ([o]): the keys a beat apart or the recipe written, then the caption,
 * then the answers. Where the screen took another's place, a caption that says the same does not move (its mark says
 * it is there), and one that says something else comes once the band has been written, while the old one fades.
 */
@Composable
private fun Band(model: OverlayModel, under: Under, armed: Int, o: Own, answers: @Composable (Own) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val second = scheme.onSurface.copy(alpha = SECOND)
    val now = o.now
    val marks = o.marks
    Row(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth().height(BAND), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            Keys(model, under, o)
            // Nothing is armed until Tab (on "Now press your keys", in a lesson): a `tab` cap says so, 10 dp before the answers, and goes once one is.
            // (It lies over the band's free end and takes no room: the caps have all of it. It comes with the answers.)
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.align(Alignment.CenterEnd).offset(x = 14.dp).graphicsLayer { alpha = cap * come(now(), marks.answers, Motion.RISE_MS) }) { Keycap("tab") }
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(CAPTION_BAND), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(under.caption, transitionSpec = { (EnterTransition.None togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) }, contentAlignment = Alignment.CenterStart, label = "caption") {
                    val mine = own(now, marks, transition.targetState == EnterExitState.Visible)
                    Text(it, color = second, style = CAPTION, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, modifier = Modifier.fillMaxWidth().fadesIn(mine.now, mine.marks.caption).fadeEnd())
                }
            }
        }
        Spacer(Modifier.width(24.dp))
        answers(o)
    }
}

/**
 * What the band shows, from the titles' edge on its centre line. The key's step: on its first screen the suggested
 * keys, Action and the key beside it, as two large caps that come up a beat apart; on the others "your key", one blank
 * cap that takes the check once the key works. A lesson: its recipe, what to type in the field's own type as it will
 * stand in the field, written a letter at a time, and the keys to press as large caps (Enter is the drawn mark); 12 dp
 * between two caps, 16 between a cap and letters. A picture: it takes no pointer and no key. Where one picture takes
 * another's place the old one fades where it stands and the new one comes by its marks.
 */
@Composable
private fun BoxWithConstraintsScope.Keys(model: OverlayModel, under: Under, o: Own) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val context = LocalContext.current
    val second = scheme.onSurface.copy(alpha = SECOND)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val room = with(density) { maxWidth.toPx() }
    val key = model.suggested
    val name = SystemWords.name(context, key)
    val action = stringResource(R.string.key_action)
    // (Its stand-in on the cap is a word known to fit, the same in every language: the caption and the rows keep the name.)
    val fallback = stringResource(if (key == FirstRun.Key.QUICK_INSERT) R.string.first_key_quick_cap else R.string.first_key_letter)
    // The system's name for the key is used on the cap only where the cap then has room: a cap never shrinks or wraps.
    val fits = remember(action, name, fallback, room) {
        fun wide(s: String) = measurer.measure(s, CAP, maxLines = 1, softWrap = false).size.width
        val fixed = with(density) { (CAP_PAD * 4 + PLUS).toPx() }
        if (wide(action) + wide(name) + fixed <= room) name else fallback
    }
    val picture: Picture = when {
        under.on.step != FirstRun.Step.KEY -> Picture.Recipe(under.parts)
        under.on == FirstRun.Screen.K1 -> Picture.Caps(action, fits)
        else -> Picture.Yours
    }
    AnimatedContent(picture, Modifier.align(Alignment.CenterStart).clearAndSetSemantics { },
        // (A recipe that was no part of what was set down, because this device's example was worked out a moment late, fades in where it belongs.)
        transitionSpec = { ((if (o.marks.pieces.isEmpty()) fadeIn(motion.fade(Motion.CAP_MS, 40)) else EnterTransition.None) togetherWith fadeOut(motion.fade(Motion.PRESS_MS))).using(null) },
        contentAlignment = Alignment.CenterStart, label = "keys") { what ->
        val mine = own(o.now, o.marks, transition.targetState == EnterExitState.Visible)
        val now = mine.now
        val m = mine.marks
        when (what) {
            is Picture.Caps -> Row(verticalAlignment = Alignment.CenterVertically) {
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0))) { Text(what.action, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
                Text("+", color = second, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight(500)), modifier = Modifier.width(PLUS).fadesIn(now, SetDown.piece(m, 1), Motion.CAP_MS), textAlign = TextAlign.Center)
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 2))) { Text(what.name, color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
            }
            // "Your key": blank, because Booklight cannot know the keys; the one check Booklight has is drawn in it once it works,
            // a moment after the cap has come up. As the user's key lands it is held down as theirs is (0.96 of its size, a
            // little ink), comes up on `pop`, and only then takes its check (motion.md §4, T3 and T4): under the system's
            // dialog it is down at once, so that the first frame that shows the panel shows it down.
            Picture.Yours -> {
                val checked by remember(now, m) { derivedStateOf { now() >= SetDown.piece(m, 0) + Motion.CHECK_AFTER_MS } }
                val held = model.keyDown
                val goes = if (model.keyUncovered) 0 else Motion.PRESS_MS
                val sunk by animateFloatAsState(if (held) 1f else 0f, if (held) motion.fade(goes) else motion.pop(), label = "key")
                val ink by animateFloatAsState(if (held) 1f else 0f, motion.fade(if (held) goes else Motion.RELEASE_MS), label = "ink")
                BigCap(Modifier.comesUp(now, SetDown.piece(m, 0), down = { sunk }).width(YOUR_KEY), pressed = { ink }) {
                    DrawnCheck(model.stage == FirstRun.Screen.K4 && checked && model.keyChecked, scheme.onSurface, Modifier.size(28.dp), stroke = 2.5.dp)
                }
            }
            is Picture.Recipe -> Row(Modifier.fitsWidth(RECIPE_END), verticalAlignment = Alignment.CenterVertically) {
                what.parts.forEachIndexed { i, part ->
                    if (i > 0) Spacer(Modifier.width(if (part is FirstRun.Part.Key && what.parts[i - 1] is FirstRun.Part.Key) 12.dp else 16.dp))
                    when (part) {
                        is FirstRun.Part.Typed -> Written(part.text, now, m, i, scheme.onSurface)
                        FirstRun.Part.Key.TAB -> BigCap(Modifier.comesUp(now, SetDown.piece(m, i))) { Text(stringResource(R.string.first_key_tab), color = scheme.onSurface, style = CAP, maxLines = 1, softWrap = false) }
                        FirstRun.Part.Key.ENTER -> BigCap(Modifier.comesUp(now, SetDown.piece(m, i))) { Icon(Symbols.enter, null, Modifier.size(24.dp), tint = scheme.onSurface) }
                    }
                }
            }
        }
    }
}

/** A recipe ends this far before its room does: the `tab` cap lies over the band's free end. */
private val RECIPE_END = 16.dp

/**
 * What is wider than its room is drawn smaller, as a whole, from its left end and about its own centre line: a recipe
 * is one line, and is never cut, never wrapped and never laid over the answers (a larger type size; lesson 3 in German
 * is the tight case). What fits is drawn as it is. [end]: how much of the room's end is kept free.
 */
private fun Modifier.fitsWidth(end: Dp): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
    val room = if (constraints.hasBoundedWidth) (constraints.maxWidth - end.roundToPx()).coerceAtLeast(1) else p.width
    val small = if (p.width > room) room.toFloat() / p.width else 1f
    layout(minOf(p.width, room), p.height) {
        p.placeWithLayer(0, 0) { scaleX = small; scaleY = small; transformOrigin = TransformOrigin(0f, 0.5f) }
    }
}

/**
 * A typed part of a recipe, in the field's own type: piece [k] of the band whose marks are [m]. It is laid out whole,
 * once: its width never changes. Where the recipe is written its letters are cut in one after the other, 40 ms apart,
 * each when core's schedule says it comes (`SetDown.letter`; motion.md §3, "written": in the stage, never in the
 * field), each a whole character; where the screen comes back as a row does they fade in together, with their piece.
 */
@Composable
private fun Written(text: String, now: () -> Float, m: SetDown.Marks, k: Int, ink: Color) {
    // Where each of its characters ends, as a reader counts them (core `FirstRun.ends`, which the model's marks count by
    // too): it is written a whole character at a time, never half of one, never a flag's first half or a letter without its mark.
    val ends = remember(text) { FirstRun.ends(text) }
    val n = ends.size
    val at = SetDown.piece(m, k)
    val written = m.written
    val shown by remember(text, m, k, now) {
        derivedStateOf {
            if (at == SetDown.THERE || !written) n
            else now().let { t -> (0 until n).count { i -> t >= SetDown.letter(m, PACE, k, i) } }
        }
    }
    val cut = if (shown <= 0) 0 else ends[shown - 1]
    Text(
        if (cut >= text.length) AnnotatedString(text) else buildAnnotatedString { append(text); addStyle(SpanStyle(color = Color.Transparent), cut, text.length) },
        color = ink, style = CAP, maxLines = 1, softWrap = false,
        modifier = if (written) Modifier else Modifier.fadesIn(now, at, Motion.CAP_MS),
    )
}

/**
 * The question's band (design.md §7): the disclosure, whole and in strong ink, from the titles' edge to the right
 * margin; under it a note, and at the right end the two answers. Neither is lit until Tab or the pointer has chosen
 * one, so Enter alone gives no answer: a `tab` cap says how to get to them. [more]: what the text needs beyond its four
 * lines; the stage is that much taller, and the text is never cut. Its parts rise in reading order: the text, the
 * note, then the answers (motion.md T8).
 */
@Composable
private fun AskBand(under: Under, more: Dp, armed: Int, o: Own, rise: Float, answers: @Composable (Own) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val now = o.now
    val marks = o.marks
    Column(Modifier.padding(start = LEFT, end = RIGHT).fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(ASK_TEXT + more).rises(now, SetDown.piece(marks, 0), rise)) { Text(under.text, color = scheme.onSurface, style = ASK) }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth().height(ASK_ANSWERS), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).rises(now, SetDown.piece(marks, 1), rise)) {
                Text(under.note, color = scheme.onSurface.copy(alpha = SECOND), style = SMALL, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = ASK_NOTE))
            }
            val cap by animateFloatAsState(if (armed < 0) 1f else 0f, motion.fade(120), label = "cap")
            Box(Modifier.graphicsLayer { alpha = cap * come(now(), marks.answers, Motion.RISE_MS) }) { Keycap("tab") }
            Spacer(Modifier.width(10.dp))
            answers(o)
        }
    }
}

/**
 * The room the question's disclosure needs beyond its four lines, told to the model as soon as the question is due,
 * before its screen stands: the panel's height is then known before anything moves, and the text is never cut (a long
 * name of a search engine, a large type size). It draws nothing. [width]: the panel's.
 */
@Composable
fun AskRoom(model: OverlayModel, width: Dp) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text = stringResource(R.string.first_suggest_text, model.settings.engine().name)
    // Measured with [ASK], the whole style [AskBand]'s `Text` is given: a style handed to Material's `Text` takes the
    // place of the theme's own and is not laid over it, so the theme's style here would measure another text.
    val more = remember(text, width, density) {
        with(density) {
            val high = measurer.measure(text, ASK, constraints = Constraints(maxWidth = (width - LEFT - RIGHT).roundToPx())).size.height
            (high.toDp() - ASK_TEXT).coerceAtLeast(0.dp)
        }
    }
    SideEffect { model.askRoom(more) }
}

/** The coach line as it stands: where the lesson is in its run, the key's cap (none before the lesson's title), and the words. */
private data class Coached(val counter: String?, val cap: String, val words: String)

/**
 * First run's coach line, at the footer's left end while a lesson's list is typed (design.md §6): where the lesson is
 * in its run, one key cap, and what that key does today. Where the selected row is not the lesson's own, the lesson's
 * title stands there. Its words change where they stand, in a fade; where the line has no room, the counter goes first.
 * [shown]: false while a word that just happened ("Copied") has the seat. Where no lesson stands it has no line, and
 * draws nothing: it is kept in the footer for the frames in which its last line fades.
 */
@Composable
fun CoachLine(model: OverlayModel, shown: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val motion = LocalMotion.current
    val ink = scheme.onSurface.copy(alpha = SECOND)
    val counter = FirstRun.count(model.settings.firstRun())?.let { stringResource(R.string.first_step, it.first, it.second) }
    val title = stringResource(when (model.lesson) {
        FirstRun.Screen.L3 -> R.string.first_search_title
        FirstRun.Screen.L4 -> R.string.first_sum_title
        else -> R.string.first_open_title
    })
    // The key, as the footer's caps name it, and what it does.
    val said: Pair<String, String>? = when (model.coach.takeIf { shown }) {
        FirstRun.Coach.OPENS -> "⏎" to stringResource(R.string.first_coach_open)
        FirstRun.Coach.TO_SEARCH -> "tab" to stringResource(R.string.first_coach_tab)
        FirstRun.Coach.INTO_APP -> "⏎" to stringResource(R.string.first_coach_into)
        FirstRun.Coach.SEARCHES -> "⏎" to stringResource(R.string.first_coach_search)
        FirstRun.Coach.COPIES -> "⏎" to stringResource(R.string.first_coach_sum)
        FirstRun.Coach.TITLE -> "" to title
        null -> null
    }
    // (With the counter it stood with: a line that fades keeps it, though the run has gone on by then.)
    val line = said?.let { Coached(counter, it.first, it.second) }
    // The footer is told whether the line has the seat: a word that comes into it waits for the line's fade (`Footer`).
    SideEffect { model.coachUp = line != null }
    DisposableEffect(Unit) { onDispose { model.coachUp = false } }
    val enter = stringResource(R.string.first_key_enter)
    val tab = stringResource(R.string.first_key_tab)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // (Where it takes the seat of a word that just happened, or comes with a new list, it waits for what stood there to
    // fade: the two are never drawn over each other. Its own words change where they stand, 80 out and 120 in.)
    AnimatedContent(line, transitionSpec = { (fadeIn(motion.fade(120, if (initialState == null) Motion.COACH_AFTER_MS else 0)) togetherWith fadeOut(motion.fade(80))).using(null) }, contentAlignment = Alignment.CenterStart, label = "coach") { coached ->
        if (coached != null) BoxWithConstraints(Modifier.fillMaxWidth()) {
            val (counter, cap, words) = coached
            val before = counter?.let { "$it · " }.orEmpty()
            // The counter stands before the cap only where all of the line then fits.
            val room = with(density) { maxWidth.toPx() }
            val whole = remember(before, cap, words, room) {
                fun wide(s: String) = measurer.measure(s, CAPTION, maxLines = 1, softWrap = false).size.width
                val key = if (cap.isEmpty()) 0f else with(density) { (if (cap == "⏎") 27.dp else 14.dp).toPx() + (if (cap == "⏎") 0 else wide(cap)) + 12.dp.toPx() }
                wide(before) + key + wide(words) <= room
            }
            // A screen reader is told the line when it changes, the key by its name.
            val told = listOfNotNull(counter, (if (cap == "⏎") enter else if (cap.isEmpty()) null else tab)?.let { "$it $words" } ?: words).joinToString(". ")
            Row(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite; contentDescription = told }, verticalAlignment = Alignment.CenterVertically) {
                if (whole && before.isNotEmpty()) Text(before, color = ink, style = CAPTION, maxLines = 1, softWrap = false)
                if (cap.isNotEmpty()) { Keycap(cap); Spacer(Modifier.width(6.dp)) }
                Text(words, color = ink, style = CAPTION, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * First run's choices arrive as a list with nothing selected, and its ending is the bare field with a placeholder; so
 * is the opening in which the steps began to wait in the Booklight window. A screen reader is told of each once
 * (design.md §10). It draws nothing.
 */
@Composable
fun ChoicesSaid(model: OverlayModel) {
    val rows = model.results.takeIf { model.choicesUp }.orEmpty().joinToString(". ") { r -> listOfNotNull(r.title, (r.body as? Body.Switch)?.word).joinToString(", ") }
    val choices = stringResource(R.string.a11y_first_choices)
    val done = stringResource(R.string.a11y_first_done)
    val later = stringResource(R.string.a11y_first_later)
    LaunchedEffect(model.choicesUp, model.ended) {
        if (model.choicesUp) model.tell("$rows. $choices") else if (model.ended) model.tell(done)
    }
    LaunchedEffect(model.later) { if (model.later) model.tell(later) }
}

/** The "+" of a chord, with 12 dp of air on either side. */
private val PLUS = 34.dp

/**
 * A key as large as a row: flat, the key cap's fill, a white ring inside its edge as the scope's chip has. A
 * picture, not a button: it takes no pointer and no key. [pressed]: how far it is pressed, 0 to 1: its fill gains
 * ink 0.08, the design system's pressed token, and no colour.
 */
@Composable
private fun BigCap(modifier: Modifier = Modifier, pressed: () -> Float = { 0f }, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDark.current
    val ink = scheme.onSurface
    Box(
        modifier.height(Metrics.row).defaultMinSize(minWidth = Metrics.row).clip(CAP_SHAPE)
            .background(scheme.onSurface.copy(alpha = if (dark) 0.14f else 0.10f))
            .drawBehind { pressed().let { if (it > 0f) drawRect(ink.copy(alpha = 0.08f * it.coerceAtMost(1f))) } }
            .border(with(LocalDensity.current) { 1f.toDp() }, Color.White.copy(alpha = if (dark) 0.30f else 0.55f), CAP_SHAPE)
            .padding(horizontal = CAP_PAD),
        contentAlignment = Alignment.Center,
    ) { content() }
}
