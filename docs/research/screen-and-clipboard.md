# Booklight: the on-device model, the screen and the clipboard

*Desk research, 1 October 2026. It adds to `docs/research/on-device-ai.md` and the "on-device model" section of
`docs/research/device-findings.md`; what those two already say is not repeated. No device was touched, no file in
the repo was changed, nothing was built.*

**Tried on the Lenovo afterwards** (`device-findings.md`, "A picture to the model, and the clipboard's
description"): the model answers about a picture; a picture costs about 256 tokens whatever its size; text in a
picture of a region or a window is read correctly; a whole 2880 × 1800 screen is not, and the model invents. The
clipboard's description gives the type, the age and the entity scores without reading the clip. Sections 1.2,
2.3 (items 3 and 4), 3.1 and 4.1 are confirmed by that; item 1 of 2.3 (does a screenshot land on the clipboard)
is still open.

**How it was done, and its limit.** The session's web-search budget was already used up, so nothing here comes from
a search engine. Every claim comes from a page opened directly today (Google's, Android's, Microsoft's, Apple's,
Raycast's and others' own pages), from Google's Maven index, or from the Android 17 source (`android17-release`) and
the Chromium source, downloaded and read. Where a page could not be opened, the claim is marked **not verified**.

**How sure each claim is.** *docs* = read on the vendor's page today. *source* = read in the platform source today.
*device* = already measured, in `device-findings.md`. *inferred* = my reading of the evidence. *not verified* = could
not be confirmed today.

---

## The short version

1. **The model takes pictures.** The Prompt API accepts one or several images with the text (several since
   beta3, July 2026). The Googlebook has nano-v3, which very likely sees every picture shrunk to at most
   768 × 768 pixels. A 2880 × 1800 screen shrunk that far has no readable small text left. So: **read the text
   with OCR first, ask the model about the text**, and send the picture itself only for "what is this a picture
   of". Google's own page says the same, and Windows Click to Do is built that way.
2. **No route gives an app from Play a silent picture of the screen without a large ask.** The routes that ask
   nothing (a screenshot on the clipboard, shared to Booklight, or picked in the photo picker) cost the user one
   to three steps. The only routes that are silent at the moment the panel opens are being the device's default
   digital assistant (it replaces Gemini on its key) and an accessibility service (Play declaration). Both are the
   owner's decision. MediaProjection asks every single time and needs a foreground service: not a fit.
3. **The clipboard can be looked at without reading it.** `getPrimaryClipDescription()` tells Booklight that
   there is text, when it was copied, and whether the system thinks it holds a link, an address, a date or a
   phone number, all without the "pasted from your clipboard" message. That is exactly what a zero-state row
   "there is a fresh copy, Tab for actions" needs. The text itself is read only when the user presses Tab.

---

## 1. What the on-device model can do today

### 1.1 The APIs

Versions on Google's Maven today are the ones `on-device-ai.md` names; nothing newer exists
([Maven index][maven], read today).

| API | Library | State | What goes in | What comes out | Languages |
| --- | --- | --- | --- | --- | --- |
| **Prompt** | `genai-prompt` 1.0.0-beta4 (21 July 2026) | beta | Text, or **one or more images plus text**. An image is a `Bitmap`, encoded bytes or a `Uri` | Text, whole or streamed; a typed object with the alpha schema library | No list published. German and English worked on the Lenovo (device) |
| Structured output | `genai-schema`, `genai-schema-compiler` 1.0.0-alpha1 | alpha | A Kotlin class marked `@Generable` (String, numbers, Boolean, List, nested classes). Kotlin only, KSP 2.3.6+ | That class, filled in | as Prompt. **Reports unavailable on the Lenovo** (device) |
| Summarization | `genai-summarization` 1.0.0-beta1 | beta | Article or conversation, under 4,000 tokens, an article over 400 characters | One, two or three bullets | English, Japanese, Korean |
| Proofreading | `genai-proofreading` 1.0.0-beta1 | beta | Under 256 tokens; typed or dictated | Corrected text | English, Japanese, French, German, Italian, Spanish, Korean |
| Rewriting | `genai-rewriting` 1.0.0-beta1 | beta | Under 256 tokens | Elaborate, Emojify, Shorten, Friendly, Professional, Rephrase | the same seven |
| Image description | `genai-image-description` 1.0.0-beta1 | beta | One image | "One short description of the image" | English only |
| Speech recognition | `genai-speech-recognition` 1.0.0-alpha1 | alpha | Microphone, or 16 kHz mono PCM | Text | Basic mode: en-US plus 14 beta locales (de-DE among them). Advanced mode (the generative model): **Pixel 10 and 11 only** |

Sources: [overview][genai], [Prompt][prompt], [get started][prompt-start], [structured output][prompt-structured],
[summarization][sum], [proofreading][proof], [rewriting][rewrite], [image description][imgdesc],
[speech][speech], [release notes][relnotes], [`ImagePart`][imagepart].

There is still **no translation API** in the GenAI family; "Short translations" is listed as a use of the Prompt
API ([Prompt][prompt]). The Prompt package has **no audio part and no tool calling** today (its class list has
neither: [package reference][prompt-pkg]); Google's April post names tool calling as coming
([AICore Developer Preview post][aicore-post]).

### 1.2 The Prompt API in detail

- **Images: yes.** "GenAI Prompt API accepts either a text input or a combined image and text input"
  ([Prompt][prompt]). Since beta3 it has "multi-image support, which lets you pass multiple images and text
  together" ([release notes][relnotes]); "You can … bundle multiple images and text together in the same request"
  ([get started][prompt-start]). No limit on the number, size or format is documented; `ImagePart` documents
  none ([`ImagePart`][imagepart]). A bad image gives error `INVALID_INPUT_IMAGE` ([error codes][errors]).
- **What the model sees of an image (inferred).** Google says nano-v3 is "built on the same architecture as
  Gemma 3n" ([Prompt alpha post][prompt-alpha]). Gemma 3n takes "Images, normalized to 256x256, 512x512, or
  768x768 resolution and encoded to 256 tokens each" ([Gemma 3n model card][gemma3n]). So a picture probably
  costs about 256 tokens and is seen at no more than 768 pixels a side. Check on the device with
  `countTokens()` on a request that holds an image. Encoding a picture added 0.6 s on a Pixel 10 Pro
  ([Google, August 2025][nano-aug]); on the Lenovo it is unmeasured.
- **Google's own advice:** "For best results, consider using the ML Kit Text Recognition API to first extract
  text from an image." ([Prompt][prompt])
- **Token limits.** Input "under 4000 tokens (or approximately 3000 English words)"; "Use cases that require long
  output (more than 4K tokens) should be avoided" ([get started][prompt-start]). beta3 raised the output limit
  to 4,096 tokens ([release notes][relnotes]). `getTokenLimit()` is the total of input and output
  ([`GenerativeModel`][genmodel]); the Lenovo reports 8,192 (device). `countTokens()` tells before sending.
- **Streaming: yes** (`generateContentStream`, [`GenerativeModel`][genmodel]).
- **Structured output, system instructions, thinking mode** arrived in beta3 (14 July 2026,
  [release notes][relnotes]). Each has an "is it available" call, and **on the Lenovo all three say no**
  (device). So on a Googlebook today a typed answer means asking for JSON in the prompt and reading it with
  Booklight's own code; the device probe got a JSON extraction right that way (device).
- **Caching.** A fixed prompt prefix is cached; explicit cache management since beta1 (January 2026)
  ([release notes][relnotes], [`GenerativeModel`][genmodel]). Google's skill file: "If the prompt is more than
  200 words, implement the prefix caching API" ([android/skills][skill]).
- **Settings:** temperature, topK, seed, candidateCount, maxOutputTokens ([get started][prompt-start]). Google's
  prompt advice for this model: short prompts, `##` between parts, examples, short output, temperature 0.2 for
  fixed tasks ([prompt design][prompt-design]).
- **Uses Google itself names:** image understanding (tags, a draft post), short translations, guided
  summarization, entity extraction, intelligent document scanning (OCR first, then the model sorts the lines),
  text classification ([Prompt][prompt], [Prompt alpha post][prompt-alpha]).

### 1.3 Devices and model versions

| Model | Devices Google lists | Notes |
| --- | --- | --- |
| nano-v2 | Older partner phones (Honor Magic 7, OnePlus 13, Xiaomi 15, Galaxy Z Fold7 and others) | |
| nano-v3 | Pixel 9 and 10, Galaxy S26, OnePlus 15, **Lenovo tablets**, and others | The Lenovo Googlebook 15 reports `nano-v3` (device). No Googlebook is named in the list as fetched |
| nano-v4 | Pixel 11 series, Galaxy Z Flip8 and Fold8 | Built on Gemma 4 |

Source: [overview][genai] (last updated 28 September 2026).

**What nano-v4 would change** (not on the Googlebooks today): Gemma 4 "natively supports over 140 languages", is
"up to 4x faster than previous versions and uses up to 60% less battery", and is better at OCR tasks: "chart
understanding, visual data extraction, and handwriting recognition" ([AICore Developer Preview post][aicore-post]).
Gemma 4 lets a picture use a budget of 70 to 1,120 tokens and keeps its shape, where Gemma 3n has a fixed 256
([Gemma 4 model card][gemma4], [Gemma 3n model card][gemma3n]). On the Lenovo the FAST and PREVIEW variants
report unavailable (device).

### 1.4 Limits, quotas and error codes

- Only the app in front is served: "GenAI API inference is permitted only when the app is the top foreground
  application"; otherwise `BACKGROUND_USE_BLOCKED` ([overview][genai]).
- Two quota errors: `BUSY` (too many requests in a short time) and `PER_APP_BATTERY_USE_QUOTA_EXCEEDED` (a
  longer-term battery budget) ([overview][genai]). No numbers are published. A way round the quota for testing
  exists only on Pixel devices ([developer preview page][aicore-dev]).
- Not supported with an unlocked bootloader ([summarization][sum] and the other API pages).
- The full list of codes ([error codes][errors]). New ones worth handling since the list in `on-device-ai.md`:

| Code | Value | Meaning |
| --- | --- | --- |
| `INVALID_INPUT_IMAGE` | -102 | The image could not be read |
| `REQUEST_TOO_SMALL` | -100 | "Use a longer input" (the summarizer's 400 characters, for one) |
| `REQUEST_PROCESSING_ERROR` | 4 | The request did not pass a policy check |
| `RESPONSE_PROCESSING_ERROR` | 11 | The answer did not pass a policy check |
| `RESPONSE_GENERATION_ERROR` | 15 | The model could not answer because of a policy check |
| `NOT_SUPPORTED` | 16 | The engine does not do this |
| `CACHE_PROCESSING_ERROR` | -103 | The cached prefix failed |
| `STRUCTURED_OUTPUT_REQUEST_ERROR`, `…_RESPONSE_ERROR`, `…_MAX_TOKENS_ERROR` | -104, -105, -106 | Typed output failed |
| `AUDIO_BUFFER_OVERFLOW` | -107 | Speech only |

  A screenshot or a clipboard text can trip the policy checks (4, 11, 15) through no fault of the user. The row
  needs a plain "the model would not answer this" state.

### 1.5 Terms that matter (beyond the 18+ term)

From the [ML Kit GenAI terms][genai-terms] (last updated 14 May 2025): no use "in clinical practice, to provide
medical advice"; the output is "not a substitute for advice from a qualified professional" in medical, legal or
financial matters; the safety measures may not be bypassed; the developer must tell users "about Google's
processing of metrics data"; services marked preview may not be used in production. For Booklight: an `explain`
or "what is this" answer about a medical or legal text should stay a plain explanation with the web row beside
it, and the PREVIEW model variant is not for the release build.

### 1.6 What changed since `on-device-ai.md`

That file is from the same day and already has the right versions. What it does not yet say:

1. The Prompt API takes **several images** in one request, and an image may be given as a `Uri` or bytes.
2. The output limit is 4,096 tokens since beta3.
3. Google recommends **OCR before the model** for text in pictures.
4. The new error codes above, the three policy-check codes in particular.
5. The generative ("advanced") speech mode is Pixel 10 and 11 only.
6. Its table "sounds good, not worth it" lists "describing or reading a screenshot" because Image Description is
   English only and Booklight takes no images. Both reasons fall away with OCR plus the Prompt API; the third
   reason (Magic Pointer and Circle to Search own this) still stands and is a product call.

---

## 2. Getting a picture of the screen when the panel opens

### 2.1 The routes, ranked by how little they ask of the user and of Play review

| # | Route | What it gives | What the user does and sees | Permission or role | Play | Desktop windows (Android 16/17) |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **Screenshot on the clipboard**, read by `clip` | The pixels the user captured (whole screen or a region, the user's choice) | Takes a screenshot, opens the panel. Booklight sees "an image, copied 5 s ago" without reading it; reading it shows the "pasted from your clipboard" message once | None | Nothing | Depends on the system's screenshot tool copying to the clipboard: ChromeOS does ("Screenshots and recordings are automatically copied to your clipboard"); **the Googlebook is not verified** |
| 2 | **Share to Booklight** from the screenshot preview or any app | That image | Screenshot, Share, Booklight: about three steps | None (an `ACTION_SEND` filter for `image/*`) | Nothing | Works like the text hand-over Booklight already has (inferred) |
| 3 | **Photo picker** | The image the user picks; newest first, so a fresh screenshot is the first tile | A row "Pick an image", then one click in the system picker | None | Nothing. It is what Play asks apps to use | The picker is another window: the panel loses focus while it is up, so the panel must survive that |
| 4 | **Drag and drop** onto Booklight | The dragged image | Drags a thumbnail or file onto Booklight's window | None | Nothing | Only onto the Booklight window or the pinned window: the panel closes on a click outside it (device) |
| 5 | Screenshot detection callback | **No image.** Only "a screenshot was just taken while my window was visible" | A system toast tells the user the app noticed | `DETECT_SCREEN_CAPTURE` (normal) | Nothing | Only a trigger for route 3. Hardware-key screenshots only |
| 6 | **Default digital assistant** | A bitmap of the display's windows, plus the text and structure of every visible window, plus what each app offers (a web address, for one). No picture of Booklight itself if the system starts the session | Once: Settings, Apps, Default apps, Digital assistant, choose Booklight, confirm a warning. After that nothing: no dialog, no indicator documented | The assistant role. On Android 17 also `READ_ASSIST_STRUCTURE_SCREEN_CONTENT` (normal) and three manifest flags | No declaration form found. Screen content is sensitive data: an in-app disclosure is prudent | The text arrives once per visible window ("a free-form window" is named in the API). The bitmap is of the whole display, not one window |
| 7 | Accessibility service | A bitmap of the display or of one window (`takeScreenshotOfWindow`), and the text tree of every window | Once: Settings, Accessibility, switch on, confirm a warning. After that nothing | An accessibility service | **Declaration form, in-app disclosure and consent.** Not an accessibility tool, so no exemption | Per-window capture exists. At most one shot every 333 ms; secure windows refuse |
| 8 | Notes role, "capture content for note" | A screenshot the user framed and edited in a system screen | Once: make Booklight the default notes app. Then a system capture screen every time | The notes role | Nothing found | Not verified |
| 9 | MediaProjection | The whole screen or one app, the user's choice in the dialog | **A consent dialog every time**, a status-bar chip while it runs | A foreground service of type `mediaProjection` | Foreground-service declaration with a video | One app or the whole screen |
| 10 | Newest screenshot from MediaStore | The file | A runtime permission dialog | `READ_MEDIA_IMAGES` (runtime) | Photo and video policy: only for apps whose core is a gallery; otherwise "use the picker" | Works, but Play would refuse it |
| – | Not open to an app from Play | | | Contextual search (Circle to Search's platform hook), reading the frame buffer, the platform's on-device intelligence service | | |

Sources for each row are in 2.2. The one-time warnings in rows 6 and 7 are as I know the platform; they were
not re-checked today.

**Reading the table.** Routes 1 to 4 fit Booklight's rule as it stands. Route 1 is the lightest if the
Googlebook's screenshot key copies to the clipboard, which needs one look at the device. Routes 6 and 7 are the
only ones that capture *at the moment the panel is engaged* with nothing for the user to do, and both are a new
kind of permission: the owner's decision. Route 9 cannot be made silent. Route 10 is not allowed.

### 2.2 The routes in detail

**Clipboard (route 1).** A clip may hold a content URI to an image; when the focused app reads the clip the
system grants it read access to that URI (source: `addActiveOwnerLocked` and `grantUriPermission` in
[`ClipboardService`][clipsvc]). The description of the clip (its MIME type and timestamp) can be read first
without the paste message (section 4). On ChromeOS a screenshot is copied to the clipboard automatically
([Chromebook help][cros-shot]); whether the Googlebook does the same is not verified. AOSP's own screenshot
preview offers Share and Edit, not a copy (not verified today).

**Share (route 2).** An activity with an intent filter for `ACTION_SEND` and `image/*` receives the image as a
content URI in `EXTRA_STREAM` ([receiving shared data][share-recv]). No permission. Booklight already receives
text this way.

**Photo picker (route 3).** Needs no permission; shows media "sorted by date from newest to oldest"; access
lasts until the device restarts or the app stops ([photo picker][picker]). It is what Play's policy points to
for "one-time or infrequent access" ([photo and video permissions policy][play-photo]).
`READ_MEDIA_VISUAL_USER_SELECTED` is not a way round: it is a runtime permission and has to be requested
together with `READ_MEDIA_IMAGES` ([partial access][partial]).

**Drag and drop (route 4).** The source app must start the drag with `DRAG_FLAG_GLOBAL` and
`DRAG_FLAG_GLOBAL_URI_READ`; the target calls `requestDragAndDropPermissions()` and reads the URI; no manifest
permission ([drag and drop in multi-window][dnd]). `OnReceiveContentListener` handles a paste, a drop and a
keyboard image with one piece of code, and "Read permissions are granted and released automatically"
([receive rich content][rich]).

**Screenshot detection (route 5).** `Activity.registerScreenCaptureCallback` with the install-time permission
`DETECT_SCREEN_CAPTURE`. "The callback doesn't provide an image of the actual screenshot." It fires only for
hardware-key screenshots while the activity is visible, and the user gets a system toast
([screenshot detection][shot-detect]). Useful only as a cue: "you just took a screenshot, ask about it?".

**Default digital assistant (route 6).** What the platform does (source, `android17-release`):

- The assistant's session gets `onHandleScreenshot(Bitmap)` and `onHandleAssist(AssistState)` with the app's
  `AssistStructure` (text and view tree), `AssistContent` and extra data. `onHandleAssist` "is called for all
  activities"; the older variant's text names "a free-form window, a picture-in-picture window, or another window
  in a split-screen display" ([`VoiceInteractionSession`][vis]).
- The bitmap is taken of the display the top activity is on, as one capture of the window layer, not of one
  window ([`WindowManagerService.requestAssistScreenshot`][wms]). It "May be null if screenshots are disabled by
  the user, policy, or application"; a window marked secure gives an empty structure
  ([`VoiceInteractionSession`][vis], [assistant guide][assist-guide]).
- The role can be held by an ordinary app: the role file asks for a voice interaction service with
  `supportsAssist`, a session service and a recognition service, or an activity for `ACTION_ASSIST`
  ([`roles.xml`][roles]). On the HP, ChatGPT and Firefox already answer the assist intent (device). Only the
  service form receives the screenshot and the structure.
- The role is `requestable="false"` ([`roles.xml`][roles]): an app cannot ask for it with a dialog. The user
  goes to Settings › Apps › Default apps and picks it ([assistant guide][assist-guide]). Two user switches, "Use
  text from screen" and "Use screenshot", default to on ([`VoiceInteractionSessionConnection`][visc]).
- **New in Android 17, for an assistant that targets it:** nothing is handed over by default any more. The
  service's manifest entry must set `usesAssistData`, `usesAssistScreenshots` and
  `usesAssistStructureScreenContent`, the app must hold the new normal permission
  `READ_ASSIST_STRUCTURE_SCREEN_CONTENT`, and it must ask with the flag
  `SHOW_WITH_ASSIST_STRUCTURE_SCREEN_CONTENT` ([`VoiceInteractionManagerService`][vims],
  [platform manifest][manifest], [`attrs.xml`][attrs]). None of this is on the Android 17 developer pages yet
  ([features][a17-feat], [behaviour changes][a17-changes]).
- **Starting the session oneself.** The assistant's own service may start a session and still get screen data
  only after a hotword, during a media projection, or when "The voice interaction service's application is
  already in the foreground" ([`VoiceInteractionManagerService`][vims]). So an open panel could ask, but the
  picture would then include the panel. The clean picture comes when the system starts the session from the
  assistant key or gesture, before Booklight draws anything.
- The source also holds, behind a flag, a per-assistant "read screen context" app-op with a consent request an
  assistant can make up to ten times ([`VoiceInteractionManagerService`][vims]). Whether the Googlebook's build
  has it on is not verified.

What it costs: Booklight replaces Gemini on Action + Space, the Assistant key and the status-bar chip (device);
the chosen assistant's service "is kept always running by the system"
([`VoiceInteractionService`][vis-service]), against "nothing of Booklight's own in the background"; and the
three services are new manifest entries. Play: the permissions
policy page has sections on accessibility, photos, foreground services and file access and none on assistants
([Play permissions policy][play-perms]); the user data policy counts "other sensitive device or usage data" and
asks for a prominent disclosure when use "may not be within the reasonable expectation of the user"
([Play user data policy][play-userdata]).

**What Chrome hands an assistant** (source, Chromium `main`): the page's address through
`AssistContent.setWebUri`, and the page's text as a virtual view structure built from its accessibility tree;
neither in incognito ([`ChromeActivity`][chrome-activity], [`WebContentsAccessibilityImpl`][chrome-wcai]).

**Accessibility service (route 7).** `takeScreenshot(displayId, …)` and `takeScreenshotOfWindow(windowId, …)`
need `canTakeScreenshot` in the service's metadata; the platform's own text says both "can be used for machine
learning-based visual screen understanding"; requests closer together than 333 ms fail; a secure window gives
`ERROR_TAKE_SCREENSHOT_SECURE_WINDOW` ([`AccessibilityService`][a11y-src]). Play: an app with an accessibility
service must complete a declaration in Play Console; only apps "designed to support people with disabilities"
may call themselves an accessibility tool; all others need an in-app disclosure and affirmative consent; and the
policy does not let the service be used to plan and carry out actions on its own, only to follow fixed rules a
person set ([Play accessibility policy][play-a11y], [Play permissions policy][play-perms]). Android's own guide: "Only build
an accessibility service if you are creating a general-purpose assistive tool" ([accessibility service
guide][a11y-guide]). It is also a service the system keeps running. Alex's device notes name three of his apps
that already hold such a service (BentoBar, Welcome, StudioSnap); one of them handing a capture to Booklight
would keep Booklight itself clean. That is an idea to weigh, not a checked fact about those apps.

**Notes role (route 8).** `ACTION_LAUNCH_CAPTURE_CONTENT_ACTIVITY_FOR_NOTE` opens a system screen in which the
user frames a screenshot; "User interaction is required"; the permission is "Intended for use by ROLE_NOTES
only", and that role is not requestable either ([`Intent`][intent-src], [platform manifest][manifest],
[`roles.xml`][roles]). Keep holds the notes intent on the HP (device).

