# Booklight: a flight number in the panel ("LH455")

*Desk research, 1 October 2026 (the calls were made between 04:50 and 05:10 UTC on 2 October, from a Mac on
the US west coast). No device was touched, no code was changed, nothing was built.*

**The question.** The owner types a flight number (LH455, "lh 455", "UA 90") and wants a row that answers with
the next flight of that number: airline, route, scheduled and expected times, terminal or gate, status. Where
can that honestly come from, with no server of Booklight's own, no secret key in the APK, and nothing typed
leaving the device without the user's say-so?

**How it was done, and its limit.** The session's web-search budget was already used up, so nothing here comes
from a search engine. Every keyless source was called with `curl`; vendors' pages, terms and OpenAPI files were
opened directly; the Android source and the classifier's model file were downloaded and read. Several vendor
pages are drawn by JavaScript and gave no text; those claims are marked.

**How sure each claim is.** *called* = I made the request today and the reply is quoted. *docs* = read on the
vendor's own page or file today. *source* = read in the Android source today. *device* = already measured, in
`device-findings.md`. *memory* = what I know from before, not confirmed today. *not verified* = could not be
confirmed today.

**A correction to the brief.** LH455 is San Francisco to Frankfurt, not the other way round (LH454 is
Frankfurt to San Francisco). All four route sources below agree (called).

---

## The short version

There is no keyless source of scheduled or expected times for an arbitrary flight. What is free and keyless is
(a) the airline behind a designator, from a small table that can be bundled, (b) the usual route of a callsign
(San Francisco to Frankfurt for DLH455), from community tables that sometimes disagree with each other, and
(c) where the aircraft is right now, but only while a volunteer receiver on the ground can hear it: **LH455
itself was not visible on any keyless feed during the twenty minutes of this research**, while LH459 (San
Francisco to Munich, just departed) and BA64 (over Belgium) were. Times, terminal, gate, baggage belt and a real
status ("delayed 25 min", "landed") exist only behind an API key or on a web page. So the feature has two
honest shapes: **Enter opens the right page** (works for everyone, nothing leaves the device before Enter), and
**a full answer in the row with a key the user pastes in themselves** (AeroDataBox through RapidAPI gives about
200 lookups a month free; Raycast's flight extension works exactly this way). A keyless middle step (route and
"in the air now") is possible and cheap, and needs the same kind of switch as search suggestions.

### What each road gives

| Road | What the row could show | What leaves the device | Cost | What is not verified |
| --- | --- | --- | --- | --- |
| **No network** (a bundled airline table, about 25 to 30 KB) | "LH 455 · Lufthansa". Enter opens the flight's page (FlightAware, FlightStats, a Google search) or hands a question to Gemini | Nothing until Enter; then the browser or Gemini has the flight number, like any web row today | 0 | Whether Google's card and Flightradar24's page open as expected in a browser (bot walls stopped `curl`); what the system's own "Track" action opens on a Googlebook |
| **Keyless network** (route table, live aircraft feeds) | "San Francisco → Frankfurt" (the usual route), and while a receiver hears the aircraft: "in the air, 11,300 m, 970 km/h", with a rough time left worked out from the distance. No scheduled or expected times, no gate, no "landed", no "delayed" | The flight number and the device's IP address, to one or two volunteer-run services | 0 | Whether the operators allow a shipped app to call them (two ask to be contacted, one says personal use only); how often the route tables are wrong (they disagreed on 3 of 5 flights compared) |
| **The user's own key** (AeroDataBox; FlightAware AeroAPI as the second) | Everything asked for: airline, route, scheduled, revised and actual times in local time, terminal, gate, check-in desk, baggage belt, status, aircraft | The flight number, the user's key and the IP address, to the provider (and to RapidAPI in between) | 0 for about 200 lookups a month; then $8 a month | No key was at hand: no keyed endpoint was called. Field coverage per airport, and which flight "nearest date" picks, are from the docs only |
| **Hand over to the web or Gemini** | Nothing in the row; the page or the Gemini app shows it | The flight number, to the site the user opens | 0 | Whether the Gemini app answers a flight question with live data |

---

## 1. Keyless sources, called

Flights used: **LH455** (callsign DLH455) and, because LH455 was not to be seen, **BA64** (BAW64, Nairobi to
London, at 38,000 ft over Belgium), **LH459** (DLH459, San Francisco to Munich, climbing over Oregon) and
**LH403** (DLH403, Newark to Frankfurt, on approach). Latency is the median of five calls with the user agent
`Booklight`, from the US west coast (one call where marked); from Europe the European hosts will be faster.

### 1.1 What each one is

