package com.c0d3m4513r.deadlockdetector.main;

import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Scanner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionSender implements com.c0d3m4513r.deadlockdetector.api.ActionSender {
    public static final ActionSender SENDER = new ActionSender();

    @Override
    public void action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, String data) {
        if (info.getPanelUrl().isEmpty()){
            ServerWatcherChild.logger.info("No panel url specified. Will not actually send a action.");
            return;
        }
        InputStream error=null;
        try{
            URL url = new URL(info.getPanelUrl() + api);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(requestMethod);
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+info.getKey());
            con.setFixedLengthStreamingMode(data.length());
            if (con instanceof HttpsURLConnection && info.getIgnore_ssl_cert_errors()){
                con = doTrustToCertificates((HttpsURLConnection) con);
            }
            con.setDoOutput(true);
            con.setDoInput(true);
            error=con.getErrorStream();
            //Data
            Writer ods = new OutputStreamWriter(con.getOutputStream());
            ods.write(data,0,data.length());
            ods.flush();
            ods.close();

            ServerWatcherChild.logger.info("Sent Action to " + info.getPanel().name() + " at '"+url+"'. Response below:");
            Scanner scn = new Scanner(con.getInputStream());
            while (scn.hasNextLine()) System.out.println(scn.nextLine());
        } catch (MalformedURLException mue){
            throw new RuntimeException(mue);
        } catch (IOException e){
            ServerWatcherChild.logger.error("Tried to send action to " + info.getPanel().name() + " at '"+ info.getPanelUrl() +api+"'. There was an error:");
            e.printStackTrace();
            if(error!=null){
                Scanner scn = new Scanner(error);
                while (scn.hasNextLine()) ServerWatcherChild.logger.error(scn.nextLine());
            }
        }
    }

    public HttpsURLConnection doTrustToCertificates(HttpsURLConnection connection) {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e){
            ServerWatcherChild.logger.error("Error occurred whilst setting SSLSocetFactory:",e);
        }
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                    ServerWatcherChild.logger.warn("URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        connection.setHostnameVerifier(hv);
        return connection;
    }
}
