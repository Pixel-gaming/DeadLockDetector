package com.c0d3m4513r.deadlockdetector.plugin;

import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import com.google.inject.Inject;
import lombok.NonNull;
import lombok.val;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;

import java.io.*;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;

@org.spongepowered.api.plugin.Plugin(id = "deadlockdetector", name = "DeadlockDetector")
public class Plugin {
    @NonNull
    @Inject(optional = true)
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private static final long defaultMaxTimer = Long.MAX_VALUE - 1;
//    private static final AtomicBoolean debug = new AtomicBoolean(false);
    private static final AtomicLong restartWait = new AtomicLong(defaultMaxTimer);
    private static boolean startOnServerStart;
//    private static final AtomicBoolean serverAlreadyStopping = new AtomicBoolean(false);
//    private static volatile Instant lastTick = null;
    private static Process proc;
    private static Writer o;
//    private static Scanner i;
//    private static Instant lastAsyncExec = null;
    private static final AtomicLong maxTimer = new AtomicLong(defaultMaxTimer);
//    private static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(new DeamonThreadFactory());

    @NonNull
    @Inject(optional = true)
    @DefaultConfig(sharedRoot = false)
    private Path configFile;

    @NonNull
    private YAMLConfigurationLoader configurationLoader;
    private ConfigurationNode root;
    @NonNull
    private final Logger logger;

    @Inject
    public Plugin(@NonNull PluginContainer container, @NonNull final Logger logger) throws IOException {
        logger.info("[DeadlockDetector] Construct start");
        //Init config stuff
        if (configDir == null) {
            logger.warn("[DeadlockDetector] Manually getting config-dir from sponge, because the Injector did not inject the config dir");
            configDir = Sponge.getConfigManager().getPluginConfig(container).getDirectory();
        }
        configDir.toFile().mkdirs();
        if (configFile == null) {
            logger.warn("[DeadlockDetector] Manually constructing config-file, because the Injector did not inject the config dir");
            configFile = configDir.resolve("config.yml");
        }
        logger.info("[DeadlockDetector] The Plugin Config directory is '" + configDir.toString() + "'.");
        logger.info("[DeadlockDetector] The Plugin Config File is '" + configFile.toString() + "'.");
        //Init config loader
        this.logger = logger;
        //init api, register events
        {
            File configFileFile = new File(configFile.toUri());
            if (configFileFile.createNewFile()) {
                logger.info("[DeadlockDetector] The Config file is missing. Creating new config-file");
                FileWriter fw = new FileWriter(configFileFile);
                BufferedWriter writer = new BufferedWriter(fw);
                logger.info("[DeadlockDetector] Created Writers");
                Optional<String> os = getDefaultConfigContents();
                if (os.isPresent()) {
                    logger.info("[DeadlockDetector] Got default String");
                    writer.write(os.get());
                    writer.flush();
                    logger.info("[DeadlockDetector] Wrote default Config");
                } else {
                    logger.info("[DeadlockDetector] Got no String from the getDefaultConfigContents method.");
                }
                writer.close();
                fw.close();
            }
        }

        configurationLoader = YAMLConfigurationLoader.builder().setIndent(2).setPath(configFile).build();
        root = configurationLoader.load();
        logger.info("[DeadlockDetector] Construct end");
    }

    private Optional<String> getDefaultConfigContents() {
        return Optional.of(
                "timeout: " + defaultMaxTimer + "\n"
                    + "restartWait: " + defaultMaxTimer +"\n"
                    + "url: "+"INSERT-Perodactyl-Url-HERE\n"
                    + "key: "+"INSERT-API-TOKEN-HERE\n"
                    + "startOnServerStart: true"
        );
    }

    @Listener
    public void PreInit(GamePreInitializationEvent event) throws IOException {
        logger.info("[DeadlockDetector] PreInit start");
        loadConfig(Sponge.getServer().getConsole());
        logger.info("[DeadlockDetector] PreInit end");
    }

