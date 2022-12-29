package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Process;
import com.c0d3m4513r.deadlockdetector.plugin.config.ConfigStrings;
import com.c0d3m4513r.deadlockdetector.plugin.config.PermissionConfig;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.Nullable;
import com.c0d3m4513r.pluginapi.command.CommandException;
import com.c0d3m4513r.pluginapi.command.CommandResult;
import com.c0d3m4513r.pluginapi.command.CommandSource;
import com.c0d3m4513r.pluginapi.config.TimeEntry;
import lombok.NonNull;
import lombok.var;

import java.util.*;
import java.util.stream.Collectors;

public class Command implements com.c0d3m4513r.pluginapi.command.Command {
    public static final Command Instance = new Command();

    @Override
    public @NonNull CommandResult process(CommandSource source, String[] arguments) throws CommandException {
        if (!source.hasPerm(PermissionConfig.Instance.getBase().getValue())){
            source.sendMessage(ConfigStrings.Instance.getNoPermission().getValue());
            return API.getCommandResult().error();
        }
        API.getLogger().info("[DeadlockDetector] Manually reset timer. Issued by " + source.getIdentifier());
        Process.PROCESS.heartbeat();

        //arg0 should just be the command alias
        if (arguments.length < 1 || arguments[0] == null) {
            source.sendMessage(getUsage(source));
            return API.getCommandResult().success();
        }

        SubCommands subcommand = PermissionConfig.subcommandConversion.get(arguments[0]);
        if (subcommand==null) {
            source.sendMessage("No valid subcommand was found. ");
            source.sendMessage(getUsage(source));
            throw new CommandException("No valid subcommand was found. " + getUsage(source));
        } else if (source.hasPerm(subcommand.perm.get()) && (subcommand.enabled == null || subcommand.enabled.getAsBoolean()) ){
            return subcommand.function.get().process(source, Arrays.copyOfRange(arguments, 1, arguments.length));
        } else {
            throw new CommandException(ConfigStrings.Instance.getNoPermission().getValue());
        }
    }
    @Override
    public List<String> getSuggestions(CommandSource source, String[] arguments) {
        var stream = Arrays.stream(SubCommands.values())
                .filter(s->source.hasPerm(s.perm.get()))
                .filter(s->s.enabled == null || s.enabled.getAsBoolean());
        if(arguments.length <= 1) {
            return stream.map(Enum::name)
                    .filter(s -> s.startsWith(arguments[0]))
                    .collect(Collectors.toList());
        }
        return stream.map(s -> s.function.get())
                .map(s -> {
                    Optional<List<String>> output;
                    try {
                        output = Optional.of(s.getSuggestions(source, Arrays.copyOfRange(arguments, 1, arguments.length)));
                    } catch (CommandException e) {
                        output = Optional.empty();
                    }
                    return output;
                }).filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(new LinkedList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                });

    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Configure DeadlockDetector");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        if (!source.hasPerm(PermissionConfig.Instance.getBase().getValue())) return Optional.empty();

        var str = ConfigStrings.Instance.getBase().getValue();
        str = str + "\n"+ Arrays.stream(SubCommands.values())
                .parallel()
                .map(e->e.function.get())
                .map(e->e.getHelp(source))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.joining("\n"));
        return Optional.of(str);
    }

    @Override
    public String getUsage(CommandSource source) {
        String subcommands = Arrays.stream(SubCommands.values())
                .filter(sc -> source.hasPerm(sc.perm.get()))
                .filter(sc -> sc.enabled == null || sc.enabled.getAsBoolean())
                .map(SubCommands::toString)
                .collect(Collectors.joining(","));
        return "Valid Subcommands are "+ subcommands +".";
    }

    public static Optional<TimeEntry> tryGetRequiredTimeEntry(@NonNull CommandSource source, @Nullable String argument){
        if (argument == null){
            source.sendMessage(ConfigStrings.Instance.getRequiredArgs().getValue());
            return Optional.empty();
        }
        Optional<TimeEntry> time = TimeEntry.of(argument);
        if (!time.isPresent()){
            source.sendMessage(ConfigStrings.Instance.getUnrecognisedArgs().getValue());
            return Optional.empty();
        }
        return time;
    }
}
