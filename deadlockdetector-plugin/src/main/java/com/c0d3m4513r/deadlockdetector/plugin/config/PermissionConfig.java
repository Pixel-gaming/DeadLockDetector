package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.deadlockdetector.plugin.commands.SubCommands;
import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@Setter(AccessLevel.NONE)
public class PermissionConfig implements IConfigLoadableSaveable {
    public static final PermissionConfig Instance = new PermissionConfig();

    public static final Map<String, SubCommands> subcommandConversion = Collections.unmodifiableMap(new HashMap<String, SubCommands>() {
        {
            put("help", SubCommands.help);
            put("h", SubCommands.help);
//            put("usage", SubCommands.usage);
//            put("u", SubCommands.usage);
            put("start", SubCommands.start);
            put("stop", SubCommands.stop);
            put("sl", SubCommands.sleep);
            put("sleep", SubCommands.sleep);
            put("reloadConfig", SubCommands.reloadConfig);
            put("reload", SubCommands.reloadConfig);
            put("r", SubCommands.reloadConfig);
        }
    });

    @NonNull
    private ConfigEntry<String> base = new ConfigEntry<>(new ClassValue<>("deadlockdetector.base", String.class), "permissions.base");

    @NonNull
    private ConfigEntry<String> reload = new ConfigEntry<>(new ClassValue<>("deadlockdetector.reload", String.class), "permissions.reload");

    @NonNull
    private ConfigEntry<String> stop = new ConfigEntry<>(new ClassValue<>("deadlockdetector.stop", String.class), "permissions.stop");

    @NonNull
    private ConfigEntry<String> start = new ConfigEntry<>(new ClassValue<>("deadlockdetector.start", String.class), "permissions.start");

    @NonNull
    private ConfigEntry<String> sleep = new ConfigEntry<>(new ClassValue<>("deadlockdetector.sleep", String.class), "permissions.sleep");

    @Override
    public void loadValue() {
        base.loadValue();
        reload.loadValue();
        stop.loadValue();
        start.loadValue();
        sleep.loadValue();
    }

    @Override
    public void saveValue() {
        base.saveValue();
        reload.saveValue();
        stop.saveValue();
        start.saveValue();
        sleep.saveValue();
    }
}
