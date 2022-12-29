package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Process;
import com.c0d3m4513r.deadlockdetector.plugin.config.ConfigStrings;
import com.c0d3m4513r.deadlockdetector.plugin.config.PermissionConfig;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.command.Command;
import com.c0d3m4513r.pluginapi.command.CommandException;
import com.c0d3m4513r.pluginapi.command.CommandResult;
import com.c0d3m4513r.pluginapi.command.CommandSource;
import com.c0d3m4513r.pluginapi.config.TimeEntry;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandStop implements Command {
    public static final CommandStop Instance = new CommandStop();

    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) {
        if (arguments.length < 1 || arguments[0] == null) {
            source.sendMessage(ConfigStrings.Instance.getRequiredArgs().getValue());
            return API.getCommandResult().error();
        }
        Optional<TimeEntry> time = com.c0d3m4513r.deadlockdetector.plugin.commands.Command.tryGetRequiredTimeEntry(source, arguments[0]);
        if (!time.isPresent()) return API.getCommandResult().error();

        API.getLogger().info("Stopping DeadLockDetector.");
        Process.PROCESS.stopAction(time.get());
        source.sendMessage("Send a request to not take any action in the next "+time.get()+".");
        return API.getCommandResult().success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Stops the DeadLockDetector from taking actions for a given time");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (!source.hasPerm(PermissionConfig.Instance.getStop().getValue())) return Optional.empty();
        return Optional.of(ConfigStrings.Instance.getStop().getValue());
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/deadlockdetector stop [TimeEntry]";
    }
}
