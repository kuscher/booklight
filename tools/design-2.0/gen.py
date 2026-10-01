"""Generates docs/design/booklight-2.0.html: the 2.0 plan and design page, panels drawn at 1 px = 1 dp.

    python3 tools/design-2.0/gen.py     then publish docs/design/booklight-2.0.html to update the artifact.
"""

W = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
ICONS = {
    "booklight": "M6 4h12a3 3 0 0 1 0 6H6a3 3 0 0 1 0-6zM7.2 12h9.6l3.2 8H4l3.2-8z",
    "list": "M4 5h3v3H4zm5 0h11v3H9zM4 10.5h3v3H4zm5 0h11v3H9zM4 16h3v3H4zm5 0h11v3H9z",
    "bolt": "M11 21h-1l1-7H7.5c-.6 0-.6-.3-.4-.7L13 3h1l-1 7h3.5c.5 0 .6.3.4.7z",
    "sun": "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM2 13h2a1 1 0 0 0 0-2H2a1 1 0 0 0 0 2zm18 0h2a1 1 0 0 0 0-2h-2a1 1 0 0 0 0 2zM11 2v2a1 1 0 0 0 2 0V2a1 1 0 0 0-2 0zm0 18v2a1 1 0 0 0 2 0v-2a1 1 0 0 0-2 0z",
    "search": "M15.5 14h-.79l-.28-.27A6.5 6.5 0 1 0 14 15.5l.27.28v.79l5 4.99L20.49 19zm-6 0a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9z",
    "lock": "M12 2a5 5 0 0 0-5 5v3H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1V7a5 5 0 0 0-5-5zm-3 8V7a3 3 0 0 1 6 0v3z",
    "info": "M11 7h2v2h-2zm0 4h2v6h-2zm1-9a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z",
    "key": "M12.65 10A6 6 0 1 0 12.65 14H17v4h4v-4h2v-4zM7 14a2 2 0 1 1 0-4 2 2 0 0 1 0 4z",
    "open": "M14 3h7v7h-2V6.41l-8.3 8.3-1.4-1.42L17.58 5H14zM5 5h6v2H5v12h12v-6h2v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
    "app": "M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z",
    "window": W + "m0 4v10h16V8zm7 2h2v2h2v2h-2v2h-2v-2H9v-2h2z",
    "left": W + "m8 2v12h8V6z",
    "right": W + "m0 2v12h8V6z",
    "full": W,
    "p3l": W + "M8.5 6v12h5V6zM15.5 6v12H20V6z",
    "p3m": W + "M4 6v12h4.5V6zM15.5 6v12H20V6z",
    "p3r": W + "M4 6v12h4.5V6zM10.5 6v12h5V6z",
    "p23l": W + "M15.5 6v12H20V6z",
    "p23r": W + "M4 6v12h4.5V6z",
    "ptl": W + "M12 6v6H4v6h16V6z",
    "ptr": W + "M4 6v12h16v-6h-8V6z",
    "pbl": W + "M4 6v6h8v6h8V6z",
    "pbr": W + "M4 6v12h8v-6h8V6z",
    "pc": W + "M4 6v12h16V6zM8 9h8v6H8z",
    "more": "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z",
    "settings": "M19.14 12.94a7 7 0 0 0 0-1.88l2.03-1.58a.5.5 0 0 0 .12-.61l-1.92-3.32a.5.5 0 0 0-.59-.22l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.48-.41h-3.84a.5.5 0 0 0-.47.41l-.36 2.54a7 7 0 0 0-1.62.94l-2.39-.96a.5.5 0 0 0-.59.22L2.74 8.87a.5.5 0 0 0 .12.61l2.03 1.58a7 7 0 0 0 0 1.88l-2.03 1.58a.5.5 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54a7 7 0 0 0 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.5.5 0 0 0-.12-.61zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z",
    "note": "M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h9l7-7V5a2 2 0 0 0-2-2zM7 8h10v2H7zm0 4h5v2H7zm7 7.5V14h5.5z",
    "check": "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z",
    "timer": "M15 1H9v2h6zm-4 13h2V8h-2zm8.03-6.61 1.42-1.42a11 11 0 0 0-1.41-1.41l-1.42 1.42a9 9 0 1 0 1.41 1.41zM12 20a7 7 0 1 1 0-14 7 7 0 0 1 0 14z",
    "spark": "M12 2l1.9 6.1L20 10l-6.1 1.9L12 18l-1.9-6.1L4 10l6.1-1.9zM19 15l.9 2.6 2.6.9-2.6.9L19 22l-.9-2.6-2.6-.9 2.6-.9z",
    "plus": "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z",
    "trash": "M9 3h6l1 1h4v2H4V4h4zM6 8h12l-1 12a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2z",
    "copy": "M8 3h10a2 2 0 0 1 2 2v12h-2V5H8zM5 7h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v10h10V9z",
    "send": "M3 20.5v-7L14 12 3 10.5v-7L22 12z",
    "edit": "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75z",
    "enter": "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z",
    "pin": "M16 3v2h-1v6l2 3v2h-4v5l-1 1-1-1v-5H7v-2l2-3V5H8V3z",
    "arrowl": "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20z",
    "back": "M22 5H9a2 2 0 0 0-1.6.8L2 12l5.4 6.2A2 2 0 0 0 9 19h13a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zm-3.3 10.3-1.4 1.4-2.8-2.7-2.8 2.7-1.4-1.4 2.8-2.8-2.8-2.8 1.4-1.4 2.8 2.7 2.8-2.7 1.4 1.4-2.8 2.8z",
    "user": "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm0 2c-3.3 0-8 1.7-8 5v1h16v-1c0-3.3-4.7-5-8-5z",
}


