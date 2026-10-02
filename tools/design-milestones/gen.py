"""Generates booklight-milestones.html: what comes after 2.1 (the copy, then flights; three more milestones parked), with the designs drawn at 1 px = 1 dp.

    python3 gen.py      then publish booklight-milestones.html (it is one self-contained file).

The helpers draw the panel on the app's own lines (Metrics.kt, Rows.kt, Field.kt, Strip.kt, Footer.kt):
field 68; mark or chip from x = 20; text from 72; rows 56 on the list's 8 dp inset; disc centred on 38;
the strip and the labels end at 700; slots at a 32 dp pitch; the footer is 36 and the list's lower 8.
The prose lives in text.py; this file draws.
"""
import html as _html
import os

HERE = os.path.dirname(os.path.abspath(__file__))

# ---------------------------------------------------------------- tokens of the app's own surfaces
WALL_L = "radial-gradient(120% 90% at 15% 10%, #9DB8F5 0%, #C9D6F7 38%, #E9E6F6 70%, #F6E9E3 100%)"
WALL_D = "radial-gradient(120% 90% at 15% 10%, #2B3F78 0%, #1E2749 40%, #171A2C 72%, #1F1822 100%)"
APP_LIGHT = ("--veil: 255 255 255; --tint: .46; --rim: .8; --hair: .18; --shade: .3; --on: 23 28 43; --sel: 195 210 250; --sela: .78; --rim2: .55; "
             "--capfill: .1; --disc: .08; --pane: .62; --win: #EEF0F8; --winline: #D3D5E5; --paper2: #FFFFFF; --seltext: #B7CCFB; --err: #B3261E; "
             "--errc: #F9DEDC; --onerrc: #5F1410; --primary: #3A5BA9; --wall: " + WALL_L + "; --paper: #FFFFFF; --deskwin: #D4DAF5; --deskline: #D3D5E5; --guide: #B4126E; --ew: 2px; --soft: .16; --qage: .8;")
# Dark: the selection is the app's own, secondaryContainer #33456F at 0.42 on glass (Theme.kt, Glass.kt, design-system.md §12). The page behind is a white one: the hard case.
APP_DARK = ("--veil: 14 16 22; --tint: .52; --rim: .44; --hair: .3; --shade: .6; --on: 227 230 242; --sel: 51 69 111; --sela: .42; --rim2: .3; "
            "--capfill: .14; --disc: .12; --pane: .36; --win: #1D2030; --winline: #34384F; --paper2: #232634; --seltext: #3B4F86; --err: #F2B8B5; "
            "--errc: #8C1D18; --onerrc: #F9DEDC; --primary: #AFC6FF; --wall: " + WALL_D + "; --paper: #F1F2F7; --deskwin: #252A44; --deskline: #31344B; --guide: #FF8FC3; --ew: 1.75px; --soft: .12; --qage: 1;")

# ---------------------------------------------------------------- icons
W = "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"
ICONS = {
    "info": "M11 7h2v2h-2zm0 4h2v6h-2zm1-9a10 10 0 1 0 0 20 10 10 0 0 0 0-20zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16z",
    "open": "M14 3h7v7h-2V6.41l-8.3 8.3-1.4-1.42L17.58 5H14zM5 5h6v2H5v12h12v-6h2v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
    "window": W + "m0 4v10h16V8zm7 2h2v2h2v2h-2v2h-2v-2H9v-2h2z",
    "left": W + "m8 2v12h8V6z",
    "right": W + "m0 2v12h8V6z",
    "full": W + "m0 2v12h16V6z",
    "more": "M7.41 8.59 12 13.17l4.59-4.58L18 10l-6 6-6-6z",
    "settings": "M19.14 12.94a7 7 0 0 0 0-1.88l2.03-1.58a.5.5 0 0 0 .12-.61l-1.92-3.32a.5.5 0 0 0-.59-.22l-2.39.96a7 7 0 0 0-1.62-.94l-.36-2.54a.5.5 0 0 0-.48-.41h-3.84a.5.5 0 0 0-.47.41l-.36 2.54a7 7 0 0 0-1.62.94l-2.39-.96a.5.5 0 0 0-.59.22L2.74 8.87a.5.5 0 0 0 .12.61l2.03 1.58a7 7 0 0 0 0 1.88l-2.03 1.58a.5.5 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54a7 7 0 0 0 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.5.5 0 0 0-.12-.61zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z",
    "search": "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    "check": "M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z",
    "timer": "M15 1H9v2h6zm-4 13h2V8h-2zm8.03-6.61 1.42-1.42a11 11 0 0 0-1.41-1.41l-1.42 1.42a9 9 0 1 0 1.41 1.41zM12 20a7 7 0 1 1 0-14 7 7 0 0 1 0 14z",
    "spark": "M12 2l1.9 6.1L20 10l-6.1 1.9L12 18l-1.9-6.1L4 10l6.1-1.9zM19 15l.9 2.6 2.6.9-2.6.9L19 22l-.9-2.6-2.6-.9 2.6-.9z",
    "plus": "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z",
    "copy": "M8 3h10a2 2 0 0 1 2 2v12h-2V5H8zM5 7h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v10h10V9z",
    "send": "M3 20.5v-7L14 12 3 10.5v-7L22 12z",
    "enter": "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7z",
    "pin": "M16 3v2h-1v6l2 3v2h-4v5l-1 1-1-1v-5H7v-2l2-3V5H8V3z",
    "back": "M22 5H9a2 2 0 0 0-1.6.8L2 12l5.4 6.2A2 2 0 0 0 9 19h13a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zm-3.3 10.3-1.4 1.4-2.8-2.7-2.8 2.7-1.4-1.4 2.8-2.8-2.8-2.8 1.4-1.4 2.8 2.7 2.8-2.7 1.4 1.4-2.8 2.8z",
    "left_": "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
    # new for this plan
    "clip": "M19 3h-4.18C14.4 1.84 13.3 1 12 1s-2.4.84-2.82 2H5a2 2 0 0 0-2 2v15a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm-7-.25a1 1 0 1 1 0 2 1 1 0 0 1 0-2zM19 20H5V5h2v3h10V5h2z",
    "event": "M17 12h-5v5h5zM16 1v2H8V1H6v2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2h-1V1zm3 18H5V8h14z",
    "phone": "M6.62 10.79a15.15 15.15 0 0 0 6.59 6.59l2.2-2.2a1 1 0 0 1 1.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1.02z",
    "text": "M3 18h12v-2H3zM3 6v2h18V6zm0 7h18v-2H3z",
    "image": "M21 19V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5z",
    "replace": "M6.99 11 3 15l3.99 4v-3H14v-2H6.99zM21 9l-3.99-4v3H10v2h7.01v3z",
    # new for M5
    "plane": "M21 16v-2l-8-5V3.5a1.5 1.5 0 0 0-3 0V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5z",
    "takeoff": "M2.5 19h19v2h-19zm19.57-9.36a1.5 1.5 0 0 0-1.84-1.06L14.92 10l-6.9-6.43-1.93.51 4.14 7.17-4.97 1.33-1.97-1.54-1.45.39 1.82 3.16.77 1.33 16.57-4.43a1.5 1.5 0 0 0 1.07-1.85z",
    "landing": "M2.5 19h19v2h-19zm7.18-5.27 4.35 1.16 5.31 1.42a1.5 1.5 0 0 0 .78-2.9l-5.31-1.42-2.76-9.02L10.12 2.5v8.28L5.15 9.45l-.93-2.32-1.45-.39v5.17l1.6.43z",
}
RAW = {
    "globe": '<circle cx="12" cy="12" r="8.7" fill="none" stroke="currentColor" stroke-width="1.9"/><ellipse cx="12" cy="12" rx="3.7" ry="8.7" fill="none" stroke="currentColor" stroke-width="1.7"/><path d="M3.6 12h16.8" fill="none" stroke="currentColor" stroke-width="1.7"/>',
    # Copy clean: a link and a spark. Not scissors (they read as Cut, which stands beside it in the system's menu)
    "clean": '<path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z" transform="translate(-1.3 4) scale(.9)" fill="currentColor"/><path d="M19 .5l1.4 3.1L23.5 5l-3.1 1.4L19 9.5l-1.4-3.1L14.500 5l3.1-1.400z" fill="currentColor"/>',
    "letters": '<text x="12" y="18.5" text-anchor="middle" font-family="Google Sans Flex, system-ui, sans-serif" font-size="19" font-weight="700" fill="currentColor">ä</text>',
}


def esc(t):
    return _html.escape(t, quote=False)


