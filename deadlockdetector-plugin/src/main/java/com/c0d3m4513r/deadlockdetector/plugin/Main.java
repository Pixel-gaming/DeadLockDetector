package com.c0d3m4513r.deadlockdetector.plugin;

import com.c0d3m4513r.deadlockdetector.plugin.commands.Command;
import com.c0d3m4513r.deadlockdetector.plugin.config.Config;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.TaskBuilder;
import com.c0d3m4513r.pluginapi.config.MainConfig;
import com.c0d3m4513r.pluginapi.events.EventRegistrar;
import com.c0d3m4513r.pluginapi.events.EventType;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Getter
public class Main extends MainConfig {
    public static final boolean DEVELOPMENT = false;

    public Main() {
        assertNotDevelopment();
        API.getLogger().info("[DeadlockDetector] Construct start");
        //init api, register events
        API.getLogger().info("[DeadlockDetector] Construct end");
    }

    @Override
    public void main() {
        assertNotDevelopment();
        new EventRegistrar(this::commandRegister,EventType.commandRegister, 1);
        new EventRegistrar(()-> TaskBuilder.builder()
                .executer(this::onServerStart)
                .build(), EventType.load_complete, 1);
    }

    public Optional<String> getDefaultConfigContents() {
        try(
            InputStream is = Objects.requireNonNull(Main.class.getResourceAsStream("/config.yml"));
            BufferedReader br = new BufferedReader(new InputStreamReader(is))
        ) {
            return Optional.of(br.lines().collect(Collectors.joining("\n")));
        }catch (IOException e){
            return Optional.empty();
        }
    }

    @Override
    public void loadValue(){
        API.getLogger().info("[DeadlockDetector] Reloading Config.");
        Config.Instance.loadValue();
        API.getLogger().info("[DeadlockDetector] Reloaded Config.");
    }

    @Override
    public void saveValue() {
        API.getLogger().info("[DeadlockDetector] Saving Config.");
        Config.Instance.saveValue();
        API.getLogger().info("[DeadlockDetector] Saved Config.");

    }

    public void commandRegister() {
        assertNotDevelopment();
        API.getLogger().info("[DeadlockDetector] Command register start");
        ArrayList<String> aliases = new ArrayList<>(2);
        aliases.add("deadlockdetector");
        aliases.add("dld");
        API.getCommandRegistrar().register(new Command(), aliases);
        API.getLogger().info("[DeadlockDetector] Command register end");
    }

    public void onServerStart() {
        assertNotDevelopment();
        if(Config.Instance.getStartOnServerStart().getValue()){
            Process.PROCESS.heartbeat();
            Process.PROCESS.loadValue();
        }
        TaskBuilder.builder()
                .name("DetectDeadlocks-SR-1t-Heartbeat")
                //todo: fixing this at 50ms is not ideal.
                .timer(1)
                .executer(() -> TaskBuilder
                        .builder()
                        .async()
                        .executer(Process.PROCESS::heartbeat)
                        .build())
                .build();
        API.getLogger().info("[DeadlockDetector] Started Threads");
    }

    public static void assertNotDevelopment(){
        assert !DEVELOPMENT;
        //If this throws, I should be in dev, and remove it.
        //If this throws in Production, that means I'm an idiot and forgot to change the Development variable!
    }
}