def svg(name, cls="i", size=None):
    st = f' style="width:{size}px;height:{size}px"' if size else ""
    return f'<svg class="{cls}" viewBox="0 0 24 24" aria-hidden="true"{st}><path fill-rule="evenodd" d="{ICONS[name]}"/></svg>'


ENTER = svg("enter", "i ent")


def app(letter, colour):
    return f'<span class="ic app" style="--c:{colour}">{letter}</span>'


def sym(name):
    return f'<span class="ic sym">{svg(name)}</span>'


def slot(icon, name=None, armed=False, bad=False, extra=""):
    if armed:
        return f'<span class="a arm{" bad" if bad else ""}"{extra}>{svg(icon)}<b>{name}</b>{ENTER}</span>'
    return f'<span class="a{" bad" if bad else ""}"{extra}>{svg(icon)}</span>'


NINE = [("open", "Open"), ("window", "New window"), ("info", "App info"), ("left", "Left half"), ("right", "Right half"),
        ("full", "Full"), ("p3l", "Left third"), ("p3m", "Middle third"), ("p3r", "Right third")]
REST = [("p23l", "Left ⅔"), ("p23r", "Right ⅔"), ("ptl", "Top left"), ("ptr", "Top right"), ("pbl", "Bottom left"),
        ("pbr", "Bottom right"), ("pc", "Centre")]


def strip(armed=0, tenth=None, chev="more"):
    """armed: index into the nine, 9 = More. tenth: (icon, name) shown in the arrow's slot instead of the chevron."""
    out = []
    for i, (ic, nm) in enumerate(NINE):
        out.append(slot(ic, nm, armed == i))
    if tenth:
        out.append(slot(tenth[0], tenth[1], True))
    else:
        out.append(f'<span class="a arm chev">{svg(chev)}<b>More</b>{ENTER}</span>' if armed == 9 else f'<span class="a chev">{svg(chev)}</span>')
    return '<span class="strip">' + "".join(out) + "</span>"


def row(icon_html, title, sub=None, right="", on=False, cls="", tall=False):
    t = f'<span class="t"><b>{title}</b>{f"<i>{sub}</i>" if sub else ""}</span>'
    return f'<div class="r{" on" if on else ""}{" tall" if tall else ""} {cls}">{icon_html}{t}{right}</div>'


def kind(text):
    return f'<span class="kind">{text}</span>'


def caps(*keys):
    return '<span class="keys">' + '<i class="pl">+</i>'.join(f'<span class="kc">{k}</span>' for k in keys) + "</span>"


def fld(text="", chip=None, placeholder=None, mark="G"):
    m = f'<span class="g">{mark}</span>' if chip is None else ""
    c = f'<span class="chip">{svg(chip[0])}{chip[1]}</span>' if chip else ""
    body = f'<span class="typed">{text}</span>' if placeholder is None else f'<span class="typed"></span><span class="ph">{placeholder}</span>'
    return f'<div class="fld">{m}{c}{body}</div>'


def foot(*pairs):
    return '<div class="foot">' + "".join(f'<span class="fk">{k}</span>{v}' for k, v in pairs) + "</div>"


def panel(inner, extra_cls="", attrs=""):
    return f'<div class="scroll"><div class="desk"><div class="panel {extra_cls}" {attrs}>{inner}</div></div></div>'


def shot(panel_html, title, text, wide=True):
    return f'<div class="shot">{panel_html}<h3>{title}</h3><p class="muted small">{text}</p></div>'


WEB = lambda q: row(sym("search"), f"Search Google for “{q}”", right=kind("Web"))

# ---------------------------------------------------------------- panels

# 1. The row that opens (live)
subs = "".join(
    f'<div class="sub" data-sub="{i}"><span class="g2">{svg(ic)}</span><b>{nm}</b><span class="mk">{ENTER}</span></div>'
    for i, (ic, nm) in enumerate(REST))
subs += '<div class="gap8"></div><div class="sub bad" data-sub="7"><span class="g2">' + svg("trash") + '</span><b>Uninstall</b><span class="mk">' + ENTER + "</span></div>"
demo_row = panel(
    fld("chr")
    + '<div class="rows" id="orows"><div class="hl" id="ohl"></div>'
    + '<div class="r" id="oparent">' + app("C", "#DB4437") + '<span class="t"><b>Chrome</b></span>'
    + '<span class="strip" id="ostrip">' + "".join(slot(ic, nm) for ic, nm in NINE) + f'<span class="a arm chev" id="ochev">{svg("more")}<b id="omore">More</b>{ENTER}</span></span></div>'
    + f'<div class="block" id="oblock">{subs}</div>'
    + '<div class="others" id="oothers">' + row(app("C", "#4285F4"), "Chrome Remote Desktop", right=kind("App")) + row(sym("settings"), "Chrome OS flags", right=kind("Settings")) + WEB("chr") + "</div>"
    + "</div>"
    + '<div class="foot" id="ofoot"><span class="fk">tab</span>Actions<span class="fk">esc</span>Close</div>',
    attrs='id="opanel"')

