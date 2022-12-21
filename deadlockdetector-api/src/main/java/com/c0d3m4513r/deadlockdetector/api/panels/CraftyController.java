package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.Panel;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import com.c0d3m4513r.deadlockdetector.api.panels.craftycontroller.Request;
import com.c0d3m4513r.deadlockdetector.api.panels.craftycontroller.ServerInformation;
import com.c0d3m4513r.logger.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import lombok.NonNull;
import lombok.var;

import java.io.File;

public class CraftyController implements Panel {
    @Override
    public @NonNull String getUUID(@NonNull ActionSender sender, @NonNull PanelInfo info, @NonNull Logger logger) {
        //get working directory
        String workingDir = System.getProperty("user.dir");
        if (workingDir == null) return info.getUuid();
        //get parent directory
        String parentDir = workingDir.substring(0, workingDir.lastIndexOf(File.separatorChar));

        var output = sender.action(info, "/api/v2/servers", "GET", logger);
        if (!output.isPresent()) return info.getUuid();

        try{
            Gson gson = new Gson();
            var type = TypeToken.getParameterized(Request.class,ServerInformation[].class);
            var requestResponse = gson.fromJson(output.get(), type);

            if (requestResponse instanceof Request<?>){
                var data = ((Request<?>) requestResponse).getData();
                if (data instanceof ServerInformation[]){
                    for (var server:(ServerInformation[]) data){
                        //we got a uuid
                        var uuid = server.getServer_uuid();
                        //is it the correct one?
                        if (!uuid.equals(parentDir)) continue;
                        var id = server.getServer_id();
                        logger.info("Id was determined to be '{}'",id);
                        //yes!
                        return String.valueOf(server.getServer_id());
                    }
                    logger.warn("Could not find server with uuid '"+parentDir+"' in the list of accessible servers.");
                }else {
                    logger.warn("Request data is not deserializable to an array of ServerInformation");
                }
            }else {
                logger.warn("Request response is not deserializable to Request");
            }
        }catch (JsonSyntaxException e) {
            logger.error("Error while parsing Crafty Controller response.", e);
        }
        logger.warn("Falling back to config supplied ID.");

        return info.getUuid();
    }

    @Override
    public void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info, @NonNull Logger logger) {
        System.out.println("just before action call");
        sender.action(info, "/api/v2/servers/" + info.getUuid() + "/action/" + getAction(action),"POST", logger);
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