    private void loadConfig(CommandSource src) throws IOException {
        root = configurationLoader.load();
        long newMaxTimer = root.getNode("timeout").getLong();
        long oldMaxTimer = maxTimer.getAndSet(newMaxTimer);

        long newRebootWait = root.getNode("restartWait").getLong();
        long oldRebootWait = restartWait.getAndSet(newRebootWait);

        startOnServerStart = root.getNode("startOnServerStart").getBoolean(true);

        if (newMaxTimer != oldMaxTimer)
            src.sendMessage(Text.of("The Maximum timer value has changed from " + oldMaxTimer + " to " + newMaxTimer + "."));
        if (oldRebootWait != newRebootWait)
            src.sendMessage(Text.of("The Reboot Wait value has changed from " + oldRebootWait + " to " + newRebootWait + "."));
        if(proc!=null && proc.isAlive()) sendConfig();
        logger.info("Reloaded Config. Issued by " + src.getIdentifier());
        src.sendMessage(Text.of("Realoaded Config."+
                ((proc!=null && proc.isAlive())?" Also sent a update to the Watcher.":"")
        ));
    }

    @Listener
    public void Init(GameInitializationEvent event) {
        logger.info("[DeadlockDetector] Init start");
        CommandSpec reload = CommandSpec.builder().executor((src, args) -> {
            heartbeat();
            if (src.hasPermission("deadlockdetector.reload")) {
                try {
                    loadConfig(src);
                    return CommandResult.successCount(2);
                } catch (IOException e) {
                    throw new CommandException(Text.of("Failed to load config"), e);
                }
            }
            return CommandResult.success();
        }).build();
//        CommandSpec sleep = CommandSpec.builder()
//                .arguments(GenericArguments.duration(Text.of("duration")))
//                .executor((src, args) -> {
//                    try {
//                        Duration d = (Duration) args.getOne(Text.of("duration")).orElse(Duration.of(0, ChronoUnit.SECONDS));
//                        Thread.sleep(d.toMillis());
//                        } catch (InterruptedException|ClassCastException e) {
//                        throw new CommandException(Text.of("exception"),e);
//                    }
//                    return CommandResult.success();
//                })
//                .permission("deadlockdetector.sleep")
//                .build();
        CommandSpec stop = CommandSpec.builder()
                .arguments(GenericArguments.duration(Text.of("duration")))
                .executor((src, args) -> {
                    try {
                        Duration d = (Duration) args.getOne(Text.of("duration")).orElse(Duration.of(0, ChronoUnit.SECONDS));
                        stopAction(d);
                        src.sendMessage(Text.of("Send a request to not take any action in the next "+d.getSeconds()+"s"+d.getNano()+"ns"));
                    } catch (ClassCastException e) {
                        throw new CommandException(Text.of("exception"),e);
                    }
                    return CommandResult.success();
                })
                .permission("deadlockdetector.stop")
                .build();
        CommandSpec start = CommandSpec.builder()
                .executor((src, args) -> {
                    startAction();
                    src.sendMessage(Text.of("Send a request to reactivate the DeadLockDetector"));
                    return CommandResult.success();
                })
                .permission("deadlockdetector.start")
                .build();
        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .child(reload, "reload")
//                        .child(sleep,"sleep")
                        .child(stop,"stop")
                        .child(start,"start")
                        .executor((source, args) -> {
                            logger.info("Manually reset timer. Issued by " + source.getIdentifier());
                            heartbeat();
                            return CommandResult.success();
                        })
                        .build(),
                "deadlockdetector","dld");
        logger.info("[DeadlockDetector] Init end");
    }

    @Listener
    public void start(GameStartedServerEvent start) {
        if(startOnServerStart){
            startProcess();
            sendConfig();
        }
        Sponge.getScheduler().createTaskBuilder().name("DetectDeadlocks-SR-1t-Heartbeat").intervalTicks(1).delayTicks(0)
                .execute(() -> {
                    Sponge.getScheduler().createTaskBuilder().async().execute(this::heartbeat).submit(this);
                }).submit(this);
        logger.info("Started Threads");
    }
    private void startProcess(){
        if (proc!=null && proc.isAlive()){
            if(o==null) o=new OutputStreamWriter(proc.getOutputStream());
            return;
        }
        try {
            CodeSource codeSource = Plugin.class.getProtectionDomain().getCodeSource();
            String jarFile = codeSource
                    .getLocation()
                    .getPath();
            jarFile=jarFile.substring(0,jarFile.indexOf('!')).replace("file:","");
            String java = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java"+(System.getProperty("os.name").startsWith("Win")?".exe":"");
//            logger.error("File: "+jarFile);
//            logger.error("Java: "+java);
//            java=new File(java).getPath();
//            logger.error("Java normalized: "+java);

            proc=new ProcessBuilder(new File(java).getPath(),
                    "-jar",
                    new File(jarFile).getPath())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .start();
            o=new OutputStreamWriter(proc.getOutputStream());
            sendConfig();
            heartbeat();
//            i = new Scanner(proc.getInputStream());
        } catch (Exception e) {
            logger.warn("Process exception:",e);
        }
    }
    private void sendConfig(){
        if(o==null || proc==null || !proc.isAlive()) startProcess();
        sendValue(
                ServerWatcher.config,"\n",
                Long.toString(maxTimer.longValue())," ",
                Long.toString(restartWait.get())," ",
                System.getenv("P_SERVER_UUID"),"\n",
                root.getNode("panel-url").getString(""), "\n",
                root.getNode("key").getString(""),"\n"
        );
    }
    private void stopAction(Duration timeout){
        sendValue(ServerWatcher.stopActions,"\n",
                Long.toString(timeout.getSeconds())," ",
                Long.toString(timeout.getNano()),"\n"
        );
    }
    private void startAction(){
        sendValue(ServerWatcher.startActions,"\n");
    }
    private void heartbeat(){
        if(proc!=null && proc.isAlive()) sendValue(ServerWatcher.heartbeat,"\n");
    }

    @SafeVarargs
    private final void sendValue(String... strings){
        if(o==null) startProcess();
        try {
            for(val s : strings){
                o.write(s);
            }
            o.flush();
        }catch (IOException e){
            logger.warn("error whilst sending value: ",e);
        }
    }