row_more = panel(fld("chr") + '<div class="rows">' + row(app("C", "#DB4437"), "Chrome", right=strip(0), on=True) + row(app("C", "#4285F4"), "Chrome Remote Desktop", right=kind("App")) + WEB("chr") + "</div>" + foot(("tab", "Actions"), ("esc", "Close")))
row_typed = panel(fld("chrome top left") + '<div class="rows">' + row(app("C", "#DB4437"), "Chrome", right=strip(-1, tenth=("ptl", "Top left")), on=True) + WEB("chrome top left") + "</div>" + foot(("tab", "Actions"), ("esc", "Close")))

# 2. other apps
keep = panel(fld("keep") + '<div class="rows">'
             + row(app("K", "#F4B400"), "Keep", right=strip(0), on=True)
             + row(app("K", "#F4B400"), "New text note", right=kind("Keep"))
             + row(app("K", "#F4B400"), "New list", right=kind("Keep"))
             + row(app("K", "#F4B400"), "New photo note", right=kind("Keep"))
             + WEB("keep") + "</div>" + foot(("tab", "Actions"), ("esc", "Close")))

# 3. s then Tab
s_tab = panel(fld("s") + '<div class="rows">'
              + row(app("S", "#4A154B"), "Slack", right=strip(0), on=True)
              + row(app("S", "#1DB954"), "Spotify", right=kind("App"))
              + row(sym("settings"), "Sound", right=kind("Settings"))
              + row(sym("settings"), "Search settings", right='<span class="fk big">tab</span>')
              + WEB("s") + "</div>" + foot(("tab", "Search settings"), ("esc", "Close")))

s_scope = panel(fld("blu", chip=("settings", "Settings")) + '<div class="rows">'
                + row(sym("settings"), "Bluetooth", right='<span class="strip">' + slot("open", "Open", True) + "</span>", on=True)
                + row(sym("settings"), "Connected devices", right=kind("Settings"))
                + row(sym("open"), "Search the Settings app", "Every setting, in its own search")
                + WEB("s blu") + "</div>" + foot(("⌫", "Leave"), ("esc", "Close")))

# 4. keys
RES = '<span class="res">'
k_scope = panel(fld("desk", chip=("key", "Keys")) + '<div class="rows">'
                + row(sym("key"), "Next desk", right=caps("Action", "Ctrl", "]") + RES + '<span class="strip">' + slot("open", "All shortcuts", True) + slot("copy") + "</span></span>", on=True)
                + row(sym("key"), "Previous desk", right=caps("Action", "Ctrl", "[") + RES + kind("Key") + "</span>")
                + row(sym("key"), "Move window to the next desk", right=caps("Action", "Shift", "]") + RES + kind("Key") + "</span>")
                + row(sym("key"), "Peek at the desktop", right=caps("Action", "V") + RES + kind("Key") + "</span>")
                + "</div>" + foot(("tab", "Actions"), ("esc", "Close")))

# 5. todo (live tick)
def task(i, text, date, on=False):
    strip_html = '<span class="strip tstrip">' + f'<span class="a arm">{svg("check")}<b class="tdo">Done</b>{ENTER}</span>' + slot("copy") + "</span>"
    return (f'<div class="r task" data-task="{i}"><span class="ic"><svg class="box" viewBox="0 0 20 20"><rect x="1" y="1" width="18" height="18" rx="6"/>'
            f'<path class="tk" d="M5.5 10.5l3 3 6-7"/></svg></span><span class="t"><b><span class="tt">{text}<i class="strike"></i></span></b></span>'
            f'{kind(date)}{strip_html}</div>')

todo = panel(fld("", chip=("check", "To-do")) + '<div class="rows" id="trows"><div class="hl"></div>'
             + task(0, "call the bank about the card", "Yesterday", on=True)
             + task(1, "book the dentist", "Monday")
             + task(2, "renew passport", "24 Sep")
             + "</div>" + '<div class="foot"><span class="done" id="tdone"></span><span class="fk">⌫</span>Leave<span class="fk">esc</span>Close</div>',
             attrs='id="tpanel"')

# 6. tip (live typing)
tip = panel(
    '<div class="fld" id="tipfld"><span class="g" id="tipg">G</span><span class="chip" id="tipchip" hidden>' + svg("timer") + 'Timer</span><span class="typed" id="tiptyped"></span><span class="ph" id="tipph">timer 10m tea</span></div>'
    + '<div class="card" id="tipcard"><span class="ic sym">' + svg("timer") + '</span><span class="t"><b>Timer</b><i><em>timer 10m tea</em> counts down in the Clock app.</i></span>'
    + '<span class="opts"><span id="tiptry">Try it<span class="fk">tab</span></span><span>Turn off tips</span></span></div>'
    + '<div class="rows" id="tiprows" hidden><div class="r on tall"><span class="ic sym">' + svg("timer") + '</span><span class="t"><i>tea · set for 14:12</i><b class="big">10:00</b></span><span class="strip">' + slot("open", "Start", True) + slot("pin") + "</span></div></div>"
    + '<div class="foot" id="tipfoot" hidden><span class="fk">tab</span>Actions<span class="fk">⌫</span>Leave<span class="fk">esc</span>Close</div>',
    attrs='id="tippanel"')