def svg(name, cls="i", size=None):
    st = f' style="width:{size}px;height:{size}px"' if size else ""
    inner = RAW[name] if name in RAW else f'<path fill-rule="evenodd" d="{ICONS[name]}"/>'
    return f'<svg class="{cls}" viewBox="0 0 24 24" aria-hidden="true"{st}>{inner}</svg>'


ENTER = svg("enter", "i ent")

# ---------------------------------------------------------------- pictures (what a capture holds), drawn, never loaded
MONO = "ui-monospace, 'SF Mono', Menlo, Consolas, monospace"
SANS = "'Google Sans Flex', system-ui, sans-serif"


def _t(x, y, size, fill, text, weight=500, anchor="start", family=SANS):
    return f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}" font-family="{family}">{esc(text)}</text>'


def _terminal():
    k, d, r = "#2A2D3A", "#7A8096", "#C5221F"
    lines = [(d, "$ ./gradlew assembleDebug"), (k, "> Task :app:preBuild UP-TO-DATE"), (k, "> Task :app:mergeDebugResources UP-TO-DATE"), (r, "> Task :app:compileDebugKotlin FAILED"),
             (r, "e: Panel.kt:42:17 Unresolved reference 'Metrics'"), ("", ""), (r, "FAILURE: Build failed with an exception."), ("", ""),
             (k, "* What went wrong:"), (k, "Execution failed for task ':app:compileDebugKotlin'."), (k, "> Compilation error. See log for more details"), ("", ""), (r, "BUILD FAILED in 4s")]
    out = '<rect width="1600" height="900" fill="#FBFBFD"/>'
    for i, (c, t) in enumerate(lines):
        if t:
            out += _t(48, 92 + i * 64, 44, c, t, 500, family=MONO)
    return out


def _table():
    out = '<rect width="1200" height="600" fill="#FFFFFF"/>' + _t(48, 84, 42, "#1B1E2B", "Flights from Berlin · Sat 10 Oct", 650)
    out += '<rect x="0" y="118" width="1200" height="70" fill="#EEF0F7"/>' + _t(48, 166, 30, "#5A5F73", "City", 600) + _t(470, 166, 30, "#5A5F73", "Flight", 600) + _t(1152, 166, 30, "#5A5F73", "Price", 600, "end")
    rows = [("Lisbon", "TP 533", "€98"), ("Madrid", "IB 3675", "€112"), ("Rome", "AZ 423", "€121"), ("Athens", "A3 821", "€139")]
    for i, (a, b, c) in enumerate(rows):
        y = 256 + i * 96
        out += _t(48, y, 40, "#1B1E2B", a, 550) + _t(470, y, 40, "#3A3F52", b) + _t(1152, y, 40, "#1B1E2B", c, 600, "end") + f'<rect x="0" y="{y + 38}" width="1200" height="2" fill="#E3E5EE"/>'
    return out


def _sheet():
    out = '<rect width="1800" height="600" fill="#FFFFFF"/><rect width="1800" height="200" fill="#EEF0F7"/>'
    cols = [60, 560, 1030, 1480]
    for i, row in enumerate([("Quarter", "Revenue", "Costs", "Margin"), ("Q2", "44,930", "30,115", "33 %"), ("Q3", "48,210", "31,870", "34 %")]):
        for x, t in zip(cols, row):
            out += _t(x, 128 + i * 200, 68, "#5A5F73" if i == 0 else "#1B1E2B", t, 620 if i == 0 else 520)
    out += '<rect y="198" width="1800" height="4" fill="#D9DCE8"/><rect y="398" width="1800" height="4" fill="#E3E5EE"/>'
    for x in cols[1:]:
        out += f'<rect x="{x - 44}" y="0" width="4" height="600" fill="#E3E5EE"/>'
    return out


def _screen():
    out = '<rect width="1920" height="1200" fill="#5B79D6"/><circle cx="1500" cy="140" r="520" fill="#7C95E4"/>'
    out += '<rect x="110" y="110" width="930" height="820" rx="28" fill="#FFFFFF"/>' + _t(160, 196, 46, "#1B1E2B", "Calendar", 650)
    out += '<rect x="160" y="250" width="830" height="130" rx="20" fill="#DCE6FF"/>' + _t(196, 332, 44, "#1F3A8A", "Design review 14:00", 600)
    out += '<rect x="160" y="410" width="830" height="130" rx="20" fill="#EEF0F7"/>' + _t(196, 492, 44, "#3A3F52", "Lunch with Jonas 12:30", 550)
    out += '<rect x="160" y="570" width="830" height="130" rx="20" fill="#EEF0F7"/>' + _t(196, 652, 44, "#3A3F52", "Dentist 16:00", 550)
    out += '<rect x="1100" y="230" width="720" height="700" rx="28" fill="#1E2030"/>' + _t(1150, 316, 40, "#E6E8F2", "Notes: launch checklist", 600)
    for i, t in enumerate(["store text", "release notes", "tag v2.2"]):
        out += _t(1150, 410 + i * 76, 38, "#B9BED3", "[ ] " + t, 500, family=MONO)
    out += '<rect x="560" y="1070" width="800" height="92" rx="46" fill="#FFFFFF" fill-opacity=".86"/>'
    for i, c in enumerate(["#DB4437", "#4285F4", "#0F9D58", "#F4B400", "#7E57C2", "#26303F"]):
        out += f'<circle cx="{650 + i * 124}" cy="1116" r="30" fill="{c}"/>'
    return out


def _invite():
    out = '<rect width="1200" height="1200" fill="#FFF4E3"/><rect x="60" y="60" width="1080" height="1080" rx="40" fill="none" stroke="#E0A84F" stroke-width="8"/>'
    out += _t(600, 330, 78, "#8A5A12", "You’re invited", 500, "middle") + _t(600, 540, 164, "#3B2606", "Lena’s 30th", 720, "middle")
    out += _t(600, 760, 84, "#3B2606", "Sat 10 Oct, 18:00", 600, "middle") + _t(600, 900, 64, "#8A5A12", "Kastanienallee 12", 500, "middle")
    return out


def _chat():
    out = '<rect width="720" height="1280" fill="#F4F5FA"/><rect width="720" height="150" fill="#FFFFFF"/><circle cx="84" cy="76" r="38" fill="#7E57C2"/>' + _t(146, 92, 46, "#1B1E2B", "Anna", 650)
    out += '<rect x="40" y="230" width="520" height="120" rx="44" fill="#FFFFFF"/>' + _t(78, 306, 42, "#1B1E2B", "Are we still on for 7?")
    out += '<rect x="170" y="400" width="510" height="190" rx="44" fill="#3D5BD9"/>' + _t(208, 474, 42, "#FFFFFF", "Yes. Table at Lokal,") + _t(208, 536, 42, "#FFFFFF", "under Anna")
    out += '<rect x="40" y="640" width="360" height="120" rx="44" fill="#FFFFFF"/>' + _t(78, 716, 42, "#1B1E2B", "See you there")
    out += '<rect x="40" y="1130" width="640" height="104" rx="52" fill="#FFFFFF"/>' + _t(90, 1196, 40, "#8A8FA3", "Message")
    return out


def _photo():
    return ('<rect width="1200" height="900" fill="#A9CFF2"/><rect y="520" width="1200" height="380" fill="#DDEBF7"/><circle cx="880" cy="250" r="110" fill="#FFD772"/>'
            '<path d="M0 620 240 380 470 560 700 300 1000 560 1200 440V900H0z" fill="#6E9FA0"/><path d="M0 760 300 560 620 740 900 600 1200 760V900H0z" fill="#3E7566"/>')


PICS = {"term": (1600, 900, _terminal), "table": (1200, 600, _table), "sheet": (1800, 600, _sheet), "screen": (1920, 1200, _screen),
        "invite": (1200, 1200, _invite), "chat": (720, 1280, _chat), "photo": (1200, 900, _photo)}


def fit(pw, ph, mw=176, mh=104):
    """A picture fitted into the row's 176 x 104 in its own shape: never cropped, never padded, never larger than it is."""
    s = min(mw / pw, mh / ph, 1)
    return round(pw * s), round(ph * s)


def picture(kind, w, h, crop=False, corner=False):
    """crop: fill the box and cut what does not fit; corner: cut from the picture's top-left, as a mark is."""
    pw, ph, draw = PICS[kind]
    fit_ = ("xMinYMin" if corner else "xMidYMid") + (" slice" if crop else " meet")
    return f'<svg viewBox="0 0 {pw} {ph}" width="{w}" height="{h}" preserveAspectRatio="{fit_}" role="img" aria-label="a capture">{draw()}</svg>'


# ---------------------------------------------------------------- the panel's parts
def app(letter, colour):
    return f'<span class="ic app" style="--c:{colour}">{letter}</span>'


