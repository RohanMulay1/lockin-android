# LockIn: Trading Risk Guardrail & Anti-Tilt OS

Android app (Kotlin, Jetpack Compose, MVVM) that enforces trading rules when willpower doesn't:

- **Hourly audits:** heads-up notifications at 09:15 through 15:15, Mon–Fri. Exact alarms that re-arm after reboot.
- **1R engine:** a 60-minute unskippable lockout on any of these:
  - −₹2,000 session P&L;
  - more than 6 trades in a day;
  - giving back 50% of a peak of ₹1,000 or more.
- **Pre-flight gate:** 5 checks before every trade. A 30-minute time stop with 25- and 30-minute alerts. A 15-minute cooldown after 2 losses in a row.
- **Tilt protocol:** a 180-second box-breathing overlay. A 3-minute craving grounding protocol.
- **Smoke-free tracker:** stopwatch, healing milestones, and an ongoing status notification.
- **History:** DataStore and Room storage, with CSV export.

## Build

CI (`.github/workflows/build.yml`) runs the unit tests and builds `app-debug.apk` on every push to `main`.

```
gh run download -R RohanMulay1/lockin-android -n app-debug -D dist
```

To install, copy `dist/app-debug.apk` to the phone, allow installs from unknown sources, and open the file.
