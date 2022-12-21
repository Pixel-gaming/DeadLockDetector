package com.c0d3m4513r.deadlockdetector.main;

import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import com.c0d3m4513r.deadlockdetector.api.ActionSenderImpl;
import com.c0d3m4513r.deadlockdetector.api.panels.Panels;
import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import com.c0d3m4513r.logger.Slf4jLogger;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.simple.SimpleLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class ServerWatcherChild {
    Scanner scn;
    volatile long maxTimer;
    volatile long maxRebootWait;
    volatile PanelInfo panelInfo;
    volatile boolean active = true;

    ScheduledFuture<?> reactivateTask;
    Instant lastMessage=Instant.now();
    AtomicReference<Instant> lastHeartBeat = new AtomicReference<>(null);
    boolean serverRestartSent = false;
    public static Logger logger;
    public static com.c0d3m4513r.logger.Logger apiLogger;

    ServerWatcherChild(){
        logger.info("Creating Scanner");
        scn = new Scanner(System.in);
    }
    private void handleAction(String l){
        switch (l) {
            case ServerWatcher.heartbeat:
                lastHeartBeat.set(Instant.now());
                logger.debug("Received Heartbeat");
                break;
            case ServerWatcher.stopActions:
                long seconds = scn.nextLong();
                scn.skip(" ");
                long nanos = scn.nextLong();
                scn.nextLine();//discard the rest of this line

                if (seconds > 0 || nanos > 0) {
                    Duration d = Duration.ofSeconds(seconds, nanos);
                    final long value;
                    final TimeUnit unit;
                    {
                        long value1;
                        TimeUnit unit1;
                        try {
                            value1 = d.toMillis();
                            unit1 = TimeUnit.MILLISECONDS;
                        } catch (ArithmeticException e) {
                            value1 = d.getSeconds();
                            unit1 = TimeUnit.SECONDS;
                        }
                        unit = unit1;
                        value = value1;
                    }
                    reactivateTask = Executors.newSingleThreadScheduledExecutor().schedule(this::reactivate, value, unit);
                    active = false;
                    logger.warn("Deactivated DeadLockDetector for " + value + unit.toString().toLowerCase() + ".");
                }
                break;
            case ServerWatcher.config:
                logger.info("Getting MaxTimer");
                maxTimer = scn.nextLong();
                logger.info("Getting MaxRebootTime");
                maxRebootWait = scn.nextLong();
                logger.info("Getting ignore_ssl_cert_errors");
                Boolean ignore_ssl_cert_errors = scn.nextBoolean();
                logger.info("Getting ServerID");
                scn.skip(" ");
                String serverId = scn.nextLine();
                logger.info("Getting Panel Type");
                String panelType = scn.nextLine();
                Panels panels = getPanel(panelType);
                logger.info("Getting API-Key");
                String key = scn.nextLine();
                logger.info("Getting Panel Url");
                String localPanelUrl = scn.nextLine().trim();
                if (localPanelUrl.endsWith("/"))
                    localPanelUrl = localPanelUrl.substring(0, localPanelUrl.lastIndexOf('/'));
                panelInfo = new PanelInfo(panels, localPanelUrl, ignore_ssl_cert_errors, key, serverId);
                logger.info("Done With Init.");
                break;
            case ServerWatcher.startActions:
                if (reactivateTask != null) reactivateTask.cancel(true);
                reactivate();
                break;
            default:
                logger.error("Non-recognised Message: '" + l + "'");
                break;
        }
    }
    private void reactivate(){
        reactivateTask=null;
        lastHeartBeat.set(null);
        active=true;
        logger.info("Reactivating DeadLockDetector, because timeout has passed. Deleting last Heartbeat info, to avoid restarting the server instantly if it is lagging badly.");
    }
    private void run(){
        logger.info("Created ServerWatcherChild");
        ForkJoinPool.commonPool().execute(()->{
            logger.info("Hello from Heartbeat watcher");
            try {
                //noinspection InfiniteLoopStatement
                while (true) {
                    String l = scn.nextLine();
                    handleAction(l);
                }
            }catch (NoSuchElementException ignored){}
        });
        logger.info("Started Heartbeat watcher");
        logger.info("Going into ServerWatching mode");
        while (true){
            Instant lastHeartBeatLocal = lastHeartBeat.get();
            if (lastHeartBeatLocal==null) continue;
            else if (active && Instant.now().isAfter(lastHeartBeatLocal.plusSeconds(maxTimer + maxRebootWait))) {
                final long time = (Instant.now().getEpochSecond() - lastHeartBeatLocal.getEpochSecond());
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
            } else if (active && Instant.now().isAfter(lastHeartBeatLocal.plusSeconds(maxTimer))) {
                if (!serverRestartSent) {
                    serverRestartSent = true;
                    power(Actions.Restart);
                    logger.warn("Sent Reboot request");
                }
                if (Instant.now().isAfter(lastMessage.plusMillis(900))) {
                    final long time = (Instant.now().getEpochSecond() - lastHeartBeatLocal.getEpochSecond());
                    logger.warn("Server Thread has not been setting the timer for " + time + "s." +
                            (serverRestartSent ? "Server Reboot request was sent already. " : "") +
                            "Waiting " + (maxTimer+maxRebootWait - time) + "s more, until the server gets killed.");
                    lastMessage = Instant.now();
                }
            } else if (Instant.now().isAfter(lastHeartBeatLocal.plusSeconds(5))) {
                if (Instant.now().isAfter(lastMessage.plusMillis(900))) {
                    final long time = (Instant.now().getEpochSecond() - lastHeartBeatLocal.getEpochSecond());
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
        System.setProperty(SimpleLogger.LOG_FILE_KEY, "System.out");
        System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
        System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "[HH:mm:ss.SSS]");
        logger = new org.slf4j.simple.SimpleLoggerFactory().getLogger("ServerWatcherChild");
        apiLogger = new Slf4jLogger(logger, Logger.class);
        new ServerWatcherChild().run();
    }

    public void power(Actions action) {
        Panels panel;
        if (panelInfo == null || (panel = panelInfo.getPanel()) == null){
            logger.warn("Invalid panel Information. Will not send a Power action!");
            return;
        }
        panel.getPanel().power(ActionSenderImpl.SENDER, action, panelInfo, apiLogger);
    }

    private Panels getPanel(String panelType){
        if (panelType.equals(Panels.Pterodactyl.name())){
            return Panels.Pterodactyl;
        }else if(panelType.equals(Panels.CraftyController.name())){
            return Panels.CraftyController;
        }else {
            logger.error("Not all Panels in the Panel enum are implemented. Your 'panel.type' config key is set to a unrecognised or unsupported value.");
            return  null;
        }
    }
}