def sym(name):
    return f'<span class="ic sym">{svg(name)}</span>'


def slot(icon, name=None, armed=False, off=False):
    if armed:
        return f'<span class="a arm">{svg(icon)}<b>{name}</b>{ENTER}</span>'
    return f'<span class="a{" off" if off else ""}">{svg(icon)}</span>'


def strip(*slots, attrs=""):
    """slots: (icon, name) at rest, (icon, name, True) armed, (icon, name, "off") dim."""
    out = "".join(slot(s[0], s[1], len(s) > 2 and s[2] is True, len(s) > 2 and s[2] == "off") for s in slots)
    return f'<span class="strip"{attrs}>{out}</span>'


def kind(text):
    return f'<span class="kind">{text}</span>'


def cap(k):
    return f'<span class="fk">{svg("back") if k == "⌫" else k}</span>'


def row(icon_html, title, sub=None, right="", on=False, cls="", attrs=""):
    t = f'<span class="t"><b>{title}</b>{f"<i>{sub}</i>" if sub else ""}</span>'
    return f'<div class="r{" on" if on else ""}{" " + cls if cls else ""}"{attrs}>{icon_html}{t}{right}</div>'


def ans(caption, text, right="", icon="spark", on=True, grown=False, own=False, cls="", attrs="", cap_attrs="", txt_attrs=""):
    """An answer row: 92, or 136 once it has grown. The caption, then the text on 22 dp lines."""
    return (f'<div class="r ans{" on" if on else ""}{" grown" if grown else ""}{" " + cls if cls else ""}"{attrs}>{sym(icon)}'
            f'<div class="t"><div class="cap"{cap_attrs}>{caption}</div><div class="txt{" own" if own else ""}"{txt_attrs}>{text}</div></div>{right}</div>')


def picrow(pic, caption, lines, right="", on=False, low=False, size=None):
    """A picture's row: the picture hangs from x = 24, y = 16; the text starts at x = 220 whatever its shape.
    The caption is the picture's size; the text the recogniser read stands under it in the small type, wrapping, so the answer is the only title-size text."""
    pw, ph, _ = PICS[pic]
    w, h = size or fit(pw, ph, 176, 60 if low else 104)
    body = "".join(f'<span class="ln">{esc(l)}</span>' for l in lines)
    return (f'<div class="r pic{" on" if on else ""}{" low" if low else ""}"><div class="pslot"><div class="pimg" style="width:{w}px;height:{h}px">{picture(pic, w, h)}</div></div>'
            f'<div class="t"><div class="cap">{caption}</div><div class="txt rec">{body}</div></div>{right}</div>')


def markpic(pic="term"):
    """A picture as a mark in a 56 or 92 dp row: 36 dp like every mark, radius 11, the ring, cut from the picture's top-left."""
    return f'<span class="ic markpic">{picture(pic, 36, 36, True, True)}</span>'


def say(icon, text):
    """A row with nothing to run: row one's seat, no highlight, the small type. Drawn as the quiet line is."""
    return f'<div class="r say">{sym(icon)}<span class="qt">{text}</span></div>'


def edge(P=1809):
    """The white light on the outline (design-system.md §12): the outline at full white, 1.75 dp in dark and 2.0 in light, its inner edge softened over 3 dp.
    A front of 40 dp and a tail behind it, measured along the outline: nine dashes that share their front, so the tail fades towards its end."""
    g = "".join(f'<g style="--t:{t};opacity:{o}"><rect class="e1" pathLength="{P}"/><rect class="e2" pathLength="{P}"/><rect class="e3" pathLength="{P}"/></g>'
                for t, o in [(0, 1)] + [(k / 8, .2) for k in range(1, 9)])
    return f'<svg class="edge" style="--P:{P}" aria-hidden="true">{g}</svg>'


def chip(icon, name):
    return f'<span class="chip">{svg(icon)}<span class="nm">{name}</span></span>'


def picchip(pic="term", name="Picture"):
    return f'<span class="chip pic"><span class="thumb">{picture(pic, 28, 28, True)}</span><span class="nm">{name}</span></span>'


def fld(text="", chip_html=None, ph=None, esc_cap=None):
    """The field. The G or the chip, the text and its caret, the placeholder while nothing is typed, the esc cap while there is no footer."""
    mark = chip_html if chip_html else '<span class="g">G</span>'
    hint = f'<span class="ph">{ph}</span>' if ph and not text else ""
    key = f'<span class="fk esc">{esc_cap}</span>' if esc_cap else ""
    return f'<div class="fld">{mark}<span class="typed">{text}</span><span class="caret"></span>{hint}{key}</div>'


def foot(*pairs, done=None, attrs="", done_attrs=""):
    left = f'<span class="done"{done_attrs}>{svg("check")}{done}</span>' if done else ""
    return f'<div class="foot"{attrs}>{left}' + "".join(f"{cap(k)}{v}" for k, v in pairs) + "</div>"


def quiet(age, what, key="tab", parts=False):
    """The quiet line: row one's seat, no pill. One text in two inks; the cap under the field's esc cap."""
    d = [' data-i="0"', ' data-i="1"', ' data-i="2"'] if parts else ["", "", ""]
    return (f'<div class="quiet"><span class="ic sym"{d[0]}>{svg("clip")}</span><span class="qt"{d[1]}><span class="age">{age}</span> · {what}</span>'
            f'<span class="fk"{d[2]}>{key}</span></div>')


def panel(inner, cls="", attrs=""):
    return f'<div class="panel{" " + cls if cls else ""}"{" " + attrs if attrs else ""}>{inner}</div>'


def rows(inner, attrs=""):
    return f'<div class="rows"{attrs}>{inner}</div>'


def desk(*children, theme="light", cls="", style=""):
    st = f' style="{style}"' if style else ""
    return f'<div class="scroll"><div class="desk th-{theme}{" " + cls if cls else ""}"{st}>{"".join(children)}</div></div>'


def dtag(text, over=False):
    return f'<span class="dtag{" over" if over else ""}">{text}</span>'


def guided(panel_html, xs, low=(20, 712)):
    g = "".join(f'<i class="gl{" rt" if x == 700 else ""}{" lo" if x in low else ""}" style="left:{x}px"><b>{x}</b></i>' for x in xs)
    return f'<div class="pwrap guided">{g}{panel_html}</div>'


def shot(label, drawing, caption="", extra=""):
    lab = f'<div class="lab">{label}</div>' if label else ""
    c = f'<p class="capt">{caption}</p>' if caption else ""
    return f'<div class="shot">{lab}{drawing}{extra}{c}</div>'


def screen(n, title, body, why=""):
    w = f'<p class="why">{why}</p>' if why else ""
    return f'<div class="screen"><div class="scrhead"><span class="scrn">Screen {n}</span><h4>{title}</h4></div>{w}{body}</div>'


def controls(name, *buttons, note=""):
    b = "".join(f'<button type="button" data-k="{k}">{v}</button>' for k, v in buttons)
    return f'<div class="controls" data-for="{name}"><span class="lbl">Try it</span>{b}{f"<span class=muted>{note}</span>" if note else ""}</div>'


# ---------------------------------------------------------------- strings, English and German, with the drafts for M4
L = {
    "en": dict(search="Search apps, settings and the web", what="What to do with it", open="Open", clean="Copy clean", event="Event", phone="Phone", device="On this device",
               fix="Fix spelling", shorter="Shorter", tr="In German", actions="Actions", close="Close", esc="esc", copychip="Dinner with Anna on…",
               when="Fri 2 Oct, 19:00–20:00", title="Dinner with Anna", line=("Copied 20 s ago", "a link and a date")),
    "de": dict(search="Apps, Einstellungen und das Web durchsuchen", what="Was damit geschehen soll", open="Öffnen", clean="Sauber kopieren", event="Termin", phone="Telefon",
               device="Auf diesem Gerät", fix="Korrigieren", shorter="Kürzer", tr="Auf Englisch", actions="Aktionen", close="Schließen", esc="esc", copychip="Abendessen mit Anna am…",
               when="Fr., 2. Okt., 19:00–20:00", title="Abendessen mit Anna", line=("Kopiert vor 20 s", "ein Link und ein Datum")),
    "fr": dict(search="Rechercher des applis, des réglages et sur le Web", what="Que faut-il en faire", open="Ouvrir", clean="Copie propre", event="Événement", phone="Téléphone",
               device="Sur cet appareil", fix="Corriger", shorter="Plus court", tr="En anglais", actions="Actions", close="Fermer", esc="échap", copychip="Dîner avec Anna vendredi…",
               when="ven. 2 oct., 19:00–20:00", title="Dîner avec Anna", line=("Copié il y a 20 s", "un lien et une date")),
}


