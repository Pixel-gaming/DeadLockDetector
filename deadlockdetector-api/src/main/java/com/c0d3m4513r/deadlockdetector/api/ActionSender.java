package com.c0d3m4513r.deadlockdetector.api;

import lombok.NonNull;

public interface ActionSender {
    default void action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod){
        action(info, api, requestMethod, "");
    }

    void action(@NonNull PanelInfo info, @NonNull String api, @NonNull String requestMethod, String data);
}
