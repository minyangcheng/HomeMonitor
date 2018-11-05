package com.min.home.monitor.rtc;

import com.min.home.monitor.BuildConfig;
import com.min.home.monitor.bean.UserBean;
import com.min.home.monitor.util.LogUtil;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

/**
 * Created by minych on 18-11-2.
 */
public class SignalClient {

    private static SignalClient signalClient;
    private Socket client;
    private UserBean user;

    private SignalClient() {
        try {
            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{x509TrustManager}, new SecureRandom());
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    })
                    .sslSocketFactory(sslContext.getSocketFactory(), x509TrustManager)
                    .build();
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);
            IO.Options opts = new IO.Options();
            opts.callFactory = okHttpClient;
            opts.webSocketFactory = okHttpClient;
            client = IO.socket(BuildConfig.SIGNAL_SERVER_HOST, opts);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("创建信令客户端失败");
        }
    }

    public static SignalClient getInstance() {
        if (signalClient == null) {
            synchronized (SignalClient.class) {
                if (signalClient == null) {
                    signalClient = new SignalClient();
                }
            }
        }
        return signalClient;
    }

    public void connect() {
        if (!client.connected()) {
            client.on("connectedEvent", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String name = args[0].toString();
                    LogUtil.d("用户上线id-connectedEvent:" + name);
                    user = new UserBean(name, "");
                }
            });
            client.connect();
        }
    }

    public void disConnect() {
        client.disconnect();
    }

    public void on(String event, Emitter.Listener listener) {
        client.on(event, listener);
    }

    public void off(String event) {
        client.off(event);
    }

    public void emit(String event, Object... args) {
        client.emit(event, args);
    }

    public UserBean getUser() {
        return user;
    }

}
