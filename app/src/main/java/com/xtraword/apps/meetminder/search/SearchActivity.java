package com.xtraword.apps.meetminder.search;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.EditMeetingActivity;
import com.xtraword.apps.meetminder.Meeting;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.databases.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";

    private AdView adView;
    private DatabaseHelper databaseHelper;
    private MeetingAdapter meetingAdapter;

    private final List<Meeting> meetings = new ArrayList<>();

    private TextView noResultsText;
    private EditText searchInput;
    private RecyclerView searchResults;

    private boolean adLoaded = false;
    private boolean adDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        // Initialize views
        adView = findViewById(R.id.adView);
        searchInput = findViewById(R.id.search_input);
        searchResults = findViewById(R.id.search_results);
        noResultsText = findViewById(R.id.no_results_text);

        // Initialize AdMob
        MobileAds.initialize(
                getApplicationContext(),
                initializationStatus -> {
                    // Initialization completed
                }
        );

        // Initialize database
        databaseHelper = new DatabaseHelper(this);

        // Configure meeting adapter
        meetingAdapter = new MeetingAdapter(
                this,
                meetings,
                this::openEditMeetingActivity
        );

        searchResults.setLayoutManager(
                new LinearLayoutManager(this)
        );

        searchResults.setAdapter(meetingAdapter);

        // Display all meetings initially
        searchMeetings("");

        // Live search
        searchInput.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
                // No action required
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                searchMeetings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action required
            }
        });

        updateAdVisibility();
    }

    // --------------------------------------------------
    // SEARCH MEETINGS
    // --------------------------------------------------

    public void searchMeetings(String query) {

        List<Meeting> results =
                databaseHelper.searchMeetings(query);

        meetings.clear();

        if (results != null) {
            meetings.addAll(results);
        }

        meetingAdapter.notifyDataSetChanged();

        noResultsText.setVisibility(
                meetings.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // --------------------------------------------------
    // OPEN MEETING FOR EDITING
    // --------------------------------------------------

    public void openEditMeetingActivity(Meeting meeting) {

        if (meeting == null) {
            return;
        }

        Intent intent = new Intent(
                this,
                EditMeetingActivity.class
        );

        intent.putExtra(
                "meeting_id",
                meeting.getId()
        );

        startActivity(intent);
    }

    // --------------------------------------------------
    // AD REMOVAL PREFERENCE
    // --------------------------------------------------

    private boolean areAdsRemoved() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        return prefs.getBoolean(
                PREF_ADS_REMOVED,
                false
        );
    }

    // --------------------------------------------------
    // AD VISIBILITY
    // --------------------------------------------------

    private void updateAdVisibility() {

        if (adView == null) {
            return;
        }

        if (areAdsRemoved()) {

            adView.setVisibility(View.GONE);

            if (!adDestroyed) {
                adView.destroy();
                adDestroyed = true;
            }

        } else {

            // A destroyed AdView cannot be reused.
            if (adDestroyed) {
                return;
            }

            adView.setVisibility(View.VISIBLE);

            if (!adLoaded) {

                AdRequest adRequest =
                        new AdRequest.Builder().build();

                adView.loadAd(adRequest);

                adLoaded = true;
            }
        }
    }

    // --------------------------------------------------
    // ACTIVITY LIFECYCLE
    // --------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh results after a meeting was edited.
        if (databaseHelper != null
                && meetingAdapter != null
                && searchInput != null) {

            searchMeetings(
                    searchInput.getText().toString()
            );
        }

        updateAdVisibility();

        if (adView != null && !adDestroyed) {
            adView.resume();
        }
    }

    @Override
    protected void onPause() {

        if (adView != null && !adDestroyed) {
            adView.pause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        if (adView != null && !adDestroyed) {
            adView.destroy();
            adDestroyed = true;
            adView = null;
        }

        if (databaseHelper != null) {
            databaseHelper.close();
            databaseHelper = null;
        }

        super.onDestroy();
    }
}
