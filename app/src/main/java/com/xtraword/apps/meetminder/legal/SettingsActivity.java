
package com.xtraword.apps.meetminder.legal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SwitchCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import com.xtraword.apps.meetminder.BaseActivity;
import com.xtraword.apps.meetminder.R;
import com.xtraword.apps.meetminder.firstrunfragment.PermissionActivity;

import java.util.Collections;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = "SettingsActivity";

    private static final String PREFS_NAME = "AppPreferences";
    private static final String PREFS_NAME2 = "app_prefs";

    private static final String PREF_RINGTONE_URI =
            "notification_tone";

    private static final String PREF_ADS_REMOVED =
            "ads_removed";

    private static final String PRODUCT_ID_REMOVE_ADS =
            "remove_ads";

    private static final int BILLING_OK =
            BillingClient.BillingResponseCode.OK;

    private Button buttonPermissions;
    private Button buttonRing;

    private TextView playButton;
    private TextView selectedTone;
    private TextView statusMessageTextView;

    private ProgressBar statusProgressBar;
    private SwitchCompat switchRemoveAds;

    private AdView adView;
    private BillingClient billingClient;

    private SharedPreferences billingPrefs;

    private MediaPlayer mediaPlayer;

    private boolean billingConnecting = false;
    private boolean purchaseInProgress = false;
    private boolean purchasePending = false;
    private boolean adLoaded = false;

    private final ActivityResultLauncher<Intent>
            ringtonePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() != RESULT_OK) {
                            return;
                        }

                        Intent data = result.getData();

                        if (data == null) {
                            return;
                        }

                        Uri selectedUri;

                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            selectedUri = data.getParcelableExtra(
                                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                                    Uri.class
                            );
                        } else {
                            selectedUri = data.getParcelableExtra(
                                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI
                            );
                        }

                        if (selectedUri != null) {
                            saveSelectedRingtone(selectedUri);
                            updateSelectedTone();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        billingPrefs = getSharedPreferences(
                PREFS_NAME2,
                MODE_PRIVATE
        );

        buttonPermissions = findViewById(
                R.id.buttonPermissions
        );

        buttonRing = findViewById(
                R.id.buttonRing
        );

        playButton = findViewById(
                R.id.playButton
        );

        selectedTone = findViewById(
                R.id.selectedTone
        );

        statusMessageTextView = findViewById(
                R.id.tv_status_message
        );

        statusProgressBar = findViewById(
                R.id.statusProgressBar
        );

        switchRemoveAds = findViewById(
                R.id.switch_remove_ads
        );

        adView = findViewById(R.id.adView);

        if (statusProgressBar != null) {
            statusProgressBar.setVisibility(View.GONE);
        }

        setupRingtoneSettings();
        setupPermissionsButton();
        setupRemoveAdsSwitch();

        MobileAds.initialize(
                getApplicationContext(),
                initializationStatus ->
                        Log.d(TAG, "AdMob initialized")
        );

        updateSwitchAndAdStatePostPurchaseAction();

        setupBillingClient();
    }

    // --------------------------------------------------
    // RINGTONE SETTINGS
    // --------------------------------------------------

    private void setupRingtoneSettings() {

        updateSelectedTone();

        if (buttonRing != null) {
            buttonRing.setOnClickListener(
                    view -> openRingtonePicker()
            );
        }

        if (playButton != null) {
            playButton.setOnClickListener(view -> {

                Uri ringtoneUri = getSavedRingtoneUri(this);

                if (ringtoneUri != null) {
                    playRingtone(this, ringtoneUri);
                }
            });
        }
    }

    private void openRingtonePicker() {

        Intent intent = new Intent(
                RingtoneManager.ACTION_RINGTONE_PICKER
        );

        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION
        );

        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                "Select Notification Tone"
        );

        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                getSavedRingtoneUri(this)
        );

        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
                true
        );

        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,
                false
        );

        try {
            ringtonePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to open ringtone picker", e);
            showStatus("Ringtone picker unavailable.");
        }
    }

    private void saveSelectedRingtone(Uri ringtoneUri) {

        SharedPreferences preferences =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        preferences.edit()
                .putString(
                        PREF_RINGTONE_URI,
                        ringtoneUri.toString()
                )
                .apply();
    }

    private Uri getSavedRingtoneUri(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        Uri defaultUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        String savedUri = preferences.getString(
                PREF_RINGTONE_URI,
                defaultUri != null
                        ? defaultUri.toString()
                        : ""
        );

        if (savedUri == null || savedUri.isEmpty()) {
            return defaultUri;
        }

        return Uri.parse(savedUri);
    }

    private void updateSelectedTone() {

        if (selectedTone == null) {
            return;
        }

        Uri ringtoneUri = getSavedRingtoneUri(this);

        if (ringtoneUri == null) {
            selectedTone.setText(
                    "Selected Ringtone: Default"
            );
            return;
        }

        try {

            Ringtone ringtone =
                    RingtoneManager.getRingtone(
                            this,
                            ringtoneUri
                    );

            String title = ringtone != null
                    ? ringtone.getTitle(this)
                    : "Default";

            selectedTone.setText(
                    "Selected Ringtone: " + title
            );

        } catch (Exception e) {

            Log.e(TAG, "Unable to read ringtone", e);

            selectedTone.setText(
                    "Selected Ringtone: Default"
            );
        }
    }

    private void playRingtone(
            Context context,
            Uri ringtoneUri) {

        stopRingtonePreview();

        try {

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(
                    context,
                    ringtoneUri
            );

            mediaPlayer.setOnCompletionListener(player -> {
                if (mediaPlayer == player) {
                    mediaPlayer = null;
                }
                player.release();
            });

            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {

            Log.e(TAG, "Error playing ringtone", e);

            stopRingtonePreview();

            showStatus("Unable to play ringtone.");
        }
    }

    private void stopRingtonePreview() {

        if (mediaPlayer == null) {
            return;
        }

        MediaPlayer player = mediaPlayer;
        mediaPlayer = null;

        try {
            if (player.isPlaying()) {
                player.stop();
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to stop preview", e);
        }

        try {
            player.release();
        } catch (Exception e) {
            Log.w(TAG, "Unable to release player", e);
        }
    }

    // --------------------------------------------------
    // PERMISSIONS
    // --------------------------------------------------

    private void setupPermissionsButton() {

        if (buttonPermissions != null) {

            buttonPermissions.setOnClickListener(view -> {

                Intent intent = new Intent(
                        SettingsActivity.this,
                        PermissionActivity.class
                );

                startActivity(intent);
            });
        }

        updatePermissionsButtonText();
    }

    private void updatePermissionsButtonText() {
        // The APK leaves this method without
        // additional behavior.
    }

    // --------------------------------------------------
    // REMOVE ADS SWITCH
    // --------------------------------------------------

    private void setupRemoveAdsSwitch() {

        if (switchRemoveAds == null) {
            return;
        }

        switchRemoveAds.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (!buttonView.isPressed()) {
                        return;
                    }

                    if (isChecked) {

                        if (areAdsRemoved()) {
                            switchRemoveAds.setChecked(true);
                            return;
                        }

                        initiatePurchase();

                    } else {

                        if (areAdsRemoved()) {

                            switchRemoveAds.setChecked(true);

                            showStatus(
                                    "Ads removal is permanent."
                            );

                        } else {

                            showStatus("");
                        }
                    }
                }
        );
    }

    // --------------------------------------------------
    // GOOGLE PLAY BILLING
    // --------------------------------------------------

    private void setupBillingClient() {

        PendingPurchasesParams pendingParams =
                PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build();

        billingClient = BillingClient.newBuilder(this)
                .setListener(
                        (billingResult, purchases) ->
                                onPurchasesUpdated(
                                        billingResult,
                                        purchases
                                )
                )
                .enablePendingPurchases(pendingParams)
                .enableAutoServiceReconnection()
                .build();

        connectBilling(false);
    }

    private void connectBilling(boolean retryPurchase) {

        if (billingClient == null || billingConnecting) {
            return;
        }

        if (billingClient.isReady()) {

            if (retryPurchase) {
                initiatePurchase();
            } else {
                checkIfPurchased();
            }

            return;
        }

        billingConnecting = true;

        if (retryPurchase) {
            setBusy(true, "Connecting to billing...");
        }

        billingClient.startConnection(
                new BillingClientStateListener() {

                    @Override
                    public void onBillingSetupFinished(
                            BillingResult billingResult) {

                        runOnUiThread(() -> {

                            billingConnecting = false;

                            if (billingResult.getResponseCode()
                                    == BILLING_OK) {

                                if (retryPurchase) {

                                    purchaseInProgress = false;
                                    initiatePurchase();

                                } else {

                                    checkIfPurchased();
                                }

                            } else {

                                purchaseInProgress = false;

                                setBusy(
                                        false,
                                        "Billing setup failed. Try again."
                                );

                                updateSwitchAndAdStatePostPurchaseAction();
                            }
                        });
                    }

                    @Override
                    public void onBillingServiceDisconnected() {

                        billingConnecting = false;

                        Log.w(
                                TAG,
                                "Billing service disconnected"
                        );
                    }
                }
        );
    }

    private void initiatePurchase() {

        if (areAdsRemoved()) {
            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        if (purchaseInProgress) {
            return;
        }

        if (billingClient == null) {
            showStatus("Billing unavailable.");
            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        if (!billingClient.isReady()) {
            connectBilling(true);
            return;
        }

        purchaseInProgress = true;

        setBusy(
                true,
                "Fetching product details..."
        );

        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId(PRODUCT_ID_REMOVE_ADS)
                        .setProductType(
                                BillingClient.ProductType.INAPP
                        )
                        .build();

        QueryProductDetailsParams params =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                Collections.singletonList(product)
                        )
                        .build();

        billingClient.queryProductDetailsAsync(
                params,
                (billingResult, result) ->
                        runOnUiThread(() ->
                                handleProductDetails(
                                        billingResult,
                                        result
                                )
                        )
        );
    }

    private void handleProductDetails(
            BillingResult billingResult,
            QueryProductDetailsResult result) {

        if (billingResult.getResponseCode() != BILLING_OK) {

            purchaseInProgress = false;

            setBusy(
                    false,
                    "Error fetching product details."
            );

            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        List<ProductDetails> products =
                result != null
                        ? result.getProductDetailsList()
                        : null;

        ProductDetails selectedProduct = null;

        if (products != null) {

            for (ProductDetails product : products) {

                if (PRODUCT_ID_REMOVE_ADS.equals(
                        product.getProductId())) {

                    selectedProduct = product;
                    break;
                }
            }
        }

        if (selectedProduct == null) {

            purchaseInProgress = false;

            setBusy(
                    false,
                    "Remove Ads product unavailable."
            );

            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        ProductDetails.OneTimePurchaseOfferDetails offer =
                selectedProduct.getOneTimePurchaseOfferDetails();

        if (offer == null) {

            purchaseInProgress = false;

            setBusy(
                    false,
                    "Product offer unavailable."
            );

            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(selectedProduct)
                        .build();

        BillingFlowParams billingFlowParams =
                BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                                Collections.singletonList(
                                        productParams
                                )
                        )
                        .build();

        BillingResult launchResult =
                billingClient.launchBillingFlow(
                        this,
                        billingFlowParams
                );

        purchaseInProgress = false;

        if (launchResult.getResponseCode() == BILLING_OK) {

            setBusy(
                    false,
                    "Purchase flow started..."
            );

        } else {

            setBusy(
                    false,
                    "Failed to start purchase. Try again."
            );

            updateSwitchAndAdStatePostPurchaseAction();
        }
    }

    // --------------------------------------------------
    // PURCHASE UPDATES
    // --------------------------------------------------

    private void onPurchasesUpdated(
            BillingResult billingResult,
            List<Purchase> purchases) {

        runOnUiThread(() -> {

            int responseCode =
                    billingResult.getResponseCode();

            if (responseCode == BILLING_OK
                    && purchases != null) {

                boolean foundProduct = false;

                for (Purchase purchase : purchases) {

                    if (purchase.getProducts().contains(
                            PRODUCT_ID_REMOVE_ADS)) {

                        foundProduct = true;
                        handlePurchase(purchase);
                    }
                }

                if (!foundProduct) {

                    setBusy(
                            false,
                            "Purchase confirmed for unexpected item."
                    );

                    updateSwitchAndAdStatePostPurchaseAction();
                }

            } else if (responseCode ==
                    BillingClient.BillingResponseCode.USER_CANCELED) {

                setBusy(false, "Purchase cancelled.");

                updateSwitchAndAdStatePostPurchaseAction();

            } else {

                Log.e(
                        TAG,
                        "Purchase error: "
                                + billingResult.getDebugMessage()
                );

                setBusy(
                        false,
                        "Purchase failed. Try again."
                );

                updateSwitchAndAdStatePostPurchaseAction();
            }
        });
    }

    private void handlePurchase(Purchase purchase) {

        if (!purchase.getProducts().contains(
                PRODUCT_ID_REMOVE_ADS)) {
            return;
        }

        if (purchase.getPurchaseState()
                == Purchase.PurchaseState.PENDING) {

            purchasePending = true;

            setBusy(
                    true,
                    "Purchase pending. Ads will be removed "
                            + "once payment is confirmed."
            );

            return;
        }

        if (purchase.getPurchaseState()
                != Purchase.PurchaseState.PURCHASED) {

            purchasePending = false;

            setBusy(
                    false,
                    "Purchase status unclear."
            );

            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        purchasePending = false;

        if (purchase.isAcknowledged()) {

            grantRemoveAdsEntitlement();

            setBusy(
                    false,
                    "Ads are already removed."
            );

            updateSwitchAndAdStatePostPurchaseAction();
            return;
        }

        setBusy(true, "Processing purchase...");

        AcknowledgePurchaseParams params =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(
                                purchase.getPurchaseToken()
                        )
                        .build();

        billingClient.acknowledgePurchase(
                params,
                result -> runOnUiThread(() -> {

                    if (result.getResponseCode() == BILLING_OK) {

                        grantRemoveAdsEntitlement();

                        setBusy(
                                false,
                                "Ads removed successfully!"
                        );

                    } else {

                        Log.e(
                                TAG,
                                "Purchase acknowledgement failed: "
                                        + result.getDebugMessage()
                        );

                        setBusy(
                                false,
                                "Purchase confirmed. "
                                        + "Acknowledgement failed; "
                                        + "please try again."
                        );
                    }

                    updateSwitchAndAdStatePostPurchaseAction();
                })
        );
    }

    // --------------------------------------------------
    // RESTORE PURCHASES
    // --------------------------------------------------

    private void checkIfPurchased() {

        if (billingClient == null
                || !billingClient.isReady()) {
            return;
        }

        QueryPurchasesParams params =
                QueryPurchasesParams.newBuilder()
                        .setProductType(
                                BillingClient.ProductType.INAPP
                        )
                        .build();

        billingClient.queryPurchasesAsync(
                params,
                (billingResult, purchases) ->
                        runOnUiThread(() -> {

                            if (billingResult.getResponseCode()
                                    != BILLING_OK) {

                                Log.e(
                                        TAG,
                                        "Purchase restore failed: "
                                                + billingResult.getDebugMessage()
                                );

                                updateSwitchAndAdStatePostPurchaseAction();
                                return;
                            }

                            boolean foundPurchase = false;

                            purchasePending = false;

                            if (purchases != null) {

                                for (Purchase purchase : purchases) {

                                    if (purchase.getProducts().contains(
                                            PRODUCT_ID_REMOVE_ADS)) {

                                        foundPurchase = true;

                                        handlePurchase(purchase);
                                    }
                                }
                            }

                            if (!foundPurchase) {

                                updateSwitchAndAdStatePostPurchaseAction();
                            }
                        })
        );
    }

    // --------------------------------------------------
    // AD REMOVAL PREFERENCES
    // --------------------------------------------------

    private void grantRemoveAdsEntitlement() {

        if (billingPrefs == null) {

            billingPrefs = getSharedPreferences(
                    PREFS_NAME2,
                    MODE_PRIVATE
            );
        }

        billingPrefs.edit()
                .putBoolean(
                        PREF_ADS_REMOVED,
                        true
                )
                .apply();

        Log.i(
                TAG,
                "Ads removal entitlement granted"
        );
    }

    public boolean areAdsRemoved() {

        if (billingPrefs == null) {

            billingPrefs = getSharedPreferences(
                    PREFS_NAME2,
                    MODE_PRIVATE
            );
        }

        return billingPrefs.getBoolean(
                PREF_ADS_REMOVED,
                false
        );
    }

    // --------------------------------------------------
    // AD DISPLAY
    // --------------------------------------------------

    private void updateAdVisibility() {

        if (adView == null) {
            return;
        }

        if (areAdsRemoved()) {

            adView.setVisibility(View.GONE);

        } else {

            adView.setVisibility(View.VISIBLE);

            if (!adLoaded) {

                AdRequest request =
                        new AdRequest.Builder().build();

                adView.loadAd(request);

                adLoaded = true;
            }
        }
    }

    public void updateSwitchAndAdStatePostPurchaseAction() {

        boolean adsRemoved = areAdsRemoved();

        if (switchRemoveAds != null) {

            switchRemoveAds.setChecked(adsRemoved);

            switchRemoveAds.setEnabled(
                    !adsRemoved
                            && !purchasePending
                            && !purchaseInProgress
                            && !billingConnecting
            );
        }

        updateAdVisibility();

        if (statusMessageTextView != null) {

            String currentStatus =
                    statusMessageTextView.getText()
                            .toString()
                            .toLowerCase();

            boolean preserveStatus =
                    currentStatus.contains("pending")
                            || currentStatus.contains("failed")
                            || currentStatus.contains("error")
                            || currentStatus.contains("processing")
                            || currentStatus.contains("started")
                            || currentStatus.contains("unavailable")
                            || currentStatus.contains("try again");

            if (adsRemoved) {

                showStatus("Ads are removed.");

            } else if (!preserveStatus) {

                showStatus("");
            }
        }

        if (statusProgressBar != null
                && !purchasePending
                && !purchaseInProgress
                && !billingConnecting) {

            statusProgressBar.setVisibility(View.GONE);
        }
    }

    // --------------------------------------------------
    // UI HELPERS
    // --------------------------------------------------

    private void showStatus(String message) {

        if (statusMessageTextView != null) {
            statusMessageTextView.setText(message);
        }
    }

    private void setBusy(
            boolean busy,
            String message) {

        showStatus(message);

        if (statusProgressBar != null) {

            statusProgressBar.setVisibility(
                    busy ? View.VISIBLE : View.GONE
            );
        }

        if (switchRemoveAds != null) {

            switchRemoveAds.setEnabled(
                    !busy && !areAdsRemoved()
            );

            if (!areAdsRemoved() && !busy) {
                switchRemoveAds.setChecked(false);
            }
        }
    }

    // --------------------------------------------------
    // ACTIVITY LIFECYCLE
    // --------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();

        updateSelectedTone();
        updatePermissionsButtonText();

        if (adView != null && !areAdsRemoved()) {
            adView.resume();
        }

        if (billingClient != null
                && billingClient.isReady()) {

            checkIfPurchased();
        }
    }

    @Override
    protected void onPause() {

        stopRingtonePreview();

        if (adView != null) {
            adView.pause();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        stopRingtonePreview();

        if (adView != null) {
            adView.destroy();
            adView = null;
        }

        if (billingClient != null) {

            billingClient.endConnection();
            billingClient = null;
        }

        super.onDestroy();
    }
}
