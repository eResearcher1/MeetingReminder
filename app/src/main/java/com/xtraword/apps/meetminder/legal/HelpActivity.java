
package com.xtraword.apps.meetminder.legal;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

public class HelpActivity extends BaseActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String PREF_ADS_REMOVED = "ads_removed";

    private AdView adView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        MobileAds.initialize(this, initializationStatus -> { });

        adView = findViewById(R.id.adView);
        webView = findViewById(R.id.webView);
        swipeRefreshLayout =
                findViewById(R.id.swipeRefreshLayout);
        progressBar = findViewById(R.id.progressBar);

        setupWebView();
        setupSwipeRefresh();
        updateAdVisibility();

        if (isConnected()) {
            webView.loadUrl("file:///android_asset/help.html");
        } else {
            webView.loadUrl(
                    "file:///android_asset/no_internet.html"
            );
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

                progressBar.setVisibility(
                        newProgress < 100
                                ? View.VISIBLE
                                : View.GONE
                );
            }
        });

        webView.setOnScrollChangeListener(
                (view, scrollX, scrollY,
                 oldScrollX, oldScrollY) -> {

                    swipeRefreshLayout.setEnabled(
                            scrollY == 0
                    );
                }
        );
    }

    private void setupSwipeRefresh() {

        swipeRefreshLayout.setEnabled(false);

        swipeRefreshLayout.setOnRefreshListener(() -> {

            if (isConnected()) {
                webView.reload();
            } else {
                Toast.makeText(
                        HelpActivity.this,
                        "No internet connection",
                        Toast.LENGTH_SHORT
                ).show();

                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private boolean isConnected() {

        ConnectivityManager cm =
                (ConnectivityManager)
                        getSystemService(
                                Context.CONNECTIVITY_SERVICE
                        );

        if (cm == null) {
            return false;
        }

        NetworkInfo activeNetwork =
                cm.getActiveNetworkInfo();

        return activeNetwork != null
                && activeNetwork.isConnected();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAdVisibility();
    }

    private void updateAdVisibility() {

        if (adView == null) {
            return;
        }

        if (areAdsRemoved()) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);

            AdRequest adRequest =
                    new AdRequest.Builder().build();

            adView.loadAd(adRequest);
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

    @Override
    protected void onDestroy() {

        if (adView != null) {
            adView.destroy();
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
