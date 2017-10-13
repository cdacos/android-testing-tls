A simple Android app to test behaviour of pre-Lollipop (Android 5.0, SDK 21) with sites that
no longer support TLS 1.0. Between SDK 16 and 20, Android OS had the ciphers for TLS 1.2 but
they were not enabled. The app is a demonstration of how to enable it for these OS versions.

See Florian Krauthan's excellent blog post:
https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/

About checking with https://www.howsmyssl.com/
It will always say "Your SSL client is Bad" because the OS supports obsolete old ciphers.
The phrase to look for is: "Your client is using TLS 1.2" which is "Good".
If the SSL socket factory override is not running it says instead: "Your client is using TLS 1.0"
which is "Bad".

You can enter sites that only support TLS 1.2 for clearer examples of SUCCESS and FAILURE.