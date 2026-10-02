# Booklight: language support

*Research and plan, 1 October 2026. Read for it: the repo at commit `428f5b5` (all counts and line numbers are from
that commit), Google's ML Kit, Android and Play pages, AOSP, Unicode CLDR, the Google Fonts repository, and five
search engines' suggestion addresses (asked once each, with a harmless word). Nothing in the repo was changed, no
device was touched, nothing was built.*

*While this was written 2.1 was committed (`f52fbf3`): a decimal comma for the calculator, taken from
the locale (`providers/Providers.kt:28`, `core/…/Calc.kt`), and a new `abc` scope for the letters of other languages
(`core/…/Letters.kt`, with a `german: Boolean`). It is not in the counts below. It adds 5 strings (468), one guide
row (32), and moves a few line numbers in `strings_11.xml`, `strings_20.xml`, `Calc.kt`, `Providers.kt`, `Picks.kt`.
Where it changes the plan, the text says so.*

**How sure each claim is.** *counted* or *read* = from the repo. *tested* = run today on this Mac (JVM, curl).
*docs* = read on the vendor's page today. *not verified* = could not be checked without a device or a native reader.

---

## 1. The short answer

**Translating the screens is the easy fifth. The hard part is that Booklight is typed.** German was not done by
translating strings: it was done by adding German words to 50 keyword lists, to 15 word tables in the Kotlin code
(220 words), to two search tables and to the emoji table, by hand, and all of it still assumes spaces between
words, a decimal point and Latin letters.

- **French, Spanish, Italian, Danish, Swedish, Norwegian: easy**, about two days each, *after* roughly two weeks of
  one-time work that moves the typed vocabulary out of the code and makes numbers and dates follow the locale.
  **Polish: medium** (plural forms, inflected day and month names, one-letter words).
- **Japanese, Korean, Chinese: hard.** They need a second piece of one-time work of about the same size: matching
  that understands kana, Hangul and pinyin, a field that behaves while an input method is composing, and a date
  reader that does not need spaces. Korean is the mildest of the three, Japanese the most work.
- **The on-device model is the least certain part.** Google publishes no language list for the Prompt API that
  Booklight uses. French, Spanish, Italian, Japanese and Korean are on Google's lists for the neighbouring APIs;
  Danish, Swedish, Norwegian, Polish and Chinese are on none. That needs a probe on the Lenovo, per language.
- **The lasting cost is not the first translation.** Today's rule "every string in English and German" becomes
  "in twelve languages", for every new keyword, tip, release note and screenshot, checked by people who read them.

All ten (plus Traditional Chinese): about **50 working days**, of which about 19 are one-time engineering. The six
western European languages alone: about **four weeks** (21 days) including the first block of one-time work.

---

## 2. One row per language

Effort is working days for Alex with Claude, after the one-time work of §7, without the wait for a native reader.
"Model" is about the five prompts a new installation starts with (§5).

| Language | Difficulty | What makes it so | On-device model | Rough effort |
| --- | --- | --- | --- | --- |
| **French** `fr` | Easy | Decimal comma; dates day first with a slash (3/10 is 3 October, the parser reads October 3); times typed "15h30"; "en" and "de" are everyday words, so they cannot be prompt keywords; text as long as German or longer | Fix and Shorter: French is on Google's Proofreading and Rewriting lists, and on Chrome's Prompt API list. Summary: on no list | 2 days |
| **Spanish** `es` | Easy | As French. "mañana" is tomorrow and morning. "¿" opens a question. Spain writes 1,5 and Mexico 1.5: one translation, numbers from the device's locale | Fix and Shorter: listed. Chrome's Prompt API lists `es` | 2 days |
| **Italian** `it` | Easy | As French ("alle 15", "tra 20 min"). Short weekday names collide with other languages ("mar" is Tuesday and March) | Fix and Shorter: listed | 2 days |
| **Danish** `da` | Easy | æ ø å (ø and æ are not folded today); time "kl. 15.30" with a dot, which the parser reads as a date; "i morgen" is two words | On no official list: **not verified** | 2 days |
| **Swedish** `sv` | Easy | å ä ö; "kl 15", "imorgon", "om 20 min"; numbers grouped with a space | On no official list: **not verified** | 2 days |
| **Norwegian** `nb` | Easy | As Danish, and most of the Danish work carries over (it still needs its own reader). Three names for one language: resources `values-nb`, CLDR `no`, Play `no-NO` | On no official list: **not verified** | 1.5 days |
| **Polish** `pl` | Medium | Plurals with three forms (22 znaki, 25 znaków): three strings must become `<plurals>`. Day and month names change with grammar ("w środę", "3 października"). ł is not folded. One-letter words (w, z, o, i) collide with one-letter keywords: `w` is Wikipedia. Text 20 to 30 % longer than English | On no official list: **not verified** | 3 days |
| **Japanese** `ja` | Hard | An input method composes every word; no spaces, so "keyword, Space" and "start of a word" need new rules; names in kana and kanji; full-width letters, digits, space and "？"; dates with counters (15時, 10分後); a character is about a token, so the 8,000-character limit is wrong | Best of all: on the Proofreading, Rewriting **and** Summarization lists, and Chrome's | 7 days, after the CJK block |
| **Korean** `ko` | Medium to hard | Hangul is composed a syllable at a time and a half-typed syllable does not match (tested, §4); people expect to find 카카오 by ㅋㅋㅇ; particles stick to words (금요일에); "in 20 minutes" comes after the number (20분 후). Spaces exist, so chips and word starts work as they are | On all three lists | 4.5 days, after the CJK block |
| **Chinese, Simplified** `zh-Hans` | Hard | Pinyin input: the field holds Latin letters while composing, so 设置 must be found by "shezhi" and "sz"; no spaces; dates like 周五下午3点; full-width punctuation. And a market question: mainland China has neither Google Play nor the system's model | On no official Android list: **not verified** | 4.5 days, after the CJK block |
| **Chinese, Traditional** `zh-Hant` | Comes along | **Yes, bring it**: Taiwan and Hong Kong are where a Google laptop with Play is likelier to be sold than the mainland. The strings are a conversion plus a reader (設定 not 设置, 軟體 not 软件). Its users type Zhuyin or Cangjie, not pinyin: matching by Zhuyin is extra and can wait | As Simplified | 1.5 days on top of `zh-Hans` |

