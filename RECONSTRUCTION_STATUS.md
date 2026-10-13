# Meeting Reminder — APK-first reconstruction (UNVERIFIED)

Source of truth: `Meeting Reminder latest .apk`, via user-provided JADX export. Baseline: working Android Studio project.

## Changes performed
- Replaced 52 app-specific Java source files with recovered APK implementations (excluding generated binding, R, and synthetic $ classes).
- Normalized captured `this.f$0` references to qualified enclosing Activity/Service references where present.
- Restored 35 application layout XML files from APK, excluding known third-party library layouts.
- Kept existing Gradle dependencies, manifest, drawable, and values resources as the compatibility base; changed compileSdk to 36 to match APK compilation.
- Excluded generated/build/release outputs.

## IMPORTANT: this is a reconstruction candidate, NOT a verified ready-to-run build
- JADX output is not original Java source; lambda, obfuscated and anonymous code can still fail compilation.
- APK resources may have resource ID or style incompatibilities with retained baseline resources.
- No Android SDK/Gradle build available here; compilation and runtime equivalence are not verified.
- No claim is made that the APK's alarm, snooze, billing, or recurrence workflows work without further testing.
- The older database schema may require a migration strategy. Back up app data before installing.

## Build instructions
1. Open this folder (containing settings.gradle.kts) in Android Studio.
2. Install SDK 36 if prompted and sync Gradle.
3. Build > Make Project. Copy the first actual Java or AAPT error, not just the task summary.
4. Test alarms, snooze, recurrence, notifications, purchase status, and upgrade data on a test device.

## APK Java files reconstructed
- adapters/AlarmsAdapter.java
- adapters/ScheduledAlarmsAdapter.java
- AddMeetingActivity.java
- AlarmActivity.java
- alarms/AlarmHelper.java
- alarms/AlarmMeetingAdapter.java
- alarms/AlarmReceiverMM.java
- alarms/AlarmServicesMM.java
- alarms/BootReceiverMM.java
- alarms/DailySummaryReceiver.java
- alarms/DailySummaryScheduler.java
- alarms/MeetingAlarm.java
- alarms/SetAlarmsActivity.java
- BaseActivity.java
- cosmetics/FirstItemTopPaddingDecoration.java
- cosmetics/UniformSpacingDecoration.java
- databases/DatabaseHelper.java
- databases/DatabaseHelperAlarms.java
- EditMeetingActivity.java
- firstrun/BatteryOptimizationFragment.java
- firstrun/PermissionSlideAdapter.java
- firstrun/PermissionSlideshowActivity.java
- firstrun/SlideFragment.java
- firstrunfragment/PermissionActivity.java
- firstrunfragment/SlideFragment1.java
- firstrunfragment/SlideshowDialogFragment.java
- firstrunfragment/SlideshowPagerAdapter.java
- foreground/ReminderService.java
- fragments/CalenderFragment.java
- fragments/LayoutSwitchable.java
- fragments/OngoingMeetingsFragment.java
- fragments/PassedMeetingsFragment.java
- fragments/PendingMeetingsFragment.java
- legal/HelpActivity.java
- legal/OtherAppsActivity.java
- legal/PrivacyActivity.java
- legal/SettingsActivity.java
- MainActivity.java
- manager/Meeting.java
- manager/MeetingAdapterAll.java
- manager/MeetingListActivity.java
- Meeting.java
- MeetingAdapter.java
- MeetingPagerAdapter.java
- model/ScheduledAlarmLogEntry.java
- MyApplication.java
- search/MeetingAdapter.java
- search/SearchActivity.java
- SplashActivity.java
- TodayMeetingAdapter.java
- worker/ImmediateMeetingRolloverWorker.java
- worker/MeetingScheduler.java

## Restored layout XML
- res/layout/activity_add_meeting.xml
- res/layout/activity_alarm.xml
- res/layout/activity_edit_meeting.xml
- res/layout/activity_help.xml
- res/layout/activity_main.xml
- res/layout/activity_meeting_list.xml
- res/layout/activity_other_apps.xml
- res/layout/activity_permission.xml
- res/layout/activity_permission_slideshow.xml
- res/layout/activity_privacy.xml
- res/layout/activity_search.xml
- res/layout/activity_settings.xml
- res/layout/activity_set_alarms.xml
- res/layout/activity_splash.xml
- res/layout/custom_dialog.xml
- res/layout/custom_tab.xml
- res/layout/dialog_color_picker.xml
- res/layout/dialog_meeting_options.xml
- res/layout/fragment_battery_optimization.xml
- res/layout/fragment_calender.xml
- res/layout/fragment_ongoing_meetings.xml
- res/layout/fragment_passed_meetings.xml
- res/layout/fragment_pending_meetings.xml
- res/layout/fragment_slide.xml
- res/layout/fragment_slide1.xml
- res/layout/fragment_slideshow_dialog.xml
- res/layout/item_alarm.xml
- res/layout/item_alarm_meeting.xml
- res/layout/item_meeting.xml
- res/layout/item_meeting2.xml
- res/layout/item_meeting_linear.xml
- res/layout/item_scheduled_alarm.xml
- res/layout/item_search.xml
- res/layout/item_today_meeting.xml
- res/layout/nav_header_main.xml

## Remaining synthetic references
