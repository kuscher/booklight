#!/usr/bin/env python3
# SPDX-License-Identifier: MIT
"""Writes app/src/main/assets/emoji.tsv, the table behind the emoji and symbol picker.

    glyph <tab> kind <tab> English name <tab> English words <tab> German name <tab> German words

kind is e (emoji) or s (text symbol); words are search keywords separated by "|". The emoji, their
order, names and words come from Unicode (emoji-test.txt and the CLDR annotations); the symbols are
the list in this file. Run it again after changing a version or the list. `--check` only checks the
file that is there and needs no network.
"""
import argparse
import collections
import pathlib
import re
import sys
import tempfile
import urllib.request
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/emoji.tsv"

# Fixed versions, so that this script always writes the same file. To update: change them, run again.
EMOJI_TEST = "https://unicode.org/Public/18.0.0/emoji/emoji-test.txt"
CLDR_TAG = "release-48-2"
CLDR = f"https://raw.githubusercontent.com/unicode-org/cldr/{CLDR_TAG}/common/"

NEWEST = (15, 1)    # the Googlebooks' emoji font may not have anything newer
MAX_WORDS = 8
# Skin tones and hair styles: the same emoji again, five and four times over.
LOOKS = set(range(0x1F3FB, 0x1F400)) | set(range(0x1F9B0, 0x1F9B4))
ROW = re.compile(r"([0-9A-F ]+?) *; fully-qualified +# \S+ E(\d+)\.(\d+) (.+)")
VS16 = chr(0xFE0F)    # the selector that asks for the emoji form of a character