# 7. ? list
qlist = panel(fld("ti", chip=("list", "?")) + '<div class="rows">'
              + row(sym("timer"), "Timer", "timer 10m tea", right='<span class="strip">' + slot("edit", "Type…", True) + "</span>", on=True)
              + row(sym("right"), "Open an app in a place", "chrome right third", right=kind("Try it"))
              + row(sym("booklight"), "All commands", "The full list, in the Booklight window")
              + "</div>" + foot(("⌫", "Leave"), ("esc", "Close")))

# 8. AI (live)
ai = panel(fld("good morning, are we still on for lunch?", chip=("spark", "In German")) + '<div class="rows">'
           + '<div class="r on tall ai"><span class="ic sym">' + svg("spark") + '</span><span class="t"><i id="aicap">In German · on this device</i><b class="ans" id="aians"></b></span><span class="strip" id="aistrip" style="opacity:.35">' + slot("copy", "Copy", True) + slot("pin") + slot("send") + "</span></div>"
           + WEB("de good morning, are we still on for lunch?")
           + "</div>" + foot(("tab", "Actions"), ("⌫", "Leave"), ("esc", "Close")),
           attrs='id="aipanel"')

# ---------------------------------------------------------------- window
def prow(mark, title, sub=None, right="", on=False):
    m = f'<span class="m">{mark}</span>' if mark else ""
    return f'<div class="prow{" on" if on else ""}">{m}<span class="t"><b>{title}</b>{f"<i>{sub}</i>" if sub else ""}</span>{right}</div>'


def ex(text):
    return f'<span class="ex">{text}</span>'


SW = '<span class="sw"></span>'
SWOFF = '<span class="sw off"></span>'


def opts(labels, chosen):
    return '<span class="pick">' + "".join(f'<span{" class=on" if i == chosen else ""}>{l}</span>' for i, l in enumerate(labels)) + "</span>"