def line_panel(lang, age=None, what=None, cls="", attrs=""):
    s = L[lang]
    return panel(fld(ph=s["search"], esc_cap=s["esc"]) + quiet(age or s["line"][0], what or s["line"][1]), cls, attrs)


def list_rows(lang, sel=0):
    """Screen 2: what is in the copy, then what the model can do. sel: the row the pill is on."""
    s = L[lang]
    leave = {"en": "Leave", "de": "Verlassen", "fr": "Quitter"}[lang]
    ask = {"en": "Ask", "de": "Fragen", "fr": "Demander"}[lang]
    r = [row(sym("globe"), "example.com/table", right=strip(("open", s["open"], True), ("clean", s["clean"])) if sel == 0 else kind("Link"), on=sel == 0, attrs=' data-i="0"'),
         row(sym("event"), s["when"], s["title"], right=kind(s["event"]), attrs=' data-i="1"'),
         row(sym("phone"), "+49 30 5550 1234", right=kind(s["phone"]), attrs=' data-i="2"')]
    for i, key in enumerate(("fix", "shorter", "tr")):
        on = sel == 3 + i
        r.append(row(sym("spark"), s[key], right=strip(("spark", ask, True)) if on else kind(s["device"]), on=on, attrs=f' data-i="{3 + i}"'))
    f = foot(("tab", s["actions"]), (s["esc"], s["close"])) if sel == 0 else foot(("⌫", leave), (s["esc"], s["close"]))
    return rows("".join(r)) + f


def list_panel(lang, sel=0, cls="", attrs=""):
    s = L[lang]
    return panel(fld(chip_html=chip("clip", s["copychip"]), ph=s["what"]) + list_rows(lang, sel), cls, attrs)


AGEMINI = ("send", "Open in Gemini")
COPY3 = strip(("copy", "Copy", True), ("pin", "Pin"), AGEMINI)
COPY2 = strip(("copy", "Copy", True), ("pin", "Pin"))
REPLACE4 = strip(("replace", "Replace", True), ("copy", "Copy"), ("pin", "Pin"), AGEMINI)
ASK = strip(("spark", "Ask", True))
DE_TEXT = "Am Freitag treffen wir uns um 15 Uhr im Büro, bitte bringt die unterschriebenen Formulare und euren Laptop mit."
EN_TEXT = "On Friday we meet at 3 pm in the office. Please bring the signed forms and your laptop."
PIC_LINES = ["e: Panel.kt:42:17 Unresolved reference 'Metrics'", "FAILURE: Build failed with an exception.", "> Compilation error. See log for more details"]
PIC_ANSWER = "Panel.kt uses a name, Metrics, that it cannot find. It is in line 42, column 17. An import is probably missing."
PICTXT = strip(("copy", "Copy text", True), ("text", "Use text"), ("pin", "Pin"))

D = {}   # every drawing, by name; text.py places them

# ---------------------------------------------------------------- the ground
D["ground"] = desk(guided(panel(
    fld(chip_html=chip("clip", "Dinner with Anna on…"), ph="What to do with it")
    + rows(row(sym("globe"), "example.com/table", right=strip(("open", "Open", True), ("clean", "Copy clean")), on=True)
           + row(sym("event"), "Fri 2 Oct, 19:00–20:00", "Dinner with Anna", right=kind("Event"))
           + row(sym("spark"), "Fix spelling", right=kind("On this device")))
    + foot(("tab", "Actions"), ("esc", "Close"))), [8, 20, 38, 72, 700, 712]))

# ---------------------------------------------------------------- M1
APPSTRIP = strip(("open", "Open", True), ("window", "New window"), ("info", "App info"), ("full", "Maximise"), ("left", "Left half"), ("right", "Right half"), ("more", "More"))
c1 = panel(
    '<i class="seam"></i>'
    '<div class="fld"><span class="g" id="c1g">G</span><span class="chip" id="c1chip" hidden>' + svg("clip") + '<span class="nm">Dinner with Anna on…</span></span>'
    '<span class="typed" id="c1typed"></span><span class="caret"></span>'
    '<span class="ph" id="c1ph" data-empty="Search apps, settings and the web" data-chip="What to do with it">Search apps, settings and the web</span><span class="fk esc" id="c1esc">esc</span></div>'
    '<div class="under" id="c1under">'
    '<div data-b="line">' + quiet("Copied 20 s ago", "a link and a date", parts=True) + "</div>"
    '<div data-b="list" hidden>' + list_rows("en") + "</div>"
    '<div data-b="typed" hidden>' + rows(row(app("D", "#4285F4"), "Docs", right=APPSTRIP, on=True, attrs=' data-i="0"') + row(sym("settings"), "Display", right=kind("Settings"), attrs=' data-i="1"')
                                           + row(sym("search"), "Search Google for “d”", right=kind("Web"), attrs=' data-i="2"')) + foot(("tab", "Actions"), ("esc", "Close")) + "</div>"
    "</div>" + edge(1665), attrs='id="c1"')
D["c1"] = desk(c1) + controls("c1", ("key", "The key"), ("key200", "The key, the line at 200 ms"), ("tab", "Tab"), ("letter", "A letter"), ("back", "Backspace"), ("theme", "Dark"),
                              note="The key replays the opening, the line and the reflection. The reflection shows in dark.")

# 140 px to the second. Medium, design-system.md §12: the glass is at rest at 420 ms; the line 320 ms after the glass is 85 % open (257 ms);
# the reflection 2.4 s after the panel has opened, 1 ms for each dp of outline (1,665 dp round the 140 dp panel).
PX = 140
D["timeline"] = ('<div class="tlwrap"><div class="tl" role="img" aria-label="The key at 0 seconds. The glass is at rest at 0.42 seconds. The line rises between 0.58 and 0.94 seconds. Then nothing moves. From 2.8 seconds the white reflection runs once round the edge, for about 1.7 seconds.">'
                 '<span class="lb" style="left:0;top:0">The key. The glass is at rest at 0.42 s</span>' f'<span class="lb" style="left:{round(2.82 * PX)}px;top:0">The white reflection, once round</span>'
                 f'<i class="seg" style="left:0;width:{round(.42 * PX)}px"></i><i class="seg" style="left:{round(.577 * PX)}px;width:{round(.364 * PX)}px"></i>'
                 f'<i class="seg still" style="left:{round(.94 * PX) + 4}px;width:{round(1.88 * PX) - 8}px">nothing moves</i><i class="seg white" style="left:{round(2.82 * PX)}px;width:{round(1.665 * PX)}px"></i><i class="tk"></i>'
                 f'<span class="lb" style="left:{round(.577 * PX)}px;top:46px">The line rises, 0.58 to 0.94 s</span><span class="lb" style="left:{round(2.82 * PX)}px;top:46px">from 2.8 s, for about 1.7 s</span><i class="ax"></i>'
                 + "".join(f'<i class="tick" style="left:{n * PX}px"></i><span class="tm" style="left:{n * PX + 5}px">{n} s</span>' for n in range(6)) + "</div></div>")

D["s1dark"] = desk(dtag("English"), line_panel("en"), dtag("German"), line_panel("de"), theme="dark")
D["s1states"] = desk(dtag("German"), line_panel("de"), dtag("Just copied, nothing found in it yet"), line_panel("en", "Copied just now", "text"),
                     dtag("Three things or more"), line_panel("en", "Copied 1 min ago", "a link, a date and more"))
D["s2light"] = desk(list_panel("en"))
D["s2dark"] = desk(dtag("The highlight on the link"), list_panel("en"), dtag("The highlight on a model’s row"), list_panel("en", sel=5), theme="dark")

clipchip_de = chip("clip", "Am Freitag treffen wir…")
D["s3think"] = desk(panel(fld(chip_html=clipchip_de, ph="What to do with it")
                          + rows(ans("In English", DE_TEXT, ASK, own=True) + row(sym("spark"), "Fix spelling", right=kind("On this device"), cls="fade") + row(sym("spark"), "Shorter", right=kind("On this device"), cls="fade"))
                          + foot(("⌫", "Leave"), ("esc", "Close"))))