# Text symbols that are not emoji: glyph, English name, English words, German name, German words.
# Kept by hand. The bare character, never with the selector: these are for text, and some (©, ↔, ♥)
# have an emoji twin among the emoji that differs only by it. Characters nobody can see are written
# as chr(...), so that the source shows which one is meant.
SYMBOLS = [
    # Arrows
    ("←", "left arrow", "arrow|back|previous", "Pfeil nach links", "links|zurück"),
    ("↑", "up arrow", "arrow|top", "Pfeil nach oben", "oben|hoch|aufwärts"),
    ("→", "right arrow", "arrow|forward|next", "Pfeil nach rechts", "rechts|weiter|vor"),
    ("↓", "down arrow", "arrow|bottom", "Pfeil nach unten", "unten|runter|abwärts"),
    ("↔", "left-right arrow", "arrow|horizontal|both ways", "Pfeil nach links und rechts", "waagerecht|horizontal|beidseitig"),
    ("↕", "up-down arrow", "arrow|vertical", "Pfeil nach oben und unten", "senkrecht|vertikal"),
    ("↖", "up-left arrow", "arrow|diagonal|north-west", "Pfeil nach links oben", "diagonal|schräg|Nordwest"),
    ("↗", "up-right arrow", "arrow|diagonal|north-east", "Pfeil nach rechts oben", "diagonal|schräg|Nordost"),
    ("↘", "down-right arrow", "arrow|diagonal|south-east", "Pfeil nach rechts unten", "diagonal|schräg|Südost"),
    ("↙", "down-left arrow", "arrow|diagonal|south-west", "Pfeil nach links unten", "diagonal|schräg|Südwest"),
    ("⇐", "double left arrow", "arrow|implied by", "Doppelpfeil nach links", "Pfeil|folgt aus"),
    ("⇒", "double right arrow", "arrow|implies|follows", "Doppelpfeil nach rechts", "Pfeil|daraus folgt|Implikation|Folgepfeil"),
    ("⇔", "double left-right arrow", "arrow|equivalent|if and only if|iff", "Doppelpfeil nach links und rechts", "Pfeil|äquivalent|genau dann wenn"),
    ("↩", "right arrow curving left", "arrow|return|back|reply|undo", "geschwungener Pfeil nach links", "Pfeil|zurück|antworten|rückgängig"),
    ("↪", "left arrow curving right", "arrow|forward|redo", "geschwungener Pfeil nach rechts", "Pfeil|weiterleiten|wiederholen"),
    ("⤴", "right arrow curving up", "arrow|up", "geschwungener Pfeil nach oben", "Pfeil|oben"),
    ("⤵", "right arrow curving down", "arrow|down", "geschwungener Pfeil nach unten", "Pfeil|unten"),
    ("↺", "counterclockwise arrow", "arrow|anticlockwise|undo|rotate", "Kreispfeil gegen den Uhrzeigersinn", "Pfeil|drehen|rückgängig|links herum"),
    ("↻", "clockwise arrow", "arrow|reload|refresh|redo|rotate", "Kreispfeil im Uhrzeigersinn", "Pfeil|neu laden|aktualisieren|drehen|rechts herum"),
    ("⇄", "right arrow over left arrow", "arrow|swap|exchange|switch", "Pfeile nach rechts und links", "Pfeil|tauschen|wechseln|hin und her"),
    ("➔", "heavy right arrow", "arrow|bold", "fetter Pfeil nach rechts", "Pfeil|fett|rechts"),
    ("➜", "round-tipped right arrow", "arrow|bold|heavy", "fetter Pfeil nach rechts mit runder Spitze", "Pfeil|fett|rechts"),
    ("➤", "right arrowhead", "arrow|pointer|bullet", "Pfeilspitze nach rechts", "Pfeil|Aufzählungszeichen|Zeiger"),
    # Maths
    ("±", "plus-minus", "plus or minus|tolerance", "Plusminuszeichen", "plus oder minus|Toleranz"),
    ("×", "multiplication sign", "times|multiply|by", "Malzeichen", "mal|multiplizieren|Malkreuz"),
    ("÷", "division sign", "divide|divided by|obelus", "Geteiltzeichen", "geteilt durch|dividieren|Division"),
    ("−", "minus sign", "subtract|negative", "Minuszeichen", "minus|subtrahieren|negativ"),
    ("⋅", "dot operator", "times|multiply|dot product", "Malpunkt", "mal|multiplizieren|Skalarprodukt"),
    ("≠", "not equal", "unequal|different|inequality", "ungleich", "nicht gleich|Ungleichheit|verschieden"),
    ("≈", "almost equal", "approximately|about|roughly|circa", "ungefähr gleich", "circa|etwa|rund|gerundet"),
    ("≡", "identical to", "equivalent|congruent|triple bar", "identisch", "äquivalent|kongruent"),
    ("≙", "corresponds to", "estimates|equals", "entspricht", "Entsprechung"),
    ("∝", "proportional to", "proportional|varies as", "proportional zu", "Proportionalität"),
    ("≤", "less than or equal", "at most|not greater", "kleiner oder gleich", "kleiner gleich|höchstens"),
    ("≥", "greater than or equal", "at least|not less", "größer oder gleich", "größer gleich|mindestens"),
    ("∞", "infinity", "infinite|endless|forever", "unendlich", "Unendlichkeit|endlos|liegende Acht"),
    ("√", "square root", "root|radical", "Quadratwurzel", "Wurzel|Wurzelzeichen"),
    ("∑", "summation", "sum|sigma|total", "Summenzeichen", "Summe|Sigma"),
    ("∏", "product", "pi|multiply", "Produktzeichen", "Produkt|Pi"),
    ("∫", "integral", "calculus|area", "Integral", "Integralzeichen|Analysis"),
    ("∂", "partial derivative", "partial|del|differential", "partielle Ableitung", "Del|Differential"),
    ("∆", "increment", "delta|difference|change", "Differenz", "Delta|Änderung|Inkrement"),
    ("∇", "nabla", "del|gradient", "Nabla", "Gradient|Nabla-Operator"),
    ("∈", "element of", "in|member|belongs to|set", "Element von", "enthalten in|Menge"),
    ("∉", "not an element of", "not in|set", "kein Element von", "nicht enthalten in|Menge"),
    ("∩", "intersection", "set|cap|and", "Schnittmenge", "Durchschnitt|Menge|geschnitten"),
    ("∪", "union", "set|cup|or", "Vereinigungsmenge", "Vereinigung|Menge|vereinigt"),
    ("⊂", "subset of", "set|contained in", "Teilmenge von", "Untermenge|Menge"),
    ("⊃", "superset of", "set|contains", "Obermenge von", "Menge|enthält"),
    ("⊆", "subset of or equal to", "set", "Teilmenge oder gleich", "Untermenge|Menge"),
    ("⊇", "superset of or equal to", "set", "Obermenge oder gleich", "Menge"),
    ("∅", "empty set", "null set|nothing|void", "leere Menge", "Nullmenge|nichts"),
    ("∀", "for all", "every|any|universal quantifier", "für alle", "Allquantor|für jedes"),
    ("∃", "there exists", "exists|existential quantifier", "es existiert", "Existenzquantor|es gibt"),
    ("¬", "not sign", "negation|logical not", "Negation", "nicht|logisches Nicht|Negationszeichen"),
    ("∧", "logical and", "and|conjunction|wedge", "logisches Und", "und|Konjunktion"),
    ("∨", "logical or", "or|disjunction|vee", "logisches Oder", "oder|Disjunktion"),
    ("⊕", "circled plus", "xor|exclusive or|direct sum", "Plus im Kreis", "XOR|exklusives Oder|direkte Summe"),
    ("∴", "therefore", "hence|so|thus", "folglich", "daher|deshalb|also"),
    ("∠", "angle", "geometry", "Winkel", "Geometrie|Winkelzeichen"),
    ("⊥", "perpendicular", "orthogonal|right angle|up tack", "senkrecht", "orthogonal|rechtwinklig|Lot"),
    ("∥", "parallel to", "parallel", "parallel", "parallel zu"),
    ("°", "degree", "degrees|temperature|angle", "Grad", "Gradzeichen|Temperatur|Winkel"),
    ("′", "prime", "minutes|feet|arcminute", "Minutenzeichen", "Prime|Strich|Fuß|Bogenminute"),
    ("″", "double prime", "seconds|inches|arcsecond", "Sekundenzeichen", "Doppelprime|Zoll|Bogensekunde"),
    ("‰", "per mille", "per thousand|permille", "Promille", "Tausendstel|pro tausend"),
    ("µ", "micro sign", "micro|mu|millionth", "Mikro", "Mikrozeichen|My|Millionstel"),
    ("⌀", "diameter", "average|diameter sign", "Durchmesser", "Durchmesserzeichen|Durchschnitt"),
    ("ℕ", "natural numbers", "set|double-struck N", "natürliche Zahlen", "Menge|Zahlenmenge"),
    ("ℤ", "integers", "set|whole numbers|double-struck Z", "ganze Zahlen", "Menge|Zahlenmenge"),
    ("ℚ", "rational numbers", "set|fractions|double-struck Q", "rationale Zahlen", "Menge|Zahlenmenge|Brüche"),
    ("ℝ", "real numbers", "set|reals|double-struck R", "reelle Zahlen", "Menge|Zahlenmenge"),
    ("ℂ", "complex numbers", "set|double-struck C", "komplexe Zahlen", "Menge|Zahlenmenge"),
    # Currency
    ("€", "euro", "EUR|currency|money", "Euro", "EUR|Währung|Geld"),
    ("£", "pound", "GBP|sterling|currency", "Pfund", "GBP|Pfund Sterling|Währung"),
    ("¥", "yen", "JPY|yuan|CNY|currency", "Yen", "JPY|Yuan|CNY|Währung"),
    ("¢", "cent", "currency|money", "Cent", "Währung|Geld"),
    ("₹", "Indian rupee", "INR|currency", "indische Rupie", "INR|Währung"),
    ("₽", "ruble", "RUB|rouble|currency", "Rubel", "RUB|Währung"),
    ("₩", "won", "KRW|Korea|currency", "Won", "KRW|Korea|Währung"),
    ("₿", "bitcoin", "BTC|crypto|currency", "Bitcoin", "BTC|Kryptowährung"),
    ("₺", "Turkish lira", "TRY|currency", "türkische Lira", "TRY|Währung"),
    ("₴", "hryvnia", "UAH|Ukraine|currency", "Hrywnja", "UAH|Ukraine|Währung"),
    ("₪", "shekel", "ILS|sheqel|Israel|currency", "Schekel", "ILS|Israel|Währung"),
    ("₫", "dong", "VND|Vietnam|currency", "Dong", "VND|Vietnam|Währung"),
    ("₱", "peso", "PHP|Philippines|currency", "Peso", "PHP|Philippinen|Währung"),
    ("฿", "baht", "THB|Thailand|currency", "Baht", "THB|Thailand|Währung"),
    ("¤", "currency sign", "generic|money", "Währungszeichen", "allgemein|Geld"),
    # Typography
    ("—", "em dash", "dash|long dash", "Geviertstrich", "langer Strich|Gedankenstrich"),
    ("–", "en dash", "dash|range|to", "Halbgeviertstrich", "Gedankenstrich|Bis-Strich|Streckenstrich"),
    (chr(0x2011), "non-breaking hyphen", "hyphen|no-break", "geschützter Bindestrich", "Bindestrich|nicht umbrechend"),
    ("…", "ellipsis", "dots|three dots|omission", "Auslassungspunkte", "drei Punkte|Ellipse"),
    ("•", "bullet", "dot|list|point", "Aufzählungspunkt", "Punkt|Liste|Aufzählungszeichen"),
    ("·", "middle dot", "interpunct|centered dot|dot", "Mittelpunkt", "Punkt|Hochpunkt|halbhoher Punkt"),
    ("‹", "single left angle quote", "guillemet|chevron|quotation mark", "einfaches Guillemet links", "Anführungszeichen|Chevron|Spitzzeichen"),
    ("›", "single right angle quote", "guillemet|chevron|quotation mark", "einfaches Guillemet rechts", "Anführungszeichen|Chevron|Spitzzeichen"),
    ("«", "left angle quote", "guillemet|chevron|quotation mark|French", "Guillemet links", "Anführungszeichen|Chevron|Spitzzeichen|französisch"),
    ("»", "right angle quote", "guillemet|chevron|quotation mark|French", "Guillemet rechts", "Anführungszeichen|Chevron|Spitzzeichen|französisch"),
    ("“", "left double quote", "quotation mark|opening|curly|66", "Anführungszeichen oben", "Gänsefüßchen|schließend|deutsch"),
    ("”", "right double quote", "quotation mark|closing|curly|99", "englisches Anführungszeichen rechts", "Gänsefüßchen|schließend|englisch"),
    ("‘", "left single quote", "quotation mark|opening|curly", "einfaches Anführungszeichen oben", "halbes Anführungszeichen|schließend|deutsch"),
    ("’", "right single quote", "apostrophe|closing|curly", "Apostroph", "Hochkomma|Auslassungszeichen|einfaches Anführungszeichen rechts"),
    ("„", "low double quote", "quotation mark|opening|German", "Anführungszeichen unten", "Gänsefüßchen|öffnend|deutsch"),
    ("‚", "low single quote", "quotation mark|opening|German", "einfaches Anführungszeichen unten", "halbes Anführungszeichen|öffnend|deutsch"),
    ("§", "section sign", "section|paragraph|law|legal", "Paragrafzeichen", "Paragraf|Paragraph|Gesetz"),
    ("¶", "pilcrow", "paragraph mark|paragraph", "Absatzzeichen", "Absatzmarke|Absatz|Alinea"),
    ("†", "dagger", "cross|footnote|died|obelisk", "typografisches Kreuz", "Kreuz|Fußnote|gestorben|Sterbekreuz"),
    ("‡", "double dagger", "cross|footnote", "typografisches Doppelkreuz", "Doppelkreuz|Fußnote"),
    ("©", "copyright", "c|rights reserved", "Copyright", "Urheberrecht|c"),
    ("®", "registered", "r|registered trademark", "eingetragene Marke", "registriert|Warenzeichen|r"),
    ("™", "trademark", "tm|trade mark", "Markenzeichen", "Trademark|Warenzeichen|tm"),
    ("℠", "service mark", "sm", "Dienstleistungsmarke", "Service Mark|sm"),
    ("№", "numero sign", "number|no.", "Nummernzeichen", "Nummer|Nr.|Numero"),
    ("‽", "interrobang", "question|exclamation", "Interrobang", "Fragerufzeichen|Frage|Ausruf"),
    ("¡", "inverted exclamation mark", "Spanish|upside down", "umgekehrtes Ausrufezeichen", "spanisch|kopfstehend"),
    ("¿", "inverted question mark", "Spanish|upside down", "umgekehrtes Fragezeichen", "spanisch|kopfstehend"),
    ("ẞ", "capital sharp s", "eszett|German|uppercase", "großes Eszett", "scharfes S|Versal-Eszett|Großbuchstabe"),
    # Fractions, superscripts, subscripts
    ("½", "one half", "half|fraction|1/2", "ein halb", "Hälfte|Bruch|1/2"),
    ("⅓", "one third", "third|fraction|1/3", "ein Drittel", "Bruch|1/3"),
    ("⅔", "two thirds", "fraction|2/3", "zwei Drittel", "Bruch|2/3"),
    ("¼", "one quarter", "quarter|fourth|fraction|1/4", "ein Viertel", "Bruch|1/4"),
    ("¾", "three quarters", "fraction|3/4", "drei Viertel", "Bruch|3/4"),
    ("⅕", "one fifth", "fifth|fraction|1/5", "ein Fünftel", "Bruch|1/5"),
    ("⅛", "one eighth", "eighth|fraction|1/8", "ein Achtel", "Bruch|1/8"),
    ("⁰", "superscript zero", "exponent|power|0", "hochgestellte Null", "Exponent|hoch 0"),
    ("¹", "superscript one", "exponent|power|1", "hochgestellte Eins", "Exponent|hoch 1"),
    ("²", "superscript two", "squared|exponent|power|2", "hochgestellte Zwei", "Quadrat|Exponent|hoch 2"),
    ("³", "superscript three", "cubed|exponent|power|3", "hochgestellte Drei", "Kubik|Exponent|hoch 3"),
    ("⁴", "superscript four", "exponent|power|4", "hochgestellte Vier", "Exponent|hoch 4"),
    ("⁵", "superscript five", "exponent|power|5", "hochgestellte Fünf", "Exponent|hoch 5"),
    ("⁶", "superscript six", "exponent|power|6", "hochgestellte Sechs", "Exponent|hoch 6"),
    ("⁷", "superscript seven", "exponent|power|7", "hochgestellte Sieben", "Exponent|hoch 7"),
    ("⁸", "superscript eight", "exponent|power|8", "hochgestellte Acht", "Exponent|hoch 8"),
    ("⁹", "superscript nine", "exponent|power|9", "hochgestellte Neun", "Exponent|hoch 9"),
    ("ⁿ", "superscript n", "exponent|power|nth", "hochgestelltes n", "Exponent|hoch n"),
    ("₀", "subscript zero", "index|0", "tiefgestellte Null", "Index|0"),
    ("₁", "subscript one", "index|1", "tiefgestellte Eins", "Index|1"),
    ("₂", "subscript two", "index|2|H2O|CO2", "tiefgestellte Zwei", "Index|2|H2O|CO2"),
    ("₃", "subscript three", "index|3", "tiefgestellte Drei", "Index|3"),
    ("₄", "subscript four", "index|4", "tiefgestellte Vier", "Index|4"),
    ("₅", "subscript five", "index|5", "tiefgestellte Fünf", "Index|5"),
    ("₆", "subscript six", "index|6", "tiefgestellte Sechs", "Index|6"),
    ("₇", "subscript seven", "index|7", "tiefgestellte Sieben", "Index|7"),
    ("₈", "subscript eight", "index|8", "tiefgestellte Acht", "Index|8"),
    ("₉", "subscript nine", "index|9", "tiefgestellte Neun", "Index|9"),
    # Greek letters
    ("α", "alpha", "Greek|letter|angle", "Alpha", "griechisch|Buchstabe|Winkel"),
    ("β", "beta", "Greek|letter", "Beta", "griechisch|Buchstabe"),
    ("γ", "gamma", "Greek|letter", "Gamma", "griechisch|Buchstabe"),
    ("δ", "delta", "Greek|letter", "Delta", "griechisch|Buchstabe"),
    ("ε", "epsilon", "Greek|letter", "Epsilon", "griechisch|Buchstabe"),
    ("ζ", "zeta", "Greek|letter", "Zeta", "griechisch|Buchstabe"),
    ("η", "eta", "Greek|letter|efficiency", "Eta", "griechisch|Buchstabe|Wirkungsgrad"),
    ("θ", "theta", "Greek|letter|angle", "Theta", "griechisch|Buchstabe|Winkel"),
    ("ι", "iota", "Greek|letter", "Iota", "griechisch|Buchstabe|Jota"),
    ("κ", "kappa", "Greek|letter", "Kappa", "griechisch|Buchstabe"),
    ("λ", "lambda", "Greek|letter|wavelength", "Lambda", "griechisch|Buchstabe|Wellenlänge"),
    ("μ", "mu", "Greek|letter|micro", "My", "griechisch|Buchstabe|Mü|Mikro"),
    ("ν", "nu", "Greek|letter|frequency", "Ny", "griechisch|Buchstabe|Nü|Frequenz"),
    ("ξ", "xi", "Greek|letter", "Xi", "griechisch|Buchstabe"),
    ("π", "pi", "Greek|letter|circle|3.14", "Pi", "griechisch|Buchstabe|Kreiszahl|3,14"),
    ("ρ", "rho", "Greek|letter|density", "Rho", "griechisch|Buchstabe|Dichte"),
    ("σ", "sigma", "Greek|letter|standard deviation", "Sigma", "griechisch|Buchstabe|Standardabweichung"),
    ("τ", "tau", "Greek|letter", "Tau", "griechisch|Buchstabe"),
    ("υ", "upsilon", "Greek|letter", "Ypsilon", "griechisch|Buchstabe"),
    ("φ", "phi", "Greek|letter|angle|golden ratio", "Phi", "griechisch|Buchstabe|Winkel|goldener Schnitt"),
    ("χ", "chi", "Greek|letter", "Chi", "griechisch|Buchstabe"),
    ("ψ", "psi", "Greek|letter", "Psi", "griechisch|Buchstabe"),
    ("ω", "omega", "Greek|letter|angular frequency", "Omega", "griechisch|Buchstabe|Kreisfrequenz"),
    ("Γ", "capital gamma", "Greek|letter|uppercase", "großes Gamma", "griechisch|Buchstabe|Großbuchstabe"),
    ("Δ", "capital delta", "Greek|letter|uppercase|difference|change", "großes Delta", "griechisch|Buchstabe|Großbuchstabe|Differenz|Änderung"),
    ("Θ", "capital theta", "Greek|letter|uppercase", "großes Theta", "griechisch|Buchstabe|Großbuchstabe"),
    ("Λ", "capital lambda", "Greek|letter|uppercase", "großes Lambda", "griechisch|Buchstabe|Großbuchstabe"),
    ("Π", "capital pi", "Greek|letter|uppercase|product", "großes Pi", "griechisch|Buchstabe|Großbuchstabe|Produkt"),
    ("Σ", "capital sigma", "Greek|letter|uppercase|sum", "großes Sigma", "griechisch|Buchstabe|Großbuchstabe|Summe"),
    ("Φ", "capital phi", "Greek|letter|uppercase", "großes Phi", "griechisch|Buchstabe|Großbuchstabe"),
    ("Ψ", "capital psi", "Greek|letter|uppercase", "großes Psi", "griechisch|Buchstabe|Großbuchstabe"),
    ("Ω", "capital omega", "Greek|letter|uppercase|ohm|resistance", "großes Omega", "griechisch|Buchstabe|Großbuchstabe|Ohm|Widerstand"),
    # Keys
    ("⌘", "command key", "cmd|Mac|shortcut", "Befehlstaste", "Cmd|Command|Mac|Tastenkürzel"),
    ("⌥", "option key", "alt|opt|Mac", "Wahltaste", "Option|Alt|Mac"),
    ("⇧", "shift key", "shift|uppercase|up arrow", "Umschalttaste", "Shift|Hochstelltaste|Großschreibung"),
    ("⌃", "control key", "ctrl|Mac|caret", "Steuerungstaste", "Strg|Ctrl|Control|Mac"),
    ("⇪", "caps lock", "caps|uppercase|key", "Feststelltaste", "Caps Lock|Umschaltsperre|Großschreibung"),
    ("⏎", "return key", "enter|return|newline", "Eingabetaste", "Enter|Return|Zeilenschaltung"),
    ("↵", "return arrow", "enter|newline|line break|arrow", "Zeilenumbruch", "Enter|Return|Eingabe|Pfeil"),
    ("⌫", "backspace key", "delete|erase|delete left", "Rücktaste", "Backspace|löschen|Rückschritt"),
    ("⌦", "forward delete key", "delete|del|erase|delete right", "Entfernen-Taste", "Entf|Delete|löschen"),
    ("⇥", "tab key", "tab|tabulator|indent", "Tabulatortaste", "Tab|Tabulator|einrücken"),
    ("⎋", "escape key", "esc|cancel", "Escape-Taste", "Esc|abbrechen"),
    ("␣", "visible space", "space|blank|space bar|open box", "sichtbares Leerzeichen", "Leerzeichen|Leertaste|Leerschritt"),
    # Marks
    ("✓", "check mark", "tick|yes|done|correct", "Häkchen", "Haken|erledigt|richtig|ja"),
    ("✔", "heavy check mark", "tick|bold|done|correct", "kräftiges Häkchen", "Haken|erledigt|richtig|fett"),
    ("✗", "ballot x", "cross|no|wrong|x", "Kreuzchen", "x|falsch|nein|durchgekreuzt"),
    ("✕", "multiplication x", "close|cancel|cross|x", "schräges Kreuz", "x|schließen|abbrechen|Kreuz"),
    ("☐", "ballot box", "checkbox|empty|unchecked|to-do", "Kästchen", "Checkbox|leer|Ankreuzfeld|To-do"),
    ("☑", "ballot box with check", "checkbox|checked|done|tick", "Kästchen mit Häkchen", "Checkbox|angekreuzt|erledigt"),
    ("☒", "ballot box with x", "checkbox|crossed|cross", "Kästchen mit Kreuz", "Checkbox|angekreuzt|durchgekreuzt"),
    ("★", "black star", "star|filled|favorite|rating", "gefüllter Stern", "Stern|schwarz|Favorit|Bewertung"),
    ("☆", "white star", "star|outline|empty|rating", "leerer Stern", "Stern|Umriss|weiß|Bewertung"),
    ("✦", "four-pointed star", "star|sparkle|AI", "vierzackiger Stern", "Stern|Funkeln|Glitzer|KI"),
    ("♠", "spade suit", "spades|cards|suit", "Pik", "Karten|Kartenfarbe|Spielkarte"),
    ("♥", "heart suit", "hearts|cards|suit|love", "Herz", "Karten|Kartenfarbe|Spielkarte|Liebe"),
    ("♦", "diamond suit", "diamonds|cards|suit", "Karo", "Karten|Kartenfarbe|Spielkarte"),
    ("♣", "club suit", "clubs|cards|suit", "Kreuz", "Karten|Kartenfarbe|Spielkarte|Treff"),
    ("♡", "white heart suit", "heart|outline|love", "leeres Herz", "Herz|Umriss|Liebe"),
    ("♪", "eighth note", "music|note|quaver", "Achtelnote", "Musik|Note"),
    ("♫", "beamed eighth notes", "music|notes|song", "verbundene Achtelnoten", "Musik|Noten|Lied"),
    ("♭", "flat", "music|note|accidental", "B-Vorzeichen", "Musik|Note|Erniedrigungszeichen"),
    ("♯", "sharp", "music|note|accidental", "Kreuzvorzeichen", "Musik|Note|Erhöhungszeichen|Kreuz"),
    # Shapes
    ("◆", "black diamond", "filled|rhombus|lozenge", "gefüllte Raute", "Raute|Rhombus|schwarz"),
    ("◇", "white diamond", "outline|rhombus|lozenge", "leere Raute", "Raute|Rhombus|Umriss"),
    ("●", "black circle", "filled|dot|round", "gefüllter Kreis", "Kreis|Punkt|schwarz"),
    ("○", "white circle", "outline|ring|round", "leerer Kreis", "Kreis|Ring|Umriss"),
    ("◉", "fisheye", "radio button|selected|circle", "Kreis mit Punkt", "Optionsfeld|Radiobutton|ausgewählt"),
    ("■", "black square", "filled|stop|box", "gefülltes Quadrat", "Quadrat|schwarz|Stopp"),
    ("□", "white square", "outline|empty|box", "leeres Quadrat", "Quadrat|Umriss|Kasten"),
    ("▲", "black up triangle", "triangle|filled|arrow|up", "gefülltes Dreieck nach oben", "Dreieck|Pfeil|oben"),
    ("▼", "black down triangle", "triangle|filled|arrow|down", "gefülltes Dreieck nach unten", "Dreieck|Pfeil|unten"),
    ("◀", "black left triangle", "triangle|arrow|back|previous", "gefülltes Dreieck nach links", "Dreieck|Pfeil|zurück"),
    ("▶", "black right triangle", "triangle|arrow|play|next", "gefülltes Dreieck nach rechts", "Dreieck|Pfeil|abspielen|weiter"),
    ("△", "white up triangle", "triangle|outline|up", "leeres Dreieck nach oben", "Dreieck|Umriss|oben"),
    ("▽", "white down triangle", "triangle|outline|down", "leeres Dreieck nach unten", "Dreieck|Umriss|unten"),
    ("❮", "heavy left angle bracket", "chevron|back|previous", "fette spitze Klammer links", "Chevron|Winkel|zurück"),
    ("❯", "heavy right angle bracket", "chevron|prompt|next", "fette spitze Klammer rechts", "Chevron|Winkel|Prompt|weiter"),
    ("☰", "three lines", "menu|hamburger|trigram", "drei Striche", "Menü|Hamburger-Menü|Trigramm"),
    ("⋮", "vertical ellipsis", "more|menu|three dots", "senkrechte Auslassungspunkte", "drei Punkte|Menü|mehr"),
    ("⋯", "midline ellipsis", "more|three dots|centered", "mittige Auslassungspunkte", "drei Punkte|mehr"),
    ("⌂", "house", "home|start", "Haus", "Home|Startseite|Zuhause"),
    # Spaces and marks nobody sees: here the glyph column is itself white space or empty to the eye.
    (chr(0x00A0), "no-break space", "nbsp|non-breaking|space|blank", "geschütztes Leerzeichen", "Leerzeichen|nicht umbrechend|nbsp"),
    (chr(0x202F), "narrow no-break space", "nnbsp|thin|non-breaking|space", "schmales geschütztes Leerzeichen", "Leerzeichen|nicht umbrechend|schmal"),
    (chr(0x2009), "thin space", "space|narrow|blank", "schmales Leerzeichen", "Leerzeichen|dünn|Spatium"),
    (chr(0x2003), "em space", "space|wide|blank", "Geviert-Leerzeichen", "Leerzeichen|Geviert|breit"),
    (chr(0x200B), "zero-width space", "zwsp|invisible|space|break", "breitenloses Leerzeichen", "Leerzeichen|unsichtbar|nullbreit|zwsp"),
    (chr(0x00AD), "soft hyphen", "shy|hyphen|optional|invisible", "weiches Trennzeichen", "bedingter Trennstrich|Trennstrich|Silbentrennung|unsichtbar"),
]


