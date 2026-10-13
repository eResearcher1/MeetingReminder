package com.xtraword.apps.meetminder;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    /* JADX WARN: Type inference failed for: r1v0, types: [com.xtraword.apps.meetminder.SplashActivity$1] */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        new CountDownTimer(2000L, 1000L) {
            @Override
            public void onFinish() {
                Intent startActivity = new Intent(SplashActivity.this, (Class<?>) MainActivity.class);
                SplashActivity.this.startActivity(startActivity);
                SplashActivity.this.finish();
            }

            @Override
            public void onTick(long millisUntilFinished) {
            }
        }.start();
    }
}
