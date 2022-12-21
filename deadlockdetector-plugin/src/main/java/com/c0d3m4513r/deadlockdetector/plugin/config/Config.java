package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.deadlockdetector.api.panels.Panels;
import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import com.c0d3m4513r.pluginapi.config.iface.Loadable;
import com.c0d3m4513r.pluginapi.config.iface.Savable;
import lombok.*;

@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor

public class Config implements IConfigLoadableSaveable {
    public static final Config Instance = new Config();

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<Long> restartWait = new ConfigEntry<>(new ClassValue<>(300L, Long.class), "restartWait");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<Long> timeout = new ConfigEntry<>(new ClassValue<>(300L, Long.class), "timeout");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<Boolean> startOnServerStart = new ConfigEntry<>(new ClassValue<>(false, Boolean.class), "startOnServerStart");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> panelType = new ConfigEntry<>(new ClassValue<>("Pterodactyl", String.class), "panel.type");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> apiKey = new ConfigEntry<>(new ClassValue<>("INSERT-API-TOKEN-HERE", String.class), "panel.key");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> panelUrl = new ConfigEntry<>(new ClassValue<>("INSERT-Panel-Url-HERE", String.class), "panel.url");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<Boolean> ignore_ssl_cert_errors = new ConfigEntry<>(new ClassValue<>(false, Boolean.class), "panel.ignore_ssl_cert_errors");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> id = new ConfigEntry<>(new ClassValue<>("", String.class), "panel.id");

}
