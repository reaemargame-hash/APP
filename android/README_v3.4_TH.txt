ASCENT Fitness RPG 3.4 — Navigation & Persistent Running Dialog

* Home now includes a collapsible daily quest log, quick water/sleep/weight logging and current quest. No separate Quests view.
* Food and Pantry are integrated as tabs in one menu (Meals / Inventory / Shopping). Inventory data model unchanged.
* Training panel or Run menu opens a GPS popup over the current view. Close/reopen popup without calling gpsPause() or stopping tracking.
* Live GPS floating pill on screen whenever an activity is ongoing or paused. GPS finish pre-fills the manual activity log.
* Web/PWA: GPS can be suspended when phone locks/backgrounds; only Android native app foreground service is designed to continue tracking in that case.
* Existing user data uses localStorage key ascent_fitness_v1; back up JSON before deploying.

For Firebase: extract WEB zip, deploy its web contents to public/ascent, do not overwrite game root.
Android Studio: open android directory, compile locally; APK and locked-screen tracking still require on-device QA.