def fetch(url: str) -> bytes:
    """What is at url, kept in the temp folder so that a second run needs no network."""
    cache = pathlib.Path(tempfile.gettempdir()) / "booklight-emoji"
    # The whole address is the name: it holds the version, so a new version is fetched anew.
    file = cache / re.sub(r"[^A-Za-z0-9.]+", "-", url.split("://")[1])
    if not file.exists():
        cache.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(url, timeout=60) as reply:
            data = reply.read()
        part = file.with_name(file.name + ".part")    # a broken download must not pass for the file
        part.write_bytes(data)
        part.replace(file)
    return file.read_bytes()


def clean(text: str) -> str:
    """One line, single spaces: a tab or a line break inside a field would break the table."""
    return " ".join(text.split())


def emoji() -> tuple[str, list[tuple[str, str]]]:
    """The list's version and (glyph, name) for every emoji we keep, in the order of the file,
    which is the order of an emoji keyboard: smileys first."""
    text = fetch(EMOJI_TEST).decode("utf-8")
    version = re.search(r"^# Version: (\S+)", text, re.M).group(1)
    rows, group = [], ""
    for line in text.splitlines():
        if line.startswith("# group:"):
            group = line.split(":", 1)[1].strip()
        m = ROW.match(line)
        if not m or group == "Component" or (int(m[2]), int(m[3])) > NEWEST:
            continue
        points = [int(p, 16) for p in m[1].split()]
        if not LOOKS.intersection(points):
            rows.append(("".join(map(chr, points)), clean(m[4])))
    return version, rows