answered = panel(fld(chip_html=clipchip_de, ph="What to do with it") + rows(ans("In English · on this device", EN_TEXT, COPY3)) + foot(("tab", "Actions"), ("esc", "Close")))
D["s3done"] = desk(answered)
D["s3dark"] = desk(panel(fld(chip_html=clipchip_de, ph="What to do with it") + rows(ans("In English", DE_TEXT, ASK, own=True)) + foot(("⌫", "Leave"), ("esc", "Close")) + edge(1809), cls="lapstill"), theme="dark")
NOTES = "Monday sync. Anna: send the contract to legal by Wednesday. Jonas: book the room for the workshop and ask IT about the projector. Open: who writes the release notes."
D["s3small"] = desk(
    dtag("An instruction"),
    panel(fld("pull out the tasks as a list", chip_html=chip("clip", "Notes from Monday…")) + rows(ans("With what you copied", NOTES, ASK, own=True)) + foot(("esc", "Close"))),
    dtag("The model does not answer"),
    panel(fld(chip_html=clipchip_de, ph="What to do with it") + rows(ans("No answer on this device this time", DE_TEXT, strip(("send", "Ask Gemini", True)), own=True)) + foot(("⌫", "Leave"), ("esc", "Close"))))

trchip = chip("spark", "Translate")
D["s9"] = desk(
    dtag("Under the copy’s chip, a typed language is the translation into it"),
    panel(fld("danish", chip_html=clipchip_de) + rows(row(sym("spark"), "In Danish", right=ASK, on=True)) + foot(("esc", "Close"))),
    dtag("From the empty field: the keyword, and nothing typed yet"),
    panel(fld(chip_html=trchip, ph="A language, then the text", esc_cap="esc")),
    dtag("A language, then the text"),
    panel(fld("danish see you on Saturday", chip_html=trchip) + rows(ans("In Danish · on this device", "Vi ses på lørdag", COPY3)) + foot(("tab", "Actions"), ("esc", "Close"))))

# ---------------------------------------------------------------- M2
PICCAP = "1600 × 900"   # the chip and the label at the right say “Picture”; the caption says only what they do not
pic_idle = panel(fld(chip_html=picchip(), ph="A question about this picture") + rows(picrow("term", PICCAP, PIC_LINES, PICTXT, on=True)) + foot(("tab", "Actions"), ("esc", "Close")))
D["s4idle"] = desk(pic_idle)


def pic_answered(attrs="", live=False):
    a = ans("From the picture · on this device", PIC_ANSWER,
            (strip(("spark", "Ask", True), attrs=' data-s="ask" hidden') + strip(("copy", "Copy", True), ("pin", "Pin"), attrs=' data-s="done"')) if live else COPY2,
            attrs=" data-ans" if live else "", cap_attrs=' data-a="About this picture" data-b="From the picture · on this device"' if live else "",
            txt_attrs=f' data-own="" data-said="{PIC_ANSWER}"' if live else "")
    # while it is being answered there is one action and text in the field: esc alone. Answered: Copy and Pin, so Tab has somewhere to go
    f = (foot(("esc", "Close"), attrs=' data-f="ask" hidden') + foot(("tab", "Actions"), ("esc", "Close"), attrs=' data-f="done"')) if live else foot(("tab", "Actions"), ("esc", "Close"))
    return panel(fld("what is the error, and where", chip_html=picchip()) + rows(a + picrow("term", PICCAP, PIC_LINES, kind("Picture"))) + f + (edge(1985) if live else ""), attrs=attrs)


D["s4done"] = desk(pic_answered('id="p4"', live=True)) + controls("p4", ("ask", "Enter"), ("reset", "Again"), ("theme", "Dark"), note="Enter asks: the light runs its slow lap, then the answer is written in. The light shows in dark.")
D["s4dark"] = desk(dtag("The highlight on the picture’s row"), pic_idle, dtag("The highlight moved off it: a bright picture on bare dark glass"), pic_answered(), theme="dark")

SHAPES = [("sheet", "1800 × 600", ["Quarter Revenue Costs Margin", "Q2 44,930 30,115 33 %", "Q3 48,210 31,870 34 %"]),
          ("table", "1200 × 600", ["Flights from Berlin · Sat 10 Oct", "City Flight Price", "Lisbon TP 533 €98", "Madrid IB 3675 €112"]),
          ("screen", "1920 × 1200", ["Calendar", "Design review 14:00", "Lunch with Jonas 12:30", "Notes: launch checklist"]),
          ("invite", "1200 × 1200", ["You’re invited", "Lena’s 30th", "Sat 10 Oct, 18:00", "Kastanienallee 12"]),
          ("chat", "720 × 1280", ["Anna", "Are we still on for 7?", "Yes. Table at Lokal,", "under Anna"])]
D["s5sheet"] = desk(guided(panel(rows("".join(picrow(p, c, l, PICTXT if i == 0 else kind("Picture"), on=i == 0) for i, (p, c, l) in enumerate(SHAPES))), cls="cut"), [24, 220, 700]))
D["s5states"] = desk(
    dtag("A whole screen: asked through its text"),
    panel(fld("when is the design review", chip_html=picchip("screen")) + rows(ans("From the picture’s text · on this device", "The design review is at 14:00.", COPY2) + picrow("screen", "1920 × 1200", SHAPES[2][2], kind("Picture"))) + foot(("tab", "Actions"), ("esc", "Close"))),
    dtag("No text in this picture"),
    panel(fld(chip_html=picchip("photo"), ph="A question about this picture") + rows(picrow("photo", "1200 × 900", ["No text in this picture"], strip(("copy", "Copy text", "off"), ("text", "Use text", "off"), ("pin", "Pin", True)), on=True)) + foot(("⌫", "Leave"), ("esc", "Close"))),
    dtag("A picture that cannot be opened"),
    panel(fld(chip_html=chip("image", "Picture"), ph="A question about this picture") + rows(say("image", "This picture could not be read")) + foot(("⌫", "Leave"), ("esc", "Close"))))


def pinpic(pic, w, h, label=None, bands=False):
    pw, ph, _ = PICS[pic]
    iw, ih = (w, round(w * ph / pw)) if bands else (w, h)
    box = f'<div class="pin pinpic" style="width:{w}px;height:{h}px">{picture(pic, iw, ih)}</div>'
    return f'<div class="pinbox">{box}<span>{label}</span></div>' if label else box


def pintext(w, h, inner, label):
    return f'<div class="pinbox"><div class="pin" style="width:{w}px;height:{h}px"><div class="in">{inner}</div></div><span>{label}</span></div>'


code = [(38, "@Composable"), (39, "fun Panel(model: OverlayModel) {"), (40, "    val motion = LocalMotion.current"), (41, "    val scheme = MaterialTheme.colorScheme"),
        (42, "    val h = Metrics.height(model)"), (43, "    val open = remember { Animatable(0f) }"), (44, "    Box(Modifier.width(720.dp).height(h)) {"), (45, "        Field(model, field, onChange, focus)"),
        (46, "        ResultsBody(model, icons, onRun)"), (47, "    }"), (48, "}")]
editor = ('<div class="win editor" style="position:absolute;left:0;top:64px"><div class="syscap">Panel.kt<span>–  ▢  ✕</span></div><div class="code">'
          + "".join(f'<div{" class=hit" if n == 42 else ""}><i>{n}</i>{esc(t)}</div>' for n, t in code) + "</div></div>")
D["s6desk"] = desk(f'<div class="stage" style="width:840px;height:372px">{editor}<div style="position:absolute;left:420px;top:0">{pinpic("term", 420, 236)}</div>'
                   '<span class="note" style="left:620px;top:250px">The capture, whole, on top of the editor. It never takes the keys.</span></div>', cls="plain")
D["s6sizes"] = desk('<div class="pinrow">'
                    + pinpic("table", 280, 140, "2 : 1 · 280 × 140, measured")
                    + pinpic("screen", 280, 175, "16 : 10 · about 280 × 175, not verified")
                    + pintext(280, 118, '<div class="pcap">tea · set for 14:12</div><div class="fig">7:42</div>', "2.0’s timer · 280 × 118")
                    + pintext(280, 118, '<div class="ptxt">Gate B22, boarding 16:40.<br>Ask about the aisle seat.</div>', "2.0’s note · 280 × 118")
                    + "</div>", cls="plain")
D["s6dark"] = desk('<div class="pinrow">' + pinpic("sheet", 280, 118, "3 : 1 · bands above and below", bands=True)
                   + pinpic("table", 280, 140, "2 : 1 · fills its window") + "</div>", theme="dark", cls="plain")
mark = markpic("term")
D["s6pin"] = desk(panel(fld(chip_html=chip("pin", "Pin"), ph="A line to keep on top")
                        + rows(row(mark, "Picture", "Stays on top of your windows", right=strip(("pin", "Unpin", True), ("image", "Copy image")), on=True)) + foot(("tab", "Actions"), ("esc", "Close"))))
