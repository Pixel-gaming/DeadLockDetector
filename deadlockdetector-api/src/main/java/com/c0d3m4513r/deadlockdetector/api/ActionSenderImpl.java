package com.c0d3m4513r.deadlockdetector.api;

import com.c0d3m4513r.deadlockdetector.api.panels.Panels;
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
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionSenderImpl implements com.c0d3m4513r.deadlockdetector.api.ActionSender {
    public static final ActionSenderImpl SENDER = new ActionSenderImpl();

    @Override
    public @NonNull Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, @NonNull Logger logger, @NonNull String data) {
        Panels panel = info.getPanel();
        if (info.getPanelUrl().isEmpty()){
            logger.info("No panel url specified. Will not actually send a action.");
            return Optional.empty();
        } else if (panel == null){
            logger.info("No valid panel specified. Will not actually send a action.");
            return Optional.empty();
        }
        HttpURLConnection con = null;
        try{
            URL url = new URL(info.getPanelUrl() + api);
            logger.info("Sending action to '{}' via '{}' with data '{}'",url.toString(),requestMethod,data);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+info.getKey());
            if (con instanceof HttpsURLConnection && info.getIgnore_ssl_cert_errors()){
                con = doTrustToCertificates((HttpsURLConnection) con, logger);
            }
            //This is a special check in the jdk, that sets the method to POST, even if the data is empty.
            con.setRequestMethod(requestMethod);
            con.setDoInput(true);

            if(!requestMethod.equals("GET")){
                con.setDoOutput(true);
                con.setFixedLengthStreamingMode(data.length());
                //Data
                try (var ods = con.getOutputStream()) {
                    //write request
                    ods.write(data.getBytes(), 0, data.getBytes().length);
                    ods.flush();
                }
            }

            try (var reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
                //read response
                logger.info("Sent Action to " + panel.name() + " at '" + url + "'. Response below:");
                String output = reader.lines().collect(Collectors.joining("\n"));
                logger.info("Request result is: {}", output);
                return Optional.of(output);
            }
        } catch (MalformedURLException mue){
            throw new RuntimeException(mue);
        } catch (IOException e){
            logger.error("Tried to send action to " + info.getPanel().name() + " at '"+ info.getPanelUrl() +api+"'. There was an error:", e);
            if(con == null) return Optional.empty();
            InputStream error = con.getErrorStream();
            if (error == null) return Optional.empty();
            try (var reader = new BufferedReader(new InputStreamReader(error))) {
                logger.error("Error stream is: {}", reader.lines().collect(Collectors.joining("\n")));
            }catch (IOException e2){
                logger.error("Error whilst reading error stream:", e2);
            }
            return Optional.empty();
        }
    }

    public HttpsURLConnection doTrustToCertificates(HttpsURLConnection connection, Logger logger) {
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
