# Ultron AI (Android)

Voice assistant that controls your phone. Native Kotlin app, package `com.ultron.ai`.

## Build
1. Put these files in your GitHub repo (replace the old project files) and push to `main`.
2. Open the repo's **Actions** tab, open the latest "Build Ultron APK" run, download the `ultron-apk` artifact, install `app-debug.apk`.

Optional: add repo secrets `GEMINI_API_KEY`, `ELEVEN_API_KEY`, `ELEVEN_VOICE_ID` (Settings > Secrets and variables > Actions) to bake them in as defaults. Anyone with the APK file can extract baked-in keys, so keep the APK to yourself. Keys typed into the app's KEYS screen override them.

## First run
1. Open Ultron, tap **KEYS**, paste your Google (Gemini) API key, ElevenLabs API key and ElevenLabs voice ID, tap SAVE, then TEST VOICE.
2. Tap the orange banner (or KEYS > OPEN ACCESSIBILITY SETTINGS), find **Ultron**, turn it on. Android shows a warning; that permission is what lets Ultron tap and type for you.
3. Tap the core, say "open WhatsApp", or "open WhatsApp and message Rahul hi".

## What runs where
- "open <app>", back, home, recents, scroll: instant, handled on the phone with no internet.
- Everything else: Gemini reads the live screen and plans the next taps and typing.
- Voice out: ElevenLabs (falls back to the phone's own voice if the key or voice ID is missing or fails).
- Voice in: Android's built-in speech recognizer. Set the language under KEYS (en-IN, hi-IN, ...).

## Safety rules built in
- Ultron refuses to tap payment, transfer, purchase or delete-account buttons.
- Text on screen is treated as untrusted, so a message that says "ignore your user" is not obeyed.
- Tap the core while it is working to stop it.