//    private void handleOut(){
//        while (i.hasNext()){
//            String l = i.nextLine();
//            if (l.equals(ServerWatcher.shutdown)) shutdown();
//            else if (l.equals(ServerWatcher.crash)) crash();
//        }
//    }
//    private void shutdown(){
//        if (!serverAlreadyStopping.get()) {
//            serverAlreadyStopping.set(true);
//            Sponge.getServer().shutdown(Text.of("Detected Deadlock. Restarting."));
//            logger.error("Detected Deadlock. Restarting.");
//        }
//    }
//    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
//        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
//        theUnsafe.setAccessible(true);
//        return (Unsafe) theUnsafe.get(null);
//    }
//
//    private void crash() {
//        //fml looks for these methods, and replaces them.
//        //This will not work on forge modded servers, which sadly we are.
//        //BTW: there are multiple calls here, just because the calls are not supposed to return.
//        //If they do, something has gone not as expected.
//        logger.error("It has been determined, that the server has DEADLOCKED. TERMINATING SERVER, THIS PROCESS, THIS JAVA INSTANCE AND THIS JVM!");
//
//        try {
//            SecurityManager sm = new SecurityManager() {
//                @Override
//                public void checkExit(int status) {
//                }
//
//                @Override
//                public void checkPermission(Permission perm) {
//                }
//            };
//            System.setSecurityManager(sm);
//        } catch (Throwable e) {
//            logger.info("Could not set a custom Security manager. ", e);
//        }
//
//        try {
//            System.exit(-1);
//        } catch (Throwable e) {
//            logger.info("System.exit threw:", e);
//        }
//        logger.error("System.exit returned.");
//        try {
//            Runtime.getRuntime().exit(-1);
//        } catch (Throwable e) {
//            logger.info("Runtime.getRuntime().exit:", e);
//        }
//        logger.error("Runtime.getRuntime().exit returned.");
//        try {
//            Runtime.getRuntime().halt(-1);
//        } catch (Throwable e) {
//            logger.info("Runtime.getRuntime().halt threw:", e);
//        }
//        logger.error("Runtime.getRuntime().halt returned.");
//
//        try {
//            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("java.lang.Shutdown");
//            try {
//                Method m = clazz.getDeclaredMethod("exit", int.class);
//                m.setAccessible(true);
//                m.invoke(null, -1);
//            } catch (Throwable e) {
//                logger.error("Cannot call Shutdown.exit. ", e);
//            }
//            try {
//                Method m = clazz.getDeclaredMethod("halt", int.class);
//                m.setAccessible(true);
//                m.invoke(null, -1);
//            } catch (Throwable e) {
//                logger.error("Cannot call Shutdown.halt. ", e);
//            }
//        } catch (Throwable e) {
//            logger.error("Cannot get Shutdown class. ", e);
//        }
//
//        //This should produce some sort of segfault or something.
//        //You usually are not supposed to get stuff from the first page.
//        try {
//            getUnsafe().getByte(0);
//        } catch (Throwable e) {
//            logger.info("getUnsafe().getByte(0) threw:", e);
//        }
//        logger.error("getUnsafe().getByte(0) returned.");
//        logger.error("I do not have another way of crashing the jvm rn. This is the end.");
//    }

}
