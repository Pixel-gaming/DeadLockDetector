package com.c0d3m4513r.deadlockdetector.plugin.config;

import com.c0d3m4513r.pluginapi.config.ClassValue;
import com.c0d3m4513r.pluginapi.config.ConfigEntry.ConfigEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import lombok.*;

@Data
@Setter(AccessLevel.NONE)
@NoArgsConstructor
public class ConfigStrings implements IConfigLoadableSaveable {
    public static final ConfigStrings Instance = new ConfigStrings();

    @NonNull
    private ConfigEntry<String> reload = new ConfigEntry<>(new ClassValue<>("Reloads the Config of the Plugin.", String.class), "translation.reload");

    @NonNull
    private ConfigEntry<String> stop = new ConfigEntry<>(new ClassValue<>("Stops any actions from being taken. This will essentially temporarily disable all functionality of the plugin, except some stdout only logs.", String.class), "translation.stop");

    @NonNull
    private ConfigEntry<String> start = new ConfigEntry<>(new ClassValue<>("Makes the Plugins start caring about server freezes.", String.class), "translation.start");

    @NonNull
    private ConfigEntry<String> sleep = new ConfigEntry<>(new ClassValue<>("Makes the Server freeze. DEVELOPMENT ONLY. Just because you see this doesn't mean that the command will work.", String.class), "translation.sleep");
//--------------------------------------------------------------------------------------------------------------------
    @NonNull
    private ConfigEntry<String> noPermission = new ConfigEntry<>(
            new ClassValue<>("The Action failed, or you did not have enough permissions.",String.class),
            "translate.commands.noPermission");

    @NonNull
    private ConfigEntry<String> requiredArgs = new ConfigEntry<>(
            new ClassValue<>("Some required Arguments were missing.",String.class),
            "translate.commands.requiredArgs");
    @NonNull
    private ConfigEntry<String> unrecognisedArgs = new ConfigEntry<>(
            new ClassValue<>("Some Arguments were unrecognised. The Action cannot continue.",String.class),
            "translate.commands.unrecognisedArgs");

    @NonNull
    private ConfigEntry<String> error = new ConfigEntry<>(
            new ClassValue<>("There was an internal error whilst trying to execute the Command. Please refer to the usage again.",String.class),
            "translate.commands.error");

    @Override
    public void loadValue() {
        reload.loadValue();
        stop.loadValue();
        start.loadValue();
        sleep.loadValue();
        noPermission.loadValue();
        requiredArgs.loadValue();
        unrecognisedArgs.loadValue();
        error.loadValue();
    }

    @Override
    public void saveValue() {
        reload.saveValue();
        stop.saveValue();
        start.saveValue();
        sleep.saveValue();
        noPermission.saveValue();
        requiredArgs.saveValue();
        unrecognisedArgs.saveValue();
        error.saveValue();
    }
}
