# Data safety: the facts

Written the way the app says it, for the Play data-safety answers and the privacy page.

## The short version
Booklight keeps what it learns on the device and sends nothing anywhere, with one exception that is
**off until the user turns it on**: search suggestions. With suggestions on, the text being typed is sent
to the chosen search engine to get suggested searches.

## What is stored on the device
- `files/history.json`: for results the user picked, the result's id (for an app: its package and
  activity name), the text that was typed when it was picked, a count and a time. Used to put the usual
  choice first. Cleared by Settings › What Booklight keeps › Forget everything, and by uninstalling.
- `files/settings.json`: the chosen search engine, whether suggestions are on, the keyword searches,
  which result kinds show, which first-run cards were seen.
- Both are included in the user's own Android backup and device-to-device transfer. Booklight has no
  server and no account.

## What leaves the device, and when
**Search suggestions (off by default).**
- Turned on only by the user: the first-run card under the search field ("Search suggestions are off.
  Turn them on and what you type is sent to Google to suggest searches. Sums and web addresses are not
  sent." with **Turn on** / **Not now**), or Settings › Web search › Search suggestions. Turned off in
  the same setting at any time.
- When on: after a 140 ms pause in typing, if the text is 2 to 80 characters and is not a sum, not a web
  address and not a keyword search, Booklight makes one HTTPS GET request with the typed text as a query
  parameter to the chosen engine's suggestion address:

  | Engine | Address |
  | --- | --- |
  | Google (default) | `https://suggestqueries.google.com/complete/search?client=firefox&ie=utf-8&oe=utf-8&q=<text>` |
  | DuckDuckGo | `https://duckduckgo.com/ac/?type=list&q=<text>` |
  | Bing | `https://api.bing.com/osjson.aspx?query=<text>` |
  | Brave Search | `https://search.brave.com/api/suggest?q=<text>` |
  | Ecosia | `https://ac.ecosia.org/autocomplete?type=list&q=<text>` |

- The request carries the typed text, an `Accept: application/json` header and Android's default
  `User-Agent`. No cookies, no account, no advertising ID, no device identifier, no location. Like any
  internet request it reveals the device's IP address to that search engine.
- The reply (a list of suggested searches) is shown as rows and kept in memory for the session
  (64 entries at most). It is not written to storage.
- Booklight's developer receives nothing: there is no Booklight server.

**Choosing a web row.** "Search Google for …", a keyword search, a suggestion or a typed address opens
that address in the user's browser. That is the user sending it, in their browser, not Booklight in the
background.

**Nothing else.** No analytics, no crash reporting, no ads, no third-party SDKs.

## For the form
- Data collected by the developer: none.
- Data shared with third parties: with suggestions on, the text typed into the search field (Play's
  "App activity › In-app search history") is sent to the search engine the user chose, to provide the
  suggestions feature. Optional (off unless the user turns it on); not used for advertising or
  analytics by Booklight; transferred over HTTPS; not stored by Booklight.
- Data encrypted in transit: yes. Deletion request: nothing is held by the developer.
