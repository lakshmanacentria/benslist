package com.acentria.benslist;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;


/**
 * Created by fed9i on 12/23/15.
 */
public class AcceptActivity extends AppCompatActivity {
    public static AcceptActivity instance;
    private static Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Lang.setDirection(this);

		/* get account data from instance */
        intent = getIntent();
        String data = (String) intent.getSerializableExtra("data");

        String acceptType = "view";
        if (intent.getSerializableExtra("mode")!=null) {
            acceptType = (String) intent.getSerializableExtra("mode");
        }
        String page_name = Lang.get("android_terms_of_use");
        if (intent.getSerializableExtra("name")!=null) {
            page_name = (String) intent.getSerializableExtra("name");
        }
        setTitle(page_name);
        setContentView(R.layout.activity_accept);

        /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        LinearLayout content = (LinearLayout) findViewById(R.id.activity_accept);
        ScrollView accept_view = (ScrollView) findViewById(R.id.accept_view);
        WebView accept_webview = (WebView) findViewById(R.id.accept_webview);

        if(acceptType.equals("view")) {
            accept_view.setVisibility(View.VISIBLE);
            accept_webview.setVisibility(View.GONE);

            TextView accept_info = (TextView) content.findViewById(R.id.accept_info);
            accept_info.setText(data);
        }
        else {
            final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("android_loading"));
            accept_view.setVisibility(View.GONE);
            accept_webview.setVisibility(View.VISIBLE);

            accept_webview.getSettings().setJavaScriptEnabled(true); // enable javascript

            accept_webview.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    progress.dismiss();
                }
            });


            accept_webview.loadUrl(data);
        }

        Button accept = (Button) content.findViewById(R.id.accept);
        Button cancel = (Button) content.findViewById(R.id.cancel);

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK, intent);
                // finish activity
                instance.finish();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED, intent);
                // finish activity
                instance.finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}