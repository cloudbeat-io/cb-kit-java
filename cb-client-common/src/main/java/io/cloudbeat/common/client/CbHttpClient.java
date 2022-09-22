package io.cloudbeat.common.client;

import io.cloudbeat.common.client.api.RuntimeApi;
import io.cloudbeat.common.client.api.RuntimeApiRetro;
import io.cloudbeat.common.reporter.model.RunStatus;
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

public class CbHttpClient {
    //final static String DEFAULT_BASE_URL = "https://api.cloudbeat.io";
    final static String DEFAULT_BASE_URL = "http://212.80.207.119:8887";
    final static long DEFAULT_TIMEOUT_MIN = 60;
    final String baseUrl;
    final String apiKey;
    OkHttpClient client;
    Retrofit retrofit;
    RuntimeApi runtimeApi;
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

    public CbHttpClient(final String apiKey) throws CbClientException {
        this(apiKey, DEFAULT_BASE_URL);
    }
    public CbHttpClient(final String apiKey, final String baseUrl) throws CbClientException {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;

        try {
            init();
        } catch (NoSuchAlgorithmException | KeyManagementException |MalformedURLException e) {
            throw new CbClientException(e);
        }
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
                            .addQueryParameter("apiKey", apiKey)
                            .build();
                    Request.Builder requestBuilder = request.newBuilder()
                            .url(url);

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
        runtimeApi = new RuntimeApi(retrofit.create(RuntimeApiRetro.class));
    }

    public RuntimeApi getRuntimeApi() {
        return runtimeApi;
    }
}