low = picrow("term", PICCAP, PIC_LINES[:2], PICTXT, on=True, low=True)
D["s6fall"] = desk(
    dtag("First fallback: the picture at most 60 dp high, in a 92 dp row"),
    panel(fld(chip_html=picchip(), ph="A question about this picture") + rows(low) + foot(("tab", "Actions"), ("esc", "Close"))),
    dtag("Second fallback: the picture as a 36 dp mark; it is seen large only in the pin"),
    panel(fld(chip_html=picchip(), ph="A question about this picture")
          + rows(f'<div class="r ans on">{mark}<div class="t"><div class="cap">{PICCAP}</div><div class="txt rec">' + "".join(f'<span class="ln">{esc(l)}</span>' for l in PIC_LINES[:2]) + f"</div></div>{PICTXT}</div>")
          + foot(("tab", "Actions"), ("esc", "Close"))), theme="dark")
D["s4line"] = desk(line_panel("en", "Copied just now", "a picture"))

# ---------------------------------------------------------------- M3
WRONG = "i think we shoud meet on friday becuase the room is free"
RIGHT = "I think we should meet on Friday because the room is free."


def mail(sel_id=None, style=""):
    ids = f' id="{sel_id}" data-fixed="{RIGHT}"' if sel_id else ""
    st = f' style="{style}"' if style else ""
    m = f'<mark class="selx"{ids}>{WRONG}</mark>'
    return (f'<div class="win mail"{st}><div class="syscap">New message<span>–  ▢  ✕</span></div>'
            '<div class="line"><span>To</span>Jonas Weber</div><div class="line"><span>Subject</span>The workshop room</div>'
            f'<div class="bodytx"><p>Hi Jonas,</p><p>{m} Can you book it for ten?</p><p>Thanks, Alex</p></div></div>')


menu = ('<div class="sysmenu" style="position:absolute;left:300px;top:238px"><div>Cut</div><div>Copy</div><div>Paste</div><div>Select all</div><hr>'
        '<div class="ours">Fix spelling</div><div class="ours">Translate</div><div class="ours">Booklight</div></div>')
D["s7menu"] = desk(f'<div class="stage" style="width:760px;height:500px">{mail(style="position:absolute;left:0;top:0")}{menu}'
                   '<span class="note" style="left:524px;top:300px">The system draws this menu. Its order and icons are not verified. The three names in bold are Booklight’s.</span></div>', cls="plain")
fixchip = chip("text", "i think we shoud meet…")
s7 = panel(
    '<i class="seam"></i>' + fld(chip_html=fixchip, ph="What to do with it")
    + '<div class="under" id="s7under"><div data-b="row">'
    + rows(ans("Fix spelling · on this device", RIGHT,
               strip(("spark", "Ask", True), attrs=' data-s="ask" hidden') + strip(("replace", "Replace", True), ("copy", "Copy"), ("pin", "Pin"), AGEMINI, attrs=' data-s="done"'),
               attrs=' data-ans data-i="0"', cap_attrs=' data-a="Fix spelling" data-b="Fix spelling · on this device"', txt_attrs=f' data-own="{WRONG}" data-said="{RIGHT}"'))
    # while it is being answered: a chip, an empty field and one action, so the way back and esc. Answered: four actions
    + foot(("⌫", "Leave"), ("esc", "Close"), attrs=' data-f="ask" hidden') + foot(("tab", "Actions"), ("esc", "Close"), done="Replaced", attrs=' data-f="done"', done_attrs=' id="s7done" hidden')
    + "</div></div>" + edge(1809), attrs='id="s7"')
D["s7live"] = (desk(s7, mail("s7sel", "position:absolute;z-index:-1;left:50%;margin-left:-320px;top:172px"), cls="plain", style="min-height:466px")
               + controls("s7", ("play", "Pick “Fix spelling”"), ("enter", "Enter"), ("reset", "Again"), ("theme", "Dark"), note="Enter replaces the sentence in the mail behind the panel. The light shows in dark."))
fixed = rows(ans("Fix spelling · on this device", RIGHT, REPLACE4))
D["s7dark"] = desk(panel(fld(chip_html=fixchip, ph="What to do with it") + fixed + foot(("tab", "Actions"), ("esc", "Close"))), theme="dark")
D["s7after"] = desk(panel(fld(chip_html=fixchip, ph="What to do with it") + fixed + foot(("tab", "Actions"), ("esc", "Close"), done="Replaced")))

abc = chip("letters", "Letters")
PLAIN = "Gruesse aus Koeln, ich komme spaeter"
WITH = "Grüße aus Köln, ich komme später"
D["s8"] = desk(
    dtag("From a selection: right-click, Booklight, the row “Put the letters back”"),
    panel(fld(chip_html=chip("text", "Gruesse aus Koeln, ich…"), ph="What to do with it")
          + rows(ans("Put the letters back · on this device", WITH, strip(("replace", "Replace", True), ("copy", "Copy"), ("pin", "Pin")))) + foot(("tab", "Actions"), ("esc", "Close"))),
    dtag("Typed: abc and a sentence"),
    panel(fld(PLAIN, chip_html=abc) + rows(ans("With its letters · on this device", WITH, COPY2)) + foot(("tab", "Actions"), ("esc", "Close"))),
    dtag("Refused: the model changed a word, so its answer is not shown"),
    panel(fld(PLAIN, chip_html=abc) + rows(ans("Could not do it without changing your words", PLAIN, strip(("send", "Ask Gemini", True)), own=True)) + foot(("esc", "Close"))),
    dtag("One word"),
    panel(fld("Koeln", chip_html=abc) + rows(ans("With its letters · on this device", "Köln", COPY2)) + foot(("tab", "Actions"), ("esc", "Close"))))

# ---------------------------------------------------------------- M4
D["s10line"] = desk(dtag("English"), line_panel("en"), dtag("German"), line_panel("de"), dtag("French, draft"), line_panel("fr"))
D["s10list"] = desk(dtag("English"), list_panel("en"), dtag("German"), list_panel("de"), dtag("French, draft"), list_panel("fr"))


def event_panel(chipname, typed, caption, when, title, create, web, kindweb, actions, close, esccap="esc"):
    slots = (f'<div class="t"><div class="cap">{caption}</div><div class="sl"><span><em>{when[0]}</em>{when[1]}</span><span><em>{title[0]}</em>{title[1]}</span></div></div>')
    r = f'<div class="r tall on">{sym("event")}{slots}{strip(("plus", create, True), ("copy", "Copy"))}</div>'
    return panel(fld(typed, chip_html=chip("event", chipname)) + rows(r + row(sym("search"), web, right=kind(kindweb))) + foot(("tab", actions), (esccap, close)))


D["s10event"] = desk(
    dtag("English, as today"), event_panel("Event", "Fri 3pm Dentist", "New event. Opens in your calendar to save.", ("When", "Fri 2 Oct, 3–4 PM"), ("Title", "Dentist"), "Create", "Search Google for “event Fri 3pm Dentist”", "Web", "Actions", "Close"),
    dtag("German, as today"), event_panel("Termin", "Fr 15 Uhr Zahnarzt", "Neuer Termin. Öffnet sich im Kalender zum Speichern.", ("Wann", "Fr., 2. Okt., 15–16 Uhr"), ("Titel", "Zahnarzt"), "Erstellen", "Mit Google nach „termin Fr 15 Uhr Zahnarzt“ suchen", "Web", "Aktionen", "Schließen"),
    dtag("French, draft"), event_panel("Événement", "ven 15h dentiste", "Nouvel événement. S’ouvre dans votre agenda pour l’enregistrer.", ("Quand", "ven. 2 oct., 15 h–16 h"), ("Titre", "dentiste"), "Créer", "Rechercher « événement ven 15h dentiste » sur Google", "Web", "Actions", "Fermer", "échap"),
    dtag("Danish, draft"), event_panel("Begivenhed", "fre kl. 15 tandlæge", "Ny begivenhed. Åbner i din kalender, hvor du gemmer den.", ("Hvornår", "fre. 2. okt., kl. 15–16"), ("Titel", "tandlæge"), "Opret", "Søg på Google efter „begivenhed fre kl. 15 tandlæge“", "Web", "Handlinger", "Luk"))


def approw(name_left):
    return row(app("C", "#DB4437"), "Chrome", right=strip(("open", "Open"), ("window", "New window"), ("info", "App info"), ("full", "Maximise"), ("left", name_left, True), ("right", "Right half"), ("more", "More")), on=True)


def cutp(inner):
    return panel(inner, cls="cut")


def tip(example, name, rest, yes, no, ph, esccap="esc", icon="left"):
    return panel(fld(ph=ph, esc_cap=esccap) + f'<div class="card">{sym(icon)}<span class="t"><b>{name}</b><i><em>{example}</em> {rest}</i></span><span class="fk">tab</span><span class="opts"><span>{yes}</span><span>{no}</span></span></div>')


