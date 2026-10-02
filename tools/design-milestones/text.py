# The page's words. Run by gen.py (it supplies shot, screen and the drawings); [[name]] is where a drawing goes.
# Screens: 1 to 4 are M1, 12 to 15 are M5. The parked milestones keep theirs: 5 to 7 are M2, 8 and 9 are M3, 10 and 11 are M4.

NV = '<span class="tag nv">not verified</span>'


def src(text):
    return f' <span class="src">({text})</span>'
REC = '<span class="tag rec">recommended</span>'


def table(head, *rows, cls="", wide=False):
    h = "".join(f"<th>{c}</th>" for c in head)
    b = "".join("<tr>" + "".join(f'<td{" class=k" if i == 0 and cls == "k" else ""}>{c}</td>' for i, c in enumerate(r)) + "</tr>" for r in rows)
    # on a narrow screen a table of three columns or more keeps a readable width and scrolls inside its own box
    minw = "52rem" if len(head) >= 5 else "34rem" if len(head) >= 3 else ""
    return f'<div class="tablewrap"><table{f" style=min-width:{minw}" if minw else ""}><tr>{h}</tr>{b}</table></div>'


def eg(do, get):
    return f'<div class="eg"><span class="k">You do</span><span>{do}</span><span class="k">You get</span><span>{get}</span></div>'


def feat(title, text, example=""):
    return f"<li><div><p><b>{title}</b> {text}</p>{example}</div></li>"


def mshead(n, name, promise, size):
    return (f'<div class="mshead"><div class="mline"><span class="mtag">M{n}</span><span class="mname">{name}</span><span class="msize">{size}</span></div>'
            f"<h2>{promise}</h2></div>")


def q(title, rec, other):
    return f'<li><div><b>{title}</b><span class="rec">Recommended: {rec}</span><span class="alt">{other}</span></div></li>'


HEADER = '''
<header>
  <div class="brand">
    <svg viewBox="0 0 48 48" aria-hidden="true"><rect width="48" height="48" rx="13" fill="var(--accent)"/><path d="M14 11h20a5 5 0 0 1 0 10H14a5 5 0 0 1 0-10z" fill="var(--accent-ink)"/><path d="M16.5 25h15L37 39H11z" fill="var(--lamp)"/></svg>
    <h1>Booklight after 2.1</h1>
  </div>
  <p class="lede">The plan, adjusted on your word: the main panel gets M1, the copy, and a new M5, flights. The improvements you asked for after 2.1 are built, on main, and go out first as 2.2. M2, M3 and M4 are parked at the end of this page, unchanged. Nothing of M1 or M5 is built, and no device was touched for them; where something is not verified, this page says so.</p>
  <div class="meta"><span>1 October 2026</span><span>Eight screens: 1 to 4 are M1, 12 to 15 are M5</span><span>Thirteen decisions for you, at the end: seven large, six small</span><span>Nothing is built before you say go</span></div>
</header>
'''

GLANCE = '''
<section>
  <h2>What comes next, at a glance</h2>
  <div class="glance">
    <a class="gcard" href="#r22"><span class="num">2.2</span><h3>What you asked for after 2.1</h3><p>The opening at Medium on its new curve, glass that is frosted from its first frame, the white reflection, three placements on an app’s row, the see-through selection in dark.</p><span class="size">Built and reviewed, on main · waits for your word to release</span></a>
    <a class="gcard" href="#m1"><span class="num">M1</span><h3>The copy</h3><p>Copy something, open Booklight, and one line offers what can be done with it.</p><span class="size">About 14 days · no new permission</span></a>
    <a class="gcard" href="#m5"><span class="num">M5</span><h3>Flights</h3><p>Type a flight number and the row answers with the next flight: when it leaves, when it lands, where to go, and whether it is late.</p><span class="size">About 10 days · no new permission · the times need a free key of your own</span></a>
  </div>
  <div class="prose blk">
    <p><b>M1 is the M1 of the four-milestone plan.</b> One thing changed: a flight number in a copy now gets M5’s row; before, flights were left out of M1 because Booklight could only search the web for them.</p>
    <p><b>M5 is new and has one hard fact in it.</b> You asked: “I want to type LH455 and get info about the next flight.” No free source gives a flight’s times without a key''' + src("FL, the short version") + '''. What Booklight can do by itself is name the airline and open the flight’s page. For the times, the gate and the delay in the row, it needs a key from a flight data service, and since Booklight has no server and its code is public, that key has to be yours: a free account, about 200 lookups a month. That is decision 4.</p>
    <p class="muted small">Sizes: M1’s is the engineer’s from the four-milestone plan, not re-sized. M5’s is mine, from the parts listed under it; no engineer has sized it. Read the days as sizes against each other, not as a calendar. Sources are named as in the plan: FL is <code>flights.md</code>, NF2 is <code>next-features-2.md</code>, S&amp;C is <code>screen-and-clipboard.md</code>, DF is <code>device-findings.md</code>, LANG is <code>languages.md</code>, all in <code>docs/research/</code>; UX, ENG and DES are the team’s papers behind the four-milestone plan. “Check n” is a line of the engineer’s device session; “check F n” is one of M5’s.</p>
    <p class="muted small"><b>Parked, not dropped:</b> M2 the picture, M3 the selection, M4 your language, with their designs and decisions, are at the <a href="#parked">end of this page</a> as they were.</p>
  </div>
</section>

<section id="r22">
  <h2>2.2: what is on main</h2>
  <div class="prose blk">
    <p>Your notes on 2.1, each taken through a visual designer and a motion designer, and your question about the blur. All of it is built, reviewed and on main; none of it is released.</p>
  </div>
  ''' + table(("What you said", "What it is now"),
              ("“default to medium for opening speed”; “slow beginning then fast then slow again”", "The opening is at Medium unless chosen otherwise, on a curve that starts slowly, is quick through the middle and lands over a long stretch, with no rebound"),
              ("“why the blur just pops in”", "The glass is frosted from its first frame and stays so until the seam is gone. The window never moves; the blur’s region is fitted to the glass before every frame. The first try resized the window, and you saw it open one side first; that was thrown away"),
              ("“the shimmer … should come like a few seconds later and be more complete … a white reflection running around”", "One white light, 2.4 s after the panel has opened, once round from the middle of the top edge. Any key and it fades. While the model works, the same light runs steady and dimmer"),
              ("“the many Window management options by default are too much”", "An app’s row has six icons: Open, New window, App info, Maximise, Left half, Right half. The other places are in the list under the arrow"),
              ("“the highlight feels stark on dark”", "The selection in dark is see-through like the panel: the same colour at 42 %"),
              ("“it doesnt show the actions for apps like chrome which has new tab”", "Found, not changed: Chrome keeps that shortcut closed to other apps. Only the device’s assistant may start it" + src("DF")),
              ("Found on the way", "The seam the panel opens out of had been invisible since 2.0’s shadow. It is seen again"), cls="k") + '''
  <div class="prose blk">
    <p><b>Releasing it</b> is a tag: a GitHub release and a draft on Play’s closed testing. Nothing goes to Play’s production; that stays empty until you say otherwise. It is decision 2.</p>
  </div>
</section>
'''

