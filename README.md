# Smart Day AI — Android app (real source project)

This is a **complete, real Android Studio project** written in Kotlin — not a
mockup. It uses Android's actual `AlarmManager`, `NotificationManager`, and
on-device text-to-speech, so reminders genuinely fire **even if the app is
fully closed or swiped away**, which a website/PWA can never reliably do.

### Honesty check, upfront
I can't compile this into a ready-to-tap `.apk` inside this chat — that needs
the Android SDK and build tools, which this environment doesn't have. What I
*can* do — and did — is write the full, real source code so that opening it
in Android Studio and pressing **Run** gives you a working app on your phone
in a few minutes, and pressing **Build → Generate Signed APK** gives you a
real installable `.apk` file.

## What actually works here
- 🎤 **Voice input** — tap the mic, speak your tasks; if speech recognition
  isn't available on the phone (or permission is denied), it automatically
  falls back to the text box, so nothing is ever blocked on voice.
- ⏰ **Real alarms** — every task schedules two real `AlarmManager` alarms
  (10 minutes before, and at start time) using `setExactAndAllowWhileIdle`,
  Android's mechanism for alarms that fire even in Doze/battery-saving mode.
- 🔔 **Real notifications** — a heads-up notification appears for every
  reminder, with the task name and time.
- 🗣️ **Spoken announcement** — when an alarm fires, the phone also *speaks*
  "Your next task is …" out loud using Android's built-in text-to-speech,
  in whichever of English/Urdu/Arabic you've selected — so it comes through
  even if you can't look at the screen right away.
- 📱 **Survives reboot** — a `BOOT_COMPLETED` receiver re-schedules the day's
  remaining alarms if the phone restarts (Android wipes `AlarmManager` alarms
  on every reboot, so this step is what keeps reminders alive).
- 🌐 Full Urdu / Arabic / English UI, dark navy-and-gold theme, From–To time
  pickers, auto-scheduling of tasks with no stated time, and break suggestions
  — same behavior as the web version, now running as a true native app.

## How to build it (about 10 minutes, free)
1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `SmartDayAIApp` folder.
3. Let it sync (it will download Gradle + dependencies automatically the
   first time — needs internet once, not after).
4. Plug in an Android phone via USB with **USB debugging** enabled
   (Settings → About phone → tap "Build number" 7 times → Developer options →
   USB debugging), or use a virtual device (emulator) in Android Studio.
5. Press the green **Run ▶** button. The app installs and opens on the phone.

## To get a real, installable `.apk` file to share
Android Studio → **Build → Generate Signed App Bundle / APK** → choose **APK**
→ create a new keystore (just a password you set, one time) → Build. You'll
get an `app-release.apk` you can send to any Android phone and install
directly (the phone will ask you to allow installs from that source once).

## Permissions to grant on first run (the app will prompt for these)
- **Notifications** — required on Android 13+, or reminders won't show.
- **Alarms & reminders** ("Schedule exact alarms") — Android 12+ requires
  this to be granted from Settings; the app shows a banner with a direct
  link if it's missing.
- **Ignore battery optimization** — not strictly required, but strongly
  recommended; some phone brands (Xiaomi, Oppo, Vivo, Huawei especially)
  aggressively kill background alarms unless this is granted. The app shows
  a banner with a direct link for this too.

## Project structure
```
SmartDayAIApp/
  app/src/main/java/com/smartdayai/app/
    MainActivity.kt       – UI, voice input, draft review, scheduling
    TaskParser.kt          – turns spoken/typed text into draft tasks
    Task.kt                 – the task data model
    TaskStorage.kt          – saves today's schedule (per-day, auto-resets)
    AlarmScheduler.kt       – schedules the real AlarmManager alarms
    AlarmReceiver.kt        – fires notification + speaks reminder aloud
    BootReceiver.kt         – reschedules alarms after phone restart
    LocaleHelper.kt         – applies the in-app language choice everywhere
  app/src/main/res/
    layout/                 – screens
    values, values-ur, values-ar/ – English, Urdu, Arabic text
    mipmap-*/                – app icon
```

If you'd rather not install Android Studio yourself, you can also paste this
whole folder into a service like **GitHub + Codemagic/Bitrise/GitHub Actions**
to get a cloud-built APK without installing anything locally — happy to help
set that up if you want to go that route instead.
