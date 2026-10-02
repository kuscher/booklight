# The Booklight window: a plan for its structure and look

*1 October 2026. Desk work: the code in `app/…/window/`, eleven captures of the window on the Lenovo at three sizes (not kept in the repo), Google's
published guidance, and the Material 3 library Booklight already ships (its class files were read, not guessed).
No device was touched and nothing in the repo was changed. The mock-ups are in `booklight-window.html`, beside this file (made by `tools/design-window/build_mock.py`).
Everything is in dp unless it says otherwise. Sources are numbered at the end as [S1] and so on.*

## 1. The verdict

The window was built from the panel's parts, and the panel is not a desktop window. Five things are wrong today.

1. **The navigation floats.** The column starts about 47 dp in from the window's edge at the default size
   (`frame-default-commands.png`) and about 440 dp in when the window is 1840 wide (`frame-wide-commands.png`),
   because column and page are centred as one group. Material: the rail "should be placed on the leading edge of the
   window", "outside any panes" [S5]. Google's quality checklist asks for "leading-edge navigation rails" [S11].
2. **A narrow window breaks.** At 517 dp the Look page's text is one letter per line and the column is a strip of
   bare icons (`frame-narrow-look.png`). Material: under 600 dp use a navigation bar, never a rail, with one pane and
   16 dp margins [S3, S5]. The checklist's first tier is that the app survives a window being resized [S10].
3. **A wide window wastes itself.** Six rows of Look sit in the middle of a 1840 × 1053 window and the rest is empty
   (`frame-wide-look.png`). Material: "additional space doesn't just mean making the same thing bigger"; two panes are
   recommended from 840 dp up [S1, S2]. Android's desktop guide: "set a max width on content" [S13].
4. **It does not use the system's own parts.** The caption bar is a lavender band over a page of another colour; the
   open section is a pale pane with a heavy dark ring; switches, choices and rows are hand-drawn
   (`default-look.png`, `default-results.png`). Material 3 Expressive has a part for each of them (the rail's filled
   indicator, segmented lists, connected button groups) [S5, S6, S7], and the caption bar can be drawn through [S14].
5. **Seven sections for five jobs, and words where a picture would do.** Access has three rows and About two
   (`default-access.png`, `default-about.png`); Commands and Yours are two places for one thing; Look explains glass
   and shadow in three-line paragraphs without showing either. Android's settings guidance: group related settings,
   keep supporting text short, do not repeat the section's title [S9].

