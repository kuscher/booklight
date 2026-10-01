# Privacy

Booklight has no account and no ads, and its developer receives nothing from it. Since 2.0 it contains one
library of Google's, which reports how it is used to Google (see "Answers on this device").

**On your device.** Booklight remembers what you pick for the text you type, so your usual choice comes
first, and it keeps your settings, your links, snippets and recipes, and the emoji you picked lately.
These are files in the app's own storage. The Booklight window › What Booklight keeps › Forget
everything clears what it has learned.

**Notes** you jot down go into files (Notes.md, Todo.md, a file per day if you ask for one) in a folder
you choose once. Booklight reads and writes in that folder only, and reads it only when you search your
notes or your tasks.

**Other apps' commands.** Booklight reads what your installed apps declare they offer (their shortcuts) from
their own packages. It starts one only when you press Enter on its row, and those apps never see what you type.

**Search suggestions are off until you turn them on.** If you turn them on, the text you type is sent
over HTTPS to the search engine you chose (Google by default; DuckDuckGo, Bing, Brave Search or Ecosia
if you change it) to get suggested searches. Sums, web addresses, and anything typed after a keyword
(a note, a mail, a search of one site) are not sent. No cookies or identifiers are added; like any
internet request it shows the search engine your IP address. Turn suggestions off again in the
Booklight window › Web search.

**Handing things to other apps.** A web row opens in your browser. A mail, an event, a note for Keep, a
timer or a question for Gemini is handed to the app that opens: your mail app, your calendar, the Clock,
Gemini. Booklight sends none of it anywhere itself, and a mail or a Gemini question is only filled in:
you send it there, or you don't.

**Answers on this device.** A prompt (`fix …`, `sum`, one of your own) is answered by the system's own
model, Gemini Nano, which Android keeps and runs on the device; Booklight reaches it through Google's
ML Kit library. Your text and the answer stay on the device: they are not sent to Booklight's developer,
and the system's service does not keep them. That library, like all of ML Kit, **reports how it is used
to Google**: the device model and Android version, Booklight's name and version, an identifier made for
this installation, which feature ran, how long it took, how large the input and the answer were, and
error codes. Not the text. It sends this in the background when the network allows. Booklight cannot
switch that off in this version; if you never use a prompt, the model is never asked. Where the device has
no such model, a prompt opens the Gemini app with your text in its prompt instead, and you send it there
or you don't.

**The pinned window** shows what you pinned until you close it; a pinned line is kept nowhere else.

**The clipboard** is read only when you ask for it (`clip`, a prompt with nothing typed, or a link of yours
that uses `{clipboard}`). Something a password manager marked as private is not read.
**Text from another app** (its selection menu, its share sheet) is shown in the panel and forgotten when
the panel closes.

**Permissions.** Internet, for suggestions and for the library's usage reports. Asking Android to
uninstall an app (Android asks you before it does). Setting alarms and timers in the Clock app. Changing
the brightness, which does nothing until you switch on "Modify system settings" for Booklight yourself.
Since 2.0, from Google's library: connecting to the system's on-device AI service, and seeing whether the
network is up. None of these shows a prompt. No accessibility service, no notification access, no access
to your contacts or location. Nothing of Booklight's own runs in the background; the library's usage
report is a short job the system runs.

The exact addresses and details are in [store-submission/forms/data-safety.md](store-submission/forms/data-safety.md).
Questions: kuscher.projects@gmail.com.
