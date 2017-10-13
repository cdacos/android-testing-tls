package com.cdacos.testingtls;

import android.content.DialogInterface;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class MainActivity extends AppCompatActivity implements ProviderInstaller.ProviderInstallListener {
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
    webView = findViewById(R.id.webview);
    sslSocketFactoryToggle = findViewById(R.id.sslSocketFactoryToggle);
    testUrl = findViewById(R.id.testUrl);
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

      contents.append("<h1 style='color:#00AAAA;'>CONNECTION ALLOWED</h1>");
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
      contents.append("<h1 style='color:#FF0000;'>CONNECTION FAILED</h1>");
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

  public void installGooglePlaySecurityProvider(View view) {
    int availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
    switch (availability) {
      case ConnectionResult.SUCCESS:
        Toast.makeText(this, "Google Play services SUCCESS (is available and up to date)", Toast.LENGTH_LONG).show();
        ProviderInstaller.installIfNeededAsync(this, this);
        break;
      case ConnectionResult.SERVICE_MISSING:
        Toast.makeText(this, "Google Play services SERVICE_MISSING", Toast.LENGTH_LONG).show();
        break;
      case ConnectionResult.SERVICE_UPDATING:
        Toast.makeText(this, "Google Play services SERVICE_UPDATING", Toast.LENGTH_LONG).show();
        ProviderInstaller.installIfNeededAsync(this, this);
        break;
      case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
        Toast.makeText(this, "Google Play services SERVICE_VERSION_UPDATE_REQUIRED", Toast.LENGTH_LONG).show();
        ProviderInstaller.installIfNeededAsync(this, this);
        break;
      case ConnectionResult.SERVICE_DISABLED:
        Toast.makeText(this, "Google Play services SERVICE_DISABLED", Toast.LENGTH_LONG).show();
        break;
      case ConnectionResult.SERVICE_INVALID:
        Toast.makeText(this, "Google Play services SERVICE_INVALID", Toast.LENGTH_LONG).show();
        break;
      default:
        Toast.makeText(this, "Google Play services ???" + Integer.toString(availability), Toast.LENGTH_LONG).show();
    }
  }

  //region Google Play Security GMS Provider
  // Taken from https://developer.android.com/training/articles/security-gms-provider.html
  // Curious the mistakes that needed to be fixed to get this to compile.
  // (The continuous loop when the upgrade fails...)
  private static final int ERROR_DIALOG_REQUEST_CODE = 1;

  private Boolean mRetryProviderInstall = null;

  /**
   * This method is only called if the provider is successfully updated
   * (or is already up-to-date).
   */
  @Override
  public void onProviderInstalled() {
    Toast.makeText(this, "Google Play services updated and security provider installed.", Toast.LENGTH_LONG).show();
    lastUrl = null;
    downloadAsync(testUrl.getText().toString());
  }

  /**
   * This method is called if updating fails; the error code indicates
   * whether the error is recoverable.
   */
  @Override
  public void onProviderInstallFailed(int errorCode, Intent recoveryIntent) {
    if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
      // Recoverable error. Show a dialog prompting the user to
      // install/update/enable Google Play services.
      GoogleApiAvailability.getInstance().showErrorDialogFragment(
          this,
          errorCode,
          ERROR_DIALOG_REQUEST_CODE,
          new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              // The user chose not to take the recovery action
              onProviderInstallerNotAvailable();
            }
          });
    }
    else {
      // Google Play services is not available.
      onProviderInstallerNotAvailable();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
      // Adding a fragment via GooglePlayServicesUtil.showErrorDialogFragment
      // before the instance state is restored throws an error. So instead,
      // set a flag here, which will cause the fragment to delay until
      // onPostResume.
      if (mRetryProviderInstall == null) {
        mRetryProviderInstall = true;
      }
      else {
        Toast.makeText(this, "Google Play services not updated and security provider could not be installed. Need to use DeprecatedTLSSocketFactory.", Toast.LENGTH_LONG).show();
        // This is where your app would then fallback to using DeprecatedTLSSocketFactory
      }
    }
  }

  /**
   * On resume, check to see if we flagged that we need to reinstall the
   * provider.
   */
  @Override
  protected void onPostResume() {
    super.onPostResume();
    if (mRetryProviderInstall != null && mRetryProviderInstall) {
      mRetryProviderInstall = false;
      // We can now safely retry installation.
      ProviderInstaller.installIfNeededAsync(this, this);
    }
  }

  private void onProviderInstallerNotAvailable() {
    // This is reached if the provider cannot be updated for some reason.
    // App should consider all HTTP communication to be vulnerable, and take
    // appropriate action.
    Toast.makeText(this, "Google Play services security provider could not be installed. Need to use DeprecatedTLSSocketFactory.", Toast.LENGTH_LONG).show();
    // This is where your app would then fallback to using DeprecatedTLSSocketFactory
  }
  //endregion
}
