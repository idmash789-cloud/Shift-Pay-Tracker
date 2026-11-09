# Shift & Pay Tracker — MVP Android

Free, fully offline Android app for US hourly and gig workers: track shifts
from several jobs at once, calculate estimated pay including overtime, tips
and mileage, and export the result. No account, no server, no login.

> **This is a genuinely new app — not an upgrade of Kalkulator Weton.**
> Weton stays a separate product (website-as-APK, `com.weton.app`).
> Shift & Pay Tracker is native Kotlin + Jetpack Compose, built for the US
> market from the start, because its features need real on-device performance.

- **Stack:** Kotlin + Jetpack Compose (Material 3), MVVM + Repository,
  Room (shift/job/pay data) + DataStore (settings), WorkManager
- **Package:** `com.shiftpay.tracker` · minSdk 26 · **target/compile SDK 36
  (Android 16 — required by Google Play for new apps)**
- **Version:** 1.0.0 (versionCode 1)
- **Spec:** `~/workspace/your_files/shift-pay-tracker-mvp-spec/shift-pay-tracker-mvp-spec.pdf`
  (Indonesian, v1.0, 7 Nov 2026) — the single source of truth for the MVP.
- **Design guide applied 9 Nov 2026:** lightweight / fast / stable +
  AdMob revenue optimisation without harming UX (see below).

## Locked MVP decisions

- Individual users only. No company / manager features.
- Full offline. All data is local (Room). No online/offline indicator anywhere.
- Multi-job + simultaneous clock-in. Clocking out one job never stops another.
- Recurring / rotating shift templates, including overnight shifts (one shift,
  dated on its start day).
- Widget: clock in / out in one tap without opening the app.
- Tips + mileage per shift. Mileage default $0.70/mile, adjustable in Settings.
- Pay periods: weekly / biweekly / semimonthly / monthly + payday countdown.
- Overtime: 40 hours/week × 1.5x by default, per job, adjustable per job.
  Paid/unpaid breaks, deductions, night/holiday differentials.
- CSV export: always free. PDF pay stub: only after a rewarded ad.
- Every pay figure is an **estimate**: Pay Summary and PDF carry
  "Estimate only — not legal or tax advice."
- Retention by design: history, payday countdown, templates and widget give
  a reason to open the app every workday — that daily use is what makes
  the ads earn, not more ads.

## Performance (target, not a guarantee on every device)

- Home interactive in ~2 s on a mid-range phone; no heavy work on the main
  thread; history uses lazy loading (`LazyColumn`).
- Small release AAB: R8 + resource shrinking on, no unneeded SDKs.
- Low RAM / battery: no polling, no location, no foreground service.
  The widget is refreshed once a day by WorkManager.
- Works fully offline, including airplane mode.
- Release optimisation: R8 + a starter Baseline Profile
  (`app/src/main/baseline-prof.txt` — regenerate with Macrobenchmark after
  the first working build). Monitor crash/ANR in Play Console Android Vitals.

## AdMob placement (the point is quality impressions, not more ads)

| Format | Placement | Rule |
| --- | --- | --- |
| Adaptive Banner | Bottom of Home and Pay Summary | Below clock in/out buttons — never covering them |
| Native Ads | Between history items (after the 3rd) | Clearly labelled "Ad", never disguised as a shift |
| Interstitial | After a completed action (e.g. viewing a pay summary) | Max 1×/session AND ≥3 min apart; never on app open, never while typing / clocking in |
| Rewarded Ads | PDF pay-stub export only | Always opt-in; CSV stays free if the ad fails |
| App Open Ads | After startup is ready | Optional, max 1×/session |

Requirements prepared in this repo: UMP Consent SDK (`util/ConsentManager.kt`
— consent is checked on open, ads start after it), test ad IDs during
development (`util/AdMobHelper.kt`), `PRIVACY_POLICY.md`, `docs/app-ads.txt`
(replace the placeholder publisher ID and host it on your domain before
release), Data Safety declaration draft in the privacy policy. AdMob also
requires app-ads.txt verification and an app-readiness review before full
serving on a new app — that happens in your AdMob account, not in code.
Mediation is a later step, once there is enough traffic to analyse.

## Revenue — honest maths, not a prediction

Revenue = impressions / 1,000 × eCPM. Example only: 100,000 impressions/month
at $1.00 eCPM = $100/month. Real income depends on country, category,
advertiser demand, format and impressions — it cannot be predicted from
the code. What moves it is daily active users and retention, plus clean
traffic (never click your own ads, no bots, no click exchange).

## Verified pay example (spec §5.3)

One job, $20/hr, 1.5x OT, 45 paid hours in a week, $60 tips, 100 miles
@ $0.70, $25 deduction → regular $800.00 + overtime $150.00 + tips $60.00
= **gross $1,010.00, net $985.00, mileage $70.00 shown separately.**
This is implemented in `data/PayCalculator.kt` as pure logic (no Android).

## Screens

Home (timers + payday countdown) · Jobs · Add/Edit Shift · History ·
Templates · Pay Summary (CSV/PDF) · Settings (mileage rate, deductions) ·
Home-screen widget.

## Build in Android Studio (on your laptop)

1. Clone this repo: `git clone https://github.com/idmash789-cloud/Shift-Pay-Tracker.git`
2. Open the cloned folder in Android Studio (Ladybug or newer, with SDK 36
   installed — Android Studio will offer to install it on first sync).
3. Let Gradle sync finish (first sync downloads dependencies — needs internet
   once; the app itself works fully offline afterwards, including airplane mode).
4. Run on a device / emulator. If sync fails, check the Gradle JDK is 17
   (Settings → Build → Gradle → Gradle JDK) and try File → Sync Project
   with Gradle Files again.
5. Build → Generate Signed App Bundle / APK for release (AAB for Play Store).
   Before a Play Store release, replace the AdMob **test** IDs in
   `util/AdMobHelper.kt` and the test App ID in `AndroidManifest.xml` with
   production IDs, host `docs/app-ads.txt` on your domain, and complete
   Privacy Policy + Data Safety in Play Console.

This machine (Muse) has no JDK / Android SDK, so the first compile happens
in Android Studio on your laptop. If the build reports an error, send the
error text / screenshot back and it will be fixed here and pushed again.

## Play Store preparation (before soft launch)

Privacy Policy (in this repo) · Data Safety declaration · app-ads.txt hosted
· screenshots · AAB · staged rollout · watch retention + AdMob revenue in
Play Console / AdMob, then optimise formats and frequency caps from data.

## Acceptance checklist (spec §9.1)

- [ ] Two jobs clock in at once; clocking out one does not stop the other
- [ ] Overnight shift is one shift, counted in its start-date week
- [ ] §5.3 example reproduces exactly: $1,010.00 / $985.00 / $70.00
- [ ] Overtime changes correctly when threshold / multiplier change per job
- [ ] Rotating template creates shifts on the right days, including nights
- [ ] Widget clocks in / out without opening the app
- [ ] Payday countdown is correct for all four period types
- [ ] Any period exports to CSV with no ads; PDF only after a rewarded ad
- [ ] Whole core flow works in airplane mode, with no network message
- [ ] "Estimate" label + disclaimer appear in Pay Summary and in the PDF
- [ ] Release AAB builds with R8; target SDK 36; consent form appears where required