| Source | Address called | Key | Gives | Latency | Quirks | Terms for a redistributed open-source app |
| --- | --- | --- | --- | --- | --- | --- |
| **adsbdb** | `https://api.adsbdb.com/v0/callsign/DLH455` (also takes `LH455` and `lh455`) | none | Airline (name, ICAO, IATA, country, radio callsign), origin and destination airport (name, city, country, IATA, ICAO, coordinates). No times | 0.46 s | `access-control-allow-origin: *`. Maps IATA to ICAO itself. `LH0455` → 404; anything under four characters (`FR1`, `4Y1`) → 400 "invalid callsign". Unknown → 404 `{"response":"unknown callsign"}`. No rate-limit header seen; no limit stated in the part of the README read | Code is MIT. The route data "may not be copied, published, or incorporated into other databases without the explicit permission of David J Taylor" ([README][adsbdb]): call it, never bundle it |
| **adsb.lol route file** | `https://vrs-standing-data.adsb.lol/routes/DL/DLH455.json` (folder = first two letters of the callsign) | none | Callsign, airline code, the airports (name, IATA, ICAO, city, country, coordinates). No times | 0.04 s | A static file on GitHub Pages behind Cloudflare, `cache-control: max-age=600`, CORS `*`. Unknown → 404 with an HTML page. Wants the ICAO callsign. The whole table is one file: `routes.csv.gz`, 4.5 MB, 620,700 callsigns | **CC0** ([adsblol/vrs-standing-data][vrs-lol], "Updated hourly"; made from [vradarserver/standing-data][vrs], CC0). The only route data here that may be copied |
| **hexdb.io** | `https://hexdb.io/api/v1/route/iata/DLH455` (or `/route/icao/`; legacy `callsign-route-iata?callsign=`) | none | `{"flight":"DLH455","route":"SFO-FRA","updatetime":1747593022}`. The time is when the entry was last changed (18 May 2025 here; 2011 for BAW64) | 0.17 s | Cloudflare cache, `max-age=14400`, CORS `*`. Wants the ICAO callsign | No licence stated. "Please do not scrape the database… If users make an excessive number of requests in a short period of time, IP addresses may be blocked" ([hexdb.io][hexdb]). Jim Mason is credited for route data here and by adsbdb, yet the two answered differently for two of the callsigns below |
| **adsb.lol live** | `https://api.adsb.lol/v2/callsign/BAW64` | none today | The aircraft now: hex, registration, type, barometric and geometric altitude, ground speed, track, position, squawk, wind and temperature, how fresh the position is (`seen_pos`, under 0.4 s in every reply) | 0.51 s | **403 without a `User-Agent`** header; `Booklight` and Android's default both pass. An empty list when the aircraft is not heard. `cache-control: no-store` | Data and API under **ODbL 1.0** (attribution, share-alike on the database). "In the future, you will require an API key which you can get by feeding… If you want to use the API for production purposes, please contact me" ([API description][lol-api]) |
| **adsb.fi open data** | `https://opendata.adsb.fi/api/v2/callsign/BAW64` | none | The same record format, plus a type description | 0.21 s | 1 request a second; 4xx replies count towards a temporary IP block | "for personal, non-commercial use only… You must cite adsb.fi and include a link to our home page" ([README][adsbfi]) |
| **airplanes.live** | `https://api.airplanes.live/v2/callsign/DLH455` | none | **Not obtained.** 403 with `{"error": "Please contact us at contact@airplanes.live…"}` for every user agent tried | 0.03 s (three calls) | Its API guide is behind a Cloudflare check | Not verified |
| **OpenSky Network** | `https://opensky-network.org/api/states/all?icao24=4080bf`; `…/tracks/all?icao24=4080bf&time=0`; `…/routes?callsign=DLH455` | none for these | States: position, altitude, speed by transponder address. **No filter by callsign**: the whole list is 906 KB (6,956 aircraft). Tracks: the path since take-off, so the take-off time (BAW64: 20:54 UTC). Routes: `{"route":["KSFO","EDDF"],"updateTime":"2023-09-05…","operatorIata":"LH","flightNumber":455}` | 0.46 s (one call each) | 400 credits a day without an account, 4 per call (the header counted 396, then 392). `flights/arrival`, `flights/departure`, `flights/aircraft` → 403 "You cannot access historical flights". The docs say tracks need a login; the call answered without one. CORS only for its own site | **Not usable.** "Use of the REST API in any operational capacity — including integration into a live product, service, or automated system… requires a previous written agreement, even for non-profit…" ([terms][opensky-terms]). Accounts use OAuth2 client credentials only ([REST docs][opensky-rest]) |
| **Avinor** (Norway's airports) | `https://asrv.avinor.no/XmlFeed/v1.0?TimeFrom=1&TimeTo=4&airport=OSL&direction=D` | none | Per airport: `flight_id`, `schedule_time`, gate, check-in, a status code with its time | 0.71 s (one call) | XML, ISO-8859-1. The old `flydata.avinor.no` address answers 301 | Terms not read. Norwegian airports only: the one keyless source of scheduled times found, and no use for LH455 |
| FAA airport status | `https://nasstatus.faa.gov/api/airport-status-information` | none | Delay programmes and closures per US airport, XML. Nothing per flight | 0.14 s (one call) | | Not read |

Not called, on purpose: Flightradar24's and FlightAware's internal web endpoints, which exist and answer without
a key. Their terms forbid it.

### 1.2 The replies for LH455 and the others (trimmed)

adsbdb, `GET /v0/callsign/DLH455`, 200:

```json
{"response":{"flightroute":{"callsign":"DLH455","callsign_icao":"DLH455","callsign_iata":"LH455",
 "airline":{"name":"Lufthansa","icao":"DLH","iata":"LH","country":"Germany","country_iso":"DE","callsign":"LUFTHANSA"},
 "origin":{"iata_code":"SFO","icao_code":"KSFO","municipality":"San Francisco","name":"San Francisco International Airport","country_iso_name":"US", …},
 "destination":{"iata_code":"FRA","icao_code":"EDDF","municipality":"Frankfurt am Main","name":"Frankfurt am Main Airport","country_iso_name":"DE", …}}}}
```

adsb.lol route file, `GET /routes/DL/DLH455.json`, 200 (file dated 20 September 2026):

```json
{"callsign":"DLH455","number":"455","airline_code":"DLH","airport_codes":"KSFO-EDDF","_airport_codes_iata":"SFO-FRA",
 "_airports":[{"name":"San Francisco International Airport","icao":"KSFO","iata":"SFO","location":"San Francisco","countryiso2":"US", …},
              {"name":"Frankfurt-am-Main International Airport","icao":"EDDF","iata":"FRA","location":"Frankfurt-am-Main","countryiso2":"DE", …}]}
```

hexdb.io: `{"flight": "DLH455", "route": "SFO-FRA", "updatetime": 1747593022}`.
OpenSky routes: `{"callsign":"DLH455","route":["KSFO","EDDF"],"updateTime":"2023-09-05T22:53:37.000Z","operatorIata":"LH","flightNumber":455}`.

adsb.lol and adsb.fi live, `GET /v2/callsign/DLH455`, 200, at 04:52 and again at 05:09 UTC:

```json
{"ac":[],"msg":"No error","now":1790916727501,"total":0,"ctime":1790916727501,"ptime":0}
```

The same call for a flight a receiver could hear, adsb.fi `GET /api/v2/callsign/DLH459`, 200:

```json
{"ac":[{"hex":"3c670a","type":"adsb_icao","flight":"DLH459  ","r":"D-AIXJ","t":"A359","desc":"AIRBUS A-350-900",
 "alt_baro":37000,"alt_geom":38700,"gs":522.4,"track":37.22,"baro_rate":0,"squawk":"1747","emergency":"none",
 "lat":42.736397,"lon":-118.272932,"seen_pos":0.244,"seen":0.1, …}],"msg":"No error","now":1790917774001,"total":1}
```

OpenSky, `states/all?icao24=4080bf`: `["4080bf","BAW64   ","United Kingdom",1790916816,1790916816,4.6883,50.9208,11582.4,false,239.91,308.3,-0.33,null,11902.44,"3126",false,0]`.

### 1.3 What the calls showed

- **A flight can be in the air and absent.** DLH455 was in none of the three live feeds (adsb.lol and adsb.fi
  at 04:52 and 05:09 UTC, OpenSky's full list at 04:53). Why was not verified: by its usual schedule (memory) it was
  hours into its flight, over northern Canada or the Atlantic, where no ground receiver hears it; it may also
  not have flown that day. A keyless row can therefore say "in the air" when it sees the aircraft, and must say
  nothing when it does not: an empty reply means "not heard", not "landed" and not "not departed".
- **The route tables disagree.** Compared on five callsigns, adsbdb differed from the others on three:

  | Callsign | adsbdb | hexdb.io | adsb.lol file |
  | --- | --- | --- | --- |
  | DLH455 | SFO → FRA | SFO-FRA | SFO-FRA |
  | BAW64 | NBO → LHR | NBO-LHR | NBO-LHR |
  | DLH7LA | STR → MUC | DUS-MUC | DUS-MUC |
  | EZY68XU | PMI → LGW | LGW-CFU | LGW-CFU |
  | EZY8001 (as `U28001`) | BOD → BIO | not called | MXP-LGW |

  From memory: long-haul numbers are stable for years; short-haul and low-cost numbers are reassigned every
  season, and alphanumeric callsigns more often still. A row must call this "usual route", and the page behind Enter is the
  authority.
- **A rough "time left" is arithmetic, not data.** BAW64 was 204 nautical miles from Heathrow at 465 knots:
  about 26 minutes plus the approach. Good enough for "lands in about half an hour", wrong by tens of minutes
  near the airport or with a hold, and it needs the destination from a route table that may be wrong.
- **adsbdb is the only one that takes what the user types** (IATA form, either case). The others need the ICAO
  callsign, so Booklight needs the airline table of 1.4 anyway.

### 1.4 IATA to ICAO: a table to bundle

The user types IATA (`LH455`); the feeds and FlightAware's address speak ICAO (`DLH455`).

| Table | Licence | Size | State |
| --- | --- | --- | --- |
| **VRS standing data, `airlines.csv`** ([vradarserver/standing-data][vrs]) | **CC0** | 5,965 airlines; 1,375 with both codes; 987 different IATA designators. As `IATA⇥ICAO⇥name`: 30.7 KB (14.8 KB gzipped). Only the 1,078 pairs whose airline has routes in the same repository: 23.8 KB (11.6 KB) | Pushed to today. Has Discover (4Y/OCN), ITA (AZ/ITY), Breeze (MX/MXY). No "active" flag: US Airways is still in it |
| Wikidata (airlines with P229 and P230) | CC0 | 2,678 valid pairs; 1,211 without a dissolution date. 26.4 KB (13.4 KB) | Live query, 2.8 s. Gaps (no live entry for PS) and a few entries without an English name |
| OpenFlights `airlines.dat` ([openflights.org][openflights]) | ODbL: attribution, and "license any derived works made available to the public with a free license as well" | 6,162 rows; 1,255 marked active; 799 active with both codes. 17.9 KB (9.0 KB) | **Stale: the file's last commit is 2 February 2017.** Germanwings and Alitalia are "active"; 4Y is "Airbus France"; no Discover, ITA or Breeze |

Use the VRS table. Two things it needs:

- **A designator is not unique.** 307 of the 987 designators belong to more than one airline (LH: Lufthansa and
  Lufthansa Cargo; 4Y: Discover and three Airbus transport units). Picking the airline with the most routes in
  the VRS route table settles all but 79, and those are small carriers.
- **Airports**, if the row is to say "San Francisco" and not "SFO": OurAirports `airports.csv` is public domain
  ("All data is released to the Public Domain", [ourairports.com/data][ourairports]) and has 9,051 airports with an
  IATA code, 4,133 of them with scheduled service. `IATA⇥ICAO⇥city⇥country` for those 4,133: 93 KB (49 KB
  gzipped). It has **no time zone**. [mwgg/Airports][mwgg] (MIT) has one per airport; joined, the table is
  153 KB (55 KB gzipped) and 341 of the 4,133 lack a zone. The route sources already return city names, and
  the keyed APIs return local times, so this table is only needed if step 1 of the recommendation should name
  cities without a network. It should not.

### 1.5 A callsign is often not the airline code plus the flight number

Over Frankfurt during the research (adsb.lol, 150 nautical miles around the airport): `DLH403`, `DLH479`,
`BAW64`, `SIA312` beside `DLH7LA`, `DLH1CH`, `DLH8YY`, `EZY68XU`, `RYR61AT`, `EWG3WD`, `BAW911U`. European
short-haul flights mostly fly under alphanumeric callsigns that are chosen so that two similar numbers are
not on one frequency; they have no fixed relation to the ticket's flight number. In the VRS route table 41% of
Lufthansa's callsigns are alphanumeric (3,246 of 7,933), 52% of British Airways', 63% of Eurowings', 15% of
United's; over all airlines 234,110 of 620,700.

What that means for a keyless lookup:

- **Long-haul and most non-European flights:** IATA number → ICAO callsign by the table works (LH455 → DLH455,
  BA64 → BAW64, UA90 → UAL90).
- **European short-haul:** the route may still be found under the numeric form (the adsb.lol file has DLH2093
  as Hanover to Munich; adsbdb has no LH2093), but the **live aircraft cannot be found**: it is flying as
  something like DLH7LA, and no open table maps the ticket number to today's callsign. That mapping is exactly
  what the commercial APIs sell.
- Leading zeros must be stripped before any call (`LH0455` → 404 at adsbdb).

---

## 2. APIs with a key (2026)

None was called: there was no key. "Free" means a key from a free account.

| API | Free tier | First paid tier | A personal key for a few lookups a day? | Sign-up | "Next flight of number X" | Fields | Terms on whose key it is | Sure |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **AeroDataBox** (RapidAPI; also API.Market and direct) | RapidAPI "Basic": 400 units a month, 1 request a second and 1,000 an hour. A flight status call is tier 2 = 2 units, so **200 lookups a month**. API.Market's free plan is a 7-day trial | RapidAPI Pro $8 a month, 5,000 units; API.Market Pro $7.50; direct from $19 | **Yes** | A RapidAPI account; whether RapidAPI asks for a card on the free plan is not verified. HTTPS only | `GET https://aerodatabox.p.rapidapi.com/flights/number/LH455` with `X-RapidAPI-Key` and `X-RapidAPI-Host`. The number may be IATA or ICAO, with or without a space, in any case ("KL1395, Klm 1395"), so no table is needed for this call. Without a date it returns the flight "operating on the nearest date (either in past or in future)"; with `/{dateLocal}` that day. Also by callsign, registration or transponder address. (The path value is listed as `Number` in the enum and written `number` in the description) | Per end: airport, `scheduledTime`, `revisedTime`, `predictedTime`, `runwayTime` (each in UTC and local), `terminal`, `checkInDesk`, `gate`, `baggageBelt`, `runway`. `status`: Unknown, Expected, EnRoute, CheckIn, Boarding, GateClosed, Departed, Delayed, Approaching, Arrived, Canceled, Diverted, CanceledUncertain. `codeshareStatus`, aircraft (registration, model), airline, optional position | Keys "are strictly confidential and must not be shared with any third parties"; exposing one "within a source code published in a GitHub repository" is named a breach. Free plans: non-commercial use, attribution required. Not for anything safety-related. (Terms of 19 September 2026) | docs ([pricing][adb-pricing], [terms][adb-terms], [OpenAPI][adb-doc]) |
| **FlightAware AeroAPI** | "Personal": no monthly minimum, "up to $5 free per month"; `/flights/{ident}` is $0.005 a result set, so up to 1,000 lookups (a result set is a page of up to 15 flights and the reply covers about 14 days: with `max_pages=1` a lookup is one page); 10 result sets a minute | Standard: $100 a month minimum | **Yes**; the tier is for exactly this: "personal or academic purposes only" | An account; whether a card is needed for Personal is not stated on the page: not verified. | `GET https://aeroapi.flightaware.com/aeroapi/flights/DLH455` with `x-apikey`. Returns "approximately 14 days of recent and scheduled flight information… ordered by `scheduled_out`… descending": the app picks the next one. "It is highly recommended to specify ICAO flight ident rather than IATA"; `ident_type=designator` forces a flight number | `scheduled_out` / `estimated_out` / `actual_out`, the same for `off`, `on`, `in`; `gate_origin`, `gate_destination`, `terminal_origin`, `terminal_destination`, `baggage_claim`, `status`, `progress_percent`, delays, aircraft type | Personal tier: no business use, no commercialisation | docs ([tiers and per-query fees][aeroapi], [OpenAPI 4.17.1][aeroapi-spec]) |
| **aviationstack** | 100 requests a month; HTTPS is listed in the free plan today; real-time flights yes, schedules and future flights no; "Non-Commercial Use" | Basic $49.99 a month, 10,000 requests | Barely: three a day | Account (apilayer); card not verified | `https://api.aviationstack.com/v1/flights?access_key=KEY&flight_iata=LH455` (memory; the docs page gave no text). The key travels in the address | scheduled, estimated, actual, terminal, gate, delay, baggage on arrival; `flight_status` scheduled, active, landed, cancelled, incident, diverted (memory) | Not read | docs ([pricing][avstack]) for the plans; the rest memory |
| **AirLabs** | A free plan exists ("Get a FREE package"); its quota is not verified (the price table is drawn by script). **On the free plan the flight endpoint returns only some fields**: airports, `dep_time`, `arr_time`, position, airline, flight number, aircraft type. Not the estimated times, terminals, gates, baggage or `status` | Not verified; the home page's comparison says $19 per 10,000 queries | No: the free fields are the schedule only | Account | `https://airlabs.co/api/v9/flight?flight_iata=LH455&api_key=KEY`. "Only one closest (live, scheduled or landed) flight returns" | Paid: `dep_time`, `dep_estimated`, `arr_time`, `arr_estimated`, `dep_terminal`, `dep_gate`, `arr_terminal`, `arr_gate`, `arr_baggage`, `dep_delayed`, `arr_delayed`, `status` (scheduled, en-route, landed), aircraft | Replies carry: "Reselling data 'As Is' without AirLabs.Co permission is strictly prohibited" | docs ([flight endpoint][airlabs-flight]) |
| **Aviation Edge** | "Free API Key with limited data and API calls"; limits not verified | Developer: $7 a month shown as a reduced price (from $299), 30,000 calls | Not verified | Account | Timetable and flight-tracker endpoints; exact address not verified | Not verified | Not read | docs ([pricing][avedge]) |
| **FlightLabs** (goflightlabs) | None: "7-day trial for FREE or up to 50 requests, whichever comes first" | Starter $24.99 a month, 4,000 calls | No | Account | Not read | Not read | Not read | docs ([home page][flightlabs]) |
| **Amadeus Self-Service** (On-Demand Flight Status) | Not verified: the portal's pages gave no text. From memory: `GET /v2/schedule/flights?carrierCode=LH&flightNumber=455&scheduledDepartureDate=…` with an OAuth token from a key and a secret, a free monthly quota. **From memory, Amadeus announced in early 2026 that the Self-Service portal closes in July 2026.** Check before spending a minute on it | | | | | | | memory, not verified |
| **Cirium / FlightStats** | No standing free tier: "Free trials are now available exclusively through Cirium Developer Studio" | Enterprise pricing (not verified) | No | A trial request | Not read | Not read | Not read | docs ([FlightStats developer centre][flightstats-dev]) |
| **Flightradar24 API** | Not verified: its pages gave no text. From memory: paid subscriptions with credits, a sandbox with fixed data, no free live tier | | No | | | | | memory, not verified |
| **Lufthansa Open API** | A free developer account; public and partner APIs. Rate limit not verified (its page is gone; from memory 5 calls a second, 1,000 an hour) | none (partner programme) | Yes, for Lufthansa Group flights only | Account; **two secrets** (client id and client secret) exchanged for a token at `POST https://api.lufthansa.com/v1/oauth/token` | `GET https://api.lufthansa.com/v1/operations/flightstatus/LH455/2026-10-02`. A date is required ("7 days in the past until 5 days in the future"), so "next" takes two calls | Per end: `AirportCode`, `ScheduledTimeLocal`/`UTC`, `EstimatedTime…` or `ActualTime…`, `TimeStatus` (FE early, NI next information, OT on time, DL delayed, NO no status), `Terminal.Name`, `Terminal.Gate` | The licensee "may grant a sub-licence to End Users to permit them to use and view the Licensed Data" in its application; no redistribution | docs ([flight status][lh-status], [response][lh-response], [terms][lh-terms]) |
| **Schiphol** | A free account gives an app id and an app key (memory). Flights through Amsterdam only | none | Only for Amsterdam | Account | `GET https://api.schiphol.nl/public-flights/flights?flightName=KL1001` with `app_id`, `app_key`, `ResourceVersion: v4` (memory) | Schedule time, estimated and actual landing time, gate, terminal, baggage belts, a state code (memory) | Not read | the portal exists ([developer.schiphol.nl][schiphol]); the rest memory |
| **Fraport** | Not verified: `developer.fraport.de` timed out | | | | | | | not verified |

**Which one a user could really paste a key for.**

1. **AeroDataBox through RapidAPI.** One key, free, 200 lookups a month, every field asked for, one call that
   answers "nearest flight of this number". Raycast's "Flight Tracker" extension does exactly this and tells its
   users to subscribe to the Basic plan and paste the key ([its README][raycast-flight]). Its own FAQ is frank
   that the data is "best-effort" with coverage that depends on the airport.
2. **FlightAware AeroAPI, Personal tier**, as the second choice for people who want FlightAware's data: about
   1,000 lookups a month within the free $5, the tier is meant for personal use, and Booklight can pick the next
   flight from the list it returns.

Both fit "the user brings their own key": the user is the provider's customer, the key stays in the app's
private storage on their device, and Booklight's developer never sees it. That reading of the terms is mine;
neither vendor was asked.

---

## 3. Web addresses that take a flight number

Checked with `curl` and a desktop Chrome user agent. Nothing was read from the pages beyond the title.

| Site | Address | What came back |
| --- | --- | --- |
| **FlightAware** | `https://www.flightaware.com/live/flight/DLH455` | **200, title "LH455 (DLH455) Lufthansa Flight Tracking and History".** `UAL90` and `EZY8001` likewise. The IATA form is not safe: `/LH455`, `/BA64`, `/FR1` gave 200 with a bare title ("LH455 Flight Tracking and History") and no mention of the ICAO callsign in the page; `/UA90`, `/UA926` and `/U28001` gave "Unknown Flight". Lower case is redirected (301) to upper case. FlightAware's own API notes say an ident is "interpreted as a registration if possible". **Build the ICAO form from the table** |
| **FlightStats** | `https://www.flightstats.com/v2/flight-tracker/LH/455` | **200, title "LH455 - Lufthansa LH 455 Flight Tracker".** Also `/UA/90` ("United Airlines UA 90") and `/U2/8001`. Takes the IATA designator and the number as two parts: no table needed |
| Google | `https://www.google.com/search?q=LH455` | 200, but `curl` gets the "enable JavaScript" page: **the flight card is not verified.** From memory the bare number or "LH 455" shows the card, and "LH455 flight status" is the form that works for designators Google would otherwise read as something else |
| Flightradar24 | `https://www.flightradar24.com/data/flights/lh455` | 403, Cloudflare's "Just a moment…" check, for this and for `/dlh455`: **not verified.** From memory this address takes the IATA form in lower case and lists recent and coming flights |
| Kayak | `https://www.kayak.com/tracker/LH-455` | 200 with the general title "Flight Tracker": not verified that it opens the flight |
| Plane Finder, AirNav Radar, Flightera | `/flight/LH455` and the like | 403 bot walls: not verified |
| Bing, DuckDuckGo | `…?q=LH455+flight+status` | 200, ordinary result pages; cards not looked at |

**The airlines' own pages: no general pattern, and bot walls everywhere.**

| Airline | What was tried | What came back |
| --- | --- | --- |
| Lufthansa | `lufthansa.com/de/en/flight-status`, and the home page | 403 for every address, the home page too. No address that takes the number was found |
| United | `united.com/en/us/flightstatus/details/90/2026-10-02/EWR/TLV/UA` (the form from memory: it wants the date and both airports) | The connection is cut (HTTP/2 error, then time-outs). Not verified, and not buildable from a number alone |
| British Airways | `britishairways.com/travel/flightstatus/public/en_gb` | Time-out. Not verified |
| easyJet | `easyjet.com/en/flight-tracker/8001` | 200, 240 KB, the site's general title; with `U28001` it redirects to `/en/flight-tracker`, which answers 403. So the number without the designator seems to be the form: not verified in a browser |
| Ryanair | `ryanair.com/gb/en/flight-info/flight-tracker` | 200, "Live Flight Information": a form, no address per flight found |

So Enter should open **FlightAware (ICAO form)**; the other actions of the row are **FlightStats** and **a search
with the chosen engine**. A table of airline pages is not worth making.

---

## 4. What a flight number looks like

From memory of the IATA and ICAO rules; the standards themselves were not opened today.

- **IATA:** an airline designator of two characters, then one to four digits, then an optional letter (an
  operational suffix, rare in public). The designator is two letters, or a letter and a digit in either order:
  `U2`, `W6`, `B6`, `F9`, `A3`, `S7`, `3U`, `9W`, `6E`, `4Y`. Of the 987 designators in the VRS table 609 are two
  letters and 378 contain a digit (called).
- **ICAO:** three letters, then the flight identification: up to four characters, digits first, the last one or
  two may be letters (`DLH455`, `BAW9AB`, `EZY68XU`). VRS's own pattern is
  `^([A-Z]{2,3}|[A-Z][0-9]|[0-9][A-Z])(\d[A-Z0-9]*)` with the number at most four characters ([route schema][vrs]).
- **What people type:** `LH455`, `lh455`, `LH 455`, `lh 455`, `LH0455`, `LH 0455`, `DLH455`, and in running text
  "LH 455". A hyphen (`LH-455`) is rare but seen.

### The false positives

Nearly every two-character token is a designator of some airline. In the VRS table (called):

| Typed | Reads as |
| --- | --- |
| `PS5` | PS (Ukraine International) 5 |
| `MP3` | MP (Martinair) 3 |
| `H264` | H2 (Sky Airline) 64 |
| `G7 2026` | G7 (GoJet) 2026 |
| `Q4 2026` | Q4 (Starlink Aviation) 2026 |
| `MS 365`, `OS 26`, `AS 400`, `HD 1080`, `SD 128`, `AI 5`, `BT 5`, `QR 1`, `PC 100`, `4K 60`, `5G 2`, `M3 16`, `E5 2600`, `I7 1260` | Egyptair, Austrian, Alaska, Air Do, Sudan Airways, Air India, airBaltic, Qatar, Pegasus, Askari, Anda Air, LATAM Cargo, Air Arabia Egypt, IndiaOne |
| `AM 7`, `PM 5`, `IN 2026`, `TO 100`, `AT 1200`, `NO 5`, `ME 2`, `WE 3`, `US 1`, `IT 100`, `DE 2026`, `FR 24`, `OK 2`, `ON 2`, `OR 2`, `IF 2`, `DO 2`, `GO 2`, `BY 2`, `SO 2`, `UP 2`, `MY 2` | Aeroméxico, Canary Fly, NAM Air, Transavia France, Royal Air Maroc, Neos, Middle East Airlines, Parata Air, Silk Avia, Tigerair Taiwan, Condor, Ryanair, Czech Airlines, and so on |
| `A4`, `U2`, `K9`, `B1` alone | Not a flight: no number after the designator |
| `B12` | B1 is not in the VRS table (it is in Wikidata, as Air Bucharest) |
| `K9 1` | K9 (Kalitta Charters) 1 |
| `X86 64` | Nothing: X8 is no designator |
| Postcodes (`SW1A 1AA`, `M1 1AE`), `A4 210`, `B6 12` | Some match (`M1` is free, `B6` is JetBlue) |

adsbdb turned away `PS5`, `MP3`, `B12`, `U2`, `A4` (400, too short) and did not know `H264` or `G72026` (404): a
network lookup filters some of these, but it sends "PS5" to a third party to find out.

### A rule a launcher can apply

1. **Shape.** The whole trimmed text, upper-cased, matches
   `^([A-Z]{2}|[A-Z][0-9]|[0-9][A-Z])[ -]?0*([1-9][0-9]{0,3})[A-Z]?$` (IATA) or
   `^[A-Z]{3}[ -]?0*([1-9][0-9]{0,3})[A-Z]?$` (ICAO). Nothing else in the field. Leading zeros dropped.
2. **Table.** The designator is in the bundled table (for IATA: the airline with the most routes wins).
3. **Strong or weak.**
   - *Strong:* a two-letter designator that is not an everyday word, with two to four digits (`LH455`, `BA64`,
     `UA 90`), or any ICAO form. Shown as a row; a network lookup may start after a pause.
   - *Weak:* a designator with a digit (`U2 8001`, `H264`, `G7 2026`), a designator on a short stop list of
     words and abbreviations (AM, PM, IN, TO, AT, NO, ME, WE, US, IT, DE, FR, OK, ON, OR, IF, DO, GO, BY, SO, UP,
     MY, AS, MS, OS, HD, SD, AI, PC, BT, QR, PS, MP, TV, ID, IP, IQ; German: AB, AN, DA, ER, ES, IM, JA, UM, ZU), a
     single digit after the designator (`PS5`, `MP3`), or a number that is a year from 1990 to 2039 after a
     space. Shown as a row below everything local, and **nothing is sent** until Enter or Tab.
4. **Rank.** A flight row never outranks a local match (an app, a sum, one of the user's links). A false
   positive then costs one extra row at the bottom, not a wrong first row.
5. **A keyword makes it certain.** `flight ps5` (German `flug`) skips the weak test, and inside that scope the
   text is looked up as typed.

**What it still gets wrong.** easyJet, Wizz, JetBlue, IndiGo and every other airline with a digit in its
designator are "weak" and never fetch by themselves: their passengers must press Tab or use the keyword.
`BA1`, `QF1`, `LH2` are real flights and "weak". `AS 400` and `MS 365` with three digits pass as strong unless
the stop list has them (it does above, at the price of making Alaska and Egyptair weak). `DE 2026` is a Condor
flight to somebody. A flight number with a suffix letter is rare enough to drop if it causes trouble. And the
table cannot say whether flight 455 exists: `LH9999` is a row too.

---

## 5. Privacy and terms, for the recommended route

**Privacy.** With the row that only opens a page, nothing leaves the device until Enter, and then the browser
goes to FlightAware, FlightStats or the search engine with the flight number in the address: the same as any
web row today. With a keyless lookup, Booklight itself sends one HTTPS request holding the callsign (`DLH455`)
to adsb.lol's file host (GitHub Pages behind Cloudflare) or adsbdb, and perhaps one to a live feed; with the
user's key it sends the flight number and that key to RapidAPI and AeroDataBox, or to FlightAware. In each case
the receiver also sees the IP address, and with a key it can tie the lookups to an account the user made
there. No cookies, no identifier, the plain `Booklight` user agent, as in `SuggestProvider`. Is a flight
number with an IP address personal data in a meaningful sense? In law an IP address is personal data in the
EU, so yes formally. In substance one lookup says little, since a flight has hundreds of passengers and many
more people waiting for them, but a run of lookups from one address (the flight out, the flight back, the same
route every other week) is a travel pattern, and "which flight is someone at this address interested in today"
is the kind of fact the owner's rule is about. So it needs the same treatment as suggestions: off until
turned on, a sentence that names who receives it, never sent for a weak match or while still typing, and for
the keyed road the key is asked for only by the user's own act of pasting it.

**Terms.** No keyless source is plainly licensed for a redistributed app except the CC0 route file. adsbdb
publishes no limit and no terms beyond "report incorrect data", and its route data must not be copied, so it
may be queried and not bundled. hexdb.io asks not to be scraped and blocks heavy users. adsb.lol's data is
ODbL (attribution in the app; a database made from it must be shared alike) and its operator asks to be told
before production use and says a key will come. adsb.fi says personal, non-commercial use, one request a second
and a visible citation with a link; a free open-source app whose user looks up a flight is arguably that, but
the app's author is not the one who can decide it. airplanes.live refused the calls. OpenSky forbids
operational use without a written agreement. So before step 2 or 3 below ships, **write to adsb.lol (and to
adsb.fi, if it is used) and say what the app does**; both ask for exactly that. For the keyed road the contract
is between the user and the provider: AeroDataBox's free plans are for non-commercial use with attribution and
its keys must not be shared, FlightAware's Personal tier is for personal or academic use. Booklight should show
"Data: AeroDataBox" (or FlightAware) in the row's footer or the settings line, keep the key in its private
storage and out of any backup or log, and never ship a key of its own.

**Play's data-safety form.** The suggestions feature is already filed as "App activity › In-app search
history", collected and shared, optional, processed ephemerally, for app functionality
(`store-submission/forms/data-safety.md`). A flight lookup is the same data type going to a new recipient: the
form's answers stay as they are, and the written notes, `PRIVACY.md` and the settings text gain the new
recipients and addresses. A key the user pastes is stored on the device only and sent only to the service it
belongs to; Play's form has no data type that plainly fits a third party's API key the user supplies, and
whether it wants one declared is not verified. No new Android permission is needed for any road: `INTERNET` is
already there.

---

## 6. What other launchers do

| Launcher | What it does with a flight number | Where the data comes from | Sure |
| --- | --- | --- | --- |
| **Spotlight** (macOS, iOS) | A card for an airline code and number: airline and number, a status word (on time, delayed, landed), both airports with scheduled and updated local times, terminal and gate, baggage claim on arrival, the time left, and a map of the route with the aircraft on it. The same card opens from a flight number detected in Mail or Messages | Apple does not say on the pages opened today; the Mac User Guide's Spotlight pages do not mention flights at all | memory, not verified |
| **Raycast** | Nothing built in. The Store has "Flight Tracker" (3,759 installs as shown today): `track DL123`; the user subscribes to AeroDataBox's free Basic plan on RapidAPI and pastes the key on first use; it shows "the departure and arrival airports, the scheduled and actual departure and arrival times, and the current location of the flight if it is in the air". Also "Flighty" (2,152 installs), which talks to the Flighty app, and "Flight Search", which opens Skyscanner | AeroDataBox, with the user's own key | called (the Store's search) and docs ([README][raycast-flight]) |
| **Alfred** | Nothing built in. A custom web search (a keyword that opens FlightAware or Google with the text) is the usual way; workflows exist | The site that opens | memory; the Gallery's search page gave no results to `curl` |
| **KDE KRunner** | No flight runner among the ones it ships with | | memory; the documentation page answered 403 |
| **Google's search box** | A card on top of the results: airline and number, a status line ("On time", "Delayed 25 min", "Landed"), a line between the two airports showing progress, scheduled and estimated or actual times in each airport's local time, terminal and gate at both ends, sometimes the baggage belt, and a switch between yesterday, today and tomorrow | Not named | memory; `curl` gets a JavaScript wall |

For the design: all of them show the same eight things (airline, number, status word, two airports, two pairs of
times, terminal and gate), and the two that have live data without the user's key (Apple, Google) pay for it.

---

## 7. The system's own "Track" action

`docs/research/screen-and-clipboard.md` 4.2 and `device-findings.md` already say that the system's text
classifier knows the entity type `flight` and scored "LH 454 lands 14:05" as flight 1.0 on the Lenovo Googlebook.
What the action behind it is:

- **Android 10** (source, `android10-release`, `LegacyClassificationIntentFactory.createForFlight`): one action,
  label `view_flight` = "Track", description `view_flight_desc` = "Track selected flight", and the intent is
  `new Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text)`. **A web search for the selected
  text, nothing more.**
- **Android 11 to 17** (source): that file is gone from `frameworks/base` (404 on every release branch from
  `android11-release` to `android17-release`). The classifier lives in `external/libtextclassifier`, whose
  `TemplateIntentFactory` builds each action from a template that comes out of the model file, not out of Java
  code.
- **AOSP's model** (`native/models/textclassifier.en.model`, 603 KB, `android17-release`, downloaded and
  searched): it holds the same two strings in every language ("Track", "Track selected flight", German
  "Ausgewählten Flug verfolgen") and entity fields named `airline_code` and `flight_number`. The template that
  says which intent is built sits in a compressed part I could not read: **not verified** that it is still a
  web search, though nothing suggests it changed.
- **On a Googlebook** the classifier is very likely Google's own service with its own model, not AOSP's: what
  `classifyText("LH 455").getActions()` returns there, and which app the action opens, is a device check
  (section 9). Whether an app can read `airline_code` and `flight_number` from the classification is not
  verified either.

For Booklight this means: the classifier is a second opinion on "is this a flight number" (useful for the
clipboard row and for text handed over from another app), and its action is no better than Booklight's own
web row.

---

## 8. Recommendation: from cheapest to richest

**Step 1. The row, with no network.** A parser in `core/` (the rule of section 4, with tests for every line of
the false-positive table) and the VRS airline table as an asset (CC0, about 24 to 31 KB, a line in `NOTICE` out
of courtesy). The row reads "LH 455 · Lufthansa". Enter opens FlightAware with the ICAO form; the other
actions are FlightStats, a search with the chosen engine ("LH455 flight status"), and Gemini with the question
filled in, not sent. No consent, no permission, nothing new in the data-safety form. This alone answers "type
LH455, press Enter, see the next flight", one key press later than asked for.

**Step 2. The usual route, keyless.** Behind a switch of its own, worded like the suggestions switch ("the
flight number you type is sent to adsb.lol to look up its route"): for a strong match, after a pause in
typing, one request for `routes/DL/DLH455.json`; the row becomes "LH 455 · Lufthansa · San Francisco → Frankfurt".
CC0 data, 0.04 s, 800 bytes, cached for the session. Label it as the usual route, since the tables are
sometimes out of date. Tell adsb.lol first. adsbdb is the fallback if they say no.

**Step 3. "In the air now", keyless (optional).** The same switch: one request to a live feed by callsign. If
the aircraft is heard: "in the air · 11,300 m · 970 km/h · about 9 h to go", the last part worked out from the
distance to the destination of step 2 and marked as an estimate. If not: nothing, and never "landed". It will
show nothing for most European short-haul flights (section 1.5) and for aircraft over oceans. Needs the
operator's yes (adsb.lol: ODbL and "contact me"; adsb.fi: personal use, a citation). It is the least sure step
and can be left out without loss.

**Step 4. The full answer, with the user's own key.** A field in the Booklight window: "AeroDataBox key
(RapidAPI)", with two lines on how to get one. With a key, a strong match (or anything after the `flight`
keyword) fetches `flights/number/LH455` after a pause and the row shows what was asked for, for example (made-up values) "LH 455 · San
Francisco → Frankfurt · departs 14:40, now 15:05 · Terminal G, gate G4 · delayed", and Tab opens the rest
(arrival terminal, belt, aircraft). At most one request a second and 200 lookups a month on the free plan, so:
only on a pause, never per keystroke, a cache of a minute or two, and a clear line when the quota is used up.
"Data: AeroDataBox" is shown. FlightAware's Personal key can be a second choice in the same field later.

**Not recommended.** OpenSky (its terms forbid it). Any internal endpoint of Flightradar24, FlightAware or
Google (scraping). A key of Booklight's own in the APK. Bundling the whole route table (4.5 MB compressed;
1.8 MB for the numeric callsigns alone) to avoid a request that is 800 bytes. Asking the on-device model: it
does not know what is flying.

Steps 1 and 4 together are the feature. Step 1 can ship alone and is useful on day one; step 4 is what makes the
row answer. Steps 2 and 3 are a nice middle for users without a key and cost two letters to two operators.

---

## 9. To check on a device

1. `TextClassifier.classifyText` on "LH 455", "LH455", "lh455", "UA 90", "U2 8001", and on "PS5", "MP3",
   "H264", "G7 2026": the flight score of each, and for the real ones `getActions()`: the label, the intent's
   action and extras, and which app opens. On both Googlebooks.
2. `ACTION_WEB_SEARCH` with "LH455": which app takes it, and whether a flight card appears. The same for
   `https://www.google.com/search?q=LH455`, `…q=LH+455` and `…q=LH455+flight+status` in Chrome on the device: which
   form shows the card every time, also for `U2 8001` and `PS 5`.
3. The addresses of section 3 in Chrome on the device: FlightAware `/live/flight/DLH455` (the flight, with no
   wall to click away), FlightAware `/live/flight/LH455` (does a browser resolve what `curl` did not),
   FlightStats `/v2/flight-tracker/LH/455`, Flightradar24 `/data/flights/lh455`. And whether an installed
   tracker app takes any of these links instead of the browser.
4. The Gemini hand-over with "Status of flight LH455 today?": does the Gemini app answer from live data, and
   how long does it take.
5. The keyless calls from the app's own `HttpURLConnection` with the `Booklight` user agent, on the device's
   network: adsb.lol's route file, adsbdb, adsb.lol and adsb.fi live. Status codes, time to answer from Europe,
   and that a missing user agent is the only reason for adsb.lol's 403.
6. With a real RapidAPI key: `flights/number/LH455`, `…/BA64`, `…/U28001`, a codeshare number, and a flight that
   landed two hours ago. Which fields are filled at Frankfurt, San Francisco and a small airport; whether
   `revisedTime` is what a passenger calls "expected"; which flight "nearest date" picks at 23:00 local time
   and just after landing; what the quota error looks like.
7. The row: do airline, route, two times and a status fit the panel's slots in English and in German, with
   12-hour and 24-hour clocks, and with a long airport name.
8. A day of real typing with the rule of section 4 switched on in a debug build: how often a flight row turns
   up for text that was not a flight.

---

## What could not be verified

- Why LH455 was absent from the live feeds (out of receiver range, or not flying).
- Google's flight card and Flightradar24's page for a typed number (bot walls); every airline's own status page.
- airplanes.live (403 for every call; its guide is behind a check).
- Any keyed API's real reply; whether RapidAPI or AeroAPI ask for a card; AirLabs' and Aviation Edge's free quotas; Lufthansa's
  rate limit; Schiphol's and Fraport's details; Flightradar24's API plans; Amadeus Self-Service's state in 2026.
- The intent behind the classifier's "Track" action in Android 17's model, and what a Googlebook's classifier
  returns.
- Spotlight's, Alfred's, KRunner's and Google's behaviour (written from memory).
- Whether the operators of adsb.lol, adsb.fi, adsbdb and hexdb.io accept calls from a shipped app.
- The IATA and ICAO rules for designators and numbers (from memory, not from the standards).

## Sources

Called today: the addresses in section 1.1; `https://api.adsb.lol/api/openapi.json`;
`https://vrs-standing-data.adsb.lol/routes.csv.gz`; the Wikidata query service (airlines with P229 and P230);
`https://www.raycast.com/api/v1/store_listings/search?q=flight`; the addresses in section 3.

Read today: [adsbdb README][adsbdb], [adsb.fi open data README][adsbfi], [adsb.lol API description][lol-api],
[adsblol/vrs-standing-data][vrs-lol], [vradarserver/standing-data][vrs] (README, airline and route schemas,
licence), [hexdb.io][hexdb], [OpenSky REST docs][opensky-rest], [OpenSky terms of use][opensky-terms],
[OpenFlights data][openflights], [OurAirports data][ourairports], [mwgg/Airports][mwgg],
[AeroDataBox pricing][adb-pricing], [terms][adb-terms], [FAQ][adb-faq] and [OpenAPI][adb-doc],
[FlightAware AeroAPI][aeroapi] and its [OpenAPI file][aeroapi-spec], [aviationstack pricing][avstack],
[AirLabs flight endpoint][airlabs-flight], [Aviation Edge pricing][avedge], [FlightLabs][flightlabs],
[FlightStats developer centre][flightstats-dev], [Lufthansa flight status][lh-status], [its response][lh-response]
and [API terms][lh-terms], [Schiphol developer portal][schiphol], [Raycast Flight Tracker README][raycast-flight],
[Apple, Spotlight on Mac][apple-spotlight].

Android source: `frameworks/base` at `android10-release`,
`core/java/android/view/textclassifier/intent/LegacyClassificationIntentFactory.java` and
`core/res/res/values/strings.xml`; `external/libtextclassifier` at `android17-release`,
`java/src/com/android/textclassifier/common/intent/TemplateIntentFactory.java` and
`native/models/textclassifier.en.model` ([android.googlesource.com][aosp-tc]).

In this repository: `docs/research/screen-and-clipboard.md` (4.2), `docs/research/device-findings.md`
(the clipboard's description), `docs/research/on-device-ai.md`, `PRIVACY.md`,
`store-submission/forms/data-safety.md`, `app/…/providers/SuggestProvider.kt`.

[adsbdb]: https://github.com/mrjackwills/adsbdb
[adsbfi]: https://github.com/adsbfi/opendata
[lol-api]: https://api.adsb.lol/docs
[vrs-lol]: https://github.com/adsblol/vrs-standing-data
[vrs]: https://github.com/vradarserver/standing-data
[hexdb]: https://hexdb.io/
[opensky-rest]: https://openskynetwork.github.io/opensky-api/rest.html
[opensky-terms]: https://opensky-network.org/about/terms-of-use
[openflights]: https://openflights.org/data.php
[ourairports]: https://ourairports.com/data/
[mwgg]: https://github.com/mwgg/Airports
[adb-pricing]: https://aerodatabox.com/pricing/
[adb-terms]: https://aerodatabox.com/terms/
[adb-faq]: https://aerodatabox.com/faq/
[adb-doc]: https://doc.aerodatabox.com/
[aeroapi]: https://www.flightaware.com/commercial/aeroapi/
[aeroapi-spec]: https://www.flightaware.com/commercial/aeroapi/resources/aeroapi-openapi.yml
[avstack]: https://aviationstack.com/pricing
[airlabs-flight]: https://airlabs.co/docs/flight
[avedge]: https://aviation-edge.com/premium-api/
[flightlabs]: https://www.goflightlabs.com/
[flightstats-dev]: https://developer.flightstats.com/
[lh-status]: https://developer.lufthansa.com/docs/read/api_details/operations/Flight_Status
[lh-response]: https://developer.lufthansa.com/docs/read/api_details/operations/Flight_Status_Response
[lh-terms]: https://developer.lufthansa.com/API_Terms_of_Use
[schiphol]: https://developer.schiphol.nl/
[raycast-flight]: https://github.com/raycast/extensions/tree/main/extensions/trackflight
[apple-spotlight]: https://support.apple.com/guide/mac-help/find-what-you-need-with-spotlight-mchlp1008/mac
[aosp-tc]: https://android.googlesource.com/platform/external/libtextclassifier/+/refs/heads/android17-release