pages = {
    "start": '<h4>Booklight</h4><p class="lead">Press a key, type a few letters, press Enter.</p>'
             '<div class="stagebox"><div class="mini"><b>G</b> st<span>orage</span></div></div>'
             '<div class="lbl">Your key</div><p class="nokey"><i></i>No key yet</p>'
             '<p class="lead sm">Android gives no app a key of its own. Choose one once in Keyboard shortcuts; these two are free.</p>'
             '<div class="keysrow">' + caps("Action", "Alt", "Space") + '<span class="or">or</span>' + caps("Action", "K") + "</div>"
             + prow(svg("key"), "Open Keyboard shortcuts", "Choose App shortcuts, then Add shortcut, then Booklight", svg("open"), on=True)
             + '<div class="lbl">Tips</div>' + prow(svg("spark"), "Tips", "A tip under the field when the panel is open and nothing is typed", SW),
    "commands": '<h4>Commands</h4><p class="lead">Everything you can type. Enter opens the panel and types the example for you.</p>'
                '<div class="lbl">Open</div>'
                + prow(svg("app"), "An app", "By name, by initials, by a few letters", ex("chr") + ENTER, on=True)
                + prow(svg("p3r"), "An app in a place", "Halves, thirds, quarters, centre, full; when the app is not open yet", ex("chrome right third"))
                + prow(svg("bolt"), "Something inside an app", "What your apps offer on their icons", ex("new tab"))
                + '<div class="lbl">Find</div>'
                + prow(svg("settings"), "A settings page", "By name, or s and Tab for all of them", ex("wifi"))
                + prow(svg("key"), "A key of the system", "By name, or k and Tab for all 46", ex("snap"))
                + prow(svg("note"), "In your notes", "Lines of Notes.md, newest first", ex("notes milk"))
                + '<div class="lbl">Write down</div>'
                + prow(svg("check"), "A task", "Todo.md; Enter ticks one off", ex("todo call bank"))
                + prow(svg("timer"), "A timer", "The Clock rings; Pin keeps the countdown in view", ex("timer 10m tea")),
    "yours": '<h4>Yours</h4><p class="lead">Links, snippets, recipes and prompts you made. A row opens its editor in place.</p>'
             '<div class="lbl">Links</div>' + prow(svg("search"), "YouTube", "https://www.youtube.com/results?search_query={argument}", '<span class="kc">yt</span>')
             + '<div class="lbl">Recipes</div>' + prow(svg("bolt"), "Start work", "Chrome: Left ⅔ · Slack: Right third · Volume 30 %", '<span class="kc">work</span>')
             + '<div class="lbl">Prompts</div>'
             + prow(svg("spark"), "Fix spelling and grammar", "Fix the spelling and grammar. Reply with only the corrected text: {text}", '<span class="kc">fix</span>', on=True)
             + prow(svg("spark"), "Shorter", "Make this shorter and keep its meaning: {text}", '<span class="kc">shorter</span>')
             + prow(svg("spark"), "In German", "Translate to German. Reply with only the translation: {text}", '<span class="kc">de</span>')
             + prow(svg("plus"), "Add a prompt…"),
    "look": '<h4>Look</h4><p class="lead">Theme, colours, glass, and how fast the panel opens and closes.</p>'
            + prow(svg("sun"), "Theme", None, opts(["Auto", "Light", "Dark"], 0))
            + prow(svg("app"), "Glass", "How much of your desktop shows through", opts(["Clear", "Balanced", "Frosted", "Solid"], 1), on=True)
            + prow(svg("more"), "Opening and closing", "The panel unfolds from a line and folds back into it", opts(["Off", "Fast", "Medium", "Slow"], 1)),
    "results": '<h4>Results</h4><p class="lead">What the list may show, and where a web search goes.</p>'
               + prow(svg("settings"), "Settings pages", None, SW) + prow(svg("plus"), "Sums", None, SW)
               + prow(svg("key"), "The system's keys", "“snap” answers with Action + [", SW)
               + '<div class="lbl">Other apps</div>'
               + prow(svg("bolt"), "Commands from your apps", "What apps offer on their icons. Booklight reads the list; no app sees what you type.", SW, on=True)
               + prow('<span class="ic app" style="--c:#F4B400">K</span>', "Keep", "New text note · New list · New photo note · New audio note", SW)
               + prow('<span class="ic app" style="--c:#DB4437">C</span>', "Chrome", "New tab · New incognito tab", SW)
               + prow('<span class="ic app" style="--c:#1DB954">S</span>', "Spotify", "Search", SWOFF),
    "access": '<h4>Access</h4><p class="lead">Each of these is yours to give, in the system\'s own screens, and to take back there.</p>'
              + prow(svg("note"), "Notes folder", "Notes.md, Todo.md and daily notes go to Documents/Notes", '<b class="act">Change…</b>', on=True)
              + prow(svg("sun"), "Brightness", "Booklight may change the screen's brightness.", '<b class="act">Allowed</b>')
              + prow(svg("spark"), "Assistant key", "Choose Booklight as the digital assistant and the assistant key opens the panel.", svg("open")),
    "about": '<h4>About</h4><p class="lead">Version 2.0. A personal hobby project by Alexander Kuscher. Not affiliated with Google.</p>'
             + prow(svg("lock"), "Privacy policy", None, svg("open"), on=True)
             + prow(svg("spark"), "Show the tips again", None, "")
             + '<div class="lbl">What Booklight keeps</div>'
             + prow(svg("trash"), "Forget everything", "What you picked for the text you typed. It stays on this device.", '<b class="act">Delete</b>'),
}
NAV = [("start", "booklight", "Start"), ("commands", "list", "Commands"), ("yours", "bolt", "Yours"), ("look", "sun", "Look"),
       ("results", "search", "Results"), ("access", "lock", "Access"), ("about", "info", "About")]
nav_html = "".join(f'<button data-go="{k}"{" aria-current=true" if i == 0 else ""}>{svg(ic)}<span>{name}</span>{"<b class=dot></b>" if i == 0 else ""}</button>' for i, (k, ic, name) in enumerate(NAV))
pages_html = "".join(f'<div class="pg" data-page="{k}"{"" if k == "start" else " hidden"}>{v}</div>' for k, v in pages.items())
window = (f'<div class="scroll"><div class="appwin" id="appwin"><div class="syscap">Booklight<span>–  ▢  ✕</span></div>'
          f'<nav class="nav" aria-label="Sections"><div class="navhl" id="navhl"></div>{nav_html}</nav><div class="pagebox">{pages_html}</div></div></div>')

# ---------------------------------------------------------------- pins
def pinwin(label, w, inner):
    return f'<div class="pin{" focus" if label == "Note" else ""}" style="width:{w}px"><div class="syscap">{label}<span>–  ✕</span></div><div class="in">{inner}</div></div>'


COPYBTN = '<div class="actline"><span class="a arm">' + svg("copy") + "<b>Copy</b>" + ENTER + "</span></div>"
import hashlib
_bits = "".join(format(b, "08b") for b in hashlib.sha256(b"booklight").digest())[:121]
qr = "".join(f'<i{"" if b == "1" else " class=o"}></i>' for b in _bits)
pins = ('<div class="scroll"><div class="desk pinsdesk">'
        + pinwin("Timer", 280, '<div class="cap">tea · set for 14:12</div><div class="fig" id="cd"><span>7</span>:<span>4</span><span>2</span></div>')
        + pinwin("Answer", 280, '<div class="cap">1280 × 0.19</div><div class="fig">243.2</div>' + COPYBTN)
        + pinwin("Note", 320, '<div class="txt">Gate B22, boarding 16:40.<br>Ask about the aisle seat.</div>' + COPYBTN)
        + pinwin("Colour", 280, '<div class="col"><b></b><div class="forms"><span class="on">#3478F6' + ENTER + '</span><span>rgb(52, 120, 246)</span><span>hsl(219 91% 58%)</span><span>oklch(0.59 0.19 259)</span></div></div>')
        + pinwin("QR code", 216, f'<div class="plate">{qr}</div>' + '<div class="actline"><span class="a arm">' + svg("copy") + "<b>Copy image</b>" + ENTER + "</span></div>")
        + "</div></div>")

