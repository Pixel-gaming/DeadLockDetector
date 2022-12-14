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

import java.io.*;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@org.spongepowered.api.plugin.Plugin(id = "deadlockdetector", name = "DeadlockDetector")
public class Plugin {
    @NonNull
    @Inject(optional = true)
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    public static final boolean DEVELOPMENT = false;
    private static final long defaultMaxTimer = Long.MAX_VALUE - 1;
    private long restartWait = defaultMaxTimer;
    private long maxTimer = defaultMaxTimer;
    private boolean startOnServerStart;

    private static Process proc;
    private static Writer o;

    @NonNull
    @Inject(optional = true)
    @DefaultConfig(sharedRoot = false)
    private Path configFile;

    @NonNull
    private final YAMLConfigurationLoader configurationLoader;
    private ConfigurationNode root;
    @NonNull
    private final Logger logger;

    @Inject
    public Plugin(@NonNull PluginContainer container, @NonNull final Logger logger) throws IOException {
        assertNotDevelopment();
        logger.info("[DeadlockDetector] Construct start");
        //Init config stuff
        if (configDir == null) {
            logger.warn("[DeadlockDetector] Manually getting config-dir from sponge, because the Injector did not inject the config dir");
            configDir = Sponge.getConfigManager().getPluginConfig(container).getDirectory();
        }
        if (configDir.toFile().mkdirs()) logger.info("Created configuration Directory for DeadLockDetector.");
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
        assertNotDevelopment();
        logger.info("[DeadlockDetector] PreInit start");
        loadConfig(Sponge.getServer().getConsole());
        logger.info("[DeadlockDetector] PreInit end");
    }

    private void loadConfig(CommandSource src) throws IOException {
        root = configurationLoader.load();
        long newMaxTimer = root.getNode("timeout").getLong();
        long newRebootWait = root.getNode("restartWait").getLong();
        boolean newStartOnServerStart = root.getNode("startOnServerStart").getBoolean(true);

        if (newMaxTimer != maxTimer){
            src.sendMessage(Text.of("The 'timeout' value has changed from " + maxTimer + " to " + newMaxTimer + "."));
            maxTimer = newMaxTimer;
        }
        if (newRebootWait != restartWait){
            src.sendMessage(Text.of("The 'restartWait' value has changed from " + restartWait + " to " + newRebootWait + "."));
            restartWait = newRebootWait;
        }
        if (newStartOnServerStart != startOnServerStart){
            src.sendMessage(Text.of("The 'startOnServerStart' value has changed from " + startOnServerStart + " to " + newStartOnServerStart + "."));
            startOnServerStart = newStartOnServerStart;
        }
        if(proc!=null && proc.isAlive()) sendConfig();
        logger.info("Reloaded Config. Issued by " + src.getIdentifier());
        src.sendMessage(Text.of("Realoaded Config."+
                ((proc!=null && proc.isAlive())?" Also sent a update to the Watcher.":"")
        ));
    }

    @Listener
    public void Init(GameInitializationEvent event) {
        assertNotDevelopment();
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
        CommandSpec.Builder builder = CommandSpec.builder();
        if (DEVELOPMENT){
            CommandSpec sleep = CommandSpec.builder()
                    .arguments(GenericArguments.duration(Text.of("duration")))
                    .executor((src, args) -> {
                        try {
                            Duration d = (Duration) args.getOne(Text.of("duration")).orElse(Duration.of(0, ChronoUnit.SECONDS));
                            Thread.sleep(d.toMillis());
                            } catch (InterruptedException|ClassCastException e) {
                            throw new CommandException(Text.of("exception"),e);
                        }
                        return CommandResult.success();
                    })
                    .permission("deadlockdetector.sleep")
                    .build();
            builder.child(sleep, "sleep");
        }
        Sponge.getCommandManager().register(
                this,
                builder
                        .child(reload, "reload")
                        .child(stop, "stop")
                        .child(start, "start")
                        .executor((source, args) -> {
                            logger.info("Manually reset timer. Issued by " + source.getIdentifier());
                            heartbeat();
                            return CommandResult.success();
                        })
                        .build(),
                "deadlockdetector",
                "dld");

        logger.info("[DeadlockDetector] Init end");
    }

    @Listener
    public void start(GameStartedServerEvent start) {
        assertNotDevelopment();
        if(startOnServerStart){
            startProcess();
            sendConfig();
        }
        Sponge.getScheduler()
                .createTaskBuilder()
                .name("DetectDeadlocks-SR-1t-Heartbeat")
                .intervalTicks(1)
                .delayTicks(0)
                .execute(() -> Sponge.getScheduler()
                        .createTaskBuilder()
                        .async()
                        .execute(this::heartbeat)
                        .submit(this))
                .submit(this);
        logger.info("Started Threads");
    }
    private void startProcess(){
        if (proc!=null && proc.isAlive()){
            if(o==null) o=new OutputStreamWriter(proc.getOutputStream());
            return;
        }
        try {
            logger.info("Getting current file path");
            CodeSource codeSource = Plugin.class.getProtectionDomain().getCodeSource();
            logger.info("Converting current file path to a String");
            String jarFile = codeSource
                    .getLocation()
                    .getPath();
            logger.info("Cleaning up the file path, to only point to the jar of the plugin.");
            jarFile=jarFile.substring(0,jarFile.indexOf('!')).replace("file:","");
            logger.info("Getting the Java-Instance, that executes the server.");
            String java = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java"+(System.getProperty("os.name").startsWith("Win")?".exe":"");
            logger.info("File location is: '"+jarFile+"'.");
            logger.info("Java executable is: '"+java+"'.");
            logger.info("Starting a new Thread to Observe the Server.");

            proc=new ProcessBuilder(new File(java).getPath(),
                    "-jar",
                    new File(jarFile).getPath())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .start();

            logger.info("Thread started successfully. Getting it's output stream now.");
            o=new OutputStreamWriter(proc.getOutputStream());
            sendConfig();
            heartbeat();
        } catch (Exception e) {
            logger.warn("Process exception:",e);
        }
    }
    private void sendConfig(){
        if(o==null || proc==null || !proc.isAlive()) startProcess();

        logger.info("Sending Config to Observer Process.");
        sendValue(
                ServerWatcher.config,"\n",
                Long.toString(maxTimer)," ",
                Long.toString(restartWait)," ",
                System.getenv("P_SERVER_UUID"),"\n",
                root.getNode("panel-url").getString(""), "\n",
                root.getNode("key").getString(""),"\n"
        );
        logger.info("Config Send has completed.");
    }
    private void stopAction(Duration timeout){
        logger.info("DeadLockDetector was instructed to not care about the server's state in the next "+timeout.toString()+".");
        sendValue(ServerWatcher.stopActions,"\n",
                Long.toString(timeout.getSeconds())," ",
                Long.toString(timeout.getNano()),"\n"
        );
    }
    private void startAction(){
        logger.info("DeadLockDetector was instructed to start caring about the server's state.");
        sendValue(ServerWatcher.startActions,"\n");
    }
    private void heartbeat(){
        //no log here. This is a hot path, and would spam console.
        if(proc!=null && proc.isAlive()) sendValue(ServerWatcher.heartbeat,"\n");
    }

    private void sendValue(String... strings){
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

    private static void assertNotDevelopment(){
        assert !DEVELOPMENT;
        //If this throws, I should be in dev, and remove it.
        //If this throws in Production, that means I'm an idiot and forgot to change the Development variable!
    }
}
