package com.c0d3m4513r.deadlockdetector.api.panels;

import com.c0d3m4513r.deadlockdetector.api.ActionSender;
import com.c0d3m4513r.deadlockdetector.api.Actions;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import lombok.NonNull;

import java.util.Optional;

@SuppressWarnings("unused")
class Panel implements com.c0d3m4513r.deadlockdetector.api.Panel {
    Panel(){}
    @Override
    public @NonNull Optional<String> getUUID() {
        return Optional.empty();
    }

    @Override
    public void power(@NonNull ActionSender sender, @NonNull Actions action, @NonNull PanelInfo info) {
    }
}
