package com.c0d3m4513r.deadlockdetector.api;

import com.c0d3m4513r.logger.Logger;
import lombok.NonNull;

import java.util.Optional;

public interface ActionSender {
    @NonNull
    default Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, @NonNull Logger logger){
        return action(info, api, requestMethod, logger,"");
    }

    @NonNull
    Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, @NonNull Logger logger, @NonNull String data);

}
