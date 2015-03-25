package com.nike.tools.bgm.client.http;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Component;

/**
 * Makes an httpclient with some tweaks for ssl convenience on internal networks.
 */
@Component
public class HttpClientSSLFactory
{
  /**
   * Returns a thread-safe (pooled) httpClient, with freely trusting ssl for convenience.
   * Assumes https on internal networks.
   */
  public HttpClient makeHttpClient()
  {
    HttpClientConnectionManager connectionManager = makeConnectionManager();
    return HttpClientBuilder.create().setConnectionManager(connectionManager).build();
  }

  /**
   * Makes a pooled httpclient connection manager, which uses a freely trusting ssl socket factory registry.
   */
  private HttpClientConnectionManager makeConnectionManager()
  {
    Registry<ConnectionSocketFactory> socketFactoryRegistry = makeConnectionSocketFactoryRegistry();
    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
    poolingHttpClientConnectionManager.setDefaultMaxPerRoute(50);
    poolingHttpClientConnectionManager.setMaxTotal(200);
    return poolingHttpClientConnectionManager;
  }

  /**
   * Makes a socket factory registry for http and https, which is freely trusting for ssl connections.
   */
  private Registry<ConnectionSocketFactory> makeConnectionSocketFactoryRegistry()
  {
    SSLContext sslContext = makeFreelyTrustingSSLContext();
    return RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))
        .build();
  }

  /**
   * SSLContext configured such that our TLS will accept all certificates.
   */
  private SSLContext makeFreelyTrustingSSLContext()
  {
    try
    {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      X509TrustManager trustManager = new X509TrustManager()
      {
        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException
        {
          //Do nothing - always trust
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException
        {
          //Do nothing - always trust
        }

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
          return null;
        }
      };

      sslContext.init(null, new TrustManager[] { trustManager }, null);

      return sslContext;
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }
    catch (KeyManagementException e)
    {
      throw new RuntimeException(e);
    }
  }

}
