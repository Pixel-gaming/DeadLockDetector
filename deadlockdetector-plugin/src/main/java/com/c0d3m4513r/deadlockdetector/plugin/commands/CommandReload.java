package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.config.ConfigStrings;
import com.c0d3m4513r.deadlockdetector.plugin.config.PermissionConfig;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.command.Command;
import com.c0d3m4513r.pluginapi.command.CommandException;
import com.c0d3m4513r.pluginapi.command.CommandResult;
import com.c0d3m4513r.pluginapi.command.CommandSource;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandReload implements Command {
    public static final CommandReload Instance = new CommandReload();
    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) {
        API.getConfigLoader().updateConfigLoader();
        API.getConfig().loadValue();
        return API.getCommandResult().success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Reloads the config of the DeadLockDetector plugin");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (!source.hasPerm(PermissionConfig.Instance.getReload().getValue())) return Optional.empty();
        return Optional.of(ConfigStrings.Instance.getReload().getValue());
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/deadlockdetector reload";
    }
}
