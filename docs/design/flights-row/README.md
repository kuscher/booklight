# A richer flight row: a design (2 October 2026)

Alex: "I love flights so I am willing to give it more height to fit in a bit more good looking into like a line on
where the plane is for progress with a plane icon, delayed and on time badges. Make it pretty and visually
appealing but clean and fitting of the app."

A product manager, a designer and an engineer worked on it. **Nothing of it is built**: it waits for his review.

- `flights.html`: the page he was shown, with switches for the flight's phase, theme, backdrop and language
  (https://claude.ai/artifact/8X4PE6x7MDebcZ7oMBJFgK).
- `design.md`: the row's anatomy with every measure, each phase, the motion, the pin, the row without a key.
- `pm.md`: what the row must answer per phase, the badges, the extras.
- `eng.md`: what the service's replies support, what the row can be technically, the costs.

## Decided (Alex, 2 October 2026: "Go ahead, approved. When done ship v3.0!")

The design is approved as shown, with the five decisions at their recommended answers:
1. Colour in the badge only: green for on time and early, amber for late; the line, the plane and every word in the
   row's own ink. (This changes two rules of the design system: "a delay is said, not painted" and the selection
   being the only coloured surface.)
2. "Late" starts 15 minutes after the plan (the code said 5).
3. Cancelled without red: the word as the headline, the times struck, no plane.
4. The row is 136 dp high in every state.
5. The aircraft's type in small words once it has left; no altitude, no speed.

`design.md` is the specification.