---

## 3. Everything that depends on language

Counted at `428f5b5`. "German" says how German was done, so that the cost of the next language shows.

### 3.1 In the string resources

| What | Count | Where | How German was done | Language N+1 |
| --- | --- | --- | --- | --- |
| UI strings | **463** (460 to translate; `app_name`, `kind_command`, `help_words` are not) | `app/src/main/res/values/strings.xml` (110), `strings_11.xml` (185), `strings_20.xml` (168) | A full second copy in `values-de/` (460). German is 22 % longer in characters (9,161 → 11,222) | Translate 460. 32 are the same in German and English (Timer, Emoji, Tab…) |
| …with format arguments | **27**, all positional (`%1$s`), none with `xliff:g` | same files | Copied with the arguments | Safe to reorder. Translators need a note on what each `%1$s` is |
| …plurals | **0** `<plurals>` | — | Not needed in German | Three strings carry a count and need plurals for Polish: `qr_sub`, `password_sub` (`strings_11.xml:101`, `:107`), `clip_counted` (`:126`, two counts in one string) |
| Typed keyword lists (comma-separated strings) | **50** strings: 21 scope keyword lists, `password_keys`, `snip_add_keys`, 19 `verb_*`, 7 hidden-word lists (`dial_*_words`, `cmd_*_words`), `help_words` | `strings.xml:22–100`, `strings_11.xml:38–134`, `strings_20.xml:16–29, 45, 55, 63, 77, 134, 157` | "The German lists keep the English keywords" (`strings_11.xml:3`). Scope keywords 36 → 56 words, verbs and places 43 → 83, hidden words 37 → 43. Umlauts doubled by hand (`öffnen,oeffnen`) | The real work: each word chosen, not translated, and checked for collisions (§3.4) |
| `guide`: everything Booklight does | **31** rows of 7 fields | `strings_20.xml:170–202` | Rewritten: 19 of 31 examples differ in German (`termin Fr 15 Uhr Zahnarzt`). Each example must run | 31 rows; every example is a test (`./bl debug guide`) |
| `tips` | **12** rows of 5 fields, 90 characters at most (`strings_20.xml:212`) | `strings_20.xml:213–226` | Rewritten. Longest German line: 88 of 90 | A longer language will not fit. The line is built as example, a space, the rest (`Panel.kt:414`): a word order Japanese and Korean do not have |
| `prompt_seeds` | **5** rows: keyword, name, the prompt | `strings_20.xml:117–123` | Prompts written in German; keywords `fix, kürzer, en, sum, erkläre`; "translate" turns round (to English) | See §5. Read once, at first start (`Prefs.kt:143–151`): a later change of language does not change them |
| Settings pages for `s` | **49** titles + **49** keyword lists (143 words) | `values/settings_pages.xml` | 49 + 49 in `values-de/` (175 words); the titles are what the system's Settings app calls its pages. `values-en-rGB/` overrides the 49 titles only | 98 items. Titles should be the system's own words in that language (AOSP's Settings translations are a source; **not verified** against Googlebook OS) |
| Shortcuts for `k` | **43** titles + **43** keyword lists (181 words) | `values/keys_table.xml` | 43 + 43 in `values-de/` (354 words: the German lists carry the English words too) | 86 items |
| Key names | 10 (`key_ctrl` … `key_overview`) | `strings_11.xml:140–141`, `strings_20.xml:37–44` | Strg, Umschalt, Entf | As printed on that country's keyboard |
| Place names, 12 characters at most (`strings_20.xml:4`) | 11 + Left half, Right half | `strings_20.xml:5–15`, `strings.xml:106–107` | Longest German name is exactly 12 | Will not fit in French or Polish without shortening, or a wider strip |
| **All of it** | **692** units, **2,934** English words | | | About 2,900 words to translate per language |

### 3.2 In Kotlin

| What | Count | Where | How German was done | Language N+1 |
| --- | --- | --- | --- | --- |
| Words of the date and time reader | **159** words in 12 tables (with the two rows below: **220** words in 15 tables): `LEADING` 13, `HOUR` 2, `WORDS` 6, `WEAK` 11, `WEEKDAYS` 37, `MONTHS` 36, `AHEAD` 13, `AHEAD_MONTHS` 5, `MARKS` 5, `NAMED` 5, ordinal endings 4, `Durations.UNITS` 22; plus `"in"`, `"at"`, `"um"` as literals | `core/…/When.kt:46–47, 85`, `WhenParts.kt:18–32, 119, 131–133, 197–203`, `Durations.kt:8–12`, `Jot.kt:107, 121, 127` | German words typed into the same tables as the English ones. **Both are always on**, whatever the app's language: "morgen" and "15 Uhr" work in an English Booklight. "do", "so", "di", "mi" needed a special rule (`WEAK`) because they are words too | Cannot go on like this. With ten languages in one table "mar" is Tuesday (it, es) and March (en), "man" is Monday (da), "dim" is Sunday (fr). §7, item 1 |
| Kinds for `new` | **16** words | `core/…/Jot.kt:38–44` | German words in the same map | A real trap: choosing a kind writes its **translated** name into the field (`scopes/Jot.kt:219`), and the parser only knows English and German. In French "fichier " would be read as a file called "fichier" |
| Question words (when Gemini goes first) | **42** (21 + 21) | `core/…/Ask.kt:8–13` | A second list | A list per language. Chinese, Japanese and Korean ask at the end of the sentence, and "five or more words" (`Ask.kt:19`) never happens without spaces |
| "New …" in other apps' commands | 3 | `providers/AppCommands.kt:330` | `"neue", "neu "` added | One more word per language |
| Calculator | number format, `of`, `mod`, 15 function names | `core/…/Calc.kt:26–43, 101–102, 157–167, 173–180` | **Not done in 2.0**: "The calculator still has no decimal comma (the German examples use a point)" (`docs/PICKING-UP.md:62`). Done in 2.1 (`f52fbf3`): the decimal sign comes from the locale, the answer is the English one with comma and point swapped | All seven European targets write 1,5, so 2.1 covers reading. French, Swedish, Norwegian and Polish group thousands with a space (tested: 1 234 567,5), which the swap does not give: they would see 1.234.567,5 |
| Dates and times shown | 6 places with a fixed pattern | `scopes/Jot.kt:53, 54, 56, 58–66`, `scopes/NotesScopes.kt:26–27`, `pin/PinActivity.kt:339` | The names come from the locale, the order is fixed: "Fr. 2 Okt." | Wrong in Japanese, Korean, Chinese: "EEE d MMM" gives 金 2 10月, "h:mm a" gives 3:00 午後 (tested, JDK 21). The range shortener (`Jot.kt:64–65`) assumes "PM" is a last word |
| Emoji and symbols | **1,886** emoji + **233** symbols, each with a name and keywords | `app/src/main/assets/emoji.tsv` (216 KB), `tools/emoji.py`, `core/…/Emoji.kt:16`, `scopes/Picks.kt:66` | Emoji: German from Unicode CLDR by the script (`emoji.py:371`). Symbols: 233 names and about 1,090 words by hand. A `german: Boolean` in the code | CLDR has all eleven languages (checked; Norwegian is `no.xml`). The 233 symbols are hand work again. One file per language, read one at a time; each is about 100 KB |
| English left in the code | keycaps `esc`, `tab` (5 places); `UUID` and the word `uuid`; `HEX RGB HSL OKLCH`; `Notes.md`, `Todo.md`; the `?` that opens the list of everything; ` / ` and ` @ ` as separators | `overlay/Field.kt:118`, `Footer.kt:84–100`, `Panel.kt:422`, `Rows.kt:467`; `providers/Answers.kt:36`; `core/…/NoteText.kt:15–16`; `overlay/OverlayModel.kt:659`; `core/…/Jot.kt:71, 152` | Left as they are | Fine for Latin scripts. A Japanese keyboard in kana mode types ？, ／, ＠ and a full-width space: none of the four is recognised (§4) |
| Search engines and sites | 5 engines, 7 default sites | `core/…/Sites.kt:14–39` | Not localised: `w` searches the **English** Wikipedia in German too; suggestions are asked for with no language | §6 |
| The app's two lists of languages | 2 | `res/xml/locales_config.xml`, `app/build.gradle.kts:58` | `de` in both | One line in each, or let the build write the first (§8) |

### 3.3 Outside the app

| What | Count | Where | German | Language N+1 |
| --- | --- | --- | --- | --- |
| Play listing | title (30), short (80), full (4,000), release notes (500) | `store-submission/listing/en-US/`, `de-DE/` | Written by hand. Full description: 3,549 characters in English (3,604 after 2.1), **3,995 of 4,000** in German | Any longer language must be cut. 641 words |
| Screenshots | 8 captions + a tagline, 12 captures | `store-submission/graphics/spec.json`, `docs/design/captures/` | None: the German listing shows the English pictures (`googlebook-tech/scripts/play/listing.mjs:40`) | A decision (§12) |
| Release notes on Play | one language | `tools/play-upload.mjs:65`, `.github/workflows/release.yml:24` | `de-DE/release-notes.txt` exists and is **not uploaded**: the workflow sends `en-US` only | A small change, then N texts for every release |
| Privacy page | 1, English, 694 words | `PRIVACY.md`, googlebook.studio/privacy/booklight (the site is `lang="en"`) | English only | Can stay English; Play wants one address |
| Tests | 221 core tests; German in 31 lines of 3 files | `core/src/test/…/WhenTest.kt`, `JotTest.kt`, `VerbsTest.kt` | Cases added beside the English ones. "German was not checked on the device" (`docs/PICKING-UP.md:199`) | §10 |

### 3.4 Keywords and collisions today

- A keyword and a Space becomes a chip (`core/…/SearchEngine.kt:75–82`, `overlay/OverlayModel.kt:208`); the keyword
  alone and Tab does too (`SearchEngine.kt:85–89`). Both look for the ASCII space only.
- Built-in keywords are reserved: a user's link cannot take one, except the one-letter `s` and `k`, which give way
  to the user's link (`scopes/Scopes.kt:55–62`, `providers/Keys.kt:81, 100`). A prompt whose keyword is taken comes
  without one (`Scopes.kt:86–92`, `Prefs.kt:145–148`). Another app never gets a keyword somebody has (`Scopes.kt:96`).
- Between Booklight's own scopes the first in the list wins (`SearchEngine.kt:80`), and **nothing checks** that two
  lists do not share a word. Counted: none do, in English or German.
- A word that is also an ordinary word is saved by three rules: the whole text is tried as an app's name first
  ("play store"), Backspace turns the chip back into the word ("new york weather"), and `new window` is only a verb
  after a name (`providers/AppsProvider.kt:116`).
- What the next languages add: "en" (the German seed's keyword for "to English") is "in" in French and Spanish and
  "a" in Danish, Swedish and Norwegian; "de" (the English seed's keyword for "to German") is "of" in French, Spanish
  and Italian; `w` is "in" in Polish ("w piątek dentysta" would search Wikipedia); `pin` and `de` are pinyin
  syllables (§4).

---

## 4. Search and matching

**How it works today** (`core/…/Matcher.kt`). Both sides are folded (`fold`, `:113–125`): Unicode NFD, every
combining mark dropped, lower case, everything that is not a letter or digit becomes one space. Then, best first:
the same (1.0), the start of the name (0.9), the start of a later word (0.8), initials (0.75), anywhere inside (0.6),
the letters in order with gaps, from three letters (0.45). Words are split at separators and camelCase humps
(`words`, `:128–146`). There is **no typo tolerance** other than the gaps. A query of several words must match
word by word (`:27–39`).

**What that does to each language** (tested with the same rule on JDK 21):

| Typed / name | Result today | Why | Fix |
| --- | --- | --- | --- |
| `goteborg` / Göteborg, `are` / Åre | match | NFD splits ö and å into a letter and a mark | — |
| `lodz` / Łódź | **no match** (folds to `łodz`) | ł has no decomposition | A small table: ł→l, ø→o, æ→ae, đ→d, ß→ss, œ→oe |
| `smorrebrod` / Smørrebrød, `aero` / Ærø | **no match** | ø and æ the same | same table |
| `strasse` / Straße | **no match** | ß; German gets by with doubled entries | same table; the doubles can go |
| `かれ` / カレンダー | **no match** | hiragana and katakana are different letters | fold hiragana to katakana |
| `ガイド` | folds to `カイト` | the voicing mark is a combining mark and is dropped, so が = か, ぱ = は | keep U+3099 and U+309A |
| `ｃｈｒｏｍｅ` / Chrome, `１２*３` | **no match**, no sum | full-width forms are other characters; the parsers test `in '0'..'9'` | NFKC before folding |
| `メモ　牛乳` (full-width space) | no chip | `indexOf(' ')` (`SearchEngine.kt:77, 87`, `Sites.kt:47`) | any white space |
| `？` | not the list of everything | `HELP = "?"` (`OverlayModel.kt:659`) | accept ？ |
| `하` / 한글 | match | NFD turns a syllable into its jamo, and 하 is a prefix of 한 | — (a lucky accident worth keeping) |
| `간` on the way to 가나 | **no match** for one keystroke | the ㄴ sits under 가 as a final until the next vowel moves it | let a trailing final count as the next initial |
| `ㅎ`, `한ㄱ` | **no match** | a lone consonant is a "compatibility" letter, not the jamo of the syllable | NFKC maps it; then initials-only search (ㅋㅋㅇ) is a small step |
| `shezhi`, `sz` / 设置 | **no match** | nothing knows pinyin | a second search key per name, from `android.icu.text.Transliterator` |
| `せってい` / 設定 | **no match** until converted | nothing knows the reading of kanji, and Android has no API that does | the app's English label as a second name ("Settings"); after conversion 設定 matches exactly |
| `Googleカレンダー` | one word | no space, no hump | a change of script starts a word |

**No spaces** (Japanese, Chinese). Three things depend on spaces:
1. *Keyword, Space, chip.* Works once the keyword is committed and any space counts. The keyword itself must be a
   word of the language (メモ, 笔记) **and** the English one must stay, because many people switch to Latin input
   for a launcher.
2. *Start of a word.* A name such as 写真とビデオ has none. Short names make "anywhere inside" good enough; it
   should score as a word start when it begins where the script changes.
3. *Words of the line* (`core/…/Words.kt:8`, used by every parser). "明日15時歯医者" is one word, so no date is found.
   The date reader needs a second way in that reads from the two ends of the line by characters (§7, item 5).
   Also: lengths counted in letters (two letters before a keyword hints, `SearchEngine.kt:102`; two before shortcuts
   answer, `Keys.kt:67`; three before a prompt is asked, `OverlayModel.kt:656`) are wrong when one character is a word.

**Input methods and the field** (`overlay/Field.kt:106–115`, `overlay/Panel.kt:228–294, 333–336`).
- *Does it search on composing text?* **Yes.** The field is a `BasicTextField` with a `TextFieldValue`; every
  change, composing or not, goes to `model.type(v.text)`. In Chinese that is useful (the composing text is pinyin
  letters). In Japanese it is hiragana, which finds nothing until kana are folded.
- *Can a keyword become a chip in the middle of a composition?* **Yes, and it should not.** Gboard's pinyin shows
  syllables apart while composing ("pin yin"); `pin` is a keyword, so the text would turn into the Pin chip and the
  field would be replaced under the input method (`Panel.kt:335`). **Not verified** on the device; the guard is
  small: `v.composition != null` means search only, no chip, no grey completion, no question to the model.
- *Tab, Enter, Space, arrows while composing.* Booklight takes keys in `onPreviewKeyEvent` (`Panel.kt:228, 294`),
  which Android delivers after the input method has had them. While composing, the input method keeps Space
  (convert), Enter (commit), Tab and the arrows (candidates), and Booklight sees none of them: correct, and it
  means "type, Enter" is "type, Enter, Enter" for these users. By the platform's design; **not verified** with
  Gboard and the Googlebook's keyboard.
- *One-letter keywords.* In kana mode "s" is a half-typed syllable (ｓ), so `s`, Tab does not work until the user
  switches to Latin input. `KeyboardOptions.hintLocales` and `KeyboardType.Ascii` exist in the Compose version in
  use (checked in the Gradle cache) and could ask the input method to start in Latin; whether Gboard honours them
  on a hardware keyboard is **not verified**.
- *The candidate window* may cover the first rows of the panel. **Not verified.**
- `./bl debug keys` calls `model.type` directly (`DebugReceiver.kt`), so none of this can be tested through the
  debug hooks. It needs hands on the keyboard, or a new hook that sets a composing range.

**Fonts** (`ui/Theme.kt:26–38`). Booklight draws with `/product/fonts/GoogleSansFlex-Regular.ttf`. Google Sans
Flex covers Latin, extended Latin and Vietnamese (Google Fonts' metadata): Polish and Nordic letters are in it;
**Japanese, Korean and Chinese are not**. A font loaded from a file falls back to the system's fonts for missing
glyphs, so the text will show, in Noto Sans CJK, without the rounded cut and with its own line height.
**Not verified** on the device: row heights, the 36 sp headings, and the upper-case small labels
(`overlay/Bodies.kt:94`, `window/MainActivity.kt:321`), which do nothing for these scripts.

---

## 5. The on-device model

`docs/research/on-device-ai.md` and `device-findings.md` already have: the Prompt API through ML Kit, the Lenovo
runs `nano-v3` with a limit of 8,192 tokens, German worked, the HP is unchecked. What is new here is languages.

**What Google says, as of today:**

| Source | Languages |
| --- | --- |
| ML Kit GenAI overview (updated 28 Sep 2026) | No list. "Availability of specific language support may vary depending on the particular device's configuration and the models that have been downloaded to the device." |
| Prompt API pages (get started, prompt design, select a model) | Nothing about languages |
| Proofreading API, Rewriting API (same model underneath) | English, Japanese, French, German, Italian, Spanish, Korean |
| Summarization API | English, Japanese, Korean |
| Chrome's Prompt API (Gemini Nano on desktops) | `en`, `ja`, `es`, `de`, `fr`; "Support for additional languages is in development" |
| The model families | `nano-v3` is "built on the same architecture as Gemma 3n", which is "trained in over 140 languages"; Nano 4 comes from Gemma 4, which "natively supports over 140 languages" |

So: **there is no official input or output language list for the API Booklight uses.** The nearest evidence is
the lists of the purpose-built APIs.

**What it means for the five prompts** (`strings_20.xml:117–123`):

| Prompt | fr, es, it | ja, ko | da, sv, nb, pl, zh |
| --- | --- | --- | --- |
| Fix spelling | Listed for Proofreading: expect it to work | Listed | **Not verified** |
| Shorter | Listed for Rewriting | Listed | **Not verified** |
| Translate | On no list in any language (there is no translation API). German ↔ English was right on the Lenovo | same | **Not verified** |
| Summary | **Not listed** (only en, ja, ko). The German summary on the Lenovo was right but wrote "Google Books" for "Googlebooks" | Listed | **Not verified** |
| Explain | On no list in any language | same | **Not verified** |

What to do about it:
1. **Probe before promising.** For each language: the five prompts on a fixed paragraph, on the Lenovo, the answers
   put in a table for the native reader. The panel must be in front (`device-findings.md`), so it is a `./bl debug`
   loop with pauses for the per-app quota, not a unit test.
2. **Where a language fails**, the row already has its way out: "Open in Gemini" (`scopes/Prompts.kt:89–90`). The
   decision is whether to offer the on-device answer there at all (§12).
3. **The translate prompt** needs a target and a keyword per language. English ships "de, In German" (Alex's own
   case), German ships "en, Auf Englisch". Everyone else wants "to English", and "en" cannot be the keyword in
   French, Spanish, Danish, Swedish or Norwegian.
4. **The length limit is in characters** (`Prompts.kt:133`, `MAX = 8000`, "about 4,000 tokens"). A Japanese or
   Chinese character is about one token, so 8,000 characters is twice the limit. Use the library's `countTokens`,
   or a limit per script.
5. **The prompts are stored once**, in the language the device had at first start (`Prefs.kt:143–151`).
6. The HP (Qualcomm) is still unchecked for any language.

---

## 6. Outside the app

- **Play listing.** `googlebook-tech/scripts/play/listing.mjs` already uploads every `listing/<language>/` folder's
  three texts, so a new language is four text files. Limits: 30, 80, 4,000, and 500 for release notes. German is at
  3,995: French, Spanish, Italian and Polish must be written shorter, not translated.
  Play Console translates listings by machine for free into ten languages, of which four are ours (French, Italian,
  Japanese, Spanish); it says they are "not reviewed or approved by humans". For a language with no listing, Play
  offers the visitor an automatic translation. Human translation through Play: "as little as USD 0.07 per word".
- **Play's own translation of app strings** (free, Gemini, needs a bundle in a draft release, can be edited and
  switched off per string). Not for Booklight as it stands: it would reach Play installs only (the GitHub APK
  would stay English), and it would translate keyword lists and examples as if they were prose.
- **Screenshots.** Eight captions and a tagline in `spec.json`, twelve captured panels. Play says to localise
  "taglines or text overlays". Captions per language are cheap; panels per language need a capture run per
  language (§10).
- **Release notes.** One line in `tools/play-upload.mjs:65` sends `en-US`. Sending every
  `listing/*/release-notes.txt` is small; writing twelve texts of 500 characters for every release is the cost.
- **Privacy page.** English, on googlebook.studio. It can stay English. The app's own words about privacy (the
  first-run card, the About page) are strings and are translated with the rest; the three must still say the same.
- **Search suggestions.** Asked with no language today, so a Polish user gets American suggestions. Tested today
  with "pog": without a parameter all five answer in English ("pogo", "pogba"); with one, all five answer "pogoda":

  | Engine | Parameter |
  | --- | --- |
  | Google | `hl=pl` (and `gl=pl`) |
  | DuckDuckGo | `kl=pl-pl` |
  | Bing | `mkt=pl-PL` |
  | Brave | `country=pl` |
  | Ecosia | `mkt=pl-pl` |

  It sends one more thing to the search engine (the language), so the first-run card, `PRIVACY.md` and the data
  safety form change by a clause.
- **Default sites.** `w` should search the Wikipedia of the language, and must not be `w` in Polish.

---

## 7. Engineering needed once

S = half a day or less, M = one to three days, L = about a week.

### Before any language (about 10 days)

| # | What | Files | Approach | Size |
| --- | --- | --- | --- | --- |
| 1 | **The parsers' words out of the code** | `core/…/When.kt`, `WhenParts.kt`, `Durations.kt`, `Jot.kt`, `Ask.kt`; `providers/AppCommands.kt:330` | A `Lexicon` per language, as plain Kotlin in `core/` (one small file each, 60 to 90 words, with its own test): day words, weekday and month names (seeded from `java.time`'s own names, which the JDK has for every target, plus the short forms people type), the words before a time and a duration, the markers, the units, the kinds of `new`, the question words. The reader gets **the app's language plus English**, never all of them. German becomes the first file and must pass today's 221 tests unchanged | M |
| 2 | **Numbers and dates by locale** | `core/…/Calc.kt`, `providers/Providers.kt:27–32`, `scopes/Jot.kt:51–66`, `scopes/NotesScopes.kt:20–29`, `pin/PinActivity.kt:337–347`, `WhenParts.kt:54–72` | Calculator: 2.1 already takes the decimal sign from `DecimalFormatSymbols` (so Mexico differs from Spain); what is left is the grouping sign (French, Swedish, Norwegian and Polish group with a space). Shown dates: `DateFormat.getBestDateTimePattern(locale, "EEEdMMM")` instead of fixed patterns; ranges with `android.icu.text.DateIntervalFormat` (API 24). Typed dates: day or month first after a slash by locale; "15.30" as a time where the locale writes it so | M |
| 3 | **Folding for Latin scripts** | `core/…/Matcher.kt:113–146` | The table of §4 (ł, ø, æ, đ, ß, œ) in `fold` and `words`; any white space in `SearchEngine.kt:77, 87` and `Sites.kt:47`. Then the doubled German entries can go | S |
| 4 | **Behaviour strings apart, with rules a test can check** | `res/values/strings*.xml` → a `lexicon.xml` for the 50 keyword lists, `guide`, `tips`, `prompt_seeds`; `app/src/test/` (new) | A JVM test that reads the XML of every language: same array lengths, 7 and 5 and 3 fields, tips ≤ 90 and places ≤ 12 characters, no keyword in two scopes, the English keywords still there, no keyword on that language's list of everyday words, arguments the same as in English. Three strings become `<plurals>`. Comments for translators on every list | M |
| 5 | **Emoji, symbols, letters by language** | `tools/emoji.py`, `core/…/Emoji.kt:16–40`, `scopes/Picks.kt:59–68`, and the new `Letters.kt` | A language code instead of `german: Boolean` (now in two places: `Picks.kt:67` and `:86` at `f52fbf3`); one `emoji-<lang>.tsv` per language written by the script from CLDR; only the current one is read | M |
| 6 | **A language change while Booklight runs** | `BooklightApp.kt:61–87` | Everything is built once at start from the strings of that moment (keywords, pages, verbs, the emoji table, app names). After a switch the tables are in the old language until the process dies. Rebuild on a change of configuration, or end the process when no window is showing | S |
| 7 | **The lists of languages, and the tools** | `res/xml/locales_config.xml`, `app/build.gradle.kts:58`, `app/lint.xml`, `bl`, `tools/play-upload.mjs:65`, `core/…/Sites.kt`, `providers/SuggestProvider.kt` | `generateLocaleConfig = true` with `res/resources.properties` (`unqualifiedResLocale=en-US`) and the hand-written file removed (the build fails if both exist); `./bl lang fr` (sets the app's language and restarts it); `./bl debug guide` and `./bl shot` over every language; release notes for every language; the suggestion parameter; Wikipedia by language | M |
| 8 | **Room for longer text** | `strings_20.xml:4, 212`, `overlay/Strip.kt`, `overlay/Field.kt:140`, `overlay/Panel.kt:414` | A run in the pseudolocale `en-XA` (needs `isPseudoLocalesEnabled` in the debug build and `en-rXA` in its filter); decide the 12 and the 90; the tip's line as a format string instead of "example, space, rest" | S to M |

### Before Japanese, Korean or Chinese (about 9 days more)

| # | What | Files | Approach | Size |
| --- | --- | --- | --- | --- |
| 9 | **Matching for CJK** | `core/…/Matcher.kt`, `providers/AppsProvider.kt:69–89, 129–157`, `SearchEngine.kt:95–111` | NFKC first; hiragana to katakana, voicing kept; a change of script starts a word; a trailing Hangul final counts as the next initial, and initials-only search; **second search keys per name**, made when the app list is read, not per keystroke: `core/` takes an interface, the app fills it with `android.icu.text.Transliterator` (API 29; AOSP's contacts use `"Han-Latin/Names; Latin-Ascii"` for exactly this) for pinyin and pinyin initials, and with each app's English label, read through that app's resources with an English configuration. Length rules by script | L |
| 10 | **The field under an input method** | `overlay/Panel.kt:228–336`, `overlay/OverlayModel.kt:193–211, 659`, `overlay/Field.kt:106–115` | Pass "is composing" to the model: search only while composing. Full-width space, ？, ／ and ＠ count as their plain twins when a line is read. A debug hook that sets composing text. Then a session on the Lenovo with Gboard in Japanese, Korean and pinyin | M |
| 11 | **A date reader that needs no spaces** | `core/…/When.kt:72–81`, `Words.kt` | A second way in beside the word-based one: read the longest expression from the start or the end of the line by characters (明日, 金曜, 午後3時, 15時半, 10分後; 周五下午3点; 금요일 오후 3시, 20분 후). The same `Moment` comes out, so nothing after it changes | M to L |
| 12 | **The model's limit by tokens** | `scopes/Prompts.kt:67, 133`, `ai/OnDevice.kt` | `countTokens`, or 8,000 characters for Latin scripts and about 3,500 for CJK | S |

**The three biggest:** item 1 with item 2 (the typed language as data, by locale), item 9 with item 10 (matching
and the field for CJK), and item 4 with item 7 (what makes eleven more languages checkable by one person).

---

## 8. How Android handles it

- **Language per app** is already there: `android:localeConfig` in the manifest (`AndroidManifest.xml:53`), and the
  system's Settings shows "Language" for Booklight (Android 13 and later). Tags to use: `fr`, `es`, `it`, `da`,
  `sv`, `nb`, `pl`, `ja`, `ko`, `zh-Hans`, `zh-Hant` (Google's page names the last two that way), with folders
  `values-fr` … `values-b+zh+Hans`, `values-b+zh+Hant`.
- **Testing without changing the device's language:**
  `adb shell cmd locale set-app-locales io.github.kuscher.booklight --user current --locales ja-JP`, and
  `get-app-locales` to read it back (AOSP's `LocaleManagerShellCommand`). Until item 6 is done, restart the app
  after it. The device rules stay as they are: this touches Booklight only.
- **Plurals:** `<plurals>` with `one`, `few`, `many`, `other`. Only Polish needs more than two forms; Japanese,
  Korean and Chinese have one.
- **Pseudolocales:** `en-XA` puts accents on the English text, makes it longer and brackets it, to show what
  breaks before a translator is paid.
- **Text length.** Measured here: German is 22 % longer than English. IBM's table (W3C): a string of up to 10
  characters may triple, one of 70 or more grows by about 30 %. In practice French, Spanish, Italian and Polish
  run as long as German or longer; Danish, Swedish and Norwegian about as English; Japanese, Korean and Chinese
  have fewer but wider characters.
- **APIs that replace hand-made code:** `DateFormat.getBestDateTimePattern`, `android.icu.text.DateIntervalFormat`,
  `RelativeDateTimeFormatter` (today, yesterday), `DecimalFormatSymbols`, `android.icu.text.Transliterator`,
  `android.icu.text.BreakIterator` (words in Japanese and Chinese). They format; **none of them reads what a person
  typed** ("Fri 3pm Dentist"). The reader stays Booklight's own.
- **Right to left** is out of scope. The manifest already says `supportsRtl="true"`.

---

## 9. How the translating would really be done

1. **A glossary first**, per language, of the forty or so words that are Booklight's own: panel, row, chip, scope,
   keyword, pin, snippet, recipe, prompt, link, note, to-do, and every keyword. Decided once, used everywhere. The
   keywords are **chosen**, not translated: the word a person would type without thinking, short, not an everyday
   first word of a sentence, not another scope's.
2. **Draft by machine** (Claude, with the glossary, the English and the German side by side, and the comments of
   item 4). The German copy is the better source for tone: "Copy is plain" (`CLAUDE.md`).
3. **Checks that need no reader:** the resource test of item 4, the core tests of that language's lexicon, and
   `./bl debug guide` in that language: every example of the list of everything is typed and must give a row
   that can run.
4. **A picture book for the reader.** `./bl shot` and `./bl wshot` in that language: the twelve store scenes, the
   seven sections of the window, each scope with its example, the tips, the first-run cards. About forty pictures
   on one page. A native reader checks pictures, not XML: wrong words show there, and so does text that is cut off.
5. **A native reader, once per language**, with three questions: is anything wrong, is anything odd, what would
   you type for these twenty things. The last one finds the keywords a translation never would.
6. **Cost if bought:** about 3,650 words per language (2,934 in the app, 641 in the listing, the rest captions and
   notes): about USD 260 at Play's quoted USD 0.07 a word. The 233 symbols add about 1,000 words if they are done.

---

## 10. Tests per language

| Test | Where it runs | What it catches |
| --- | --- | --- |
| Lexicon test: about forty typed lines and what they mean ("ven 15h dentiste", "dans 20 min", "demain 9-9:30") | `./bl test`, JUnit | The date and time reader in that language; English lines must still pass with that lexicon on |
| Matching test: the table of §4 | `./bl test` | Folding, kana, Hangul, pinyin (with a fake for the transliterator) |
| Resource test (item 4) | Gradle, on the Mac and in CI | Missing rows, broken fields, limits, colliding keywords, lost English keywords, arguments |
| Lint `MissingTranslation` | already in the release workflow (`:app:lintRelease`) | A string that is not there |
| `./bl debug guide`, once per language | Lenovo | Every example of the list of everything works in that language (31 rows, three of them the user's own) |
| Picture book | Lenovo, then a reader | Wrong or cut-off text |
| Model probe: 5 prompts × a fixed text | Lenovo | Whether the device's model can be offered in that language |
| Input method script, by hand: twelve steps (compose, convert, commit, Space, Tab, Enter, Esc, a keyword, a sum, a date) | Lenovo with Gboard | Everything in §4 that the debug hooks cannot reach |

---

## 11. Phases

| Phase | What | Why this order | Days |
| --- | --- | --- | --- |
| 0 | Items 1 to 8. German moves onto the new structure and nothing changes for the user. German is checked on a device for the first time | Proves the structure on a language Alex reads. Fixes ß, the comma and the release notes for German on the way | about 10 |
| 1 | French, Spanish, Italian | The same shape as German, the model is on Google's lists for them, readers are easy to find, the largest audiences | about 6 |
| 2 | Danish, Swedish, Norwegian, then Polish | The three share most of their work. Polish last: it is the one that tests plurals and inflection. The model must pass the probe first | about 8.5 |
| 3 | Items 9 to 12, then Japanese | Japanese is the hardest and has the best model support: if the design holds for it, it holds for the other two | about 9 + 7 |
| 4 | Korean, then Chinese (Simplified and Traditional) | They reuse phase 3 | about 4.5 + 6 |

Each language is a release of its own, to the closed testers first. A language can stop at "screens only" (see
decision 2) and get its typed words later.

---

## 12. Decisions for Alex

1. **Keywords: translated, with the English ones always working too** (as German today)? Recommended: yes. The
   other way, English keywords only, makes every language cheap and makes the list of everything read oddly.
2. **How deep per language.** (a) screens and listing only; (b) plus the typed words: keywords, verbs, dates,
   settings and shortcut words (where German is); (c) plus matching and the input method. Recommended: (b) for the
   seven European languages, (c) or nothing for Japanese, Korean and Chinese: a launcher that cannot find カレンダー
   from かれ is not a Japanese launcher.
3. **Which words the date reader knows:** the app's language and English (recommended), or every language at once
   (as German today). With the first, "morgen" stops working in an English Booklight.
4. **One-letter keywords** `s`, `k`, and the sites `g`, `w`: the same in every language, or other letters where
   they are words (Polish `w`)?
5. **The translate prompt:** its target and keyword per language; and whether the English one stays "to German".
6. **The on-device model where Google lists no support** (Danish, Swedish, Norwegian, Polish, Chinese): offer it
   if the probe reads well, or hand those languages to the Gemini app only?
7. **Japanese, Korean, Chinese:** accept "Enter, Enter", or take Enter before the input method does? Ask the
   input method to start in Latin?
8. **Traditional Chinese with Simplified?** Recommended: yes. And which countries are Googlebooks sold in: that
   should set the order more than anything here. **Not verified.**
9. **Who reads each language**, and whether to pay (about USD 260 a language).
10. **Store pictures:** per language, or English pictures under translated text (as German today)?
11. **Suggestions in the user's language:** yes means the language goes to the search engine with the text, and
    the privacy words change.
12. **Release notes:** all languages every release, or English plus the ones that are ready?
13. **The rule in `CLAUDE.md`** ("Every user-facing string is a resource, English and German"): what it becomes,
    and whether a release may ship with a language behind.

---

## 13. Risks

- **Every later feature costs twelve times the words**, and a keyword needs twelve collision checks. This is the
  one that does not go away.
- **A wrong word here is a broken feature**, not a typo: a keyword nobody would type, an example that does not run.
  Machine translation is weakest exactly there.
- **Chips that steal ordinary text.** Already delicate in English ("new york weather"). Each language adds its own
  ("en", "de", "w", "pin").
- **The model writes badly in a language and Booklight shows it as "On this device".**
- **Input methods differ**, cannot be tested by the hooks, and Gboard on a Googlebook with a hardware keyboard is
  unexplored.
- **Settings page names that are not the system's** make `s` feel wrong; they have to be checked against the
  device in each language.
- **Limits:** 12 characters, 90 characters, 4,000 on Play. German is already at all three.
- **Reviews and mail in languages Alex does not read.**
- **What is stored in one language** (the five prompts; what Booklight has learned for typed text) stays in it
  after a switch.
- **2.1 added one more German-only switch** (`Letters(german)`, beside `EmojiIndex(german)`), and a table of
  letter names in English and German inside `Letters.kt`: one more thing to make general in item 5.

---

## 14. Not verified

- The Prompt API's real languages on a Googlebook: no list exists; only German was tried on the Lenovo.
- Everything about input methods in §4: composing text with spaces, which keys Gboard keeps, `hintLocales`, the
  candidate window.
- Whether the device's Google Sans Flex file matches Google Fonts' coverage, and how CJK fallback looks in the panel.
- That `Han-Latin` and the kana transliterators are in the Googlebook's ICU data (AOSP uses them; not run here).
- The system Settings app's page names in each language on Googlebook OS.
- Play's language codes for the folders (from memory: `fr-FR`, `es-ES`, `es-419`, `it-IT`, `da-DK`, `sv-SE`,
  `no-NO`, `pl-PL`, `ja-JP`, `ko-KR`, `zh-CN`, `zh-TW`), and what Play shows for release notes in a language that
  has none.
- Where Googlebooks are sold.
- The day figures: estimates from the counts above, not from having done one.

---

## 15. Sources

**The on-device model**
- [ML Kit GenAI overview](https://developers.google.com/ml-kit/genai) (language sentence, device lists; updated 28 Sep 2026)
- [Prompt API](https://developers.google.com/ml-kit/genai/prompt/android) · [get started](https://developers.google.com/ml-kit/genai/prompt/android/get-started) (4,000 tokens, `countTokens`) · [prompt design](https://developers.google.com/ml-kit/genai/prompt/android/prompt-design) · [select a model](https://developers.google.com/ml-kit/genai/prompt/android/select-model)
- [Proofreading](https://developers.google.com/ml-kit/genai/proofreading/android) · [Rewriting](https://developers.google.com/ml-kit/genai/rewriting/android) · [Summarization](https://developers.google.com/ml-kit/genai/summarization/android) (the language lists)
- [Gemini Nano on Android](https://developer.android.com/ai/gemini-nano) · [AICore developer preview](https://developers.google.com/ml-kit/genai/aicore-dev-preview)
- [Prompt API alpha, Oct 2025](https://android-developers.googleblog.com/2025/10/ml-kit-genai-prompt-api-alpha-release.html) (nano-v3 and Gemma 3n) · [AICore Developer Preview, Apr 2026](https://android-developers.googleblog.com/2026/04/AI-Core-Developer-Preview.html) (Gemma 4, 140 languages)
- [Gemma 3n](https://ai.google.dev/gemma/docs/gemma-3n) ("trained in over 140 languages")
- [Chrome's Prompt API](https://developer.chrome.com/docs/ai/prompt-api) (`en`, `ja`, `es`, `de`, `fr`)

**Android**
- [Per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages) (`localeConfig`, `generateLocaleConfig`, `zh-Hans`, `zh-Hant`, `nb`)
- [Pseudolocales](https://developer.android.com/guide/topics/resources/pseudolocales) · [Localize your app](https://developer.android.com/guide/topics/resources/localization) (`xliff:g`, notes for translators)
- [Text fields in Compose](https://developer.android.com/develop/ui/compose/text/user-input)
- [`android.icu.text.Transliterator`](https://developer.android.com/reference/android/icu/text/Transliterator) (API 29, by the SDK's `api-versions.xml`)
- [AOSP `LocaleManagerShellCommand`](https://github.com/aosp-mirror/platform_frameworks_base/blob/main/services/core/java/com/android/server/locales/LocaleManagerShellCommand.java) (`set-app-locales`, `get-app-locales`)
- [AOSP contacts, `HanziToPinyin`](https://android.googlesource.com/platform/packages/providers/ContactsProvider/+/refs/heads/main/src/com/android/providers/contacts/HanziToPinyin.java) (`"Han-Latin/Names; Latin-Ascii; Any-Upper"`)

**Play**
- [Translate and localise your app](https://support.google.com/googleplay/android-developer/answer/9844778) (machine translation, its ten languages, USD 0.07 a word, app strings with Gemini)
- [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152) (30, 80, 4,000)
- [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348) (500 characters per language, the `<en-US>` tags)
- [Preview assets](https://support.google.com/googleplay/android-developer/answer/9866151) (screenshots, localised overlays)

**Text, fonts, emoji**
- [W3C: Text size in translation](https://www.w3.org/International/articles/article-text-size) (IBM's table)
- [Google Sans Flex, Google Fonts metadata](https://github.com/google/fonts/blob/main/ofl/googlesansflex/METADATA.pb) (subsets)
- [Unicode CLDR 48.2 annotations](https://github.com/unicode-org/cldr/tree/release-48-2/common/annotations) (emoji names; a file for each target, Norwegian as `no.xml`)

**Suggestion addresses asked today** (query "pog", with and without the parameter)
- `https://suggestqueries.google.com/complete/search?client=firefox&hl=pl&gl=pl&q=…` · `https://duckduckgo.com/ac/?type=list&kl=pl-pl&q=…` · `https://api.bing.com/osjson.aspx?mkt=pl-PL&query=…` · `https://search.brave.com/api/suggest?country=pl&q=…` · `https://ac.ecosia.org/autocomplete?type=list&mkt=pl-pl&q=…`

**This repo, at `428f5b5`**
- `CLAUDE.md` · `docs/PLAN.md:239` · `docs/PICKING-UP.md:62, 199` · `docs/design/ux-model.md` §3 · `docs/research/on-device-ai.md` · `docs/research/device-findings.md:210–237` · `docs/RELEASING.md` · `store-submission/README.md`
- `~/googlebook-tech/scripts/play/listing.mjs` (other languages' texts are uploaded; pictures are not)
