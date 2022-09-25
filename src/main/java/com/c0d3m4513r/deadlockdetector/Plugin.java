package com.c0d3m4513r.deadlockdetector;

import com.google.inject.Inject;
import lombok.NonNull;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameLoadCompleteEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import sun.misc.Unsafe;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@org.spongepowered.api.plugin.Plugin(id = "deadlockdetector",name = "DeadlockDetector")
public class Plugin {
    @NonNull
    @Inject(optional = true)
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private static final long defaultMaxTimer = Long.MAX_VALUE-1;
//    private static final AtomicBoolean debug = new AtomicBoolean(false);
    private  static final AtomicLong restartWait = new AtomicLong(defaultMaxTimer);
    private static final AtomicBoolean serverAlreadyStopping = new AtomicBoolean(false);
    private static final AtomicLong timer = new AtomicLong(0);
    private static final AtomicLong maxTimer = new AtomicLong(defaultMaxTimer);

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
        if (configDir==null){
            logger.warn("[DeadlockDetector] Manually getting config-dir from sponge, because the Injector did not inject the config dir");
            configDir = Sponge.getConfigManager().getPluginConfig(container).getDirectory();
        }
        configDir.toFile().mkdirs();
        if (configFile==null){
            logger.warn("[DeadlockDetector] Manually constructing config-file, because the Injector did not inject the config dir");
            configFile = configDir.resolve("config.yml");
        }
        logger.info("[DeadlockDetector] The Plugin Config directory is '"+configDir.toString()+"'.");
        logger.info("[DeadlockDetector] The Plugin Config File is '"+configFile.toString()+"'.");
        //Init config loader
        this.logger=logger;
        //init api, register events
        {
            File configFileFile = new File(configFile.toUri());
            if (configFileFile.createNewFile()) {
                logger.info("[DeadlockDetector] The Config file is missing. Creating new config-file");
                FileWriter fw = new FileWriter(configFileFile);
                BufferedWriter writer = new BufferedWriter(fw);
                logger.info("[DeadlockDetector] Created Writers");
                Optional<String> os = getDefaultConfigContents();
                if (os.isPresent()){
                    logger.info("[DeadlockDetector] Got default String");
                    writer.write(os.get());
                    writer.flush();
                    logger.info("[DeadlockDetector] Wrote default Config");
                }else {
                    logger.info("[DeadlockDetector] Got no String from the getDefaultConfigContents method.");
                }
                writer.close();
                fw.close();
            }
        }

