package com.baseform.power;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {
    protected WebView webView;
    protected String url;
    protected String MOBILE_REQ = "?mobileReq=true";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra("url");
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLayout();
    }

    public void resetLoginToken() {
        SharedPreferences prefs = this.getSharedPreferences("com.baseform.power", Context.MODE_PRIVATE);
        prefs.edit().remove("token").apply();
    }

    public void setLoginToken(String loginToken) {
        SharedPreferences prefs = this.getSharedPreferences("com.baseform.power", Context.MODE_PRIVATE);
        prefs.edit().putString("token", loginToken).apply();
    }

    public String getLoginToken() {
        SharedPreferences prefs = this.getSharedPreferences("com.baseform.power", Context.MODE_PRIVATE);
        return prefs.getString("token", null);
    }

    public Map<String, String> getLoginHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (getLoginToken() != null) headers.put("token", getLoginToken());
        return headers;
    }

    public void initLayout() {
        final BaseActivity scope = this;

        final View back = findViewById(R.id.back);
        if (back != null)
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(scope, MainActivity.class);
                    startActivity(intent);
                }
            });

        final View challenges = findViewById(R.id.challenges);
        if (challenges != null)
            challenges.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (scope.webView == null) {
                    Intent intent = new Intent(scope, PageActivity.class);
                    intent.putExtra("url", scope.url);
                    intent.putExtra("path",  "&location=home");
                    scope.startActivity(intent);
                    return;
                }
                changeTab(R.id.challenges);
                scope.webView.loadUrl(scope.url + MOBILE_REQ + "&location=home");
                }
            });

        final View about = findViewById(R.id.about);
        if (about != null)
            about.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (scope.webView == null) {
                    Intent intent = new Intent(scope, PageActivity.class);
                    intent.putExtra("url", scope.url);
                    intent.putExtra("path",  "&location=about");
                    scope.startActivity(intent);
                    return;
                }
                changeTab(R.id.about);
                scope.webView.loadUrl(scope.url + MOBILE_REQ + "&location=about");
                }
            });

        final View account = findViewById(R.id.account);
        if (account != null)
            account.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                if (scope.webView == null) {
                    Intent intent = new Intent(scope, PageActivity.class);
                    intent.putExtra("url", scope.url);
                    intent.putExtra("path",  "&location=area");
                    scope.startActivity(intent);
                    return;
                }
                changeTab(R.id.account);
                scope.webView.loadUrl(scope.url + MOBILE_REQ + "&location=area");
                }
            });

        final View comment = findViewById(R.id.comment);
        if (comment != null)
            comment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getLoginToken() == null || getLoginToken().length() == 0) {
                        account.performClick();
                        return;
                    }
                    Intent intent = new Intent(scope, CommentActivity.class);
                    intent.putExtra("url", scope.url);
                    changeTab(R.id.comment);
                    startActivity(intent);
                }
            });
    }

    protected void changeTab(int tab) {
        findViewById(R.id.challenges).setAlpha(0.6f);
        findViewById(R.id.about).setAlpha(0.6f);
        findViewById(R.id.account).setAlpha(0.6f);
        findViewById(R.id.comment).setAlpha(0.6f);

        findViewById(tab).setAlpha(1f);
    }
}
