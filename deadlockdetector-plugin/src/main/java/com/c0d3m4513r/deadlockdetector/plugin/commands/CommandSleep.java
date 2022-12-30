package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Main;
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

public class CommandSleep implements Command {
    public static final CommandSleep Instance = new CommandSleep();

    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) throws CommandException {
        if (!Main.DEVELOPMENT) return API.getCommandResult().error();
        if (arguments.length < 1 || arguments[0] == null) return API.getCommandResult().error();

        Optional<TimeEntry> time = com.c0d3m4513r.deadlockdetector.plugin.commands.Command.tryGetRequiredTimeEntry(source, arguments[0]);
        if (!time.isPresent()) return API.getCommandResult().error();

        API.getLogger().info("Sleeping now.");
        try {
            Thread.sleep(time.get().getMs());
        } catch (InterruptedException e) {
            throw new CommandException("exception",e);
        }
        return API.getCommandResult().success();
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (!source.hasPerm(PermissionConfig.Instance.getSleep().getValue())) return Optional.empty();
        if (!Main.DEVELOPMENT) return Optional.empty();
        return Optional.of(ConfigStrings.Instance.getSleep().getValue());
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/deadlockdetector sleep [TimeEntry]";
    }
}
