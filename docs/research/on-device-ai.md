# Booklight: on-device AI

*Desk research, 1 October 2026. Read for it: this repo (`CLAUDE.md`, `docs/PLAN.md` §6 and §11, `PRIVACY.md`,
`store-submission/forms/data-safety.md`, `docs/research/permissions.md`, `next-features.md` §2.7, the 2.0 spec §9,
`core/` and the overlay code), Google's ML Kit and Android AI pages, AOSP `android17-release`, and the libraries
themselves: the AARs and POMs were downloaded from Google's Maven and opened. No device was touched, no code was
changed, nothing was built.*

**How sure each claim is.** *measured* = read today from the artifact, the AOSP source or this repo. *docs* = read on
the vendor's page today. *reported* = a secondary source. *inferred* = my reading of evidence. *unverified* = could
not be confirmed; needs the probe in §5.

---

## 1. The short answer

**Yes, there is an easy way, if the Googlebook lets ordinary apps in.** Gemini Nano runs in a system service
(AICore). An app adds one library (`com.google.mlkit:genai-prompt`), asks "is it there?", and sends a prompt; the
answer streams back. No model to ship, no model to host, no runtime permission. About thirty lines of Kotlin.

Whether it works on a Googlebook is **not documented**. Google's supported-device list names phones and two Lenovo
tablets, no laptop (docs, checked today). The Lenovo Googlebook has an Intel production build of AICore and the
matching system features, so it probably does. One debug build answers it (§5).

What "easy" costs:

