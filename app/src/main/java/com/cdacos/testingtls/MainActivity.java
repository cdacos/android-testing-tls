package com.cdacos.testingtls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends AppCompatActivity {
  private final static String SHARED_PREFERENCES_NAME = "app";
  private final static String FIX_STATE = "FIX_STATE";

  private WebView webView = null;
  private Button sslSocketFactoryToggle = null;
  private EditText testUrl = null;
  private SSLSocketFactory sslSocketFactory = null;
  private HostnameVerifier hostnameVerifier = null;
  private String lastUrl = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    webView = (WebView)findViewById(R.id.webview);
    sslSocketFactoryToggle = (Button)findViewById(R.id.sslSocketFactoryToggle);
    testUrl = (EditText)findViewById(R.id.testUrl);
    sslSocketFactory = null;

    SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    if (sharedPreferences.contains(FIX_STATE)) {
      if (sharedPreferences.getBoolean(FIX_STATE, false)) {
        sslSocketFactory = DeprecatedTLSSocketFactory.createInstance(null);
      }
    }

    downloadAsync(testUrl.getText().toString());

    testUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
          downloadAsync(testUrl.getText().toString());
        }
        return false;
      }
    });
  }

  public void sslSocketFactoryToggle(View view) {
    lastUrl = null;
    sslSocketFactory = sslSocketFactory == null ? DeprecatedTLSSocketFactory.createInstance(null) : null;
    SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(FIX_STATE, sslSocketFactory != null);
    editor.apply();
    downloadAsync(testUrl.getText().toString());
  }

  void downloadAsync(final String urlText) {
    if (!TextUtils.equals(urlText, lastUrl)) {
      lastUrl = urlText;
      new Thread() {
        @Override
        public void run() {
          download(urlText);
        }
      }.start();
    }
  }

  void download(final String urlText) {
    final StringBuilder contents = new StringBuilder();
    contents.append("<div>");
    contents.append(new Date().toString());
    contents.append("</div>");
    loadContents(contents);
    try {
      final URL url = new URL(urlText);

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (sslSocketFactory != null) {
            sslSocketFactoryToggle.setText("SSL 'override'");
          }
          else {
            sslSocketFactoryToggle.setText("Default behaviour");
          }
        }
      });

      URLConnection request = url.openConnection();
      if (request instanceof HttpsURLConnection) {
        if (sslSocketFactory != null) ((HttpsURLConnection)request).setSSLSocketFactory(sslSocketFactory);
        if (hostnameVerifier != null) ((HttpsURLConnection)request).setHostnameVerifier(hostnameVerifier);
      }
      request.setConnectTimeout(5_000);
      request.setReadTimeout(5_000);
      request.connect();
      InputStream inputStream = request.getInputStream();

      contents.append("<h1 style='color:#00AAAA;'>SUCCESS</h1>");
      loadContents(contents);

      if (inputStream != null) {
        BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = r.readLine()) != null) {
          contents.append(line).append('\n');
        }
      }
    }
    catch (final Exception e) {
      contents.append("<h1 style='color:#FF0000;'>FAILED</h1>");
      contents.append(Log.getStackTraceString(e));
    }
    loadContents(contents);
  }

  private void loadContents(final StringBuilder contents) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        webView.loadDataWithBaseURL(null, contents.toString(), null, null, null);
        webView.scrollTo(0, 0);
      }
    });
  }

  public void openPerfectSites(View view) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://http-observatory.security.mozilla.org/api/v1/getRecentScans?min=135&num=25")));
  }
}
