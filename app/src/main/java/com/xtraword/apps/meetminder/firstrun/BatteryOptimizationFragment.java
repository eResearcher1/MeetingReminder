package com.xtraword.apps.meetminder.firstrun;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.xtraword.apps.meetminder.R;

public class BatteryOptimizationFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_battery_optimization, container, false);
        TextView tvDescription = (TextView) view.findViewById(R.id.tvDescription);
        tvDescription.setText("To improve the app's performance, please disable battery optimization.");
        Button btnDisableBatteryOptimization = (Button) view.findViewById(R.id.btnDisableBatteryOptimization);
        btnDisableBatteryOptimization.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view2) {
                BatteryOptimizationFragment.this.m305xaed5a1f5(view2);
            }
        });
        return view;
    }

    void m305xaed5a1f5(View v) {
        disableBatteryOptimization();
    }

    private void disableBatteryOptimization() {
        Intent intent = new Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
        startActivity(intent);
    }
}