HOLDS = '''
<section>
  <h2>What holds for both</h2>
  <div class="prose blk">
    <p>No new permission. A model’s row never runs anything by itself. Nothing typed leaves the device unless you have switched that on. Nothing in the background. Every string in English and German. Every “not verified” gets its device check before the feature is promised. The release build is installed and asked a prompt before every tag.</p>
    <p>The model is asked on Enter, or when you pick its name in another app’s menu; never after a pause in typing. Ctrl + 1…9 never asks it. Esc closes at every step.</p>
    <p class="notice"><b>One check stands before M1.</b> Your HP has never run 2.0, and its model has never been tried. If it does not answer there, every model row on the HP hands over to Gemini, and M1’s model rows can only be judged on the Lenovo. It is check 1 of the device session. M5 does not need the model.</p>
  </div>
  <h3>The keys in the new states</h3>
  ''' + table(("State", "Enter", "Tab", "Down", "Typing", "Backspace on the empty field"),
              ("The quiet line", "Nothing", "Opens it", "Opens it", "Puts it away in the same frame", "Nothing"),
              ("A thing’s rows", "Runs the armed action. On a model’s row: asks", "The selected row’s next action", "Next row", "Narrows the rows by name. What matches no row is the instruction, or the question about a picture", "The chip goes; the line is back"),
              ("A row being answered", "Waits for the whole answer, then does the armed action", "Next action", "To the picture’s row, if there is one", "Takes the answer back", "The rows are back"), cls="k", wide=True) + '''
  <div class="prose blk">
    <p><b>One state when the model does not answer.</b> The caption says it in plain words, the thing’s first line stays, and the way to the Gemini app is the action. No colour, no warning mark. On a device with no model the row’s label is “Gemini” from the start. A text too long to rewrite here (over about 3,000 characters) carries “Gemini” before Enter. An answer that was cut says “cut off here”, and Replace is never first on it.</p>
    <p><b>Two lights, one outline.</b> Both are white and both run along the panel’s edge. The <i>reflection</i> runs once, a few seconds after an idle panel has opened: it is for fun and means nothing. The <i>slow lap</i> runs round and round, dimmer, while the model has been asked and has said nothing yet: it means “working”. Both are today’s polish; this plan adds no light of its own.</p>
  </div>
</section>

<section>
  <h2>The lines every design is drawn on</h2>
  <div class="prose blk">
    <p>You rejected an earlier mock-up because it “looks really misaligned”. So every panel on this page is drawn at 1 px for 1 dp on the app’s own lines, taken from <code>Metrics.kt</code>, <code>Rows.kt</code>, <code>Field.kt</code> and <code>Strip.kt</code>, and on nothing else.</p>
  </div>
  ''' + shot("The six lines, on one panel", "[[ground]]",
             "<b>8 and 712:</b> the highlight, inset 8 dp. <b>20:</b> the chip’s left edge. <b>38:</b> the centre of every disc and of every chip’s icon. <b>72:</b> the text edge of the field, of every title and of every answer. <b>700:</b> where the strip, the labels and the footer end.") + '''
  ''' + table(("What", "Value"),
              ("Panel", "720 dp wide, radius 32"),
              ("Field", "68 high. Mark or chip from x = 20. Text from x = 72"),
              ("Rows", "56 · tall 92 · an answer grown to four lines 136 · list padding 8 above and below · footer 36"),
              ("Icon column", "36 dp disc centred on x = 38. Every chip’s icon is centred there too"),
              ("Strip", "Flush right at x = 700. Slots at a 32 dp pitch, icons 18"),
              ("Chip", "36 high, from x = 20: 8 · icon 20 · 6 · name 15/600 · 14"),
              ("Type", "Field 24/500 · title 17/500 · small 14/500 · hint 13/500"),
              ("Ink", "1.0 · 0.80 · 0.60 of the surface’s ink"),
              ("The quiet line", "Row one’s seat: y = 76 to 132, so the panel is 68 + 8 + 56 + 8 = 140 dp. Disc on x = 38. One text from x = 72 in the small type; the age at 0.80, the rest at 1.0 (both 1.0 in dark). Its <kbd>tab</kbd> cap ends at x = 700"),
              ("An answer’s row", "2.0’s, unchanged: 92 dp, grown once to 136. Caption small at 0.80. Text 17/500 on 22 dp lines, from x = 72 to 16 dp before the strip"),
              ("A picture’s row", "136 dp from the moment it appears. The picture hangs from x = 24, y = 16, fitted into 176 × 104 in its own shape; radius 12, a 1 px ring of ink at 0.20, no shadow. From x = 220: the picture’s size, then the read text in the small type at 0.80 on 19 dp lines, four at most, the last one fading"),
              ("A picture as a mark", "In the chip: a 28 dp circle centred on x = 38. In a 56 dp row: 36 dp like every mark (edges at 20 and 56), radius 11, cut from the picture’s top-left"),
              ("A pinned picture", "Edge to edge in the small window: 280 × 140 for a 2 : 1 picture (measured); no margin, caption, button or ring"), cls="k") + '''
  <div class="prose blk">
    <p><b>Drawn on today’s polish, not on 2.1.</b> Five things are taken from the code and not from the 2.0 design page: the chip’s padding; the strip after today’s polish (three placements and an app’s own shortcuts); the dark highlight, see-through and with less colour, at the app’s value of this afternoon (<code>secondaryContainer</code> at 0.42 on glass: design system §12); the edge light in white; small type at 14 and hints at 13. Screens 2, 5 and 8 show that highlight under the new rows. None of the new elements uses its colour.</p>
    <p><b>The motion in the panels that play</b> follows the numbers settled today in design system §12, at Medium: the seam for 160 ms, the glass from 60 ms for 360 ms on its new curve (slow, fast, slow), at rest 420 ms after the key; the white light 1.75 dp wide in dark and 2.0 in light, with a front and a tail, and no glow. A browser draws them, so they are a close copy, not the app: the glass’s blur and the light’s soft inner edge are simpler here. The app and its recordings stay the reference.</p>
    <p><b>Nothing new in the look beyond four elements,</b> each built from parts the app has: the quiet line; a chip with a picture in it; a row with a picture; a picture in the pinned window. No new colour, type size or row height. (The last three belong to the parked M2.) M5 adds one more: a flight’s row, which is the Event row’s height with a third line.</p>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ M1
M1 = '<section class="ms" id="m1">' + mshead(1, "The copy", "Copy something, open Booklight, and one line offers what can be done with it.", "About 14 days · no new permission") + '''
  <div class="prose blk">
    <p class="notice">Your idea, in your words: “a function to grab the copy contents and act on them, like if there is text in the copy buffer hit tab and have translate options. Maybe that’s a suggestion for zero state if zero state is coming and there was a recent copy”.</p>
    <p><b>The story.</b> A mail in German is open in Gmail. You select a paragraph, press Ctrl + C and press the Booklight key. Under the empty field one quiet line says “Copied just now · text”. Tab opens it and the first row is “In English”; Enter, Enter, and the translation is on your clipboard. Four presses, no typing. Want it in Danish instead? After Tab, type <code>danish</code>.</p>
  </div>

  <div class="prose blk">
    <h3>What is in it</h3>
    <ol class="feat">
      ''' + feat("One quiet line for a fresh copy.", "If you copied something in the last two minutes, the empty panel shows one line after it has opened. For this Booklight reads only the system’s description of the copy (its kind, its age, what the system found), not the text. Android’s documentation says the system then shows no “pasted” message" + src("S&amp;C §4.1") + ". Reading the description was tried on the Lenovo" + src("DF") + "; the missing message for another app’s copy is check 4, " + NV + ". The system looks only at copies of up to 400 characters; a longer one always says “text”. A copy marked private (a password manager’s) never gets a line, if the app marks it (check 5), nor does Booklight’s own copy. Enter does nothing on the line.",
                 eg("Ctrl + C on a chat message, then the key", "“Copied just now · a link and a date”")) + '''
      ''' + feat("Tab: what is in it.", "Tab or Down makes the copy the chip. Now the text is read, and the system shows “pasted” once. First come the things that need no model, three rows at most: a link (Open, and Copy clean without its tracking tail), a date (the Event row, filled in, so you see the day before the calendar opens), a phone number (Enter calls if an app answers it, else copies it) and a mail address (Enter opens the compose window). What answers a phone number is check 7, " + NV + "; no test text held a mail address, so that row is not tried.",
                 eg("Tab, Enter. Or Tab, Down, Enter, Enter", "<code>example.com/table</code> opens. Or “Dinner with Anna, Fri 2 Oct, 19:00” stands in the calendar’s editor")) + '''
      ''' + feat("What the model can do with it, answered in the row.", "Your first three prompts stand there by name; the translation is one of them. Summary too for a long text. The translation knows its direction: a text not in your language comes into it and stands first; a text in your language goes into your other one (German in an English Booklight, English otherwise). Enter asks, the row grows into the answer, and the chip stays the copy.",
                 eg("The key, Tab, Enter, Enter", "The German paragraph in English, on your clipboard")) + '''
      ''' + feat("Into any language.", "Your words were “translate options”. Under the copy’s chip, a typed language is the translation into it. From the empty field the same is <code>tr</code>, then the language, then the text. <code>de</code> and <code>en</code> stay. A language that read badly in the device test hands over to Gemini.",
                 eg("Tab, then <code>danish</code>, Enter, Enter. From the empty field: <code>tr danish see you on Saturday</code>", "The copy in Danish, on your clipboard. Or “Vi ses på lørdag” in the row")) + '''
      ''' + feat("Say what to do with it.", "Under the copy’s chip, what you type is the instruction, unless it is a row’s name or a language. There is no keyword for it: <code>do</code> and a space would swallow “do not disturb”.",
                 eg("Tab, then <code>pull out the tasks as a list</code>, Enter. From the empty field: <code>clip pull out the tasks as a list</code>", "The tasks as a list, in the row, to copy or pin")) + '''
    </ol>
    <p>Two changes to <code>clip</code> come with it. It opens the same list at any age, with fewer rows than today (decision 9). And it no longer reads a copy marked private; it says “What you copied is marked private”.</p>
    <p><b>This changes one thing in the released app</b> (decision 10). Shared text behaves the new way too. Today Enter on “Fix spelling” turns the chip into the prompt and types the whole text into the field. After M1 the chip stays the text and the row is answered in its place. The selection handed over by today’s “Booklight” menu entry gets the same list at no extra cost, because it is the same code.</p>
  </div>

  <div class="blk">
    <h3>The designs</h3>
    ''' + screen(1, "Open, and the copy is there.",
                 shot("Light · plays", "[[c1]]", "<b>What happens.</b> The panel opens as always, at Medium. The line comes 320 ms after the glass is 85 % open: the panel grows by one row and the mark, the words and the cap rise 12 dp and fade in, 22 ms apart. A letter takes the line away in the same frame. Tab makes the copy the chip. ⌫ out of that chip brings the line back; once typing has put it away, emptying the field does not, or it would flicker while you edit. The second button plays the same with the line at 200 ms, to compare.")
                 + shot("The first five seconds", "[[timeline]]", "The line has landed long before the white reflection starts. The reflection comes 2.4 s after the panel has opened and never sooner than 0.7 s after the last change under the field, so it never runs while the line is arriving, while its words settle, or while an answer is being written. It takes 1 ms for each dp of outline: about 1.7 s round this panel. In light theme over a white window it cannot be seen (accepted today); press Dark above to see it.")
                 + shot("Dark", "[[s1dark]]", "The line has no highlight, so in dark it is ink on bare glass. The left half of each panel lies over a white page, the right half over a dark window. Over the white page dark glass turns mid-grey and small text on it measures about 3.4 : 1 here; the design system asks 4.5 : 1 for text that is said nowhere else. So in dark both parts of the line are full ink, and the line over a white window is a device check before this design is final.")
                 + shot("Light · German, and two more states", "[[s1states]]", "German is the longer one here and is far from the limit of 80 characters. “text” stands when the system found nothing or has not looked yet; the words may settle once, in place, and nothing is pushed. From three things on the line ends in “and more”. A line too long ends in a fade, never in an ellipsis."),
                 "You press the key after a copy and one line stands under the field: when you copied, and what the system found in it. It is not a result. A result comes with the highlight and the footer, and Enter runs it; a line nobody asked for has none of that. It sits where row one would sit, with its <kbd>tab</kbd> cap under the field’s <kbd>esc</kbd> cap. What was found is said in words, because small icons at a row’s end mean “what the selected row can do”. It cannot quote the text: it has not read it.") + '''
    ''' + screen(2, "Tab.",
                 shot("Light", "[[s2light]]", "The copy: “Dinner with Anna on Friday at 7pm. Call +49 30 5550 1234 or book at https://example.com/table?utm_source=chat”, copied on Thursday 1 October 2026. Press Tab in the playing panel of screen 1 to see it arrive: the rows cascade, and the highlight appears on row one in place. It never flies in. Copy clean (the link with a spark) shows only on a link that has a tail to take off.")
                 + shot("Dark", "[[s2dark]]", "The list on today’s softer highlight. With the highlight on a model’s row the right end is “Ask” and the footer offers the way back."),
                 "Tab, and the copy is the chip. Under it stand ordinary rows: first what was found in the copy, each named by the thing itself, so a wrong date is seen before it is used; then what the model can do, each with the spark, which makes them a family without a heading. “On this device” says before Enter that a row is answered here. Three of those labels stack; it is the busiest column in M1 and the first thing to thin out if you want fewer things: the label could stand only where a row hands over (“Gemini”). Eight rows at most; a title never changes after its row is drawn.") + '''
    ''' + screen(3, "The answer, where it stands.",
                 shot("Light · thinking", "[[s3think]]", "Enter on “In English”. This copy is German, so its translation stands first; in screen 2 the copy was English and “In German” stood last. The row has grown to an answer’s height and shows the start of your text, dimmer. The other rows fade where they are.")
                 + shot("Dark · thinking, a moment later", "[[s3dark]]", "The other rows are gone and the panel has the answer’s height. Until the first word the white light runs its slow lap round the edge; it is drawn in dark, where it shows.")
                 + shot("Light · answered", "[[s3done]]", "The caption gains “on this device”, the answer is written in, and Copy is armed. Enter pressed early waits for the whole answer.")
                 + shot("Light · two small states", "[[s3small]]", "An instruction: what you type under the chip, when it is no row’s name. And the one state for every row when the model does not answer: plain words in the caption, your text stays, the way to the Gemini app is the action."),
                 "You press Enter on a model’s row and the answer is written into that row. The chip stays the copy, so the field is free for a next instruction and the thing stays in sight. The caption names the prompt, because the chip no longer does.") + '''
    ''' + screen(4, "Into any language.",
                 shot("Light", "[[s9]]", "The row names its direction in one form everywhere: “In Danish”, as “In English” in screen 3. The chip of <code>tr</code> stays “Translate”. The Danish is a draft."),
                 "After Tab you type a language and the row is the translation into it. From the empty field the keyword is <code>tr</code>, with the language as the first word. It is also the keyword the parked M4 needs: “en” or “de” is an everyday word in each of its six languages." + src("LANG §3.4")) + '''
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>Addresses (a web address; they were not found in the Lenovo test). A flight number in a copy comes with M5, as M5’s row. Copy as Markdown (Booklight does not know a page’s title, so the link would hold its address twice). The system classifier’s ready-made actions (Booklight could not say what one does). A history of copies and pasting in place (both need the power pack). A picture on the clipboard (the parked M2). A line for a copy older than two minutes. A setting for the two minutes.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>The line is built as a third thing that can stand under the empty field, beside the first-run card and the tip, so it brings no highlight, no footer and no stray Enter. When the window has focus and the panel is drawn, Booklight asks the system for the copy’s description; the rule (younger than two minutes, not private, not Booklight’s own, a known kind) is plain code with tests. Tab reads the text and opens the list that <code>clip</code> opens, so there is one list. What is in the text comes from the system’s findings, looked up before the list is shown and capped at about 150 ms; nothing is slipped in above the selection later. <code>tr</code> is a keyword and a table of languages on the translation.</p>
    <p>Six shared pieces are built here once, and the parked milestones use them again: one way to an answer for any row, the material travelling beside the field, ready-made prompts kept by reference, one clipboard helper, the language of a text, and plain answers. <code>PRIVACY.md</code> changes: today it says the clipboard is read only when you ask.</p>
    <p><b>About 14 days:</b> the engineer’s 13 (11 to 15: 3.5 for the shared pieces, 7.5 for the features, 2 to finish) and 1.5 for the typed language and <code>tr</code>, which came from M3''' + src("ENG §7.3") + '''. The 13 were made before the cuts above and without the answer where it stands as its own line; they were not re-sized. The cheapest form that keeps the promise is about 5 days. Riskiest: the line, done to the standard of the opening.</p>
  </div>

  <div class="blk">
    <h3>To check on a device first</h3>
    <p class="prose">All of these are ''' + NV + ''' today, and so is the HP as a whole.</p>
    ''' + table(("Check", "What", "If it goes badly"),
                ("1", "Does the model answer on the HP at all? 2.0 has never run there", "The model’s rows there say “Gemini” and hand over. The line, the link and the date still work"),
                ("4", "The “pasted” message: none on the look, one on Tab", "No line at all. Tab on the empty field opens the copy"),
                ("3", "How soon the system has looked at a copy; what “not looked at” means", "The line says “text” more often"),
                ("5", "A password manager’s copy; Booklight’s own copy", "A password’s copy would get a line that says “text”. Its content is still not read before Tab"),
                ("6, 7", "What the system finds, and how fast; what answers a phone number", "A phone number gets Copy only"),
                ("8", "The language of a text; a translation of 2,000 characters", "The limits of the model’s rows move"),
                ("18, its second half", "A translation into eight languages. It moves to the first session with <code>tr</code>", "A language that reads badly is not in the table; typing it hands over to Gemini"),
                ("In dark", "The line over a white window: can it be read?", "The line is designed again for dark: larger type, or a ground of its own")) + '''
  </div>

  <div class="prose blk">
    <h3>For you to decide</h3>
    <p><b>A line with nothing typed</b> (decision 3). Your plan says “Nothing typed: nothing at all” (PLAN.md §11.3). This is the first line that depends on what you did in another app, and it changes one sentence of the privacy text. ''' + REC + ''' Yes, with a switch in the window (“What you copied”). If no: the empty panel stays empty, Tab there opens the copy, and the rest of M1 is the same.</p>
    <p><b>The translation first</b> (decision 8). For a text not in your language its translation stands above what was found in the copy. That puts a model’s row above local rows; the 2.0 design says a model’s rows “never outrank a local match”. ''' + REC + ''' Yes, this one exception: it is what makes “four presses, no typing” true.</p>
    <p>Two smaller ones are named above: <code>clip</code>’s shorter list (decision 9) and shared text in the released app (decision 10).</p>
    <p class="muted small">Settled by the team; say so if you disagree: two minutes, fixed. The copy wins over a tip, and a first-run card wins over both. Enter does nothing on the line.</p>
  </div>

  <div class="prose blk">
    <h3>How we will know it worked</h3>
    <ul class="checks">
      <li>Copy a line with a link and a date, press the key: the line is there after the opening, and the system showed no “pasted” message. After Tab it shows once.</li>
      <li>No line for a password manager’s copy, for Booklight’s own, or for one older than two minutes.</li>
      <li>From Ctrl + C to a translation on the clipboard: four presses, no typing.</li>
      <li>The panel opens as fast as 2.1, and late frames stay at about 1 %.</li>
      <li>After a week on your HP the switch is still on.</li>
    </ul>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ M5
