package com.march.socialsdk.helper;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

/**
 * CreateAt : 2016/12/3
 * Describe : wx发送http请求。获取token
 *
 * @author chendong
 */
public class HttpsRequestHelper {

    public static void sendHttpsRequest(final String url, final OnResultListener onResultListener) {
        final Capture<HttpsURLConnection> capture = new Capture<>();
        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, new TrustManager[]{new MyTrustManager()}, new SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());
                HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
                capture.set(conn);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();

            }
        }).continueWith(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                if (capture.get() != null) {
                    capture.get().disconnect();
                }
                if (onResultListener != null) {
                    if (task.isFaulted() || TextUtils.isEmpty(task.getResult())) {
                        onResultListener.onFailure(task.getError());
                    } else {
                        onResultListener.onSuccess(task.getResult());
                    }
                }
                return null;
            }
        },Task.UI_THREAD_EXECUTOR);
    }

    public interface OnResultListener {
        void onSuccess(String result);

        void onFailure(Exception exception);
    }

    private static class MyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


    private static class MyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
