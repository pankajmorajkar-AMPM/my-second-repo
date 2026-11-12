# Home Organizer (Android)

An app that tidies your phone's apps into categories. You can also set it as your Home screen.

## Features
- **Sorts apps automatically** into Social, Productivity, Finance, Shopping, Food & Delivery, Music & Video, Photos, Travel & Maps, News & Reading, Health & Fitness, Games, Tools, System and Other. It recognises popular Indian apps too (PhonePe, Paytm, GPay, Swiggy, Zomato, Flipkart, IRCTC and others).
- **Category chips** at the top let you jump to one group, and each chip shows how many apps it holds.
- **Instant search**: type a few letters and press Enter/Go to open the first match.
- **Favourites** appear in a ★ section at the top.
- **Move any app** to another category, or create your own (e.g. "Work", "Kids", "Daily").
- **Hide apps** you never use. They stay available under ⋮ → Show hidden apps.
- **Sorting**: A–Z, Most used (the app counts launches), or Recently installed.
- **Icons per row**: 3–6. You can also turn category grouping on or off.
- **Use as Home screen**: ⋮ → Set as home screen.
- The list updates by itself when you install or uninstall apps.
- No internet permission, no ads and no tracking. Everything is stored on the phone.

Long-press any app for: Favourite · Move to category · Hide · App info · Uninstall.

## Getting the APK

### Option A — GitHub (no software needed)
1. Create a new repository on github.com and upload everything in this folder, including the hidden `.github` folder.
2. Open the **Actions** tab. The "Build APK" job runs automatically (or click *Run workflow*).
3. When it turns green (about 3–5 min), open the run and download **HomeOrganizer-apk** from *Artifacts*. Unzip it to get `HomeOrganizer.apk`.

### Option B — Android Studio
1. Open this folder in Android Studio (File → Open).
2. Wait for Gradle sync, then use **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
3. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Command line (with the Android SDK installed): `./gradlew assembleDebug`

## Installing on the phone
Copy the APK to the phone and tap it. Allow "Install unknown apps" for your file manager or browser when Android asks.
Requires Android 8.0 or newer.
