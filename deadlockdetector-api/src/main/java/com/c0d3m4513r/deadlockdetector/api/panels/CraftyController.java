package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.Panel;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import lombok.NonNull;

import java.util.Optional;

public class CraftyController implements Panel {
    @Override
    public @NonNull Optional<String> getUUID() {
        return Optional.empty();
    }

    @Override
    public void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info) {
        System.out.println("just before action call");
        sender.action(info, "/api/v2/servers/" + info.getUuid() + "/action/" + getAction(action),"POST");
    }

    @NonNull
    private String getAction(@NonNull Actions action){
        switch (action){
            case Start: return "start_server";
            case Restart: return "restart_server";
            case Stop: return "stop_server";
            case Kill: return "kill_server";
            default: throw new RuntimeException("Actions enum has more cases than we handled.");
        }
    }
}