M5 = '<section class="ms" id="m5">' + mshead(5, "Flights", "Type a flight number and the row answers with the next flight.", "About 10 days · no new permission · the times need a free key of your own") + '''
  <div class="prose blk">
    <p class="notice">Your idea, in your words: “Let’s add a new M5 which handles flights. I want to type LH455 and get info about the next flight.”</p>
    <p><b>The story.</b> Someone you are meeting is on LH 455. You press the key and type <code>LH455</code>. A moment after the last letter the first row says: Lufthansa, San Francisco to Frankfurt, leaves 15:05, lands Friday 10:55, delayed 25 minutes, Terminal G, gate G4. Enter opens the flight’s page with its map. Tab, Tab, Enter keeps the flight on top of your windows, counting down. (Every time, gate and delay on this page is made up; the route is real.)</p>
    <p><b>The hard fact.</b> There is no free source of a flight’s planned or expected times that an app may call without a key; the research called every candidate''' + src("FL §1") + '''. What is free: which airline a designator belongs to (a small table Booklight can carry), a flight’s usual route (public tables, sometimes out of date), and where an aircraft is at this moment, but only while a volunteer’s receiver on the ground hears it. LH 455 itself was not to be seen on any of those feeds during the research. Times, terminal, gate and a real status are sold. Raycast’s flight extension asks each of its users for a key''' + src("FL §6") + '''; Apple and Google show such a card without one and do not say where the data comes from.</p>
    <p>So M5 has two layers. <b>For everyone:</b> the row names the airline and Enter opens the flight’s page. <b>With a key of your own</b> (AeroDataBox through RapidAPI: a free account, about 200 lookups a month): the row answers as in the story. Booklight ships no key: its code is public, and the service’s terms forbid a shared one''' + src("FL §2") + '''.</p>
  </div>

  <div class="prose blk">
    <h3>What is in it</h3>
    <ol class="feat">
      ''' + feat("A flight number is a row.", "Booklight reads <code>LH455</code>, <code>lh 455</code>, <code>LH0455</code> and the callsign <code>DLH455</code> as Lufthansa 455, from a table of airlines it carries (about 30 KB, in the public domain). The row says who flies it. Enter opens the flight’s page at FlightAware; the other actions are a web search, Ask Gemini, and “Get times”, which opens the place in Booklight’s window where a key is set up. Nothing leaves the device before Enter. This part needs no key and no switch.",
                 eg("<code>LH455</code>, Enter", "FlightAware’s page for Lufthansa 455, in your browser")) + '''
      ''' + feat("With your key, the row answers.", "A moment after the last letter (not after each one) Booklight asks the service once and the row is written in: who flies it and where; when it leaves and lands, each in its airport’s own time, with the day when that is not your today; the status in plain words; and where to go: the terminal and gate before it leaves, the arrival’s terminal and belt after. Which flight is “the next” is decision 12. A day after the number picks that day’s: <code>LH455 fri</code>, <code>LH455 tomorrow</code>. The answer is kept for two minutes, so opening the panel again does not ask again, and the footer says who gave it and when.",
                 eg("<code>LH455</code>", "“LH 455 · Lufthansa · San Francisco → Frankfurt. Leaves SFO 15:05, lands FRA Fri 10:55. Delayed 25 min, Terminal G, gate G4”")) + '''
      ''' + feat("What to do with it.", "Enter still opens the flight’s page, which has everything else: desks, the aircraft, the map. Copy puts one line on the clipboard to send to someone. Pin keeps it on top. The arrow opens three more as a list under the row: Add to calendar (your calendar’s editor with the flight as an event from take-off to landing), Search the web, Ask Gemini. All four stand in the row from its first frame; one that needs the answer waits for it, as an answer’s row does.",
                 eg("<code>LH455</code>, Tab, Enter", "“LH 455 San Francisco 15:05 → Frankfurt Fri 10:55, delayed 25 min, Terminal G, gate G4” on your clipboard")) + '''
      ''' + feat("Keep it on top.", "Pin puts the flight in the small window 2.0 built: the time to go, then the time to landing, then “Landed”. It counts down by itself. To stay true it has to ask the service again while it is pinned and the panel is closed: every 30 minutes, about twenty lookups for a long flight. That is sending without a key press, so it is part of decision 7.",
                 eg("<code>LH455</code>, Tab, Tab, Enter", "“LH 455 · lands Fri 10:48 · 6 h 20 min” on top of your windows")) + '''
      ''' + feat("Its usual route, without a key.", "For people without a key, behind a switch that is off until they turn it on: Booklight looks the number up in a public table and the row adds “Usually San Francisco → Frankfurt”. The table is free to use and was right for LH 455, but such tables disagreed on three of five flights compared, so the row says “usually” and the page behind Enter is the authority" + src("FL §1.3") + ". The table’s host asks to be told before an app relies on it; that letter comes first.") + '''
      ''' + feat("A flight in a copy.", "With M1 in, a flight number in something you copied is one of the things found: the line says “a flight”, and after Tab its row stands with the link and the date. Selected, it answers like a typed one.",
                 eg("Copy “Landing with LH 454 at 12:45”, the key, Tab", "The flight’s row first, then the event")) + '''
    </ol>
    <p><b>What looks like a flight and is not.</b> Nearly every pair of letters is some airline’s designator: <code>PS5</code> is Ukraine International 5, <code>MP3</code> is Martinair 3, <code>H264</code> is Sky Airline 64, <code>MS 365</code> is Egyptair 365''' + src("FL §4") + '''. So there are two kinds of match. A <i>strong</i> one (two letters that are not an everyday word, two to four digits, or the three-letter form) may be looked up after a pause. A <i>weak</i> one (a digit in the designator, a single digit, a word like AM, IN or OK, a year) is a row at the very end, and nothing is sent for it until you go to it: Down onto it, or Tab, and it is a flight’s row like any other. A flight’s row never stands above an app, a sum or one of your own links. The keyword <code>flight</code> (German <code>flug</code>) makes any of them a flight at once: <code>flight u2 8001</code>. The price of this rule is decision 13: Ryanair (FR), easyJet (U2), Wizz (W6), JetBlue (B6), Austrian (OS), Alaska (AS), Condor (DE) and Qatar (QR) are all weak.</p>
  </div>

  <div class="blk">
    <h3>The designs</h3>
    ''' + screen(12, "Type the number.",
                 shot("Light", "[[f1type]]", "Above: the answer. Line one is the number, who flies it and where. Line two is the two times, each in its airport’s own time, with the day when it is not your today. Line three is the status, in full ink, then where to go. Open is armed: Enter opens the flight’s page. The footer says who gave the answer and when. Below: the frame before. The row has its height, its three lines and its four actions from the moment it appears, and “Lands” already stands where it will stand, so nothing moves when the answer is written in. The white light’s slow lap, the sign of work, starts only if the answer takes longer than 600 ms: a quick one should not flash.")
                 + shot("Dark", "[[f1dark]]", "The same row on today’s softer highlight. A delay is said, not painted: no red, no warning mark. Red stays what it is in Booklight, the colour of an action that removes something.")
                 + shot("German, and the widest case", "[[f1de]]", "German says „Ab“ and „An“. Below it the widest this row gets in English: a twelve-hour clock, a day at both ends, a long airline and long city names. Line one is cut short when it must be; the times never are. With four actions the times have room whichever action is armed."),
                 "You type a flight number and row one is the flight. It is the Event row’s shape, which Booklight has had since 1.1: a small line, a line of named values, a small line. Nothing in it is new type, colour or height.") + '''
    ''' + screen(13, "What the row says, state by state.",
                 shot("Light · one row, eight states", "[[f2sheet]]", "From the top: planned; on time; delayed; boarding; in the air; landed; diverted; cancelled. Every disc is on x = 38, every text starts at x = 72, every right end is at x = 700, and the second time starts at one x in every row. “Leaves” becomes “Left” and “Lands” becomes “Landed” as they happen. Once it has left, the third line still says how late it is, and where to go becomes the arrival’s terminal. A flight that is diverted or cancelled keeps its times struck through, as a done task is: they are what was planned, not what will happen.")
                 + table(("The service says", "The row says"),
                         ("Unknown, or Expected with no new time", "Planned"),
                         ("Expected, with a new time that equals the plan", "On time"),
                         ("Delayed, or a new time later than the plan", "Delayed 25 min"),
                         ("CheckIn · Boarding · GateClosed", "Check-in open · Boarding · Gate closed"),
                         ("Departed · EnRoute", "In the air (with “18 min late” or “on time” from the new landing time)"),
                         ("Approaching", "Landing soon"),
                         ("Arrived", "Landed (with “12 min late” or “early”)"),
                         ("Diverted", "Diverted, the planned landing struck"),
                         ("Canceled · CanceledUncertain", "Cancelled · May be cancelled"), cls="k")
                 + '<p class="capt">The service’s thirteen states' + src("FL §2") + " and Booklight’s words for them. “On time” is said only when the service has given a new time and it equals the plan; otherwise the row says “Planned”. No answer of the service was seen in the research, so this table is from its description, " + NV + ".</p>",
                 "The row says each state in two or three plain words and never shows a field the service left empty.") + '''
    ''' + screen(14, "More, another day, and kept on top.",
                 shot("Light · the arrow opens three more", "[[f3more]]", "As an app’s row opens its places: the highlight leaves the row for the list, the row keeps only its arrow, turned over, and the line the highlight is on carries the Enter mark. One highlight.")
                 + shot("Light · another day", "[[f3day]]", "A day after the number. Each time carries its day, because neither is your today, and a flight that far ahead has no gate yet.")
                 + shot("Light · pinned", "[[f3pin]]", "The pin is 2.0’s small window at the timer’s size. Its figure is hours and minutes in words, because the timer’s figure beside it is minutes and seconds and “1:07” would read as 67 seconds. The small line is the number, where to go and the time; it is the part that changes when the service says something new. A pin can be up to 30 minutes behind the service."),
                 "Three things you do after the answer: something else with it, another day, keep it in sight.") + '''
    ''' + screen(15, "Without a key, and when it is not a flight.",
                 shot("Light · no key", "[[f4nokey]]", "Without a key the row is an ordinary 56 dp row and Enter opens the flight’s page. With the route switch on, a second line says where it usually goes. The last action, “Get times”, is how someone without a key learns that there is more: it opens the Flights group in Booklight’s window.")
                 + shot("Light · weak matches, and the keyword", "[[f4weak]]", "“ps5” is a search first; the flight is the last row and nothing was sent for it. It is the row most likely to look like a mistake, and it is the price of “type the number” needing no keyword (decision 13). Behind the keyword an airline with a digit in its designator is looked up like any other.")
                 + shot("Light · when there is no answer", "[[f4none]]", "Four plain states. The row keeps its three lines and its actions where they were, with the times empty, so nothing jumps when an answer does not come; Enter still opens the flight’s page, and Copy and Pin wait dimmed.")
                 + shot("Light · in a copy (with M1)", "[[f4copy]]", "The line says “a flight”. After Tab the flight’s row is one of the things found, and answers when it is selected.")
                 + shot("Light · the tip", "[[f4tip]]", "One new tip, in the tips’ own card. Tips pass over what you already use, so this one is for people who have never typed a flight number; the row’s own “Get times” is for those who have."),
                 "Most people will have no key, most text that looks like a flight is not one, and sometimes no answer comes. These are the states for that.") + '''
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>A key of Booklight’s own in the app (the code is public, and the service’s terms forbid it). A server of Booklight’s that holds one (you said “not yet” to a relay). “In the air now” from volunteers’ receivers: it shows nothing over oceans and for most short flights in Europe, which fly under callsigns that have nothing to do with the ticket’s number''' + src("FL §1.5") + '''. A map. A list of facts under the row (desks, the aircraft, the baggage belt before landing): the flight’s page has them. The flight after this one (a second lookup). Codeshares and numbers with two legs get no drawing yet: the row shows what the service answers first. Search by route (“SFO to FRA tomorrow”). Booking, prices, seats. Notifications when a gate changes: that needs something running in the background. A second service to choose from (FlightAware’s personal key fits the same field later). Airports as rows of their own.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>A reader for flight numbers in <code>core/</code>, with a test for every false friend in the research’s table. The airline table is an asset made by a script from a public-domain list, like the emoji table. A provider gives the row; it ranks under every local match. With a key, a strong match starts one request 400 ms after the last key; a new letter cancels it. The request, the answer’s reader and the two-minute memory are plain code beside the search suggestions, the only other network code Booklight has; the reader is tested against saved answers. The key is typed into the Booklight window, kept in the app’s private storage and out of its backup, and sent to that service only. The row is the Event row’s body with one more line. Add to calendar is the Event row’s action; the pin is one more kind of 2.0’s pin.</p>
    <p><code>PRIVACY.md</code> and the window’s text name the new recipients. The Play form’s answers stay as they are: it is the kind of data already declared for suggestions''' + src("FL §5") + '''. The footer says “AeroDataBox” and the time whenever an answer is on screen: its free plan asks for attribution. Whether that line is enough for its terms is ''' + NV + '''.</p>
    <p><b>About 10 days,</b> my estimate: the reader, the table and their tests 1; the row without a key and its three ways out 1; the request, the key’s field, the states and the memory 3; the three more actions and the day 1; the pin 1; the usual route and its switch 1; the copy’s row, the tip, the list of everything, German 1; a pass on both devices and the release build 1. The cheapest form that is still worth having is the first layer alone, about 2 days: it is the difference between “LH455” finding nothing and Enter opening the right page.</p>
  </div>

  <div class="blk">
    <h3>To check on a device first</h3>
    <p class="prose">None of the services was called with a key: there was none to call with. So every word about the answer is from the service’s own description, ''' + NV + '''.</p>
    ''' + table(("Check", "What", "If it goes badly"),
                ("F1", "With a real key: LH 455, a short flight in Europe, a codeshare, a number with two legs, one that landed two hours ago. How long an answer takes. Which fields come filled at a large and at a small airport; whether “revised” is what a traveller calls “expected”; what the reply says when the key’s lookups are used up", "The row shows less: the times without a gate. The design never shows an empty field"),
                ("F2", "Which flight the service calls nearest at 23:00 and just after a landing", "Booklight asks for a day itself: two requests where there was one"),
                ("F3", "The addresses in Chrome on a Googlebook: FlightAware’s page for DLH455, and whether an installed tracker takes the link", "Enter opens a web search for “LH455 flight status”"),
                ("F4", "Not in the half-day session: a day of your own typing with the reader on, in a build made for it. How often a flight’s row turns up for text that was not a flight", "The rule gets stricter: only after the keyword"),
                ("F5", "The row with the longest names and a twelve-hour clock, in English and German, on both devices", "City names give way to the three-letter codes in line one"),
                ("F6", "What the sign-up really asks for: whether the free plan wants a card", "The second service, FlightAware’s personal key, becomes the first"),
                ("F7", "The answer from the hosts of the public route table", "No usual route without a key; the row names the airline only"),
                ("F8", "What the system’s own “Track” action for a flight number opens on a Googlebook", "Nothing changes: Booklight does not use it")) + '''
  </div>

  <div class="prose blk">
    <h3>For you to decide</h3>
    <p><b>Your own key</b> (decision 4). The full answer needs an account with a flight data service: yours, and one for every other person who wants it. ''' + REC + ''' Yes: it is the only honest road to what you asked for, it is free at this size, and people without a key still get a row that opens the right page. If no: M5 is the first layer and the usual route, about 4 days, and the row never shows a time.</p>
    <p><b>When it asks</b> (decision 5). ''' + REC + ''' A moment after the last letter, for a strong match, once a key is in. That is what “type LH455 and get info” means, and it is how suggestions already work once switched on. If no: only on Enter; nothing is ever sent while you type, and the answer is one key press away.</p>
    <p><b>The usual route without a key</b> (decision 6) and <b>the pin</b> (decision 7) are each about a day and can each be left out. The pin is the one place where Booklight would ask the service with the panel closed.</p>
    <p class="muted small">Settled by me; say so if you disagree: times are shown in each airport’s own time; the status is words, never a colour; the answer is kept for two minutes; the light of work starts after 600 ms.</p>
  </div>

  <div class="prose blk">
    <h3>How we will know it worked</h3>
    <ul class="checks">
      <li>Type <code>LH455</code> with your key in: a moment after the last letter the row shows both times, the status and the gate, and they agree with the airline’s own page. How long that moment is, is check F1.</li>
      <li>Type <code>ps5</code>, <code>mp3</code>, <code>h264</code>, <code>q4 2026</code>: no flight in the first row, and nothing was sent.</li>
      <li>Without a key: <code>LH455</code>, Enter opens the right page. Nothing left the device before Enter.</li>
      <li>A pinned flight counts down, says “Landed” when it has, and never takes the keyboard.</li>
      <li>After a month of your own use the free plan’s lookups have not run out.</li>
    </ul>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ M2
M2 = '<section class="ms" id="m2">' + mshead(2, "The picture", "Capture a piece of the screen, open Booklight, and copy its text, ask about it or keep it on top.", "About 14 days · no new permission, one more Google library") + '''
  <div class="prose blk">
    <p class="notice">Your idea, in your words: “features which are based on taking a screenshot when booklight is engaged and then analyzing and asking about it (with Gemini nano)”.</p>
    <p><b>Not as you worded it.</b> Booklight can take the picture itself only as the device’s assistant or with an accessibility service''' + src("NF2 §2.2") + ''', and a whole screen is the picture the model read wrongly on the Lenovo''' + src("DF") + '''. So M2 is: you frame it, then open Booklight. The nearest thing to your wording without a new role is “capture from Booklight” through StudioSnap''' + src("NF2 §4") + ''', placed after M2. The silent capture is decision 5.</p>
    <p><b>The story.</b> A build fails in a terminal window. You capture that corner of the screen and open Booklight. The picture is the chip, and beside it stands the text Booklight read in it; Enter copies those characters. Or you type “what is the error, and where” and the answer is written above the picture, or you pin the picture on top while you type.</p>
  </div>

  <div class="prose blk">
    <h3>What is in it</h3>
    <ol class="feat">
      ''' + feat("A picture reaches Booklight, by two doors.", "From the clipboard, as M1’s line: “Copied just now · a picture”. Whether the Googlebook’s capture tool copies what it captures is " + NV + src("NF2 §2.2") + ". Or shared to Booklight, which opens the panel with the picture as its chip. Whether the capture’s preview offers Share is " + NV + " too; Android’s own preview offers Share and Edit, not a copy" + src("S&amp;C §2.2, also not verified") + ". Every example below has both forms.",
                 eg("Capture a region, the key, Tab. Or, if a capture is not copied: capture, Share, Booklight", "The picture as the chip, and its row under it")) + '''
      ''' + feat("Copy its text.", "A text recogniser on the device reads the exact characters, with no model (Google’s documentation; not run on either device: check 12), and shows them beside the picture before anything is copied. It is the first action, so Enter on a picture does the one thing that cannot be invented. “Use text” makes that text the chip, with everything M1 built: the link and the date in it, the translation, your prompts, an instruction.",
                 eg("Capture, the key, Tab, Enter. Or: capture, Share, Booklight, Enter", "The characters of the picture on your clipboard")) + '''
      ''' + feat("Ask about it.", "What you type is the question; the answer is a row above the picture. The promise is for a region or one window: a table’s cheapest row, a build error with its file and line, and the numbers in a terminal were read correctly on the Lenovo. A whole screen was not: the model made text up" + src("DF") + ". A picture too large for the model is asked through its text, and the caption says so.",
                 eg("With the picture as the chip, type <code>what is the error, and where</code>, Enter", "“Panel.kt uses a name, Metrics, that it cannot find. It is in line 42, column 17.”")) + '''
      ''' + feat("Pin it.", "The picture stays on top in the small window 2.0 built and never takes the keys. To take it down: <code>pin</code>, Enter.",
                 eg("With the picture as the chip: Tab, Tab, Enter", "The capture on top of your windows while you type")) + '''
    </ol>
  </div>

  <div class="blk">
    <h3>The designs</h3>
    ''' + screen(5, "Ask about what you framed.",
                 shot("Light · nothing typed", "[[s4idle]]", "The picture is the chip and row one. Beside it stand its size and the text the recogniser read, small and dimmer (“Reading…” until it is there). Copy text is armed: Enter copies those characters.")
                 + shot("Light · a question answered · plays", "[[s4done]]", "The first letter of a question makes room above the picture in one move; the picture rides down on the list’s spring and nothing fades and comes back. <b>The motion designer has not yet drawn this move: it is the part of this screen to review first,</b> and it is not played here. The answer gets a full-width row and names its source: “From the picture · on this device”. It is the only title-size text; the read text under it stays small.")
                 + shot("Dark, twice", "[[s4dark]]", "The highlight on the picture’s row, and the highlight moved off it so that the bright picture stands on bare dark glass. This is the state most likely to be too loud, and one of the things only you can judge on a device. The read text on bare glass over a white window is a device check too, as M1’s line is.")
                 + shot("If a capture is copied: M1’s line, with one more kind", "[[s4line]]", "No thumbnail: showing one would mean reading the copy, and the system would say “pasted” before you asked for anything. Whether this line ever shows depends on your own check (decision 3)."),
                 "You open Booklight after a capture and the picture is the chip. Its row shows the picture whole, in its own shape, never cropped and never larger than it is, with the text that was read in it beside it. You type a question and the answer is written above. The picture never shimmers or dims while the model works: the slow lap on the edge is the one sign of work. In light theme over a white window that light cannot be seen (accepted today, design system §12). What shows then is the row itself: the caption without “on this device”, and “Ask” in place of Copy, until the first word. Whether that is enough is for you to judge on the device.") + '''
    ''' + screen(6, "Every shape hangs from the same corner.",
                 shot("Light · the picture’s row, five times", "[[s5sheet]]", "From the top: 3 : 1 at 176 × 59, 2 : 1 at 176 × 88, 16 : 10 at 166 × 104, 1 : 1 at 104 × 104, 9 : 16 at 58 × 104. Every picture hangs from x = 24 and 16 dp under its row’s top edge; every text starts at x = 220; every right end is at x = 700. A flat picture leaves air under it and a narrow one leaves air before the text. That is the price of two edges that never move, and it is accepted.")
                 + shot("Light · three more states", "[[s5states]]", "A whole screen is too large for the model to read as a picture, which is where it made text up. So it is not asked as a picture: it is asked through its text, the caption says so, and the read text stands beside the answer. A picture with no text: Copy text and Use text go dim, Tab skips them, and Pin is armed. A picture that cannot be opened has nothing to run, so it gets no highlight: one plain line, as the quiet line is drawn."),
                 "This screen is the proof of alignment. The picture is the one loud thing in this plan, so its place is fixed by two edges that never move.") + '''
    ''' + screen(7, "Kept on top.",
                 shot("Light · a pinned capture", "[[s6desk]]", "The picture fills the small window edge to edge: no margin, no caption, no buttons, no ring. The window’s own corners and the system’s shadow are its frame. Drawn here at 420 dp wide, which needs a manifest entry of its own; without one it opens tiny. How large it opens is check 14, " + NV + ".")
                 + shot("Light · the sizes a pin opens at, with 2.0’s pins for scale", "[[s6sizes]]", "280 × 140 for a 2 : 1 picture is measured. The 16 : 10 size is an estimate.")
                 + shot("Dark · a shape the system will not take", "[[s6dark]]", "The system takes no window flatter than 2.39 : 1. A flatter picture is centred on the window’s ground, with bands.")
                 + shot("Light · the panel after <code>pin</code>", "[[s6pin]]", "In a 56 dp row a picture is a mark like every mark: 36 dp, on the same left edge as the chip above it, cut from the picture’s top-left corner so that a capture of text shows the start of its lines.")
                 + shot("Dark · the two fallbacks, if 176 × 104 is too loud on your device", "[[s6fall]]", "Not the plan: what comes, in this order, if the picture at full size is too loud on dark glass. Both keep the same text edges. In the first the picture is small and the text still starts at x = 220, which leaves a wide gap; that is accepted for a fallback."),
                 "You pin the capture and it stays on top while you work in another window. It can be the table you are typing from, so it is drawn from the picture’s own pixels at the window’s size and gets sharper when you make the window larger.") + '''
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>The photo picker and a keyword <code>pic</code> (built only if both doors fail: checks 9 and 10). Taking the picture silently when the panel opens (decision 5). Starting the capture from Booklight (a small release of StudioSnap, after M2). Saving the picture. A table as a grid, a spoken description, the QR code in a picture. Picking a colour. Several pictures at once (the first is taken, and the row says so). A way from a picture to the Gemini app, until someone has checked that the app takes one. A ready-made prompt <code>error</code>: with a picture, typing the question does it.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>A handed-over picture is copied into Booklight’s cache, decoded off the main thread and deleted when the panel or the pin closes; the cache is in no backup, because a capture can show other people’s mail. Share is one more intent filter on the panel. The clipboard door is M1’s line with one more kind. The model’s library already takes a picture. A size rule decides whether the picture or its text is asked (its limit is check 13). The model can still misread the recogniser’s text; that is why the read text stands beside the answer.</p>
    <p>The recogniser is Google’s on-device library, bundled so that it works at first use. The engineer read the library’s manifest and those of the seventeen Google libraries it pulls in: two permissions, and 2.1 already has both. The AndroidX ones were not followed, and a real build’s merged manifest is the last word''' + src("ENG §2.2") + '''. It adds about 5 to 6 MB to the Play download and about 13 MB on the device; the GitHub APK grows from 3.5 MB to about 13 MB if it is limited to the two Googlebook architectures and packed compressed. <code>PRIVACY.md</code> gets a paragraph “Pictures”.</p>
    <p><b>About 14 days</b> (12 to 16); about 10 without the recogniser. Sized with three doors; the picker’s day is out now: not re-sized. The cheapest form is about 12 days: every one of Copy text, Ask and Pin is a third of the promise. Riskiest: the way in, with two doors not verified.</p>
  </div>

  <div class="blk">
    <h3>To check on a device first</h3>
    ''' + table(("Check", "What", "If it goes badly"),
                ("9", "Capture a region, then Paste: does the picture come? The first half is yours (decision 3)", "No line for captures; Share is the door"),
                ("10", "Does the capture’s preview offer Share, and is Booklight in it?", "With 9 failing as well: build the picker and <code>pic</code>, and swap M2 and M3"),
                ("12", "The recogniser in a release build: its speed, small interface text on the HP", "Copy text may not deserve to be first on the HP"),
                ("13", "A dozen pictures with smaller and smaller text, asked of the model", "The size rule’s limit moves"),
                ("14", "How large a pin of a picture opens, and how far it can be dragged", "“The table you are typing from” becomes “a small region”"),
                ("1", "The model on the HP", "No Ask there. Copy text and Pin are then all of M2 on the HP, and the recogniser is a must")) + '''
  </div>

  <div class="blk">
    <h3>For you to decide</h3>
    ''' + table(("Decision", "Recommended", "If you say the other thing"),
                ("<b>3 · Your own check:</b> capture a region on your Googlebook, then try Paste somewhere. Does the picture come? Look at the same time whether the capture’s preview has Share", "Do it now. It also decides whether screen 1 has a picture state", "Not copied: Share is the door. Neither: the picker is built and M3 goes before M2"),
                ("<b>4 · The text recogniser.</b> One more Google library, with the usage reporting you accepted for the model’s library. No new permission", "Yes. Without it nothing M1 built works on a picture, and a whole screen gets only “too large”", "No Copy text, about 10 days. The research suggested regions first and deciding later"),
                ("<b>5 · Silent capture as the device’s assistant.</b> It replaces Gemini on its key and keeps a service of Booklight’s running always", "Not in these four, and not ruled out. Look again after M2 has been used for a month", "Not sized. It breaks “nothing in the background”. It is also the only road to Chrome’s “New tab”")) + '''
  </div>

  <div class="prose blk">
    <h3>How we will know it worked</h3>
    <ul class="checks">
      <li>The three test pictures from the Lenovo (a table, a build error, a piece of a terminal) are answered correctly through the real flow, not a debug hook.</li>
      <li>A whole screen is not asked as a picture: it is answered from its text, with the read text beside the answer, or the row says plainly that it cannot read it.</li>
      <li>Copy text gives the exact characters of those three pictures, in three presses after the capture.</li>
      <li>A pinned picture stays on top and never takes the keyboard.</li>
      <li>The manifest has no new permission, and the release build does all of the above.</li>
    </ul>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ M3
