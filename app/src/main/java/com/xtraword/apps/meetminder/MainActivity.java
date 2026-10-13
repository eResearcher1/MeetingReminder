package com.xtraword.apps.meetminder;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.common.net.HttpHeaders;
import com.xtraword.apps.meetminder.firstrunfragment.SlideshowDialogFragment;
import com.xtraword.apps.meetminder.fragments.LayoutSwitchable;
import com.xtraword.apps.meetminder.legal.HelpActivity;
import com.xtraword.apps.meetminder.legal.OtherAppsActivity;
import com.xtraword.apps.meetminder.legal.PrivacyActivity;
import com.xtraword.apps.meetminder.legal.SettingsActivity;
import com.xtraword.apps.meetminder.manager.MeetingListActivity;
import com.xtraword.apps.meetminder.search.SearchActivity;

public class MainActivity extends BaseActivity {
    private static final String KEY_BATTERY_OPTIMIZATION_SHOWN = "battery_optimization_shown";
    public static final String KEY_LAYOUT_MODE = "LayoutMode";
    private static final String KEY_SLIDESHOW_SHOWN = "slideshow_shown";
    public static final int LAYOUT_GRID = 1;
    public static final int LAYOUT_LINEAR = 0;
    public static final String PREFS_NAME = "UserPreferences";
    private static final String PREFS_NAME2 = "app_prefs";
    private static final String PREFS_NAME3 = "app_prefs3";
    private static final String PREFS_NAME_BATTERY = "battery_optimization_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";
    AdView adView;
    private int currentLayoutMode;
    private DrawerLayout drawerLayout;
    GridLayoutManager gridLayoutManager;
    private ImageButton layoutSwitchIcon;
    LinearLayoutManager linearLayoutManager;
    private MeetingPagerAdapter meetingPagerAdapter;
    TextView searchView;
    private ActionBarDrawerToggle toggle;
    Toolbar toolbar;
    ViewPager2 viewPager;
    private ViewPager2 viewPager2;
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() {
        @Override
        public final void onActivityResult(Object obj) {
            MainActivity.this.m291lambda$new$0$comxtrawordappsmeetminderMainActivity((Boolean) obj);
        }
    });
    private boolean isFirstCall = true;

    void m291lambda$new$0$comxtrawordappsmeetminderMainActivity(Boolean isGranted) {
        if (!isGranted.booleanValue()) {
            Toast.makeText(this, "Notification permission is required for alarms to work correctly.", 0).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PowerManager pm = (PowerManager) getSystemService("power");
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent("android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS");
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME3, 0);
        boolean slideshowShown = prefs.getBoolean(KEY_SLIDESHOW_SHOWN, false);
        if (!slideshowShown) {
            showSlideshowDialog();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_SLIDESHOW_SHOWN, true);
            editor.apply();
        }
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public final void onInitializationComplete(InitializationStatus initializationStatus) {
                MainActivity.lambda$onCreate$1(initializationStatus);
            }
        });
        this.adView = (AdView) findViewById(R.id.adView);
        this.adView.setVisibility(8);
        performAdVisibilityUpdate();
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(this.toolbar);
        this.toolbar.setElevation(4.0f);
        this.viewPager = (ViewPager2) findViewById(R.id.viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        MeetingPagerAdapter adapter = new MeetingPagerAdapter(this);
        this.viewPager.setAdapter(adapter);
        new TabLayoutMediator(tabLayout, this.viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0:
                        tab.setText("Pending");
                        break;
                    case 1:
                        tab.setText("Ongoing");
                        break;
                    case 2:
                        tab.setText("Past");
                        break;
                    case 3:
                        tab.setText("Calendar");
                        break;
                }
            }
        }).attach();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    TextView tabText = (TextView) customView.findViewById(R.id.tabText);
                    tabText.setTextColor(MainActivity.this.getResources().getColor(android.R.color.white));
                    tabText.setTextSize(16.0f);
                    tabText.setTypeface(null, 1);
                }
                if (tab.getPosition() == 3) {
                    fab.hide();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    TextView tabText = (TextView) customView.findViewById(R.id.tabText);
                    tabText.setTextColor(MainActivity.this.getResources().getColor(android.R.color.darker_gray));
                }
                if (tab.getPosition() == 3) {
                    fab.show();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m292lambda$onCreate$2$comxtrawordappsmeetminderMainActivity(view);
            }
        });
        this.layoutSwitchIcon = (ImageButton) findViewById(R.id.layoutSwitchIcon);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        this.currentLayoutMode = preferences.getInt(KEY_LAYOUT_MODE, 1);
        updateLayoutIcon();
        this.layoutSwitchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m293lambda$onCreate$3$comxtrawordappsmeetminderMainActivity(view);
            }
        });
        this.searchView = (TextView) findViewById(R.id.searchView);
        this.searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                MainActivity.this.m294lambda$onCreate$4$comxtrawordappsmeetminderMainActivity(view);
            }
        });
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigationView);
        this.toggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.string.open, R.string.close);
        this.drawerLayout.addDrawerListener(this.toggle);
        this.toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public final boolean onNavigationItemSelected(MenuItem menuItem) {
                return MainActivity.this.m295lambda$onCreate$5$comxtrawordappsmeetminderMainActivity(menuItem);
            }
        });
    }

    static void lambda$onCreate$1(InitializationStatus initializationStatus) {
    }

    void m292lambda$onCreate$2$comxtrawordappsmeetminderMainActivity(View v) {
        checkAndRequestExactAlarmPermission();
        checkPermissionAndLaunchActivity();
    }

    void m293lambda$onCreate$3$comxtrawordappsmeetminderMainActivity(View v) {
        toggleLayoutMode();
    }

    void m294lambda$onCreate$4$comxtrawordappsmeetminderMainActivity(View v) {
        Intent intent = new Intent(this, (Class<?>) SearchActivity.class);
        startActivity(intent);
    }

    boolean m295lambda$onCreate$5$comxtrawordappsmeetminderMainActivity(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.switchOffAds) {
            startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
        } else if (itemId == R.id.pendingMeetingsButton) {
            this.viewPager.setCurrentItem(0);
        } else if (itemId == R.id.ongoingMeetingsButton) {
            this.viewPager.setCurrentItem(1);
        } else if (itemId == R.id.pastMeetingsButton) {
            this.viewPager.setCurrentItem(2);
        } else if (itemId == R.id.calendarButton) {
            this.viewPager.setCurrentItem(3);
        } else if (itemId == R.id.settingsButton) {
            startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
        } else if (itemId == R.id.helpButton) {
            startActivity(new Intent(this, (Class<?>) HelpActivity.class));
        } else if (itemId == R.id.otherAppsButton) {
            startActivity(new Intent(this, (Class<?>) OtherAppsActivity.class));
        } else if (itemId == R.id.privacyButton) {
            startActivity(new Intent(this, (Class<?>) PrivacyActivity.class));
        } else if (itemId == R.id.rateButton) {
            rateApp();
        } else if (itemId == R.id.managerButton) {
            startActivity(new Intent(this, (Class<?>) MeetingListActivity.class));
        }
        this.drawerLayout.closeDrawers();
        return true;
    }

    private void performAdVisibilityUpdate() {
        if (this.isFirstCall) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public final void run() {
                    MainActivity.this.m296xd1bbeb30();
                }
            }, 60000L);
        } else {
            updateAdVisibility();
        }
    }

    void m296xd1bbeb30() {
        updateAdVisibility();
        this.isFirstCall = false;
    }

    private void showSlideshowDialog() {
        SlideshowDialogFragment slideshowDialog = new SlideshowDialogFragment();
        slideshowDialog.show(getSupportFragmentManager(), "SlideshowDialog");
    }

    private boolean areAdsRemoved() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME2, 0);
        return prefs.getBoolean(PREF_ADS_REMOVED, false);
    }

    private void toggleLayoutMode() {
        this.currentLayoutMode = this.currentLayoutMode == 0 ? 1 : 0;
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, 0);
        preferences.edit().putInt(KEY_LAYOUT_MODE, this.currentLayoutMode).apply();
        updateLayoutIcon();
        notifyFragmentsAboutLayoutChange();
    }

    private void updateLayoutIcon() {
        if (this.currentLayoutMode == 1) {
            this.layoutSwitchIcon.setImageResource(R.drawable.ic_linear);
        } else {
            this.layoutSwitchIcon.setImageResource(R.drawable.ic_grid);
        }
    }

    private void notifyFragmentsAboutLayoutChange() {
        for (ActivityResultCaller activityResultCaller : getSupportFragmentManager().getFragments()) {
            if (activityResultCaller instanceof LayoutSwitchable) {
                ((LayoutSwitchable) activityResultCaller).onLayoutModeChanged(this.currentLayoutMode);
            }
        }
    }

    public int getCurrentLayoutMode() {
        return this.currentLayoutMode;
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(NotificationCompat.CATEGORY_ALARM);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM");
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setToolbarVisibility(boolean isVisible) {
        if (this.toolbar != null) {
            this.toolbar.setVisibility(isVisible ? 0 : 8);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        performAdVisibilityUpdate();
    }

    private void updateAdVisibility() {
        if (areAdsRemoved()) {
            this.adView.setVisibility(8);
            this.adView.destroy();
        } else {
            AdRequest adRequest = new AdRequest.Builder().build();
            this.adView.loadAd(adRequest);
            this.adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    MainActivity.this.adView.setVisibility(0);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    MainActivity.this.adView.setVisibility(8);
                }
            });
        }
    }

    public void rateApp() {
        try {
            Intent rateIntent = rateIntentForUrl("market://details");
            startActivity(rateIntent);
        } catch (ActivityNotFoundException e) {
            Intent rateIntent2 = rateIntentForUrl("https://play.google.com/store/apps/details");
            startActivity(rateIntent2);
        }
    }

    private Intent rateIntentForUrl(String url) {
        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(String.format("%s?id=%s", url, getPackageName())));
        int flags = 1207959552 | 524288;
        intent.addFlags(flags);
        return intent;
    }

    private void checkPermissionAndLaunchActivity() {
        if (Build.VERSION.SDK_INT >= 33) {
            boolean isNotificationPermissionGranted = ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == 0;
            if (isNotificationPermissionGranted) {
                Intent intent = new Intent(this, (Class<?>) AddMeetingActivity.class);
                startActivity(intent);
                return;
            } else {
                Toast.makeText(this, "Notification permission is required to proceed.", 0).show();
                requestNotificationPermission();
                return;
            }
        }
        Intent intent2 = new Intent(this, (Class<?>) AddMeetingActivity.class);
        startActivity(intent2);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.POST_NOTIFICATIONS") != 0) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.POST_NOTIFICATIONS")) {
                    new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("This app requires notification permission to send you important alerts.").setPositiveButton(HttpHeaders.ALLOW, new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.m297x1d93f9fb(dialogInterface, i);
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public final void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).show();
                    return;
                } else {
                    this.requestNotificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
                    return;
                }
            }
            Toast.makeText(this, "Notification permission is already granted.", 0).show();
        }
    }

    void m297x1d93f9fb(DialogInterface dialog, int which) {
        this.requestNotificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS");
    }
}
