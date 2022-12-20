package com.c0d3m4513r.deadlockdetector.api;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

public interface ActionSender {
    @NonNull
    default Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod){
        return action(info, api, requestMethod, "");
    }

    @NonNull
    Optional<String> action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, String data);


    @Nullable
    Logger getLogger();
}