# ---------------------------------------------------------------- the page
import os
HERE = os.path.dirname(os.path.abspath(__file__))
CSS = open(os.path.join(HERE, "page.css")).read()
JS = open(os.path.join(HERE, "page.js")).read()

html = f'''<title>Booklight 2.0</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght,ROND@6..144,300..800,0..100&family=Google+Sans+Code:wght@400..600&display=swap">
<style>
{CSS}
</style>

<main>
<header>
  <div class="brand">
    <svg viewBox="0 0 48 48" aria-hidden="true"><rect width="48" height="48" rx="13" fill="var(--accent)"/><path d="M14 11h20a5 5 0 0 1 0 10H14a5 5 0 0 1 0-10z" fill="var(--accent-ink)"/><path d="M16.5 25h15L37 39H11z" fill="var(--lamp)"/></svg>
    <h1>Booklight 2.0</h1>
  </div>
  <p class="lede">The plan and the design to approve, redrawn after three independent reviews: how it is used, how it looks, how it moves. The panels below are drawn at their real size, and four of them play.</p>
  <div class="meta"><span>Spec and plan in <code>docs/superpowers/</code></span><span>Reviews in <code>docs/design/reviews-2.0/</code></span><span>Nothing built yet</span></div>
</header>

<section class="prose">
  <h2>What the reviews changed</h2>
  <p>All three said the same thing first: the ideas fit Booklight, and the first draft would not have been built to 1.1's standard. What changed because of them:</p>
  <div class="tablewrap"><table>
    <tr><th>They found</th><th>Now</th></tr>
    <tr><td>An opened row had no room: eight results and eight actions are sixteen rows on a screen that holds twelve.</td><td>While a row is open the list is that row and its actions. The others fade, as when a scope is entered.</td></tr>
    <tr><td>The action rows were indented onto a line that exists nowhere else; nine icons left the title 130 dp.</td><td>Action rows sit on the panel's two existing lines. Icons at a 32 dp pitch; the title has a fixed box and fades.</td></tr>
    <tr><td>A held Tab would have opened and shut the list every 850 ms.</td><td>The arrow is a stop called More. Only Enter, a second →, or a click opens it.</td></tr>
    <tr><td><code>new tab</code> would have made a file called "tab": <code>new</code> is a keyword.</td><td>Inside a scope, a command whose name starts with the keyword and the text comes first.</td></tr>
    <tr><td>Tab on a keyword "whichever row is selected" took Tab away from a row you had moved to.</td><td>Tab enters the keyword only while the selection is where typing left it.</td></tr>
    <tr><td>Tips made Enter live on an empty panel; Enter twice would have started the example.</td><td>Nothing is armed until Tab. After Booklight fills the field, the next Enter must be a new press.</td></tr>
    <tr><td>The tip card would have changed the opening itself, every time.</td><td>The panel opens as always. The tip arrives 0.4 s later, and only if nothing was typed.</td></tr>
    <tr><td>Ticking a task showed no tick, and a second Enter ticked the next one.</td><td>The check draws itself and the row stays, with Undo.</td></tr>
    <tr><td>The window had two identical highlights, divider lines and yellow "New" marks.</td><td>The column's highlight is quiet glass, the page keeps the one coloured pill, no lines, no "New".</td></tr>
    <tr><td>Left third and left half were 2 px apart at the device's size.</td><td>A third is drawn as three columns: a different shape from a half.</td></tr>
  </table></div>
</section>

<section>
  <h2>A row and its other actions</h2>
  <p class="prose">Nine icons at the row's right end, where 1.1 has them, then More. Open it and the row unfolds the way the panel does: the actions are laid out once and uncovered by one edge, the chevron turns over, the list's one pill pours into the first row. Closing is the same, backwards.</p>
  <div class="shot">
    {demo_row}
    <div class="controls" data-for="row"><span class="lbl">Try it</span><button data-k="enter">Enter</button><button data-k="down">↓</button><button data-k="up">↑</button><button data-k="left">←</button><span class="muted">or click the chevron and the rows</span></div>
    <p class="muted small">In the app the other results fade as the list opens, and typing closes it in the same frame. Uninstall is last, apart, and red; arrows and Ctrl + digit never run it.</p>
  </div>
  <div class="shots">
    {shot(row_more, "At rest", "Open is armed, as today. Tab and → walk the nine and stop on More. 1.1's first five keep their places; Full and the three thirds follow.")}
    {shot(row_typed, "A typed place", "“chrome top left” is not among the nine, so the arrow's own slot becomes that action, armed and named. Never an eleventh slot; delete the words and the chevron is back.")}
  </div>
</section>

<section>
  <h2>In the panel</h2>
  <div class="shots">
    {shot(keep, "Commands from your apps", "The app's icon, and its name where the kind stands. Listed under the app only for the top hit, three at most. “new tab” finds one directly.")}
    {shot(s_tab, "<kbd>s</kbd>, then Tab", "The keyword's row keeps the last local place, so <kbd>s</kbd> and Enter still opens your app. Tab makes the keyword the chip while the selection is untouched; the footer says so.")}
    {shot(s_scope, "Settings", "Every page Booklight knows. The Settings app's own index is closed to apps, so one row hands over to its search. The web row stays last.")}
    {shot(k_scope, "<kbd>k</kbd>, then Tab", "The system's 46 shortcuts. The caps keep one place in every row, selected or not. Enter opens the system's own window: Booklight cannot press keys, and the name says so.")}
  </div>
  <div class="shots">
    <div class="shot">{todo}<div class="controls" data-for="todo"><span class="lbl">Try it</span><button data-k="tick">Enter</button><button data-k="tdown">↓</button><span class="muted">or click a task</span></div>
      <h3>To-do</h3><p class="muted small">Enter ticks: the check draws itself, a line strikes the title, the row stays and its action becomes Undo. Typing adds a task, or narrows to the ones that match.</p></div>
    <div class="shot">{tip}<div class="controls" data-for="tip"><span class="lbl">Try it</span><button data-k="try">Tab, Enter</button><button data-k="reset">Again</button></div>
      <h3>Tips</h3><p class="muted small">The reminder's card, 0.4 s after the panel has opened and only if nothing was typed. The example also stands in the field. “Try it” makes Booklight type it, a letter at a time, so the keyword becomes its chip in front of you.</p></div>
    {shot(qlist, "The list of everything", "<kbd>?</kbd> is the scope at once. A keyword's row enters it; anything else is typed for you. The last row opens the whole list in the window.")}
  </div>
</section>

<section>
  <h2>Answers on this device</h2>
  <p class="prose">Measured on the Lenovo today, from Booklight's own process: the Googlebook's on-device model (Gemini Nano) answers a plain app. So the prompts can be answered in their own row instead of in the Gemini app.</p>
  <div class="shot">
    {ai}
    <div class="controls" data-for="ai"><span class="lbl">Try it</span><button data-k="ask">Pause in typing</button><button data-k="aireset">Again</button></div>
    <p class="muted small">Half a second after you stop typing, the model is asked. While it works the panel's edge light runs; the words fade in as they arrive. Enter copies. Typing again cancels. Nothing leaves the device.</p>
  </div>
  <div class="cols">
    <div class="prose">
      <h3>What it did on the Lenovo</h3>
      <div class="tablewrap"><table>
        <tr><th>Asked</th><th>Time</th></tr>
        <tr><td>English to German, two sentences</td><td class="n">2.2 s</td></tr>
        <tr><td>Fix “their going too the store tomorow…”</td><td class="n">1.5 s</td></tr>
        <tr><td>Make a stiff decline shorter and friendlier</td><td class="n">2.7 s</td></tr>
        <tr><td>What is the capital of Australia?</td><td class="n">0.7 s</td></tr>
        <tr><td>First word, once loaded</td><td class="n">0.3 s</td></tr>
      </table></div>
      <p class="muted small">All correct. In one summary it wrote “Google Books” for Googlebooks: it can get names wrong, so its rows never run anything. The HP is not checked.</p>
    </div>
    <div class="prose">
      <h3>The five</h3>
      <ol>
        <li><b>Fix and rewrite</b>: <code>fix …</code>, <code>shorter …</code>, <code>friendly …</code>. Replace puts it back into the field it came from.</li>
        <li><b>Translate</b>: <code>de …</code>, <code>en …</code>.</li>
        <li><b>Summarise</b> what you copied or what another app handed over: <code>sum</code>.</li>
        <li><b>A short answer</b>: <code>explain idempotent</code>, with “Open in Gemini” beside it.</li>
        <li><b>Say it plainly</b>: “remind me tomorrow at nine to call the bank”. The model proposes, Booklight's parsers decide. Larger; for the release after.</li>
      </ol>
      <p class="muted small">1 to 4 are one mechanism, about a week; your own prompts get it for free.</p>
    </div>
  </div>
  <div class="notice prose">
    <p><b>What it costs.</b> One more install-time permission (the system's AI service; no prompt). Google's library reports usage to Google from a background job; I took that part out on the Lenovo and the model still answered, nothing was scheduled, and “no analytics, nothing in the background” stayed true. Google has not called that supported. Google's terms for these APIs exclude apps for people under 18, so Booklight's Play audience would move from 13 to 18 and over.</p>
  </div>
</section>

<section>
  <h2>The Booklight window</h2>
  <p class="prose">Seven sections and one page beside them, on one ground with no lines. The column's highlight is quiet glass and says where you are; the page's is the coloured pill and says where the keys are. Click the sections: the page follows the pill and comes from the side the pill came from.</p>
  {window}
  <p class="prose muted small">Tab moves between column and page. Under 1008 dp the column narrows with the window, down to its icons; nothing jumps at a threshold. “No key yet” changes in place to “Your key works” the next time the window has the keys.</p>
</section>

<section>
  <h2>Pinned</h2>
  <p class="prose">A small, opaque window that stays above the others. The system's caption is its only bar. The keyboard stays with the app you were in; a click gives the pin the keys. The countdown changes one digit at a time.</p>
  {pins}
</section>

<section class="prose">
  <h2>How it moves</h2>
  <p>The motion review wrote a table of 35 rows in the design system's format, each with how it is interrupted, what it does with animations off, and what would pop if built carelessly. Everything rides 1.1's springs; two new named specs are added.</p>
  <div class="tablewrap"><table>
    <tr><th>Moment</th><th>What moves</th><th>Spec</th></tr>
    <tr><td><b>The row unfolds</b></td><td>One edge uncovers rows laid out once; the window's height follows the same value; the chevron turns; the pill pours in.</td><td><code>place</code>, <code>pop</code>, <code>lead</code>/<code>trail</code></td></tr>
    <tr><td><b>Booklight types</b></td><td>Letters land one by one; the keyword becomes its chip when its space lands; one search at the end.</td><td>new: <code>type</code>, 480 ms in all</td></tr>
    <tr><td><b>The check that draws itself</b></td><td>One stroke wherever Booklight says done: a task, “Copied”, “Your key works”.</td><td>140 ms</td></tr>
    <tr><td>A typed place</td><td>The arrow's slot becomes the action; the pane goes straight there; no name in between unrolls.</td><td><code>arm</code></td></tr>
    <tr><td>Tab on a keyword</td><td>The keyword's letters slide into the chip; text and chip change in one frame; the pill does not move.</td><td><code>place</code>, <code>pop</code></td></tr>
    <tr><td>The tip</td><td>The opening is unchanged; the card's parts rise in after it. Gone in the frame of the first letter.</td><td><code>place</code>, 22 ms apart</td></tr>
    <tr><td>The page changes</td><td>The old page fades where it stands; the new follows the column's pill by 60 ms.</td><td>fade 70; <code>place</code></td></tr>
    <tr><td>A countdown digit</td><td>Only the digits that changed, 8 dp; one wake a second.</td><td>new: <code>tick</code>, 160 ms</td></tr>
    <tr><td>Closing</td><td>As 1.1.1, drawn in to the field's centre line whatever the panel's height.</td><td>155 ms</td></tr>
  </table></div>
  <p class="muted small">Every row gets stepped through frame by frame on the device, with a debug-only slow factor for all springs.</p>
</section>

<section class="prose">
  <h2>The plan</h2>
  <p>Sixteen tasks with on-device answers, fifteen without. Each ends with the tests green, the build green, the feature driven once on the Lenovo, and a commit. The same three reviewers read the built app at the end.</p>
  <ol class="plan">
    <li><span><b>Core.</b> Places, the text of the notes files, prompts, three-word verbs; with tests.</span></li>
    <li><span><b>Motion's ground.</b> The new specs in one file, the slow factor, the footer on the window's bottom edge, the opening's gate.</span></li>
    <li><span><b>Places and the row.</b> Seventeen actions, ten slots at a 32 dp pitch, More, the list that unfolds.</span></li>
    <li><span><b>Other apps' commands.</b> What apps declare, the file for Booklight, the checks before anything starts, the switches.</span></li>
    <li><span><b><kbd>s</kbd>, <kbd>k</kbd> and Tab on a keyword.</b> More settings pages, checked on the device.</span></li>
    <li><span><b>The system's keys.</b> The table of 46 (written), rows with caps in one column.</span></li>
    <li><span><b>Notes.</b> Find, tasks with the tick and Undo, another file, today's file.</span></li>
    <li><span><b>Prompts</b>, and <b>answers on this device</b> if you say yes.</span></li>
    <li><span><b>Pin.</b> The permission, the window, the countdown.</span></li>
    <li><span><b>The guide and Booklight typing.</b> One table behind <kbd>?</kbd>, the Commands page and the tips.</span></li>
    <li><span><b>Tips.</b></span></li>
    <li><span><b>The window.</b> The column, the pages, “No key yet”.</span></li>
    <li><span><b>Finish.</b> German, TalkBack, version 2.0, the documents.</span></li>
    <li><span><b>On the device.</b> The motion review's fifteen checks, frame by frame.</span></li>
    <li><span><b>Reviews of the built app</b>: use, look, motion, code. Findings fixed.</span></li>
    <li><span><b>The release.</b> Tag <code>v2.0</code>.</span></li>
  </ol>
</section>

<section class="prose">
  <h2>Decide</h2>
  <ol class="qs">
    <li><div><b>Answers on this device.</b><span>In 2.0, with the usage reporting taken out <span class="tag rec">recommended</span>; or after 2.0; or not at all.</span></div></li>
    <li><div><b>The 18-and-over term.</b><span>Needed for the on-device answers on Play. Move Booklight's Play audience to 18 and over, or keep the answers out of the Play build.</span></div></li>
    <li><div><b>Tips.</b><span>Tab then Enter tries one, and they end after one pass <span class="tag rec">recommended by the review</span>; or Enter tries at once and they never end.</span></div></li>
    <li><div><b>The plan and the design.</b><span>Approve, or say what to change.</span></div></li>
  </ol>
</section>
</main>

<script>
{JS}
</script>
'''
open(os.path.join(HERE, "..", "..", "docs", "design", "booklight-2.0.html"), "w").write(html)
print(len(html))