        configurationLoader = YAMLConfigurationLoader.builder().setIndent(2).setPath(configFile).build();
        root=configurationLoader.load();
        logger.info("[DeadlockDetector] Construct end");
    }

    private Optional<String> getDefaultConfigContents() {
        return Optional.of(
                "timeout: "+defaultMaxTimer+"\n"
                +"restartWait: "+defaultMaxTimer
        );
    }

    @Listener
    public void PreInit(GamePreInitializationEvent event) throws IOException {
        logger.info("[DeadlockDetector] PreInit start");
        loadConfig(Sponge.getServer().getConsole());
        logger.info("[DeadlockDetector] PreInit end");
    }
    private void loadConfig(CommandSource src) throws IOException{
        root=configurationLoader.load();
        long newMaxTimer = root.getNode("timeout").getLong();
        long oldMaxTimer = maxTimer.getAndSet(newMaxTimer);

        long newRebootWait = root.getNode("restartWait").getLong();
        long oldRebootWait = restartWait.getAndSet(newRebootWait);

        src.sendMessage(Text.of("Timer has been reset. "));
        if(newMaxTimer!=oldMaxTimer)
            src.sendMessage(Text.of("The Maximum timer value has changed from "+oldMaxTimer + " to "+ newMaxTimer+"."));
        if (oldRebootWait!=newRebootWait)
            src.sendMessage(Text.of("The Reboot Wait value has changed from "+oldRebootWait + " to "+ newRebootWait+"."));
        logger.info("Reloaded Config. Issued by "+src.getIdentifier());
    }
    @Listener
    public void Init(GameInitializationEvent event){
        logger.info("[DeadlockDetector] Init start");
        CommandSpec reload= CommandSpec.builder().executor((src, args) -> {
            timer.set(0);
            if (src.hasPermission("deadlockdetector.reload")){
                try {
                    loadConfig(src);
                    return CommandResult.successCount(2);
                } catch (IOException e) {
                    throw new CommandException(Text.of("Failed to load config"),e);
                }
            }
            return CommandResult.success();
        }).build();
//        CommandSpec debug = CommandSpec.builder().executor(((src, args) -> {
//            logger.info("Debug method. Setting timer to 1 above maxTimer");
//            timer.set(maxTimer.get()+1);
//            Plugin.debug.set(true);
//            return CommandResult.success();
//        })).build();
        Sponge.getCommandManager().register(this,
                CommandSpec.builder()
                        .child(reload,"reload")
//                        .child(debug,"debug")
                        .executor((source,args)->{
                            logger.info("Manually reset timer. Issued by "+source.getIdentifier());
                            timer.set(0);
                            return CommandResult.success();
                        })
                        .build(),
                "deadlockdetector");
        logger.info("[DeadlockDetector] Init end");
    }

    @Listener
    public void start(GameStartedServerEvent start){
        Sponge.getScheduler().createTaskBuilder().name("DetectDeadlocks-SR-1t-SetTimer").intervalTicks(1)
                .execute(()->{
//                    if (!debug.get())
                        timer.set(0);
                }).submit(this);
        Sponge.getScheduler().createTaskBuilder().name("DetectDeadlocks-AR-1s-IncTimer-Eval")
                .interval(1, TimeUnit.SECONDS)
                .async()
                .execute(()->{
                    long time = timer.getAndIncrement();
                    long maxTime = maxTimer.get();
                    long rebootWait = restartWait.get();
                    if (time>maxTime){
                        logger.warn("Server Thread has not been setting the timer for "+time+"s. Stopping.");
                        if (!serverAlreadyStopping.get()) {
                            serverAlreadyStopping.set(true);
                            Sponge.getServer().shutdown(Text.of("Detected Deadlock. Restarting."));
                        }
                        if((maxTime+rebootWait-time)<0) crash();
                        else logger.info("Waiting for "+(rebootWait+maxTime-time)+" more seconds for the server to reboot.");
                    }
                }).submit(this);
    }

    private static Unsafe getUnsafe() throws NoSuchFieldException, IllegalAccessException {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        return (Unsafe) theUnsafe.get(null);
    }
    private void crash(){
        //fml looks for these methods, and replaces them.
        //This will not work on forge modded servers, which sadly we are.
        //BTW: there are multiple calls here, just because the calls are not supposed to return.
        //If they do, something has gone not as expected.
        logger.error("It has been determined, that the server has DEADLOCKED. TERMINATING SERVER, THIS PROCESS, THIS JAVA INSTANCE AND THIS JVM!");

        try {
            SecurityManager sm = new SecurityManager(){
                @Override
                public void checkExit(int status) {}
                @Override
                public void checkPermission(Permission perm) {}
            };
            System.setSecurityManager(sm);
        }catch (Throwable e){
            logger.info("Could not set a custom Security manager. ",e);
        }

        try {
            System.exit(-1);
        } catch (Throwable e){
            logger.info("System.exit threw:",e);
        }
        logger.error("System.exit returned.");
        try {
            Runtime.getRuntime().exit(-1);
        }catch (Throwable e){
            logger.info("Runtime.getRuntime().exit:",e);
        }
        logger.error("Runtime.getRuntime().exit returned.");
        try {
            Runtime.getRuntime().halt(-1);
        }catch (Throwable e){
            logger.info("Runtime.getRuntime().halt threw:",e);
        }
        logger.error("Runtime.getRuntime().halt returned.");

        try {
            Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("java.lang.Shutdown");
            try {
                Method m = clazz.getDeclaredMethod("exit", int.class);
                m.setAccessible(true);
                m.invoke(null,-1);
            }catch (Throwable e){
                logger.error("Cannot call Shutdown.exit. ",e);
            }
            try {
                Method m = clazz.getDeclaredMethod("halt", int.class);
                m.setAccessible(true);
                m.invoke(null,-1);
            }catch (Throwable e){
                logger.error("Cannot call Shutdown.halt. ",e);
            }
        }catch (Throwable e){
            logger.error("Cannot get Shutdown class. ",e);
        }

        //This should produce some sort of segfault or something.
        //You usually are not supposed to get stuff from the first page.
        try {
            getUnsafe().getByte(0);
        } catch (Throwable e){
            logger.info("getUnsafe().getByte(0) threw:",e);
        }
        logger.error("getUnsafe().getByte(0) returned.");
        logger.error("I do not have another way of crashing the jvm rn. This is the end.");
    }

}
