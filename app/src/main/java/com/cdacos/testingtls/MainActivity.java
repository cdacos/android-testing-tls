package com.cdacos.testingtls;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends AppCompatActivity {
  private final static String SHARED_PREFERENCES_NAME = "app";
  private final static String FIX_STATE = "FIX_STATE";

  private WebView webView = null;
  private Button button = null;
  private EditText testUrl = null;
  private SSLSocketFactory sslSocketFactory = null;
  private HostnameVerifier hostnameVerifier = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    webView = (WebView)findViewById(R.id.webview);
    button = (Button)findViewById(R.id.togglefix);
    testUrl = (EditText)findViewById(R.id.testUrl);
    sslSocketFactory = null;

    SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    if (sharedPreferences.contains(FIX_STATE)) {
      if (sharedPreferences.getBoolean(FIX_STATE, false)) {
        sslSocketFactory = DeprecatedTLSSocketFactory.createInstance(null);
      }
    }

    downloadAsync(testUrl.getText().toString());
  }

  public void toggleFix(View view) {
    sslSocketFactory = sslSocketFactory == null ? DeprecatedTLSSocketFactory.createInstance(null) : null;
    SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(FIX_STATE, sslSocketFactory != null);
    editor.apply();
    downloadAsync(testUrl.getText().toString());
  }

  void downloadAsync(final String urlText) {
    new Thread() {
      @Override
      public void run() {
        download(urlText);
      }
    }.start();
  }

  void download(final String urlText) {
    try {
      final URL url = new URL(urlText);

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          if (sslSocketFactory != null) {
            button.setText("SSL 'override'");
          }
          else {
            button.setText("Default behaviour");
          }
        }
      });

      URLConnection request = url.openConnection();

      if (request instanceof HttpsURLConnection) {
        if (sslSocketFactory != null) ((HttpsURLConnection)request).setSSLSocketFactory(sslSocketFactory);
        if (hostnameVerifier != null) ((HttpsURLConnection)request).setHostnameVerifier(hostnameVerifier);
      }

      BufferedReader r = new BufferedReader(new InputStreamReader((InputStream)request.getContent()));
      final StringBuilder contents = new StringBuilder();
      String line;
      while ((line = r.readLine()) != null) {
        contents.append(line).append('\n');
      }

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          webView.loadDataWithBaseURL(url.toString(), contents.toString(), null, null, null);
          webView.scrollTo(0, 0);
        }
      });
    }
    catch (final Exception e) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          webView.loadDataWithBaseURL(null, Log.getStackTraceString(e), "text/plain", null, null);
        }
      });
    }
  }

  public void openPerfectSites(View view) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://http-observatory.security.mozilla.org/api/v1/getRecentScans?min=135&num=25")));
  }
}
