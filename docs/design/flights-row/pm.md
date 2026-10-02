# The flight row: what it must say (PM)

Two facts from the saved replies shape this. **The service never says "boarding" or "delayed"**: it has four states (scheduled, en-route, landed, cancelled) and minutes. **Outside the air there is little**: `eta`, position and aircraft are absent on the ground; the belt was null in every saved reply; of three flights in the air one had no position and no aircraft, but all three had `percent` and `eta`.

## 1. Who looks a flight up from a laptop (most often first)

| Moment | The one thing needed first |
| --- | --- |
| 1. Someone to pick up, or someone arriving | How many minutes until it lands |
| 2. Your own flight later today, before leaving for the airport | When it leaves as things stand now, and whether that is the plan |
| 3. Watching a delay through the day (what Pin is for) | The new time, and how far it is from the plan |
| 4. Curiosity: a friend's flight, "where is it now" | How far along it is |

At the gate people use the phone. The row is built for 1 and 2; the line gives 4 for free.

## 2. What the row answers, per phase

Line one stays (number · airline · city → city). The two slots become the line's two ends. Under it: one **headline** in full ink, the **badge**, then **where to go** a step lighter.

| Phase | Headline | Supporting | Noise here |
| --- | --- | --- | --- |
| **Scheduled**, more than 3 h off | "Leaves Fri 10:25" (the day only when not today) | departure terminal, gate | arrival gate, a countdown in hours |
| **Soon**, within 3 h of leaving (the clock against the expected time) | "Leaves in 42 min" | "Gate Z58 · Terminal 1" | the arrival, but for its time |
| **In the air** | "Lands in 34 min" | arrival terminal, gate if known; aircraft type | departure gate, time flown, altitude, speed |
| **Landed** (under 3 h ago; after that the next flight is shown) | "Landed 12 min ago" | arrival terminal, belt if known, else gate | the departure end, aircraft |
| **Cancelled** | "Cancelled" | both times struck, as today | terminal, gate, countdown |
| **Diverted** | "Diverted" | arrival time struck | where to go (nobody knows) |
| **Unknown**: waiting, no answer, a timetable's flight | "Looking it up" · the reason · "From the timetable" | the timetable's times | anything that looks live |

A missing field's words are simply not there: gate null says the terminal alone; both null says nothing; never "Gate –", never a held place. No expected time: the countdown runs to the planned time, as the pin does today. One height in every phase, from the first frame.

## 3. The progress line

- **Its two ends** are the airports: three letters and the time there (actual, else expected, else planned), with the day when it is not today. They replace the LEAVES and LANDS slots.
- **The plane's place** is the share of the flight behind it. It is not a place on a map.
- **Scheduled and soon**: the plane rests at the left end, the line is empty. Nothing creeps towards departure; the countdown says that.
- **In the air**: the plane stands at `percent`, the flown part drawn stronger. It travels there once when the answer lands and never idles or pulses. `percent` null: the same share worked out from the times; those missing too: no plane, and "In the air" in words.
- **Landed**: the whole line drawn, the plane at rest at the right end.
- **Cancelled, diverted, a timetable's flight past its time, waiting, no answer**: the line stays, a step lighter, with no plane. A plane on the line is a claim that we know where it is.
- **The number is time left**, never time flown. Line and number come from the same minutes, so they cannot disagree.

## 4. Badges

The line shows the phase; the badge says whether it runs to plan. One badge: the first that applies, in this order.

| Badge (English · German) | When | Colour |
| --- | --- | --- |
| Cancelled · Gestrichen | status cancelled | error red |
| Diverted · Umgeleitet | status diverted | error red |
| Landed · Gelandet | status landed | neutral |
| Delayed 25 min · 25 Min. verspätet | 15 min or more after the plan | amber |
| 24 min early · 24 Min. früher | landing expected 15 min or more before the plan (in the air only) | green |
| On time · Pünktlich | the service sent a new time, within 14 min of the plan | green |
| In the air · In der Luft | en-route, no expected landing known | neutral |
| Planned · Geplant | only the plan is known | neutral |
| From the timetable · Laut Flugplan | a timetable's flight | neutral |

- **Which delay**: the departure's until it has left, the landing's after (as the code does). From an hour: "Delayed 1 h 15 min · 1 Std. 15 Min. verspätet".
- **15 minutes, not today's 5**: it is what airlines call on time, the service reports every minute (2, 3 and 7 were seen), and an amber "Delayed 6 min" is alarm for nothing. The exact time is at the line's end, so nothing is hidden.
- **"On time" is a claim**, made only when the service sent an estimate. The plan alone is "Planned".
- **No "Boarding"**: the service never says it, and a badge from the clock would be a guess. The countdown and the gate do that job.
- **Landed late** says "Landed" in the badge and "25 min late · 25 Min. verspätet" in plain words after it.

## 5. What is worth the height

Ranked: where to go at the end that matters now; the day when not today; aircraft type; codeshare; the other end's terminal and gate; altitude and speed; local time difference; "asked 2 min ago".

**Shown (three):**
1. **Where to go, one end only**: departure terminal and gate until it leaves; arrival terminal and belt (else gate) after. The belt belongs here, not on a line of its own.
2. **The day beside a time** when it is not the user's today. A wrong day is the costliest mistake a flight row can make.
3. **Aircraft type** when known ("Boeing 747-8"): short, never stale, and for an owner who loves flights. No held place; it is usually absent on the ground.

**Left to the flight's page:** altitude and speed (they look live and are two minutes old in the panel, thirty in the pin); both ends' gates at once; the time difference (each time is already local, and the countdown removes the sum); codeshare; registration and age. "Asked at" stays in the footer.

## 6. The pinned flight

Yes to the line, no to a pill. The pin is glanced at, and the line is the glance. It keeps its 280 × 118 window and its big figure ("34 min", "Landed"); the line runs thin under the figure with the plane on it, and the badge shrinks to its word in the small line, in its colour ("LH 455 · 25 min late"). The pin is asked only every half hour, so between answers the plane moves by the clock together with the countdown, and both jump together when an answer comes. No labels at the ends, no aircraft. Before departure the plane rests at the left end; cancelled has no plane.

## 7. Without a key

It stays the ordinary 56 dp row: "LH 400 · Lufthansa", Enter opens the flight's page. No empty line, no grey badge, no ghost of the rich row: someone who never wants a key would see a broken promise on every lookup. The invitation has two places. In the row: the last action "Set up times · Zeiten einrichten" (as today), and while it is armed the footer says what a key buys: "With your own AirLabs key: times, gate and where it is · Mit eigenem AirLabs-Schlüssel: Zeiten, Gate und wo er gerade ist". In the window, where the key goes: the rich row drawn on the stage, in the air, so the promise is shown where it can be kept.

## 8. Decisions for the owner

1. **Two new colours, in the badge only** (green: on time or early; amber: late; cancelled and diverted take the existing error red)? It overrides §14's "a delay is said, not painted". **Recommended: yes**; the line and the plane stay in ink, so colour never spreads.
2. **Late and early from 15 minutes, not 5?** **Recommended: 15.**
3. **No "Boarding" badge**; "Leaves in 42 min" from three hours out instead. **Recommended: yes.**
4. **How much more height?** **Recommended: at most one ordinary row more (92 → up to 148 dp), the same in every phase**; less if the line fits in less.
5. **Aircraft type in, altitude and speed out?** **Recommended: yes.** If he wants the live figures for the love of it: in the panel's row only, in the air only, never in the pin.