What is good and stays: the page title on the centre line of the first navigation item, the 720 dp measure (the
panel's own width), the keyboard model (arrows inside, Tab between), the live demo, and every string being plain.

## 2. The guidelines that matter

| # | Rule | Source |
| --- | --- | --- |
| 1 | Width classes: compact under 600, medium 600 to 839, expanded 840 to 1199, large 1200 to 1599, extra-large from 1600. Height: compact under 480, medium 480 to 899, expanded from 900. | [S1], [S15] |
| 2 | Navigation by class: compact, a navigation bar or a modal expanded rail; medium, a rail; expanded and up, a rail, collapsed or expanded ("modal or standard expanded navigation rail"). | [S1], [S2], [S3], [S5] |
| 3 | The rail runs along the leading edge of the window, outside every pane, and is never hidden when collapsed. It holds 3 to 7 destinations. | [S5] |
| 4 | Material 3 Expressive replaced the old rail and the drawer with two rails that turn into each other: collapsed (96 dp) and expanded (220 to 360 dp). The baseline rail "is no longer recommended". | [S4], [S5], widths from the library's tokens [S8] |
| 5 | Panes: one in compact and medium, two recommended from expanded up, three at most. A fixed pane is 360 dp (expanded) or 412 dp (large). The spacer between panes is 24 dp. | [S2], [S3] |
| 6 | Margins: 16 dp in compact, 24 dp from medium up. | [S2], [S3] |
| 7 | Line length: 40 to 60 characters is ideal; large screens can take up to 120. Adjust margins, do not stretch. | [S1], [S6] |
| 8 | Desktop: set a maximum width on content and components; raise density a little; give components exact click targets; type a step or two up. | [S13] |
| 9 | Settings: group related settings with containment and headings; 15 or more go on a subscreen; add search for deep hierarchies; list-detail adapts settings to large screens; supporting text is short and neutral. | [S9] |
| 10 | Lists in Material 3 Expressive: standard or segmented; "use gaps for contained lists", dividers only for uncontained ones; switches go at the trailing end. | [S6] |
| 11 | Connected button groups "replace the segmented button", which "is no longer recommended". | [S7] |
| 12 | Keyboard: Tab cycles through all interactive elements in reading order; arrows move directionally; set the first focus on something useful; Escape is a local cancel; focus styles are "distinct and consistent"; publish shortcuts in the Keyboard Shortcuts Helper. | [S12], [S11] |
| 13 | Pointer: hover states, right-click context menus, a visible scrollbar, tooltips, pointer targets that match what is drawn (they may be smaller than 48 dp). | [S11], [S13] |
| 14 | Windows: every desktop window has a header bar; an app may make it transparent and draw behind it, and must leave the system's controls alone. State survives resize, minimise and maximise. | [S14], [S10] |
| 15 | Googlebooks: "set layout max widths", "define explicit click targets", "right click context menus and hover states", a styled caption bar; Play gives an "optimized for desktop" badge. There is no numeric spec for Googlebooks. | [S16], [S17] |

What desktop settings windows share (Android Settings, ChromeOS, macOS, Windows 11, Raycast, Alfred) [S18]: a list of
sections that stays put on the leading side, with the detail beside it; five to eleven sections with About last; a
detail column with a capped width (680 px in ChromeOS, about 1000 in Windows); grouped rows, label left and control
right; search over the settings; one pane below a threshold (720 dp in Android Settings).

## 3. The new structure

Five sections, in the order a person meets them. No groups in the rail: five items are one group.

| Section | What the person comes to do | What the page holds | What moved |
| --- | --- | --- | --- |
| **Start** | Set it up, see what it is | The live demo. **Open Booklight**: whether your key works, "Open Keyboard shortcuts", the assistant key. **Tips**: show tips, show them again. | The assistant key comes from Access: it is a way to open Booklight, so it belongs beside the key. The key caps for the two free shortcuts show only while no key works. |
| **Commands** | See what it can do, make your own | A switch **Built in · Yours**, a search field, one list in groups. Built in: the thirty rows of today's Commands. Yours: links, snippets, recipes, prompts, and one **New link** button whose arrow offers the other three. | Yours stops being a section. The four "Add…" rows become one button. |
| **Look** | Change how it looks | A preview of the panel. Theme, Colours. **Panel**: Glass, Shadow, Dim the desktop, Opening. | The explanations shrink to one line each; the preview does the explaining. |
| **Results** | Choose what the list shows | **Search**: engine (a menu: five names are too many for buttons), suggestions. **Show in the list**: settings pages, sums, Ask Gemini, keyboard shortcuts. **Other apps**: the switch for all, then each app. | Nothing moves; three headings are added. |
| **Privacy** | Check what it may access and keeps | **What Booklight may use**: notes folder, brightness. **What Booklight keeps**: the two sentences of today's About, then "Forget everything" last, in the error colour. The privacy policy. One closing line: version, "A personal hobby project by Alexander Kuscher. Not affiliated with Google." | Access and About become one page. |

German names: Start, Befehle, Aussehen, Ergebnisse, Datenschutz. The panel's "All commands" still opens Commands;
"Edit…" opens Commands on Yours with that item open. The window remembers the section it was left on; a first run
opens on Start.

## 4. The layout at each window size

`W` is the window's width. The caption bar is 40 dp (measured: `device-findings.md`).

| Class | W | Navigation | Content |
| --- | --- | --- | --- |
| **Compact** | under 600 | Navigation bar at the bottom, 64 dp, five items, name under mark | One pane. Margins 16. Column = W − 32. A choice stands under its text; an example under its row. |
| **Medium** | 600 to 839 | Collapsed rail on the leading edge, 96 dp, name under mark | One pane. Margins 24. Column = W − 96 − 48 (456 to 695). |
| **Expanded** | 840 to 1199 | Expanded rail on the leading edge, 220 dp, name beside mark | One pane. The column starts 24 from the rail and is min(720, W − 268) wide. Spare room stays at the trailing side. |
| **Large, extra-large** | from 1200 | The same expanded rail | The same column in the same place. From W = 1332 a second pane, 24 from the column, takes what is left: 320 to 560 wide, 24 from the window's edge. |

- **Why the column is aligned to the start and not centred.** A page's title is then in the same place in every
  section and at every width, and resizing a window moves nothing sideways. (Windows 11's Settings does the same, from
  memory; ChromeOS centres a 680 px card.)
- **Why 1332 and not 1200.** 220 + 24 + 720 + 24 + 320 + 24 = 1332. The second pane arrives when there is room for
  it beside a full column, so the column never shrinks to make room. Material allows one or two panes in large windows.
- **What the second pane holds.** Look: the preview, in view while the settings scroll (a supporting pane). Start:
  the live demo. Commands: the editor of the open link, snippet, recipe or prompt, with its row shown selected
  (list-detail); for a built-in command, the preview with its example typed. Results and Privacy: nothing.
  Below 1332 the preview is at the top of the page and an editor opens under its row, as today.
- **Height.** A window lower than 480 keeps the rail (a bar would take a seventh of it). The rail does not scroll;
  five expanded items need 296 dp, five collapsed ones 336 dp.
- **Caption bar.** Transparent (`APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND`, with `APPEARANCE_LIGHT_CAPTION_BARS`
  following the theme) so the window is one surface from its top edge. Booklight draws nothing of its own in it. The
  rail and the pane start under `WindowInsets.captionBar`. The system keeps drawing its chip and its three controls.
- **Minimum size.** `<layout android:minWidth="400dp" android:minHeight="480dp"/>` on `MainActivity`.
- **What it opens at.** The system's default: 1053 × 889 on the Lenovo, which is Expanded. Do not set a size. (The
  HP's default was not measured; at the same share of its 1707 × 1067 dp screen it would be about 936 × 790, also
  Expanded.) Whether the system restores a window's last bounds is to be checked.

