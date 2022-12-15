package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import lombok.*;

@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor

public class Config implements IConfigLoadableSaveable {
    public static final Config Instance = new Config();
    public static final long DEFAULT_MAX_TIMER = Long.MAX_VALUE - 1;

    @NonNull
    private ConfigEntry<Long> restartWait = new ConfigEntry<>(new ClassValue<>(DEFAULT_MAX_TIMER, Long.class), "restartWait");
    @NonNull
    private ConfigEntry<Long> timeout = new ConfigEntry<>(new ClassValue<>(DEFAULT_MAX_TIMER, Long.class), "timeout");
    @NonNull
    private ConfigEntry<Boolean> startOnServerStart = new ConfigEntry<>(new ClassValue<>(false, Boolean.class), "startOnServerStart");
    @NonNull
    private ConfigEntry<String> apiKey = new ConfigEntry<>(new ClassValue<>("", String.class), "key");
    @NonNull
    private ConfigEntry<String> panelUrl = new ConfigEntry<>(new ClassValue<>("", String.class), "panel-url");

    @Override
    public void loadValue(){
        timeout.loadValue();
        restartWait.loadValue();
        startOnServerStart.loadValue();
        apiKey.loadValue();
        panelUrl.loadValue();
        PermissionConfig.Instance.loadValue();
        ConfigStrings.Instance.loadValue();
    }

    @Override
    public void saveValue() {
        timeout.saveValue();
        restartWait.saveValue();
        startOnServerStart.saveValue();
        apiKey.saveValue();
        panelUrl.saveValue();
        PermissionConfig.Instance.saveValue();
        ConfigStrings.Instance.saveValue();
    }
}