def annotations(lang: str) -> tuple[dict[str, str], dict[str, list[str]]]:
    """CLDR's names and words in one language, by glyph without the selector (CLDR leaves it out)."""
    names, words = {}, {}
    for folder in ("annotations", "annotationsDerived"):    # derived: flags, keycaps, joined emoji
        for a in ET.fromstring(fetch(f"{CLDR}{folder}/{lang}.xml")).iter("annotation"):
            glyph = a.get("cp").replace(VS16, "")
            if a.get("type") == "tts":
                names[glyph] = clean(a.text)
            else:
                words[glyph] = [w for w in map(clean, a.text.split("|")) if w]
    return names, words


def pick(name: str, words: list[str], shared: collections.Counter) -> list[str]:
    """The words to keep: never the name itself, MAX_WORDS at most."""
    seen, rest = {name.casefold()}, []
    for word in words:
        if word.casefold() not in seen:
            seen.add(word.casefold())
            rest.append(word)
    if len(rest) <= MAX_WORDS:
        return rest
    # CLDR lists words by alphabet, so cutting the tail would lose "smile" and "wow". The first to
    # go are words the name finds anyway ("face" for "grinning face"), then the ones the fewest
    # emoji share: what people type ("love", "laugh") sits on many emoji, the rare words are
    # mostly slang ("roflmao"). Of the orders tried on a list of everyday searches, in both
    # languages, this one lost the fewest.
    def plain(text: str) -> str:
        return " " + " ".join(re.findall(r"\w+", text.casefold()))

    keep = sorted(range(len(rest)), key=lambda i: (plain(rest[i]) in plain(name), -shared[rest[i].casefold()], i))
    return [rest[i] for i in sorted(keep[:MAX_WORDS])]


