*Proposed, shown to Alex on 2 October 2026 as a page he could try (`rubber-highlight.html` beside this file, published at https://claude.ai/artifact/VYpvsnoFJdiQBFK7LW8eht), reviewed twice by a visual designer, and approved by him ("I agree with A for the rubber band"). **A is built** for the list's highlight and the grid's square (`overlay/Rows.kt`: `Band`, `Edge`, `Pill`; `Motion.kt`: `pillLead`, `pillTrail`, `pillHold`; design-system §15). On the test Googlebook (120 Hz, 1.5 px per dp, debug build) the logged edges of a one-row step matched the table below to the pixel (the old edge stands for five frames, the pill is 136 px at its longest, the lower edge is one pixel past for two frames); a held key through seven rows kept the pill between 83 and 109 px; the release build drew 152 frames of ten moves with three late ones. B was not built.*

# The list's highlight, more like rubber

*Motion designer's proposal, 2 October 2026, second version after the visual review. Nothing is built: the
repo (branch `next-five`) was only read. The page to feel it on: `prototype.html` beside this file.*

Alex: "Can you make it so the animation when the highlight switches between rows feels a little more rubber
like going from one to another. I don't want it to be super slow but maybe ease the old highlight and rubber
banding more."

This is one motion: the list's pill travelling from row to row (`Pill` in `overlay/Rows.kt`).

**What the review changed** (the reviewer's frame strips and scripts are in `review/`; I ran them again and
they hold):

| | First version | Now |
| --- | --- | --- |
| A, one row | kept | kept; the hold is five frames of the screen, not `delay(40)` |
| A, a long way | hold 40 ms, old edge 0.86 / 900: the old edge slowed down and sped up again at the landing | hold two frames, old edge 0.9 / 1400: one move, no second start, at rest sooner than today |
| How the pill is drawn | not looked at (and §8 said something wrong about it) | from two rounded edges; it is part of the change |
| B | a bounce: 2.5 dp past its place, 60 ms hold | withdrawn. What is called B now is A one step stronger, without a bounce |
| The page | px, a 40 ms key repeat, a held-key table for another list | dp, Android's 50 ms, the table for the page's own list, the caveat beside every claim about today's held key, a note on the other highlights |

## 1. What it does today

The pill has two edges, each on its own spring (`Motion.kt`). The edge on the side it is going to rides
`lead` = spring(0.82, 1100); the other rides `trail` = spring(0.9, 420). Both set off in the same frame.

One row (56 dp), from rest:

| | Today |
| --- | --- |
| Front edge at the new row (within 2 dp) | 111 ms |
| Old edge at the new row (within 2 dp) | 210 ms |
| Everything at rest (both edges within half a dp) | 255 ms |
| Longest the pill gets | 73.6 dp, 76 ms in: 17.6 dp of stretch, 31 % of a row |
| The old edge | moves from the first frame; half way after 78 ms, 90 % after 167 ms |
| Past its place | front edge 0.6 dp, old edge 0.1 dp: nothing is seen to bounce |

So the rubber is faint today for two reasons: the old edge leaves at once, and it is the slow part of the
move for the whole 255 ms. It is a slow follower, not a band that holds and lets go.

Other things the code does today:

- **A move of several rows**: the same two springs, so the stretch grows with the distance. Five rows: the
  pill is 144 dp long, and the front edge goes 3 dp past its row. A list closing from its twelfth action
  (480 dp): 216 dp long, 5.7 dp past.
- **How it is drawn**: `offset` takes the top edge rounded to a pixel, `height` the length rounded to a
  pixel. The bottom is the sum of two roundings, so it steps back and forth by a pixel while it settles: six
  times in a one-row step at 1.125 px per dp, once at 1.5. An edge that should stand still does not, when
  the other one moves.
- **A row that grows under the pill** (`if (top == upper.value)`): only the lower edge moves, on `place`
  (0.86, 520), the spring the row's own height rides. This holds only if the upper edge stands exactly on the
  row's top; one that is on its way is sent again.
- **A new target while it is moving** (a held key, the pointer, a quick second press, a reversal): see §6.
  The pill starts again from a standstill each time.
- **The speed setting** (Fast, Medium, Slow) does not touch the pill. It only sets the opening's times
  (`Arrival` in `Panel.kt`). `Motion.slow` is the debug stretch (`./bl open stay slow=4`) and is 1 for everyone.
- **Animations off**: every spring is `snap()`; the pill cuts.
- **Rows are 56 dp** (`Metrics.row`), a flight's or an answer's row 92 dp, an opened row's lines 40 dp. The
  prototype uses these.

## 2. The idea

Rubber is three things, in this order:

1. **The front edge goes first, and quickly.** It is what says "there".
2. **The old edge holds on.** For a moment it does not move at all, so the pill stretches over both rows.
3. **Then it lets go and gathers**, faster than today, so the whole move is not longer.

Width, corner radius, colour and alpha do not change while it stretches. It stays one flat shape: no
thinning, no gloss, no bounce. ("Not too 3D", and one highlight per level: at its longest it is one pill
over two rows, never two.)

## 3. The numbers

Springs are Compose's `spring(dampingRatio, stiffness)`. A frame is a frame of the device's screen: 8.3 ms
at 120 Hz.

### A, the one proposed

The front edge always rides spring(0.85, 1400) and starts at once. The old edge:

| The pill sets off | The old edge holds for | then rides |
| --- | --- | --- |
| from rest, to the next row (the front edge has at most 98 dp to go: a 56, 92 or 40 dp row) | 5 frames (42 ms) | spring(0.86, 900) |
| from rest, a long way (more than 98 dp) | 2 frames (17 ms) | spring(0.90, 1400) |
| while it is already moving (a held key, the pointer, a reversal) | nothing | spring(0.86, 900) up to 98 dp, spring(0.90, 1400) beyond |

**The limit of the stretch.** The pill is drawn at most 56 dp longer than its row (the taller of the row it
left and the row it goes to). Up to 40 dp of stretch it is drawn as it is; from there it eases into the
limit: drawn = 40 + 16 × (1 − e^(−(stretch − 40) / 16)). A one-row step peaks at 34 dp and never meets it.

**The hold is counted in frames, not in milliseconds.** With `delay(40)` the old edge would let go four,
five or six frames after the front edge, and the longest stretch would be 85, 90 or 94.5 dp from one press
to the next.

### One row (56 dp), from rest

| | Today | **A** | B, one step stronger |
| --- | --- | --- | --- |
| Front edge | spring(0.82, 1100) | spring(0.85, 1400) | spring(0.85, 1600) |
| Old edge | spring(0.90, 420) | spring(0.86, 900) | spring(0.82, 1400) |
| The old edge holds for | 0 | 5 frames (42 ms) | 6 frames (50 ms) |
| Front edge at the new row (within 2 dp) | 111 ms | **105 ms** | 98 ms |
| Old edge at the new row (within 2 dp) | 210 ms | 174 ms | 149 ms |
| Everything at rest (within half a dp) | 255 ms | **197 ms** | 215 ms |
| Longest the pill gets | 73.6 dp at 76 ms | 90.2 dp at 59 ms | 95.3 dp at 59 ms |
| Stretch, as a share of a row | 31 % | 61 % | 70 % |
| The old edge: half way, 90 % | 78, 167 ms | 94, 150 ms | 91, 132 ms |
| Front edge past its place | 0.6 dp | 0.3 dp | 0.3 dp |
| Old edge past its place | 0.1 dp | 0.3 dp | 0.6 dp (1.0 coming off a 92 dp row) |
| More than 1 dp short of its row, ever | no | no | no |

A's old edge, once it lets go, moves 1.6, 3.8, 5.0, 5.6, 5.7 dp a frame and then slows: it eases away.

### A long way, from rest (B is the same as A here)

| | Today | **A** |
| --- | --- | --- |
| Two rows (112 dp): longest · front edge there · at rest | 91 dp · 120 ms · 271 ms | 84 dp · 114 ms · 184 ms |
| Three rows (168 dp) | 109 dp · 124 ms · 279 ms | 98 dp · 119 ms · 201 ms |
| Five rows (280 dp) | 144 dp · 199 ms · 287 ms | 110 dp · 123 ms · 218 ms |
| A list closing, 480 dp | 216 dp · 225 ms · 408 ms | 112 dp · 192 ms · 238 ms |
| Front edge past its place, 480 dp | 5.7 dp | 3.1 dp, inside the list's 8 dp margin |

In no frame does the old edge slow down and speed up again, and the pill is never short of its row.

### A held key (B is the same as A after the first step)

Android repeats a held key every 50 ms unless a device sets something else; what a Googlebook does has not
been measured. Today's column is a simulation of today's code (§6), not a film of a device.

| | Today, simulated | **A** |
| --- | --- | --- |
| Eight rows of 56 dp, a row every 50 ms: longest | 151 dp | 92 dp (in the first two steps; 69 to 70 from the fourth on) |
| Most the front edge still has to go when the next key arrives | 70 dp | 25 dp |
| At rest after the last key | 291 ms | 146 ms |
| The same at 33 ms: longest · still to go · at rest | 172 dp · 169 dp · 296 ms | 90 dp · 51 dp · 172 ms |
| The prototype's own list (its third row is 92 dp), 50 ms | 153 dp · 70 dp · 291 ms | 96 dp · 39 dp · 146 ms |

## 4. Recommendation

**Build A.** It does what was asked: the old edge holds, the pill stretches nearly twice as far as today
(34 dp for 18), and everything is at rest 58 ms sooner than today. Nothing passes its place by more than a
third of a dp, which fits flat glass and what Alex chose for the opening. This is the motion a launcher
shows most often; a flourish in it wears.

**B is on the page only to show degree.** The first B bounced, and the review was right about it: on every
step the pill was more than 1 dp short of its row for 67 ms, with the action pane off-centre in it
meanwhile, and on a tall row it covered both rows almost whole. That B is withdrawn. The B on the page now
is A one step stronger and has no bounce: the old edge holds one frame longer and gathers faster, 5 dp more
stretch, at rest 18 ms later than A. "A little more" is a question of how much, and one stronger step beside
A lets Alex answer it. If he picks it, it is three other numbers for the one-row step; everything else is A.

## 5. The hard cases, each decided

"Today" is today's code as in §6.

| Case | Decision | In numbers |
| --- | --- | --- |
| **Several rows at once** (the pointer comes onto a far row, a list closes, Tab wraps in an opened list) | The front edge goes; the old edge holds two frames and follows on a firmer spring, so the pill arrives in one move and gathers as it lands. The limit keeps it at its row plus 56 dp | §3, "A long way". At rest sooner than today at every distance |
| **A held key** | A pill that is moving does not hold, and it keeps its speed when the target moves on (today it does not, §6). It glides as one piece. The first repeat, from rest, is an ordinary step with its hold | §3, "A held key" |
| **A reversal in mid-flight** | Every edge turns round from where it is drawn, with the speed it has. No hold, because the pill is moving. If the old edge had not let go yet, it stays, and the front edge comes back on the old edge's spring | Back after 30 ms: at rest 166 ms after the second key (today 224). After 70 ms: 161 (today 255). After 120 ms: 156 (today 263). The top edge is never more than 0.3 dp outside its row |
| **A row changes height under the pill** (an answer needs more lines, Enter on a model's row) | As today: no rubber. Only the lower edge moves, on `place`, the spring the row itself grows on. One thing better: an upper edge still on its way is no longer stopped and sent again | Unchanged: 0.86 / 520 |
| **Onto and off a tall row** | The same move; the two edges have different ways to go, so the pill grows or shrinks as it travels | Onto a 92 dp row: longest 118 dp (26 over), front edge there after 112 ms, at rest 197 ms (today 106 dp, 118, 255). Off it: 124 dp, 105 ms, 202 ms. Onto a 40 dp line: 64 dp, 99 ms, 192 ms |
| **The pointer sweeping over rows** | Each row crossed is a new target. A slow pointer makes ordinary steps, each with its hold. A fast one is a held key. Hover still selects only when the pointer moves | As the held key |
| **The list changes under a resting pill** (typing) | The pill does not travel: the selection stays in row one's place, and only its height may change (`place`, as today). If the selected row itself is moved by a list change (rows arriving above a kept selection), the pill goes with its row: both edges on `place`, no rubber. Today it goes on `lead` and `trail` while its row goes on `place`. Typing waits for nothing | Needs the selected row's id in `Pill` (§7). Not in the prototype |
| **Fast, Medium, Slow** | They are the opening's setting and stay so. The pill is the same at all three, as today | `slow` (debug) stretches the hold with the springs |
| **Animations off** | It cuts: the springs are `snap()`, the hold is no frames | The prototype does the same when the system asks for less motion |
| **Dark theme** (the pill is see-through, 0.42) | The same motion. The fill keeps its alpha while it stretches; it is one shape, so nothing doubles where it lies over two rows. Over a dark window the white rim (0.30) is what shows the stretch | To be looked at on the device (§9) |
| **Whole pixels** | The pill is drawn from two rounded edges: top = round(upper), bottom = round(lower), height = what lies between. An edge that is held stands still; an edge that settles does not step back | At 1.125 and 1.5 px per dp, in every move tried: no edge steps back (today's way of drawing: up to six times in one step) |
| **Coming from nothing, going** | As today: it appears where it belongs and fades where it stands | Unchanged |
| **Text while the pill passes** | Next paragraph | |

**The row's ink when the pill is half on it.** Nothing about a row's ink depends on where the pill is. The
pill is drawn under the rows. A title is full ink on every row, selected or not, and reads on the pill and
on the glass alike (in dark 5.6:1 and 12.6:1, design-system §12). The second ink (the small line, the mark)
goes from 0.80 to 1.0 on the new row in 120 ms from the key, and back on the old row in the same time; the
kind label fades out in 60 ms and the strip comes 60 ms after the key. So with the pill half on a row, half
of its words lie on colour and half on glass, at the same strength. Two things get better with the hold:
the old row's strip fades (60 ms) while the pill still covers all of it, where today its upper part is
already bare; and the new row's strip arrives over a pill that covers more of the row (A's front edge is
72 % there after 60 ms, today's 67 %).

## 6. What surprised me in today's code

1. **A pill that is moving starts again from a standstill at every new target.** `Pill` animates inside
   `LaunchedEffect(top, height, visible)`. When a key changes, Compose cancels that effect and starts a new
   one. Cancelling an `Animatable`'s animation resets its speed (`Animatable.endAnimation`), and the new
   animation shows its first movement a frame later than one that takes over a running one. I checked both
   with the library itself (animation-core 1.13.0-alpha01, run on the Mac outside the repo): at 1334 dp/s,
   a restarted effect stands still for one frame and goes on from speed 0; an `animateTo` that takes over
   keeps the 1334. On a single step nobody sees it. On a held key it is the difference between a glide and
   a smear. This is read from the code and from that test, not from a film of a Googlebook; a held Down at
   `slow=4` would show it. The same pattern is in the card's options (`Strip.kt` 377), the grid's square
   (`Bodies.kt` 285) and the window's highlights (`Nav.kt` 234, `Page.kt` 415).
2. **The bottom edge is not rounded by itself.** It is round(top) + round(length). The first version of
   this proposal said each edge was rounded by itself; that was wrong, and the reviewer's pixel strips show
   the cost: the bottom going 126, 127, 126 px while it settles.
3. **The speed setting does not scale springs or fades.** Only the opening.
4. **Rows are 56 dp, not 64.**
5. **The stretch has no limit today.** It is a fixed share of the distance: 31 % for one row, 144 dp over
   five rows, 216 dp when a long list closes.
6. **`if (top == upper.value)`** compares with where the edge is, so "its row only grew" is not recognised
   while the upper edge is still travelling; the edge is then stopped and sent again.

## 7. The change, described (not applied)

`app/src/main/java/io/github/kuscher/booklight/overlay/Motion.kt`

- `pillLead()` = `s(0.85f, 1400f)`.
- `pillTrail(far: Boolean)` = `if (far) s(0.90f, 1400f) else s(0.86f, 900f)`.
- `pillHold(far: Boolean, refresh: Float): Int`, in frames: `if (!on) 0 else ((if (far) 0.017f else 0.040f) * slow * refresh).roundToInt()`.
  At 120 Hz that is 5 and 2; at 60 Hz 2 and 1.
- In the companion: `PILL_NEAR = 98.dp` (a way longer than this is a long way), `STRETCH_KNEE = 40.dp`,
  `STRETCH_MOST = 56.dp`, and `stretch(extra: Dp): Dp` = `extra` up to the knee, then
  `KNEE + room * (1 − exp(−(extra − KNEE) / room))` with `room = MOST − KNEE`.
- `lead()` and `trail()` stay as they are for everything else that uses them.
- If Alex picks the stronger step: `s(0.85f, 1600f)`, `s(0.82f, 1400f)` and 0.050 for the one-row step from
  rest. Nothing else.

`app/src/main/java/io/github/kuscher/booklight/overlay/Rows.kt`, `Pill`

1. **One effect that lives as long as the pill**, in place of `LaunchedEffect(top, height, visible)`: the
   target is read through `rememberUpdatedState` and `snapshotFlow { … }.collect { … }`, and each edge's
   `animateTo` is launched from inside it. A new `animateTo` then takes over through the `Animatable`'s own
   mutex and keeps the edge's speed. This is how `animate*AsState` does it. It is what makes the held key
   work.
2. **At rest** means: neither edge is running and no hold is waiting. From rest: the front edge
   `animateTo(…, pillLead())` at once; the old edge waits `pillHold(far, refresh)` frames on the frame clock
   (`repeat(n) { withFrameNanos { } }`), then `animateTo(…, pillTrail(far))`. Its animation then has its
   first frame exactly n frames after the front edge's. Not at rest: both at once. The hold is a job that
   only ever waits (it launches the `animateTo` as its sibling), so cancelling it on the next target never
   cancels an animation.
3. **Its row only grew**: `top == upper.targetValue` and no hold waiting → only the lower edge, on
   `place()`, as today.
4. **The limit.** `rest` = the taller of the last row and this one (an `Animatable<Dp>`: set at once from
   rest, on `place()` otherwise). `extra = (lower − upper) − rest`. If `extra > STRETCH_KNEE` the pill is
   `rest + stretch(extra)` long, measured from the front edge (`down` is kept as state).
5. **Drawn from two rounded edges**, in a `layout` modifier in place of `offset` and `height`:
   `val t = top.roundToPx(); val b = bottom.roundToPx()`, measured `b − t` high (12 dp at least), placed at
   `t`. The two animated values are then read in layout, not in composition.
6. **On a new target, an edge goes on from where it is drawn.** Only if the limit was holding it: `snapTo`
   that place, then `animateTo` with the speed it had.
7. **Glued to its row**: `Pill` takes the selected row's id. Same id, other top → both edges on `place()`.

A sketch of 1 and 2, not compiled:

```kotlin
val goal by rememberUpdatedState(Triple(top, height, visible))
LaunchedEffect(Unit) {
    var hold: Job? = null
    snapshotFlow { goal }.collect { (top, height, visible) ->
        if (!visible) return@collect
        val resting = !upper.isRunning && !lower.isRunning && hold?.isActive != true
        val grew = top == upper.targetValue && hold?.isActive != true
        hold?.cancel(); hold = null
        if (shown == 0f) { upper.snapTo(top); lower.snapTo(top + height); return@collect }
        if (grew) { launch { lower.animateTo(top + height, motion.place()) }; return@collect }
        down = top + height / 2 > (upper.targetValue + lower.targetValue) / 2
        val front = if (down) lower else upper; val old = if (down) upper else lower
        val frontTo = if (down) top + height else top; val oldTo = if (down) top else top + height
        val far = abs((frontTo - front.value).value) > Motion.PILL_NEAR.value
        launch { front.animateTo(frontTo, motion.pillLead()) }
        if (!resting) launch { old.animateTo(oldTo, motion.pillTrail(far)) }
        else hold = launch {
            repeat(motion.pillHold(far, refresh)) { withFrameNanos { } }
            this@LaunchedEffect.launch { old.animateTo(oldTo, motion.pillTrail(far)) }
        }
    }
}
```

Docs to follow the code: `docs/design/design-system.md` §6 (a line for the list's pill) and §11's list of
springs.

## 8. The other highlights

The same kind of highlight exists in more places. This proposal changes the list's pill only; which of the
others follow is Alex's decision. My view, and where it differs from the review:

- **The grid's square** (`Bodies.kt`, `GridBody`): built the same way, from edges on `lead` and `trail`.
  It can take A's pair in the same release, with the same two repairs (keep the speed, two rounded edges).
  A held arrow runs along fourteen cells there, so it needs "no hold while moving" more than the list does.
  Agreed with the review.
- **The pane on a row's actions** (`Strip.kt`, `ActionStrip`): the review would move it in the same release.
  I would not, yet. It is not built from two edges: one `arm` spring per slot carries the pane's place and
  how much of each name is unrolled, as one value. Giving the pane a held edge would take it out of step
  with the names, or the names would have to wait too. It needs its own proposal and its own film.
- **The card's options** (`Strip.kt` 377) and **the window's rail and rows** (`Nav.kt`, `Page.kt`): two
  edges on `lead` and `trail`. Later, as the review says; the window has its own feel (Material's) to weigh.

## 9. To be judged on the Googlebook

1. **The hold at 120 frames a second.** Five frames. Does it read as the pill holding on, or as the old row
   answering late? Film a step at `slow=4` and at full speed; try four and six.
2. **A or the stronger step.** The page shows the difference; the device decides.
3. **Dark theme over a dark window.** The fill is 0.42 and the stretch is carried by the rim. Is the
   stretch seen at all there, and is that enough?
4. **Today's held key.** Is it the smear §6 computes? And how fast does the device's key really repeat?
5. **The strip against the hold.** The old row's pane must fade on colour, the new one arrive on colour: no
   frame with a pane on bare glass.
6. **The long way at 1.125 px per dp**, where a two-row pill moves 34 dp in a frame: one shape, or a
   flicker of two?

## 10. How the numbers were made

Both springs integrated as Compose defines them (mass 1: acceleration = stiffness × (target − x) −
2 × dampingRatio × √stiffness × speed) in steps of a quarter of a millisecond. "There" is the first moment
from which the front edge stays within 2 dp of its place; "at rest" the same for both edges and half a dp.
Times count from the animation's first frame; the frame or two Compose takes between a key and that frame
are the same in all three and left out. Today's restart is modelled as the test in §6 showed it: speed 0,
and one frame (8.3 ms) without movement. The prototype runs the same code and prints the same numbers under
its list; `work/pagecheck.js` takes the page's table from the page's own script and its own list.
`work/engine3.js`, `r3.js`, `held3.js` and `px3.js` are this version's simulation; the first version's
files are beside them.