**MediaProjection (route 9).** "Your app must ask the user to give consent before each capture session"; a
token works once ([Android 14 changes][a14]). The user chooses one app or the whole screen; one-app sharing
leaves out the status bar and other apps ([media projection][mp]). Apps targeting Android 14 need a foreground
service of type `mediaProjection` ([media projection][mp]), and Play wants a declaration for each foreground
service type with a description and a video ([Play foreground service policy][play-fgs]). From Android 15 QPR1
a status-bar chip shows while it runs, it stops at the lock screen, and notification content and password
fields are hidden from the capture ([Android 15 changes][a15]). The dialog every time rules it out for "when the
panel opens".

**MediaStore (route 10).** "Apps … may only request the READ_MEDIA_IMAGES and READ_MEDIA_VIDEO permissions if
system pickers (like the Android Photo Picker), are not sufficient for your app to provide core functionality"
([photo and video permissions policy][play-photo]).

**New in Android 16 and 17, checked and not useful here.** AppFunctions let an app offer functions to
assistants; calling them needs `EXECUTE_APP_FUNCTIONS`, and they carry no screen content
([AppFunctions][appfunctions]). Handoff moves an activity to another device ([Android 17 features][a17-feat]).
The contextual-search permission is `signature|privileged` ([platform manifest][manifest]). The Googlebook
developer page names no screen-context API for apps ([Googlebook][gb-dev]).