M3 = '<section class="ms" id="m3">' + mshead(3, "The selection", "Select text where you are writing, right-click, and Booklight rewrites it in place.", "About 8 days · no new permission") + '''
  <div class="prose blk">
    <p class="notice">Not one of your three ideas. The team proposes it as the step after the copy: the same rows, opened from where you write, with the answer put back there. It is the research’s ideas 5, 4 and 6''' + src("NF2 §1") + '''.</p>
    <p><b>The story.</b> You are writing a mail and the sentence is not right: a typo, the wrong tone, no umlauts. You select it, right-click and pick “Fix spelling”. The panel opens with the answer already being written. Enter puts it where the sentence stood: two clicks and one key.</p>
    <p>“Where you are writing” means: in apps that show other apps’ entries in their selection menu. Which do on a Googlebook is ''' + NV + ''' (check 15). Replace itself has never been tried on a device either (check 16). If it lands nowhere, M3 has no promise. Both checks are in the first device session, before M1.</p>
  </div>

  <div class="prose blk">
    <h3>What is in it</h3>
    <ol class="feat">
      ''' + feat("Ready-made prompts in the selection menu, by name.", "“Fix spelling” and “Translate” stand in the menu from the start; Shorter, Formal, Friendly, Put the letters back and Explain can be switched on in the window. The model is asked the moment you pick. Replace is the first action in a field you can type in, and it is built to work for several paragraphs with their line breaks; that is " + NV + ". On a page you can only read there is no Replace and Copy is first. Nothing is ever put back without your Enter.",
                 eg("Select “i think we shoud meet on friday becuase the room is free”, right-click, Fix spelling, Enter", "The corrected sentence where the old one stood")) + '''
      ''' + feat("Put the letters back.", "A sentence typed without its umlauts or accents gets them back, in place. Booklight checks that only letters changed and shows “Could not do it without changing your words” otherwise, so it cannot rewrite you. Typed, it lives under <code>abc</code>; a language may go first: <code>abc danish jeg kommer pa lordag</code>.",
                 eg("Select “Gruesse aus Koeln, ich komme spaeter” in your mail, right-click, Booklight, “Put the letters back”, Enter", "“Grüße aus Köln, ich komme später” where the sentence stood")) + '''
      ''' + feat("Two more ready-made prompts:", "<code>formal</code> and <code>friendly</code> (German <code>förmlich</code>, <code>freundlich</code>).",
                 eg("Select, right-click, Booklight, type <code>formal</code>, Enter, Enter", "The selection in a formal tone, put back where it stood")) + '''
    </ol>
    <p>The menu’s “Translate” is M1’s translation behind a new door: it knows its direction, and a typed language changes it.</p>
  </div>

  <div class="blk">
    <h3>The designs</h3>
    ''' + screen(8, "Right-click, Fix spelling.",
                 shot("Light · the other app’s menu", "[[s7menu]]", "Booklight only gives the names, and a name is fixed when the app is built" + src("ENG §3") + ". So the translation’s entry is “Translate”, not “In German”, and a prompt you wrote stays behind “Booklight”.")
                 + shot("Light · the panel over the mail · plays", "[[s7live]]", "The panel opens as always, with the chip in the field from the start. The answer’s row comes only when the glass is 85 % open, even if the answer is already there, and the slow lap starts then, not before. The row shows your sentence first, dimmer, then the answer written over it. Over this white mail the light cannot be seen in light theme; press Dark to see it.")
                 + shot("Light · after Enter", "[[s7after]]", "“Replaced” and its check, where “Copied” stands today. Then the panel folds after its usual hold.")
                 + shot("Dark", "[[s7dark]]", "This row on the softer highlight is what M3’s user sees most."),
                 "You pick “Fix spelling” in the other app’s menu and the panel opens over your mail with the sentence as its chip and the answer on its way. Nothing in the panel is a new element. The design work is the order of events when an answer is already coming: nothing under the field before the glass is open. Replace is armed first in a field you can type in.") + '''
    ''' + screen(9, "Put the letters back.",
                 shot("Light · from a selection, typed, the refusal, one word", "[[s8]]", "The answer is not shown while it arrives: it is checked first, then written in. A wrong one is never shown, not for one frame; the row then keeps your own text and offers the way to the Gemini app, as every row does when there is no answer here" + src("ENG §7.2") + ". One word takes the same road as a sentence, because a blind swap would turn “neue” into “neü”."),
                 "From a selection it is one more row, and Enter puts the sentence back with its letters. Typed, it is <code>abc</code> and the sentence: an answer row stands in place of the letter grid.") + '''
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>A prompt you wrote under its own name in the menu (not possible). “In German” as a menu name. Pasting into a field that does not take the answer back (power pack). Snippets that expand as you type (ruled out). <code>reply</code>, <code>commit</code>, <code>define</code>, <code>error</code> (you can add them under Yours). <code>abc</code> by keystrokes. A one-word swap without the model.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>Each menu entry is declared in the manifest, pointing at the panel, with its own fixed name, and off. The window switches one on or off; no permission is involved. The panel sees which entry was picked, takes the text, and asks that prompt at once; the model is warmed as the panel is created. Replace and “this text cannot be edited” exist today; Replace moves to the front, and several paragraphs become replaceable through the shared piece that carries the material beside the field.</p>
    <p>The letters check walks both texts: a letter with a mark must stand where one of its plain writings stood, and every other character must be the same. It is the one model feature a unit test can hold, with about sixty pairs.</p>
    <p><b>About 8 days:</b> the engineer’s 10 (8 to 12), less 1.5 for the typed language and <code>tr</code>, which went to M1, and less half a day for <code>error</code>. The cheapest form is about 5 days: two entries, Replace first, several paragraphs, and the letters for German. Riskiest: the menu and Replace in the apps you write in. The engineer found no note that Replace was ever tried on a device.</p>
  </div>

  <div class="blk">
    <h3>To check on a device first</h3>
    <p class="prose">Checks 15 and 16 belong to the first session, before M1: they decide whether this milestone holds at all. Also ''' + NV + ''': that the other app’s Ctrl + Z undoes a Replace in one step.</p>
    ''' + table(("Check", "What", "If it goes badly"),
                ("16", "Replace: does the answer land in the selection, in Chrome, Gmail, Docs, Keep and the text editor; with several paragraphs", "Replace also copies, or Copy is first in that app. If it lands nowhere, M3 has no promise"),
                ("15", "Where the entries show; how many of one app before the menu folds them; their order; by right-click and by touch", "One entry, “Booklight”, with the two prompts first in its rows: one key more for the same promise"),
                ("17", "How soon the menus follow a switch", "An entry may show late after its switch, and the window has to say so"),
                ("18, its first half", "Fifty sentences without their letters in German, Danish and French", "A language that passes fewer than about four in five is not offered")) + '''
  </div>

  <div class="prose blk">
    <h3>For you to decide</h3>
    <p><b>How many entries stand in the menu from the start</b> (decision 6). ''' + REC + ''' Two: Fix spelling and Translate. The rest are off until you switch them on. The other answers: one (“Booklight” only, as today, and every prompt is one key more) or all seven.</p>
    <p class="muted small">Settled by the team: “put the letters back” lives under <code>abc</code>. <code>formal</code> and <code>friendly</code> come ready-made; <code>reply</code>, <code>commit</code>, <code>define</code> and <code>error</code> do not.</p>
  </div>

  <div class="prose blk">
    <h3>How we will know it worked</h3>
    <ul class="checks">
      <li>In Chrome, in a mail being written and in the text editor: select, right-click, “Fix spelling” is there by name, and Enter replaces the selection. Three paragraphs keep their line breaks.</li>
      <li>On a page that cannot be edited the answer is shown and copied, and no Replace is offered.</li>
      <li>Fifty test sentences in German, Danish and French: each comes back with the right letters or not at all. Never with a changed word.</li>
      <li>The menu holds exactly the entries that are switched on.</li>
    </ul>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ M4
M4 = '<section class="ms" id="m4">' + mshead(4, "Your language", "Booklight speaks six more languages: its screens, its keywords and its dates.", "About 17 days and the wait for readers, after 12 days of groundwork · no new permission") + '''
  <div class="prose blk">
    <p class="notice">Your idea, in your words: “language support for e.g. more languages including French, Spanish, Italian, Japanese, Korean, mandarin, danish, swedish, Norwegian, Polish”.</p>
    <p><b>Six of your ten.</b> Polish, Japanese, Korean and Mandarin are not in these four (decision 7).</p>
    <p><b>The story.</b> Your Googlebook is set to French. Booklight’s screens are in French, <code>ven 15h dentiste</code> makes an event, and the keywords are words you would type. The English keywords still work. Where the model on the device writes your language well it answers in the row; where it does not, the row hands over to Gemini and says so before Enter.</p>
  </div>

  <div class="prose blk">
    <h3>What is in it</h3>
    <p>It is one thing, six times: French, Spanish and Italian first, then Danish, Swedish and Norwegian. These are its parts.</p>
    <ol class="feat">
      ''' + feat("The six languages as deep as German is today:", "the screens and the typed words (keywords, verbs, dates and times, the words for settings pages and shortcuts).",
                 eg("<code>ven 15h dentiste</code>; in Danish <code>fre kl. 15 tandlæge</code>", "The Event row, filled in, in your language")) + '''
      ''' + feat("Dates and numbers the way the language writes them:", "“dans 20 min”, “kl. 15.30”, 1 234,5.") + '''
      ''' + feat("The prompts where the model passed a test on the device,", "and a hand-over to the Gemini app where it did not. The row looks like any other row; its right end says “Gemini” where it would say “On this device”.",
                 eg("In a Danish Booklight where the model failed: copy “jeg kommer pa lordag”, the key, Tab", "“Ret stavning” with “Gemini” at its right. Enter opens the Gemini app with the prompt and the text, not sent")) + '''
      ''' + feat("One choice in the window, “Translate into”:", "your other language, used when a text is already in yours. Until M4 it is fixed (German in an English Booklight, English otherwise), and that guess is wrong for most people in these six.",
                 eg("In a French Booklight, set “Translate into” to Spanish, then copy a French sentence, the key, Tab", "The row “En espagnol”, where it said “En anglais”")) + '''
      ''' + feat("The store page", "in each language, written to fit Play’s limits, not translated.") + '''
    </ol>
  </div>

  <div class="blk">
    <h3>The designs</h3>
    ''' + screen(10, "The same panel, in your language.",
                 shot("Light · the line", "[[s10line]]", "One state, three times, on one left edge, so the eye sees which lines grow.")
                 + shot("Light · the list after Tab", "[[s10list]]", "The copy is in each language. The titles of found things do not grow; the names of prompts and the labels at the right do, and they have room.")
                 + shot("Light · the same event, typed the way each language writes it", "[[s10event]]", "The same event four times: Friday, three to four. English and German are today’s app. French and Danish show what M4 adds: the words for the day, “15h” and “kl. 15”, and the date written the language’s way."),
                 "You see the panels of M1 again, in your language. No new element and no new motion. The French and the Danish are drafts for the native readers, and are marked so.") + '''
    ''' + screen(11, "Where long words go.",
                 shot("A place’s name in the armed slot · 12 characters", "[[s11a]]", "The three placements today’s polish leaves on the row. The other places stand in the list under the row, where a name has 600 dp.")
                 + shot("Any other action’s name · 16 characters", "[[s11b]]", "The word is chosen to fit. The type never shrinks and the row never gains a line.")
                 + shot("A tip · 90 characters, its two answers 18 each", "[[s11c]]", "The places tip, written shorter in French. The longer answer still fits beside two lines, and it is over its limit, so the reader shortens it.")
                 + shot("A chip’s name · 24 characters", "[[s11d]]")
                 + shot("The quiet line · 80 characters", "[[s11e]]", "From three things on the line names two and says “and more”: things are dropped from the end, never cut mid-word.")
                 + shot("An answer’s caption · the first part 32 characters", "[[s11f]]", "“On this device” is the part that goes when both do not fit, because it is the same on every row. Beside the widest strip there is, both still fit in this draft.")
                 + shot("A footer hint · 28 characters", "[[s11g]]", "The footer stays flush right at 700. Its caps move left as the words grow, and “échap” is a wider cap than “esc”.")
                 + shot("A menu entry · short forms", "[[s11h]]", "A menu cuts long names, and it is the system’s to draw.")
                 + shot("A prompt the model failed in", "[[s11i]]", "The same three rows of a list. Where the device’s model did not pass, the label at the right is “Gemini” where it would say “On this device”, and the action is the hand-over. They must look like normal rows, not like errors."),
                 "One sheet: each component at its limit, English above, the longest language under it. The face has every letter the six need by Google Fonts’ list, so no row changes height; the device’s own file is " + NV + src("LANG §14") + ". A test fails the build when a word does not fit.") + '''
    <p class="prose">The picture book for the native readers is made from captures of the real app, not from drawings: a drawing would hide the clipping the reader is there to find. It has about fifty screens: the research’s forty and about ten for M1 to M3.</p>
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>Polish (three plural forms, day names that change with grammar, <code>w</code> is a word: its own release next). Japanese, Korean and Chinese (a second block and a different kind of work). Store pictures per language. Smaller type or a second line to make a long word fit.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>Before M4 the groundwork is done: the typed words live in one file per language, numbers and dates follow the locale, and a test fails the build when a language’s keyword list is missing or collides. M4 is then the languages themselves: a word set and its test, about 460 strings, 50 keyword lists, the list of everything, the tips, the settings and shortcut words, per language. What M1 to M3 added is small here (about sixty strings, three keywords, two prompts), because ready-made prompts are kept by reference since M1. The model is tested per language on the Lenovo with five prompts on a fixed text, and a list says where the device may answer.</p>
    <p><b>About 17 days</b> (15 to 19) and the wait for six readers: 11.5 for the six languages, 2 for the model tests, 2 for the picture books and what the readers find, 1 for store pages and notes, half a day for M1 to M3’s words. The first step (French, Spanish, Italian) is about 8 days. The day figures are estimates from counts, “not from having done one”''' + src("LANG §14") + '''. Riskiest: keywords nobody would type, and the model in the Nordic languages.</p>
  </div>

  <div class="blk">
    <h3>To check on a device first</h3>
    <p class="prose">''' + NV + ''' besides: the model in any of the six (only German was tried on the Lenovo; Danish, Swedish and Norwegian are on none of Google’s lists: LANG §5, §14); Play’s language codes; where Googlebooks are sold, which should set the order.</p>
    ''' + table(("Check", "What", "If it goes badly"),
                ("19", "German on a device on the new structure. German was never checked on a device before", "The groundwork is not done"),
                ("20", "Per language: five prompts on one text; the Settings app’s page names", "That language’s prompts hand over to Gemini"),
                ("None yet", "Ctrl + 1…9 on a French keyboard, where digits need Shift", "For M4’s device pass"),
                ("None yet", "Whether the system tells Danish from Norwegian", "The worst case is a translation offered into the language the text is in. The row names its direction")) + '''
  </div>

  <div class="blk">
    <h3>For you to decide</h3>
    ''' + table(("Decision", "Recommended", "If you say the other thing"),
                ("<b>7 · Which languages, and how.</b> You named ten", "These six, in two steps. Polish next as its own release. Japanese, Korean and Chinese as a later block", "Polish in M4 adds about 3 days. Japanese, Korean and Chinese: about 9 days of one-time work and about 18 for the languages"),
                ("<b>8 · German words in an English Booklight.</b> After the groundwork the date reader knows the app’s language and English, so “morgen 9 Uhr” would stop working in an English Booklight. Your own examples are German", "Keep German for you: one setting, about a day", "“morgen 9 Uhr” stops in an English Booklight. It saves a day"),
                ("<b>12 · Who reads each language.</b> A machine draft, then one native reader per language", "A reader you know where you have one; paid, about USD 260 a language, where you do not", "Unread drafts ship. A wrong keyword here is a broken feature, not a typo"),
                ("<b>13 · Suggestions in the user’s language.</b> The language goes to the search engine with the text, and the privacy text gains a clause", "Yes", "Suggestions stay as they are today, in every language"),
                ("<b>14 · Your rule “every string in English and German”.</b> After M4 the two stay the rule for building; the other six may trail by one release", "Yes", "Every release waits for all eight languages and their readers")) + '''
    <p class="prose muted small">Standing at the research’s recommendation: the model is offered in a language only where the device test reads well to that language’s reader. English pictures under translated store text.</p>
  </div>

  <div class="prose blk">
    <h3>How we will know it worked</h3>
    <ul class="checks">
      <li>In every language each example of the list of everything runs.</li>
      <li>One native reader per language has seen the picture book and said what he would type for twenty things.</li>
      <li>Nothing is cut off: every limit of screen 11 holds, and the store text is within 4,000 characters.</li>
      <li>There is a table of the five prompts per language from the Lenovo; where a language failed, its rows hand over to Gemini.</li>
      <li>English and German behave as before.</li>
    </ul>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ what comes next
