package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Process;
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

public class CommandStart implements Command {
    public static final CommandStart Instance = new CommandStart();

    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) {
        API.getLogger().info("Starting DeadLockDetector again.");
        Process.PROCESS.startAction();
        source.sendMessage("Send a request to reactivate the DeadLockDetector");
        return API.getCommandResult().success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Makes the DeadLockDetector take actions again");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (!source.hasPerm(PermissionConfig.Instance.getStart().getValue())) return Optional.empty();
        return Optional.of(ConfigStrings.Instance.getStart().getValue());
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/deadlockdetector start";
    }
}
