package com.c0d3m4513r.deadlockdetector.api;

import lombok.NonNull;

public interface Panel {

    @NonNull
    String getUUID(@NonNull ActionSender sender, @NonNull PanelInfo info);

    void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info);
}
