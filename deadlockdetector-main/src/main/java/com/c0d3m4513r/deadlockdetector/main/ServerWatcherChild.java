package com.c0d3m4513r.deadlockdetector.main;

import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLogger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.*;

public class ServerWatcherChild {
    String perodactylUrl;
    Scanner scn;
    volatile long maxTimer;
    volatile long maxRebootWait;
    volatile String serverId;
    volatile String key;

    volatile boolean active = true;

    ScheduledFuture<?> reactivateTask;
    Instant lastMessage=Instant.now();
    volatile Instant lastHeartBeat=null;
    boolean serverRestartSent = false;
    Logger logger;

    ServerWatcherChild(){
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[HH:mm:ss.SSS]");
        logger = new org.slf4j.simple.SimpleLoggerFactory().getLogger("ServerWatcherChild");
        logger.info("Creating Scanner");
        scn = new Scanner(System.in);
    }
    private void handleAction(String l){
        if(l.equals(ServerWatcher.heartbeat)){
            lastHeartBeat=Instant.now();
            logger.debug("Received Heartbeat");
        } else if (l.equals(ServerWatcher.stopActions)) {
            long seconds = scn.nextLong();
            scn.skip(" ");
            long nanos = scn.nextLong();
            scn.nextLine();//discard the rest of this line
            if (seconds>0 || nanos>0){
                Duration d = Duration.ofSeconds(seconds,nanos);
                final long value;
                final TimeUnit unit;
                {
                    long value1;
                    TimeUnit unit1;
                    try{
                        value1 = d.toMillis();
                        unit1 = TimeUnit.MILLISECONDS;
                    }catch (ArithmeticException e){
                        value1 = d.getSeconds();
                        unit1 = TimeUnit.SECONDS;
                    }
                    unit = unit1;
                    value = value1;
                }
                reactivateTask=Executors.newSingleThreadScheduledExecutor().schedule(this::reactivate,value,unit);
                active=false;
                logger.warn("Deactivated DeadLockDetector for "+value+unit.toString().toLowerCase()+".");
            }
        } else if (l.equals(ServerWatcher.config)) {
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
        } else if (l.equals(ServerWatcher.startActions)) {
            if (reactivateTask!=null) reactivateTask.cancel(true);
            reactivate();
        } else logger.error("Non-recognised Message: '"+l+"'");
    }
    private void reactivate(){
        reactivateTask=null;
        lastHeartBeat=null;
        active=true;
        logger.info("Reactivating DeadLockDetector, because timeout has passed. Deleting last Heartbeat info, to avoid restarting the server instantly if it is lagging badly.");
    }
    private void run(){
        logger.info("Created ServerWatcherChild");
        ForkJoinPool.commonPool().execute(()->{
            logger.info("Hello from Heartbeat watcher");
            //noinspection InfiniteLoopStatement
            while (true){
                String l = scn.nextLine();
                handleAction(l);
            }
        });
        logger.info("Started Heartbeat watcher");
        logger.info("Going into ServerWatching mode");
        while (true){
            if (lastHeartBeat==null) continue;
            else if (active && Instant.now().isAfter(lastHeartBeat.plusSeconds(maxTimer + maxRebootWait))) {
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
            } else if (active && Instant.now().isAfter(lastHeartBeat.plusSeconds(maxTimer))) {
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
                    logger.warn("Server Thread has not been setting the timer for " + time + "s. "+
                            (active?"":"No action will be taken!"));
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
        InputStream error=null;
        try{
            URL url = new URL(perodactylUrl +api);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(requestMethod);
            con.setRequestProperty("Accept","application/json");
            con.setRequestProperty("Content-Type","application/json");
            con.setRequestProperty("Authorization","Bearer "+key);
            con.setFixedLengthStreamingMode(data.length());
            con.setDoOutput(true);
            con.setDoInput(true);
            error=con.getErrorStream();
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
            logger.error("Tried to send action to Pterodactyl at '"+ perodactylUrl +api+"'. There was an error:",e);
            if(error!=null){
                Scanner scn = new Scanner(error);
                while (scn.hasNextLine()) System.err.println(scn.nextLine());
            }
        }
    }
}