### 2.3 What to check on a device before deciding

1. Does the Googlebook's screenshot key put the image on the clipboard? (Decides between route 1 and route 2.)
2. Does the screenshot preview offer Share, and does Booklight appear there?
3. `countTokens()` for a request with one image: is it about 256?
4. One whole-screen screenshot given to the model as a picture with "what does the error say?", against the
   same screenshot through OCR and a text prompt.

---

## 3. Prior art: asking about what is on the screen

| Product | What the user does | What it answers with | Where it runs | How it gets the screen |
| --- | --- | --- | --- | --- |
| **Circle to Search** | Holds the Home button or handle, then circles, highlights or taps | Search results, translation, AI Mode follow-ups | Google's servers | A system feature (not open to apps) |
| **Gemini "Ask about screen"** | Opens Gemini over an app; a chip "Ask about…" appears | A chat answer about the screen, page or file | Cloud: "Gemini will upload the info on your screen" | The assistant role: the user must have "Use text from screen" and "Use screenshot" on |
| **Magic Pointer** (Googlebook) | Wiggles the pointer, selects something | Gemini with suggested actions | Not stated | System |
| **Pixel Screenshots** | Takes screenshots as usual; later asks the app | Finds the screenshot and the fact in it (a door code) | Not verified today (Google's posts say Tensor G4 runs Gemini Nano "with Multimodality") | Not verified (a Pixel-only app) |
| **Apple visual intelligence, iOS 26** | Presses the screenshot buttons | Ask ChatGPT, search Google or Etsy, highlight an object, **add an event** with "the date, time, and location" pulled out | Mixed; not stated per action | System |
| **Windows Click to Do** | Win + Q or Win + click | On text: copy, search, summarize, bulleted list, rewrite. On images: visual search, blur, erase | On the device | "takes a screenshot … and analyzes it"; **OCR finds the text, the small model (Phi Silica) gets the selected text** |
| **Windows Recall** | Nothing; later searches in plain words or a timeline | The moment and the app it was in | On the device | A snapshot "every few seconds", OCR, opt-in, Windows Hello |
| **Raycast Screen Awareness** | A hotkey, then a question | A chat answer | Cloud models | "The readable text in the window, taken from the system accessibility layer", plus a screenshot of the focused window; needs the Accessibility permission, Screen Recording optional |
| **ChatGPT desktop "work with apps"** | Picks an open app in the chat bar | Answers about the editor or terminal content | Cloud | Not verified today (OpenAI's page refused the request) |
| **Screenpipe** | Nothing; later searches or lets an agent read | Search over everything seen | On the device | A screenshot on each change, paired with the accessibility tree, "falling back to OCR" |
| Rewind | | | | Not verified: its site now hosts something else |

Sources: [Circle to Search help][cts-help], [Circle to Search, July 2025][cts-2025],
[Gemini screen actions][gemini-screen], [Googlebook intelligence][gb-intel], [Pixel 9 post][pixel9],
[Pixel 9 Pro post][pixel9pro], [Apple, June 2025][apple-ai-news], [Apple Intelligence][apple-ai],
[Click to Do][click-to-do], [Manage Click to Do][click-to-do-it], [Recall][recall],
[Raycast Screen Awareness][raycast-screen], [Screenpipe][screenpipe].

**What people use.** Circle to Search "is now on over 300 million Android devices" (July 2025,
[Google][cts-2025]); Lens has "more than 1.5 billion people" a month ([Google, May 2025][search-io]). In
SellCell's survey of 2,000 US phone owners (late 2024), 82.1 % of Samsung owners had used Circle to Search, the
most-used AI feature by a wide margin, yet 87 % said the AI features add little or no value
([SellCell][sellcell]). No breakdown of *what* people ask about their screen was found. What the products
themselves put first is a fair proxy: translate what is on screen, look something up, pull a date into the
calendar, summarise a long text, copy text out of a picture.

**The pattern worth copying.** Every product that answers well reads the screen **as text** (the accessibility
tree, the assist structure, or OCR) and uses the pixels as an extra. Click to Do is the closest relative of
what Booklight could build: a screenshot when invoked, OCR on the device, and a small on-device model that only
ever sees text.

### 3.1 A small model and a screenshot

| Task | The picture straight to nano-v3 | OCR first, then a text prompt |
| --- | --- | --- |
| "What is this a picture of?" | Good: this is what Google lists first ("classification … tags") | Not possible |
| Read small text (a dialog, a terminal, a web page) | Poor: at 768 pixels a side, a 2880 × 1800 screen is shrunk 3.75 times; body text some 21 pixels high ends up under 6 (inferred from the Gemma 3n card and the Lenovo's screen) | Good: exact characters, with a box for each line. Latin, Chinese, Devanagari, Japanese, Korean |
| Exact numbers from a table | Poor, and wrong numbers look right | Good for the cells; rows and columns come from the boxes' positions, without any model |
| A chart | The gist ("sales rise"); not the values | Only the labels |
| Where a button is | No | The words' boxes, yes |
| Speed | 0.6 s more for the picture on a Pixel 10 Pro | OCR, then the usual 0.3 s to the first word (device) |
| Cost in tokens | About 256 per picture (inferred) | A screen full of text is perhaps 1,000 to 2,000 tokens (my estimate): inside the 4,000 limit |
| Wrong answers | It makes text up. Even on plain text the model turned "Googlebooks" into "Google Books" (device) | The model can still misread the OCR text, but the row can show the OCR text beside the answer |

Sources: [Gemma 3n model card][gemma3n], [Prompt alpha post][prompt-alpha], [Google, August 2025][nano-aug],
[Text Recognition v2][ocr], [Text Recognition on Android][ocr-android], `device-findings.md`.

**ML Kit Text Recognition v2**: on the device; returns blocks, lines, words and symbols, each with "bounding
boxes, corner points, rotation information, confidence score, recognized languages and recognized text"
([Text Recognition v2][ocr]). Bundled Latin model `com.google.mlkit:text-recognition:16.0.1`, "about 4 MB size
increase per script per architecture"; or through Play services, "about 260 KB". "each character should be at
least 16x16 pixels" ([Text Recognition on Android][ocr-android]). On the HP (1920 × 1200) small interface text
is near that floor; enlarging the picture before OCR may be needed (inferred). It is an ML Kit library, so it
brings the same usage reporting as the GenAI library; its manifest was not opened today.

For an Android 17 Googlebook nano-v4 would move the left column (section 1.3), but the device has nano-v3.

---

## 4. Acting on the clipboard

### 4.1 Android's rules

| Rule | Detail | Sure |
| --- | --- | --- |
| Who may read | Only the app with window focus, or the default keyboard. Anything else gets nothing | source: [`ClipboardService`][clipsvc]; [secure clipboard handling][clip-secure] |
| The message | Android 12+: on `getPrimaryClip()` the system shows "APP pasted from your clipboard". Not when the clip is the app's own, not for the keyboard, and only once per clip per app | docs: [copy and paste][clip-docs]; source |
| **Looking without the message** | `getPrimaryClipDescription()` and `hasPrimaryClip()` do not show it. The docs: no toast when the app "Retrieves metadata for the clip object, such as by calling `getPrimaryClipDescription()`" | docs + source |
| What the description holds | MIME types (text, HTML, an image URI), the label, `getTimestamp()` (when it was copied, wall-clock time), `isStyledText()`, the sensitive flag in its extras, and the classification below | source: [`ClipDescription`][clipdesc] |
| Classification | For a text clip the system itself runs the text classifier and stores, per entity type, the highest confidence. `getClassificationStatus()` is `CLASSIFICATION_COMPLETE` (3), `CLASSIFICATION_NOT_COMPLETE` (1) or `CLASSIFICATION_NOT_PERFORMED` (2); `getConfidenceScore(TextClassifier.TYPE_URL)` and so on give 0 to 1 and throw unless the status is complete | source: [`ClipDescription`][clipdesc], [`ClipboardService`][clipsvc] |
| Its limit | **Only texts of up to 400 characters are classified** (a device setting can change it); longer ones are "not performed" | source: `DEFAULT_MAX_CLASSIFICATION_LENGTH = 400` |
| After reading | `ClipData.Item.getTextLinks()` gives the places of the entities | source: [`ClipDescription`][clipdesc] |
| Sensitive content | An app can mark a clip with `ClipDescription.EXTRA_IS_SENSITIVE`; the system then hides it in its copy preview. Android asks apps to set it for passwords, card numbers and codes. Booklight should not offer actions on such a clip | docs: [copy and paste][clip-docs], [secure clipboard handling][clip-secure] |
| Lifetime | The clip is cleared after one hour by default | source: `DEFAULT_CLIPBOARD_TIMEOUT_MILLIS = 3600000` |
| What the description does not tell | The text's length and its language | source |

**So a zero-state row is possible without touching the text:** when the panel has focus, read the description;
if it is text or an image, not sensitive, and `getTimestamp()` is recent, show one quiet row ("Clipboard · text ·
copied 20 s ago · link"). Tab reads the clip (the message appears once) and opens the options. The language is
known only after reading, so "Translate to English" against "Translate to German" is decided at that point with
`TextClassifier.detectLanguage`, which works on the Lenovo (device).

### 4.2 The system's text classifier

- Entity types (source: [`TextClassifier`][tc-src]): address, date, datetime, email, flight, phone, url, otp,
  dictionary, other. It has a clipboard mode of its own (`WIDGET_TYPE_CLIPBOARD`).
- Calls: `generateLinks` (find entities in a text), `classifyText` (one span, which returns the actions),
  `detectLanguage`, `suggestConversationActions`. Language detection and conversation actions since Android 10
  ([TextClassifier, AOSP][tc-aosp]).
- Actions: `TextClassification.getActions()` returns ready-made system actions with a label and an icon
  ([`TextClassification`][tc-class]). Which ones the Googlebook returns (map, call, add event, track flight,
  translate) has not been probed.
- On the Lenovo: `detectLanguage` gave "de" at 0.998, and `generateLinks` found a date and time, a link and a
  flight number (device). The probe text also held an address and a phone number, which the note does not list:
  recheck those two.

### 4.3 Translation, language detection, entity extraction

| | ML Kit Translation (`translate` 17.0.3) | The model through the Prompt API |
| --- | --- | --- |
| Languages | 59 on the list as fetched (the page says "more than 50") | Not published. German and English both ways worked (device) |
| How | "the same models used by the Google Translate app's offline mode"; pairs without English go through English, which "can affect quality" | One prompt |
| Download | A pack per language, about 30 MB, on demand; `DownloadConditions.requireWifi()`; packs can be listed and deleted | Nothing more; the model is there |
| Quality | "intended for casual and simple translations". On the Lenovo visibly worse German ("Das Treffen zog an Donnerstag") | Right in the probe (device). A model can drift from the source text |
| Speed | Tens of milliseconds once the pack is there (`on-device-ai.md`) | 0.7 to 3.7 s for a line (device) |
| Other costs | APK size (measured in `on-device-ai.md`), an attribution line, pack management | The quota; only while in front |

Sources: [Translation][translate], [Translation on Android][translate-android],
[supported languages][translate-langs], `device-findings.md`.

- **Language ID** (`language-id` 17.0.6): "over one hundred different languages", romanised text for seven
  ([Language ID][langid]). The system's `detectLanguage` already answers on the Lenovo, so the library is not
  needed.
- **Entity Extraction** (`entity-extraction` 16.0.0-beta6): address, date-time, email, flight number, IBAN,
  ISBN, money, payment card, phone, tracking number, URL; 15 languages, German among them; "focuses on precision
  over recognition"; still beta, page last updated July 2024 ([Entity Extraction][entity]). It adds IBAN,
  ISBN, money and tracking numbers over the system classifier. Not worth a library for a launcher.

### 4.4 Prior art

| Product | When it appears | What it offers |
| --- | --- | --- |
| **PopClip** (Mac) | "appears when you select text in any app" | A bar of actions; 252 extensions in its directory, in groups such as text case, text statistics, translation, search engines, AI tools, to-do and calendar apps |
| **Raycast Clipboard History** | A command | Per item: paste as plain text, copy, share, **copy text from an image** (on-device OCR), read a QR code, send to AI chat, edit, save as snippet, pin. Filters by text, images, files, links, emails, colours. Skips password managers |
| **Alfred Universal Actions** | A hotkey on selected text, a file or a link, or from its clipboard history | "over 60" built-in actions; "only shows you the relevant actions for the type of item" |
| **Apple Live Text** | On text in a picture | Copy, translate, look up; and for found data: call, open the site, mail, convert currency or units, track a flight or parcel, add to calendar, directions |
| **Gboard** | Right after a copy, in the suggestion strip | Splits the copied text into pieces to paste: "emails, URLs, phone numbers, numbers, addresses, dates, and times" |
| **PowerToys Advanced Paste** | Win + Shift + V | Paste as plain text, Markdown, JSON, a file; image to text by local OCR; "Paste with AI" with its own examples: **summarize, translate, generate code, transform, stylize**; "Fix spelling and grammar" with a mode that explains the corrections; saved custom actions; local models (Foundry Local, Ollama, Phi Silica) |
| Samsung Smart Select | | Not verified (the page could not be opened) |

Sources: [PopClip][popclip], [PopClip extensions][popclip-ext], [Raycast clipboard history][raycast-clip],
[Alfred Universal Actions][alfred-ua], [Live Text][livetext], [Gboard clipboard][gboard-clip],
[Advanced Paste][adv-paste].

**What people do with text they just copied.** No study was found. Indirect evidence: in Raycast's store
Google Translate has 493,156 installs, in the same range as the most installed extensions shown (Kill Process
768,386; Color Picker 609,384) ([Raycast store][raycast-store], as shown today); the first two examples
Microsoft gives for pasting with AI are summarise and translate ([Advanced Paste][adv-paste]); Gboard and Live
Text both lead with "use the phone number, address or date that is in it". So the order to offer is: act on what
is in it (open the link, the map, the event), translate, then fix or shorten, then summarise if it is long.

### 4.5 What this suggests for Booklight

- **The zero-state row** from the description alone (4.1), only for a clip younger than a few minutes, never
  for a sensitive clip, never for Booklight's own copy. It breaks "nothing typed = nothing shown", so it is the
  owner's call, as he said himself.
- **Tab opens the options**, in this order: what the classifier found (Open link, Open in Maps, Add event,
  Call, Mail), Translate (into the device language, or out of it when the text is already in it), Fix, Shorter,
  Summarise (only above about 400 characters), Explain. An image clip gets: Read the text (OCR), Ask about it.
- The entity actions need no model and are instant. Only the transformations ask the model, on Enter.

---

## 5. Fifteen candidates

Common rules, from the 2.0 design: the model is asked on Enter or after a pause, never per keystroke; its
answer is shown, copied, pinned or put back where the text came from, and never runs anything; a local match
always ranks above it. "Fits" refers to the limits in section 1.2. Feasibility assumes the Lenovo's measured
speed (0.3 s to the first word, 70 to 100 characters a second).

| # | Feature | What the user does | What the model does, and whether it fits | Feasibility on the device | Permission needed | Risk, and how a wrong answer stays harmless | Without the model |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | **Clipboard row with actions** (owner's) | Opens the panel after a copy; a row says what is on the clipboard; Tab shows Translate, Fix, Shorter, Summarise, and what is in it | Translates or rewrites the text. A copied paragraph fits easily; a very long text is cut and the row says so | High. Everything exists except the row and the options | None. The description is read without the paste message | Low. Shown and copied only. Skip sensitive clips | The row itself, and Open link, Map, Add event, Call: all without it |
| 2 | **Ask about a screenshot** (owner's) | Takes a screenshot, opens the panel, `clip` or the row, then types a question; or shares an image to Booklight | Answers from the OCR text; optionally also gets the picture for "what is this" | Medium. Needs the image hand-over, OCR, and a wider answer row | None with routes 1 to 4. ML Kit Text Recognition is a new library | Medium. It can misread or invent. Show "from the text on screen", offer "Copy the text" beside the answer | "Copy the text from this image" is pure OCR and useful alone |
| 3 | **Describe or read out the screen** | `describe` on an image; `read` to hear it | Describes the picture in a sentence or two (the picture goes to the model) | Medium. Image Description is English only; the Prompt API with a picture in other languages is untested | None | Medium for descriptions (invented detail). Label it as a guess | Reading out needs no model: OCR and the system's text-to-speech |
| 4 | **Pull data out of a screenshot**: a table, an address, a date | On an image: "Copy as table", or rows for the address and date found | Little: at most tidies the cells or reads a messy date | Medium for tables (rows and columns from the boxes' positions); high for addresses and dates | None | Medium for tables: a wrong cell is silent. Show the grid before copying | Nearly all of it: OCR, geometry, the text classifier, `When.kt` |
| 5 | **"What is this error"** | Copies or selects an error, `explain` or the clipboard option; or a screenshot of it | Says in two lines what it means and the likely cause | High. Short input, short output | None | Medium: a confident wrong cause. Keep "Search the web for the first line" as the next action | Detecting that it is an error and offering the web search |
| 6 | **Name a file** | `name` with a text or an image | Proposes three short names | High | None | Low. A suggestion to copy | Nothing useful |
| 7 | **Commit message from a diff** | Copies a diff, `commit` | Writes a subject line of up to 72 characters, optionally a body. A large diff does not fit: send the file list and the first hunks, and say so | High for small diffs | None | Low. Text to copy. It may describe the wrong thing on a cut diff | Only a list of the files touched |
| 8 | **Reply drafts** | Selects or copies a message, `reply` | Three short answers (yes, no, later) in the message's language | High | None | Low to medium (tone). The user picks one; Replace puts it back | Nothing (the classic Smart Reply is English only) |
| 9 | **Tone** | `formal …`, `friendly …`, `emojify …`, next to `shorter` | Rewrites. The Rewriting API has exactly these styles in seven languages, under 256 tokens; or a prompt | High | None | Low | Nothing |
| 10 | **Define a word** | `define serendipity` | One line, in the language typed | High | None | Medium: invented meanings for rare words. Web row stays second | The system's dictionary intent, where an app answers it |
| 11 | **Units and time zones in plain words** | "what time is 3 pm Berlin in New York", "2 litres in cups" | Only turns the sentence into {what, value, from, to}. Booklight's own code computes | Medium. Needs JSON from the prompt (typed output is unavailable on the Lenovo) | None | Low if the model never does the sum and the row shows what was understood | A pattern parser covers most phrasings and is instant |
| 12 | **A sentence becomes an event or reminder** | "lunch with Anna next Thursday at one at Borchardt" | Fills in title, day, time, place as written; `When.kt` checks the date and time | Medium, same reason as 11 | None | Medium: a wrong date in a ready row. Show the parsed day large; the calendar's own screen confirms | `When.kt` already does the clear cases; the text classifier finds dates |
| 13 | **Summarise the open web page** | Selects the text and picks Booklight, or copies it, then `sum` | Summarises. 3,000 words fit; longer is cut. The Summarization API is English, Japanese, Korean only, so German goes through a prompt | High for selected or copied text | None for selection and clipboard. Fetching the address itself would stretch "Internet, for suggestions only". The whole page silently: only as the assistant (section 2) | Medium: a summary can drop or bend a point. Say "summary of N words" | Nothing |
| 14 | **Settings in plain words** | "make the screen less blue at night" | Picks one entry from Booklight's own list of settings pages | Medium. One request, a one-word answer | None | Low: the row names the page it opens | A list of other words per page does most of it, instantly. Do that first |
| 15 | **A guess when nothing matches** | Types three or more words that match nothing; after a pause a row "Did you mean: timer 10 min" | Maps the text to one of Booklight's commands and its argument | Medium. One to two seconds for something that looks instant | None | Medium: a wrong command in a ready row. It never runs by itself and its arguments go through Booklight's parsers | Loose matching against the examples in the guide |

**How the page text for 13 could reach Booklight**, in order of cost: the user selects text and chooses Booklight
in the selection menu (exists); the user copies (exists); the user shares the page (the browser shares only the
address, so Booklight would have to fetch it); as the default assistant Booklight would get Chrome's address and
page text with no step at all (source: [`ChromeActivity`][chrome-activity],
[`WebContentsAccessibilityImpl`][chrome-wcai]); a screenshot with OCR gives only what is visible.

**The five strongest**, by value for a keyboard launcher against what they ask:

1. **The clipboard row with actions (1).** The owner's idea, no permission, and most of it works without the
   model.
2. **Ask about a screenshot, through OCR (2)**, with "copy the text from this image" as its first, model-free
   step. It needs one device check (does a screenshot land on the clipboard) and one library decision.
3. **"What is this error" (5).** The most common question about a screen on a laptop, short in and short out,
   and it rides on 1 and 2.
4. **A sentence becomes an event or reminder (12).** The model proposes, `When.kt` decides: the only one the
   Gemini app cannot do for Booklight.
5. **A guess when nothing matches, with settings in plain words inside it (15 and 14).** Picking from a closed
   list is what a small model is reliable at.

Next in line: the commit message (7), small and loved by exactly the people who use a launcher.

---

## 6. Sources

**ML Kit GenAI and Gemini Nano**

- [ML Kit GenAI overview][genai] · [Prompt API][prompt] · [get started][prompt-start] ·
  [structured output][prompt-structured] · [prompt design][prompt-design]
- [Summarization][sum] · [Proofreading][proof] · [Rewriting][rewrite] · [Image description][imgdesc] ·
  [Speech recognition][speech]
- [Release notes][relnotes] · [Error codes][errors] · [Prompt package reference][prompt-pkg] ·
  [`ImagePart`][imagepart] · [`GenerativeModel`][genmodel]
- [ML Kit GenAI terms][genai-terms] · [AICore developer preview][aicore-dev] ·
  [AICore Developer Preview post, April 2026][aicore-post] · [Prompt API alpha post, October 2025][prompt-alpha] ·
  [Gemini Nano with ML Kit, August 2025][nano-aug] · [android/skills][skill]
- [Gemma 3n model card][gemma3n] · [Gemma 4 model card][gemma4] · [Google's Maven index][maven]

**Classic ML Kit**

- [Text Recognition v2][ocr] · [on Android][ocr-android] · [Translation][translate] ·
  [on Android][translate-android] · [languages][translate-langs] · [Language ID][langid] ·
  [Entity Extraction][entity]

**Android documentation**

- [Media projection][mp] · [Android 14 changes][a14] · [Android 15 changes][a15] ·
  [Android 17 features][a17-feat] · [Android 17 changes][a17-changes]
- [Assistant guide][assist-guide] · [Screenshot detection][shot-detect] · [Photo picker][picker] ·
  [Partial photo access][partial] · [Drag and drop][dnd] · [Receive rich content][rich] ·
  [Receiving shared data][share-recv]
- [Copy and paste][clip-docs] · [Secure clipboard handling][clip-secure] · [`TextClassification`][tc-class] ·
  [TextClassifier, AOSP][tc-aosp] · [Accessibility service guide][a11y-guide] · [AppFunctions][appfunctions] ·
  [Googlebook][gb-dev]

**Android 17 source (`android17-release`) and Chromium**

- [`ClipboardService`][clipsvc] · [`ClipDescription`][clipdesc] · [`TextClassifier`][tc-src]
- [`VoiceInteractionSession`][vis] · [`VoiceInteractionService`][vis-service] ·
  [`VoiceInteractionManagerService`][vims] ·
  [`VoiceInteractionSessionConnection`][visc] · [`WindowManagerService`][wms] · [`roles.xml`][roles] ·
  [platform manifest][manifest] · [`attrs.xml`][attrs] · [`AccessibilityService`][a11y-src] · [`Intent`][intent-src]
- [`ChromeActivity`][chrome-activity] · [`WebContentsAccessibilityImpl`][chrome-wcai]

**Google Play policy**

- [Permissions and sensitive APIs][play-perms] · [Accessibility API][play-a11y] ·
  [Foreground services][play-fgs] · [Photo and video permissions][play-photo] · [User data][play-userdata]

**Prior art**

- [Circle to Search help][cts-help] · [Circle to Search, July 2025][cts-2025] · [Search at I/O 2025][search-io] ·
  [Gemini screen actions][gemini-screen] · [Googlebook intelligence][gb-intel] · [Pixel 9 post][pixel9] ·
  [Pixel 9 Pro post][pixel9pro]
- [Apple, June 2025][apple-ai-news] · [Apple Intelligence][apple-ai] · [Live Text][livetext]
- [Click to Do][click-to-do] · [Manage Click to Do][click-to-do-it] · [Recall][recall] ·
  [Advanced Paste][adv-paste]
- [Raycast Screen Awareness][raycast-screen] · [Raycast clipboard history][raycast-clip] ·
  [Raycast store][raycast-store] · [PopClip][popclip] · [PopClip extensions][popclip-ext] ·
  [Alfred Universal Actions][alfred-ua] · [Gboard clipboard][gboard-clip] · [Screenpipe][screenpipe] ·
  [Chromebook screenshots][cros-shot] · [SellCell survey][sellcell]

**This repo**

- `docs/research/on-device-ai.md`
- `docs/research/device-findings.md`

**Could not be opened or confirmed today:** OpenAI's "work with apps" page (refused), Samsung's Smart Select
page, Rewind's own description, Apple's support page for visual intelligence (the newsroom post was used
instead), Google's support page for Pixel Screenshots, the Android reference pages for `TextClassifier`,
`ClipDescription`, `RoleManager` and `VoiceInteractionSession` (the source files were read instead), any figure
for how often Circle to Search is used per person, and any study of what people do with copied text.

[genai]: https://developers.google.com/ml-kit/genai
[prompt]: https://developers.google.com/ml-kit/genai/prompt/android
[prompt-start]: https://developers.google.com/ml-kit/genai/prompt/android/get-started
[prompt-structured]: https://developers.google.com/ml-kit/genai/prompt/android/structured-output
[prompt-design]: https://developers.google.com/ml-kit/genai/prompt/android/prompt-design
[sum]: https://developers.google.com/ml-kit/genai/summarization/android
[proof]: https://developers.google.com/ml-kit/genai/proofreading/android
[rewrite]: https://developers.google.com/ml-kit/genai/rewriting/android
[imgdesc]: https://developers.google.com/ml-kit/genai/image-description/android
[speech]: https://developers.google.com/ml-kit/genai/speech-recognition/android
[relnotes]: https://developers.google.com/ml-kit/release-notes
[errors]: https://developers.google.com/android/reference/kotlin/com/google/mlkit/genai/common/GenAiException.ErrorCode
[prompt-pkg]: https://developers.google.com/android/reference/kotlin/com/google/mlkit/genai/prompt/package-summary
[imagepart]: https://developers.google.com/android/reference/kotlin/com/google/mlkit/genai/prompt/ImagePart
[genmodel]: https://developers.google.com/android/reference/kotlin/com/google/mlkit/genai/prompt/GenerativeModel
[genai-terms]: https://developers.google.com/ml-kit/genai-terms
[aicore-dev]: https://developers.google.com/ml-kit/genai/aicore-dev-preview
[aicore-post]: https://android-developers.googleblog.com/2026/04/AI-Core-Developer-Preview.html
[prompt-alpha]: https://android-developers.googleblog.com/2025/10/ml-kit-genai-prompt-api-alpha-release.html
[nano-aug]: https://android-developers.googleblog.com/2025/08/the-latest-gemini-nano-with-on-device-ml-kit-genai-apis.html
[skill]: https://raw.githubusercontent.com/android/skills/main/device-ai/ml-kit-genai-prompt-api/SKILL.md
[gemma3n]: https://ai.google.dev/gemma/docs/gemma-3n/model_card
[gemma4]: https://ai.google.dev/gemma/docs/core/model_card_4
[maven]: https://dl.google.com/dl/android/maven2/com/google/mlkit/group-index.xml
[ocr]: https://developers.google.com/ml-kit/vision/text-recognition/v2
[ocr-android]: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
[translate]: https://developers.google.com/ml-kit/language/translation
[translate-android]: https://developers.google.com/ml-kit/language/translation/android
[translate-langs]: https://developers.google.com/ml-kit/language/translation/translation-language-support
[langid]: https://developers.google.com/ml-kit/language/identification
[entity]: https://developers.google.com/ml-kit/language/entity-extraction
[mp]: https://developer.android.com/media/grow/media-projection
[a14]: https://developer.android.com/about/versions/14/behavior-changes-14
[a15]: https://developer.android.com/about/versions/15/behavior-changes-all
[a17-feat]: https://developer.android.com/about/versions/17/features
[a17-changes]: https://developer.android.com/about/versions/17/behavior-changes-17
[assist-guide]: https://developer.android.com/training/articles/assistant
[shot-detect]: https://developer.android.com/about/versions/14/features/screenshot-detection
[picker]: https://developer.android.com/training/data-storage/shared/photo-picker
[partial]: https://developer.android.com/about/versions/14/changes/partial-photo-video-access
[dnd]: https://developer.android.com/develop/ui/views/touch-and-input/drag-drop/multi-window
[rich]: https://developer.android.com/develop/ui/views/receive-rich-content
[share-recv]: https://developer.android.com/training/sharing/receive
[clip-docs]: https://developer.android.com/develop/ui/views/touch-and-input/copy-paste
[clip-secure]: https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling
[tc-class]: https://developer.android.com/reference/android/view/textclassifier/TextClassification
[tc-aosp]: https://source.android.com/docs/core/display/textclassifier
[a11y-guide]: https://developer.android.com/guide/topics/ui/accessibility/service
[appfunctions]: https://developer.android.com/ai/appfunctions
[gb-dev]: https://developer.android.com/googlebook
[clipsvc]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/clipboard/ClipboardService.java
[clipdesc]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/content/ClipDescription.java
[tc-src]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/view/textclassifier/TextClassifier.java
[vis]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/service/voice/VoiceInteractionSession.java
[vis-service]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/service/voice/VoiceInteractionService.java
[vims]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/voiceinteraction/java/com/android/server/voiceinteraction/VoiceInteractionManagerService.java
[visc]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/voiceinteraction/java/com/android/server/voiceinteraction/VoiceInteractionSessionConnection.java
[wms]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/wm/WindowManagerService.java
[roles]: https://android.googlesource.com/platform/packages/modules/Permission/+/refs/heads/android17-release/PermissionController/res/xml/roles.xml
[manifest]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml
[attrs]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/res/values/attrs.xml
[a11y-src]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/accessibilityservice/AccessibilityService.java
[intent-src]: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/content/Intent.java
[chrome-activity]: https://chromium.googlesource.com/chromium/src/+/main/chrome/android/java/src/org/chromium/chrome/browser/app/ChromeActivity.java
[chrome-wcai]: https://chromium.googlesource.com/chromium/src/+/main/content/public/android/java/src/org/chromium/content/browser/accessibility/WebContentsAccessibilityImpl.java
[play-perms]: https://support.google.com/googleplay/android-developer/answer/9888170
[play-a11y]: https://support.google.com/googleplay/android-developer/answer/10964491
[play-fgs]: https://support.google.com/googleplay/android-developer/answer/13392821
[play-photo]: https://support.google.com/googleplay/android-developer/answer/14115180
[play-userdata]: https://support.google.com/googleplay/android-developer/answer/10144311
[cts-help]: https://support.google.com/websearch/answer/14508957
[cts-2025]: https://blog.google/products/search/circle-to-search-ai-mode-gaming/
[search-io]: https://blog.google/products/search/google-search-ai-mode-update/
[gemini-screen]: https://support.google.com/gemini/answer/15850607?hl=en
[gb-intel]: https://blog.google/products-and-platforms/devices/googlebook/googlebook-built-in-intelligence/
[pixel9]: https://blog.google/products/pixel/google-pixel-9-new-ai-features/
[pixel9pro]: https://blog.google/products/pixel/google-pixel-9-pro-xl/
[apple-ai-news]: https://www.apple.com/newsroom/2025/06/apple-intelligence-gets-even-more-powerful-with-new-capabilities-across-apple-devices/
[apple-ai]: https://www.apple.com/apple-intelligence/
[livetext]: https://support.apple.com/guide/iphone/use-live-text-iphcf0b71b0e/ios
[click-to-do]: https://support.microsoft.com/en-us/windows/click-to-do-do-more-with-what-s-on-your-screen-6848b7d5-7fb0-4c43-b08a-443d6d3f5955
[click-to-do-it]: https://learn.microsoft.com/en-us/windows/client-management/manage-click-to-do
[recall]: https://support.microsoft.com/en-us/windows/retrace-your-steps-with-recall-aa03f8a0-a78b-4b3e-b0a1-2eb8ac48701c
[adv-paste]: https://learn.microsoft.com/en-us/windows/powertoys/advanced-paste
[raycast-screen]: https://manual.raycast.com/ai/screen-awareness
[raycast-clip]: https://manual.raycast.com/clipboard-history
[raycast-store]: https://www.raycast.com/store
[popclip]: https://www.popclip.app/
[popclip-ext]: https://www.popclip.app/extensions/
[alfred-ua]: https://www.alfredapp.com/universal-actions/
[gboard-clip]: https://support.google.com/gboard/answer/10742542
[screenpipe]: https://github.com/mediar-ai/screenpipe
[cros-shot]: https://support.google.com/chromebook/answer/10474268
[sellcell]: https://www.sellcell.com/blog/iphone-vs-samsung-ai-survey/
