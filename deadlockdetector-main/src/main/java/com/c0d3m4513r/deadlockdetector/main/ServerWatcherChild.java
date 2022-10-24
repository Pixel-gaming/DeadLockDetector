package com.c0d3m4513r.deadlockdetector.main;

import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

public class ServerWatcherChild {
    public static final String PERADACTYL_URL="REDACTED";
    Scanner scn;
    final long maxTimer;
    final long maxRebootWait;
    final String serverId;
    final String key;
    Instant lastHeartBeat=null;
    Instant lastMessage=Instant.now();
    boolean serverRestartSent = false;
    Logger logger;

    ServerWatcherChild(){
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[HH:mm:ss.SSS]");
        logger = new org.slf4j.simple.SimpleLoggerFactory().getLogger("ServerWatcherChild");
        logger.info("Creating Scanner");
        scn = new Scanner(System.in);
        logger.info("Getting MaxTimer");
        maxTimer = scn.nextLong();
        logger.info("Getting MaxRebootTime");
        maxRebootWait = scn.nextLong();
        logger.info("Getting ServerID");
        scn.skip(" ");
        serverId = scn.nextLine();
        logger.info("Getting API-Key");
        key = scn.nextLine();
        logger.info("Done With Init.");
    }
    private void run(){
        logger.info("Created ServerWatcherChild");
        ForkJoinPool.commonPool().execute(()->{
            logger.info("Hello from Heartbeat watcher");
            //noinspection InfiniteLoopStatement
            while (true){
                String l = scn.nextLine();
                if (l.equals(ServerWatcher.heartbeat)){
                    lastHeartBeat=Instant.now();
                    logger.debug("Received Heartbeat");
                }
                else logger.error("Non-recognised Message: '"+l+"'");
            }
        });
        logger.info("Started Heartbeat watcher");
        logger.info("Going into ServerWatching mode");
        while (true){
            if (lastHeartBeat==null)continue;
            if (Instant.now().isAfter(lastHeartBeat.plusSeconds(maxTimer + maxRebootWait))) {
                final long time = (Instant.now().getEpochSecond() - lastHeartBeat.getEpochSecond());
                logger.error("Server Thread has not been setting the timer for " + time + "s." +
                        (serverRestartSent ? "Server Reboot request was sent already. " : "Server Reboot request was not sent. Sending") +
                        "Killing the server");
                //if we did not send a restart request, the server will not restart if we kill it.
                if (!serverRestartSent) {
                    serverRestartSent = true;
                    power(Actions.Restart);
                }
                logger.error("Killing server now!");
                //at this point it is guaranteed, that we sent a restart request.
                //We should be able to safely kill the server.
                power(Actions.Kill);
                logger.error("Server should be dead!");
                return;
            } else if (Instant.now().isAfter(lastHeartBeat.plusSeconds(maxTimer))) {
                if (!serverRestartSent) {
                    serverRestartSent = true;
                    power(Actions.Restart);
                    logger.warn("Sent Reboot request");
                }
                if (Instant.now().isAfter(lastMessage.plusMillis(900))) {
                    final long time = (Instant.now().getEpochSecond() - lastHeartBeat.getEpochSecond());
                    logger.warn("Server Thread has not been setting the timer for " + time + "s." +
                            (serverRestartSent ? "Server Reboot request was sent already. " : "") +
                            "Waiting " + (maxTimer+maxRebootWait - time) + "s more, until the server gets killed.");
                    lastMessage = Instant.now();
                }
            } else if (Instant.now().isAfter(lastHeartBeat.plusSeconds(5))) {
                if (Instant.now().isAfter(lastMessage.plusMillis(900))) {
                    final long time = (Instant.now().getEpochSecond() - lastHeartBeat.getEpochSecond());
                    logger.warn("Server Thread has not been setting the timer for " + time + "s. ");
                    lastMessage = Instant.now();
                }
            }
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.yield();
            }
        }
    }

    public static void main(String[] args) {
        new ServerWatcherChild().run();
    }

    private void power(Actions action){
        action("api/client/servers/"+serverId+"/power","POST","{\"signal\":\""+action.action+"\"}");
    }

    private void action(@NonNull String api,@NonNull String requestMethod,String data){
        try{
            URL url = new URL(PERADACTYL_URL+api);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(requestMethod);
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+key);
            con.setFixedLengthStreamingMode(data.length());
            con.setDoOutput(true);
            con.setDoInput(true);
            //Data
            Writer ods = new OutputStreamWriter(con.getOutputStream());
            ods.write(data,0,data.length());
            ods.flush();
            ods.close();

            System.out.println("Sent Action to Pterodactyl at '"+url+"'. Response below:");
            Scanner scn = new Scanner(con.getInputStream());
            while (scn.hasNextLine()) System.out.println(scn.nextLine());
        } catch (MalformedURLException mue){
            throw new RuntimeException(mue);
        } catch (IOException e){
            logger.error("Tried to send action to Pterodactyl at '"+PERADACTYL_URL+api+"'. There was an error:",e);
        }
    }
}
