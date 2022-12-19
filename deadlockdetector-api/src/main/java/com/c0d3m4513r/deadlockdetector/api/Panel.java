package com.c0d3m4513r.deadlockdetector.api;

import lombok.NonNull;

import java.util.Optional;

public interface Panel {

    @NonNull
    Optional<String> getUUID();

    void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info);
}