def build() -> str:
    version, rows = emoji()
    cldr = CLDR_TAG.removeprefix("release-").replace("-", ".")
    lines = [
        "# Emoji and text symbols for Booklight's picker. Six columns, separated by tabs:",
        '# glyph, kind (e = emoji, s = symbol), English name, English words, German name, German words; words separated by "|".',
        f"# Emoji and their order: Unicode emoji-test.txt {version}, up to Emoji {NEWEST[0]}.{NEWEST[1]}, without skin tones and hair styles.",
        f"# Their names and words: Unicode CLDR {cldr} annotations, English and German. © Unicode, Inc.; see NOTICE.",
        "# Symbols: the list in tools/emoji.py.",
        '# Only these first lines are comments: the row of the keycap # emoji begins with "#" too.',
        "# Generated by tools/emoji.py; do not edit.",
    ]
    english, german = annotations("en"), annotations("de")
    table = []    # glyph, kind, then per language: name, words
    for glyph, own_name in rows:
        key = glyph.replace(VS16, "")
        entry = [glyph, "e"]
        # English always has a name, the list's own if CLDR has none; German may have neither.
        for (names, words), fallback in ((english, own_name), (german, "")):
            name = names.get(key) or fallback
            entry += [name, words.get(key, []) if name else []]
        table.append(entry)
    for column in (3, 5):
        shared = collections.Counter(w.casefold() for entry in table for w in set(entry[column]))
        for entry in table:
            entry[column] = "|".join(pick(entry[column - 1], entry[column], shared))
    for glyph, en, en_words, de, de_words in SYMBOLS:
        table.append([glyph, "s", en, en_words, de, de_words])
    return "\n".join(lines + ["\t".join(entry) for entry in table]) + "\n"


