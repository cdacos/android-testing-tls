package com.cdacos.testingtls;

// https://blog.dev-area.net/2015/08/13/android-4-1-enable-tls-1-1-and-tls-1-2/

import android.os.Build;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Enables TLS 1.2 in pre-Lollipop OSes (requires API 16)
 */
public class DeprecatedTLSSocketFactory extends SSLSocketFactory {
  private SSLSocketFactory internalSSLSocketFactory;

  public static DeprecatedTLSSocketFactory createInstance(TrustManager[] trustManagers) {
    try {
//      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        return new DeprecatedTLSSocketFactory(trustManagers);
//      }
//      else {
//        return null;
//      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public DeprecatedTLSSocketFactory(TrustManager[] trustManagers) throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, trustManagers, null);
    internalSSLSocketFactory = context.getSocketFactory();
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return internalSSLSocketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return internalSSLSocketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket() throws IOException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
  }

  private Socket enableTLSOnSocket(Socket socket) {
    if (socket != null && (socket instanceof SSLSocket)) {
      ((SSLSocket)socket).setEnabledProtocols(new String[]{"TLSv1.2"});
    }
    return socket;
  }
}
