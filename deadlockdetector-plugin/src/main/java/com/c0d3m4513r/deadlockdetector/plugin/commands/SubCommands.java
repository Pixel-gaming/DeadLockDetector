package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Main;
import com.c0d3m4513r.deadlockdetector.plugin.config.PermissionConfig;
import com.c0d3m4513r.pluginapi.Nullable;
import lombok.*;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@AllArgsConstructor
public enum SubCommands {
    help(()-> PermissionConfig.Instance.getBase().getValue(), ()->CommandHelp.Instance),
//    usage(()->PermissionConfig.Instance.getBase().getValue(), r->r::usage),

    stop(()->PermissionConfig.Instance.getStop().getValue(), ()->CommandStop.Instance),
    start(()->PermissionConfig.Instance.getStart().getValue(), ()->CommandStart.Instance),
//debug only. We enforce that in the method itself.
    sleep(()->PermissionConfig.Instance.getStart().getValue(), ()-> Main.DEVELOPMENT, ()->CommandSleep.Instance),

    reloadConfig(()->PermissionConfig.Instance.getReload().getValue(), ()->CommandReload.Instance);
    @NonNull
    public final Supplier<String> perm;

    @Nullable
    public final BooleanSupplier enabled;
    @NonNull
    public final Supplier<com.c0d3m4513r.pluginapi.command.Command> function;

    SubCommands(Supplier<String> permSupplier, Supplier<com.c0d3m4513r.pluginapi.command.Command> functionSupplier){
        this(permSupplier, null, functionSupplier);
    }
}