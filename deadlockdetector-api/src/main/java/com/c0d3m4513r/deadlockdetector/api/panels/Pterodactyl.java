package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.Panel;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import lombok.NonNull;

import java.util.Optional;

class Pterodactyl implements Panel {
    Pterodactyl(){}

    @Override
    public @NonNull Optional<String> getUUID() {
        return Optional.ofNullable(System.getenv("P_SERVER_UUID"));
    }

    public void power(@NonNull ActionSender sender,  @NonNull Actions action, @NonNull PanelInfo panelInfo){
        sender.action(panelInfo,"/api/client/servers/"+panelInfo.getUuid()+"/power","POST","{\"signal\":\""+getAction(action)+"\"}");
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
