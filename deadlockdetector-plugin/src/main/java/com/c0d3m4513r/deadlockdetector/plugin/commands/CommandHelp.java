package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.command.Command;
import com.c0d3m4513r.pluginapi.command.CommandException;
import com.c0d3m4513r.pluginapi.command.CommandResult;
import com.c0d3m4513r.pluginapi.command.CommandSource;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CommandHelp implements Command {
    public static final CommandHelp Instance = new CommandHelp();

    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) {
        Optional<String> help = com.c0d3m4513r.deadlockdetector.plugin.commands.Command.Instance.getHelp(source);
        if (help.isPresent()) {
            source.sendMessage(help.get());
            return API.getCommandResult().success();
        } else {
            return API.getCommandResult().error();
        }
    }

    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Gets help for the DeadLockDetector plugin");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        return Optional.empty();
    }

    @Override
    public String getUsage(CommandSource source) {
        return "/deadlockdetector help";
    }
}
