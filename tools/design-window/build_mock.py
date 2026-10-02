#!/usr/bin/env python3
"""Builds docs/design/booklight-window.html: the proposed Booklight window as static mock-ups, 1 px = 1 dp.

    python3 tools/design-window/build_mock.py     then publish docs/design/booklight-window.html to update the page.
"""
import html, pathlib

HERE = pathlib.Path(__file__).parent
e = html.escape


def ms(name, cls=""):
    return f'<span class="ms {cls}">{name}</span>'


# ---------------------------------------------------------------- window chrome

SECTIONS = [("Start", "lightbulb"), ("Commands", "format_list_bulleted"), ("Look", "palette"), ("Results", "search"), ("Privacy", "lock")]


def caption():
    return ('<div class="cap"><span class="app"><i></i></span><span class="name">Booklight</span>' + ms("expand_more", "chev") +
            ms("remove", "ctl c3") + ms("crop_square", "ctl c2") + ms("close", "ctl c1") + '</div>')


def nav(current, kind):
    items = "".join(
        f'<div class="nav{" on" if name == current else ""}"><span class="ind">{ms(icon)}</span><span>{name}</span></div>'
        for name, icon in SECTIONS)
    return f'<div class="{kind}">{items}</div>'


def window(size, theme, current, body, w, h, side=""):
    """size: x (expanded and up, rail 220), m (medium, rail 96), c (compact, bar)."""
    navhtml = nav(current, {"x": "rail", "m": "rail c", "c": "bar"}[size])
    return (f'<div class="win {theme} sz-{size}" style="width:{w}px;height:{h}px">{caption()}{navhtml}'
            f'<div class="pane"><div class="col">{body}</div>{side}</div></div>')


# ---------------------------------------------------------------- parts

def title(text, right=""):
    return f'<div class="ptitle"><h3>{e(text)}</h3>{right}</div>'


def lead(text):
    return f'<p class="lead">{e(text)}</p>'


def sp(n):
    return f'<div style="height:{n}px"></div>'


def gh(text):
    return f'<div class="gh">{e(text)}</div>'


def row(icon, label, sup=None, trail="", cls="", under=""):
    s = f'<div class="s">{e(sup)}</div>' if sup else ""
    lines = "two" if sup else ""
    lead_icon = icon if icon.startswith("<") else ms(icon)
    return (f'<div class="row {lines} {cls}">{lead_icon}<div class="t"><div class="l">{e(label)}</div>{s}{under}</div>{trail}</div>')


def group(*rows):
    return '<div class="grp">' + "".join(rows) + '</div>'


def sw(on):
    return f'<span class="sw{" on" if on else ""}"></span>'


def choice(options, chosen, cls=""):
    return f'<span class="bg {cls}">' + "".join(f'<span class="{"on" if o == chosen else ""}">{e(o)}</span>' for o in options) + '</span>'


def ex(text):
    return f'<span class="ex">{e(text)}</span>' + ms("keyboard_return", "ent")


def cap(text):
    return f'<span class="key">{e(text)}</span>'


def appicon(letter, tone):
    return f'<span class="appi {tone}">{letter}</span>'


# ---------------------------------------------------------------- the panel, drawn small (the live demo and the preview)

def panel():
    return (
        '<div class="panel">'
        '<div class="pfield"><span class="pg">G</span><span class="pq">ch</span><span class="caret"></span><span class="pc">rome</span></div>'
        '<div class="plist">'
        f'<div class="prow on"><span class="pico a">{ms("public")}</span><span class="pt">Chrome</span>'
        f'<span class="pchip">{ms("open_in_new")}Open{ms("keyboard_return")}</span>'
        f'{ms("add_box", "pa")}{ms("info", "pa")}{ms("splitscreen_left", "pa")}{ms("splitscreen_right", "pa")}{ms("expand_more", "pa")}</div>'
        f'<div class="prow"><span class="pico">{ms("settings")}</span><span class="pt">Battery</span><span class="pk">Settings</span></div>'
        f'<div class="prow"><span class="pico">{ms("search")}</span><span class="pt">Search Google for “ch”</span><span class="pk">Web</span></div>'
        '</div>'
        '<div class="pfoot"><span class="pcap">tab</span>Actions<span class="pcap">esc</span>Close</div>'
        '</div>')


