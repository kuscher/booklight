# Commands from your app in Booklight

Booklight shows what other apps offer as rows of its list: type `new event`, press Enter, and the Calendar's
editor opens. There are two ways for an app to be there. Neither needs any code that talks to Booklight, neither
starts your app before the user presses Enter, and your app never sees what is typed.

## 1. Nothing to do: your manifest shortcuts

If your launcher activity declares [static shortcuts](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#static)
(`android.app.shortcuts`), each of them is a row: its short label is the title, your app's icon and name are
shown with it.

Booklight is not the home app, so it starts the shortcut's intent itself. It can only do that when the intent
leads to an activity that

- is in your own package,
- is `android:exported="true"`,
- asks for no permission, and
- is enabled.

Shortcuts whose target is not exported (Chrome's "New tab", Google Docs' "New Doc") only work from the home screen
and are left out. Dynamic and pinned shortcuts are the home app's to read and are not shown.

## 2. One file: commands and keywords

Add to your `<application>`:

```xml
<meta-data android:name="io.github.kuscher.booklight.commands"
           android:resource="@xml/booklight_commands" />
```

and `res/xml/booklight_commands.xml`:

```xml
<booklight version="1">

  <!-- A row. -->
  <command id="mini" title="@string/mini" keywords="@string/mini_words">
    <open action="com.example.MINI" class=".ui.MiniActivity" />
  </command>

  <!-- A keyword with fixed choices: "shot", then area / window / screen. -->
  <scope id="shot" keywords="shot,screenshot" name="@string/capture" hint="@string/capture_hint">
    <command id="area" title="@string/area">
      <open class=".CaptureActivity"><extra name="source" value="area" /></open>
    </command>
    <command id="window" title="@string/window">
      <open class=".CaptureActivity"><extra name="source" value="window" /></open>
    </command>
  </scope>

  <!-- A keyword that takes text: "pdf merge" opens pdftoolbox://tool?q=merge. -->
  <scope id="pdf" keywords="pdf" name="@string/tools" hint="@string/tool_hint">
    <open class=".MainActivity" data="pdftoolbox://tool?q={argument}" />
  </scope>

</booklight>
```

| Element | Attributes | Meaning |
| --- | --- | --- |
| `command` | `id`, `title`, `keywords` (optional) | A row. Inside a `scope`: one of its choices |
| `scope` | `id`, `keywords`, `name`, `hint` (optional) | A keyword. Holds `command`s (fixed choices) or one `open` (takes text) |
| `open` | `action`, `class`, `data` (each optional; at least `class` or `action`) | What Enter starts. `class` may start with a dot |
| `extra` | `name`, `value` | A string extra of that intent |

- `title`, `name`, `hint` and `keywords` may be string references, so they follow your app's languages.
  `keywords` is a comma-separated list.
- `{argument}` in `data` or in an extra's `value` is the text typed after the keyword. In `data` it is
  percent-encoded; in an extra it is as typed. It is only allowed in a `scope`'s own `open`.
- The same four rules as above apply to what `open` leads to: your package, exported, no permission, enabled.
  A command that fails them is left out.
- Unknown elements and attributes are skipped, so a file written for a later version still loads as far as it is
  understood.

### What Booklight does with it

- A command is found by its title, by "your app's name + title", and by its keywords, which count a little less
  than a title. Under your app's own row it lists up to three of your commands when your app leads the list.
- A keyword of yours is entered with Tab or Enter on its row. Once the user has done that, the keyword and a
  Space enters it, as with Booklight's own keywords.
- A keyword that Booklight or the user already uses is not yours to take; your other keywords still work.
- Limits per app: 40 commands, 8 keywords, 12 choices in a keyword, 8 keywords per command (24 characters each),
  60 characters for a title, 8 extras.
- The user can turn off all other apps' commands, or yours alone, in the Booklight window under Results.

### What it cannot do, on purpose

No results while typing, no callbacks, no access to Booklight's permissions, no intents to other packages, no
grant flags, no clip data. An extension that answers while the user types (a calculator, a search in your app's
data) is planned as a separate, opt-in layer and is not part of this version.

### Trying it

The debug build of Booklight declares a file for itself (`app/src/debug/res/xml/booklight_commands.xml`).
`./bl debug find "your title"` prints the rows for a text without opening the panel.
