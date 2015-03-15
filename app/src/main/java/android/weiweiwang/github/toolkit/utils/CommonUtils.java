package android.weiweiwang.github.toolkit.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by weiwei on 15/3/14.
 */
public class CommonUtils {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(); // create once, reuse

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static long copyLarge(InputStream input, OutputStream output)
            throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
        long count = 0;
        int n = 0;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static final int CONNECTION_TIME_OUT = 15 * 1000;
    public static final int SOCKET_TIME_OUT = 15 * 1000;

    public static DefaultHttpClient getDefaultHttpClient() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        HttpParams params = new BasicHttpParams();
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(cm, params);

        HttpConnectionParams.setSoTimeout(defaultHttpClient.getParams(), SOCKET_TIME_OUT);
        HttpConnectionParams.setConnectionTimeout(defaultHttpClient.getParams(),
                CONNECTION_TIME_OUT);
        defaultHttpClient.getParams().setIntParameter(ClientPNames.MAX_REDIRECTS, 10);
        HttpClientParams
                .setCookiePolicy(defaultHttpClient.getParams(),
                        CookiePolicy.BROWSER_COMPATIBILITY);
        HttpProtocolParams
                .setUserAgent(
                        defaultHttpClient.getParams(),
                        "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.18) Gecko/20110628 Ubuntu/10.04 (lucid) Firefox/3.6.18");
        defaultHttpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3,
                true));
        return defaultHttpClient;
    }
}