def stage(width, height, scale, top):
    """A flat desk with two windows on it, and the panel at [scale]; nothing on it can be run."""
    return (f'<div class="stage" style="height:{height}px">'
            f'<div class="sheet s1" style="left:{round(width * 0.06)}px;top:{round(height * 0.16)}px;width:{round(width * 0.4)}px"><i style="width:40%"></i><i style="width:74%"></i><i style="width:60%"></i><i style="width:80%"></i><i style="width:50%"></i><i style="width:66%"></i><i style="width:72%"></i><i style="width:44%"></i><i style="width:70%"></i><i style="width:58%"></i><i style="width:76%"></i><i style="width:48%"></i></div>'
            f'<div class="sheet s2" style="right:{round(width * 0.06)}px;top:{round(height * 0.3)}px;width:{round(width * 0.46)}px"><i style="width:36%"></i><i style="width:70%"></i><i style="width:82%"></i><i style="width:56%"></i><i style="width:64%"></i><i style="width:78%"></i><i style="width:46%"></i><i style="width:68%"></i><i style="width:60%"></i><i style="width:74%"></i></div>'
            f'<div class="pwrap" style="top:{top}px;transform:scale({scale})">{panel()}</div></div>')


# ---------------------------------------------------------------- pages

def start_rows(short=False):
    return (
        gh("Open Booklight") +
        group(
            row(ms("check_circle", "ok"), "Your key works", "Booklight opens with the shortcut you set, and the same keys put it away.", cls="still"),
            row("keyboard", "Open Keyboard shortcuts", "Choose App shortcuts, then Add shortcut, then Booklight.", ms("open_in_new", "tr")),
            row("assistant", "Assistant key", "Choose Booklight as the digital assistant. Gemini then no longer answers that key.", ms("open_in_new", "tr")),
        ) +
        gh("Tips") +
        group(
            row("tips_and_updates", "Show tips", "One thing Booklight can do, shown when you open it and type nothing.", sw(False)),
            row("replay", "Show the tips again"),
        ))


START_LEAD = "Press a key, type a few letters, press Enter. Booklight opens apps, finds settings, searches the web, and does small things at once: a sum, a timer, a note, an emoji."


def start(size):
    if size == "wide":
        return title("Booklight") + lead(START_LEAD) + start_rows()
    w = {"x": 720, "c": 488}[size]
    st = stage(w, 240, 0.66, 28) if size == "x" else stage(w, 216, 0.6, 24)
    return title("Booklight") + lead(START_LEAD) + sp(20) + st + start_rows()


BUILT_IN = [
    ("Open", [
        ("open_in_new", "Apps", "Type a name; the icons on its row are what else it can do", "chrome"),
        ("web_asset", "An app, in a place", "left, right third, top left, full… for an app that is not open yet", "chrome left"),
        ("bolt", "What other apps offer", "Their own shortcuts and commands, as rows", "new conversation"),
        ("settings", "Settings", "s, then Tab: every settings page", "s wifi"),
        ("keyboard", "Keyboard shortcuts", "k, then Tab: what the system’s keys do", "k snap"),
        ("link", "Your links", "A keyword, then what to search for", "g lofi"),
        ("language", "The web", "The last row searches for what you typed", "weather berlin"),
    ]),
    ("Write down", [
        ("sticky_note_2", "Note", "A dated line in Notes.md", "note buy milk"),
        ("sticky_note_2", "Find a note", "Lines of Notes.md, newest first", "notes milk"),
        ("check", "To-do", "A task for Todo.md; todo alone shows what is open", "todo call the bank"),
    ]),
]

FIND = f'<span class="find">{ms("search")}<span>Find a command</span><span class="key">Ctrl F</span></span>'
FIND_ICON = f'<span class="ibtn">{ms("search")}</span>'
NEW = f'<span class="split"><span class="sl">{ms("add")}New link</span><span class="st">{ms("expand_more")}</span></span>'


def commands(size, marks=True):
    compact = size == "c"
    out = title("Commands", FIND_ICON if compact else FIND)
    out += lead("Everything Booklight does. Enter on a row types its example for you.")
    out += sp(20) + f'<div class="ctlrow">{choice(["Built in", "Yours"], "Built in", "g")}</div>'
    for name, rows in BUILT_IN:
        out += gh(name)
        items = []
        for i, (icon, label, sup, example) in enumerate(rows):
            cls = ""
            if marks and name == "Open" and i == 1: cls = "focus"
            if marks and name == "Open" and i == 3: cls = "hover"
            if compact:
                items.append(row(icon, label, sup, cls=cls, under=f'<div class="exu">{e(example)}</div>'))
            else:
                items.append(row(icon, label, sup, ex(example), cls=cls))
        out += group(*items)
    return out


