
package com.xtraword.apps.meetminder.legal;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.R;

public class PrivacyActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";

    private static final String PRIVACY_URL =
            "https://www.askfoodscientists.com/meetminder";

    private static final String OFFLINE_URL =
            "file:///android_asset/no_internet.html";

    private AdView adView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;

    private boolean adDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_privacy);

        adView = findViewById(R.id.adView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout =
                findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);

        MobileAds.initialize(this, status -> {
            // Initialization completed.
        });

        setupWebView();
        setupSwipeRefresh();

        updateAdVisibility();

        if (isConnected()) {
            webView.loadUrl(PRIVACY_URL);
        } else {
            webView.loadUrl(OFFLINE_URL);
        }
    }

    private void setupWebView() {

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    Bitmap favicon) {

                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url) {

                super.onPageFinished(view, url);

                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(
                    WebView view,
                    int newProgress) {

                super.onProgressChanged(view, newProgress);

                progressBar.setVisibility(
                        newProgress < 100
                                ? View.VISIBLE
                                : View.GONE
                );
            }
        });
    }

    private void setupSwipeRefresh() {

        swipeRefreshLayout.setEnabled(false);

        webView.setOnScrollChangeListener(
                (view, scrollX, scrollY,
                 oldScrollX, oldScrollY) -> {

                    swipeRefreshLayout.setEnabled(
                            scrollY == 0
                    );
                }
        );

        swipeRefreshLayout.setOnRefreshListener(() -> {

            if (isConnected()) {

                String currentUrl = webView.getUrl();

                if (currentUrl == null
                        || currentUrl.startsWith("file:")) {

                    webView.loadUrl(PRIVACY_URL);

                } else {

                    webView.reload();
                }

            } else {

                Toast.makeText(
                        PrivacyActivity.this,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                ).show();

                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private boolean isConnected() {

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(
                        Context.CONNECTIVITY_SERVICE
                );

        if (cm == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            Network network = cm.getActiveNetwork();

            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    cm.getNetworkCapabilities(network);

            return capabilities != null
                    && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
            );

        } else {

            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            return networkInfo != null
                    && networkInfo.isConnected();
        }
    }

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

            if (adDestroyed) {
                return;
            }

            adView.setVisibility(View.VISIBLE);

            AdRequest adRequest =
                    new AdRequest.Builder().build();

            adView.loadAd(adRequest);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adView != null && !adDestroyed) {
            adView.resume();
        }

        updateAdVisibility();
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
        }

        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
