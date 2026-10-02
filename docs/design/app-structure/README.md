# One structure for apps: a proposal (2 October 2026)

Alex: "Spotify Search and Play for example should be the second and third option in the app line so I can tab tab
tab and enter and type the name instead of being so separate. The other WM options etc are not as crucial. Same
for Netflix. Can you review the logic and information architecture ... and have team propose a structure to me
which simplifies some of these things around the app more."

A product manager and a UX designer each wrote a paper; the page puts their joint proposal in front of him with
drawn rows, the key counts and five decisions. It is built (`BUILD.md`: what was built, where it differs from
`ux.md`, what to check on a device), and has not been run on a device yet.

- `rows.html`: the page he was shown (https://claude.ai/artifact/A69qVEK8jVnms5SCKpefgs).
- `ux.md`: the interaction (the model, the row, what follows Enter on Search and Play, the other ways in, one rule
  for marks, the edge cases, and what has to change in the code's rules).
- `pm.md`: the jobs, the diagnosis, the principle, the cuts.
- `BUILD.md`: the build.

In one sentence: type the app, Tab to what you want, Enter; if it takes words, type them and press Enter again.
An app's row is Open · Search · Play · Window · More, and an app only shows what it has.

## Decided (Alex, 2 October 2026: "I agree with the decisions for one structure for apps and your call. Go ahead and have this built.")

The five decisions, each at its recommended answer. `ux.md` is the specification; where `pm.md` differs, `ux.md` holds.

1. **The end of the row**: one place called Window that opens New window, Maximise, Left half, Right half and the
   ten finer places; and the arrow (More) for App info, the app's four pages in Settings, Don't suggest (only among
   your usual) and Uninstall.
2. **A sentence that starts with an app's name**: the web leads until that app's search row has been picked once;
   from then on the app leads for words after its name; picking the web row twice running puts the web back.
3. **Play only where it really plays**: an app's row has Play if the app answers Android's play-from-search
   request and is not known to only search. Spotify only searches by itself, so its row has Play once the user's
   Spotify key is in (the lookup then finds the link that plays); without the key its row has Search.
4. **`play` and `yt`** (and `store`, `maps`, `drive` where the app is installed) are short ways into the same chip
   and the same row as the Tab way.
5. **The app's pages in Settings**: all four are lines behind the arrow, and their typed words stay.

Not taken from `pm.md`: `call`, `sms`, `wa`, `tg` stay as built (each exists only where an app answers); the editor
for the user's own app commands stays under Commands › Yours; no new typed words (`netflix search …`).