LINKS = [("Google", "https://www.google.com/search?q=%s", "g"), ("YouTube", "https://www.youtube.com/results?search_query=%s", "yt"),
         ("Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s", "w"), ("Maps", "https://www.google.com/maps/search/%s", "maps"),
         ("GitHub", "https://github.com/search?q=%s", "gh"), ("Play Store", "https://play.google.com/store/search?c=apps&q=%s", "store"),
         ("Drive", "https://drive.google.com/drive/search?q=%s", "drive")]


def yours():
    out = title("Commands", FIND)
    out += lead("Links, snippets, recipes and prompts you made.")
    out += sp(20) + f'<div class="ctlrow">{choice(["Built in", "Yours"], "Yours", "g")}{NEW}</div>'
    out += gh("Links") + group(*[row("search", n, u, cap(k), cls="sel" if n == "GitHub" else "") for n, u, k in LINKS])
    out += gh("Snippets") + group(row("notes", "sig", "Kind regards, Alex"))
    out += gh("Recipes")
    return out


def editor():
    return ('<div class="side"><div class="card">'
            '<div class="etitle"><b>GitHub</b><span>Link</span></div>'
            '<div class="frow"><div class="fld k"><small>Keyword</small><b>gh</b></div><div class="fld n"><small>Name</small><b>GitHub</b></div></div>'
            '<div class="fld focus"><small>Address</small><b>https://github.com/search?q=%s<i class="caret"></i></b></div>'
            '<p class="help">%s is what you type after the keyword. Without it the link just opens.</p>'
            '<div class="brow"><span class="btn fill">Save</span><span class="btn text">Cancel</span><span class="btn text err">Delete</span></div>'
            '</div></div>')


def look_groups(compact=False, theme="Auto"):
    def ch(icon, label, sup, options, chosen):
        if compact:
            return row(icon, label, sup, cls="rtop", under=f'<div class="ubg">{choice(options, chosen)}</div>')
        return row(icon, label, sup, choice(options, chosen))
    return (
        group(
            ch("contrast", "Theme", None, ["Auto", "Light", "Dark"], theme),
            ch("palette", "Colours", "From your wallpaper, or Booklight’s own quiet blue.", ["Wallpaper", "Booklight"], "Booklight"),
        ) +
        gh("Panel") + group(
            ch("blur_on", "Glass", "How much of the desktop shows through.", ["Clear", "Balanced", "Frosted", "Solid"], "Balanced"),
            ch("layers", "Shadow", "A soft shadow around the panel.", ["Off", "Low", "Medium", "High"], "Medium"),
            row("brightness_6", "Dim the desktop", "Darken everything behind the panel a little.", sw(False)),
            ch("animation", "Opening", "How fast the panel unfolds.", ["Off", "Fast", "Medium", "Slow"], "Medium"),
        ))


def look(size, theme="Auto"):
    if size == "wide":
        return title("Look") + look_groups(theme=theme)
    if size == "c":
        return title("Look") + stage(488, 200, 0.6, 16) + look_groups(compact=True)
    return title("Look") + stage(720, 212, 0.6, 22) + look_groups()


def side_stage(note):
    return f'<div class="side">{stage(428, 340, 0.52, 56)}<p class="snote">{e(note)}</p></div>'


def results():
    dd = f'<span class="dd">Google{ms("expand_more")}</span>'
    return (title("Results") +
            gh("Search") + group(
                row("search", "Search engine", "Where the last row of the list searches.", dd),
                row("language", "Search suggestions", "Sends what you type to Google while you type. Off: nothing leaves this device until you choose a web row.", sw(True)),
            ) +
            gh("Show in the list") + group(
                row("settings", "Settings pages", None, sw(True)),
                row("calculate", "Sums", None, sw(True)),
                row("auto_awesome", "Ask Gemini", "Offers to open a question in the Gemini app.", sw(True)),
                row("keyboard", "Keyboard shortcuts", "“snap” answers with the keys that do it.", sw(True)),
            ) +
            gh("Other apps") + group(
                row("bolt", "Commands from your apps", "What your other apps offer becomes rows.", sw(True)),
                row(appicon("31", "a"), "Calendar", "New event", sw(True)),
                row(appicon("G", "b"), "Google", "Search · Voice search", sw(True)),
            ))


