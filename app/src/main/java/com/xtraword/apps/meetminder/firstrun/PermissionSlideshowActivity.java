package com.xtraword.apps.meetminder.firstrun;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.xtraword.apps.meetminder.MainActivity;
import com.xtraword.apps.meetminder.R;

public class PermissionSlideshowActivity extends AppCompatActivity {
    private Button btnStartApp;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_slideshow);
        this.viewPager = (ViewPager2) findViewById(R.id.viewPager);
        this.btnStartApp = (Button) findViewById(R.id.btnStartApp);
        final PermissionSlideAdapter adapter = new PermissionSlideAdapter(this);
        this.viewPager.setAdapter(adapter);
        this.btnStartApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                PermissionSlideshowActivity.this.m306x2d00e074(view);
            }
        });
        this.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == adapter.getItemCount() - 1) {
                    PermissionSlideshowActivity.this.btnStartApp.setVisibility(0);
                } else {
                    PermissionSlideshowActivity.this.btnStartApp.setVisibility(8);
                }
            }
        });
    }

    void m306x2d00e074(View v) {
        getSharedPreferences("app_prefs", 0).edit().putBoolean("first_launch_complete", true).apply();
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, (Class<?>) MainActivity.class);
        startActivity(intent);
        finish();
    }
}