def linkrow(name):
    return row(sym("globe"), "example.com/table", right=strip(("open", "Open"), ("clean", name, True)), on=True)


minimenu = lambda *names: '<div class="sysmenu">' + "".join(f'<div class="ours">{n}</div>' for n in names) + "</div>"
D["s11a"] = desk(dtag("English · “Left half”, 9 characters"), cutp(rows(approw("Left half"))), dtag("German, as today · „Linke Hälfte“, 12: at the limit"), cutp(rows(approw("Linke Hälfte"))))
D["s11b"] = desk(dtag("English · “Copy clean”, 10 characters"), cutp(rows(linkrow("Copy clean"))), dtag("French, draft · « Copier sans suivi », 17: one over the limit of 16. The test fails the build until it is written shorter", True), cutp(rows(linkrow("Copier sans suivi"))))
D["s11c"] = desk(dtag("English, as today · 85 characters"), tip("chrome left", "An app, in a place", "opens Chrome in the left half if it is not open yet. Also: top left, full", "Try it", "Turn off tips", "chrome left"),
                 dtag("French, draft · 85 characters. « Désactiver les astuces » is 22: over the limit of 18", True), tip("chrome gauche", "Une appli, à sa place", "ouvre Chrome à gauche s’il n’est pas encore ouvert. Aussi : plein écran", "Essayer", "Désactiver les astuces", "chrome gauche", "échap"))
D["s11d"] = desk(dtag("English · the longest ready-made name, 12 characters"), panel(fld(chip_html=chip("spark", "Fix spelling"), ph="Text, or nothing for what you copied", esc_cap="esc")),
                 dtag("German · „Zusammenfassung“, 15"), panel(fld(chip_html=chip("spark", "Zusammenfassung"), ph="Text, oder nichts für das Kopierte", esc_cap="esc")),
                 dtag("At the limit: a name of 24 characters (a made-up one)"), panel(fld(chip_html=chip("spark", "Zusammenfassung in Kürze"), ph="Text, oder nichts für das Kopierte", esc_cap="esc")))
def counted(lang, label, age, what):
    return dtag(f"{label} · {len(age + ' · ' + what)} characters") + line_panel(lang, age, what)


D["s11e"] = desk(counted("en", "English", "Copied 20 s ago", "a link, a date and more"), counted("fr", "French, draft", "Copié il y a 20 s", "un lien, une date et plus"),
                 counted("fr", "French, draft: the two longest things", "Copié il y a 1 min", "un numéro de téléphone et une adresse e-mail"))
D["s11f"] = desk(dtag("English · the first part is 23 characters"), cutp(rows(ans("From the picture’s text · on this device", "The design review is at 14:00.", COPY2))),
                 dtag("French, draft · the first part is 27 characters; both parts fit"), cutp(rows(ans("D’après le texte de l’image · sur cet appareil", "La revue de design est à 14 h.", strip(("copy", "Copier", True), ("pin", "Épingler"))))))
D["s11g"] = desk(dtag("English · “Search settings”, 15 characters"), cutp(foot(("tab", "Search settings"), ("esc", "Close"))), dtag("French, draft · « Rechercher dans les réglages », 28: at the limit"), cutp(foot(("tab", "Rechercher dans les réglages"), ("échap", "Fermer"))))
D["s11h"] = desk('<div class="pinrow" style="align-items:flex-start"><div class="pinbox">' + minimenu("Fix spelling", "Translate", "Booklight") + "<span>English</span></div>"
                 '<div class="pinbox">' + minimenu("Corriger", "Traduire", "Booklight") + "<span>French, draft · « Corriger », not « Corriger l’orthographe »</span></div></div>", cls="plain")
def modelrows(names, ask, label):
    """The model's rows of a list: the first selected, the others with their label at the right."""
    return cutp(rows(row(sym("spark"), names[0], right=strip(ask), on=True) + "".join(row(sym("spark"), n, right=kind(label)) for n in names[1:])))


D["s11i"] = desk(dtag("English: the device’s model passed, so its rows are answered here"),
                 modelrows(("Fix spelling", "Shorter", "In German"), ("spark", "Ask", True), "On this device"),
                 dtag("Danish, draft: the model failed the test, so the same rows hand over. Ordinary rows, not errors"),
                 modelrows(("Ret stavning", "Kortere", "På engelsk"), ("send", "Spørg Gemini", True), "Gemini"))

# ---------------------------------------------------------------- M5: flights
# The page's world: Thursday 1 October 2026, the user in San Francisco. LH 455 is San Francisco to Frankfurt (docs/research/flights.md);
# every time, terminal, gate and delay drawn here is made up. Planned: leaves 14:40, lands Fri 10:30.
LH = "LH 455 · Lufthansa"
FL_WHO = LH + " · San Francisco → Frankfurt"
FL_STRIP = strip(("open", "Open", True), ("copy", "Copy"), ("pin", "Pin"), ("more", "More"))
FL_STRIP_OFF = strip(("open", "Open", True), ("search", "Search"), ("send", "Ask Gemini"), ("settings", "Get times"))
SRC = "AeroDataBox · 13:58"   # the footer's left slot while an answer is shown: who said it, and when


def flight(who, slots, note, right=None, on=False, off=False, cls="", wide=False):
    """A flight's row: the app's slots body at the Event row's 92 dp. Line one: the number, who flies it and where. Line two: the two
    times as named values; the second starts at a fixed x, so nothing moves when the answer is written in. Line three: the status in
    full ink, then where to go. No colour: a delay is said, not painted. off: the times will not happen (struck, as a done task is)."""
    sl = "".join(f'<span><em>{k}</em><span class="v">{v}</span></span>' for k, v in slots)
    st, rest = note
    n = f'<div class="cap"><span class="st">{st}</span>{" · " + rest if rest else ""}</div>'
    return (f'<div class="r tall fl{" on" if on else ""}{" " + cls if cls else ""}">{sym("plane")}<div class="t"><div class="cap">{who}</div>'
            f'<div class="sl{" off" if off else ""}{" wide" if wide else ""}">{sl}</div>{n}</div>{right if right is not None else kind("Flight")}</div>')


def opt(icon, text, on=False):
    """A line of the list under a row (design-system.md §11, Rows.kt): 40 dp, its glyph on the icon column, the name on the title's
    edge in full ink, and the Enter mark where the row's arrow stands, on the line the pill is on."""
    return f'<div class="r opt{" on" if on else ""}"><span class="ic">{svg(icon)}</span><span class="t"><b>{text}</b></span><span class="em">{ENTER if on else ""}</span></div>'


def srcfoot(*pairs):
    """The footer under an answer: where it came from and when, at the left."""
    return f'<div class="foot"><span class="done src">{SRC}</span>' + "".join(f"{cap(k)}{v}" for k, v in pairs) + "</div>"


TURNED = '<span class="strip"><span class="a turned">' + svg("more") + "</span></span>"   # its list is open: the row keeps only its arrow, turned over
WEB = lambda q, lab="Web": row(sym("search"), f"Search Google for “{q}”", right=kind(lab))
EMPTY = [("Leaves", "–"), ("Lands", "–")]
DELAYED = lambda right=None, on=False: flight(FL_WHO, [("Leaves", "SFO 15:05"), ("Lands", "FRA Fri 10:55")], ("Delayed 25 min", "Terminal G, gate G4"), right, on)
LOOKING = flight(LH, EMPTY, ("Looking it up", ""), FL_STRIP, on=True)

D["f1type"] = desk(dtag("With your key: the row answers a moment after the last letter"), panel(fld("LH455") + rows(DELAYED(FL_STRIP, True) + WEB("LH455")) + srcfoot(("tab", "Actions"), ("esc", "Close"))),
                   dtag("The moment before: the row has its height, its lines and its actions from its first frame"), panel(fld("LH455") + rows(LOOKING + WEB("LH455")) + foot(("tab", "Actions"), ("esc", "Close"))))
