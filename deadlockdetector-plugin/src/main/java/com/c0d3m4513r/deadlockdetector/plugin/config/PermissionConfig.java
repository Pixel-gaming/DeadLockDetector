package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.deadlockdetector.plugin.commands.SubCommands;
import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import com.c0d3m4513r.pluginapi.config.iface.Loadable;
import com.c0d3m4513r.pluginapi.config.iface.Savable;
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
    @Loadable
    @Savable
    private ConfigEntry<String> base = new ConfigEntry<>(new ClassValue<>("deadlockdetector.base", String.class), "permissions.base");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> reload = new ConfigEntry<>(new ClassValue<>("deadlockdetector.reload", String.class), "permissions.reload");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> stop = new ConfigEntry<>(new ClassValue<>("deadlockdetector.stop", String.class), "permissions.stop");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> start = new ConfigEntry<>(new ClassValue<>("deadlockdetector.start", String.class), "permissions.start");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> sleep = new ConfigEntry<>(new ClassValue<>("deadlockdetector.sleep", String.class), "permissions.sleep");
}
