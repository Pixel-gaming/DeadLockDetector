package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.Panel;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import com.c0d3m4513r.logger.Logger;
import lombok.NonNull;

import java.util.Optional;

class Pterodactyl implements Panel {
    Pterodactyl(){}

    @Override
    public @NonNull String getUUID(@NonNull ActionSender sender, @NonNull PanelInfo info, @NonNull Logger logger) {
        return Optional.ofNullable(System.getenv("P_SERVER_UUID")).orElse(info.getUuid());
    }

    public void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo panelInfo, @NonNull Logger logger){
        sender.action(panelInfo,"/api/client/servers/"+panelInfo.getUuid()+"/power","POST",logger,"{\"signal\":\""+getAction(action)+"\"}");
    }

    @NonNull
    private String getAction(@NonNull Actions action){
        switch (action){
            case Start: return "start";
            case Restart: return "restart";
            case Stop: return "stop";
            case Kill: return "kill";
            default: throw new RuntimeException("Actions enum has more cases than we handled.");
        }
    }
}