D["f1dark"] = desk(panel(fld("lh 455") + rows(DELAYED(FL_STRIP, True) + WEB("lh 455")) + srcfoot(("tab", "Actions"), ("esc", "Close"))), theme="dark")
D["f1de"] = desk(dtag("German"), panel(fld("LH455") + rows(flight(FL_WHO, [("Ab", "SFO 15:05"), ("An", "FRA Fr. 10:55")], ("25 Min. verspätet", "Terminal G, Gate G4"),
                                                               strip(("open", "Öffnen", True), ("copy", "Kopieren"), ("pin", "Anheften"), ("more", "Mehr")), on=True)
                                                        + row(sym("search"), "Mit Google nach „LH455“ suchen", right=kind("Web"))) + srcfoot(("tab", "Aktionen"), ("esc", "Schließen"))),
                 dtag("The widest this row gets: a twelve-hour clock, a day at both ends, long names"),
                 panel(fld("AR 1303") + rows(flight("AR 1303 · Aerolíneas Argentinas · Buenos Aires → Mexico City", [("Leaves", "EZE Thu 11:55 PM"), ("Lands", "MEX Fri 6:40 AM")], ("Delayed 1 h 15 min", "Terminal A, gate 12"), FL_STRIP, on=True, wide=True)
                                             + WEB("AR 1303")) + srcfoot(("tab", "Actions"), ("esc", "Close"))))

T1 = "Terminal G, gate G4"
FL_STATES = [
    flight(FL_WHO, [("Leaves", "SFO 14:40"), ("Lands", "FRA Fri 10:30")], ("Planned", "Terminal G"), FL_STRIP, on=True),
    flight(FL_WHO, [("Leaves", "SFO 14:40"), ("Lands", "FRA Fri 10:30")], ("On time", T1)),
    DELAYED(),
    flight(FL_WHO, [("Leaves", "SFO 15:05"), ("Lands", "FRA Fri 10:55")], ("Boarding", T1)),
    flight(FL_WHO, [("Left", "SFO 15:12"), ("Lands", "FRA Fri 10:48")], ("In the air", "18 min late · Terminal 1")),
    flight(FL_WHO, [("Left", "SFO 15:12"), ("Landed", "FRA Fri 10:42")], ("Landed 12 min late", "Terminal 1, belt 12")),
    flight(FL_WHO, [("Left", "SFO 15:12"), ("Lands", "FRA Fri 10:48")], ("Diverted", ""), off=True),
    flight(FL_WHO, [("Leaves", "SFO 14:40"), ("Lands", "FRA Fri 10:30")], ("Cancelled", ""), off=True),
]
D["f2sheet"] = desk(guided(panel(rows("".join(FL_STATES)), cls="cut"), [38, 72, 700]))

more = opt("event", "Add to calendar", on=True) + opt("search", "Search the web") + opt("send", "Ask Gemini")
D["f3more"] = desk(panel(fld("LH455") + rows(DELAYED(TURNED) + more) + srcfoot(("tab", "Actions"), ("esc", "Close"))))
D["f3day"] = desk(dtag("A day after the number picks that day’s flight"),
                  panel(fld("LH455 fri") + rows(flight(FL_WHO, [("Leaves", "SFO Fri 14:40"), ("Lands", "FRA Sat 10:30")], ("Planned", "Terminal G"), FL_STRIP, on=True, wide=True) + WEB("LH455 fri"))
                        + srcfoot(("tab", "Actions"), ("esc", "Close"))))
pinf = lambda capt, fig, label: pintext(280, 118, f'<div class="pcap">{capt}</div><div class="fig">{fig}</div>', label)
D["f3pin"] = desk('<div class="pinrow">'
                  + pinf("LH 455 · gate G4 · 15:05", "1 h 07 min", "Before it leaves: the time to go")
                  + pinf("LH 455 · lands Fri 10:48", "6 h 20 min", "In the air: the time to landing")
                  + pinf("LH 455 · 10:42 · Terminal 1", "Landed", "Landed: it stays until you take it down")
                  + pinf("LH 455 · was 14:40", "Cancelled", "Cancelled")
                  + pinf("tea · set for 14:12", "7:42", "2.0’s timer, for scale: minutes and seconds")
                  + "</div>", cls="plain")

D["f4nokey"] = desk(dtag("No key: who flies it, and Enter opens its page"),
                    panel(fld("LH455") + rows(row(sym("plane"), LH, right=FL_STRIP_OFF, on=True) + WEB("LH455")) + foot(("tab", "Actions"), ("esc", "Close"))),
                    dtag("No key, routes switched on"),
                    panel(fld("LH455") + rows(row(sym("plane"), LH, "Usually San Francisco → Frankfurt", right=FL_STRIP_OFF, on=True) + WEB("LH455")) + foot(("tab", "Actions"), ("esc", "Close"))),
                    dtag("The last action: where a key is set up"),
                    panel(fld("LH455") + rows(row(sym("plane"), LH, right=strip(("open", "Open"), ("search", "Search"), ("send", "Ask Gemini"), ("settings", "Get times", True)), on=True) + WEB("LH455"))
                          + foot(("tab", "Actions"), ("esc", "Close"))))
D["f4weak"] = desk(dtag("Text that only looks like a flight: the flight is the last row, and nothing is sent"),
                   panel(fld("ps5") + rows(row(sym("search"), "Search Google for “ps5”", right=strip(("search", "Search", True)), on=True)
                                           + row(sym("plane"), "PS 5 · Ukraine International", right=kind("Flight"))) + foot(("esc", "Close"))),
                   dtag("The keyword makes it a flight"),
                   panel(fld("u2 8001", chip_html=chip("plane", "Flight")) + rows(flight("U2 8001 · easyJet · Milan → London", [("Leaves", "MXP 18:20"), ("Lands", "LGW 19:25")], ("On time", "Terminal 2"), FL_STRIP, on=True)
                                                                                  + WEB("flight u2 8001"))
                         + srcfoot(("tab", "Actions"), ("esc", "Close"))))
FL_OPEN = strip(("open", "Open", True), ("copy", "Copy", "off"), ("pin", "Pin", "off"), ("more", "More"))
D["f4none"] = desk(dtag("Nothing found"),
                   cutp(rows(flight("LH 9999 · Lufthansa", EMPTY, ("Nothing found for this number", ""), FL_OPEN, on=True))),
                   dtag("No answer"),
                   cutp(rows(flight(LH, EMPTY, ("No answer this time", ""), FL_OPEN, on=True))),
                   dtag("The key has no lookups left"),
                   cutp(rows(flight(LH, EMPTY, ("The key’s lookups are used up", ""), FL_OPEN, on=True))),
                   dtag("The key is not accepted"),
                   cutp(rows(flight(LH, EMPTY, ("The key was not accepted", ""), strip(("open", "Open", True), ("settings", "Open settings"), ("more", "More")), on=True))))
D["f4copy"] = desk(dtag("M1’s line names it"), line_panel("en", "Copied just now", "a flight and a date"),
                   dtag("After Tab the flight is a row like the others, and answers when it is selected"),
                   panel(fld(chip_html=chip("clip", "Landing with LH 454 at…"), ph="What to do with it")
                         + rows(flight("LH 454 · Lufthansa · Frankfurt → San Francisco", [("Left", "FRA 10:22"), ("Lands", "SFO 12:38")], ("In the air", "on time · Terminal I"), FL_STRIP, on=True)
                                + row(sym("event"), "Thu 1 Oct, 12:45–13:45", "Landing with LH 454", right=kind("Event"))
                                + row(sym("spark"), "Fix spelling", right=kind("On this device")))
                         + srcfoot(("tab", "Actions"), ("esc", "Close"))))
D["f4tip"] = desk(tip("LH455", "A flight number", "names the airline; Enter opens the flight’s page. Times need a key", "Try it", "Turn off tips", "Search apps, settings and the web", icon="plane"))

# ---------------------------------------------------------------- the page
exec(open(os.path.join(HERE, "text.py"), encoding="utf-8").read())   # defines PAGE, with [[name]] where a drawing goes

CSS = open(os.path.join(HERE, "page.css"), encoding="utf-8").read().replace("/*APP-LIGHT*/", APP_LIGHT).replace("/*APP-DARK*/", APP_DARK)
JS = open(os.path.join(HERE, "page.js"), encoding="utf-8").read()
body = PAGE
for name, drawing in D.items():
    assert "[[" + name + "]]" in body, "not placed: " + name
    body = body.replace("[[" + name + "]]", drawing.replace('<div class="', '<div id="d-' + name + '" class="', 1))
assert "[[" not in body, "no drawing for: " + body[body.index("[["):body.index("[[") + 30]

out = ('<title>Booklight Milestones</title>\n'
       '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Google+Sans+Flex:opsz,wght,ROND@6..144,300..800,0..100&family=Google+Sans+Code:wght@400..600&display=swap">\n'
       "<style>\n" + CSS + "\n</style>\n\n" + body + "\n<script>\n" + JS + "\n</script>\n")
path = os.environ.get("BL_PAGE_OUT") or os.path.join(HERE, "..", "..", "docs", "design", "booklight-milestones.html")
open(path, "w", encoding="utf-8").write(out)
print(path, len(out))
