package com.baseform.power;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;

public class PageActivity extends BaseActivity {
    String currentPath = "";
    private PageActivity scope;
    private WebView mWebviewPop;
    private FrameLayout mContainer;

    public void onBackPressed(){

        if (webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        Bundle b = getIntent().getExtras();
        this.url = b.getString("url");

        if (b.getString("path") != null)
        this.currentPath = b.getString("path");

        initWebView();
    }

    private void initWebView() {
        mContainer = findViewById(R.id.webViewHolder);
        webView = findViewById(R.id.web);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.setScrollbarFadingEnabled(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("POWER Android");


        scope = this;


        webView.setWebViewClient(new CustomViewClient());
        webView.setWebChromeClient(new CustomChromeClient());

        if (currentPath.length() == 0) {
            currentPath = "&location=home";
            changeTab(R.id.challenges);
        } else {
            if (currentPath.equals("&location=home")) {
                changeTab(R.id.challenges);
            } else if (currentPath.equals("&location=account")) {
                changeTab(R.id.account);
            } else if (currentPath.equals("&location=about")) {
                changeTab(R.id.about);
            }
        }

        webView.loadUrl(this.url + MOBILE_REQ + currentPath, getLoginHeaders());
    }

    class CustomViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            String host = Uri.parse(url).getHost();

            String location = Uri.parse(url).getQueryParameter("location");
            if (location != null && location.contains("home")) {
                changeTab(R.id.challenges);
            }

            if (location != null && location.contains("about")) {
                changeTab(R.id.about);
            }

            if (location != null && location.contains("account")) {
                changeTab(R.id.account);
            }

            if (host.contains("power-h2020.eu"))
            {
                if(mWebviewPop!=null)
                {
                    mWebviewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebviewPop);
                    mWebviewPop=null;
                }

                if (Uri.parse(url).getQueryParameterNames().contains("logout_pub")) {
                    resetLoginToken();
                }

                return false;
            }

            if(host.equals("m.facebook.com") || host.equals("www.facebook.com") || host.equals("accounts.google.com"))
            {
                return false;
            }


            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler,
                                       SslError error) {
            Log.d("onReceivedSslError", "onReceivedSslError");
        }


        public void onPageStarted(WebView view, final String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);


            if (getLoginToken() == null || getLoginToken().length() == 0) {
                class ReceiveTokenCallback implements ApiAsyncTask.ReceiveCallback {

                    @Override
                    public void execute(String result, Activity activity) {
                        ((BaseActivity)activity).setLoginToken(result);
                    }
                }
                new ApiAsyncTask(scope, url, new ReceiveTokenCallback(), "GET", "token", null, getLoginHeaders()).execute();
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            String location = Uri.parse(url).getQueryParameter("location");
            if (location != null && location.contains("home")) {
                changeTab(R.id.challenges);
            }

            if (location != null && location.contains("about")) {
                changeTab(R.id.about);
            }

            if (location != null && location.contains("account")) {
                changeTab(R.id.account);
            }

        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);

            String host = Uri.parse(url).getHost();
            if (host.contains("power-h2020.eu"))
            {
                if(mWebviewPop!=null)
                    mWebviewPop.setVisibility(View.GONE);
                mContainer.removeView(mWebviewPop);
                mWebviewPop=null;
            }
        }

    }

    private class CustomChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {

            mWebviewPop = new WebView(scope);
            mWebviewPop.setVerticalScrollBarEnabled(false);
            mWebviewPop.setHorizontalScrollBarEnabled(false);
            mWebviewPop.setWebViewClient(new CustomViewClient());
            mWebviewPop.getSettings().setJavaScriptEnabled(true);
            mWebviewPop.getSettings().setSavePassword(false);
            mWebviewPop.getSettings().setUserAgentString("POWER Android");
            mWebviewPop.getSettings().setDomStorageEnabled(true);

            mWebviewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebviewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebviewPop);
            resultMsg.sendToTarget();


            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");
        }

    }
}
