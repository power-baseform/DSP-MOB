package com.baseform.power;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;

public class SplashActivity extends Activity {
    private long SPLASH_DISPLAY_LENGTH = 2000;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        View viewById = findViewById(R.id.splashId);
        viewById.animate().alpha(1f).setDuration(700);

        final SplashActivity scope = this;

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                Intent mainIntent = new Intent(scope,MainActivity.class);
                scope.startActivity(mainIntent);
                scope.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
