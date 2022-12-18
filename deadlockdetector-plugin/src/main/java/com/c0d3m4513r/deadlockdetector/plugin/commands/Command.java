package com.c0d3m4513r.deadlockdetector.plugin.commands;

import com.c0d3m4513r.deadlockdetector.plugin.Main;
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
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Command implements com.c0d3m4513r.pluginapi.command.Command {
    static Function<CommandSource, BiFunction<String,String,Boolean>> sendHelp = (source)->(perm, str) -> {
        if (source.hasPerm(perm)) {
            if (!str.isEmpty()) source.sendMessage(str);
            return true;
        }else return false;
    };

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        if (!source.hasPerm(PermissionConfig.Instance.getBase().getValue())){
            source.sendMessage(ConfigStrings.Instance.getNoPermission().getValue());
            return API.getCommandResult().error();
        }
        API.getLogger().info("[DeadlockDetector] Manually reset timer. Issued by " + source.getIdentifier());
        Process.PROCESS.heartbeat();

        ArrayDeque<String> args = new ArrayDeque<>(Arrays.asList(arguments.split(" ")));
        //arg0 should just be the command alias
        if (args.peek() != null) {
            SubCommands subcommand = PermissionConfig.subcommandConversion.get(args.poll());
            if (subcommand==null) {
                source.sendMessage("No valid subcommand was found. ");
                source.sendMessage(getUsage(source));
                throw new CommandException("No valid subcommand was found. " + getUsage(source));
            } else if (source.hasPerm(subcommand.perm.get()) && (subcommand.enabled == null || subcommand.enabled.getAsBoolean()) ){
                return subcommand.function.apply(this).apply(source, args);
            } else {
                throw new CommandException(ConfigStrings.Instance.getNoPermission().getValue());
            }
        }else{
            source.sendMessage(getUsage(source));
            return API.getCommandResult().success();
        }
    }

    public CommandResult help(CommandSource source, ArrayDeque<String> ignoredArguments){
        val sendHelp = Command.sendHelp.apply(source);
        sendHelp.apply(
                PermissionConfig.Instance.getBase().getValue(),
                ConfigStrings.Instance.getBase().getValue()
        );
        sendHelp.apply(
                PermissionConfig.Instance.getStart().getValue(),
                ConfigStrings.Instance.getStart().getValue()
        );
        sendHelp.apply(
                PermissionConfig.Instance.getStop().getValue(),
                ConfigStrings.Instance.getStop().getValue()
        );
        sendHelp.apply(
                PermissionConfig.Instance.getSleep().getValue(),
                ConfigStrings.Instance.getSleep().getValue()
        );
        sendHelp.apply(
                PermissionConfig.Instance.getReload().getValue(),
                ConfigStrings.Instance.getReload().getValue()
        );

        return API.getCommandResult().success();
    }
    @Override
    public List<String> getSuggestions(CommandSource source, String arguments) {
        var args = arguments.split(" ");
        if(args.length == 1)
            return PermissionConfig
                    .subcommandConversion
                    .keySet()
                    .parallelStream()
                    .filter(s->s.startsWith(args[0]))
                    .collect(Collectors.toList());
        else return Collections.emptyList();
    }

    @Override
    public Optional<String> getShortDescription(CommandSource source) {
        return Optional.of("Configure DeadlockDetector");
    }

    @Override
    public Optional<String> getHelp(CommandSource source) {
        source.sendMessage("Not Implemented. - Get Help");
        return Optional.empty();
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

    public CommandResult stop(CommandSource source, ArrayDeque<String> arguments) {
        Optional<TimeEntry> time = tryGetRequiredTimeEntry(source, arguments.peek());
        if (!time.isPresent()) return API.getCommandResult().error();
        arguments.poll();

        API.getLogger().info("Stopping DeadLockDetector.");
        Process.PROCESS.stopAction(time.get());
        source.sendMessage("Send a request to not take any action in the next "+time.get()+".");
        return API.getCommandResult().success();
    }

    public CommandResult start(CommandSource source, ArrayDeque<String> ignoredArguments) {
        API.getLogger().info("Starting DeadLockDetector again.");
        Process.PROCESS.startAction();
        source.sendMessage("Send a request to reactivate the DeadLockDetector");
        return API.getCommandResult().success();
    }

    @SneakyThrows
    public CommandResult sleep(CommandSource source, ArrayDeque<String> arguments) {
        if (!Main.DEVELOPMENT) return API.getCommandResult().error();

        Optional<TimeEntry> time = tryGetRequiredTimeEntry(source, arguments.peek());
        if (!time.isPresent()) return API.getCommandResult().error();
        arguments.poll();

        API.getLogger().info("Sleeping now.");
        try {
            Thread.sleep(time.get().getMs());
        } catch (InterruptedException e) {
            throw new CommandException("exception",e);
        }
        return API.getCommandResult().success();
    }

    public Optional<TimeEntry> tryGetRequiredTimeEntry(@NonNull CommandSource source, @Nullable String argument){
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

    public CommandResult reload(CommandSource ignoredArguments, ArrayDeque<String> ignoredArguments1) {
        API.getConfigLoader().updateConfigLoader();
        API.getConfig().loadValue();
        return API.getCommandResult().success();
    }
}
