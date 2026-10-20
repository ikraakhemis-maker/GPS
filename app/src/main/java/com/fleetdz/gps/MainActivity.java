package com.fleetdz.gps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://www.fleetdz.com/map";
    private static final String HOST = "fleetdz.com";

    private WebView webView;
    private SwipeRefreshLayout swipe;
    private ProgressBar progress;
    private LinearLayout offlineView;
    private boolean hadError = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0B3A4A);

        webView = new WebView(this);
        swipe = new SwipeRefreshLayout(this);
        swipe.addView(webView);
        swipe.setColorSchemeColors(0xFF14808A, 0xFFF59E0B);
        swipe.setOnRefreshListener(() -> webView.reload());
        root.addView(swipe, new FrameLayout.LayoutParams(-1, -1));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(-1, 8);
        plp.gravity = Gravity.TOP;
        root.addView(progress, plp);

        offlineView = buildOfflineView();
        root.addView(offlineView, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                String host = uri.getHost();
                if (host != null && (host.equals(HOST) || host.endsWith("." + HOST))) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                hadError = false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
                if (!hadError) offlineView.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    hadError = true;
                    swipe.setRefreshing(false);
                    offlineView.setVisibility(View.VISIBLE);
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            load();
        }
    }

    private void load() {
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            webView.loadUrl(TARGET_URL);
        } else {
            offlineView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private LinearLayout buildOfflineView() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(0xFF0B3A4A);
        l.setPadding(64, 64, 64, 64);
        l.setVisibility(View.GONE);
        l.setClickable(true);

        TextView t = new TextView(this);
        t.setText(R.string.offline_title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(20);
        t.setGravity(Gravity.CENTER);
        l.addView(t);

        TextView m = new TextView(this);
        m.setText(R.string.offline_msg);
        m.setTextColor(0xFFB8D4DA);
        m.setTextSize(14);
        m.setGravity(Gravity.CENTER);
        m.setPadding(0, 24, 0, 48);
        l.addView(m);

        Button b = new Button(this);
        b.setText(R.string.retry);
        b.setOnClickListener(v -> load());
        l.addView(b);
        return l;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        webView.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