Booklight's two devices: the Lenovo is 1920 × 1200 dp, the HP 1707 × 1067 dp. Half a screen is 960 (Expanded) on the
Lenovo and 853 (Expanded, just) on the HP; a third is 640 (Medium) on the Lenovo and 569 (Compact) on the HP.

## 5. The look

An ordinary Material 3 Expressive window. The theme is already `MaterialExpressiveTheme` with Sans Flex.

| Part | Component (all in `material3` 1.5.0-alpha29) | Values |
| --- | --- | --- |
| Navigation | `WideNavigationRail` with `WideNavigationRailItem`; `ShortNavigationBar` with `ShortNavigationBarItem` in compact | Collapsed 96, expanded 220. Item 56 high (expanded), 64 (collapsed and bar). Indicator 56 high beside the name, 56 × 32 around the mark. No container fill: the rail stands on the window's ground. |
| A group of settings | `SegmentedListItem` with `ListItemDefaults.segmentedShapes(index, count)` | Rows 56 (one line), 72 (two), 88 (three). 2 dp between rows, 24 between groups. Outer corners 16, inner 4. Padding 16; mark 24; text from 56. A heading above: `titleSmall`, 16 in. |
| A choice of 2 to 4 | `ButtonGroup` (connected) of `ToggleButton` with `ButtonGroupDefaults.connected…ButtonShapes()` | 40 high, 2 dp apart; the chosen one is fully round and filled, the others have 8 dp inner corners. Beside the text when the row is 560 or wider, else under it. |
| A choice of 5 or more | A button that opens a `DropdownMenu` | The search engine. |
| On or off | `Switch` | 52 × 32. The whole row flips it. |
| The command list | `SegmentedListItem` in groups | The example at the trailing end in the fixed-width cut; the Enter mark appears on the focused row, in room that is always kept. |
| New | `SplitButtonLayout` with `SplitButtonDefaults.TonalLeadingButton` and `TonalTrailingButton` | "New link", and an arrow for snippet, recipe, prompt. |
| Find | A rounded `TextField`, 40 high, 264 wide | In the title's row, ending on the column's edge. An icon button in compact. |
| The editors | `TextField` (rounded, tonal), `Button` for Save, `TextButton` for Cancel and for Delete (error colour, last, apart) | In the second pane: a container with 28 dp corners, 24 padding. Under a row: the same, 16 dp corners. |
| Preview and demo | `Stage` as it is, in a container with 28 dp corners | 720 × 212 at the top of Look; 240 on Start; the second pane's width when there is one. |

Not used: floating toolbars and FABs (the window has no page-wide tool actions), `LoadingIndicator` and
`MaterialShapes` (still experimental, and decoration).

