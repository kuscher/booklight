# The page's words. Run by gen.py (it supplies shot, screen and the drawings); [[name]] is where a drawing goes.
# Screens are numbered as the page shows them: 1 to 4 are M1, 5 to 7 are M2, 8 and 9 are M3, 10 and 11 are M4.

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
  <p class="lede">This is the plan the team stands behind: product manager, UX designer, visual and motion designer, engineer. It has four milestones, and each one below has its promise, what is in it, its designs drawn at real size, what is left out, how it is built, and what only you can decide. Nothing was built and no device was touched; where the research says “not verified”, this page says so too.</p>
  <div class="meta"><span>1 October 2026</span><span>Eleven screens, four of them play</span><span>Fourteen decisions for you, at the end: eight large, six small</span><span>Nothing is built before you approve it</span></div>
</header>
'''

GLANCE = '''
<section>
  <h2>The four, at a glance</h2>
  <div class="glance">
    <a class="gcard" href="#m1"><span class="num">M1</span><h3>The copy</h3><p>Copy something, open Booklight, and one line offers what can be done with it.</p><span class="size">About 14 days · no new permission</span></a>
    <a class="gcard" href="#m2"><span class="num">M2</span><h3>The picture</h3><p>Capture a piece of the screen, open Booklight, and copy its text, ask about it or keep it on top.</p><span class="size">About 14 days · no new permission, one more Google library</span></a>
    <a class="gcard" href="#m3"><span class="num">M3</span><h3>The selection</h3><p>Select text where you are writing, right-click, and Booklight rewrites it in place.</p><span class="size">About 8 days · no new permission</span></a>
    <a class="gcard" href="#m4"><span class="num">M4</span><h3>Your language</h3><p>Booklight speaks six more languages: its screens, its keywords and its dates.</p><span class="size">About 17 days and the wait for readers, after 12 days of groundwork</span></a>
  </div>
  <div class="prose blk">
    <p><b>One idea carries the first three.</b> Something you brought (a copy, a picture, a selection) becomes the chip in the field, and the rows under it are what can be done with it. 2.0 already does this for text another app hands over. M1 builds it once; M2 and M3 each add one new door to it.</p>
    <p><b>Three of the four are your own ideas:</b> M1, M2 and M4. M3 is not one of your three. It is the team’s proposal for the step after the copy, from the research’s ideas 5, 4 and 6''' + src("NF2 §1") + '''.</p>
    <p class="muted small">The sizes are the engineer’s, made before the team’s last changes. M1 and M2 were not re-sized after their cuts, the choice “the answer where it stands” was never sized, and one rule (dates read in the text’s own language) has no size yet. Two things moved in the last review, with the engineer’s own figures: the typed language and <code>tr</code> went from M3 to M1 (1.5 days), and <code>error</code> left the four (half a day). That makes about 65 days with the groundwork, without the settings window and today’s polish. The cheapest forms that still keep each promise add up to 42, with three languages in M4, not six. Read the days as sizes against each other, not as a calendar: this project has run far ahead of such figures before.</p>
    <p class="muted small">Sources are named as in the plan: NF2 is <code>next-features-2.md</code>, S&amp;C is <code>screen-and-clipboard.md</code>, DF is <code>device-findings.md</code>, LANG is <code>languages.md</code>, all in <code>docs/research/</code>; UX, ENG and DES are the team’s three papers behind <code>plan.md</code>. “Check n” is a line of the engineer’s device session.</p>
  </div>
