package com.ubuntuone.android.files.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import com.ubuntuone.android.files.Preferences;
import com.ubuntuone.android.files.UbuntuOneFiles;
import com.ubuntuone.api.sso.authorizer.OAuthAuthorizer;
import com.ubuntuone.api.sso.exceptions.TimeDriftException;

public class HttpManager {
	private static final ClientConnectionManager sManager;
	private static final DefaultHttpClient sClient;	
		
	static {
		final HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUserAgent(params, "com.ubuntuone.android.files/"
					+ Preferences.getCurrentVersionName(
							UbuntuOneFiles.getInstance().getApplicationContext()));

		HttpConnectionParams.setStaleCheckingEnabled(params, false);
		HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
		HttpConnectionParams.setSoTimeout(params, 20 * 1000);
		HttpConnectionParams.setSocketBufferSize(params, 8192);

		HttpClientParams.setRedirecting(params, false);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("https",
				SSLSocketFactory.getSocketFactory(), 443));
		// Allow connecting via http proxy.
		schemeRegistry.register(
				new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

		sManager = new ThreadSafeClientConnManager(params, schemeRegistry);
		sClient = new DefaultHttpClient(sManager, params);
		
		sClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(2, true) {
			@Override
			public boolean retryRequest(IOException exception, int executionCount,
					HttpContext context) {
				if (executionCount > 2) {
					return false;
				}
		        if (exception instanceof InterruptedIOException) {
		            // Timeout
		            return false;
		        }
		        if (exception instanceof UnknownHostException) {
		            // Unknown host
		            return false;
		        }
		        if (exception instanceof ConnectException) {
		            // Connection refused
		            return false;
		        }
		        if (exception instanceof NoHttpResponseException) {
	                // Retry if the server dropped connection on us
	                return true;
	            }
		        if (exception instanceof SSLException) {
		            // SSL handshake exception
		            return false;
		        }
		        HttpRequest request = getRequest(context);
		        boolean idempotent = isIdempotent(request); 
		        if (idempotent) {
		        	// Retry if the request is considered idempotent
		            return true;
		        }
		        return false;
			}
		});
		
		// XXX staging server certificate workaround only.
		// workaroundStagingCertificate();
	}
	
	/**
	 * Get {@link HttpRequest} from {@link ExecutionContext}.
	 * 
	 * @param context
	 *            Current {@link ExecutionContext}.
	 * @return The executed {@link HttpRequest}.
	 */
	private static HttpRequest getRequest(HttpContext context) {
		return (HttpRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
	}
	
	/**
	 * Check if the {@link HttpRequest} is idempotent.
	 * 
	 * @param request
	 *            The {@link HttpRequest} to check.
	 * @return True if request is idempotent, false otherwise.
	 */
	private static boolean isIdempotent(HttpRequest request) {
		return !(request instanceof HttpEntityEnclosingRequest);
	}
	
	/**
	 * Apply time fix to {@link OAuthAuthorizer} with HEAD request to
	 * either U1 or SSO based on current execution context.
	 * 
	 * @param request
	 *            The {@link HttpRequest} which caused 401.
	 * @param client
	 *            The {@link HttpClient} to use for HEAD request.
	 * @return True if time fix was applied and request should be retried,
	 *         false otherwise.
	 */
	@SuppressWarnings("unused")
	private static boolean syncTime(HttpRequest request, HttpClient client) {
		String uri = request.getRequestLine().getUri();
		try {
			if (uri.contains("one.ubuntu.com")) {
				OAuthAuthorizer.syncTimeWithU1(sClient);
			} else {
				OAuthAuthorizer.syncTimeWithSSO(sClient);
			}
			return true;
		} catch (TimeDriftException e) {
			// Can't adjust time, give up.
			return false;
		}
	}
	
	public static DefaultHttpClient getClient() {
		return sClient;
	}
	
	public static void shutdown() {
		sManager.shutdown();
	}
	
	@SuppressWarnings("unused")
	private void workaroundStagingCertificate() {
		SSLSocketFactory sslSocketFactory = (SSLSocketFactory) sClient
				.getConnectionManager().getSchemeRegistry().getScheme("https")
				.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(new AllowAllHostnameVerifier());
	}

	private HttpManager() {
	}
}
