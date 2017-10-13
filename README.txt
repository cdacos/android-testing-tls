A simple Android app to test behaviour of pre-Lollipop (Android 5.0, SDK 20) with sites that
no longer support TLS 1.0. Between SDK 16 and 20, Android OS had the ciphers for TLS 1.2 but
they were not enabled. The app is a demonstration of how to enable it for these OS versions.

* Android 5.0+
Do not need any of this code.

* Google Play services security provider
See https://developer.android.com/training/articles/security-gms-provider.html
This is the best choice if your device has Google Play services installed. (e.g. You're not a
typical Amazon device user, or are based somewhere like China.)
Note: the sample code in "Updating Your Security Provider to Protect Against SSL Exploits" needed
some tweaking to work.

* DeprecatedTLSSocketFactory
See Florian Krauthan's excellent blog post:
https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/
It's a simple wrapper around SSLSocketFactory which enables the protocol "TLSv1.2".

* Ideal approach?
if Google Play services available and up to date
  install security provider
else if SDK > 19
  do nothing (at least TLS 1.2 works)
else
  set SSLSocketFactory to DeprecatedTLSSocketFactory (enable TLS 1.2)

* About checking with https://www.howsmyssl.com/
If you use the DeprecatedTLSSocketFactory it will always say "Your SSL client is Bad" because the
OS supports obsolete old ciphers. But it will enable tLS 1.2 which is what we want.
The phrase to look for is: "Your client is using TLS 1.2" which is "Good".
If the SSL socket factory override is not running it says instead: "Your client is using TLS 1.0"
which is "Bad".

You can enter sites that only support TLS 1.2 for clearer examples of SUCCESS and FAILURE.
Some of the sites that get "perfect" scores from Mozilla's Observatory only allow TLS 1.2.