def states():
    return ('<div class="win L states" style="width:768px;height:416px"><div class="col" style="left:24px;top:24px;width:720px">' +
            group(
                row("settings", "At rest", "surfaceBright; inner corners 4 dp, outer corners 16 dp, 2 dp between rows.", sw(True)),
                row("settings", "Hover", "onSurface at 8 %, corners 12 dp. It fades in; the pointer stays an arrow.", sw(True), cls="hover"),
                row("settings", "Keyboard focus", "A 2 dp ring in secondary, corners 16 dp. One ring; it glides from row to row.", sw(True), cls="focus"),
                row("settings", "Pressed", "onSurface at 10 %, corners 16 dp, while the button or key is down.", sw(True), cls="pressed"),
                row("settings", "Selected", "secondaryContainer: the open item beside its detail, and the section in the rail.", sw(True), cls="sel"),
            ) + '</div></div>')


# ---------------------------------------------------------------- the page

def fig(head, meta, wins, notes):
    cols = "".join(f'<div><h4>{e(h)}</h4><p>{p}</p></div>' for h, p in notes)
    return (f'<section class="fig"><header><h2>{e(head)}</h2><span class="meta">{e(meta)}</span></header>'
            f'<div class="desk"><div class="deskin">{wins}</div></div><div class="notes n{len(notes)}">{cols}</div></section>')


figs = []

figs.append(fig("Start, at the size the window opens at", "Expanded · 1056 × 890 dp · light",
    window("x", "L", "Start", start("x"), 1056, 890),
    [("Today", "The caption bar is a lavender band over a page of another colour. The column of seven sections starts about 47 dp in from the window’s edge, and the open section carries a heavy dark outline. The title is 36 sp bold. Rows lie on the bare ground with no containers. In a wide window the column and the page drift to the middle together, and at 517 dp the Look page breaks into one letter per line."),
     ("Proposed", "Five sections in a navigation rail on the window’s leading edge, with one filled indicator. The caption bar is transparent, so the window is one surface. Rows sit in grouped containers. Material’s rule: the rail goes on the leading edge, outside every pane, and from 840 dp it may be the expanded rail.")]))

figs.append(fig("Commands", "Expanded · 1056 × 890 dp · light",
    window("x", "L", "Commands", commands("x"), 1056, 890, '<span class="sb" style="top:10px;height:300px"></span>'),
    [("What changed", "Commands and Yours are one section with a two-way switch, a search field (Ctrl F) and one list in groups. The second row shows keyboard focus: a ring and the Enter mark. The fourth shows hover. Android’s desktop guidance asks for distinct focus states, hover states and a scrollbar; Material’s lists ask for gaps between contained rows, not dividers.")]))

figs.append(fig("Look", "Expanded · 1056 × 890 dp · light",
    window("x", "L", "Look", look("x"), 1056, 890),
    [("What changed", "The panel is shown on the page, so a choice is seen as it is made; the long explanations became one line each. Choices are connected button groups, which replace segmented buttons in Material 3 Expressive; the switch is Material’s own. The column is 720 dp, the panel’s width, and starts 24 dp from the rail (Material’s margin from 600 dp up).")]))

figs.append(fig("Start, in a wide window", "Large · 1440 × 900 dp · light",
    window("x", "L", "Start", start("wide"), 1440, 900, side_stage("The real list, with the apps on this device. Nothing here can be run.")),
    [("What changed", "The column stays where it was and keeps its 720 dp; nothing drifts to the middle. Once 320 dp are free beside it (from 1332 dp), a second pane takes that room: here, the live demo. Material recommends two panes in large windows, with 24 dp between them, and Android’s desktop guide asks for a maximum width on content.")]))

figs.append(fig("Commands, yours, with the editor beside the list", "Large · 1440 × 900 dp · light",
    window("x", "L", "Commands", yours(), 1440, 900, editor() + '<span class="sb" style="top:10px;height:420px;left:752px;right:auto"></span>'),
    [("What changed", "In a wide window the editor of a link, snippet, recipe or prompt opens in the second pane, and the row it belongs to is shown selected: Material’s list-detail layout. Below 1332 dp the editor opens under its row, as it does today. “New link” is a split button; its arrow offers snippet, recipe and prompt.")]))