**Colour roles.** Ground `surfaceContainer`. Rows and containers `surfaceBright` (the one role that is lighter than
the ground in both themes; the library's default for segmented rows is `surface`, which is darker than the ground in
dark). Text `onSurface`; second text and marks `onSurfaceVariant`, replacing the panel's alphas, which glass needs
and a solid surface does not. The open section, the chosen option and a selected row `secondaryContainer` with
`onSecondaryContainer`: the panel's pill colour. A switch that is on, Save, the caret `primary`. Delete `error`.
Booklight's own two schemes in `ui/Theme.kt` need `surfaceBright` added (light `#F9F9FF`, dark about `#2F313A`).

**Type.** Title of a page: `headlineMedium`, 28/36, weight 600, the rounded cut (36/700 today). Lead: `bodyLarge`
16/24 in `onSurfaceVariant`, at most 600 wide. Group heading: `titleSmall` 14/20, sentence case (capitals today).
Row: `bodyLarge` 16/24; its second line `bodyMedium` 14/20. Rail: `labelLarge` 14/20 beside the mark, `labelMedium`
12/16 under it. Weight never changes with state.

**States.** Drawn in the mock-up's last figure.

| State | What shows |
| --- | --- |
| Hover | `onSurface` at 8 % over the row or button, a row's corners going to 12. Fades in. Every interactive thing has it. |
| Keyboard focus | A 2 dp ring in `secondary`, a row's corners going to 16. One ring for the window. |
| Pressed | `onSurface` at 10 %, corners 16, while the button or key is down. |
| Selected | `secondaryContainer`: the open section, the chosen option, the row whose editor is open. |
| Disabled | Content at 38 %; no hover, no focus. An app's row under a switched-off "Commands from your apps". |

**How it stays Booklight.** The same colour scheme, type, symbols and key caps as the panel; the panel's own
selection colour for what is open or chosen; the panel itself on Start and Look; and motion from `Motion.kt`. What
stays the panel's alone: glass, the white outline, and the one pill that follows pointer and keys. A window has
hover and focus as two separate things, as every desktop app does.

## 6. Keyboard and pointer

**Focus order.** Tab: the rail (one stop, the open section), then the page's own controls (Find, Built in · Yours,
New), then each row from the top, then the second pane. Shift + Tab goes back. F6 jumps between rail, page and
second pane. A row is one stop: its control is worked from the row.

**Keys.**