NEXT = '''
<section id="order">
  <h2>The order, and what you get when</h2>
  <ol class="plan prose">
    <li><span><b>2.2: what is on main,</b> released when you say so (decision 2). M1’s line and list are drawn on its softer highlight and shorter strip.</span></li>
    <li><span><b>One device session,</b> half a day, both devices. For M1: checks 1, 3 to 8, the translation half of 18, and the line in dark over a white window. For M5: checks F1 to F3, F5, F6 and F8. Some need only the released app; the rest a throwaway build on a branch. It needs your HP awake and attached, and for F1 a key.</span></li>
    <li><span><b>M1 · The copy,</b> released as 2.3. It builds the pieces M5’s row in a copy stands on: the line, the chip and its rows, an answer where it stands.</span></li>
    <li><span><b>M5 · Flights,</b> released as 2.4. Its first layer (the row, and Enter opens the page) is about two days and needs nothing from M1: it can ride in 2.3 if you want it sooner.</span></li>
  </ol>
  <p class="prose"><b>Beside these: the settings window.</b> Its build started today on a branch of its own, on your word, with the nine recommended answers of its plan. It ships alone once it is built and you have seen it; neither milestone waits for it.</p>
  ''' + table(("From", "It adds to the window"),
              ("M1", "One switch: “What you copied”"),
              ("M5", "One group, “Flights”: the field for your key with two lines on how to get one, and the switch for the usual route")) + '''
  <p class="prose"><b>After these two,</b> the parked three come back in the order they had (the picture, the selection, your language), unless you say otherwise. Nothing in M5 changes them.</p>
</section>

<section id="decisions">
  <h2>Your decisions, in one list</h2>
  <p class="prose">Each can be answered in a word. The recommended answer is what happens if you say only “go”. Under each: what the other answer costs.</p>
  <ol class="qs prose">
    ''' + q("All · This plan: 2.2, the device session, M1, then M5. The picture, the selection and your language stay parked.", "<b>Yes</b>", "If no: name what comes back in, or what goes first. M5’s first layer and M1 do not depend on each other.") + '''
    ''' + q("2.2 · Release what is on main now, as 2.2: a GitHub release and a draft on Play’s closed testing. Play’s production stays empty.", "<b>Yes</b>", "If no: it waits, and goes out with the settings window or with M1.") + '''
    ''' + q("M1 · A line under the empty field for something you just copied, with a switch to turn it off.", "<b>Yes</b>", "If no: the empty panel stays empty, and Tab there opens the copy.") + '''
    ''' + q("M5 · The times, the gate and the status come through a key of your own: a free account with AeroDataBox through RapidAPI, about 200 lookups a month. Everyone else who wants them makes their own.", "<b>Yes</b>", "If no: the row names the airline (and the usual route), Enter opens the flight’s page, and no time is ever shown. About 4 days.") + '''
    ''' + q("M5 · Booklight asks the service a moment after the last letter, for a strong match, once a key is in.", "<b>Yes</b>", "If no: only on Enter. Nothing is sent while you type; the answer is one key press later.") + '''
    ''' + q("M5 · The usual route for people without a key, from a public table, behind a switch that starts off, after its host has been asked.", "<b>Yes</b>, and the first thing to cut", "If no: without a key the row names the airline only. It saves about a day and a letter.") + '''
    ''' + q("M5 · Pin a flight: the time to go, then to landing, on top of your windows. A pinned flight asks the service again every 30 minutes with the panel closed, about twenty lookups for a long flight.", "<b>Yes</b>", "If no: later, and it saves about a day. Or yes without asking again: the pin counts down from what it knew when you pinned it, and may be wrong by the time it lands.") + '''
  </ol>
  <h3 class="gap">Six smaller ones</h3>
  <p class="prose">Each changes something that is yours: a rule of yours, the released app, or the privacy text.</p>
  <ol class="qs prose" start="8" style="counter-reset: q 7">
    ''' + q("M1 · The translation stands first for a text not in your language, above what was found in the copy.", "<b>Yes</b>, as the one exception", "It bends two rules: the 2.0 design’s “a model’s rows never outrank a local match”, and “the order is fixed”. If no: it stands after the found things, and the story is Tab, Down up to three times, Enter, Enter.") + '''
    ''' + q("M1 · <code>clip</code> shows fewer rows. Note, Mail, QR code, UPPERCASE, lowercase, Title Case, On one line, Counted, URL-encoded, Search and Ask Gemini are found by typing their name.", "<b>Yes</b>", "If no: those eleven rows stay in the list, under the new ones.") + '''
    ''' + q("M1 · Shared text behaves the new way in the released app: the chip stays the text and the row is answered in its place.", "<b>Yes</b>", "If no: shared text keeps today’s way (the chip becomes the prompt), and the same rows behave in two ways.") + '''
    ''' + q("M5 · Enter on a flight opens its page at FlightAware. In the research its address answered with the right flight every time when built from the airline’s three-letter code; FlightStats’ did too.", "<b>Yes</b>", "The other answers: FlightStats, or a search with your engine for “LH455 flight status”. The search is in the row’s list either way.") + '''
    ''' + q("M5 · Which flight is “the next”: the one in the air now; else one that landed in the last three hours; else the next to leave.", "<b>Yes</b>", "If you meant strictly the next to leave: after a landing the row shows tomorrow’s flight, and someone waiting at arrivals has to type the day.") + '''
    ''' + q("M5 · A flight’s row never stands above an app, a sum or a link of yours, and text that only looks like a flight is the last row and sends nothing until you go to it. The price: Ryanair, easyJet, Wizz, JetBlue, Austrian, Alaska, Condor and Qatar numbers are “only looks like”, and need Down or the keyword.", "<b>Yes</b>", "Looser: every known designator is looked up after a pause, and “ps5” or “q4 2026” goes to the service. Stricter: a flight only after the keyword <code>flight</code>, and then “type LH455” needs it too.") + '''
  </ol>
  <div class="prose blk">
    <p><b>Not a decision, a notice:</b> with M5 the privacy text names who gets a flight number: the service your key belongs to, and the host of the route table if that switch is on. Play’s form does not change.</p>
    <p><b>Three questions I have.</b> One: by “the other improvements your new plan had” I took the 2.2 list above, and M1 with everything the four-milestone plan put in it. If you meant something else, say what. Two: will you make the account and paste a key? Check F6 is whether the free plan asks for a card; I could not see that from outside. Three: can the HP be attached for half a day? It has never run 2.0.</p>
    <p><b>Five things only you can judge, on a device,</b> before the designs are final. The line at 320 ms and at 200 ms after the opening (the playing panel of screen 1 has both). The line in dark over a white window. Whether a flight’s status stands out enough as the news of its row: it differs from the rest of its line only by full ink, and could be set heavier. Whether „AB SFO“ and „AN FRA“ read as words or as a run of codes in German. And whether the “ps5” row is quiet enough to keep.</p>
    <p><b>After “go”</b> comes 2.2 if you said yes, then the device session and its results, then the designs made final on them, then the build of M1.</p>
  </div>
</section>
'''