figs.append(fig("Look, with the preview beside it", "Large · 1440 × 900 dp · light",
    window("x", "L", "Look", look("wide"), 1440, 900, side_stage("Booklight with these settings. Action + K shows it on your desktop.")),
    [("What changed", "The preview moves from the top of the page into the second pane and stays in view while the settings scroll: Material’s supporting pane. Every title and every control keeps its place from the smaller window.")]))

figs.append(fig("Look, dark", "Large · 1440 × 900 dp · dark",
    window("x", "D", "Look", look("wide", "Dark"), 1440, 900, side_stage("Booklight with these settings. Action + K shows it on your desktop.")),
    [("What changed", "The same roles in the dark scheme: ground surfaceContainer, rows surfaceBright, the chosen option and the open section in secondaryContainer, the switch in primary. These are the panel’s colours without the glass.")]))

figs.append(fig("A narrow window", "Compact · 520 × 890 dp · light",
    window("c", "L", "Start", start("c"), 520, 890) + window("c", "L", "Commands", commands("c", marks=False), 520, 890) + window("c", "L", "Look", look("c"), 520, 890),
    [("What changed", "Under 600 dp the rail becomes a navigation bar at the bottom, margins are 16 dp, and there is one pane. Choices move under their text and an example moves under its row, so nothing is squeezed. Material: compact windows use a navigation bar, never a rail."),
     ("Against today", "Today the column stays as a strip of icons and the Look page’s text is squeezed to one letter per line (frame-narrow-look.png).")]))

figs.append(fig("Between the two", "Medium · 720 × 760 dp · light",
    window("m", "L", "Results", results(), 720, 760, '<span class="sb" style="top:10px;height:420px"></span>'),
    [("What changed", "From 600 to 839 dp the rail is Material’s collapsed rail: 96 dp, the name under the mark. A setting with five choices (the search engine) is a menu, not a row of five buttons. A window that takes a third of the Lenovo’s screen (640 dp) lands here.")]))

figs.append(fig("A row’s states", "720 dp column · light",
    states(),
    [("Why", "A mouse and a keyboard are the main inputs, so every state has a shape as well as a colour. The corner sizes are Material 3 Expressive’s list tokens (hover 12 dp; focus, press and selection 16 dp).")]))

RULES = [
    ("Compact", "under 600", "Navigation bar, 64 dp, at the bottom", "One pane; margins 16; column = width − 32"),
    ("Medium", "600 to 839", "Collapsed rail, 96 dp, leading edge", "One pane; margins 24; column = width − 144"),
    ("Expanded", "840 to 1199", "Expanded rail, 220 dp, leading edge", "One pane; column up to 720, starting 24 from the rail"),
    ("Large and up", "1200 and more", "Expanded rail, 220 dp", "The same column; from 1332 a second pane, 320 to 560, 24 from the column"),
]
rules = "".join(f"<tr><th>{a}</th><td class='num'>{b}</td><td>{c}</td><td>{d}</td></tr>" for a, b, c, d in RULES)

CSS = (HERE / "mock.css").read_text()

page = f"""<title>Booklight Window Redesign</title>
<style>
{CSS}
</style>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Code:wght@400;500&family=Google+Sans+Flex:opsz,wght,ROND@6..144,400..700,0..100&family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,0..1,0&display=block">
<div class="page">
<header class="sheet-top">
  <p class="eyebrow">Booklight · the window its icon opens</p>
  <h1>Booklight window redesign</h1>
  <p class="intro">Static mock-ups of the proposed window, drawn at 1 px = 1 dp with the system’s caption bar. Wide ones scroll sideways in their own frame. The reasons and the build order are in <code>docs/design/window-redesign.md</code>.</p>
  <div class="tablewrap"><table>
    <thead><tr><th>Window</th><th>Width, dp</th><th>Navigation</th><th>Content</th></tr></thead>
    <tbody>{rules}</tbody>
  </table></div>
</header>
{"".join(figs)}
</div>
"""

(HERE.parent.parent / "docs" / "design" / "booklight-window.html").write_text(page)
wrapper = '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">\n' + page
(HERE / "wrapper.html").write_text(wrapper)
(HERE / "wrapper-dark.html").write_text(wrapper.replace('<meta charset="utf-8">', '<meta charset="utf-8"><script>document.documentElement.dataset.theme="dark"</script>', 1))
print("wrote", len(page), "bytes")
