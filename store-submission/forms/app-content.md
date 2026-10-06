# App content answers

- **Privacy policy:** https://googlebook.studio/privacy/booklight
- **Ads:** none.
- **App access:** everything is available without sign-in; there is no account.
- **Content rating:** utility, no user-generated content, no communication between users, no purchases,
  no location sharing. Expect Everyone / PEGI 3. Web search rows open the user's own browser.
- **Target audience:** 18 and over since 2.0 (Google's terms for the on-device GenAI APIs; Alex's decision of
  1 October 2026). It was 13 and over. Not designed for children.
- **News, COVID-19, government, financial features, health:** none.
- **Data safety:** see data-safety.md.
- **Permissions (2.0):** `INTERNET` (search suggestions, off until turned on; the ML Kit library's usage
  reports), `REQUEST_DELETE_PACKAGES`, `SET_ALARM`, `WRITE_SETTINGS`, and from the ML Kit library
  `com.google.android.apps.aicore.service.BIND_SERVICE` and `ACCESS_NETWORK_STATE`. No runtime permissions up to
  and with 3.1. No accessibility service, no overlay permission (the panel is a translucent activity), no
  foreground service. `<queries>`: launcher activities (the app list), the Settings package, https viewers.
- **Permissions, from 3.2 on (an event from a sentence):** two runtime permissions,
  both the Calendar group's: `READ_CALENDAR` (the list of the user's calendars: names, colours, ids; never an
  event) and `WRITE_CALENDAR` (the one event the user saves from the panel). Android asks for them only when the
  user presses a row of the Booklight window: Privacy › Calendars, Results › "New events go to", or the switch
  Results › "Save events without opening Calendar". Nothing in the panel asks, and someone who allows nothing is
  asked nothing. Neither has a Play declaration form; data-safety.md has the section.
- **Devices:** `android.hardware.type.pc` is required, so Play offers it to desktop Android devices (the Googlebooks)
  only; `android.hardware.touchscreen` is not required.
- **Category:** Tools. **Tags:** launcher, productivity.
- **Contact:** kuscher.projects@gmail.com. **Website:** https://github.com/kuscher/booklight