# ------------------------------------------------------------------------------------------------ parked
PARKED = '''
<section id="parked">
  <h2>Parked: the picture, the selection, your language</h2>
  <p class="prose">M2, M3 and M4 as the four-milestone plan had them, with their designs. Nothing in them changed. Their screens keep their numbers, 5 to 11, and their decisions keep the numbers of that plan’s list, which follows them.</p>
</section>
'''

# ------------------------------------------------------------------------------------------------ the rest
OLD_REST = '''
<section>
  <h2>The four-milestone plan: its order, and why</h2>
  <ol class="plan prose">
    <li><span><b>Today’s polish,</b> released on its own. M1’s line and list are drawn on its softer highlight and shorter strip, not on 2.1’s.</span></li>
    <li><span><b>One device session,</b> half a day, both devices: checks 1 to 11, 15, 16 and the translation half of 18. Five need only 2.1 as released; the rest need a throwaway build on a branch. It settles M1, the way a picture gets in, and whether M3’s promise holds, so the order of the four is known before the first is built. It needs your HP awake and attached.</span></li>
    <li><span><b>M1 · The copy.</b> It is useful on the first day without learning a keyword, most of it needs no model, and it builds what M2 and M3 stand on: a line for something you did in another app, the chip and its rows, an answer where it stands, the translation and its direction.</span></li>
    <li><span><b>M2 · The picture,</b> with the middle of the groundwork built beside it; they touch different code. It is your own first idea. And if the HP has no model, the picture is the milestone that still works there: Copy text and Pin need none.</span></li>
    <li><span><b>The groundwork, as a quiet release.</b> German on the new structure, on a device, before anything is built on it.</span></li>
    <li><span><b>M3 · The selection.</b> Its new keywords are then born in the per-language files.</span></li>
    <li><span><b>M4 · Your language.</b> Last, because afterwards every new keyword, tip and release note is written once per language and read by someone who speaks it. Before M4, everything M1 to M3 added is translated once.</span></li>
  </ol>
  <p class="prose"><b>Beside these seven: the settings window.</b> It ships alone as soon as you have approved it and it is built: any time, and with M2 at the latest. Its page structure must be final before M3. “Stop what is ringing” (a repair: Booklight starts timers and cannot stop one) has no owner yet; the proposal is that it rides in that release.</p>
  <p class="prose"><b>One swap rule.</b> If a capture is not copied and its preview has no Share (checks 9 and 10 both fail), and the menu and Replace work (checks 15 and 16 pass): M3 goes before M2. Nothing in M3 needs M2.</p>
  ''' + table(("From", "It hands on", "To"),
              ("Today’s polish", "The quieter highlight; the shorter strip", "M1’s line and list"),
              ("M1", "The line; the chip and its rows; an answer where it stands; the translation, its direction and <code>tr</code>; the six shared pieces", "M2 (a picture in the same line, its text in the same list), M3 (the same rows behind a new door)"),
              ("M2", "Pictures handed to Booklight; the recogniser", "Later ideas (a colour from the screen, capture from Booklight)"),
              ("The groundwork", "Words per language; the keyword test", "M3 (its new keywords), M4"),
              ("M3", "The first test of the model’s letters in other languages; the final list of prompts and keywords", "M4 (translated once)"), cls="k") + '''
</section>

<section>
  <h2>The four-milestone plan: the tracks beside the four</h2>
  <div class="prose blk">
    <p><b>The settings window.</b> You said it “looks very weird on a Googlebook”. It is being redesigned by a separate designer as a desktop-ready Material 3 Expressive window with its navigation at the left edge, and you approve it separately. It is a visible flaw in the released app, not a feature waiting for a story, so it does not wait for a milestone. It changes the window and the milestones change the panel, so neither waits for the other. Every page must be final before M4’s picture book is made. What the milestones add to it is small, and each is “a title, one line, a switch”, a row every version of the window has.</p>
  </div>
  ''' + table(("From", "It adds to the window"),
              ("M1", "One switch: “What you copied”"),
              ("M3", "One switch per ready-made prompt: “In the selection menu”"),
              ("M4", "One choice: “Translate into”. And the setting for German words, if you say yes to decision 8"), cls="k") + '''
  <div class="prose blk">
    <p><b>Languages, in two parts.</b> The groundwork is about 12 days (10 to 14), and nothing changes for the user: the typed words move out of the code into one file per language, numbers and dates follow the locale, ø æ ł ß are matched, a test checks every keyword list. It is built in three parts so that it never collides with a milestone: half a day between M1 and M2 to move the word lists; the date reader and the number formats beside M2; the parts that touch the panel’s strip and field after M2. It cannot be cut. With it comes one rule, which has no size yet: a date in a thing you brought is read in the text’s own language, so a German mail copied into an English Booklight keeps its event. The languages themselves are M4. After it: Polish, then the block of Japanese, Korean and Chinese.</p>
    <p><b>Today’s polish</b> is not planned here. This plan takes it as given: medium opening speed, the later white reflection, three placements by default, an app’s own shortcuts as actions, the softer highlight in dark.</p>
  </div>
</section>

<section>
  <h2>Not in the four, and why</h2>
  ''' + table(("Idea", "Why not now"),
              ("<b>Say it plainly, a guess when nothing matches, settings in plain words, emoji by description</b>", "One real story and the best candidate for a fifth milestone. It is the largest model work, and it takes one to two seconds for something that looks instant"),
              ("<b>Pick a colour from the screen</b>", "Close to M2’s story but another mechanism, with one device check open. The first thing to add after M2"),
              ("<b>Capture from Booklight</b> (<code>shot area</code> through StudioSnap)", "The nearest thing to “a screenshot when Booklight is engaged” without a new role. A small release of StudioSnap, in its repo, after M2"),
              ("<b>Text tools</b> <code>b64</code>, <code>jwt</code>, <code>sha256</code>, <code>epoch</code>, <code>json</code>", "Useful and small, but they share nothing with the four"),
              ("<b>Stop what is ringing</b>", "A repair, not a feature, and nobody owns it yet. Proposed: in the settings window’s release"),
              ("<b>Tasks out of a text</b>", "Typing “pull out the tasks” under a copy does the reading. Adding them to Todo.md is a notes story"),
              ("<b><code>error</code>, <code>reply</code>, <code>commit</code>, <code>define</code>; <code>abc</code> by keystrokes</b>", "You can add the prompts under Yours. The lists stay short. <code>error</code>’s own example was a copied line, and on a picture typing the question does it"),
              ("<b>Units, money, time zones</b>", "It needs Summa’s engine inside Booklight. A milestone of its own"),
              ("<b>A key per command</b>", "One device check is open, and it is about opening Booklight, not about the four stories"),
              ("<b>What other apps can do with a thing; packs; your people</b>", "Each is the start of another story"),
              ("<b>Polish; Japanese, Korean, Chinese</b>", "After M4 (decision 7)"),
              ("<b>Silent capture; the power pack; the Gmail relay</b>", "Yours to decide (decision 5); deferred; “not yet”"),
              ("<b>The other unbuilt ideas</b>", "They stay in <code>not-built-2.0.md</code> with their ratings")) + '''
</section>

<section>
  <h2>The four-milestone plan: its decisions, with their old numbers</h2>
  <p class="prose">Kept as they were numbered, because the parked milestones name them. M1’s four (2, 9, 10 and 11 here) are in the list above as 3, 8, 9 and 10. Decisions 3 to 8 and 12 to 14 wait with their milestones.</p>
  <ol class="qs prose">
    ''' + q("All · These four, in this order, starting with the half-day device session on both devices.", "<b>Yes</b>", "If no: name the order. The device session still comes first, and it needs your HP awake and attached for half a day.") + '''
    ''' + q("M1 · A line under the empty field for something you just copied, with a switch to turn it off.", "<b>Yes</b>", "If no: the empty panel stays empty, and Tab there opens the copy.") + '''
    ''' + q("M2 · A task more than a decision: capture a region on your Googlebook, then Paste somewhere. Does the picture come? Look at the same time whether the capture’s preview has Share.", "<b>Tell us yes or no</b>", "Yes: capture, the key, Tab. No: Share is the door. Neither: the picker is built and M3 goes before M2. If you say only “go”, check 9 of the device session answers it.") + '''
    ''' + q("M2 · The text recogniser: one more Google library, no new permission, about 5 to 6 MB more on Play.", "<b>Yes</b>", "If no: no Copy text, nothing of M1 works on a picture, a whole screen gets only “too large”, and M2 is about 10 days. The research suggested shipping regions first and deciding later (NF2 §2.2).") + '''
    ''' + q("M2 · Silent capture as the device’s assistant.", "<b>No, not in these four.</b> Look again after M2 has been used for a month", "If yes: it is not sized. It replaces Gemini on its key and keeps a service of Booklight’s running always, which breaks “nothing in the background”. It is also the only road to Chrome’s “New tab”, which you asked for today (DF: read in the source, not tried).") + '''
    ''' + q("M3 · Entries in the selection menu from the start.", "<b>Two</b>, Fix spelling and Translate", "The other answers: one (“Booklight” only, as today) or all seven.") + '''
    ''' + q("M4 · Six languages in two steps: French, Spanish, Italian, then Danish, Swedish, Norwegian. Polish next. Japanese, Korean, Chinese later.", "<b>Yes</b>", "If no: Polish in M4 adds about 3 days (LANG §2). Japanese, Korean and Chinese are about 9 days of one-time work and about 18 for the languages.") + '''
    ''' + q("M4 · Keep German words (“morgen 9 Uhr”) working in an English Booklight.", "<b>Yes</b>, one setting, about a day", "If no: “morgen 9 Uhr” stops working in an English Booklight. It saves a day.") + '''
  </ol>
  <h3 class="gap">Six smaller ones</h3>
  <p class="prose">The team settled these, but each changes something that is yours: a rule of yours, the released app, or the privacy text.</p>
  <ol class="qs prose" start="9" style="counter-reset: q 8">
    ''' + q("M1 · The translation stands first for a text not in your language, above what was found in the copy.", "<b>Yes</b>, as the one exception", "It bends two rules: the 2.0 design’s “a model’s rows never outrank a local match”, and “the order is fixed”. If no: it stands after the found things, and the story is Tab, Down up to three times, Enter, Enter.") + '''
    ''' + q("M1 · <code>clip</code> shows fewer rows. Note, Mail, QR code, UPPERCASE, lowercase, Title Case, On one line, Counted, URL-encoded, Search and Ask Gemini are found by typing their name.", "<b>Yes</b>", "If no: those eleven rows stay in the list, under the new ones.") + '''
    ''' + q("M1 · Shared text behaves the new way in the released app: the chip stays the text and the row is answered in its place.", "<b>Yes</b>", "If no: shared text keeps today’s way (the chip becomes the prompt), and the same rows behave in two ways.") + '''
    ''' + q("M4 · Who reads each language: a machine draft, then one native reader per language.", "<b>A reader you know</b> where you have one; <b>paid</b>, about USD 260 a language, where you do not", "If neither: unread drafts ship. A wrong keyword here is a broken feature, not a typo (LANG §13).") + '''
    ''' + q("M4 · Suggestions are asked for in the user’s language. The language goes to the search engine with the text, and the privacy text gains a clause.", "<b>Yes</b>", "If no: suggestions stay as they are today, in every language.") + '''
    ''' + q("M4 · Your rule “every string in English and German”: the two stay the rule for building; the other six may trail by one release.", "<b>Yes</b>", "If no: every release waits for all eight languages and their readers.") + '''
  </ol>
  <div class="prose blk">
    <p>Everything else in this plan was settled by the team and is listed under its milestone as “settled”. Say so if you disagree with any of it.</p>
    <p><b>After “go”</b> comes the device session and its results, and the designs made final on them. Still to be drawn then: the one new move, the picture riding down when a question is typed (screen 5), by the motion designer.</p>
    <p><b>Four things only you can judge, on a device,</b> before the designs are final. The line at 320 ms and at 200 ms after the opening: compare them first in the playing panel of screen 1, then in the throwaway build of the device session, which carries both. The picture at 176 × 104 on dark glass. The line and a picture’s read text in dark over a white window. And whether, in light theme over a white window, a row that is being answered says clearly enough that it is working, since the light cannot be seen there.</p>
  </div>
  <details>
    <summary>Where the team disagreed, and what was chosen (21 points)</summary>
    ''' + table(("Question", "What each said", "Chosen"),
                ("The chip when a model’s row is run", "UX: the chip stays the thing, the row is answered where it stands. Design and engineering: the chip becomes the prompt, the field stays empty", "UX’s. The picture needs it anyway. Engineering’s form is the fallback if it proves too dear in M1"),
                ("A keyword for “say what to do with it”", "The product manager’s first draft: <code>do</code>. Engineering: it would swallow “do not disturb”. UX: no keyword", "None. Under the chip, what you type is the instruction. From the empty field: <code>clip</code> and the instruction"),
                ("The quiet line’s layout", "UX: the words, the age at the right end, a cap. Design: one text with the age in it, one cap", "Design’s layout (one text has one baseline), UX’s words"),
                ("The line after the field is emptied again", "UX: it comes back. Design: it stays away, or it flickers while you edit", "It stays away. It returns only by Backspace out of its own chip"),
                ("The rows after Tab", "Design: eight, with Explain and the web row. UX: at most three things found, your first three prompts (the translation among them), Summary for a long text", "UX’s. The rest is found by typing its name"),
                ("The right end of a model’s row", "Design: bare. UX: “On this device”, or “Gemini”", "UX’s. It is how you see before Enter that a row hands over"),
                ("The things found in a copy", "First draft: link, date, phone, address, flight, and Copy as Markdown. Engineering: a flight is only a web search, addresses were not found, a Markdown link has no page title", "Link, date, phone number, mail address. Copy clean stays"),
                ("The rows under a picture", "Design: three rows, the answer beside the picture. UX: one row, the answer as a row above it", "UX’s rows on design’s measures. Enter on a picture copies its text"),
                ("An answer’s caption about a picture", "UX: “On this device · from the picture”. Design: the source first", "Design’s order: “From the picture · on this device”"),
                ("The doors for a picture", "UX: clipboard, Share, the photo picker, and <code>pic</code>. Engineering: three doors are three flows", "Two doors. The picker and <code>pic</code> only if both fail on the device"),
                ("<code>error</code>", "First draft: M3. Engineering: M2, a picture of an error is M2’s best test. The last review: its own example is a copy, and on a picture typing the question does it", "Not in these four. You can add it under Yours"),
                ("Where “translate into any language” goes", "First draft and engineering: M3. The last review: it answers your M1 words, “have translate options”, and the engineer had already moved its base to M1", "M1. Its 1.5 days move with it"),
                ("One word without its letters", "First draft: no model. Design: a large computed answer. Engineering: a blind swap turns “neue” into “neü”", "Through the model and the same check as a sentence, in an ordinary answer row"),
                ("The chip of <code>tr danish …</code>", "Design: it changes to the language’s name. UX: it stays “Translate”", "UX’s. The row says “In Danish”"),
                ("The names in the selection menu", "First draft: any prompt, “In German”. Engineering: a name is fixed when the app is built", "“Translate”. Your own prompts stay behind “Booklight”"),
                ("Where the language groundwork ships", "First draft: inside M3. Engineering: a quiet release before M3", "Engineering’s"),
                ("The picture before the selection", "UX: the selection is the nearer step. Engineering: the picture second is right if the HP has no model", "The picture second, with one swap rule"),
                ("Six languages at once", "First draft: yes. Engineering: French, Spanish and Italian first", "One milestone, two steps"),
                ("Dates in a thing you brought", "First draft: the app’s language and English. UX: a German mail copied into an English Booklight would lose its event", "The text’s own language. Not sized by the engineer"),
                ("The longest name of an action", "UX: 16 characters, as today. Design: 20", "16"),
                ("The sizes", "First draft: about 40 days. Engineering: 66", "Engineering’s, less what the last review moved out: about 65"), cls="k") + '''
  </details>
</section>
'''



PAGE = "<main>" + HEADER + GLANCE + HOLDS + M1 + M5 + NEXT + PARKED + M2 + M3 + M4 + OLD_REST + "</main>"
