package com.c0d3m4513r.deadlockdetector.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.var;
import com.c0d3m4513r.logger.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionSenderImpl implements com.c0d3m4513r.deadlockdetector.api.ActionSender {
    public static final ActionSenderImpl SENDER = new ActionSenderImpl();

    @Override
    public @NonNull Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, @NonNull Logger logger, @NonNull String data) {
        if (info.getPanelUrl().isEmpty()){
            if (logger != null) logger.info("No panel url specified. Will not actually send a action.");
            return Optional.empty();
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

            if (logger != null) logger.info("Sent Action to " + info.getPanel().name() + " at '"+url+"'. Response below:");
            var reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String output = reader.lines().collect(Collectors.joining("\n"));
            if (logger != null) logger.info("Request result is: {}",output);
            return Optional.of(output);
        } catch (MalformedURLException mue){
            throw new RuntimeException(mue);
        } catch (IOException e){
            if (logger != null) logger.error("Tried to send action to " + info.getPanel().name() + " at '"+ info.getPanelUrl() +api+"'. There was an error:", e);
            if(error!=null){
                Scanner scn = new Scanner(error);
                while (scn.hasNextLine() && logger != null) logger.error(scn.nextLine());
            }
            return Optional.empty();
        }
    }

    public HttpsURLConnection doTrustToCertificates(HttpsURLConnection connection, Logger logger) {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException | NoSuchAlgorithmException e){
            logger.error("Error occurred whilst setting SSLSocetFactory:",e);
        }
        HostnameVerifier hv = (urlHostName, session) -> {
            if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                logger.warn("URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
            }
            return true;
        };
        connection.setHostnameVerifier(hv);
        return connection;
    }
}