| Cost | Detail | Sure |
| --- | --- | --- |
| Dependencies | `genai-prompt` 1.0.0-beta4 pulls in ML Kit `common`, `play-services-basement` and `-tasks`, Firebase encoders and components, AndroidX AppCompat, and Google's telemetry transport (`datatransport`). Booklight has none of these today. | measured (POMs) |
| APK size | No native code in the GenAI libraries. Estimate 0.5 to 1.5 MB after R8 on today's 2.5 MB release APK. | AAR sizes measured; the result after R8 is unverified |
| Permissions | Two new manifest lines arrive by merge: `com.google.android.apps.aicore.service.BIND_SERVICE` (AICore's own) and `android.permission.ACCESS_NETWORK_STATE` (from the telemetry transport). Neither prompts. Both still need Alex's say-so under the rule in `CLAUDE.md`. | measured (AAR manifests) |
| **Telemetry** | Every ML Kit library, GenAI included, sends usage metrics to Google: device model and OS build, Booklight's package name and version, a per-installation identifier, latency, input and output **size**, event types, error codes, configured languages. Not the text. It uploads from a job service in Booklight's own process. There is no documented switch. | docs + measured |
| Terms | The ML Kit GenAI terms forbid apps "directed towards or … likely to be accessed by individuals under the age of 18". Booklight's Play target audience is "13 and over". | docs; what Play would require is unverified |
| Stability | Prompt, Summarization, Proofreading, Rewriting and Image Description are beta; Speech and structured output are alpha. "Not subject to any SLA or deprecation policy." | docs |
| Review | No Play declaration form found for on-device inference. The data-safety form changes because of the telemetry (§6). | docs |

The telemetry and the job service are the real price. They collide with three sentences Booklight publishes today:
"no analytics", "nothing that runs in the background", and "Internet, for suggestions only". §6 sets out the
choices.

**Two system APIs cost nothing and nobody mentioned them before:** `android.view.translation.TranslationManager`
(an on-device translator any app may ask for, if the device ships one) and
`android.view.textclassifier.TextClassifier` (language detection and entities). No library, no telemetry, no
permission. Whether the Googlebook's system services answer them is unverified; the probe covers both.

---

## 2. The options compared

| | What it can do | Without network | Needs a download | APK size | Permission | Devices | Latency | Effort | Risks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **AICore through ML Kit GenAI** | Free-form prompts with text or an image in, text or a typed object out; purpose-built proofread, rewrite, summarise, describe image, speech to text | Yes, once the model is on the device | The model, by AICore, once for all apps (2 to 7 GB reported on phones). Booklight downloads nothing itself | +0.5 to 1.5 MB (estimate) | AICore `BIND_SERVICE` + `ACCESS_NETWORK_STATE`, both silent | Only where AICore serves third parties. Googlebooks: unverified | Seconds, streamed. Phones: about 10 to 19 tokens a second out (reported) | S for a probe, M for the first feature, S for each one after | Beta. Telemetry. Per-app quota. Works only while Booklight is the top app. 18+ term. May be unavailable on the HP |
| **System APIs** (`TranslationManager`, `TextClassifier`) | Translate text; detect language; find dates, addresses, phone numbers, flight numbers, links | Yes | Language packs, by the system's own settings page | 0 | None | Any device whose system ships the services. Googlebooks: unverified | Tens of milliseconds (inferred: same engines as the selection toolbar and Live Translate) | S | May return nothing on this OS build; no say over quality or languages |
| **Classic ML Kit** | Translate (59 languages), identify language, smart reply (English only), entity extraction | Yes, after the pack | Translate: 31 to 48 MB per language, by Booklight over its own `INTERNET` | Translate: +7 MB download on Play per device; the GitHub APK grows by about 34 MB (two ABIs) to 63 MB (all four). Language ID +1 MB. Smart reply +6 MB. Entities +3 to 5 MB | `INTERNET` (held) + `ACCESS_NETWORK_STATE` | Everywhere, x86_64 included | Tens of milliseconds | M (pack management, a settings page) | Same telemetry. Attribution duty for translations. APK size ×10 or more |
| **Own model** (LiteRT-LM + Gemma) | Anything a 0.3 to 4 GB model can do; function calling | Yes, after the model | 0.3 to 3.7 GB, by Booklight, from a server someone runs | +10 MB download per device; APK +22 to 26 MB per ABI stored | None. No telemetry | Everywhere with enough RAM | Intel Lunar Lake laptop, Gemma 4 E2B: 30 tokens a second on CPU, 48 on GPU; first token after 2.4 s (CPU) or 0.3 s (GPU) | L | Model lives in Booklight's process: seconds to load on each cold open, up to 3.5 GB RAM. Hosting. Licence notices. Battery |
| **Hand-over to the Gemini app** (today) | Everything Gemini can do | No | No | 0 | None | Any device with the Gemini app | App start plus the cloud | Done | Leaves the panel. The answer never comes back to Booklight. Needs an account and a network |

Sources for the numbers are in §3. "Per ABI stored" assumes AGP's default for minSdk 34, where native libraries
sit uncompressed in the APK (unverified until built).

---

## 3. What the research found

### 3.1 Gemini Nano, AICore and the ML Kit GenAI APIs

**What exists** (Google's Maven index and the ML Kit pages, today):

| API | Artifact | Status | Input limit | Languages | German |
| --- | --- | --- | --- | --- | --- |
| Prompt | `genai-prompt` 1.0.0-beta4 | beta | 4,000 tokens in (about 3,000 English words); "long output (more than 4K tokens) should be avoided" | "may vary depending on the particular device's configuration"; no list published | unverified |
| Structured output | `genai-schema`, `genai-schema-compiler` 1.0.0-alpha1 (KSP 2.3.6+) | alpha | as Prompt | as Prompt | unverified |
| Proofreading | `genai-proofreading` 1.0.0-beta1 | beta | under 256 tokens | English, Japanese, French, German, Italian, Spanish, Korean | **yes** |
| Rewriting | `genai-rewriting` 1.0.0-beta1 | beta | under 256 tokens | the same seven | **yes** |
| Summarization | `genai-summarization` 1.0.0-beta1 | beta | under 4,000 tokens; an article must exceed 400 characters | English, Japanese, Korean | **no** |
| Image description | `genai-image-description` 1.0.0-beta1 | beta | one image | English | no |
| Speech recognition | `genai-speech-recognition` 1.0.0-alpha1 | alpha | microphone or 16 kHz PCM | 15 locales in basic mode, de-DE in beta | yes, but it needs the microphone |

There is no GenAI translation artifact. Google lists "translations" as a use of the Prompt API.

**The Prompt API as shipped in beta4** (measured: `javap` on the AAR's classes; the names differ slightly from
Google's April blog post, which wrote `releaseTrack`):

```kotlin
val model: GenerativeModel = Generation.getClient()                       // or getClient(generationConfig { … })
model.checkStatus()            // suspend → FeatureStatus: UNAVAILABLE 0, DOWNLOADABLE 1, DOWNLOADING 2, AVAILABLE 3
model.download()               // Flow<DownloadStatus>: DownloadStarted(bytesToDownload), DownloadProgress, DownloadCompleted, DownloadFailed(e)
model.warmup()                 // loads the model so the first answer is faster
model.getBaseModelName()       // e.g. which nano version
model.getTokenLimit()
model.isStructuredOutputFeatureAvailable(); model.isSystemPromptAvailable()
model.isCachingFeatureAvailable();          model.isThinkingModeAvailable()
model.countTokens(request).totalTokens
model.generateContent(request)              // suspend → GenerateContentResponse.candidates[0].text, .finishReason
model.generateContentStream(request)        // Flow<GenerateContentResponse>, each a new piece of text
model.close()

val request = generateContentRequest(TextPart("…")) {       // also (ImagePart, TextPart), (SystemInstruction, TextPart)
    temperature = 0.2f; topK = 10; seed = 1; maxOutputTokens = 64; candidateCount = 1
    promptPrefix = PromptPrefix("…")                        // the fixed part of a prompt, cached between calls
}
// Model choice: ModelConfig.builder().apply { releaseStage = ModelReleaseStage.STABLE /* or PREVIEW */;
//                                             preference = ModelPreference.FAST /* or FULL */ }.build()
```

**The feature APIs** share one older shape (ListenableFuture, so `await()` from `kotlinx-coroutines-guava`):

```kotlin
val p = Proofreading.getClient(ProofreaderOptions.builder(context)
    .setInputType(ProofreaderOptions.InputType.KEYBOARD).setLanguage(ProofreaderOptions.Language.GERMAN).build())
p.checkFeatureStatus().await()                                    // the same four values
p.downloadFeature(object : DownloadCallback { … })
p.runInference(ProofreadingRequest.builder(text).build()).await().results.map { it.text }   // best first
// Rewriting: RewriterOptions.OutputType ELABORATE, EMOJIFY, SHORTEN, FRIENDLY, PROFESSIONAL, REPHRASE
// Summarization: InputType ARTICLE | CONVERSATION, OutputType ONE_BULLET | TWO_BULLETS | THREE_BULLETS → .summary
```

They are small tuned adapters on the same base model: "If Gemini Nano is already downloaded on the device, the
feature-specific LoRA adapter model will be downloaded quickly."

**Limits that shape the design** (docs, and the error codes measured in `genai-common`):

- "GenAI API inference is permitted only when the app is the top foreground application." Otherwise
  `BACKGROUND_USE_BLOCKED` (30). The panel has focus, so it should qualify; a pinned window (2.0 §4) would not.
- "AICore enforces an inference quota per app." Too many requests in a short time: `BUSY` (9), and the exception
  carries a `retryDelay`. A longer-term limit: `PER_APP_BATTERY_USE_QUOTA_EXCEEDED` (27). No numbers are published.
  **A request per keystroke is out.** A request on Enter, or after a pause inside an AI scope, is the only shape
  that fits.
- Other codes worth handling: `NOT_AVAILABLE` (8), `REQUEST_TOO_LARGE` (12), `NOT_ENOUGH_DISK_SPACE` (501),
  `NEEDS_SYSTEM_UPDATE` (604), `AICORE_INCOMPATIBLE` (-101), `CANCELLED` (7).
- "Not supported on devices with an unlocked bootloader."

**Speed.** Nothing generative fits Booklight's 150 ms budget. It arrives after the local rows, as suggestions do.

| Figure | Source |
| --- | --- |
| Reading the prompt: 510 tokens a second (Pixel 9 Pro, nano-v2), 940 (Pixel 10 Pro, nano-v3); an image adds 0.6 to 0.8 s | Google, August 2025 |
| Writing the answer: Nano 3 about 9.6 tokens a second; Nano 4 Fast about 19; Nano 4 Full about 5 | Android Authority's own test (reported) |
| A cached prompt prefix on a Pixel 9: 300 + 50 tokens 0.82 s → 0.45 s; 1,000 + 100 tokens 2.11 s → 0.5 s | Google, prefix caching page |
| Intel laptops through AICore | none published. Unverified; the probe times it |

So a one-line translation or correction (15 to 30 tokens) should take one to three seconds on phone-class
hardware; a three-sentence answer five or more.

**The model and who pays.** AICore downloads and keeps the model, shared by every app. Versions today: nano-v2
(older partner phones), nano-v3 (Pixel 9 and 10, most 2025–26 flagships; "built on the same architecture as
Gemma 3n"), nano-v4 (Pixel 11, Galaxy Z Flip8 and Fold8; from Gemma 4, in a Fast and a Full variant, 4.2 and
5.9 GB per Android Authority). AICore's storage on phones is reported at 2 to 7 GB. On a Googlebook the model is
probably present already, because Magic Pointer and the Files app use AICore (inferred from the Welcome repo's
device notes). `download()` reports the exact bytes before it starts.

**Googlebooks.** Not on the ML Kit device list (docs). Gemini Intelligence devices must "support Android's AICore
service and support Gemini Nano v3 or newer" (reported). The Lenovo's flags read as AICore's own targeting keys:
`AICORE_INTEL` and `AICORE_INTEL_PTL` (Panther Lake, the Core Ultra Series 3 in that machine), `AICORE_CROS_GPU` (a
GPU path from the ChromeOS line), plus `android.hardware.npu`; the HP reports `AICORE_QC_HAMOA` (Snapdragon X), so
it very likely has AICore too (inferred). **That AICore is installed does not prove it answers a third-party app.**
Two more things to watch: Booklight runs as Android user 10 on these devices (the library has a multi-user
service class, so this is expected to work), and the panel is a see-through activity (expected to count as the
top app).

**Privacy of the route itself.** AICore "doesn't store any record of the input data or the resulting outputs",
has no direct internet access, and downloads models through Private Compute Services (docs). The prompt never
leaves the device.

**Play policy.** On-device processing is not "collection" under the data-safety form ("'Collect' means
transmitting data from your app off a user's device"). The ML Kit metrics are transmitted, and Google leaves the
answer to the developer: "you are solely responsible for deciding how to respond to Google Play's Data safety
section form". The AI-Generated Content policy puts out of scope "productivity apps that use AI to improve an
existing feature"; a text-to-text chatbot "where AI interaction is central" is in scope. Booklight stays on the
right side as long as the answer row is small.

### 3.2 Classic ML Kit

| API | Artifact | Measured in the AAR | Verdict for Booklight |
| --- | --- | --- | --- |
| Translation | `translate` 17.0.3 | x86_64 library 17.4 MB (7.1 MB compressed), arm64 16.4 MB (6.8). 58 language pairs with English, so 59 languages; a pack is 31 to 48 MB to download (German: 36.2 MB, 46.6 MB on disk). Manifest adds `INTERNET`, `ACCESS_NETWORK_STATE` | The only route that is fast enough to translate while typing and works on every device. Also the heaviest thing Booklight would ever ship. "Intended for casual and simple translations"; pairs without English go through English. Apps "must comply with the Google Cloud Translation API attribution requirements" (a Google Translate credit next to the result) |
| Language ID | `language-id` 17.0.6 | library 0.57 MB compressed per ABI, model 0.3 MB bundled | `TextClassifier.detectLanguage` does the same from the system for nothing. For German against English, the device language and a word list are enough |
| Smart Reply | `smart-reply` 17.0.4 | library 1.9 MB compressed per ABI, models 3.9 MB | "Currently, only English is supported." Needs a conversation with timestamps, which Booklight never has. No |
| Entity extraction | `entity-extraction` 16.0.0-beta6 | library `libtextclassifier3_jni_tclib.so`, 1.8 MB compressed per ABI; depends on WorkManager | It is the system's TextClassifier engine, repackaged, in beta since 2020. Addresses, dates, emails, flight numbers, IBANs, ISBNs, money, cards, phones, tracking numbers, URLs; German included. Booklight's `When.kt`, `Sites.kt` and `Jot.kt` already cover dates, times, addresses of the web and mail. Use the system's copy if at all |

All four ship x86_64 libraries (measured). None is marked deprecated, and their versions are current on Maven; no
GenAI replacement exists for translation. All carry the same telemetry as the GenAI libraries.

### 3.3 Bringing a model

- **MediaPipe LLM Inference** is "in maintenance-only mode. We recommend migrating … to LiteRT-LM" (docs).
- **LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android` 0.17.1) is the current route. Measured: arm64 and
  x86_64 libraries (21.8 and 26.0 MB, about 10 MB compressed), **no permissions, no telemetry dependencies** (gson,
  kotlin-reflect, coroutines). Kotlin API: `Engine(EngineConfig(modelPath, Backend.GPU()))`, a conversation,
  `sendMessageAsync(…)` as a flow, tools by annotation with constrained decoding.
- **Models** (docs): FunctionGemma (a Gemma 3 270M variant) 289 MB, Gemma 3 1B 1.0 GB, Gemma 3n E2B 3.0 GB, Gemma 4 E2B
  2.6 GB, Gemma 4 E4B 3.7 GB. Gemma 4 is Apache 2.0. Gemma 3 and 3n are under the Gemma Terms of Use: an app may
  redistribute them, but must pass on the terms and a notice file, and the Hugging Face copies sit behind a
  consent gate, so the app would need its own server.
- **Speed on a laptop** (Google's model card, Gemma 4 E2B on Intel Lunar Lake): CPU 30 tokens a second out, first
  token after 2.4 s, 3.5 GB peak memory; GPU 48 tokens a second, 0.3 s. The Intel NPU path exists on Windows
  through OpenVINO; on Android it is unverified.
- **llama.cpp** works on Android x86_64 and arm64 but means an NDK build, GGUF hosting and CPU-only inference in
  practice. More work than LiteRT-LM for less speed. Not pursued.

**Is it easy? No.** The code is short. Everything around it is not: a 2.6 GB download that Booklight must host,
start, resume, verify and explain; a model that loads into Booklight's own process on every cold open (the panel's
43 ms start becomes seconds) or keeps gigabytes resident in a process that is supposed to be idle; a battery cost
that is Booklight's own. AICore solves exactly these by keeping one model in one system service. The one thing an
own model does better: it sends nothing to anyone and adds no permission. Play's AI packs (beta, up to 1.5 GB per
pack) would host a model for Play installs only, not for the GitHub APK.

### 3.4 What launchers and systems do, and what gets used

| Product | AI features | How the answer comes back | Evidence of use |
| --- | --- | --- | --- |
| Raycast | Eight built-in AI commands: Improve Writing, Fix Spelling and Grammar, Explain This in Simple Terms, Change Tone (two), Find Bugs, Summarize and Ask About Webpage. Quick AI for questions | In the Raycast window, to copy or paste; **Quick Fix replaces the text in place** from a hotkey | Anecdotal: users report fixing grammar 15 to 20 times a day. No published breakdown |
| Spotlight, macOS 26 | Actions with parameters, Quick Keys ("sm" sends a message), Shortcuts' "Use Model" step | Runs the action; text through Shortcuts | None published |
| Apple Writing Tools | Proofread, Rewrite (friendly, professional, concise), Summarize, lists, tables | Replaces the selection | SellCell survey, about 1,000 iPhone owners: 72 % tried Writing Tools, 54 % notification summaries; 73 % said the AI features add little or no value |
| Windows Click to Do | Summarize, bulleted list, Rewrite (casual, formal, refine), on the NPU with Phi Silica | In place | English only, ten words minimum, Copilot+ PCs |
| PowerToys Advanced Paste | Translate, summarise, rewrite the clipboard; local models through Foundry Local or Ollama since 0.96 | Pasted | None published |
| Galaxy AI | Circle to Search, Photo Assist, Note Assist, Live Translate | — | SellCell: 82 % used Circle to Search, 55 % Photo Assist; 87 % said little or no value |

The pattern: **the features people keep are short transformations of text they already have** (fix, change tone,
translate, summarise), delivered where the text is. Open-ended chat is not a launcher feature; it is the Gemini app.

### 3.5 What Android and the Googlebook already offer

| Thing | Can an ordinary app call it? | Sure |
| --- | --- | --- |
| `TextClassifier` (`classifyText`, `generateLinks`, `detectLanguage`, `suggestConversationActions`) | Yes. Public, no permission, worker thread. Entity types: address, date, datetime, email, flight, phone, url, otp, dictionary | measured (AOSP `android17-release`) |
| `TranslationManager.createOnDeviceTranslator`, `getOnDeviceTranslationCapabilities`, `getOnDeviceTranslationSettingsActivityIntent` | Yes, since API 31. The system service forwards to the device's default translation service without a permission check. Whether the Googlebook ships such a service, and whether it answers Booklight, is unverified | measured (AOSP) / unverified (device) |
| `android.app.ondeviceintelligence.OnDeviceIntelligenceManager` | **No.** `USE_ON_DEVICE_INTELLIGENCE` is `signature\|privileged`, system API, hidden | measured (AOSP manifest) |
| AppFunctions | Publishing: yes, Android 16+. Calling other apps' functions: `EXECUTE_APP_FUNCTIONS` plus "a device allowlist"; Gemini's use is "in a private preview with trusted testers" | measured + docs |
| AppSearch embeddings | Storage and search for vectors (Jetpack AppSearch 1.1.0). It makes no embeddings; the app must bring a model | docs |

**The Googlebook's own features** (reported; the Welcome repo's device notes): Magic Pointer (wiggle or Action + G:
select anything on screen and ask Gemini, with suggested actions), Quick Insert on its own key ("emojis,
translation tools and Google's Rambler voice typing tool as well as your cut and copied snippets"), Gemini in the
status bar and on Action + Space, Files with AI search, Create My Widget.

What Booklight should leave alone: asking about what is on the screen, images and screenshots, dictation, a chat
window. What is still open for a keyboard launcher: text you type or bring, changed and handed back, without
leaving the keys. Translation overlaps with Quick Insert; Booklight's version is worth having only because it is
one keyword away and works on text another app handed over.

---

## 4. The top five for Booklight

All five assume the probe says yes. Each has an honest path when it says no. Effort: **S** up to a day, **M** up to
a week, **L** more. The first one built carries the shared work (a wrapper around the client with status, warm-up
and cancel; a row body for streamed text; the settings row; the fall-backs), which is why it is an M.

**Rules common to all five.** The model is asked on Enter, or after a pause inside the keyword's chip, never per
keystroke (quota). Local rows show at once; the answer arrives in its row afterwards, as suggestions do. One
request at a time; typing cancels it. Output is capped (64 to 128 tokens) because the panel does not scroll. The
row says "on this device". "Open in Gemini" stays as the row's next action. Booklight calls `warmup()` when an
AI chip is entered and `close()` when the panel closes.

### 1. Fix and rewrite, answered in the row

- **Typed:** `fix their going too the park tomorow` → row one, large: "They're going to the park tomorrow."
  Enter copies. `shorter …`, `formal …`, `friendly …` work the same way. With nothing typed, the clipboard.
  For text another app handed over from an editable field, the first action is **Replace**: the corrected text
  goes back into that field (`ACTION_PROCESS_TEXT` returns it; no permission).
- **API:** Proofreading and Rewriting (German supported, tuned for exactly this, under 256 tokens, which is a
  launcher-sized text). The Prompt API as the alternative with one library instead of three.
- **Effort:** M (the shared work), then S.
- **Needs:** the base model on the device; a small adapter download per feature.
- **Not available:** the row is 2.0's prompt row: it opens Gemini with the prompt filled in. No error shown.
- **Why over the hand-over:** it is the most used AI command everywhere (§3.4), the answer is one line, and
  Replace is something the Gemini app cannot do. Offline. Nothing leaves the device.
- **Risk:** a see-through launcher activity must be able to return a result to the calling app (check on a
  device). Beta libraries. Mixed `genai-common` versions (beta1 features, beta4 prompt) are untested together.

### 2. Translate in the row

- **Typed:** `tr de good morning, are we still on for lunch?` → "Guten Morgen, bleibt es beim Mittagessen?"
  Enter copies. `tr hello` goes to the device's language, or away from it when the text is already in it. A
  text chip from another app gets a "Translate" row, with Replace as above.
- **API, in this order:** the system's `TranslationManager` if the probe finds a service (instant, nothing to
  ship); else the Prompt API ("Translate to German. Reply with only the translation:", temperature 0.2); classic
  ML Kit Translation only if Alex wants translation on devices without either, and accepts its size.
- **Effort:** S after feature 1 on the Prompt API or the system translator. M with ML Kit (pack download,
  a settings page, the attribution line).
- **Needs:** nothing new with the Prompt API. System translator: a language pack from the system's settings
  page, which Booklight opens. ML Kit: 36 MB for German, started by the user, said plainly.
- **Not available:** `Intent.ACTION_TRANSLATE` if an app handles it, else the web row (translate.google.com with
  the text), as planned in `use-cases.md`.
- **Why over the hand-over:** the answer is short and wanted at once, to paste. Opening an app for it is the
  slow way round.
- **Risk:** Quick Insert already translates. The Prompt API's language coverage is not published; German quality
  must be judged on the device. A small model can drift from the source text; a dedicated translator does not.

### 3. Summarise the clipboard or a shared text in one line

- **Typed:** copy a long mail, then `sum` → "Anna moves the review to Thursday 14:00 and needs the slides by
  Wednesday noon." Enter copies; the next action appends it to Notes.md. A text chip longer than about 400
  characters gets a "Summarise" row.
- **API:** Summarization (`ONE_BULLET`) for English. German is not supported there, so German text goes through
  the Prompt API. Input cap 4,000 tokens; longer text is cut and the row says so.
- **Effort:** S after feature 1.
- **Needs:** the base model.
- **Not available:** 2.0's "Summarise" prompt row, which opens Gemini.
- **Why over the hand-over:** the result is one line that belongs in a note, a mail or the clipboard. And it is
  the case where privacy matters most: the text is somebody's mail.
- **Risk:** the slowest of the five: a full-length input takes seconds to read before the first word appears.
  The clipboard is read only on request, as today.

### 4. A short answer in the panel

- **Typed:** `explain idempotent` → two lines, streamed: "An operation is idempotent if doing it twice has the
  same effect as doing it once…". `define`, `explain` and `ask` share the row; Enter copies, the next action is
  "Continue in Gemini" with the same question.
- **API:** Prompt API, streaming, a fixed prefix (cached) that demands one or two sentences, 96 tokens at most.
- **Effort:** S after feature 1.
- **Needs:** the base model.
- **Not available:** today's Ask Gemini row, unchanged.
- **Why over the hand-over:** for a definition or a reminder of how something works, two lines in the panel beat
  a chat window. Works offline.
- **Risk:** the highest for quality. A 2 to 4 billion parameter model states wrong facts with confidence and
  knows nothing recent. Keep it to explaining and defining; anything that reads as a question about the world
  keeps Gemini as the first action. Do not let this grow into a chat: that is where Play's AI content policy
  starts to apply.

### 5. Say it plainly

- **Typed:** `open chrome on the left and set a 10 minute timer` → one row: "Chrome, left half · Timer 10 min".
  Enter runs both (`Effect.Steps`, which recipes already use). `remind me tomorrow at nine to call the bank` →
  the Reminder row, filled in. Only when Booklight's own parsers found nothing and the text has four or more
  words; the model's row never runs by itself and never outranks a local match.
- **API:** Prompt API with structured output: a `@Generable` class holding a list of steps (kind from a fixed
  list, app name, place, minutes, text, time as written). Each field then goes through Booklight's own code
  (`Matcher` for the app, `When.kt` for the time), so the model proposes and the parsers decide.
- **Effort:** L. S for the narrow first cut: dates and reminders that `When.kt` does not understand.
- **Needs:** the base model; KSP in the build; `isStructuredOutputFeatureAvailable()` true. Otherwise JSON asked
  for in the prompt and read with kotlinx.serialization, which is slower and less reliable.
- **Not available:** nothing appears. The typed text still has its web and Gemini rows.
- **Why over the hand-over:** it is the only one of the five the Gemini app cannot do at all: Gemini cannot run
  Booklight's effects. It is also the direction Spotlight and Siri took.
- **Risk:** alpha API. One to three seconds for something that looks like it should be instant. A wrong parse
  shown as a ready-to-run row is worse than no row, hence the validation and the narrow start. Much of the value
  can be had without a model by teaching `When.kt` number words.

### Runners-up

1. **What is in this text.** For clipboard or shared text, rows from the system `TextClassifier`: a date becomes
   "Add event", an address "Open in Maps", a flight number a search. S, no library. Runner-up only because the
   selection toolbar already offers the same actions for selected text.
2. **Your own prompts, answered here.** Every prompt of 2.0 §9 gets "Answer here" beside "Open in Gemini". S after
   feature 1; it is features 1 to 4 made general.
3. **Tone as one more word.** `emojify …`, `elaborate …`: the Rewriting API's other output types. S, low value.
4. **Sums asked in words.** "what is 15 percent of 80 euros in dollars": the model writes the expression, `Calc`
   or Summa computes it. Wait for the Summa extension.
5. **Reply drafts.** A shared message gets three short replies from the Prompt API (classic Smart Reply is
   English only). M; Gmail and Messages already do it where it matters.

### Sounds good, not worth it

| Idea | Why not |
| --- | --- |
| A model that answers as you type | The per-app quota and the battery quota forbid it, and nothing generative fits 150 ms |
| A chat in the panel | It is the Gemini app, on Action + Space. It also moves Booklight into the AI content policy |
| Describing or reading a screenshot | Image Description is English only; Booklight takes no images yet; Magic Pointer and Circle to Search own this |
| Semantic search over Notes.md | Needs an embedding model that Booklight would have to ship (AICore offers none to apps). One file of notes is served by plain search |
| Dictation into the panel | Needs `RECORD_AUDIO`, a runtime prompt of exactly the kind Booklight avoids. Rambler exists |
| Letting the model do sums or units | It gets them wrong, and the answer row shows them large. `Calc` and Summa are exact |
| Classic Smart Reply, classic Entity extraction | English only; a repackaged copy of what the system has (§3.2) |
| An own model as the main route | §3.3. At most, later, an opt-in for devices without AICore |
| Calling other apps' AppFunctions, or the platform's on-device intelligence service | Allowlist; signature permission (§3.5) |

---

## 5. Device probe plan

**Aim:** learn in one debug build whether AICore answers Booklight on the Lenovo, with which model, how fast,
and whether the two system APIs answer. Debug only: `debugImplementation`, code under `app/src/debug/`, so the
release build and its manifest do not change.

**Dependencies** (`app/build.gradle.kts`):

```kotlin
debugImplementation("com.google.mlkit:genai-prompt:1.0.0-beta4")
debugImplementation("com.google.mlkit:genai-proofreading:1.0.0-beta1")
debugImplementation("com.google.mlkit:genai-rewriting:1.0.0-beta1")
debugImplementation("com.google.mlkit:genai-summarization:1.0.0-beta1")
debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")   // ListenableFuture.await()
```

If the three beta1 libraries fail against `genai-common` beta4 (`NoSuchMethodError`), probe the Prompt API alone
first; it is the one that matters.

**Where to run it.** In Booklight's process **while the panel is open and focused** (`./bl open stay`, then a new
`./bl debug ai` hook in `DebugReceiver`). A receiver with no window in front gets `BACKGROUND_USE_BLOCKED`, which
would be read wrongly as "not supported". If the panel gets error 30 as well, repeat from the Booklight window
(`MainActivity`): that would mean a see-through activity does not count as the top app, and it changes the design.

**Calls, in order** (log everything under one tag, `BooklightAI`):

```kotlin
// A. Prompt API
val m = Generation.getClient()
val s = m.checkStatus()                                   // 0 unavailable · 1 downloadable · 2 downloading · 3 available
log("prompt status=$s")
// STABLE, PREVIEW = ModelReleaseStage.STABLE (0), .PREVIEW (1); FAST, FULL = ModelPreference.FAST (1), .FULL (2)
for ((stage, pref) in listOf(STABLE to FAST, STABLE to FULL, PREVIEW to FAST, PREVIEW to FULL)) {
    val c = Generation.getClient(generationConfig {
        modelConfig = ModelConfig.builder().apply { releaseStage = stage; preference = pref }.build() })
    log("stage=$stage pref=$pref status=${runCatching { c.checkStatus() }}"); c.close()
}
if (s == FeatureStatus.DOWNLOADABLE) m.download().collect { log("download $it") }   // DownloadStarted has the size. Wi-Fi; Alex's go-ahead
if (m.checkStatus() == FeatureStatus.AVAILABLE) {
    log("base=${m.getBaseModelName()} limit=${m.getTokenLimit()} structured=${m.isStructuredOutputFeatureAvailable()}" +
        " system=${m.isSystemPromptAvailable()} caching=${m.isCachingFeatureAvailable()} thinking=${m.isThinkingModeAvailable()}")
    val t0 = now(); m.warmup(); log("warmup ms=${now() - t0}")
    for (prompt in listOf(
        "Translate to German. Reply with only the translation: Good morning, are we still on for lunch?",
        "Korrigiere Rechtschreibung und Grammatik. Antworte nur mit dem korrigierten Text: Ich habe gestern ein Buch gekaufen und es gefällt mich sehr.",
        "Explain in one sentence what idempotent means.")) {
        val req = generateContentRequest(TextPart(prompt)) { temperature = 0.2f; maxOutputTokens = 64 }
        val t = now(); var first = 0L; val out = StringBuilder()
        m.generateContentStream(req).collect { if (first == 0L) first = now() - t; out.append(it.candidates.firstOrNull()?.text.orEmpty()) }
        log("in=${m.countTokens(req).totalTokens} firstMs=$first totalMs=${now() - t} chars=${out.length} text=$out")
    }                                                      // run the loop twice: the second pass is the warm figure
}
// B. Feature APIs, German and English: checkFeatureStatus().await(), then one runInference each when 3
//    Proofreading(GERMAN, KEYBOARD) "Ich habe gestern ein Buch gekaufen"  ·  Rewriting(GERMAN, SHORTEN)
//    Summarization(ENGLISH, ARTICLE, ONE_BULLET) with 500+ characters of text
// C. System translator (no library; worker thread)
val tm = getSystemService(TranslationManager::class.java)
val caps = tm.getOnDeviceTranslationCapabilities(TranslationSpec.DATA_FORMAT_TEXT, TranslationSpec.DATA_FORMAT_TEXT)
log("translator capabilities=${caps.size} settings=${tm.onDeviceTranslationSettingsActivityIntent != null}")
caps.forEach { log("${it.sourceSpec.locale} -> ${it.targetSpec.locale} state=${it.state}") }   // 1 to download · 3 on device
// then createOnDeviceTranslator(TranslationContext.Builder(en spec, de spec).build(), executor) { translator -> … translate "Good morning" }
// D. System TextClassifier (no library; worker thread)
val tc = getSystemService(TextClassificationManager::class.java).textClassifier
log("lang=${tc.detectLanguage(TextLanguage.Request.Builder("Guten Morgen, wie geht es dir heute?").build())}")
tc.generateLinks(TextLinks.Request.Builder("Lunch with Anna tomorrow at 12:30, Alexanderplatz 1, Berlin, flight LH 438, +49 30 1234567").build())
    .links.forEach { log("${it.start}-${it.end} ${it.getEntity(0)} ${it.getConfidenceScore(it.getEntity(0))}") }
```

Wrap every call in `runCatching` and log a `GenAiException`'s `errorCode` and `retryDelay`.

**From the shell** (read-only; no reboot, nothing touching the VM):

```
adb shell dumpsys package com.google.android.aicore | grep -E "versionName|BIND_SERVICE"
adb shell dumpsys translation | head -40          # the default translation service, if any
adb shell dumpsys textclassification | head -40   # which package classifies text
adb shell cmd package query-activities --brief -a android.intent.action.TRANSLATE
adb shell cmd package query-activities --brief -a android.intent.action.DEFINE
adb shell getprop ro.boot.verifiedbootstate       # anything but green: ML Kit GenAI is unsupported
adb shell dumpsys jobscheduler | grep -A4 booklight   # after the probe: is a telemetry upload job queued?
adb logcat | grep -i -E "BooklightAI|aicore|TransportRuntime"
```

Also record once, from the build: `./gradlew :app:dependencies --configuration debugRuntimeClasspath`, the merged
debug manifest's permissions, services, providers and receivers, and the release APK's size with `genai-prompt`
as `implementation` in a throwaway working copy against today's 2,474,057 bytes.

**Reading the results:**

| Result | Meaning | Next |
| --- | --- | --- |
| Prompt status 3, a base name, answers in German | The easy way works on this Googlebook | Build feature 1. Repeat the probe on the HP when it is back |
| Status 1 | Supported, model not downloaded | Note the size from `DownloadStarted`, download on Wi-Fi, run again |
| Status 0 on STABLE, 1 or 3 on PREVIEW | Only the developer-preview model | Enrolling changes a system app (AICore beta through Play and a Google group): Alex's call |
| Status 0 everywhere, or `NOT_AVAILABLE`, `AICORE_INCOMPATIBLE`, `NEEDS_SYSTEM_UPDATE` | AICore does not serve third parties on this build | Google's page says a fresh device may need time online to fetch its configuration; wait and repeat, do not reboot. Then fall back as below |
| Error 30 with the panel open | The see-through panel is not "top" | Repeat from `MainActivity`; if that works, AI answers need an opaque surface |
| First word within 0.5 s and 15 or more tokens a second | Streamed answers feel fine | Features 1 to 4 on the Prompt API |
| A 15-token translation takes more than 2 s | Too slow to be the translator | Translate through section C's system translator, or not at all |
| Feature APIs 0 while Prompt is 3 | No adapters for this device | Do fix, rewrite and summarise with prompts; one library less |
| C lists language pairs | A free translator | Use it for feature 2; open its settings page for packs |
| D returns entities and "de" | Free language detection and runner-up 1 | Use for translate's direction |

**If AICore is unavailable**, in order: the system translator and TextClassifier (if C and D answered); 2.0's
prompt rows that hand over to Gemini (already designed); classic ML Kit Translation as a deliberate, sized
decision; an own model only if Alex wants on-device answers on devices without AICore and accepts §3.3.

---

## 6. What it would mean for Booklight's promises

| Promise today | With ML Kit GenAI as shipped | With the system APIs only | With an own model |
| --- | --- | --- | --- |
| "no analytics" (`PRIVACY.md`); "No analytics, no crash reporting, no ads, no third-party SDKs" (`data-safety.md`) | **No longer true.** ML Kit reports usage metrics to Google | Unchanged | Unchanged |
| "nothing that runs in the background" | **Weakened.** `datatransport` registers a job service and an alarm receiver in Booklight to upload metrics later. Inference itself runs only while the panel is in front, in AICore's process | Unchanged | Unchanged, if the model is dropped when the panel closes |
| "Internet, for suggestions only" | **No longer true:** the same permission carries the metrics upload. The model download is AICore's, not Booklight's | Unchanged | Becomes "…and to download the model you asked for" |
| Nothing typed leaves the device unless a web row is picked | **Still true**, and stronger than the Gemini hand-over: the text stays on the device | Still true | Still true |
| No runtime permissions | Still true. Two silent manifest permissions are added (§1) | Still true | Still true |
| A 2.5 MB APK | About 3 to 4 MB (estimate) | 2.5 MB | 25 to 50 MB plus a model of 0.3 to 3.7 GB |
| Works without AICore | Yes: every feature falls back to its hand-over row | Yes | Yes |

**Privacy text, if ML Kit ships.** One new paragraph, in the page's own voice: *"On-device answers. Fix, translate,
summarise and explain are worked out on your Googlebook by the Gemini Nano model that came with it. The text is
not sent anywhere. The Google library that talks to the model reports to Google how it is used: your device model
and Android version, Booklight's version, an identifier for this installation, how long a request took and how
large it was. Never the text."* The Permissions paragraph gains the network-state permission and loses "for
suggestions only". The first sentence of the page ("no analytics") has to change.

**Data-safety form, if ML Kit ships.** Add as collected (Play counts what an SDK transmits): *Device or other IDs*
and *App info and performance › Diagnostics*, purpose analytics, not optional unless a switch disables the
feature and the library with it. The prompts and answers are not collected. This is my reading of Google's
disclosure page and Play's definition; Google says the answer is the developer's to give.

**The 18+ term.** If it means what it says, the Play target audience moves from "13 and over" to 18 and over.
Unverified how Play treats this; ask before filing.

**Three ways to keep the promises:**

1. **Accept and say so.** Simplest. Booklight stops being an app with no third-party SDK.
2. **Strip the transport.** Remove the upload backend and the job service from the merged manifest. An unanswered
   report on Google's ML Kit samples repository (25 September 2026) found zero traffic from an app built without
   network permissions, but nobody at Google has called any of this supported, and the terms say the APIs "send
   metrics". Unverified, and not something to do without Alex's decision.
3. **A separate app.** "Booklight AI" carries ML Kit, its telemetry, its permissions and the 18+ term, and offers
   Fix, Translate and Summarise as `PROCESS_TEXT` actions that return text. Booklight shows what comes back
   (idea F5 in `next-features.md`). Booklight's manifest, privacy page and form stay as they are, and the answer
   still lands in the panel. `PLAN.md` §4 already proposes this shape for the power pack. More work, cleanest result.

The system APIs (§3.5) and an own model avoid the question entirely, which is why the probe tests the system
translator in the same run.

**The rule that survives in every case:** a download over the network happens only when the user starts it, and
the row says what it is and how large.

---

## 7. Sources

**Artifacts opened (Google's Maven, 1 October 2026)**
- https://dl.google.com/dl/android/maven2/com/google/mlkit/group-index.xml (versions; `genai-schema` 1.0.0-alpha1 is listed, its AAR was not retrievable)
- `genai-prompt` 1.0.0-beta4, `genai-common` 1.0.0-beta4, `genai-proofreading`, `genai-rewriting`, `genai-summarization`, `genai-image-description` 1.0.0-beta1, `genai-speech-recognition` 1.0.0-alpha1, `translate` 17.0.3, `language-id` 17.0.6, `smart-reply` 17.0.4, `entity-extraction` 16.0.0-beta6, `common` 18.11.0 under https://dl.google.com/dl/android/maven2/com/google/mlkit/
- `transport-runtime` 2.2.6, `transport-backend-cct` 2.3.3 under https://dl.google.com/dl/android/maven2/com/google/android/datatransport/
- `litertlm-android` 0.17.1: https://dl.google.com/dl/android/maven2/com/google/ai/edge/litertlm/group-index.xml · `tasks-genai` 0.10.35: https://dl.google.com/dl/android/maven2/com/google/mediapipe/group-index.xml

**ML Kit GenAI and Gemini Nano**
- https://developers.google.com/ml-kit/genai (APIs, device list, foreground rule, quotas)
- https://developers.google.com/ml-kit/genai/prompt/android · https://developers.google.com/ml-kit/genai/prompt/android/get-started
- https://developers.google.com/ml-kit/genai/prompt/android/structured-output · https://developers.google.com/ml-kit/genai/prompt/android/prefix-caching · https://developers.google.com/ml-kit/genai/prompt/android/prompt-design
- https://developers.google.com/ml-kit/genai/summarization/android · https://developers.google.com/ml-kit/genai/proofreading/android · https://developers.google.com/ml-kit/genai/rewriting/android · https://developers.google.com/ml-kit/genai/image-description/android · https://developers.google.com/ml-kit/genai/speech-recognition/android
- https://developers.google.com/ml-kit/genai/aicore-dev-preview
- https://developers.google.com/ml-kit/genai-terms · https://developers.google.com/ml-kit/terms · https://developers.google.com/ml-kit/android-data-disclosure
- https://developer.android.com/ai/gemini-nano
- https://android-developers.googleblog.com/2025/08/the-latest-gemini-nano-with-on-device-ml-kit-genai-apis.html (speeds)
- https://android-developers.googleblog.com/2025/10/ml-kit-genai-prompt-api-alpha-release.html
- https://android-developers.googleblog.com/2026/04/AI-Core-Developer-Preview.html (Gemma 4, Fast and Full)
- https://github.com/android/skills/tree/main/device-ai/ml-kit-genai-prompt-api
- https://www.androidauthority.com/gemini-nano-4-benchmarks-3655763/ (reported speeds and sizes)
- https://www.androidcentral.com/apps-software/if-aicore-is-taking-up-space-on-your-phone-heres-whats-going-on (AICore storage)
- https://asoasis.tech/news/2026-05-19-0125-gemini-intelligence-hardware-requirements/ (reported requirements)
- https://github.com/googlesamples/mlkit/issues/1076 (telemetry without network permissions; unanswered)

**Classic ML Kit**
- https://developers.google.com/ml-kit/language/translation · https://developers.google.com/ml-kit/language/translation/android · https://developers.google.com/ml-kit/language/translation/translation-terms · https://docs.cloud.google.com/translate/attribution
- https://developers.google.com/ml-kit/language/identification · https://developers.google.com/ml-kit/language/smart-reply · https://developers.google.com/ml-kit/language/entity-extraction

**Own model**
- https://developers.google.com/edge/litert-lm/overview · https://developers.google.com/edge/litert-lm/android
- https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android (maintenance notice)
- https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm (Lunar Lake figures) · https://huggingface.co/litert-community/gemma-3-270m-it
- https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/ · https://github.com/google-ai-edge/LiteRT-LM/issues/3508 (Intel NPU, Windows)
- https://ai.google.dev/gemma/terms · https://developer.android.com/google/play/on-device-ai

**Platform (AOSP `android17-release`)**
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml (`USE_ON_DEVICE_INTELLIGENCE`, `EXECUTE_APP_FUNCTIONS`)
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/view/translation/TranslationManager.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/translation/java/com/android/server/translation/TranslationManagerService.java
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/view/textclassifier/TextClassifier.java
- https://developer.android.com/ai/intelligence-system · https://developer.android.com/ai/appfunctions · https://developer.android.com/jetpack/androidx/releases/appsearch · https://developer.android.com/googlebook

**Play policy**
- https://support.google.com/googleplay/android-developer/answer/14094294 (AI-generated content) · https://support.google.com/googleplay/android-developer/answer/10787469 (data safety)

**Launchers and systems**
- https://manual.raycast.com/ai/ai-commands · https://www.raycast.com/changelog/macos-beta/0-61 (Quick Fix)
- https://www.apple.com/newsroom/2025/06/macos-tahoe-26-makes-the-mac-more-capable-productive-and-intelligent-than-ever/ · https://9to5mac.com/2025/06/10/macos-26-spotlight-gets-actions-clipboard-manager-custom-shortcuts/
- https://support.microsoft.com/en-us/windows/click-to-do-do-more-with-what-s-on-your-screen-6848b7d5-7fb0-4c43-b08a-443d6d3f5955
- https://www.windowscentral.com/software-apps/powertoys-breaks-free-from-openai-limits-advanced-paste-now-works-with-multiple-ai-models
- https://www.sellcell.com/blog/iphone-vs-samsung-ai-survey/

**Googlebook**
- https://blog.google/products-and-platforms/devices/googlebook/googlebook-built-in-intelligence/ · https://www.androidauthority.com/googlebooks-hands-on-impressions-3713390/ (Quick Insert) · https://www.pcworld.com/article/3238878/the-googlebooks-most-compelling-ai-feature-isnt-even-ai.html
- `Welcome's device notes` §5 (Magic Pointer, AICore, `AICORE_QC_HAMOA` on the HP)

**This repo**
- `app/build/outputs/apk/release/app-release.apk` (2,474,057 bytes) · `app/src/main/AndroidManifest.xml` · `store-submission/forms/app-content.md` (target audience) · `core/…/SearchEngine.kt` (the 150 ms budget) · `app/…/overlay/OverlayActivity.kt` (text handed over is taken, not yet returned)