def entries(text: str) -> list[tuple[int, list[str]]]:
    """(line number, columns) of every row below the comments at the top."""
    lines = text.split("\n")
    if lines[-1] == "":
        lines.pop()
    start = 0
    while start < len(lines) and lines[start].startswith("#") and "\t" not in lines[start]:
        start += 1
    return [(n, line.split("\t")) for n, line in enumerate(lines[start:], start + 1)]


def problems(text: str) -> list[str]:
    """What is wrong with a table, in words; nothing when it is fine."""
    found, seen = [], set()
    if not text.endswith("\n") or "\r" in text:
        found.append("lines must end with a plain line feed, the last one too")
    for n, cols in entries(text):
        if len(cols) != 6:
            found.append(f"line {n}: {len(cols)} columns instead of 6")
            continue
        glyph, kind, name = cols[:3]
        if not glyph:
            found.append(f"line {n}: no glyph")
        if glyph in seen:
            found.append(f"line {n}: {glyph} is in the table twice")
        seen.add(glyph)
        if kind not in ("e", "s"):
            found.append(f"line {n}: kind is {kind!r}, not e or s")
        if not name.strip():
            found.append(f"line {n}: {glyph} has no English name")
    if not seen:
        found.append("no entries")
    return found


def summary(text: str) -> str:
    rows = [cols for _, cols in entries(text)]
    kinds = collections.Counter(cols[1] for cols in rows)
    no_german = sum(1 for cols in rows if not cols[4])
    return (f"{OUT.relative_to(ROOT)}: {kinds['e']} emoji, {kinds['s']} symbols, "
            f"{no_german} without a German name, {len(text.encode('utf-8'))} bytes")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--check", action="store_true", help="check the file that is there; write nothing")
    args = parser.parse_args()
    if args.check:
        try:
            text = OUT.read_bytes().decode("utf-8")
        except (OSError, UnicodeDecodeError) as e:
            print(f"{OUT}: {e}", file=sys.stderr)
            return 1
    else:
        text = build()
    wrong = problems(text)
    for line in wrong:
        print(line, file=sys.stderr)
    if wrong:
        return 1    # and a table with problems is not written
    if not args.check:
        OUT.parent.mkdir(parents=True, exist_ok=True)
        OUT.write_bytes(text.encode("utf-8"))
    print(summary(text))
    return 0


if __name__ == "__main__":
    sys.exit(main())