| Key | Does |
| --- | --- |
| ↑ ↓ | In the rail: the section, at once (as today). In the page: the row, across groups. |
| → or Enter in the rail | Into the page, on the row that was last focused there. |
| ← on a row | Steps a choice back; on a row without a choice, back to the rail (as today). |
| ← → on a choice or a switch | Steps it (as today). |
| Enter, Space | Flips a switch, opens a menu, runs the row. On a built-in command: types its example in the panel. |
| Home, End, Page Up, Page Down | First and last row; a page of rows. |
| Ctrl + 1 … 5 | The five sections. |
| Ctrl + F | Find a command, from any section. On Commands, typing letters does the same. |
| Ctrl + N | New link. |
| Delete on one of yours | Asks once more, then deletes (the panel's rule). |
| Escape | Closes a menu or an editor, clears Find. Never closes the window. |
| Ctrl + W | Closes the window. |

First focus is the page's first row, so ↓ and Enter work at once; on Commands opened from the panel it is Find.
The shortcuts are given to the system's Keyboard Shortcuts Helper (`onProvideKeyboardShortcuts`), where `k` in the
panel will find them too.

**Pointer.** Hover on everything interactive. The pointer is an arrow; an I-beam over text fields. Right-click on a
built-in command: Try it, Copy example. On one of yours: Edit, Duplicate, Delete. On an app under Other apps: Show,
Hide. The wheel and two fingers scroll the pane under the pointer; the rail never scrolls. A 4 dp scrollbar shows at
the pane's trailing edge while scrolling or hovered, and can be dragged. Icon-only buttons have a tooltip. Targets
are what is drawn: a row is one target; a control is at least 32 high.

## 7. Motion

Material's own parts keep the theme's scheme (`MotionScheme.expressive()`, set in `ui/Theme.kt`): spatial springs
0.8/380 (default), 0.6/800 (fast), 0.8/200 (slow); effects springs 1.0/1600, 1.0/3800, 1.0/800 [S8]. Everything
Booklight draws itself stays on `Motion.kt`, so the window moves like the panel.

| What happens | What moves | Spec |
| --- | --- | --- |
| A section changes | The rail's indicator travels to the new item, leading edge first. The old page fades where it stands (70 ms); the new page's blocks rise 12 dp, 22 ms apart, from the side the indicator came from. | `lead`, `trail`; `fade`; `place` (all as today) |
| Focus moves by key | One ring glides from row to row. | `lead`, `trail` |
| Hover | The state layer fades; the row's corners morph. | Material's fast effects and fast spatial |
| A choice changes | The chosen button rounds, its neighbours square off. On Look the preview changes in the same frame. | Material's button group |
| A switch flips | Thumb and track. | Material's switch |
| An editor opens | Under its row: the page makes room. In the second pane: the container fades in and rises 12 dp. | `place`; `fade` 140 |
| The window is resized | Widths follow the window frame by frame; nothing lags behind the drag. | none (as today) |
| A breakpoint is crossed | 600: bar and rail change places, cross-fading. 840: the rail widens from 96 to 220, each name moving from under its mark to beside it. 1332: the second pane fades in and slides 24 dp from the trailing edge. | Material's rail animation; `place` |
| Animations off | Everything cuts. | `Motion.on` |

## 8. How to build it

**Libraries.** `material3` stays at 1.5.0-alpha29: it is the newest (23 September 2026), and every part named above
is in it. Stable 1.4.0 has none of Material 3 Expressive and there is no 1.5.0 beta yet [S19]. In alpha29's class
files only `LoadingIndicator`, `MaterialShapes`, and parts of `Menu` and `Slider` still carry
`ExperimentalMaterial3ExpressiveApi`; `MaterialExpressiveTheme`, `MotionScheme`, `WideNavigationRail`,
`ShortNavigationBar`, `SegmentedListItem`, `ButtonGroup`, `ToggleButton`, `SplitButtonLayout`, the emphasised type
styles and the larger shapes do not [S8]. Tooltips and the search bar carry `ExperimentalMaterial3Api`. Staying on an
alpha means a rename can come with any update (alpha28 removed `ToggleButton` overloads, alpha29 changed `Slider`),
so the pin stays and updates are deliberate.

One new dependency: `androidx.compose.material3.adaptive:adaptive`, 1.3.0, stable, and the version the Compose BOM
2026.09.00 already names [S20]. It gives `currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)` and the
breakpoints of rule 1.

Not taken, and why. `NavigationSuiteScaffold` (`material3-adaptive-navigation-suite` 1.5.0-alpha29; it does have the
`WideNavigationRailCollapsed`, `WideNavigationRailExpanded` and `ShortNavigationBarCompact` types [S8]): by default it
shows a bar when the width *or the height* is compact and a rail otherwise [S21], and this plan overrides both of
those choices, needs the rail's focus in its own hands, and wants the indicator to travel. The same Material parts
are used directly, in about eighty lines. `ListDetailPaneScaffold` and `SupportingPaneScaffold` (`adaptive-layout`
1.3.0): they are made for a list that gives way to its detail in a small window, with back navigation; here the
editor opens under its row instead, so a `Row` of two boxes does it.

**In `window/`.**

| File | Replaced | Kept |
| --- | --- | --- |
| `MainActivity.kt` | The centred `Row` and its width rule; the page title and group label styles; `Choice` and `Toggle` are rebuilt on Material's controls with the same parameters. The Access and About pages merge. | Intents and `take()`, the per-section scroll states, `Rise`, the pages' content and every string key, the key handler's rules. |
| `Nav.kt` | `NavColumn` becomes the rail and the bar. `Part` has five entries. | The travelling indicator (`upper`, `lower` on `lead` and `trail`), redrawn as Material's. |
| `Page.kt` | `PageRow` wraps `SegmentedListItem`; `FlatSwitch` gives way to `Switch`; the pill goes. | The `Page` registry (it now places the focus ring and keeps the arrow order), `Cap`, `Keys`. |
| `Commands.kt` | `Input` and `Button` become Material's; the four Add rows become the split button. | The editors' logic and rules, `Editor` opening under a row. |
| `Stage.kt` | Gets a size, and the user's glass and shadow. | The demo loop. |

**Order of work.** Each step can ship on its own.

| Step | What ships | Days |
| --- | --- | --- |
| 1. The frame | Size classes. The rail on the leading edge (96 or 220) and the bar under 600. The column aligned to the start. The transparent caption bar. The minimum size. A choice goes under its text in a narrow row, which ends the one-letter-per-line break. Ctrl + 1 … 7. Pages otherwise as they are. | 2 |
| 2. Five sections | Commands with Built in · Yours, Find and New. Privacy. The assistant key on Start. The window remembers its section. Strings in English and German; the panel's two ways in. | 1.5 |
| 3. Rows and controls | Segmented groups, switches, button groups, the menu, Material's fields and buttons in the editors. Hover, the focus ring, pressed. Right-click menus, tooltips, the scrollbar. Colour roles and type. Look's shorter lines. | 3 |
| 4. The second pane and the polish | The preview on Look (in-window blur at the user's glass level, and the shadow). The demo and the editor in the second pane from 1332. The motion table. The Keyboard Shortcuts Helper. A pass on both Googlebooks, new captures. | 2.5 |

Nine days in all. Another session is changing `window/` now, so step 1 starts from whatever it leaves.

## 9. Risks, and what a device has to show

**Risks.**
- An alpha library: pinned, but a later alpha can rename what this uses.
- The preview needs the glass drawn inside an ordinary window (a blur of the stage behind the panel). `Stage` today
  draws a solid panel. If it cannot be made true to the real panel, the preview misleads; then it shows theme and
  colours only and Look keeps a line of explanation.
- Rows in containers and separate hover and focus are a different habit from the panel's one travelling pill. It is
  the right habit for a window, and it is a matter of taste: see decision 3.
- Material's rows are 56 and 72 dp. Android's desktop guide asks for more density, not less; Material has no smaller
  row. The thirty commands become about 2500 dp of list, about a fifth more than today.
- German: "Tastenkürzel öffnen", "Datenschutz" under a mark in 96 dp. To be looked at, not assumed.

**To check on a device.**
1. The transparent caption bar: is the system's chip readable on `surfaceContainer` in both themes; does the light
   and dark appearance follow the app's theme and not the system's.
2. Whether the desktop honours `android:minWidth` and `minHeight`, and what its own minimum width is (the narrowest
   capture is 517).
3. The HP's default window size, and whether the system restores the last bounds.
4. Whether the rail's first item centre still meets the page title's centre under the real caption inset.
5. Keys in the rail: Material's items take focus themselves; arrows, Tab and Ctrl + digits must not fight them.
6. Whether Material's parts draw a focus ring of their own (then there must not be two), and whether they obey the
   system's "remove animations".
7. `surfaceBright` on `surfaceContainer` with a wallpaper's colours, dark and light: is the step visible but quiet.
8. Dragging the window across 600, 840 and 1332: no frame with both layouts, no jump of the title.
9. Material adds an invisible 48 dp target around small controls; with a pointer that causes misclicks [S13]. See
   whether it shows here and lower `LocalMinimumInteractiveComponentSize` if it does.

Not verified at the desk: Material's exact focus-ring numbers (the page gives none; 2 dp in `secondary` is this
plan's choice); whether Compose now has a scrollbar of its own (the plan draws one); whether
`WindowInsets.systemBars` includes the caption bar on these devices; Android Settings' search placement, and the
details of macOS, Raycast and Alfred beyond what [S18] quotes.

## 10. Decisions for Alex

| # | Decision | Recommended |
| --- | --- | --- |
| 1 | Five sections: Yours inside Commands; Access and About as Privacy. Or six, with Yours kept as a section. | Five. |
| 2 | The names Start, Commands, Look, Results, Privacy. | These. |
| 3 | Rows in Material's containers with hover and a focus ring, or the panel's bare rows and one travelling pill. | Containers. The ring glides, so the eye still follows one thing. |
| 4 | The rail's indicator travels between sections (Booklight's rule), or Material's stock one, which grows in place. | Travels. |
| 5 | From 840: the expanded rail, names beside marks, 220. Or Material's stricter reading: collapsed, 96, until 1200. | Expanded: at the default size it reads as a desktop sidebar. |
| 6 | Under 600: a navigation bar at the bottom (Material's first choice), or a menu button that opens the rail over the page. | The bar: every section stays one click away. |
| 7 | The second pane from 1332 (preview, demo, editor). | Yes, in step 4. |
| 8 | The caption bar: transparent with nothing of Booklight's in it, or with Find in its middle. | Nothing in it. |
| 9 | Look's explanations cut to one line each once the preview exists. | Yes. |

## 11. Sources

Material's site does not render for a plain fetch; its pages were read in a browser on 1 October 2026.

- [S1] Material 3, Breakpoints, overview (formerly "window size classes"): https://m3.material.io/foundations/layout/applying-layout/window-size-classes
- [S2] Material 3, Breakpoints, expanded; large and extra-large: https://m3.material.io/foundations/layout/breakpoints/expanded , https://m3.material.io/foundations/layout/breakpoints/large-extra-large
- [S3] Material 3, Breakpoints, compact; medium: https://m3.material.io/foundations/layout/breakpoints/compact , https://m3.material.io/foundations/layout/breakpoints/medium
- [S4] Material 3, Navigation rail, overview (the Expressive update, May 2025): https://m3.material.io/components/navigation-rail/overview
- [S5] Material 3, Navigation rail, guidelines and specs: https://m3.material.io/components/navigation-rail/guidelines , https://m3.material.io/components/navigation-rail/specs . Parts of layout: https://m3.material.io/foundations/layout/understanding-layout/parts-of-layout
- [S6] Material 3, Lists, overview (Expressive update, December 2025) and guidelines: https://m3.material.io/components/lists/overview , https://m3.material.io/components/lists/guidelines
- [S7] Material 3, Button groups: https://m3.material.io/components/button-groups/overview . States: https://m3.material.io/foundations/interaction/states/applying-states
- [S8] The library itself: `androidx.compose.material3:material3-android:1.5.0-alpha29` from Booklight's Gradle cache, and `material3-adaptive-navigation-suite-android:1.5.0-alpha29` and `adaptive-android:1.3.0` from https://dl.google.com/android/maven2/ , read with `javap`: class names, token values (rail 96 and 220 to 360; list 56, 72, 88, gap 2, corners 4, 12, 16; motion springs), and which functions carry an experimental marker.
- [S9] Android design, Settings: https://developer.android.com/design/ui/mobile/guides/patterns/settings
- [S10] Adaptive app quality guidelines, tier 3: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3 (the old large-screen URL redirects to https://developer.android.com/docs/quality-guidelines/adaptive-app-quality)
- [S11] Adaptive app quality guidelines, tier 2 and the desktop experience: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-2 , https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/experiences/desktop
- [S12] Android design, desktop, keyboard: https://developer.android.com/design/ui/desktop/guides/interaction/keyboard
- [S13] Android design, desktop, get started; pointer interactions; system bars: https://developer.android.com/design/ui/desktop/guides/foundations/get-started , https://developer.android.com/design/ui/desktop/guides/interaction/pointer-interactions , https://developer.android.com/design/ui/desktop/guides/system/system-bars
- [S14] Support desktop windowing (the header bar, transparent caption, multi-instance): https://developer.android.com/develop/ui/compose/layouts/adaptive/support-desktop-windowing . The `<layout>` element: https://developer.android.com/guide/topics/manifest/layout-element
- [S15] Use window size classes: https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes . Canonical layouts: https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts
- [S16] "Land your apps on Googlebook with adaptive development", Android Developers Blog, 22 September 2026: https://android-developers.googleblog.com/2026/09/adaptive-development-scale-app-googlebook.html
- [S17] Googlebook, for developers (a page of links, no numbers): https://developer.android.com/googlebook . https://developer.android.com/googlebooks and https://developer.android.com/desktop do not exist.
- [S18] Reference windows. Android Settings' two-pane split (ratio 0.3636, from 720 dp): https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/res/values/config.xml . ChromeOS' 680 px card: https://chromium.googlesource.com/chromium/src/+/refs/heads/main/ui/webui/resources/cr_elements/cr_shared_vars.css . Windows: https://learn.microsoft.com/en-us/windows/apps/design/app-settings/guidelines-for-app-settings . macOS: https://support.apple.com/guide/mac-help/change-system-settings-mh15217/mac . Raycast: https://manual.raycast.com/settings . Alfred: https://www.alfredapp.com/help/
- [S19] Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- [S20] Compose Material 3 Adaptive releases: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive . The BOM's pins: https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom
- [S21] Build adaptive navigation: https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation
- Not found: https://developer.android.com/develop/ui/compose/designsystems/material3-expressive (404); a Compose guide page for Material 3 Expressive was not located, so the library and the release notes stand in.
