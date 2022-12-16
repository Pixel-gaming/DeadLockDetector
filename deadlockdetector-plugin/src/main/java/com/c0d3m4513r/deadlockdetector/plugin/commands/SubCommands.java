package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Main;
import com.c0d3m4513r.deadlockdetector.plugin.config.PermissionConfig;
import com.c0d3m4513r.pluginapi.Nullable;
import com.c0d3m4513r.pluginapi.command.CommandResult;
import com.c0d3m4513r.pluginapi.command.CommandSource;
import lombok.*;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

@AllArgsConstructor
public enum SubCommands {
    help(()-> PermissionConfig.Instance.getBase().getValue(), r->r::help),
//    usage(()->PermissionConfig.Instance.getBase().getValue(), r->r::usage),

    stop(()->PermissionConfig.Instance.getStop().getValue(), r->r::stop),
    start(()->PermissionConfig.Instance.getStart().getValue(), r->r::start),
//debug only. We enforce that in the method itself.
    sleep(()->PermissionConfig.Instance.getStart().getValue(), ()-> Main.DEVELOPMENT, r->r::sleep),

    reloadConfig(()->PermissionConfig.Instance.getReload().getValue(), r->r::reload);
    @NonNull
    public final Supplier<String> perm;

    @Nullable
    public final BooleanSupplier enabled;
    @NonNull
    public final Function<Command, BiFunction<CommandSource, ArrayDeque<String>, CommandResult>> function;

    SubCommands(Supplier<String> permSupplier, Function<Command, BiFunction<CommandSource, ArrayDeque<String>, CommandResult>> functionSupplier){
        this(permSupplier, null, functionSupplier);
    }
}