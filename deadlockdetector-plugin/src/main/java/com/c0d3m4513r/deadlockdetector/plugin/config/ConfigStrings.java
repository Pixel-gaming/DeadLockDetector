package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import com.c0d3m4513r.pluginapi.config.iface.Loadable;
import com.c0d3m4513r.pluginapi.config.iface.Savable;
import lombok.*;

@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor
public class ConfigStrings implements IConfigLoadableSaveable {
    public static final ConfigStrings Instance = new ConfigStrings();

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> base = new ConfigEntry<>(new ClassValue<>("This is the main command for managing server restarts.\n Arguments with <> are optional, whilst ones with [] are mandatory. \n - stands for the main command alias. By default /dld or /deadlockdetector", String.class), "translation.base");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> reload = new ConfigEntry<>(new ClassValue<>("- reload - Reloads the Config of the Plugin.", String.class), "translation.reload");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> stop = new ConfigEntry<>(new ClassValue<>("- stop [TimeEntry] - Stops any actions from being taken. This will essentially temporarily disable all functionality of the plugin, except some stdout only logs.", String.class), "translation.stop");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> start = new ConfigEntry<>(new ClassValue<>("- start - Makes the Plugins start caring about server freezes.", String.class), "translation.start");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> sleep = new ConfigEntry<>(new ClassValue<>("- sleep [TimeEntry] - Makes the Server freeze. DEVELOPMENT ONLY. Just because you see this doesn't mean that the command will work.", String.class), "translation.sleep");
//--------------------------------------------------------------------------------------------------------------------
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> noPermission = new ConfigEntry<>(
            new ClassValue<>("The Action failed, or you did not have enough permissions.",String.class),
            "translate.commands.noPermission");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> requiredArgs = new ConfigEntry<>(
            new ClassValue<>("Some required Arguments were missing.",String.class),
            "translate.commands.requiredArgs");
    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> unrecognisedArgs = new ConfigEntry<>(
            new ClassValue<>("Some Arguments were unrecognised. The Action cannot continue.",String.class),
            "translate.commands.unrecognisedArgs");

    @NonNull
    @Loadable
    @Savable
    private ConfigEntry<String> error = new ConfigEntry<>(
            new ClassValue<>("There was an internal error whilst trying to execute the Command. Please refer to the usage again.",String.class),
            "translate.commands.error");
}