</section>
'''

HOLDS = '''
<section>
  <h2>What holds for all four</h2>
  <div class="prose blk">
    <p>No new permission. A model’s row never runs anything by itself. Nothing in the background. Every string in English and German. Every “not verified” gets its device check before the feature is promised. The release build is installed and asked a prompt before every tag.</p>
    <p>The model is asked on Enter, or when you pick its name in another app’s menu; never after a pause in typing. Ctrl + 1…9 never asks it. Esc closes at every step.</p>
    <p class="notice"><b>One check stands before all four.</b> Your HP has never run 2.0, and its model has never been tried. If it does not answer there, every model row on the HP hands over to Gemini, and M1’s model rows, M2’s Ask and all of M3 can only be judged on the Lenovo. It is check 1 of the first device session.</p>
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
    <p><b>The motion in the panels that play</b> follows the numbers settled today in design system §12, at Medium: the seam for 160 ms, the glass from 60 ms for 360 ms on its new curve (slow, fast, slow), at rest 420 ms after the key; the white light 1.75 dp wide in dark and 2.0 in light, with a front and a tail, and no glow. A browser draws them, so they are a close copy, not the app: the blur that arrives last and the light’s soft inner edge are simpler here. The app and its recordings stay the reference.</p>
    <p><b>Nothing new in the look beyond four elements,</b> each built from parts the app has: the quiet line; a chip with a picture in it; a row with a picture; a picture in the pinned window. No new colour, type size or row height. One move is new and the motion designer has not drawn it yet: the picture riding down when a question is typed (screen 5).</p>
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
    <p>Two changes to <code>clip</code> come with it. It opens the same list at any age, with fewer rows than today (decision 10). And it no longer reads a copy marked private; it says “What you copied is marked private”.</p>
    <p><b>This changes one thing in the released app</b> (decision 11). Shared text behaves the new way too. Today Enter on “Fix spelling” turns the chip into the prompt and types the whole text into the field. After M1 the chip stays the text and the row is answered in its place. The selection handed over by today’s “Booklight” menu entry gets the same list at no extra cost, because it is the same code.</p>
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
                 "After Tab you type a language and the row is the translation into it. From the empty field the keyword is <code>tr</code>, with the language as the first word. It is also the keyword M4 needs: “en” or “de” is an everyday word in each of its six languages." + src("LANG §3.4")) + '''
  </div>

  <div class="prose blk">
    <h3>Deliberately out</h3>
    <p>Flights and addresses (a web search and a web address; addresses were not found in the Lenovo test). Copy as Markdown (Booklight does not know a page’s title, so the link would hold its address twice). The system classifier’s ready-made actions (Booklight could not say what one does). A history of copies and pasting in place (both need the power pack). A picture on the clipboard (M2). A line for a copy older than two minutes. A setting for the two minutes.</p>
  </div>

  <div class="prose blk">
    <h3>How it is built, and its size</h3>
    <p>The line is built as a third thing that can stand under the empty field, beside the first-run card and the tip, so it brings no highlight, no footer and no stray Enter. When the window has focus and the panel is drawn, Booklight asks the system for the copy’s description; the rule (younger than two minutes, not private, not Booklight’s own, a known kind) is plain code with tests. Tab reads the text and opens the list that <code>clip</code> opens, so there is one list. What is in the text comes from the system’s findings, looked up before the list is shown and capped at about 150 ms; nothing is slipped in above the selection later. <code>tr</code> is a keyword and a table of languages on the translation.</p>
    <p>Six shared pieces are built here once and used again by M2, M3 and M4: one way to an answer for any row, the material travelling beside the field, ready-made prompts kept by reference, one clipboard helper, the language of a text, and plain answers. <code>PRIVACY.md</code> changes: today it says the clipboard is read only when you ask.</p>
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
    <p><b>A line with nothing typed</b> (decision 2). Your plan says “Nothing typed: nothing at all” (PLAN.md §11.3). This is the first line that depends on what you did in another app, and it changes one sentence of the privacy text. ''' + REC + ''' Yes, with a switch in the window (“What you copied”). If no: the empty panel stays empty, Tab there opens the copy, and the rest of M1 is the same.</p>
    <p><b>The translation first</b> (decision 9). For a text not in your language its translation stands above what was found in the copy. That puts a model’s row above local rows; the 2.0 design says a model’s rows “never outrank a local match”. ''' + REC + ''' Yes, this one exception: it is what makes “four presses, no typing” true.</p>
    <p>Two smaller ones are named above: <code>clip</code>’s shorter list (decision 10) and shared text in the released app (decision 11).</p>
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

# ------------------------------------------------------------------------------------------------ the rest
ORDER = '''
<section>
  <h2>The order, and why</h2>
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
  <h2>The tracks beside the four</h2>
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
  <h2>Not in these four, and why</h2>
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
  <h2>Your decisions, in one list</h2>
  <p class="prose">Each can be answered in a word. The recommended answer is what happens if you say only “go”. Under each: what the other answer costs.</p>
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

PAGE = "<main>" + HEADER + GLANCE + HOLDS + M1 + M2 + M3 + M4 + ORDER + "</main>"
