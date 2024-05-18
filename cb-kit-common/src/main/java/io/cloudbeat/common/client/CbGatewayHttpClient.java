package io.cloudbeat.common.client;

import io.cloudbeat.common.client.api.GatewayApi;
import io.cloudbeat.common.client.api.GatewayApiRetro;
import io.cloudbeat.common.client.api.RuntimeApi;
import io.cloudbeat.common.client.api.RuntimeApiRetro;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static javax.print.attribute.standard.ReferenceUriSchemesSupported.HTTPS;

public class CbGatewayHttpClient {
    final static String DEFAULT_BASE_URL = "https://api.cloudbeat.io";
    //final static String DEFAULT_BASE_URL = "http://212.80.207.119:8887";
    final static long DEFAULT_TIMEOUT_MIN = 60;
    final String baseUrl;
    final String token;
    OkHttpClient client;
    Retrofit retrofit;
    GatewayApi gatewayApi;
    TrustManager TRUST_ALL_CERTS = new X509TrustManager() {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    };

    public CbGatewayHttpClient(final String url, final String token) throws CbClientException, MalformedURLException {
        this.baseUrl = getBaseUrl(url);
        this.token = token;

        try {
            init();
        } catch (NoSuchAlgorithmException | KeyManagementException |MalformedURLException e) {
            throw new CbClientException(e);
        }
    }

    private static String getBaseUrl(String gatewayUrl) throws MalformedURLException {
        URL url = new URL(gatewayUrl);
        String baseUrl = String.format("%s://%s:%s", url.getProtocol(), url.getHost(), url.getPort());
        return baseUrl;
    }

    private void init() throws NoSuchAlgorithmException, KeyManagementException, MalformedURLException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            // add apiKey to every API call
            .callTimeout(DEFAULT_TIMEOUT_MIN, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_MIN, TimeUnit.SECONDS)
            .addInterceptor(new Interceptor() {
                @NotNull
                @Override
                public Response intercept(@NotNull Chain chain) throws IOException {
                    // add apiKey query parameter to the current request
                    Request request = chain.request();
                    HttpUrl url = request.url()
                            .newBuilder()
                            .build();
                    String bearer = String.format("Bearer %s", token);
                    Request.Builder requestBuilder = request.newBuilder()
                            .url(url)
                            .addHeader("Authorization", bearer)
                            .addHeader("Content-Type", "application/json");

                    return chain.proceed(requestBuilder.build());
                }
            });
        // set up HTTPS, if required
        if (HTTPS.equals(new URL(baseUrl).getProtocol())) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { TRUST_ALL_CERTS }, new java.security.SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) TRUST_ALL_CERTS);
            // support self-signed SSL certificates
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }
        client = builder.build();
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client)
                .build();
        gatewayApi = new GatewayApi(retrofit.create(GatewayApiRetro.class));
    }

    public GatewayApi getGatewayApi() {
        return gatewayApi;
    }
}
